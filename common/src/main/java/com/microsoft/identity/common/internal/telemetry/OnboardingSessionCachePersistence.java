// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.telemetry;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * SharedPreferences-backed persistence for session correlation IDs.
 * Used by both OneAuth (via JNI/Djinni) and the broker app directly.
 * The same SharedPreferences file is written to by OnboardingTelemetryRecorder
 * on block detection for app-kill resilience.
 */
public class OnboardingSessionCachePersistence {

    private static final String PREFS_FILE = "com.microsoft.oneauth.session_correlation_cache";

    private final Context mContext;

    public OnboardingSessionCachePersistence(@NonNull Context context) {
        mContext = context.getApplicationContext();
    }

    /**
     * Load the persisted session correlation cache JSON string.
     * @return JSON string, or empty string if nothing is persisted
     */
    @NonNull
    public String load() {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        String value = prefs.getString(PREFS_FILE, "");
        return value != null ? value : "";
    }

    /**
     * Save the session correlation cache JSON string to SharedPreferences.
     * @param json The JSON string to persist
     */
    public void save(@NonNull String json) {
        SharedPreferences prefs = mContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
        prefs.edit().putString(PREFS_FILE, json).apply();
    }
}
