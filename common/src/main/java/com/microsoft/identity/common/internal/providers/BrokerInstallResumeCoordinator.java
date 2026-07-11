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
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.pm.PackageInfoCompat;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.cache.ClientActiveBrokerCache;
import com.microsoft.identity.common.internal.cache.IClientActiveBrokerCache;
import com.microsoft.identity.common.internal.commands.parameters.AndroidInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.interfaces.IStorageSupplier;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.BrokerInstallLinkValidator;
import com.microsoft.identity.common.java.providers.BrokerInstallReferrerBuilder;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.TimeoutException;

/**
 * In-memory coordinator for broker-install request resume, owned entirely by {@code common} and
 * driven by OneAuth's {@code BrokerClient}.
 *
 * <p><strong>Park</strong> (pre-install): when an interactive OneAuth request is blocked by a
 * Conditional-Access policy that requires installing the broker (Company Portal),
 * {@code BrokerClient} calls {@link #installParkAndAwait} <em>before</em> any broker controller
 * exists. That launches the Company Portal install and blocks the calling {@code acquireToken}
 * thread on an in-memory {@link ResultFuture}, keeping the request's original djinni sink pending.
 * No token or PII leaves the device; nothing is persisted.
 *
 * <p><strong>Resume</strong> (post-install): after the broker is installed and Company Portal
 * redirects back to {@code msauth://<pkg>/resume?resume=<id>}, {@link BrokerInstallResumeActivity}
 * (running in the <em>original app process</em>) calls {@link #resume(Activity, String)}. This
 * re-runs the acquire in broker context via the caller-supplied {@link IBrokerInstallResumeRetry}
 * — which now routes to the freshly installed broker — and completes the parked future, unblocking
 * the original thread so OneAuth delivers the token on its original sink with no app-side resume code.
 *
 * <p>Because state is in-memory and process-scoped, a process death during the Play Store install
 * loses the parked request (an accepted trade-off for this design).
 */
public final class BrokerInstallResumeCoordinator {

    private static final String TAG = BrokerInstallResumeCoordinator.class.getSimpleName();

    /** E2E-only logcat tag mirroring key milestones; safe to strip for production. */
    private static final String POC_TAG = "ResumePOC";

    public static final BrokerInstallResumeCoordinator INSTANCE = new BrokerInstallResumeCoordinator();

    /**
     * Broker-path (OneAuth) parked requests, keyed by correlation id. OneAuth drives its own djinni
     * {@code BrokerEventSink} through a <em>blocking</em> {@code BrokerClient.getTokenInteractivelyInternal}
     * call on a background thread. Parking that request therefore means holding a {@link ResultFuture}
     * the blocked thread is waiting on; resume invokes the caller's {@link IBrokerInstallResumeRetry}
     * (fresh broker discovery + re-acquire) and completes the future, which returns the token on the
     * original OneAuth sink.
     */
    private final ConcurrentMap<String, BrokerParkedEntry> mBrokerParked = new ConcurrentHashMap<>();

    /**
     * Upper bound on how long the parked {@code acquireToken} thread blocks awaiting resume. Sized to
     * cover a user-paced Play Store install + Company Portal round-trip, but deliberately short so that
     * when Company Portal does not fire the post-install resume deep link (e.g. a CP build that has not
     * implemented the redirect), the request fails back to the caller's normal terminal broker-install
     * behavior in a few minutes instead of holding the thread/sink for a long time. On timeout the user
     * can simply retry: Company Portal is installed by then, so a fresh request takes the normal broker
     * path and succeeds.
     */
    private static final long INSTALL_RESUME_TIMEOUT_MINUTES = 3;

    /**
     * Minimum Company Portal {@code versionCode} that implements the post-install resume redirect.
     * When the app returns to the foreground after the Play Store install, the parked request checks
     * the installed CP against this floor: below it we fail the request fast (that CP build will never
     * fire the resume deep link, so waiting out the park timeout is pointless); at/above it we do
     * nothing and let CP's own redirect drive the resume, keeping a single consistent resume path.
     *
     * <p>TODO: set this to the first Company Portal {@code versionCode} that ships the redirect once
     * that build is released. Left at {@code 0} for now so any installed CP is treated as supported
     * (the version gate never fails fast until the real floor is known).
     */
    private static final long MIN_CP_VERSION_SUPPORTING_RESUME = 0L;

    /** Outcome of inspecting the installed Company Portal for resume support on foreground. */
    private enum CpSupport { ABSENT, UNSUPPORTED, SUPPORTED }

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
     * <p>This is driven from OneAuth's {@code BrokerClient} on the broker path — where real 1P
     * (OneAuth) apps actually receive the Conditional-Access install challenge. Because the broker
     * (Company Portal) is not installed yet, no broker controller
     * exists, so {@code BrokerClient} parks here <em>before</em> any controller call and supplies a
     * {@link IBrokerInstallResumeRetry} to rebind + re-acquire once it is. {@code BrokerClient} calls
     * {@code getTokenInteractivelyInternal} synchronously on a background thread and delivers whatever
     * it returns on its djinni {@code BrokerEventSink}; so we "keep the sink pending" simply by
     * blocking that thread on a {@link ResultFuture} until resume.
     *
     * <p>Nothing is persisted (in-memory only, by design): a process death during the Play Store
     * install loses the parked request and the caller falls back to today's blocked-install behavior.
     *
     * @param retry       rebuilds a broker-bound controller (fresh, cache-skipping discovery now that
     *                    Company Portal is installed) and re-acquires the token; see
     *                    {@link IBrokerInstallResumeRetry}. Supplied by the caller because only it can
     *                    rebind the broker — the original request had no broker controller.
     * @param appContext  application context used to launch the Play Store and read the origin package.
     * @param parameters  the in-flight interactive request (carries correlation id, login hint/UPN,
     *                    and redirect uri). Must be an Android interactive request.
     * @param installUrl  the broker-install (Play Store) URL captured from the CA challenge.
     * @return the token result once resumed in broker context, or {@code null} if the request could
     * not be parked or resolved without a broker acquire (unsafe link, missing correlation id, park
     * timeout, or interruption) — in which case the caller should fall through to today's terminal
     * broker-installation behavior.
     * @throws Exception if the resumed broker acquire itself failed; the original cause is rethrown
     * (a service/CA error, broker-side failure, cancellation, or a rebind failure from the retry
     * callback) so the caller surfaces the real error instead of a generic "no broker" result.
     */
    @Nullable
    public AcquireTokenResult installParkAndAwait(
            @NonNull final IBrokerInstallResumeRetry retry,
            @NonNull final Context appContext,
            @NonNull final AndroidInteractiveTokenCommandParameters parameters,
            @NonNull final String installUrl) throws Exception {
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
        mBrokerParked.put(resumeId, new BrokerParkedEntry(retry, parameters, future));

        // Watch for the user returning to the app after the Play Store. If Company Portal is installed
        // but too old to ever fire the post-install resume deep link, fail the request fast on the next
        // foreground instead of blocking until the park timeout. A supported CP is left to drive its own
        // redirect (BrokerInstallResumeActivity -> resume), keeping a single consistent resume path.
        final Application application = asApplication(appContext);
        final ForegroundCpSupportWatcher foregroundWatcher =
                (application == null) ? null : new ForegroundCpSupportWatcher(application, resumeId);
        if (foregroundWatcher != null) {
            application.registerActivityLifecycleCallbacks(foregroundWatcher);
        }

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
            // The resumed broker acquire ran but failed with a real error (a service/CA error from
            // eSTS, a broker-side failure, user cancellation, or a rebind failure from the retry
            // callback). Surface the real cause to the caller, whose catch blocks map BaseException/
            // service errors correctly, rather than collapsing it into a generic "no broker" terminal
            // error that hides what actually went wrong on the resume.
            final Throwable cause = e.getCause();
            Logger.warn(methodTag, "Broker-install resume failed; surfacing the real error to the caller.");
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw e;
        } finally {
            mBrokerParked.remove(resumeId);
            if (application != null && foregroundWatcher != null) {
                application.unregisterActivityLifecycleCallbacks(foregroundWatcher);
            }
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

    /** Returns the process {@link Application} for lifecycle registration, or null if unavailable. */
    @Nullable
    private static Application asApplication(@NonNull final Context context) {
        final Context appContext = context.getApplicationContext();
        return (appContext instanceof Application) ? (Application) appContext : null;
    }

    /** Reads the installed Company Portal and classifies it against {@link #MIN_CP_VERSION_SUPPORTING_RESUME}. */
    private static CpSupport checkCompanyPortalSupport(@NonNull final Context context) {
        try {
            final PackageInfo info = context.getPackageManager().getPackageInfo(
                    AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, 0);
            final long versionCode = PackageInfoCompat.getLongVersionCode(info);
            return (versionCode < MIN_CP_VERSION_SUPPORTING_RESUME)
                    ? CpSupport.UNSUPPORTED : CpSupport.SUPPORTED;
        } catch (final PackageManager.NameNotFoundException e) {
            return CpSupport.ABSENT;
        }
    }

    /**
     * Fails a parked broker request fast (without waiting for the park timeout) when Company Portal is
     * installed but below the resume-support floor. Completes the blocked {@code acquireToken} thread
     * exceptionally with the terminal "no valid broker" error so the caller surfaces it immediately.
     */
    private void failFastBrokerParked(@NonNull final String resumeId) {
        final BrokerParkedEntry entry = mBrokerParked.remove(resumeId);
        if (entry != null) {
            entry.future.setException(new ClientException(
                    ClientException.NOT_VALID_BROKER_FOUND,
                    "Company Portal is installed but below the resume-support version; "
                            + "cannot auto-resume the broker-install request."));
        }
    }

    /**
     * One-shot {@link Application.ActivityLifecycleCallbacks} registered while a broker request is
     * parked, used <em>only</em> to fail the request fast when the installed Company Portal is too old
     * to ever fire the post-install resume deep link. The user returns to the app manually after the
     * Play Store, and app-foreground is the reliable signal for that return. On the next foreground:
     *
     * <ul>
     *   <li>CP absent -> install not finished (or user bailed); keep waiting, re-check next foreground.</li>
     *   <li>CP present but below {@link #MIN_CP_VERSION_SUPPORTING_RESUME} -> fail fast with the
     *       terminal broker error instead of blocking until the park timeout, since that CP build will
     *       never redirect.</li>
     *   <li>CP present and supported -> do nothing: a supported CP implements the redirect, so we let
     *       CP's deep link ({@link BrokerInstallResumeActivity} -> {@link #resume(Activity, String)})
     *       drive the resume. This keeps a single, consistent resume trigger across the MSAL and
     *       OneAuth flows and avoids a self-resume racing CP's redirect. If a supported CP somehow
     *       fails to redirect, the park timeout is the safety net (user retries -> CP is installed ->
     *       normal broker path succeeds).</li>
     * </ul>
     *
     * <p>Deduped against CP's redirect through {@code mBrokerParked.remove}: whichever path removes the
     * entry first wins, so a fail-fast and a late deep link cannot both drive the same request.
     */
    private static final class ForegroundCpSupportWatcher implements Application.ActivityLifecycleCallbacks {
        private final Application mApplication;
        private final String mResumeId;
        private final AtomicBoolean mHandled = new AtomicBoolean(false);

        ForegroundCpSupportWatcher(@NonNull final Application application, @NonNull final String resumeId) {
            mApplication = application;
            mResumeId = resumeId;
        }

        @Override
        public void onActivityResumed(@NonNull final Activity activity) {
            if (mHandled.get()) {
                return;
            }
            if (!INSTANCE.mBrokerParked.containsKey(mResumeId)) {
                // Resolved by another path (CP deep-link resume or park timeout); stop watching.
                unregister();
                return;
            }
            // Only act on a present-but-unsupported CP. ABSENT (install in progress) and SUPPORTED
            // (CP will fire its own redirect) both keep waiting.
            if (checkCompanyPortalSupport(activity) != CpSupport.UNSUPPORTED) {
                return;
            }
            if (!mHandled.compareAndSet(false, true)) {
                return;
            }
            unregister();
            Log.i(POC_TAG, "RESUME-FOREGROUND CP below resume-support floor; failing fast; resumeId=" + mResumeId);
            INSTANCE.failFastBrokerParked(mResumeId);
        }

        private void unregister() {
            mApplication.unregisterActivityLifecycleCallbacks(this);
        }

        @Override
        public void onActivityCreated(@NonNull final Activity activity, @Nullable final Bundle savedInstanceState) { }

        @Override
        public void onActivityStarted(@NonNull final Activity activity) { }

        @Override
        public void onActivityPaused(@NonNull final Activity activity) { }

        @Override
        public void onActivityStopped(@NonNull final Activity activity) { }

        @Override
        public void onActivitySaveInstanceState(@NonNull final Activity activity, @NonNull final Bundle outState) { }

        @Override
        public void onActivityDestroyed(@NonNull final Activity activity) { }
    }

    /**
     * Resumes a parked broker-path (OneAuth) request in broker context and delivers the token on the
     * original djinni sink by completing the parked future (see {@link #resumeBrokerParked}).
     *
     * @param activity the (foreground) deep-link activity used to launch the broker UI (BAL-safe).
     * @param resumeId the resume key carried back on the Company Portal redirect.
     * @return true if a parked request was found and resume was dispatched; false otherwise.
     */
    public boolean resume(@NonNull final Activity activity, @NonNull final String resumeId) {
        // Complete the blocked OneAuth acquireToken thread by re-running the acquire in broker context
        // and delivering the result on its future. Remove (not peek) so a duplicate resume deep-link
        // cannot re-launch the broker UI twice.
        final BrokerParkedEntry brokerEntry = mBrokerParked.remove(resumeId);
        if (brokerEntry != null) {
            return resumeBrokerParked(activity, resumeId, brokerEntry);
        }

        Log.w(POC_TAG, "RESUME-NO-PARKED no parked request for resumeId=" + resumeId);
        Logger.warn(TAG, "No parked request found to resume (expired process or unknown id).");
        return false;
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
            // The caller re-discovers the freshly installed broker (cache-skipping) and rebuilds a
            // broker-bound controller before acquiring — the pre-install request had none.
            final AcquireTokenResult result = entry.retry.retryInBrokerContext(resumeParams);
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

    /**
     * Immutable snapshot of a broker-path (OneAuth) parked request. Holds the retry callback that
     * rebuilds a broker-bound controller and re-acquires the token, the original request parameters,
     * and the {@link ResultFuture} the blocked {@code acquireToken} thread is waiting on. No persisted
     * state; no PII on the referrer.
     */
    private static final class BrokerParkedEntry {
        final IBrokerInstallResumeRetry retry;
        final AndroidInteractiveTokenCommandParameters params;
        final ResultFuture<AcquireTokenResult> future;

        BrokerParkedEntry(final IBrokerInstallResumeRetry retry,
                          final AndroidInteractiveTokenCommandParameters params,
                          final ResultFuture<AcquireTokenResult> future) {
            this.retry = retry;
            this.params = params;
            this.future = future;
        }
    }
}
