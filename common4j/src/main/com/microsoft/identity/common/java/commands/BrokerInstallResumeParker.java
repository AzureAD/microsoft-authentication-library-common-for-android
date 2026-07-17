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

import com.microsoft.identity.common.java.controllers.CommandResult;
import com.microsoft.identity.common.java.exception.BrokerInstallationRequiredException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.logging.Logger;

import lombok.NonNull;

/**
 * Decision logic for the MAM broker-install request-resume "park" step (PBI-1).
 * <p>
 * Extracted from {@code CommandDispatcher} so the eligibility and callback-suppression rules can be
 * unit-tested in isolation. All behavior is gated behind {@link CommonFlight#ENABLE_BROKER_INSTALL_RESUME};
 * with the flight off, both methods are no-ops and the pre-existing terminal-error behavior is unchanged.
 */
public final class BrokerInstallResumeParker {

    private static final String TAG = BrokerInstallResumeParker.class.getSimpleName();

    private BrokerInstallResumeParker() {
    }

    /**
     * Parks the request if it is an eligible broker-install interactive request and the flight is on.
     * <p>
     * Eligible when all hold: the flight is on; the command is an {@link InteractiveTokenCommand}; the
     * result is {@link CommandResult.ResultStatus#ERROR}; and the error is a
     * {@link BrokerInstallationRequiredException} (which is itself only produced when the flight is on).
     *
     * @param command       the just-executed command.
     * @param commandResult the command's terminal result.
     * @param flightsProvider the flights provider (injected for testability).
     * @param registry      the park registry (injected for testability).
     * @param parkTtlMillis the park time-to-live in millis.
     * @param nowEpochMs    the current time in epoch millis.
     * @return {@code true} if the request was parked (the caller must then suppress the terminal
     *         callback); {@code false} otherwise.
     */
    public static boolean parkIfEligible(@NonNull final BaseCommand<?> command,
                                         @NonNull final CommandResult<?> commandResult,
                                         @NonNull final IFlightsProvider flightsProvider,
                                         @NonNull final BrokerInstallResumeRegistry registry,
                                         final long parkTtlMillis,
                                         final long nowEpochMs) {
        if (!flightsProvider.isFlightEnabled(CommonFlight.ENABLE_BROKER_INSTALL_RESUME)) {
            return false;
        }
        if (!(command instanceof InteractiveTokenCommand)) {
            return false;
        }
        if (commandResult.getStatus() != CommandResult.ResultStatus.ERROR
                || !(commandResult.getResult() instanceof BrokerInstallationRequiredException)) {
            return false;
        }

        final BrokerInstallationRequiredException exception =
                (BrokerInstallationRequiredException) commandResult.getResult();
        final String correlationId = command.getCorrelationId();
        registry.park(
                correlationId,
                new ParkedRecord(
                        (InteractiveTokenCommand) command,
                        exception.getUsername(),
                        nowEpochMs + parkTtlMillis));
        Logger.info(TAG + ":parkIfEligible",
                "Parked broker-install interactive request; the terminal error will be suppressed "
                        + "and the request resumed after the broker is installed.");
        return true;
    }

    /**
     * @param correlationId   the command's correlation id.
     * @param flightsProvider the flights provider (injected for testability).
     * @param registry        the park registry (injected for testability).
     * @return {@code true} if the command's terminal callback must be suppressed because a matching
     *         parked record is awaiting broker-install resume.
     */
    public static boolean isCallbackSuppressed(@NonNull final String correlationId,
                                               @NonNull final IFlightsProvider flightsProvider,
                                               @NonNull final BrokerInstallResumeRegistry registry) {
        return flightsProvider.isFlightEnabled(CommonFlight.ENABLE_BROKER_INSTALL_RESUME)
                && registry.peek(correlationId) != null;
    }
}
