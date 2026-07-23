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
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
// SOFTWARE.
package com.microsoft.identity.common.internal.commands;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.components.AndroidPlatformComponentsFactory;
import com.microsoft.identity.common.internal.activebrokerdiscovery.BrokerDiscoveryClientFactory;
import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Keeps a broker-mediated interactive request alive across the "install Company Portal" detour (the
 * MAM-CA broker-install request-resume "sink-wait"; Feature AB#3676213 / AB#3686091).
 * <p>
 * <b>Why this is needed.</b> When interactive sign-in returns the {@code msauth://wpj?app_link=...}
 * "broker needs to be installed" result, the SDK only retries through the broker if the broker is eligible
 * <em>at that instant</em>. Company Portal is not installed yet, so the request terminates and the calling
 * app falls back to its add-account screen. By the time the user installs Company Portal and returns, the
 * request is long gone.
 * <p>
 * <b>How the wait works.</b> The SDK glue holds its request sink (does not deliver the broker-install error
 * yet), which keeps the whole request alive. This waiter watches for the app returning to the foreground;
 * once Company Portal is a valid broker it forces a fresh broker discovery (so the cached "no broker"
 * result is discarded) and calls {@link IBrokerInstallResumeSink#onBrokerAvailable()}, which re-delivers a
 * {@code RequiredBrokerMissing} result to the held sink — that drives the <em>existing</em> broker-install
 * retry, which now finds Company Portal and completes the request. If Company Portal never appears within
 * the TTL, {@link IBrokerInstallResumeSink#onGiveUp()} delivers the original terminal error so the request
 * never hangs.
 * <p>
 * Everything is single-shot (exactly one of {@code onBrokerAvailable} / {@code onGiveUp} runs) and the
 * observer/executor are torn down on completion. The caller is responsible for the feature flight gate.
 */
public final class BrokerInstallResumeSinkWaiter {

    private static final String TAG = BrokerInstallResumeSinkWaiter.class.getSimpleName();

    /**
     * Park time-to-live. Covers a realistic Company Portal download + install + first-launch on slow
     * networks while staying bounded. Mirrors the common-side registry default (7 minutes).
     */
    public static final long DEFAULT_TTL_MILLISECONDS = 7L * 60L * 1000L;

    /**
     * PoC knob (see the user request): on some dev boxes the Company Portal install is very slow — slower
     * than the 7-minute production TTL — so the sink-wait would give up and deliver the terminal error
     * before the user returns from installing CP. When {@code true} the effective TTL is stretched to
     * {@link #POC_TTL_MILLISECONDS} so a slow install has room to finish and the foreground resume fires.
     * Production keeps the bounded {@link #DEFAULT_TTL_MILLISECONDS}.
     */
    public static final boolean EXTEND_TTL_FOR_POC = true;

    /** Long backstop TTL used when {@link #EXTEND_TTL_FOR_POC} is set (still bounded so it never hangs forever). */
    public static final long POC_TTL_MILLISECONDS = 24L * 60L * 60L * 1000L;

    private final Application mApplication;
    private final Runnable mOnBrokerAvailable;
    private final Runnable mOnGiveUp;
    private final long mTtlMillis;
    private final IBrokerAvailabilityChecker mBrokerChecker;

    private final AtomicBoolean mResolved = new AtomicBoolean(false);
    private final AtomicBoolean mCheckInProgress = new AtomicBoolean(false);
    private final ScheduledExecutorService mExecutor = Executors.newSingleThreadScheduledExecutor();
    private Application.ActivityLifecycleCallbacks mLifecycleCallbacks;

    /**
     * True while an in-memory sink-wait is live in <b>this</b> process. Lets the durable-resume auto-init
     * ({@link BrokerInstallResumeAutoInitProvider}) tell "the request is still alive here, the sink-wait
     * will resume it" (process survived) from "this is a fresh process, drive the durable resume" (the MAM
     * restart case). Only the latter should be re-driven from persistence.
     */
    private static final AtomicBoolean sSinkWaitActiveInThisProcess = new AtomicBoolean(false);

    /** @return {@code true} if an in-memory sink-wait is currently handling a request in this process. */
    public static boolean isActiveInThisProcess() {
        return sSinkWaitActiveInThisProcess.get();
    }

    /**
     * Force-fresh broker discovery so a stale "no broker / use AccountManager" result — cached when the
     * broker-install detour began, before Company Portal was installed — is discarded and the
     * just-installed Company Portal becomes the active broker. Uses the shared client-SDK
     * {@code BrokerDiscoveryClient} (the same instance the SDK's {@code BrokerClient} reads); on
     * {@code shouldSkipCache=true} it re-discovers and writes back the freshly discovered broker, so a
     * subsequent interactive request routes through the broker instead of running brokerless.
     * <p>
     * Blocking IPC — call off the main thread. Best-effort: a failure is non-fatal (the native retry runs
     * its own discovery).
     *
     * @param appContext any Android context.
     */
    public static void forceRefreshBrokerDiscovery(@NonNull final Context appContext) {
        try {
            final IPlatformComponents components =
                    AndroidPlatformComponentsFactory.createFromContext(appContext);
            final Object elected = BrokerDiscoveryClientFactory.getInstanceForClientSdk(appContext, components)
                    .getActiveBroker(true /* shouldSkipCache */);
            Logger.info(TAG + ":forceRefreshBrokerDiscovery",
                    "Force-fresh discovery elected active broker: " + (elected != null ? elected : "<none>"));
        } catch (final Throwable t) {
            Logger.warn(TAG + ":forceRefreshBrokerDiscovery",
                    "Best-effort fresh broker discovery failed; native retry will re-discover.");
        }
    }

    /**
     * Abstracts the "is Company Portal a valid broker now?" and "force fresh discovery" steps so the
     * orchestration can be unit-tested without Android framework dependencies.
     */
    public interface IBrokerAvailabilityChecker {
        /** @return {@code true} if Company Portal is installed and is a validly-signed broker. */
        boolean isCompanyPortalValidBroker();

        /** Force-fresh broker discovery so a stale "no broker" cache is discarded before the retry. */
        void refreshBrokerDiscovery();
    }

    private BrokerInstallResumeSinkWaiter(@NonNull final Application application,
                                         @NonNull final Runnable onBrokerAvailable,
                                         @NonNull final Runnable onGiveUp,
                                         final long ttlMillis,
                                         @NonNull final IBrokerAvailabilityChecker brokerChecker) {
        mApplication = application;
        mOnBrokerAvailable = onBrokerAvailable;
        mOnGiveUp = onGiveUp;
        mTtlMillis = ttlMillis;
        mBrokerChecker = brokerChecker;
    }

    /**
     * Starts a broker-install sink-wait. Returns {@code true} if the wait was started (the caller must
     * then NOT deliver the broker-install error — the sink is held for deferred delivery). Returns
     * {@code false} if a wait could not be started (no {@link Application} available), in which case the
     * caller should proceed with its normal terminal delivery.
     *
     * @param context   any Android context (its application context must be an {@link Application}).
     * @param sink      the terminal actions to run on resolution (re-deliver on broker-available, or
     *                  deliver the original error on give-up).
     * @param ttlMillis the wait time-to-live in millis.
     * @return {@code true} if the wait was started; {@code false} otherwise.
     */
    public static boolean startWaiting(@NonNull final Context context,
                                       @NonNull final IBrokerInstallResumeSink sink,
                                       final long ttlMillis) {
        return startWaiting(context, sink::onBrokerAvailable, sink::onGiveUp, ttlMillis);
    }

    private static boolean startWaiting(@NonNull final Context context,
                                        @NonNull final Runnable onBrokerAvailable,
                                        @NonNull final Runnable onGiveUp,
                                        final long ttlMillis) {
        final Context appContext = context.getApplicationContext();
        if (!(appContext instanceof Application)) {
            Logger.warn(TAG + ":startWaiting",
                    "Application context is not an Application; cannot register a foreground observer.");
            return false;
        }
        return new BrokerInstallResumeSinkWaiter(
                (Application) appContext,
                onBrokerAvailable,
                onGiveUp,
                ttlMillis,
                new DefaultBrokerAvailabilityChecker(appContext)).start();
    }

    @VisibleForTesting
    static BrokerInstallResumeSinkWaiter createForTesting(@NonNull final Application application,
                                                          @NonNull final Runnable onBrokerAvailable,
                                                          @NonNull final Runnable onGiveUp,
                                                          final long ttlMillis,
                                                          @NonNull final IBrokerAvailabilityChecker checker) {
        return new BrokerInstallResumeSinkWaiter(application, onBrokerAvailable, onGiveUp, ttlMillis, checker);
    }

    @VisibleForTesting
    boolean start() {
        mLifecycleCallbacks = new ForegroundCallbacks();
        mApplication.registerActivityLifecycleCallbacks(mLifecycleCallbacks);
        final long effectiveTtl = EXTEND_TTL_FOR_POC ? Math.max(mTtlMillis, POC_TTL_MILLISECONDS) : mTtlMillis;
        mExecutor.schedule(this::giveUp, effectiveTtl, TimeUnit.MILLISECONDS);
        sSinkWaitActiveInThisProcess.set(true);
        Logger.info(TAG + ":start", "Started broker-install sink-wait (ttlMs=" + effectiveTtl + ").");
        return true;
    }

    /** Invoked (off the main thread) whenever the app returns to the foreground. */
    @VisibleForTesting
    void onForeground() {
        if (mResolved.get() || !mCheckInProgress.compareAndSet(false, true)) {
            return;
        }
        try {
            if (!mBrokerChecker.isCompanyPortalValidBroker()) {
                // Company Portal is not (yet) installed/valid; keep waiting.
                return;
            }
            mBrokerChecker.refreshBrokerDiscovery();
            if (mResolved.compareAndSet(false, true)) {
                cleanup();
                Logger.info(TAG + ":onForeground",
                        "Company Portal is now a valid broker; re-delivering to drive the broker retry.");
                safeRun(mOnBrokerAvailable);
            }
        } catch (final Throwable t) {
            Logger.warn(TAG + ":onForeground", "Broker availability check failed; will keep waiting.");
        } finally {
            mCheckInProgress.set(false);
        }
    }

    @VisibleForTesting
    void giveUp() {
        if (mResolved.compareAndSet(false, true)) {
            cleanup();
            Logger.info(TAG + ":giveUp",
                    "Broker-install sink-wait expired without Company Portal; delivering the original error.");
            safeRun(mOnGiveUp);
        }
    }

    private void cleanup() {
        sSinkWaitActiveInThisProcess.set(false);
        try {
            if (mLifecycleCallbacks != null) {
                mApplication.unregisterActivityLifecycleCallbacks(mLifecycleCallbacks);
                mLifecycleCallbacks = null;
            }
        } catch (final Throwable ignored) {
            // best-effort
        }
        mExecutor.shutdown();
    }

    private static void safeRun(@NonNull final Runnable action) {
        try {
            action.run();
        } catch (final Throwable t) {
            Logger.error(TAG + ":safeRun", "Sink-wait delivery action threw.", t);
        }
    }

    /**
     * Registers foreground transitions. The zero-to-one started-activity transition means the app came to
     * the foreground; the check + delivery run on the waiter's executor (off the main thread) because the
     * broker discovery refresh performs blocking IPC.
     */
    private final class ForegroundCallbacks implements Application.ActivityLifecycleCallbacks {
        private int mStartedActivities = 0;

        @Override
        public void onActivityStarted(@NonNull final Activity activity) {
            final boolean cameToForeground = (mStartedActivities == 0);
            mStartedActivities++;
            if (cameToForeground && !mResolved.get()) {
                try {
                    mExecutor.execute(BrokerInstallResumeSinkWaiter.this::onForeground);
                } catch (final Throwable ignored) {
                    // executor may be shut down if we resolved concurrently; ignore.
                }
            }
        }

        @Override
        public void onActivityStopped(@NonNull final Activity activity) {
            if (mStartedActivities > 0) {
                mStartedActivities--;
            }
        }

        @Override
        public void onActivityCreated(@NonNull final Activity activity, @Nullable final Bundle savedInstanceState) {
        }

        @Override
        public void onActivityResumed(@NonNull final Activity activity) {
        }

        @Override
        public void onActivityPaused(@NonNull final Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull final Activity activity, @NonNull final Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull final Activity activity) {
        }
    }

    /**
     * Production checker: Company Portal validity via {@link BrokerValidator} (a fresh PackageManager
     * signature check) and fresh discovery via the shared client-SDK {@code BrokerDiscoveryClient} (the
     * same instance the SDK's {@code BrokerClient} reads), which on {@code shouldSkipCache=true} discards
     * the cached "no broker / use AccountManager" state and writes back the freshly discovered broker.
     */
    private static final class DefaultBrokerAvailabilityChecker implements IBrokerAvailabilityChecker {
        private final Context mAppContext;

        DefaultBrokerAvailabilityChecker(@NonNull final Context appContext) {
            mAppContext = appContext;
        }

        @Override
        public boolean isCompanyPortalValidBroker() {
            return new BrokerValidator(mAppContext).isValidBrokerPackage(
                    AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        }

        @Override
        public void refreshBrokerDiscovery() {
            forceRefreshBrokerDiscovery(mAppContext);
        }
    }
}
