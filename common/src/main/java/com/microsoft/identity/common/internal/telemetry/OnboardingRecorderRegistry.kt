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

import androidx.annotation.GuardedBy
import androidx.annotation.Nullable
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.logging.Logger

/**
 * Process-local handoff for request-owned onboarding telemetry recorders.
 *
 * Owners register a recorder before launching Common authorization. The authorization WebView
 * borrows that exact recorder by correlation ID, while ownership and finalization remain with the
 * registering component.
 */
object OnboardingRecorderRegistry {
    private val TAG = OnboardingRecorderRegistry::class.java.simpleName
    private val lock = Any()

    @GuardedBy("lock")
    private val recorders = mutableMapOf<String, OnboardingTelemetryRecorder>()

    /**
     * Registers [recorder] for [correlationId].
     *
     * Registration is idempotent for the same object. A different active recorder cannot replace
     * the existing entry for the same correlation ID.
     *
     * @return `true` when the recorder is registered or was already registered; otherwise `false`.
     */
    @JvmStatic
    fun register(
        correlationId: String?,
        recorder: OnboardingTelemetryRecorder?
    ): Boolean {
        if (recorder == null) {
            return false
        }
        val key = usableKeyOrNull(correlationId)
        if (key == null) {
            Logger.warn(
                TAG,
                correlationId,
                "Not registering onboarding recorder because the correlation ID is unavailable."
            )
            return false
        }

        val outcome = synchronized(lock) {
            val existing = recorders[key]
            when {
                existing == null -> {
                    recorders[key] = recorder
                    RegistrationOutcome.REGISTERED
                }
                existing === recorder -> RegistrationOutcome.ALREADY_REGISTERED
                else -> RegistrationOutcome.COLLISION
            }
        }

        when (outcome) {
            RegistrationOutcome.REGISTERED ->
                Logger.info(TAG, key, "Registered onboarding recorder.")
            RegistrationOutcome.ALREADY_REGISTERED ->
                Logger.info(TAG, key, "Onboarding recorder was already registered.")
            RegistrationOutcome.COLLISION ->
                Logger.warn(
                    TAG,
                    key,
                    "Rejected onboarding recorder registration because the correlation ID is active."
                )
        }
        return outcome != RegistrationOutcome.COLLISION
    }

    /**
     * Returns the recorder registered for [correlationId], or null when no recorder is available.
     */
    @JvmStatic
    @Nullable
    fun get(correlationId: String?): OnboardingTelemetryRecorder? {
        val key = usableKeyOrNull(correlationId) ?: return null
        return synchronized(lock) { recorders[key] }
    }

    /**
     * Removes an entry only when [expectedRecorder] is the object registered for [correlationId].
     *
     * @return `true` when the expected recorder was removed; otherwise `false`.
     */
    @JvmStatic
    fun unregister(
        correlationId: String?,
        expectedRecorder: OnboardingTelemetryRecorder?
    ): Boolean {
        if (expectedRecorder == null) {
            return false
        }
        val key = usableKeyOrNull(correlationId) ?: return false
        val removed = synchronized(lock) {
            if (recorders[key] === expectedRecorder) {
                recorders.remove(key)
                true
            } else {
                false
            }
        }
        if (removed) {
            Logger.info(TAG, key, "Unregistered onboarding recorder.")
        }
        return removed
    }

    private fun usableKeyOrNull(correlationId: String?): String? =
        correlationId?.takeIf {
            it.isNotBlank() && it != DiagnosticContext.UNSET_CORRELATION_ID
        }

    @JvmStatic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun size(): Int = synchronized(lock) { recorders.size }

    @JvmStatic
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    fun clearForTest() {
        synchronized(lock) {
            recorders.clear()
        }
    }

    /**
     * Result of attempting to register a recorder for a correlation ID.
     */
    private enum class RegistrationOutcome {
        /** The correlation ID was unclaimed and the recorder was stored. */
        REGISTERED,

        /** The same recorder instance was already registered for the correlation ID. */
        ALREADY_REGISTERED,

        /** A different recorder instance already owns the correlation ID. */
        COLLISION
    }
}
