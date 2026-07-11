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
package com.microsoft.identity.common.internal.providers;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.cache.ClientActiveBrokerCache;
import com.microsoft.identity.common.internal.cache.IClientActiveBrokerCache;
import com.microsoft.identity.common.internal.commands.parameters.AndroidInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.controllers.BrokerMsalController;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.CommandDispatcher;
import com.microsoft.identity.common.java.controllers.IControllerFactory;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.interfaces.IStorageSupplier;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.BrokerInstallLinkValidator;
import com.microsoft.identity.common.java.providers.BrokerInstallReferrerBuilder;
import com.microsoft.identity.common.java.providers.BrokerInstallResumeParkRegistry;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * In-memory coordinator for broker-install request resume, owned entirely by {@code common}.
 *
 * <p><strong>Park</strong> (pre-install): when an interactive request is blocked by a
 * Conditional-Access policy that requires installing the broker (Company Portal), the WebView client
 * parks the in-flight {@link InteractiveTokenCommand} here — holding its original callback, request
 * parameters, controller factory, and the UPN extracted from the CA redirect — and registers its
 * correlation id in {@link BrokerInstallResumeParkRegistry} so {@code CommandDispatcher} suppresses
 * the {@code BROKER_INSTALLATION} error rather than returning it to the app. No token or PII leaves
 * the device; nothing is persisted.
 *
 * <p><strong>Resume</strong> (post-install): after the broker is installed and Company Portal
 * redirects back to {@code msauth://<pkg>/resume?resume=<id>}, {@link BrokerInstallResumeActivity}
 * (running in the <em>original app process</em>) calls {@link #resume(Activity, String)}. This
 * re-dispatches the request through the same {@code MSALControllerFactory} — which now routes to the
 * broker because it is installed — with the UPN prepopulated, and forwards the token result to the
 * <em>original</em> callback. The calling app therefore receives the token on its original
 * {@code acquireToken} callback with no app-side resume code.
 *
 * <p>Because state is in-memory and process-scoped, a process death during the Play Store install
 * loses the parked request (an accepted trade-off for this design).
 */
public final class BrokerInstallResumeCoordinator {

    private static final String TAG = BrokerInstallResumeCoordinator.class.getSimpleName();

    /** E2E-only logcat tag mirroring key milestones; safe to strip for production. */
    private static final String POC_TAG = "ResumePOC";

    public static final BrokerInstallResumeCoordinator INSTANCE = new BrokerInstallResumeCoordinator();

    private final ConcurrentMap<String, ParkedEntry> mParked = new ConcurrentHashMap<>();

    /**
     * Broker-path (OneAuth) parked requests, keyed by correlation id. Distinct from {@link #mParked}
     * (the MSAL embedded-WebView / {@code CommandDispatcher} path): OneAuth drives its own djinni
     * {@code BrokerEventSink} through a <em>blocking</em> {@code BrokerMsalController.acquireToken}
     * call on a background thread, not a {@code CommandCallback}. Parking that request therefore means
     * holding a {@link ResultFuture} the blocked thread is waiting on; resume re-runs the broker
     * acquire and completes the future, which returns the token on the original OneAuth sink.
     */
    private final ConcurrentMap<String, BrokerParkedEntry> mBrokerParked = new ConcurrentHashMap<>();

    /**
     * Upper bound on how long the parked {@code acquireToken} thread blocks awaiting resume. Covers a
     * user-paced Play Store install + Company Portal round-trip; on timeout the request fails back to
     * the caller's normal terminal broker-installation behavior rather than blocking forever.
     */
    private static final long INSTALL_RESUME_TIMEOUT_MINUTES = 10;

    private BrokerInstallResumeCoordinator() {
        // singleton
    }

    /**
     * POC-only: surface a human-readable progress step to the user as a bottom-of-screen
     * {@link Toast} so the broker-install resume flow is observable end-to-end during testing
     * (first sign-in blocked → Company Portal install → resume in broker context → token returned).
     * Posts to the main looper so it is safe to call from any thread. Safe to strip for production.
     *
     * @param context any context (its application context is used); no-op if {@code null}.
     * @param message the step description to show at the bottom of the screen.
     */
    public static void showStep(@Nullable final Context context, @NonNull final String message) {
        if (context == null) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        new Handler(Looper.getMainLooper()).post(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(appContext, message, Toast.LENGTH_LONG).show();
            }
        });
    }

    /**
     * Broker-path (OneAuth) park: installs the broker (Company Portal) and blocks the calling
     * {@code acquireToken} thread until the freshly installed broker resumes the request, returning
     * the token on the caller's original (OneAuth) sink.
     *
     * <p>Unlike {@link #park(String, InteractiveTokenCommand, String)} (the MSAL embedded-WebView /
     * {@code CommandDispatcher} path), this is driven from {@code BrokerMsalController.acquireToken}
     * on the broker path — where real 1P (OneAuth) apps actually receive the Conditional-Access
     * install challenge. OneAuth's {@code BrokerClient} calls {@code acquireToken} synchronously on a
     * background thread and delivers whatever it returns on its djinni {@code BrokerEventSink}; so we
     * "keep the sink pending" simply by blocking that thread on a {@link ResultFuture} until resume.
     *
     * <p>Nothing is persisted (in-memory only, by design): a process death during the Play Store
     * install loses the parked request and the caller falls back to today's blocked-install behavior.
     *
     * @param controller  the broker controller to re-run the acquire on once the broker is installed.
     * @param appContext  application context used to launch the Play Store and read the origin package.
     * @param parameters  the in-flight interactive request (carries correlation id, login hint/UPN,
     *                    and redirect uri). Must be an Android interactive request.
     * @param installUrl  the broker-install (Play Store) URL captured from the CA challenge.
     * @return the token result once resumed in broker context, or {@code null} if the request could
     * not be parked (unsafe link, missing correlation id, timeout, or interruption) — in which case
     * the caller should fall through to today's terminal broker-installation behavior.
     */
    @Nullable
    public AcquireTokenResult installParkAndAwait(
            @NonNull final BrokerMsalController controller,
            @NonNull final Context appContext,
            @NonNull final AndroidInteractiveTokenCommandParameters parameters,
            @NonNull final String installUrl) {
        final String methodTag = TAG + ":installParkAndAwait";

        if (!BrokerInstallLinkValidator.isSafeBrokerInstallLink(installUrl)) {
            Logger.warn(methodTag, "Broker-install link failed the allowlist; not parking.");
            return null;
        }

        final String resumeId = parameters.getCorrelationId();
        if (StringUtil.isNullOrEmpty(resumeId)) {
            Logger.warn(methodTag, "No correlation id on the request; cannot park for resume.");
            return null;
        }

        final ResultFuture<AcquireTokenResult> future = new ResultFuture<>();
        mBrokerParked.put(resumeId, new BrokerParkedEntry(controller, parameters, future));
        // Suppress any parallel CommandDispatcher-path error for the same correlation id.
        BrokerInstallResumeParkRegistry.park(resumeId);

        final String referrerUrl = BrokerInstallReferrerBuilder.withResumePointer(
                installUrl,
                resumeId,
                appContext.getPackageName(),
                parameters.getRedirectUri());

        Log.i(POC_TAG, "RESUME-PARKED broker-path request parked in common; resumeId=" + resumeId);
        Logger.info(methodTag, "Parked broker-install request; launching Company Portal install.");
        showStep(appContext,
                "Broker-install resume \u2460/\u2463: Sign-in blocked \u2192 parking request, installing Company Portal");
        launchInstall(appContext, referrerUrl);

        try {
            // Block the OneAuth acquireToken thread until the deep-link resume completes the future.
            return future.get(INSTALL_RESUME_TIMEOUT_MINUTES, TimeUnit.MINUTES);
        } catch (final TimeoutException e) {
            Logger.warn(methodTag, "Timed out awaiting broker-install resume; failing back to caller.");
            return null;
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            Logger.warn(methodTag, "Interrupted awaiting broker-install resume; failing back to caller.");
            return null;
        } catch (final ExecutionException e) {
            Logger.warn(methodTag, "Broker-install resume failed: " + e.getMessage());
            return null;
        } finally {
            mBrokerParked.remove(resumeId);
            BrokerInstallResumeParkRegistry.unpark(resumeId);
        }
    }

    /** Launches the Play Store to the (validated) broker-install link. BAL-safe from app context. */
    private static void launchInstall(@NonNull final Context context, @NonNull final String installUrl) {
        final String link = installUrl.replace(
                AuthenticationConstants.Broker.BROWSER_EXT_PREFIX, "https://");
        final Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(link));
        // OneAuth may be initialized without an Activity, so launch as a new task from app context.
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.getApplicationContext().startActivity(intent);
    }

    /**
     * Parks the in-flight interactive request so its result is suppressed and can be resumed after
     * the broker is installed.
     *
     * @param resumeId single-use resume key (the command's correlation id).
     * @param command  the interactive command currently blocked to install the broker.
     * @param upn      the user's UPN extracted from the CA redirect (used to prepopulate the resume);
     *                 may be null, in which case the original request's login hint is used.
     * @return true if the command was parked; false if it could not be parked (e.g. not an Android
     * interactive request), in which case the caller should fall back to today's behavior.
     */
    public boolean park(@NonNull final String resumeId,
                        @NonNull final InteractiveTokenCommand command,
                        @Nullable final String upn) {
        if (!(command.getParameters() instanceof AndroidInteractiveTokenCommandParameters)) {
            Logger.warn(TAG, "In-flight request is not an Android interactive request; cannot park.");
            return false;
        }
        mParked.put(resumeId, new ParkedEntry(
                command.getCallback(),
                (AndroidInteractiveTokenCommandParameters) command.getParameters(),
                command.getControllerFactory(),
                command.getPublicApiId(),
                upn));
        BrokerInstallResumeParkRegistry.park(resumeId);
        Log.i(POC_TAG, "RESUME-PARKED request parked in common; resumeId=" + resumeId);
        Logger.info(TAG, "Parked interactive request for broker-install resume.");
        return true;
    }

    /**
     * Resumes a parked request in broker context and delivers the token to the original callback.
     * Runs the interactive retry via {@link CommandDispatcher#beginInteractive(InteractiveTokenCommand)}
     * so all normal result-conversion machinery is reused.
     *
     * @param activity the (foreground) deep-link activity used to launch the broker UI (BAL-safe).
     * @param resumeId the resume key carried back on the Company Portal redirect.
     * @return true if a parked request was found and resume was dispatched; false otherwise.
     */
    public boolean resume(@NonNull final Activity activity, @NonNull final String resumeId) {
        // Broker-path (OneAuth) parked requests take priority: complete the blocked acquireToken
        // thread by re-running the acquire in broker context and delivering the result on its future.
        // Remove (not peek) so a duplicate resume deep-link cannot re-launch the broker UI twice.
        final BrokerParkedEntry brokerEntry = mBrokerParked.remove(resumeId);
        if (brokerEntry != null) {
            return resumeBrokerParked(activity, resumeId, brokerEntry);
        }

        final ParkedEntry entry = mParked.remove(resumeId);
        // No longer suppress: the resumed request must deliver its result to the (wrapped) callback.
        BrokerInstallResumeParkRegistry.unpark(resumeId);

        if (entry == null) {
            Log.w(POC_TAG, "RESUME-NO-PARKED no parked request for resumeId=" + resumeId);
            Logger.warn(TAG, "No parked request found to resume (expired process or unknown id).");
            return false;
        }

        final String loginHint = !StringUtil.isNullOrEmpty(entry.upn)
                ? entry.upn
                : entry.params.getLoginHint();

        final IPlatformComponents components =
                AndroidPlatformComponentsFactory.createFromActivity(activity, null);

        // The original request ran before the broker was installed, so client-side broker discovery
        // cached a "no active broker" result and started a ~60-minute AccountManager-only backoff.
        // Clear that stale state now (a single clear also removes the backoff key) so the resumed
        // request performs a fresh IPC discovery, finds the just-installed Company Portal, and routes
        // through the broker rather than falling back to the local controller.
        invalidateBrokerDiscoveryCache(components);

        final AndroidInteractiveTokenCommandParameters resumeParams = entry.params.toBuilder()
                .activity(activity)
                .platformComponents(components)
                .loginHint(loginHint)
                .build();

        final CommandCallback wrappedCallback = new CommandCallback() {
            @Override
            @SuppressWarnings("unchecked")
            public void onTaskCompleted(final Object result) {
                Log.i(POC_TAG, "RESUME-COMPLETED token delivered to original callback; resumeId=" + resumeId);
                Logger.info(TAG, "Resumed request completed; delivering token to original caller.");
                showStep(activity, "Broker-install resume \u2463/\u2463: Token returned successfully \u2705");
                // Deliver the token to the app's original callback first (the app updates its own
                // UI/state), then land the user back in the origin app so they actually see the
                // signed-in result instead of the leftover broker / eSTS Custom Tab.
                entry.callback.onTaskCompleted(result);
                returnToOriginApp(activity);
            }

            @Override
            @SuppressWarnings("unchecked")
            public void onError(final Object error) {
                Log.w(POC_TAG, "RESUME-ERROR resumed request failed; resumeId=" + resumeId);
                Logger.warn(TAG, "Resumed request failed; forwarding error to original caller.");
                entry.callback.onError(error);
                returnToOriginApp(activity);
            }

            @Override
            public void onCancel() {
                Log.w(POC_TAG, "RESUME-CANCEL resumed request cancelled; resumeId=" + resumeId);
                Logger.warn(TAG, "Resumed request cancelled; forwarding cancel to original caller.");
                entry.callback.onCancel();
                returnToOriginApp(activity);
            }
        };

        final InteractiveTokenCommand resumeCommand = new InteractiveTokenCommand(
                resumeParams,
                brokerForcingFactory(entry.controllerFactory),
                wrappedCallback,
                entry.publicApiId);

        Log.i(POC_TAG, "RESUME-DISPATCH re-dispatching in broker context; resumeId=" + resumeId
                + " loginHintPresent=" + !StringUtil.isNullOrEmpty(loginHint));
        Logger.info(TAG, "Re-dispatching parked request; forcing broker controller.");
        showStep(activity, "Broker-install resume \u2462/\u2463: Retrying token in broker context");
        CommandDispatcher.beginInteractive(resumeCommand);
        return true;
    }

    /**
     * Wraps the original request's controller factory so the resumed request is guaranteed to run in
     * broker context: {@code getDefaultController()} returns the {@link BrokerMsalController} if the
     * factory offers one (it will, now that Company Portal is installed and the app opted into
     * broker). Falls back to the delegate's default only if no broker controller is available.
     */
    private static IControllerFactory brokerForcingFactory(@NonNull final IControllerFactory delegate) {
        return new IControllerFactory() {
            @NonNull
            @Override
            public BaseController getDefaultController() {
                for (final BaseController controller : delegate.getAllControllers()) {
                    if (controller instanceof BrokerMsalController) {
                        return controller;
                    }
                }
                Logger.warn(TAG, "No broker controller available for resume; falling back to default.");
                return delegate.getDefaultController();
            }

            @NonNull
            @Override
            public List<BaseController> getAllControllers() {
                return delegate.getAllControllers();
            }
        };
    }

    /**
     * Clears the client-SDK active-broker discovery cache so the resumed request re-discovers the
     * freshly-installed broker via a live IPC query. A single {@code clearCachedActiveBroker()} call
     * removes both the cached package/signature and the "use AccountManager for the next N ms"
     * backoff that was set when discovery failed before the broker was installed.
     */
    private static void invalidateBrokerDiscoveryCache(@NonNull final IPlatformComponents components) {
        try {
            final IStorageSupplier storageSupplier = components.getStorageSupplier();
            final IClientActiveBrokerCache clientCache =
                    ClientActiveBrokerCache.getClientSdkCache(storageSupplier);
            clientCache.clearCachedActiveBroker();
            Log.i(POC_TAG, "RESUME-CACHE-CLEARED client broker-discovery cache invalidated");
            Logger.info(TAG, "Cleared client-side active-broker discovery cache before resume.");
        } catch (final Exception e) {
            // Non-fatal: if discovery still routes local, the resume simply fails through the normal
            // path. Log and continue rather than dropping the resume entirely.
            Logger.warn(TAG, "Failed to clear broker-discovery cache before resume: " + e.getMessage());
        }
    }

    /**
     * Hands terminal-result handling to the deep-link host activity, which lands the user back in
     * the origin app from a foreground-safe moment (see
     * {@link BrokerInstallResumeActivity#onResumedRequestTerminated()}). Falls back to simply
     * finishing the host if it is not the expected resume activity.
     */
    private static void returnToOriginApp(@Nullable final Activity activity) {
        if (activity instanceof BrokerInstallResumeActivity) {
            ((BrokerInstallResumeActivity) activity).onResumedRequestTerminated();
        } else {
            finishHost(activity);
        }
    }

    private static void finishHost(@Nullable final Activity activity) {
        if (activity != null && !activity.isFinishing()) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    activity.finish();
                }
            });
        }
    }

    /**
     * Resumes a broker-path (OneAuth) parked request: re-runs {@code acquireToken} in broker context
     * (broker is now installed) and completes the parked {@link ResultFuture}, which unblocks the
     * original {@code acquireToken} thread so OneAuth delivers the token on its original sink.
     *
     * @param activity the foreground deep-link activity used to launch the broker UI (BAL-safe).
     * @param resumeId the resume key carried back on the Company Portal redirect.
     * @param entry    the parked broker request.
     * @return true (a broker-path request was found and its resume was driven).
     */
    private boolean resumeBrokerParked(@NonNull final Activity activity,
                                       @NonNull final String resumeId,
                                       @NonNull final BrokerParkedEntry entry) {
        final String methodTag = TAG + ":resumeBrokerParked";
        // The waiting thread is unblocked via the future; drop the suppression marker now.
        BrokerInstallResumeParkRegistry.unpark(resumeId);

        Log.i(POC_TAG, "RESUME-DEEPLINK broker-path resume; resumeId=" + resumeId);
        showStep(activity,
                "Broker-install resume \u2461/\u2463: Company Portal installed \u2192 resuming request");

        final IPlatformComponents components =
                AndroidPlatformComponentsFactory.createFromActivity(activity, null);

        // The original request ran before the broker was installed, so client-side broker discovery
        // cached a "no active broker" result and started a ~60-minute AccountManager-only backoff.
        // Clear that stale state so the resumed request re-discovers the just-installed Company Portal.
        invalidateBrokerDiscoveryCache(components);

        // Retry with the resume activity + fresh platform components, and clear the install URL so the
        // retry runs the normal broker path (broker is present now) and never re-enters the park.
        final AndroidInteractiveTokenCommandParameters resumeParams = entry.params.toBuilder()
                .activity(activity)
                .platformComponents(components)
                .loginHint(entry.params.getLoginHint())
                .brokerInstallationUrl(null)
                .build();

        try {
            Log.i(POC_TAG, "RESUME-DISPATCH re-running acquire in broker context; resumeId=" + resumeId);
            showStep(activity, "Broker-install resume \u2462/\u2463: Retrying token in broker context");
            final AcquireTokenResult result = entry.controller.acquireToken(resumeParams);
            // Unblocks the parked OneAuth acquireToken thread, which returns this result on its sink.
            entry.future.setResult(result);
            Log.i(POC_TAG, "RESUME-COMPLETED token delivered to original OneAuth sink; resumeId=" + resumeId);
            showStep(activity, "Broker-install resume \u2463/\u2463: Token returned successfully \u2705");
        } catch (final Exception e) {
            Log.w(POC_TAG, "RESUME-ERROR broker-path resume failed; resumeId=" + resumeId);
            Logger.warn(methodTag, "Resumed broker request failed; forwarding error to caller.");
            // Unblocks the parked thread with the failure so the OneAuth sink surfaces a real error
            // rather than hanging until the install-resume timeout elapses.
            entry.future.setException(e);
        } finally {
            returnToOriginApp(activity);
        }
        return true;
    }

    /** Immutable snapshot of a parked interactive request. Holds no persisted state. */
    private static final class ParkedEntry {
        @SuppressWarnings("rawtypes")
        final CommandCallback callback;
        final AndroidInteractiveTokenCommandParameters params;
        final IControllerFactory controllerFactory;
        final String publicApiId;
        @Nullable
        final String upn;

        ParkedEntry(@SuppressWarnings("rawtypes") final CommandCallback callback,
                    final AndroidInteractiveTokenCommandParameters params,
                    final IControllerFactory controllerFactory,
                    final String publicApiId,
                    @Nullable final String upn) {
            this.callback = callback;
            this.params = params;
            this.controllerFactory = controllerFactory;
            this.publicApiId = publicApiId;
            this.upn = upn;
        }
    }

    /**
     * Immutable snapshot of a broker-path (OneAuth) parked request. Holds the controller to re-run the
     * acquire, the original request parameters, and the {@link ResultFuture} the blocked
     * {@code acquireToken} thread is waiting on. No persisted state; no PII on the referrer.
     */
    private static final class BrokerParkedEntry {
        final BrokerMsalController controller;
        final AndroidInteractiveTokenCommandParameters params;
        final ResultFuture<AcquireTokenResult> future;

        BrokerParkedEntry(final BrokerMsalController controller,
                          final AndroidInteractiveTokenCommandParameters params,
                          final ResultFuture<AcquireTokenResult> future) {
            this.controller = controller;
            this.params = params;
            this.future = future;
        }
    }
}
