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

import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.BrokerInstallResumeTelemetryHelper;

import lombok.NonNull;

/**
 * Orchestrates the resume of a single parked broker-install request end-to-end (PBI-4): it projects the
 * parked interactive request into silent parameters (login_hint = WPJ UPN), invokes the platform-supplied
 * silent retry (force-fresh broker discovery + silent submit through the freshly installed Company Portal),
 * and delivers the outcome to the app's original callback <em>exactly once</em> via
 * {@link BrokerInstallResumeEngine}. When a telemetry helper is supplied it stamps the funnel
 * (resume_received &rarr; retry_success &rarr; delivered, or a failure).
 * <p>
 * <b>Why the submit is delegated.</b> Forcing fresh broker discovery
 * ({@code getActiveBroker(shouldSkipCache=true)}) and building the controller stack is a
 * platform/consumer responsibility (MSAL {@code MSALControllerFactory} / OneAuth {@code BrokerClient}) —
 * {@link com.microsoft.identity.common.java.controllers.IControllerFactory} exposes no cache-skip knob, and
 * the controllers captured on the parked command were resolved <em>before</em> Company Portal was installed
 * (they route local-only). The coordinator therefore owns the common-side spine (projection, funnel,
 * single-resolution) and takes an {@link ISilentResumeSubmitter} the consumer implements to run the actual
 * fresh-discovery submit.
 * <p>
 * This class is stateless and thread-safe; the single-resolution guarantee is enforced by the engine via
 * {@link ParkedRecord#tryResolve()}.
 */
public final class BrokerInstallResumeCoordinator {

    private static final String TAG = BrokerInstallResumeCoordinator.class.getSimpleName();

    private BrokerInstallResumeCoordinator() {
    }

    /**
     * Platform-supplied silent retry. Implementations MUST force-fresh broker discovery
     * ({@code shouldSkipCache=true}) so the freshly installed Company Portal is picked up, then submit the
     * silent request through the broker controller.
     */
    public interface ISilentResumeSubmitter {
        /**
         * @param params the projected silent request parameters (login_hint already set to the WPJ UPN).
         * @param record the parked record being resumed (exposes the original command / UPN if needed).
         * @return the resumed authentication result to forward to the original callback (never {@code null}).
         * @throws BaseException if the silent broker retry fails; the coordinator forwards it to the
         *                       original callback.
         */
        @NonNull
        Object submitSilent(@NonNull SilentTokenCommandParameters params, @NonNull ParkedRecord record)
                throws BaseException;
    }

    /**
     * Resumes a single parked request. Idempotent with respect to the single-resolution guard: if the
     * record was already resolved (by a competing resume, a TTL sweep, or a duplicate redirect) this is a
     * no-op returning {@code false}.
     *
     * @param record    the parked record to resume.
     * @param submitter the platform silent retry (fresh discovery + silent submit).
     * @param telemetry optional funnel helper; may be {@code null}.
     * @return {@code true} if this call delivered the outcome (won the single-resolution race);
     *         {@code false} otherwise.
     */
    public static boolean resume(@NonNull final ParkedRecord record,
                                 @NonNull final ISilentResumeSubmitter submitter,
                                 final BrokerInstallResumeTelemetryHelper telemetry) {
        if (record.isResolved()) {
            Logger.info(TAG + ":resume", "Parked record already resolved; nothing to resume.");
            return false;
        }

        final InteractiveTokenCommand command = record.getInteractiveTokenCommand();
        if (command == null || !(command.getParameters() instanceof InteractiveTokenCommandParameters)) {
            // Defensive: a well-formed parked record always carries interactive params. If it does not we
            // must still resolve the sink so the caller never hangs.
            final BaseException error = new ClientException(
                    ClientException.UNKNOWN_ERROR,
                    "Parked record is missing interactive parameters; cannot resume.");
            if (telemetry != null) {
                telemetry.onFailed(BrokerInstallResumeTelemetryHelper.STAGE_RESUME_RECEIVED,
                        "missing_interactive_parameters");
            }
            return BrokerInstallResumeEngine.deliverError(record, error);
        }

        if (telemetry != null) {
            telemetry.onResumeReceived();
        }

        final InteractiveTokenCommandParameters interactive =
                (InteractiveTokenCommandParameters) command.getParameters();
        final SilentTokenCommandParameters silent =
                BrokerInstallResumeParamsFactory.toSilentParameters(interactive, record.getUpn());

        try {
            final Object result = submitter.submitSilent(silent, record);
            if (telemetry != null) {
                telemetry.onRetrySuccess();
            }
            final boolean delivered = BrokerInstallResumeEngine.deliverSuccess(record, result);
            if (delivered && telemetry != null) {
                telemetry.onDelivered();
            }
            Logger.info(TAG + ":resume", delivered
                    ? "Resume delivered to original callback."
                    : "Resume produced a result but the sink was already resolved.");
            return delivered;
        } catch (final BaseException e) {
            Logger.warn(TAG + ":resume", "Silent broker retry failed on resume; delivering error to caller.");
            if (telemetry != null) {
                telemetry.onFailed(BrokerInstallResumeTelemetryHelper.STAGE_RESUME_RECEIVED, e);
            }
            return BrokerInstallResumeEngine.deliverError(record, e);
        }
    }
}
