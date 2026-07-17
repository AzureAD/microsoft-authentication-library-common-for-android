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

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.logging.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Detects when the host app returns to the foreground and drives the MAM broker-install resume
 * foreground-fallback (§16 item 13). This is the trigger that lets a parked request resume when Company
 * Portal simply brings the calling app back (its existing redirect-back), or when the user manually swipes
 * back to the app after installing Company Portal — no {@code mam_resume=<cid>} redirect required.
 * <p>
 * Implemented with {@link Application.ActivityLifecycleCallbacks} (dependency-free) rather than
 * {@code ProcessLifecycleOwner}. Callbacks run on the main thread, so the started-activity counter needs no
 * synchronization. The transition from zero to one started activity is treated as "app foregrounded".
 * <p>
 * The work done on foreground is cheap and short-circuits early: {@link BrokerInstallResumeManager}
 * no-ops unless the flight is on <em>and</em> a request is currently parked, so registering this observer
 * unconditionally at SDK init is safe. Register once via {@link #install(Application)}.
 */
public final class BrokerInstallResumeForegroundObserver
        implements Application.ActivityLifecycleCallbacks {

    private static final String TAG = BrokerInstallResumeForegroundObserver.class.getSimpleName();

    private static final AtomicBoolean INSTALLED = new AtomicBoolean(false);

    private final Application mApplication;

    /** Number of started (visible) activities. Main-thread confined. */
    private int mStartedActivityCount = 0;

    private BrokerInstallResumeForegroundObserver(@NonNull final Application application) {
        mApplication = application;
    }

    /**
     * Registers the observer against the given application exactly once per process. Safe to call
     * repeatedly and from multiple entry points; subsequent calls are no-ops.
     *
     * @param application the host application (no-op if {@code null}).
     */
    public static void install(@Nullable final Application application) {
        if (application == null) {
            return;
        }
        if (INSTALLED.compareAndSet(false, true)) {
            application.registerActivityLifecycleCallbacks(
                    new BrokerInstallResumeForegroundObserver(application));
            Logger.info(TAG + ":install", "Registered broker-install resume foreground observer.");
        }
    }

    @Override
    @MainThread
    public void onActivityStarted(@NonNull final Activity activity) {
        if (mStartedActivityCount == 0) {
            // Zero -> one: the app just came to the foreground.
            try {
                BrokerInstallResumeManager.getInstance().onAppForegrounded(mApplication);
            } catch (final Throwable t) {
                // Never let a resume attempt destabilize the host app's activity lifecycle.
                Logger.warn(TAG + ":onActivityStarted",
                        "Foreground-fallback resume attempt failed (ignored).");
            }
        }
        mStartedActivityCount++;
    }

    @Override
    @MainThread
    public void onActivityStopped(@NonNull final Activity activity) {
        if (mStartedActivityCount > 0) {
            mStartedActivityCount--;
        }
    }

    // region unused lifecycle callbacks

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

    // endregion
}
