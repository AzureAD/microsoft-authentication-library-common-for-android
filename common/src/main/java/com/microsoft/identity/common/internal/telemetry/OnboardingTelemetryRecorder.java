// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.identity.common.internal.telemetry;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.telemetry.OnboardingBlobFieldKeys;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Records onboarding telemetry events during interactive auth flows.
 * Called by WebView navigation fragments to track steps, blocking errors,
 * and domain navigation. Operates on an in-memory Java model, converts
 * to JSON at flow end. On block detection, persists sessionCorrelationId
 * to SharedPreferences for app-kill resilience (best-effort, async).
 *
 * Lives in Android common core so both OneAuth (non-brokered) and broker apps
 * (brokered) use the same class.
 */
public class OnboardingTelemetryRecorder {

    private static final String PREFS_FILE = "com.microsoft.oneauth.session_correlation_cache";

    // Seed field key constants — must match OnboardingBlobConstants (Djinni-generated).
    // Duplicated here to avoid a dependency on the Djinni-generated Java class in Common.
    private static final String FIELD_SCHEMA_VERSION = "schema_version";
    private static final String FIELD_SESSION_CORRELATION_ID = "session_correlation_id";
    private static final String FIELD_ONBOARDING_MODE = "onboarding_mode";
    private static final String FIELD_STEPS_LIST = "steps_list";
    private static final String FIELD_STEP_ID = "step_id";
    private static final String FIELD_TS = "ts";

    // Seed fields (from C++ common core)
    private final String mSchemaVersion;
    private final String mSessionCorrelationId;
    private final String mOnboardingMode;

    // Recorder identity for persistence
    private final String mClientId;
    private final String mTarget;
    private final Context mContext;

    // Populated fields
    private final List<StepEntry> mStepsList = new ArrayList<>();
    private final List<String> mBlockingErrors = new ArrayList<>();
    private String mLastLoadedDomain;
    private String mProfile;
    private final List<String> mUxFlowUsed = new ArrayList<>();

    private static class StepEntry {
        final String stepId;
        final String timestamp; // ISO 8601

        StepEntry(String stepId, String timestamp) {
            this.stepId = stepId;
            this.timestamp = timestamp;
        }
    }

    /**
     * Construct a recorder from the seed JSON provided by C++ InteractiveRequest.
     *
     * @param seedJson  The seed blob JSON string from authParameters
     * @param clientId  Client ID for cache persistence key
     * @param target    Target (scopes) for cache persistence key
     * @param context   Android context for SharedPreferences access
     */
    public OnboardingTelemetryRecorder(
            @NonNull String seedJson,
            @NonNull String clientId,
            @NonNull String target,
            @NonNull Context context) {
        mClientId = clientId;
        mTarget = target;
        mContext = context.getApplicationContext();

        // Deserialize seed JSON
        String schemaVersion = "";
        String sessionCorrelationId = "";
        String onboardingMode = "";
        try {
            JSONObject seed = new JSONObject(seedJson);
            schemaVersion = seed.optString(FIELD_SCHEMA_VERSION, "");
            sessionCorrelationId = seed.optString(FIELD_SESSION_CORRELATION_ID, "");
            onboardingMode = seed.optString(FIELD_ONBOARDING_MODE, "");
        } catch (JSONException e) {
            // Corrupted seed — use empty values
        }
        mSchemaVersion = schemaVersion;
        mSessionCorrelationId = sessionCorrelationId;
        mOnboardingMode = onboardingMode;
    }

    /**
     * Record a step in the onboarding flow.
     *
     * @param stepId    Step ID constant (from OnboardingBlobFieldKeys)
     * @param isoTimestamp The time when the step occurred, as ISO 8601 string
     */
    public void addStep(@NonNull String stepId, @NonNull String isoTimestamp) {
        mStepsList.add(new StepEntry(stepId, isoTimestamp));
    }

    /**
     * Record a blocking error detected during the flow.
     * Also persists the session correlation entry to SharedPreferences
     * (best-effort, async) for app-kill resilience.
     *
     * @param errorCode The onboarding blocking-error identifier to record
     *                  (e.g., {@link OnboardingBlobFieldKeys#BLOCKING_ERROR_BROKER_INSTALL}
     *                  or {@link OnboardingBlobFieldKeys#BLOCKING_ERROR_MDM_FLOW}),
     *                  not a numeric service auth error code.
     */
    public void addBlockingError(@NonNull String errorCode) {
        mBlockingErrors.add(errorCode);

        // Persist session correlation to SharedPreferences immediately on block
        persistSessionCorrelation();
    }

    /**
     * Set the last loaded domain during WebView navigation.
     *
     * @param domain The domain URL (e.g., "login.microsoftonline.com")
     */
    public void setLastLoadedDomain(@NonNull String domain) {
        mLastLoadedDomain = domain;
    }

    /**
     * Set the Android profile context.
     *
     * @param profile One of OnboardingBlobFieldKeys.PROFILE_USER or PROFILE_WORK
     */
    public void setProfile(@NonNull String profile) {
        mProfile = profile;
    }

    /**
     * Add a UX flow variant tag.
     *
     * @param flowTag Flow variant (e.g., "MobileOnboardingPhase1")
     */
    public void addUxFlowUsed(@NonNull String flowTag) {
        mUxFlowUsed.add(flowTag);
    }

    /**
     * Finalize the blob and return the JSON string.
     * If no blocking errors were recorded, returns empty string (clears seed blob).
     * Otherwise serializes the populated blob to JSON.
     *
     * @return Populated blob JSON string, or empty string if no blocking errors
     */
    @NonNull
    public String finalizeBlob() {
        if (mBlockingErrors.isEmpty()) {
            return "";
        }

        try {
            JSONObject blob = new JSONObject();

            // Seed fields
            blob.put(FIELD_SCHEMA_VERSION, mSchemaVersion);
            blob.put(FIELD_SESSION_CORRELATION_ID, mSessionCorrelationId);
            blob.put(FIELD_ONBOARDING_MODE, mOnboardingMode);

            // StepsList
            JSONArray steps = new JSONArray();
            for (StepEntry entry : mStepsList) {
                JSONObject step = new JSONObject();
                step.put(FIELD_STEP_ID, entry.stepId);
                step.put(FIELD_TS, entry.timestamp);
                steps.put(step);
            }
            blob.put(FIELD_STEPS_LIST, steps);

            // Platform builder fields
            JSONArray errorsArray = new JSONArray();
            for (String error : mBlockingErrors) {
                errorsArray.put(error);
            }
            blob.put(OnboardingBlobFieldKeys.BLOCKING_ERRORS, errorsArray);
            blob.put(OnboardingBlobFieldKeys.LAST_BLOCKING_ERROR,
                    mBlockingErrors.get(mBlockingErrors.size() - 1));

            if (mLastLoadedDomain != null) {
                blob.put(OnboardingBlobFieldKeys.LAST_LOADED_DOMAIN, mLastLoadedDomain);
            }

            if (!mStepsList.isEmpty()) {
                blob.put(OnboardingBlobFieldKeys.LAST_COMPLETED_STEP,
                        mStepsList.get(mStepsList.size() - 1).stepId);
            }

            if (mProfile != null) {
                blob.put(OnboardingBlobFieldKeys.PROFILE, mProfile);
            }

            if (!mUxFlowUsed.isEmpty()) {
                JSONArray flows = new JSONArray();
                for (String flow : mUxFlowUsed) {
                    flows.put(flow);
                }
                blob.put(OnboardingBlobFieldKeys.UX_FLOW_USED, flows);
            }

            return blob.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    /**
     * Returns the session correlation ID from the seed blob.
     */
    @NonNull
    public String getSessionCorrelationId() {
        return mSessionCorrelationId;
    }

    /**
     * Persist session correlation entry to SharedPreferences.
     * Best-effort async write — may be lost if process is killed before disk flush.
     * Called on block detection for app-kill resilience.
     */
    private void persistSessionCorrelation() {
        if (mSessionCorrelationId.isEmpty()) {
            return;
        }

        try {
            SharedPreferences prefs = mContext.getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE);
            String existing = prefs.getString(PREFS_FILE, "");
            JSONObject cache;
            if (existing != null && !existing.isEmpty()) {
                cache = new JSONObject(existing);
            } else {
                cache = new JSONObject();
            }

            String key = mClientId + "|" + mTarget;
            JSONObject entry = new JSONObject();
            entry.put("id", mSessionCorrelationId);
            entry.put(FIELD_TS, System.currentTimeMillis());
            cache.put(key, entry);

            prefs.edit().putString(PREFS_FILE, cache.toString()).apply();
        } catch (JSONException e) {
            // Best-effort persistence — don't crash
        }
    }
}
