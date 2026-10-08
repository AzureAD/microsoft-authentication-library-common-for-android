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
import com.microsoft.identity.common.logging.Logger

/**
 * Process-local handoff for a live onboarding recorder between its owner and the authorization
 * WebView. Brokered and non-brokered hosts register by request correlation ID before launching the
 * WebView and unregister after finalizing the telemetry blob.
 */
object OnboardingRecorderRegistry {
    private val TAG = OnboardingRecorderRegistry::class.java.simpleName
    private const val MAX_ENTRIES = 16

    private val recorders = HashMap<String, OnboardingTelemetryRecorder>()

    private fun usableKey(correlationId: String?): String? =
        correlationId?.takeIf {
            it.isNotBlank() && it != "UNSET"
        }

    @JvmStatic
    fun register(correlationId: String?, recorder: OnboardingTelemetryRecorder?) {
        tryRegister(correlationId, recorder)
    }

    /**
     * Registers without replacing another owner or evicting an active request at capacity.
     * Re-registering the same instance is safe. False means no handoff was established.
     */
    @JvmStatic
    fun tryRegister(correlationId: String?, recorder: OnboardingTelemetryRecorder?): Boolean {
        if (recorder == null) {
            return false
        }
        val key = usableKey(correlationId)
        if (key == null) {
            Logger.warn(
                TAG,
                correlationId,
                "Not registering onboarding recorder because the correlation ID is unavailable."
            )
            return false
        }
        return synchronized(recorders) {
            val existing = recorders[key]
            if (existing != null) {
                return@synchronized existing === recorder
            }
            if (recorders.size >= MAX_ENTRIES) {
                return@synchronized false
            }
            recorders[key] = recorder
            true
        }
    }

    @JvmStatic
    fun get(correlationId: String?): OnboardingTelemetryRecorder? {
        val key = usableKey(correlationId) ?: return null
        return synchronized(recorders) { recorders[key] }
    }

    /** Whether an existing request already retains this exact recorder instance. */
    @JvmStatic
    fun isRegistered(recorder: OnboardingTelemetryRecorder): Boolean =
        synchronized(recorders) { recorders.values.any { it === recorder } }

    @JvmStatic
    fun unregister(correlationId: String?) {
        val key = usableKey(correlationId) ?: return
        synchronized(recorders) {
            recorders.remove(key)
        }
    }

    /** Removes only the expected instance; stale owners cannot remove a newer request. */
    @JvmStatic
    fun unregister(correlationId: String?, recorder: OnboardingTelemetryRecorder) {
        val key = usableKey(correlationId) ?: return
        synchronized(recorders) {
            if (recorders[key] === recorder) {
                recorders.remove(key)
            }
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
