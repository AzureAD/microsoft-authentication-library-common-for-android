// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.commands;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.commands.BrokerInstallResumeCoordinator;
import com.microsoft.identity.common.java.commands.BrokerInstallResumeCoordinator.ISilentResumeSubmitter;
import com.microsoft.identity.common.java.commands.BrokerInstallResumeEngine;
import com.microsoft.identity.common.java.commands.BrokerInstallResumeRegistry;
import com.microsoft.identity.common.java.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.java.commands.ParkedRecord;
import com.microsoft.identity.common.java.exception.BrokerInstallationRequiredException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.BrokerInstallResumeTelemetryHelper;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftAuthorizationErrorResponse;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.List;

/**
 * Android entry point that drives the MAM broker-install request-resume plumbing (Phase 5): it decides
 * whether a parked interactive request may be resumed, validates that the trigger genuinely comes from
 * Company Portal, and hands the resume off to the common4j
 * {@link BrokerInstallResumeCoordinator}. It owns none of the token machinery itself — the actual
 * force-fresh broker discovery ({@code getActiveBroker(shouldSkipCache=true)}) + silent submit is supplied
 * by the platform/consumer (MSAL / OneAuth) via {@link #registerSilentResumeSubmitter(ISilentResumeSubmitter)}.
 * <p>
 * Two resume triggers are supported:
 * <ul>
 *   <li><b>Automatic</b> — {@link #onResumeRedirect(Context, String, String)}: Company Portal redirects to
 *       {@code <redirectUri>?mam_resume=<cid>}; the {@code cid} selects the exact parked request.</li>
 *   <li><b>Foreground fallback</b> — {@link #onAppForegrounded(Context)}: the app returns to the foreground
 *       (e.g. Company Portal's existing redirect-back, or the user swiping back) with no {@code cid}; every
 *       still-parked request is resumed if Company Portal is now a valid broker. This is the path that makes
 *       the flow testable without any Company Portal change (§16 item 13).</li>
 * </ul>
 * Everything is gated behind {@link CommonFlight#ENABLE_BROKER_INSTALL_RESUME}; with the flight off every
 * method is an immediate no-op.
 */
public final class BrokerInstallResumeManager {

    private static final String TAG = BrokerInstallResumeManager.class.getSimpleName();

    private static final BrokerInstallResumeManager INSTANCE = new BrokerInstallResumeManager();

    /**
     * Platform-supplied silent retry (fresh discovery + silent submit). {@code volatile} because it is
     * registered on an init thread and read on redirect/foreground threads.
     */
    @Nullable
    private volatile ISilentResumeSubmitter mSubmitter;

    private BrokerInstallResumeManager() {
    }

    public static BrokerInstallResumeManager getInstance() {
        return INSTANCE;
    }

    /**
     * @return a fresh, isolated instance for unit tests (so a registered submitter does not bleed across
     *         tests). Production code must use {@link #getInstance()}.
     */
    @VisibleForTesting
    static BrokerInstallResumeManager newInstanceForTesting() {
        return new BrokerInstallResumeManager();
    }

    /**
     * Registers the platform silent-retry implementation. MSAL / OneAuth call this once at init so the
     * common module can drive the resume without owning the controller factory. If never registered, a
     * resume trigger resolves the parked request with the original install-required error (never hangs).
     *
     * @param submitter the platform silent retry; must force-fresh broker discovery.
     */
    public void registerSilentResumeSubmitter(@NonNull final ISilentResumeSubmitter submitter) {
        mSubmitter = submitter;
    }

    /**
     * Company Portal trust check, abstracted for unit-testability. The production implementation is backed
     * by {@link BrokerValidator}.
     */
    public interface ICompanyPortalTrust {
        /** @return {@code true} if {@code packageName} is Company Portal and is validly signed/installed. */
        boolean isTrustedCompanyPortal(@Nullable String packageName);

        /** @return {@code true} if Company Portal is currently installed as a valid broker. */
        boolean isCompanyPortalInstalledAndValid();
    }

    // region production entry points (Android)

    /**
     * Automatic resume: a {@code mam_resume=<cid>} redirect arrived from Company Portal.
     *
     * @param context       any Android context (used to validate the caller is Company Portal).
     * @param correlationId the {@code cid} echoed back by Company Portal.
     * @param callerPackage the package that delivered the redirect, if known (validated against CP).
     * @return {@code true} if a parked request was selected and its resume driven; {@code false} otherwise.
     */
    public boolean onResumeRedirect(@NonNull final Context context,
                                    @NonNull final String correlationId,
                                    @Nullable final String callerPackage) {
        return onResumeRedirect(correlationId, callerPackage, flights(),
                new BrokerValidatorCompanyPortalTrust(context), BrokerInstallResumeRegistry.getInstance());
    }

    /**
     * Automatic resume where the caller package is NOT available (e.g. a custom-tab / browser redirect to
     * the app's registered redirect URI — the browser does not expose which app produced it). Trust is
     * established by <em>capability</em> instead: the {@code cid} must match a request this process actually
     * parked (an unguessable per-request UUIDv4 that only reached Company Portal via the install referrer),
     * <em>and</em> Company Portal must now be installed as a valid broker.
     *
     * @param context       any Android context (used to check Company Portal is now a valid broker).
     * @param correlationId the {@code cid} carried on the resume redirect.
     * @return {@code true} if a parked request was selected and its resume driven; {@code false} otherwise.
     */
    public boolean onResumeRedirect(@NonNull final Context context,
                                    @NonNull final String correlationId) {
        return onResumeRedirectByCapability(correlationId, flights(),
                new BrokerValidatorCompanyPortalTrust(context), BrokerInstallResumeRegistry.getInstance());
    }

    /**
     * Foreground fallback: the app came to the foreground; resume any parked request if Company Portal is
     * now a valid broker.
     *
     * @param context any Android context (used to check Company Portal is now installed and valid).
     * @return the number of parked requests whose resume was driven by this call.
     */
    public int onAppForegrounded(@NonNull final Context context) {
        return onAppForegrounded(flights(), new BrokerValidatorCompanyPortalTrust(context),
                BrokerInstallResumeRegistry.getInstance());
    }

    // endregion

    // region test-visible core (no Android dependencies)

    @VisibleForTesting
    boolean onResumeRedirect(@NonNull final String correlationId,
                             @Nullable final String callerPackage,
                             @NonNull final IFlightsProvider flights,
                             @NonNull final ICompanyPortalTrust cpTrust,
                             @NonNull final BrokerInstallResumeRegistry registry) {
        if (!isEnabled(flights)) {
            return false;
        }
        // Trust anchor: only Company Portal may trigger a resume redirect (§7 caller validation).
        if (!cpTrust.isTrustedCompanyPortal(callerPackage)) {
            Logger.warn(TAG + ":onResumeRedirect",
                    "Resume redirect rejected: caller is not a trusted Company Portal.");
            return false;
        }
        final ParkedRecord record = registry.match(correlationId);
        if (record == null) {
            // Resume arrived but nothing is parked -> process death during install (funnel indicator).
            final BrokerInstallResumeTelemetryHelper telemetry = new BrokerInstallResumeTelemetryHelper();
            telemetry.setCorrelationId(correlationId);
            telemetry.onResumeReceivedNoPark();
            Logger.info(TAG + ":onResumeRedirect",
                    "Resume redirect matched no parked request (likely process death during install).");
            return false;
        }
        return resumeRecord(record);
    }

    @VisibleForTesting
    boolean onResumeRedirectByCapability(@NonNull final String correlationId,
                                         @NonNull final IFlightsProvider flights,
                                         @NonNull final ICompanyPortalTrust cpTrust,
                                         @NonNull final BrokerInstallResumeRegistry registry) {
        if (!isEnabled(flights)) {
            return false;
        }
        // Capability trust: only proceed if Company Portal is now a valid broker. The cid match below is
        // the unforgeable half — a caller that does not know the parked cid cannot select a request.
        if (!cpTrust.isCompanyPortalInstalledAndValid()) {
            Logger.warn(TAG + ":onResumeRedirectByCapability",
                    "Resume redirect rejected: Company Portal is not (yet) a valid broker.");
            return false;
        }
        final ParkedRecord record = registry.match(correlationId);
        if (record == null) {
            final BrokerInstallResumeTelemetryHelper telemetry = new BrokerInstallResumeTelemetryHelper();
            telemetry.setCorrelationId(correlationId);
            telemetry.onResumeReceivedNoPark();
            Logger.info(TAG + ":onResumeRedirectByCapability",
                    "Resume redirect matched no parked request (likely process death during install).");
            return false;
        }
        return resumeRecord(record);
    }

    @VisibleForTesting
    int onAppForegrounded(@NonNull final IFlightsProvider flights,
                          @NonNull final ICompanyPortalTrust cpTrust,
                          @NonNull final BrokerInstallResumeRegistry registry) {
        if (!isEnabled(flights) || registry.isEmpty()) {
            return 0;
        }
        if (!cpTrust.isCompanyPortalInstalledAndValid()) {
            // Company Portal is not yet a valid broker; leave the request parked (it resolves on TTL).
            return 0;
        }
        final List<ParkedRecord> pending = registry.claimAllPending();
        int resumed = 0;
        for (final ParkedRecord record : pending) {
            if (resumeRecord(record)) {
                resumed++;
            }
        }
        if (resumed > 0) {
            Logger.info(TAG + ":onAppForegrounded",
                    "Drove foreground-fallback resume for " + resumed + " parked request(s).");
        }
        return resumed;
    }

    // endregion

    /**
     * Drives the resume of a single parked record: builds a funnel span, and either hands off to the
     * coordinator (if a platform submitter is registered) or resolves the parked sink with the original
     * install-required error so the caller never hangs.
     */
    private boolean resumeRecord(@NonNull final ParkedRecord record) {
        final BrokerInstallResumeTelemetryHelper telemetry = new BrokerInstallResumeTelemetryHelper();
        telemetry.setCorrelationId(correlationIdOf(record));

        final ISilentResumeSubmitter submitter = mSubmitter;
        if (submitter == null) {
            Logger.warn(TAG + ":resumeRecord",
                    "No silent-resume submitter registered; resolving parked request with the original "
                            + "install-required error.");
            telemetry.onFailed(BrokerInstallResumeTelemetryHelper.STAGE_RESUME_RECEIVED,
                    "no_submitter_registered");
            return BrokerInstallResumeEngine.deliverError(record, installRequiredError(record));
        }
        return BrokerInstallResumeCoordinator.resume(record, submitter, telemetry);
    }

    private static boolean isEnabled(@NonNull final IFlightsProvider flights) {
        return flights.isFlightEnabled(CommonFlight.ENABLE_BROKER_INSTALL_RESUME);
    }

    @NonNull
    private static IFlightsProvider flights() {
        return CommonFlightsManager.INSTANCE.getFlightsProvider();
    }

    @Nullable
    private static String correlationIdOf(@NonNull final ParkedRecord record) {
        final InteractiveTokenCommand command = record.getInteractiveTokenCommand();
        return command == null ? null : command.getCorrelationId();
    }

    @NonNull
    private static BrokerInstallationRequiredException installRequiredError(@NonNull final ParkedRecord record) {
        return new BrokerInstallationRequiredException(
                MicrosoftAuthorizationErrorResponse.BROKER_NEEDS_TO_BE_INSTALLED,
                "Broker installation is required to complete this request.",
                record.getUpn(),
                null /* installLink not carried here */);
    }

    /**
     * Production {@link ICompanyPortalTrust} backed by {@link BrokerValidator}: a package is trusted only if
     * it is exactly the Company Portal package name and is installed with a known-good signature.
     */
    private static final class BrokerValidatorCompanyPortalTrust implements ICompanyPortalTrust {

        private final BrokerValidator mBrokerValidator;

        BrokerValidatorCompanyPortalTrust(@NonNull final Context context) {
            mBrokerValidator = new BrokerValidator(context);
        }

        @Override
        public boolean isTrustedCompanyPortal(@Nullable final String packageName) {
            return !StringUtil.isNullOrEmpty(packageName)
                    && AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME
                            .equalsIgnoreCase(packageName)
                    && mBrokerValidator.isValidBrokerPackage(packageName);
        }

        @Override
        public boolean isCompanyPortalInstalledAndValid() {
            return mBrokerValidator.isValidBrokerPackage(
                    AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        }
    }
}
