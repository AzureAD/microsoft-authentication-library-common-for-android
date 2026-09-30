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

import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.logging.Logger

/**
 * Process-local handoff for a live onboarding recorder between its owner and the authorization
 * WebView. Brokered and non-brokered hosts register by request correlation ID before launching the
 * WebView and unregister after finalizing the telemetry blob.
 */
object OnboardingRecorderRegistry {
    private val TAG = OnboardingRecorderRegistry::class.java.simpleName
    private const val MAX_ENTRIES = 16

    private val recorders = object : LinkedHashMap<String, OnboardingTelemetryRecorder>(
        MAX_ENTRIES,
        0.75f,
        true
    ) {
        override fun removeEldestEntry(
            eldest: MutableMap.MutableEntry<String, OnboardingTelemetryRecorder>?
        ): Boolean = size > MAX_ENTRIES
    }

    private fun usableKey(correlationId: String?): String? =
        correlationId?.takeIf {
            it.isNotEmpty() && it != DiagnosticContext.UNSET_CORRELATION_ID
        }

    @JvmStatic
    fun register(correlationId: String?, recorder: OnboardingTelemetryRecorder?) {
        if (recorder == null) {
            return
        }
        val key = usableKey(correlationId)
        if (key == null) {
            Logger.warn(
                TAG,
                correlationId,
                "Not registering onboarding recorder because the correlation ID is unavailable."
            )
            return
        }
        synchronized(recorders) {
            recorders[key] = recorder
        }
    }

    @JvmStatic
    fun get(correlationId: String?): OnboardingTelemetryRecorder? {
        val key = usableKey(correlationId) ?: return null
        return synchronized(recorders) { recorders[key] }
    }

    @JvmStatic
    fun unregister(correlationId: String?) {
        val key = usableKey(correlationId) ?: return
        synchronized(recorders) {
            recorders.remove(key)
        }
    }

    @JvmStatic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun size(): Int = synchronized(recorders) { recorders.size }

    @JvmStatic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun clearForTest() {
        synchronized(recorders) {
            recorders.clear()
        }
    }
}
