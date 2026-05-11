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
        val (parsedSchemaVersion, parsedSessionCorrelationId, parsedOnboardingMode) = parseSeed(seedJson)
        schemaVersion = parsedSchemaVersion
        sessionCorrelationId = parsedSessionCorrelationId
        onboardingMode = parsedOnboardingMode
    }

    /**
     * Parse the seed JSON into [schemaVersion], [sessionCorrelationId], and [onboardingMode].
     * Returns a Triple of empty strings if the seed is malformed.
     */
    private fun parseSeed(json: String): Triple<String, String, String> = try {
        val seed = JSONObject(json)
        Triple(
            seed.optString(FIELD_SCHEMA_VERSION, ""),
            seed.optString(FIELD_SESSION_CORRELATION_ID, ""),
            seed.optString(FIELD_ONBOARDING_MODE, "")
        )
    } catch (e: JSONException) {
        Logger.warn(
            TAG,
            "Failed to parse onboarding seed JSON; recorder will operate with empty fields: " + e.message
        )
        Triple("", "", "")
    }

    private data class StepEntry(val stepId: String, val timestamp: String)

    /**
     * Record a step in the onboarding flow. Captures the current time automatically.
     *
     * @param stepId Step ID constant (from OnboardingTelemetryConstants)
     */
    fun addStep(stepId: String) {
        val isoTimestamp = SimpleDateFormat(ISO_TIMESTAMP_FORMAT, Locale.US).format(Date())
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
            Logger.verbose(TAG, sessionCorrelationId, "finalizeBlob: no blocking errors recorded, returning empty")
            return EMPTY_BLOB
        }
        if (sessionCorrelationId.isEmpty()) {
            Logger.warn(
                TAG,
                "finalizeBlob: sessionCorrelationId is empty; dropping blob to avoid emitting uncorrelatable telemetry"
            )
            return EMPTY_BLOB
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
            Logger.error(TAG, sessionCorrelationId, "Failed to serialize onboarding blob", e)
            EMPTY_BLOB
        }
    }

    /**
     * Persist session correlation entry to SharedPreferences.
     * Uses async [SharedPreferences.Editor.apply] — the in-memory write is
     * effective immediately, and the disk flush happens shortly after. Acceptable
     * for this use case: blocking errors leave the app alive for seconds-to-minutes
     * of user remediation, so the flush window is far longer than typical loss.
     * Telemetry tolerates rare loss; we avoid main-thread disk I/O.
     * Called on block detection.
     */
    private fun persistSessionCorrelation() {
        if (sessionCorrelationId.isEmpty()) {
            Logger.verbose(TAG, "persistSessionCorrelation: skipped — no sessionCorrelationId")
            return
        }

        try {
            val prefs = appContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
            val existing = prefs.getString(PREFS_FILE, "")
            val cache = if (!existing.isNullOrEmpty()) JSONObject(existing) else JSONObject()

            val key = "$clientId|$target"
            val entry = JSONObject().apply {
                put(FIELD_ID, sessionCorrelationId)
                put(FIELD_TS, System.currentTimeMillis())
            }
            cache.put(key, entry)

            prefs.edit().putString(PREFS_FILE, cache.toString()).apply()
            Logger.verbose(
                TAG,
                sessionCorrelationId,
                "Persisted session correlation entry for key=$key"
            )
        } catch (e: JSONException) {
            Logger.warn(TAG, sessionCorrelationId, "Failed to persist session correlation entry: " + e.message)
        }
    }

    companion object {
        private val TAG = OnboardingTelemetryRecorder::class.java.simpleName
        private const val PREFS_FILE = "com.microsoft.oneauth.session_correlation_cache"
        private const val EMPTY_BLOB = ""
        private const val ISO_TIMESTAMP_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'"

        // Seed field key constants — must match OnboardingBlobConstants (Djinni-generated).
        // Duplicated here to avoid a dependency on the Djinni-generated Java class in Common.
        private const val FIELD_SCHEMA_VERSION = "schema_version"
        private const val FIELD_SESSION_CORRELATION_ID = "session_correlation_id"
        private const val FIELD_ONBOARDING_MODE = "onboarding_mode"
        private const val FIELD_STEPS_LIST = "steps_list"
        private const val FIELD_STEP_ID = "step_id"
        private const val FIELD_TS = "ts"
        private const val FIELD_ID = "id"
    }
}
