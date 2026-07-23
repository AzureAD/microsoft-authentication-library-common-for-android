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

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.logging.Logger;

/**
 * Durable, on-disk record of a broker-install request that still needs to be resumed — the persistent
 * half of the MAM broker-install request-resume "durability follow-up" ([R1] in the Android design).
 * <p>
 * <b>Why this exists.</b> {@link BrokerInstallResumeSinkWaiter} keeps a request alive <em>in memory</em>
 * across the Company Portal install. That is sufficient when the calling app's process survives the
 * install, but a MAM-managed 1P app (e.g. Outlook) is <b>killed and restarted</b> by the Intune MAM SDK
 * the moment Company Portal installs (so the MAM SDK can dynamically load Company Portal's classes). A
 * process kill destroys the in-memory sink-wait, so nothing resumes. This store persists just enough for
 * the SDK to re-drive the sign-in after the process is recreated.
 * <p>
 * <b>Written entirely by the SDK — no 1P app code.</b> The SDK glue calls {@link #markBrokerInstallPending}
 * when it sees the broker-install result, stamping the request's UPN, target/scopes, and {@code app_link}.
 * On the next process start, {@link BrokerInstallResumeAutoInitProvider} (a manifest-merged auto-init)
 * reads {@link #peekResumeReady} and re-issues the sign-in through the registered
 * {@link IBrokerInstallResumeReDriver}. Neither write nor re-drive requires any code in the calling app —
 * the durable resume works for apps that do not (or cannot) add a relaunch hook themselves.
 * <p>
 * The file is a dedicated {@link SharedPreferences} store (NOT the default preferences, which some hosts
 * clear on launch) so it survives a process restart. In production the record carries a UPN and should be
 * encrypted at rest (reuse the token cache's key management); this PoC keeps it in plain preferences.
 */
public final class BrokerInstallResumePendingStore {

    private static final String TAG = BrokerInstallResumePendingStore.class.getSimpleName();

    /** Dedicated preferences file so it is not wiped by a default-preferences clear on relaunch. */
    private static final String PREFS_FILE = "mam_broker_install_resume_pending";

    private static final String KEY_PHASE = "phase";
    private static final String KEY_UPN = "upn";
    private static final String KEY_TARGET = "target";
    private static final String KEY_APP_LINK = "app_link";
    private static final String KEY_EXPIRES_AT_MS = "expires_at_ms";

    /** No pending resume. */
    public static final int PHASE_NONE = 0;
    /** The SDK saw a broker-install result and persisted a request to resume after a possible restart. */
    public static final int PHASE_BROKER_INSTALL_PENDING = 2;

    /**
     * PoC knob (see the user request): the Company Portal install can be very slow on some dev boxes, so
     * the durable park would otherwise expire before the resume runs. When {@code true} the persisted TTL
     * is <b>ignored</b> so a pending resume never expires. Production should keep a bounded TTL.
     */
    public static final boolean DISABLE_EXPIRY_FOR_POC = false;

    /**
     * Default durable TTL (long, so even without {@link #DISABLE_EXPIRY_FOR_POC} a slow CP install fits).
     * The in-memory sink-wait keeps its own shorter 7-minute TTL; this durable window is separate because
     * it must also cover the process being dead for a while.
     */
    public static final long DEFAULT_TTL_MILLISECONDS = 24L * 60L * 60L * 1000L;

    private BrokerInstallResumePendingStore() {
    }

    /** The persisted record needed to re-drive a sign-in after a process restart. */
    public static final class Record {
        @Nullable public final String upn;
        @Nullable public final String target;
        @Nullable public final String appLink;

        Record(@Nullable final String upn, @Nullable final String target, @Nullable final String appLink) {
            this.upn = upn;
            this.target = target;
            this.appLink = appLink;
        }
    }

    private static SharedPreferences prefs(@NonNull final Context context) {
        return context.getApplicationContext().getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
    }

    /**
     * Persists a broker-install resume so it can survive a MAM process restart. Called by the SDK glue
     * when the broker-install result arrives — no 1P app involvement.
     *
     * @param context   any Android context.
     * @param upn       the WPJ UPN / {@code login_hint} to re-drive with.
     * @param target    the requested resource/scopes to re-drive with (may be empty).
     * @param appLink   the Company Portal install {@code app_link}.
     * @param ttlMillis the durable time-to-live in millis.
     */
    public static void markBrokerInstallPending(@NonNull final Context context,
                                                @Nullable final String upn,
                                                @Nullable final String target,
                                                @Nullable final String appLink,
                                                final long ttlMillis) {
        prefs(context).edit()
                .putInt(KEY_PHASE, PHASE_BROKER_INSTALL_PENDING)
                .putString(KEY_UPN, upn)
                .putString(KEY_TARGET, target)
                .putString(KEY_APP_LINK, appLink)
                .putLong(KEY_EXPIRES_AT_MS, System.currentTimeMillis() + ttlMillis)
                .apply();
        Logger.info(TAG + ":markBrokerInstallPending",
                "Persisted durable broker-install park (upn=" + (upn != null)
                        + ", target=" + (target != null) + ", appLink=" + (appLink != null) + ").");
    }

    /**
     * @param context any Android context.
     * @return a {@link Record} if a broker-install resume is pending and not expired (the caller should
     *         then verify Company Portal is installed before re-driving), otherwise {@code null}.
     */
    @Nullable
    public static Record peekResumeReady(@NonNull final Context context) {
        final SharedPreferences p = prefs(context);
        if (p.getInt(KEY_PHASE, PHASE_NONE) != PHASE_BROKER_INSTALL_PENDING || isExpired(p)) {
            return null;
        }
        return new Record(
                p.getString(KEY_UPN, null),
                p.getString(KEY_TARGET, null),
                p.getString(KEY_APP_LINK, null));
    }

    /**
     * Removes any persisted record. Call after the resume is re-driven, or on a normal completion.
     *
     * @param context any Android context.
     */
    public static void clear(@NonNull final Context context) {
        prefs(context).edit().clear().apply();
    }

    private static boolean isExpired(@NonNull final SharedPreferences p) {
        if (DISABLE_EXPIRY_FOR_POC) {
            return false;
        }
        final long expiresAt = p.getLong(KEY_EXPIRES_AT_MS, 0L);
        return expiresAt != 0L && System.currentTimeMillis() >= expiresAt;
    }
}
