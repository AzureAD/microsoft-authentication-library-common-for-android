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
package com.microsoft.identity.common.internal.telemetry;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

/**
 * SharedPreferences-backed persistence for session correlation IDs.
 * Used by OneAuth (via JNI/Djinni SessionCachePersistence adapter).
 * Each app (OneAuth host, broker) has its own sandboxed SharedPreferences file;
 * the same schema and file name are used across apps for consistency.
 * OnboardingTelemetryRecorder also writes to this file on block detection.
 */
public class OnboardingSessionCorrelationStore {

    private static final String PREFS_FILE = "com.microsoft.oneauth.session_correlation_cache";

    private final Context mContext;

    public OnboardingSessionCorrelationStore(@NonNull Context context) {
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
