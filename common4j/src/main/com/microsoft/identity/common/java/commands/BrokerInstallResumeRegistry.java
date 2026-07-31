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

import com.microsoft.identity.common.java.logging.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import lombok.NonNull;

/**
 * Process-wide, in-memory registry of interactive token requests that have been parked while the user
 * installs the broker (Company Portal) for a MAM Conditional-Access flow.
 * <p>
 * Records are keyed by {@code correlationId} (a UUIDv4). Keying by correlation id lets concurrent
 * requests from the same app coexist as distinct records; a resume selects exactly one via its
 * {@code mam_resume=<cid>} value while leaving the others untouched (they resolve on TTL). Cross-app
 * isolation is inherent because each app process has its own registry instance.
 * <p>
 * Lookups via {@link #match(String)} are single-use (the record is removed on match). Storage is
 * in-memory only — process death during the install is an accepted loss (the request degrades to the
 * app's normal sign-in). This class is thread-safe.
 */
public class BrokerInstallResumeRegistry {

    private static final String TAG = BrokerInstallResumeRegistry.class.getSimpleName();

    /**
     * Default park time-to-live: 7 minutes. Covers a realistic Company Portal download + install +
     * first-launch on slow networks while staying bounded. On expiry the parked sink must be resolved
     * with the original install-required error so the caller never hangs.
     */
    public static final long DEFAULT_PARK_TTL_MILLISECONDS = 7L * 60L * 1000L;

    private static final BrokerInstallResumeRegistry INSTANCE = new BrokerInstallResumeRegistry();

    private final Map<String, ParkedRecord> mParkedByCorrelationId = new ConcurrentHashMap<>();

    /**
     * Visible for testing so tests can exercise an isolated registry. Production code should use
     * {@link #getInstance()}.
     */
    BrokerInstallResumeRegistry() {
    }

    /**
     * @return the process-wide singleton registry.
     */
    public static BrokerInstallResumeRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Parks a request. Overwrites any existing record for the same correlation id.
     *
     * @param correlationId the request correlation id (park key).
     * @param record        the record to park.
     */
    public void park(@NonNull final String correlationId, @NonNull final ParkedRecord record) {
        mParkedByCorrelationId.put(correlationId, record);
        Logger.info(TAG + ":park", "Parked broker-install request. Outstanding parked count: "
                + mParkedByCorrelationId.size());
    }

    /**
     * Single-use lookup: atomically removes and returns the record for the given correlation id.
     *
     * @param correlationId the correlation id echoed back by the broker (e.g. {@code mam_resume=<cid>}).
     * @return the parked record, or {@code null} if none matched (unknown / already-consumed / expired-and-swept).
     */
    public ParkedRecord match(@NonNull final String correlationId) {
        return mParkedByCorrelationId.remove(correlationId);
    }

    /**
     * Foreground-fallback lookup for the cid-less resume path (§16 item 13). When Company Portal's
     * install redirect does not carry a {@code mam_resume=<cid>} (e.g. it only brings the app back to the
     * foreground), we resume every still-parked request in this process. Each returned record is atomically
     * removed so it is claimed at most once; the caller is responsible for resolving each record's sink.
     * <p>
     * In practice an app has at most one outstanding interactive request, so this typically returns a
     * single record; returning the full set keeps the contract correct if multiple were parked.
     *
     * @return the list of claimed-and-removed parked records (never {@code null}; possibly empty).
     */
    @NonNull
    public List<ParkedRecord> claimAllPending() {
        final List<ParkedRecord> claimed = new ArrayList<>();
        for (final Map.Entry<String, ParkedRecord> entry : mParkedByCorrelationId.entrySet()) {
            // remove(key, value) so we only claim the exact record we observed.
            if (mParkedByCorrelationId.remove(entry.getKey(), entry.getValue())) {
                claimed.add(entry.getValue());
            }
        }
        if (!claimed.isEmpty()) {
            Logger.info(TAG + ":claimAllPending", "Claimed " + claimed.size()
                    + " parked request(s) for foreground-fallback resume.");
        }
        return claimed;
    }

    /**
     * Non-destructive lookup.
     *
     * @param correlationId the correlation id.
     * @return the parked record without removing it, or {@code null}.
     */
    public ParkedRecord peek(@NonNull final String correlationId) {
        return mParkedByCorrelationId.get(correlationId);
    }

    /**
     * Removes the record for the given correlation id, if present.
     *
     * @param correlationId the correlation id.
     * @return the removed record, or {@code null}.
     */
    public ParkedRecord remove(@NonNull final String correlationId) {
        return mParkedByCorrelationId.remove(correlationId);
    }

    /**
     * Removes and returns every record that is expired at {@code nowEpochMs}. The caller is responsible
     * for resolving each returned record's pending sink (with the original install-required error).
     *
     * @param nowEpochMs the current time in epoch millis.
     * @return the list of expired-and-removed records (never {@code null}).
     */
    @NonNull
    public List<ParkedRecord> sweepExpired(final long nowEpochMs) {
        final List<ParkedRecord> expired = new ArrayList<>();
        for (final Map.Entry<String, ParkedRecord> entry : mParkedByCorrelationId.entrySet()) {
            if (entry.getValue().isExpired(nowEpochMs)) {
                // remove(key, value) so we only claim the exact record we observed as expired.
                if (mParkedByCorrelationId.remove(entry.getKey(), entry.getValue())) {
                    expired.add(entry.getValue());
                }
            }
        }
        if (!expired.isEmpty()) {
            Logger.info(TAG + ":sweepExpired", "Swept " + expired.size() + " expired parked request(s).");
        }
        return expired;
    }

    /**
     * @return the number of currently parked records.
     */
    public int size() {
        return mParkedByCorrelationId.size();
    }

    /**
     * @return {@code true} if there are no parked records.
     */
    public boolean isEmpty() {
        return mParkedByCorrelationId.isEmpty();
    }

    /**
     * Removes all parked records. Intended for test isolation.
     */
    public void clear() {
        mParkedByCorrelationId.clear();
    }
}
