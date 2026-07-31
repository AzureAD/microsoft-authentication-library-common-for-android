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

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * An in-memory record of an interactive token request that has been "parked" while the user installs
 * the broker (Company Portal) for a MAM Conditional-Access flow.
 * <p>
 * The record holds the original in-flight {@link InteractiveTokenCommand} (which owns the request
 * parameters, controller factory and the app's {@code CommandCallback}), the WPJ username (UPN) used
 * as {@code login_hint} on the silent broker retry, and an expiry timestamp. The {@link #tryResolve()}
 * guard ensures the parked request's result sink / callback is fired exactly once across the
 * resume-success, resume-failure and TTL-expiry races.
 * <p>
 * Records live only in process memory (see {@link BrokerInstallResumeRegistry}); process death during
 * the install is an accepted loss.
 */
public final class ParkedRecord {

    private final InteractiveTokenCommand mInteractiveTokenCommand;

    private final String mUpn;

    private final long mExpiresAtEpochMs;

    private final AtomicBoolean mResolved = new AtomicBoolean(false);

    /**
     * @param interactiveTokenCommand the parked in-flight interactive command (may be {@code null} in
     *                                tests that exercise registry semantics only).
     * @param upn                     the WPJ username (UPN) to inject as {@code login_hint} on resume.
     * @param expiresAtEpochMs        epoch millis after which this record is expired.
     */
    public ParkedRecord(final InteractiveTokenCommand interactiveTokenCommand,
                        final String upn,
                        final long expiresAtEpochMs) {
        this.mInteractiveTokenCommand = interactiveTokenCommand;
        this.mUpn = upn;
        this.mExpiresAtEpochMs = expiresAtEpochMs;
    }

    public InteractiveTokenCommand getInteractiveTokenCommand() {
        return mInteractiveTokenCommand;
    }

    public String getUpn() {
        return mUpn;
    }

    public long getExpiresAtEpochMs() {
        return mExpiresAtEpochMs;
    }

    /**
     * @param nowEpochMs the current time in epoch millis.
     * @return {@code true} if this record has reached or passed its expiry.
     */
    public boolean isExpired(final long nowEpochMs) {
        return nowEpochMs >= mExpiresAtEpochMs;
    }

    /**
     * Atomically claims the right to resolve (fire) the parked sink/callback exactly once.
     *
     * @return {@code true} for the first caller only; {@code false} on every subsequent call.
     */
    public boolean tryResolve() {
        return mResolved.compareAndSet(false, true);
    }

    /**
     * @return {@code true} once the parked sink/callback has been claimed via {@link #tryResolve()}.
     */
    public boolean isResolved() {
        return mResolved.get();
    }
}
