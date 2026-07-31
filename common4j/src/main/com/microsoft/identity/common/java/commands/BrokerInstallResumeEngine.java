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

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.List;

import lombok.NonNull;

/**
 * Delivers the outcome of a parked broker-install request to the app's original callback, and resolves
 * expired parked requests so a parked request can never hang (PBI-4).
 * <p>
 * The actual silent broker retry (force-fresh discovery + silent submit through the freshly installed
 * broker) is driven by the platform/OneAuth glue, which holds the controller factory; this engine owns
 * the common-side guarantees that the parked sink is fired <em>exactly once</em> (via
 * {@link ParkedRecord#tryResolve()}) and that TTL-expired parks are resolved with the original
 * install-required error.
 */
public final class BrokerInstallResumeEngine {

    private static final String TAG = BrokerInstallResumeEngine.class.getSimpleName();

    private BrokerInstallResumeEngine() {
    }

    /**
     * Supplies the terminal error to deliver for an expired parked request (typically the original
     * {@code broker_needs_to_be_installed} error the app would have received today).
     */
    public interface IExpiredParkedRequestErrorFactory {
        @NonNull
        BaseException createErrorForExpiredRequest(@NonNull ParkedRecord record);
    }

    /**
     * Delivers a successful resume result to the parked request's original callback, exactly once.
     *
     * @param record the parked record.
     * @param result the resumed authentication result to forward to the original callback.
     * @return {@code true} if this call delivered the result (won the single-resolution race);
     *         {@code false} if the sink was already resolved.
     */
    @SuppressWarnings({WarningType.unchecked_warning, WarningType.rawtype_warning})
    public static boolean deliverSuccess(@NonNull final ParkedRecord record, final Object result) {
        if (!record.tryResolve()) {
            Logger.warn(TAG + ":deliverSuccess", "Parked request already resolved; ignoring.");
            return false;
        }
        final InteractiveTokenCommand command = record.getInteractiveTokenCommand();
        if (command != null && command.getCallback() != null) {
            command.getCallback().onTaskCompleted(result);
        }
        return true;
    }

    /**
     * Delivers an error to the parked request's original callback, exactly once.
     *
     * @param record the parked record.
     * @param error  the error to forward to the original callback.
     * @return {@code true} if this call delivered the error (won the single-resolution race);
     *         {@code false} if the sink was already resolved.
     */
    @SuppressWarnings({WarningType.unchecked_warning, WarningType.rawtype_warning})
    public static boolean deliverError(@NonNull final ParkedRecord record, @NonNull final BaseException error) {
        if (!record.tryResolve()) {
            Logger.warn(TAG + ":deliverError", "Parked request already resolved; ignoring.");
            return false;
        }
        final InteractiveTokenCommand command = record.getInteractiveTokenCommand();
        if (command != null && command.getCallback() != null) {
            command.getCallback().onError(error);
        }
        return true;
    }

    /**
     * Sweeps the registry for TTL-expired parked requests and resolves each with the original
     * install-required error so the caller never hangs.
     *
     * @param registry     the park registry.
     * @param nowEpochMs   the current time in epoch millis.
     * @param errorFactory supplies the terminal error to deliver per expired request.
     * @return the number of expired requests that were resolved by this call.
     */
    public static int sweepAndResolveExpired(@NonNull final BrokerInstallResumeRegistry registry,
                                             final long nowEpochMs,
                                             @NonNull final IExpiredParkedRequestErrorFactory errorFactory) {
        final List<ParkedRecord> expired = registry.sweepExpired(nowEpochMs);
        int resolved = 0;
        for (final ParkedRecord record : expired) {
            if (deliverError(record, errorFactory.createErrorForExpiredRequest(record))) {
                resolved++;
            }
        }
        if (resolved > 0) {
            Logger.info(TAG + ":sweepAndResolveExpired",
                    "Resolved " + resolved + " expired parked request(s) with the original error.");
        }
        return resolved;
    }
}
