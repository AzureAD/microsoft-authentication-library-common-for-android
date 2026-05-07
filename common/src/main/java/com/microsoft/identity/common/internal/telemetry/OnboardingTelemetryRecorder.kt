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
import com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants
import com.microsoft.identity.common.logging.Logger
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Records onboarding telemetry events during interactive auth flows.
 * Called by WebView navigation fragments to track steps, blocking errors,
 * and domain navigation. Operates on an in-memory model, converts
 * to JSON at flow end. On block detection, persists sessionCorrelationId
 * to SharedPreferences for app-kill resilience (best-effort, async).
 *
 * Lives in Android common core so both OneAuth (non-brokered) and broker apps
 * (brokered) use the same class.
 *
 * @param seedJson  The seed blob JSON string from authParameters
 * @param clientId  Client ID for cache persistence key
 * @param target    Target (scopes) for cache persistence key
 * @param context   Android context for SharedPreferences access
 */
class OnboardingTelemetryRecorder(
    seedJson: String,
    private val clientId: String,
    private val target: String,
    context: Context
) {

    private val appContext: Context = context.applicationContext

    // Seed fields (from C++ common core)
    private val schemaVersion: String
    val sessionCorrelationId: String
    private val onboardingMode: String

    // Populated fields
    private val stepsList: MutableList<StepEntry> = mutableListOf()
    private val blockingErrors: MutableList<String> = mutableListOf()
    private var lastLoadedDomain: String? = null
    private var profile: String? = null
    private val uxFlowUsed: MutableList<String> = mutableListOf()

    init {
        // Deserialize seed JSON
        var sv = ""
        var scid = ""
        var om = ""
        try {
            val seed = JSONObject(seedJson)
            sv = seed.optString(FIELD_SCHEMA_VERSION, "")
            scid = seed.optString(FIELD_SESSION_CORRELATION_ID, "")
            om = seed.optString(FIELD_ONBOARDING_MODE, "")
        } catch (_: JSONException) {
            // Corrupted seed — use empty values
        }
        schemaVersion = sv
        sessionCorrelationId = scid
        onboardingMode = om
    }

    private data class StepEntry(val stepId: String, val timestamp: String)

    /**
     * Record a step in the onboarding flow. Captures the current time automatically.
     *
     * @param stepId Step ID constant (from OnboardingTelemetryConstants)
     */
    fun addStep(stepId: String) {
        val isoTimestamp = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", Locale.US
        ).format(Date())
        stepsList.add(StepEntry(stepId, isoTimestamp))
    }

    /**
     * Record a blocking error detected during the flow.
     * Also persists the session correlation entry to SharedPreferences
     * (best-effort, async) for app-kill resilience.
     *
     * @param errorCode The onboarding blocking-error identifier to record
     *                  (e.g., [OnboardingTelemetryConstants.BLOCKING_ERROR_BROKER_INSTALL]
     *                  or [OnboardingTelemetryConstants.BLOCKING_ERROR_MDM_FLOW]),
     *                  not a numeric service auth error code.
     */
    fun addBlockingError(errorCode: String) {
        blockingErrors.add(errorCode)

        // Persist session correlation to SharedPreferences immediately on block
        persistSessionCorrelation()
    }

    /**
     * Set the last loaded domain during WebView navigation.
     *
     * @param domain The domain URL (e.g., "login.microsoftonline.com")
     */
    fun setLastLoadedDomain(domain: String) {
        lastLoadedDomain = domain
    }

    /**
     * Set the Android profile context.
     *
     * @param profile One of [OnboardingTelemetryConstants.PROFILE_USER] or
     *                [OnboardingTelemetryConstants.PROFILE_WORK]
     */
    fun setProfile(profile: String) {
        this.profile = profile
    }

    /**
     * Add a UX flow variant tag.
     *
     * @param flowTag Flow variant (e.g., "MobileOnboardingPhase1")
     */
    fun addUxFlowUsed(flowTag: String) {
        uxFlowUsed.add(flowTag)
    }

    /**
     * Finalize the blob and return the JSON string.
     * If no blocking errors were recorded, returns empty string (clears seed blob).
     * Otherwise serializes the populated blob to JSON.
     *
     * @return Populated blob JSON string, or empty string if no blocking errors
     */
    fun finalizeBlob(): String {
        if (blockingErrors.isEmpty()) {
            Logger.verbose(TAG, "finalizeBlob: no blocking errors recorded, returning empty")
            return ""
        }

        return try {
            val blob = JSONObject().apply {
                // Seed fields
                put(FIELD_SCHEMA_VERSION, schemaVersion)
                put(FIELD_SESSION_CORRELATION_ID, sessionCorrelationId)
                put(FIELD_ONBOARDING_MODE, onboardingMode)

                // StepsList
                val steps = JSONArray()
                for (entry in stepsList) {
                    steps.put(JSONObject().apply {
                        put(FIELD_STEP_ID, entry.stepId)
                        put(FIELD_TS, entry.timestamp)
                    })
                }
                put(FIELD_STEPS_LIST, steps)

                // Platform builder fields
                val errorsArray = JSONArray()
                for (error in blockingErrors) {
                    errorsArray.put(error)
                }
                put(OnboardingTelemetryConstants.BLOCKING_ERRORS, errorsArray)
                put(
                    OnboardingTelemetryConstants.LAST_BLOCKING_ERROR,
                    blockingErrors.last()
                )

                lastLoadedDomain?.let {
                    put(OnboardingTelemetryConstants.LAST_LOADED_DOMAIN, it)
                }

                if (stepsList.isNotEmpty()) {
                    put(
                        OnboardingTelemetryConstants.LAST_COMPLETED_STEP,
                        stepsList.last().stepId
                    )
                }

                profile?.let {
                    put(OnboardingTelemetryConstants.PROFILE, it)
                }

                if (uxFlowUsed.isNotEmpty()) {
                    val flows = JSONArray()
                    for (flow in uxFlowUsed) {
                        flows.put(flow)
                    }
                    put(OnboardingTelemetryConstants.UX_FLOW_USED, flows)
                }
            }

            blob.toString()
        } catch (e: JSONException) {
            Logger.error(TAG, "Failed to serialize onboarding blob", e)
            ""
        }
    }

    /**
     * Persist session correlation entry to SharedPreferences.
     * Best-effort async write — may be lost if process is killed before disk flush.
     * Called on block detection for app-kill resilience.
     */
    private fun persistSessionCorrelation() {
        if (sessionCorrelationId.isEmpty()) {
            return
        }

        try {
            val prefs = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val existing = prefs.getString(PREFS_FILE, "")
            val cache = if (!existing.isNullOrEmpty()) JSONObject(existing) else JSONObject()

            val key = "$clientId|$target"
            val entry = JSONObject().apply {
                put("id", sessionCorrelationId)
                put(FIELD_TS, System.currentTimeMillis())
            }
            cache.put(key, entry)

            prefs.edit().putString(PREFS_FILE, cache.toString()).apply()
        } catch (_: JSONException) {
            // Best-effort persistence — don't crash
        }
    }

    companion object {
        private val TAG = OnboardingTelemetryRecorder::class.java.simpleName
        private const val PREFS_FILE = "com.microsoft.oneauth.session_correlation_cache"

        // Seed field key constants — must match OnboardingBlobConstants (Djinni-generated).
        // Duplicated here to avoid a dependency on the Djinni-generated Java class in Common.
        private const val FIELD_SCHEMA_VERSION = "schema_version"
        private const val FIELD_SESSION_CORRELATION_ID = "session_correlation_id"
        private const val FIELD_ONBOARDING_MODE = "onboarding_mode"
        private const val FIELD_STEPS_LIST = "steps_list"
        private const val FIELD_STEP_ID = "step_id"
        private const val FIELD_TS = "ts"
    }
}
