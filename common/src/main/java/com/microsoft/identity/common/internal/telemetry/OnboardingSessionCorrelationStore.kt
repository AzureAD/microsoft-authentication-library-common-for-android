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
package com.microsoft.identity.common.internal.telemetry

import android.content.Context

/**
 * SharedPreferences-backed persistence for session correlation IDs.
 * Used by OneAuth (via JNI/Djinni SessionCachePersistence adapter).
 * Each app (OneAuth host, broker) has its own sandboxed SharedPreferences file;
 * the same schema and file name are used across apps for consistency.
 * OnboardingTelemetryRecorder also writes to this file on block detection.
 */
class OnboardingSessionCorrelationStore(context: Context) {

    private val appContext: Context = context.applicationContext

    /**
     * Load the persisted session correlation cache JSON string.
     * @return JSON string, or empty string if nothing is persisted
     */
    fun load(): String {
        val prefs = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        return prefs.getString(PREFS_FILE, "") ?: ""
    }

    /**
     * Save the session correlation cache JSON string to SharedPreferences.
     * @param json The JSON string to persist
     */
    fun save(json: String) {
        val prefs = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_FILE, json).apply()
    }

    companion object {
        private const val PREFS_FILE = "com.microsoft.oneauth.session_correlation_cache"
    }
}
