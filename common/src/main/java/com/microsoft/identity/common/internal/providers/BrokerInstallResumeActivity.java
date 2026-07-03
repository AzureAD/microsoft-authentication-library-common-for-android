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
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import java.util.List;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.logging.Logger;

/**
 * Common-provided deep-link receiver for broker-install request resume.
 *
 * <p>After the broker (Company Portal) is installed, its first launch deep-links back to
 * {@code msauth://<originPkg>/resume?resume=<cid>}. Because the deep-link host equals the origin
 * app's package (which equals the manifest placeholder {@code ${applicationId}}), this receiver
 * and its intent-filter are declared once in the shared common manifest and auto-merge into every
 * consuming app — so consumers (MSAL test app, OneAuth's 1P apps) register no receiver and no
 * intent-filter of their own. This is what keeps the 1P app change at zero.
 *
 * <p>On receiving the deep-link, this activity hands the single-use resume id to
 * {@link BrokerInstallResumeCoordinator}, which — <em>in the original app process</em> — re-dispatches
 * the parked interactive request through the broker and delivers the token to the app's original
 * {@code acquireToken} callback. This activity stays in the foreground so the broker interactive UI
 * can be launched from it (Background-Activity-Launch safe); the coordinator finishes it once the
 * resumed request reaches a terminal result.
 */
public class BrokerInstallResumeActivity extends Activity {

    private static final String TAG = BrokerInstallResumeActivity.class.getSimpleName();

    /** E2E-only logcat tag mirroring key milestones; safe to strip for production. */
    private static final String POC_TAG = "ResumePOC";

    /** Deep-link query parameter that carries the resume correlation id. */
    private static final String QUERY_PARAM_RESUME = "resume";

    /** True only between {@link #onResume()} and {@link #onPause()} — i.e. actually on-screen. */
    private boolean mForeground;

    /** Set once the resumed request reaches a terminal result and we still owe the user a landing. */
    private boolean mReturnPending;

    /** Guards {@link #returnToOriginApp()} so we land the user in the app at most once. */
    private boolean mReturned;

    @Override
    protected void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final Uri data = getIntent() != null ? getIntent().getData() : null;
        final String resumeId = data == null ? null : data.getQueryParameter(QUERY_PARAM_RESUME);

        if (resumeId == null) {
            Logger.warn(TAG, "Resume deep-link missing correlation id; nothing to resume.");
            finish();
            return;
        }

        Log.i(POC_TAG, "RESUME-DEEPLINK received; resumeId=" + resumeId);
        Logger.info(TAG, "Received broker-install resume deep-link; dispatching resume.");
        BrokerInstallResumeCoordinator.showStep(this,
                "Broker-install resume \u2461/\u2463: Company Portal installed \u2192 resuming request");

        // Resume off the main thread: the coordinator clears the broker-discovery cache and submits
        // the interactive command (both may block briefly). Keep this activity alive so the broker
        // interactive UI can be launched from it; the coordinator finishes it on a terminal result.
        new Thread(new Runnable() {
            @Override
            public void run() {
                final boolean dispatched =
                        BrokerInstallResumeCoordinator.INSTANCE.resume(BrokerInstallResumeActivity.this, resumeId);
                if (!dispatched) {
                    // Nothing parked (e.g. process died during install). Nothing to keep alive for.
                    finish();
                }
            }
        }, "broker-install-resume").start();
    }

    @Override
    protected void onResume() {
        super.onResume();
        mForeground = true;
        // If the resumed request already finished while we were behind the broker / eSTS Custom Tab,
        // land the user in the app now. Because we are guaranteed to be foreground here, this launch
        // is not subject to Android's Background-Activity-Launch restrictions.
        if (mReturnPending) {
            returnToOriginApp();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        mForeground = false;
    }

    /**
     * Invoked by {@link BrokerInstallResumeCoordinator} on any terminal result (success, error, or
     * cancel) <em>after</em> the token has been delivered to the app's original callback. Lands the
     * user back in the origin app so they see the signed-in result rather than the leftover broker /
     * eSTS Custom Tab.
     *
     * <p>The actual foregrounding is deferred to {@link #onResume()} whenever we are not currently
     * on-screen (e.g. the eSTS Custom Tab is still on top). This guarantees the app is brought
     * forward from a foreground context, so it can never be silently dropped by Background-Activity-
     * Launch limits on Android 12+.
     */
    void onResumedRequestTerminated() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mReturnPending = true;
                if (mForeground) {
                    returnToOriginApp();
                }
            }
        });
    }

    private void returnToOriginApp() {
        if (mReturned) {
            return;
        }
        mReturned = true;
        mReturnPending = false;
        // The token was delivered to the app's original callback in the original process, which
        // renders the result in the app's own UI (e.g. MainActivity). Surface that UI:
        //  1) Prefer moving the app's *content* task to the front — the task whose top activity is
        //     neither this ephemeral resume activity nor the idle launcher/start screen. This shows
        //     the activity that actually received the token (never the launcher).
        //  2) If no distinct content task exists, the deep-link put us in the app's own task on top
        //     of the caller, so simply finishing this activity reveals that caller beneath us.
        try {
            if (!bringOriginContentTaskToFront()) {
                Log.i(POC_TAG, "RESUME-FOREGROUND no distinct content task; finishing to reveal caller");
            }
        } catch (final Exception e) {
            Logger.warn(TAG, "Failed to bring origin app to foreground after resume: " + e.getMessage());
        } finally {
            finish();
        }
    }

    /**
     * Brings the origin app's content task (the one showing the activity that received the token) to
     * the foreground, skipping this resume activity's own task and the idle launcher/start screen.
     * Operates only on the app's own tasks via {@link ActivityManager#getAppTasks()}, so it needs no
     * special permission. Returns {@code true} if a suitable task was found and moved to the front.
     */
    private boolean bringOriginContentTaskToFront() {
        final ActivityManager am = (ActivityManager) getSystemService(ACTIVITY_SERVICE);
        if (am == null) {
            return false;
        }

        final String selfClass = getClass().getName();
        String launcherClass = null;
        final Intent launcher = getPackageManager().getLaunchIntentForPackage(getPackageName());
        if (launcher != null && launcher.getComponent() != null) {
            launcherClass = launcher.getComponent().getClassName();
        }

        final List<ActivityManager.AppTask> tasks = am.getAppTasks();
        if (tasks == null) {
            return false;
        }

        for (final ActivityManager.AppTask task : tasks) {
            final ActivityManager.RecentTaskInfo info = task.getTaskInfo();
            if (info == null) {
                continue;
            }
            final ComponentName top = info.topActivity != null ? info.topActivity : info.baseActivity;
            if (top == null) {
                continue;
            }
            final String cls = top.getClassName();
            if (cls.equals(selfClass) || cls.equals(launcherClass)) {
                // Our own resume task or the idle launcher — not the UI that received the token.
                continue;
            }
            task.moveToFront();
            Log.i(POC_TAG, "RESUME-FOREGROUND origin content task moved to front; top=" + cls);
            return true;
        }
        return false;
    }
}
