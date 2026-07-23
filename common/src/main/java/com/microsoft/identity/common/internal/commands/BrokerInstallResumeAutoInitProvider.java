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
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.broker.BrokerValidator;
import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.logging.Logger;

import java.lang.ref.WeakReference;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * SDK-side auto-init that re-drives a persisted broker-install onboarding after a process restart —
 * <b>with no code in the calling app</b>. This is the "durability follow-up" trigger for the MAM
 * broker-install request-resume ([R1] in the Android design), owned by Common (shared by MSAL and OneAuth)
 * so that apps which will not (or cannot) add a relaunch hook still resume automatically.
 * <p>
 * <b>Why a ContentProvider.</b> Android instantiates a merged-manifest {@link ContentProvider} on every
 * process start, before {@code Application.onCreate} completes and without any app cooperation (the same
 * mechanism Firebase / WorkManager / androidx-startup use). Because this provider ships inside the Common
 * AAR, it is auto-declared in every consuming app. On {@link #onCreate} it registers a foreground observer;
 * on the first foreground of a fresh process it checks the durable park
 * ({@link BrokerInstallResumePendingStore}) and, if Company Portal is now installed and a re-driver is
 * ready, re-issues the interactive sign-in through the registered {@link IBrokerInstallResumeReDriver}.
 * <p>
 * <b>SDK glue registers the re-driver.</b> Common does not know how any particular SDK issues an
 * interactive request, so the host SDK calls {@link #registerReDriver(IBrokerInstallResumeReDriver)} once
 * at init. Until a re-driver is registered (or in apps that never resume), the provider is an inert no-op.
 * <p>
 * It only acts on a genuine restart: if an in-memory sink-wait is still live in this process
 * ({@link BrokerInstallResumeSinkWaiter#isActiveInThisProcess()}), the process survived the install and
 * that path resumes the request — the provider defers.
 * <p>
 * This provider stores nothing; the ContentProvider CRUD methods are inert.
 */
public final class BrokerInstallResumeAutoInitProvider extends ContentProvider {

    private static final String TAG = BrokerInstallResumeAutoInitProvider.class.getSimpleName();

    /** Default target used when the parked record carries no scopes/resource. */
    private static final String DEFAULT_TARGET = "https://graph.microsoft.com";

    /** How long to keep re-checking after a foreground while the SDK finishes configuring. */
    private static final int MAX_READINESS_RETRIES = 20;
    private static final long READINESS_RETRY_MS = 1000L;

    /**
     * The host SDK's re-driver. Registered once at SDK init; {@code null} until then (provider stays inert).
     * {@code volatile} because it is written from SDK init and read on the main thread by the readiness poll.
     */
    @Nullable
    private static volatile IBrokerInstallResumeReDriver sReDriver;

    /**
     * Registers the SDK-specific re-driver used to re-issue the interactive sign-in on resume. Idempotent;
     * the last registration wins. Call once at SDK initialization.
     *
     * @param reDriver the host SDK's re-driver implementation.
     */
    public static void registerReDriver(@NonNull final IBrokerInstallResumeReDriver reDriver) {
        sReDriver = reDriver;
        Logger.info(TAG + ":registerReDriver", "Broker-install resume re-driver registered.");
    }

    private final AtomicBoolean mResumeStarted = new AtomicBoolean(false);
    private final Handler mMainHandler = new Handler(Looper.getMainLooper());
    private final ExecutorService mDiscoveryExecutor = Executors.newSingleThreadExecutor();
    private WeakReference<Activity> mForegroundActivity = new WeakReference<>(null);

    @Override
    public boolean onCreate() {
        final Context context = getContext();
        if (context == null || !(context.getApplicationContext() instanceof Application)) {
            return true;
        }
        final Application application = (Application) context.getApplicationContext();
        application.registerActivityLifecycleCallbacks(new ForegroundObserver());
        Logger.info(TAG + ":onCreate", "Durable broker-install resume auto-init registered.");
        return true;
    }

    /**
     * On the first foreground of this process, if a durable broker-install park exists and this is a fresh
     * process (no live sink-wait), begin polling for readiness (Company Portal installed + SDK re-driver
     * ready) and then re-drive the sign-in.
     */
    private void onForegroundedWith(@NonNull final Activity activity) {
        mForegroundActivity = new WeakReference<>(activity);

        final Context appContext = activity.getApplicationContext();
        if (BrokerInstallResumeSinkWaiter.isActiveInThisProcess()) {
            // Process survived the install; the in-memory sink-wait will resume. Do not double-drive.
            return;
        }
        if (BrokerInstallResumePendingStore.peekResumeReady(appContext) == null) {
            return;
        }
        if (mResumeStarted.get()) {
            return;
        }
        Logger.info(TAG + ":onForegrounded",
                "Durable broker-install park found on a fresh process; waiting for readiness to resume.");
        scheduleReadinessCheck(appContext, 0);
    }

    private void scheduleReadinessCheck(@NonNull final Context appContext, final int attempt) {
        mMainHandler.postDelayed(() -> tryResume(appContext, attempt), READINESS_RETRY_MS);
    }

    private void tryResume(@NonNull final Context appContext, final int attempt) {
        if (mResumeStarted.get()) {
            return;
        }
        final BrokerInstallResumePendingStore.Record record =
                BrokerInstallResumePendingStore.peekResumeReady(appContext);
        if (record == null) {
            return;
        }
        if (!isCompanyPortalValidBroker(appContext)) {
            retryOrGiveUp(appContext, attempt, "Company Portal not a valid broker yet");
            return;
        }
        final IBrokerInstallResumeReDriver reDriver = sReDriver;
        if (reDriver == null || !reDriver.isReady()) {
            retryOrGiveUp(appContext, attempt, "SDK re-driver not ready yet");
            return;
        }
        final Activity activity = mForegroundActivity.get();
        if (activity == null || activity.isFinishing()) {
            retryOrGiveUp(appContext, attempt, "no foreground activity to host the resume");
            return;
        }
        if (!mResumeStarted.compareAndSet(false, true)) {
            return;
        }
        // Consume the record up-front so a later foreground/retry can never re-drive it again.
        BrokerInstallResumePendingStore.clear(appContext);
        refreshDiscoveryThenDrive(appContext, reDriver, record);
    }

    /**
     * Force-fresh broker discovery (off the main thread), then re-drive the interactive sign-in back on the
     * main thread. The refresh is essential: when the broker-install detour began, discovery cached a
     * "no broker / use AccountManager" result; without discarding it the resume would run <b>brokerless</b>
     * — skipping the device-registration (WPJ) step and failing token redemption (e.g. AADSTS7000218) — so
     * we re-discover so the just-installed Company Portal becomes the active broker before re-driving.
     */
    private void refreshDiscoveryThenDrive(@NonNull final Context appContext,
                                           @NonNull final IBrokerInstallResumeReDriver reDriver,
                                           @NonNull final BrokerInstallResumePendingStore.Record record) {
        mDiscoveryExecutor.execute(() -> {
            Logger.info(TAG + ":refreshDiscovery",
                    "Forcing fresh broker discovery before the durable resume so Company Portal is used.");
            BrokerInstallResumeSinkWaiter.forceRefreshBrokerDiscovery(appContext);
            mMainHandler.post(() -> {
                final Activity activity = mForegroundActivity.get();
                if (activity == null || activity.isFinishing()) {
                    Logger.warn(TAG + ":refreshDiscovery",
                            "No live foreground activity after discovery; cannot host the durable resume.");
                    return;
                }
                driveResume(reDriver, activity, record);
            });
        });
    }

    private void retryOrGiveUp(@NonNull final Context appContext, final int attempt, final String why) {
        if (attempt + 1 >= MAX_READINESS_RETRIES) {
            Logger.info(TAG + ":tryResume",
                    "Giving up durable resume after " + MAX_READINESS_RETRIES + " attempts (" + why + ").");
            return;
        }
        scheduleReadinessCheck(appContext, attempt + 1);
    }

    private void driveResume(@NonNull final IBrokerInstallResumeReDriver reDriver,
                             @NonNull final Activity activity,
                             @NonNull final BrokerInstallResumePendingStore.Record record) {
        try {
            final String target = (record.target != null && !record.target.isEmpty())
                    ? record.target
                    : DEFAULT_TARGET;
            final String hint = record.upn != null ? record.upn : "";

            Logger.info(TAG + ":driveResume",
                    "Re-driving broker-install onboarding from the SDK after restart (hintPresent="
                            + (!hint.isEmpty()) + ", target=" + target + ").");

            reDriver.reDriveInteractive(activity, hint, target);
        } catch (final Throwable t) {
            Logger.error(TAG + ":driveResume", "Failed to re-drive the durable broker-install resume.", t);
        }
    }

    private static boolean isCompanyPortalValidBroker(@NonNull final Context appContext) {
        try {
            appContext.getPackageManager().getPackageInfo(
                    AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME, 0);
        } catch (final PackageManager.NameNotFoundException e) {
            return false;
        }
        try {
            return new BrokerValidator(appContext).isValidBrokerPackage(
                    AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME);
        } catch (final Throwable t) {
            return false;
        }
    }

    /** Fires the readiness check on the first foreground of the process. */
    private final class ForegroundObserver implements Application.ActivityLifecycleCallbacks {
        private int mStartedActivities = 0;

        @Override
        public void onActivityStarted(@NonNull final Activity activity) {
            final boolean cameToForeground = (mStartedActivities == 0);
            mStartedActivities++;
            if (cameToForeground) {
                onForegroundedWith(activity);
            }
        }

        @Override
        public void onActivityResumed(@NonNull final Activity activity) {
            mForegroundActivity = new WeakReference<>(activity);
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
        public void onActivityPaused(@NonNull final Activity activity) {
        }

        @Override
        public void onActivitySaveInstanceState(@NonNull final Activity activity, @NonNull final Bundle outState) {
        }

        @Override
        public void onActivityDestroyed(@NonNull final Activity activity) {
        }
    }

    // ContentProvider CRUD is inert — this provider exists only for its auto-init onCreate.

    @Nullable
    @Override
    public Cursor query(@NonNull final Uri uri, @Nullable final String[] projection,
                        @Nullable final String selection, @Nullable final String[] selectionArgs,
                        @Nullable final String sortOrder) {
        return null;
    }

    @Nullable
    @Override
    public String getType(@NonNull final Uri uri) {
        return null;
    }

    @Nullable
    @Override
    public Uri insert(@NonNull final Uri uri, @Nullable final ContentValues values) {
        return null;
    }

    @Override
    public int delete(@NonNull final Uri uri, @Nullable final String selection,
                      @Nullable final String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(@NonNull final Uri uri, @Nullable final ContentValues values,
                      @Nullable final String selection, @Nullable final String[] selectionArgs) {
        return 0;
    }
}
