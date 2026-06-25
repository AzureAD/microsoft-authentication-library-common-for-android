//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.commands;

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import lombok.NonNull;

/**
 * Process-wide circuit breaker for silent-token network failures.
 *
 * <p>Background: when an upstream caller (e.g. an IP-phone client) cannot reach the network
 * — typically because a configured WPAD/HTTP proxy is unreachable — and naively retries
 * {@code AcquireTokenSilent} in a tight loop, every retry crosses the broker IPC boundary,
 * walks the controller chain, emits telemetry events, and writes many log lines to disk.
 * On devices with small eMMC (e.g. fixed-function phones) this is enough write amplification
 * to wear the flash out in a few months.
 *
 * <p>This class trips after {@link #FAILURE_THRESHOLD} consecutive network-class failures
 * (within the cooldown window) and short-circuits subsequent silent token requests for
 * {@link #COOLDOWN_DURATION_MS}. Callers receive the same error code they would have
 * received from a real failed network attempt, so existing fall-back logic (e.g. going
 * interactive) continues to work; the only difference is that the failure returns in
 * microseconds without performing any disk I/O.
 *
 * <p>State is process-local and intentionally non-persistent: once the proxy becomes
 * reachable again, a single successful silent call resets the counter.
 */
public final class NetworkFailureCooldown {

    private static final String TAG = NetworkFailureCooldown.class.getSimpleName();

    /** Number of consecutive network-class failures required to trip the breaker. */
    static final int FAILURE_THRESHOLD = 3;

    /** How long to suppress silent token requests after the breaker trips. */
    static final long COOLDOWN_DURATION_MS = 30_000L;

    private static final NetworkFailureCooldown INSTANCE = new NetworkFailureCooldown();

    public static NetworkFailureCooldown getInstance() {
        return INSTANCE;
    }

    private final AtomicInteger mConsecutiveFailures = new AtomicInteger(0);
    private final AtomicLong mLastFailureEpochMs = new AtomicLong(0L);

    private NetworkFailureCooldown() {
        // Singleton.
    }

    /**
     * @return true if the breaker is tripped and the caller should short-circuit.
     */
    public boolean isInCooldown() {
        return getRemainingCooldownMs() > 0L;
    }

    /**
     * @return milliseconds until the cooldown window ends, or 0 if the breaker is not tripped.
     */
    public long getRemainingCooldownMs() {
        if (mConsecutiveFailures.get() < FAILURE_THRESHOLD) {
            return 0L;
        }
        final long elapsed = currentTimeMs() - mLastFailureEpochMs.get();
        final long remaining = COOLDOWN_DURATION_MS - elapsed;
        return Math.max(0L, remaining);
    }

    /**
     * Record an outcome from a silent-token attempt.
     *
     * @param e the exception thrown by the controller, or {@code null} if the attempt succeeded.
     */
    public void recordOutcome(final BaseException e) {
        if (e == null) {
            recordSuccess();
        } else if (isNetworkFailure(e)) {
            recordNetworkFailure();
        }
        // Non-network exceptions (e.g. interaction_required, invalid_grant) are intentionally
        // ignored — they indicate the request reached eSTS and got a real answer; retrying is
        // legitimate and cheap.
    }

    private void recordNetworkFailure() {
        final int count = mConsecutiveFailures.incrementAndGet();
        mLastFailureEpochMs.set(currentTimeMs());
        if (count == FAILURE_THRESHOLD) {
            Logger.warn(TAG, "Network failure threshold reached (" + count
                    + " consecutive). Silent token requests will be short-circuited for "
                    + COOLDOWN_DURATION_MS + " ms.");
        }
    }

    private void recordSuccess() {
        final int previous = mConsecutiveFailures.getAndSet(0);
        if (previous >= FAILURE_THRESHOLD) {
            Logger.info(TAG, "Network reachable again; cooldown cleared.");
        }
    }

    /**
     * Build a {@link ClientException} that mirrors what a real failed network attempt would
     * have produced, so callers handle the short-circuit identically.
     */
    @NonNull
    public ClientException buildCooldownException() {
        return new ClientException(
                ErrorStrings.IO_ERROR,
                "Silent token request suppressed: " + mConsecutiveFailures.get()
                        + " consecutive network failures, cooldown active for another "
                        + getRemainingCooldownMs() + " ms.");
    }

    static boolean isNetworkFailure(@NonNull final BaseException e) {
        final String code = e.getErrorCode();
        if (ClientException.IO_ERROR.equals(code)
                || ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE.equals(code)
                || ErrorStrings.NO_NETWORK_CONNECTION_POWER_OPTIMIZATION.equals(code)) {
            return true;
        }
        // ServiceException with no HTTP status code is symptomatic of the request never
        // having reached the server (DNS / proxy / TLS handshake failure).
        if (e instanceof ServiceException) {
            final ServiceException svc = (ServiceException) e;
            return svc.getHttpStatusCode() == ServiceException.DEFAULT_STATUS_CODE
                    && svc.getCause() instanceof java.io.IOException;
        }
        return false;
    }

    void resetForTest() {
        mConsecutiveFailures.set(0);
        mLastFailureEpochMs.set(0L);
    }

    private static long currentTimeMs() {
        return System.currentTimeMillis();
    }
}
