// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.
package com.microsoft.identity.common.internal.telemetry;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.common.java.telemetry.OnboardingBlobFieldKeys;

import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class OnboardingTelemetryRecorderTest {

    private static final String SEED_JSON = "{\"schema_version\":\"1.0.0\","
            + "\"session_correlation_id\":\"test-uuid-123\","
            + "\"onboarding_mode\":\"non-brokered\"}";
    private static final String CLIENT_ID = "test-client-id";
    private static final String TARGET = "scope1 scope2";

    private OnboardingTelemetryRecorder mRecorder;

    @Before
    public void setup() {
        mRecorder = new OnboardingTelemetryRecorder(
                SEED_JSON, CLIENT_ID, TARGET,
                ApplicationProvider.getApplicationContext());
    }

    // --- Constructor / seed parsing ---

    @Test
    public void testGetSessionCorrelationId() {
        Assert.assertEquals("test-uuid-123", mRecorder.getSessionCorrelationId());
    }

    @Test
    public void testConstructorWithCorruptedSeedJson() {
        final OnboardingTelemetryRecorder recorder = new OnboardingTelemetryRecorder(
                "not valid json", CLIENT_ID, TARGET,
                ApplicationProvider.getApplicationContext());
        Assert.assertEquals("", recorder.getSessionCorrelationId());
    }

    @Test
    public void testConstructorWithEmptySeedJson() {
        final OnboardingTelemetryRecorder recorder = new OnboardingTelemetryRecorder(
                "{}", CLIENT_ID, TARGET,
                ApplicationProvider.getApplicationContext());
        Assert.assertEquals("", recorder.getSessionCorrelationId());
    }

    // --- finalizeBlob ---

    @Test
    public void testFinalizeBlob_NoBlockingErrors_ReturnsEmpty() {
        final String result = mRecorder.finalizeBlob();
        Assert.assertEquals("", result);
    }

    @Test
    public void testFinalizeBlob_WithBlockingError_ReturnsPopulatedJson() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");

        final String result = mRecorder.finalizeBlob();
        Assert.assertFalse(result.isEmpty());

        final JSONObject blob = new JSONObject(result);
        Assert.assertEquals("1.0.0", blob.getString("schema_version"));
        Assert.assertEquals("test-uuid-123", blob.getString("session_correlation_id"));
        Assert.assertEquals("non-brokered", blob.getString("onboarding_mode"));

        final JSONArray errors = blob.getJSONArray("blocking_errors");
        Assert.assertEquals(1, errors.length());
        Assert.assertEquals("BROKER_INSTALLATION_TRIGGERED", errors.getString(0));
        Assert.assertEquals("BROKER_INSTALLATION_TRIGGERED", blob.getString("last_blocking_error"));
    }

    @Test
    public void testFinalizeBlob_MultipleBlockingErrors() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");
        mRecorder.addBlockingError("MDM_FLOW");

        final JSONObject blob = new JSONObject(mRecorder.finalizeBlob());
        final JSONArray errors = blob.getJSONArray("blocking_errors");
        Assert.assertEquals(2, errors.length());
        Assert.assertEquals("MDM_FLOW", blob.getString("last_blocking_error"));
    }

    @Test
    public void testFinalizeBlob_ContainsSeedFields() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");

        final JSONObject blob = new JSONObject(mRecorder.finalizeBlob());
        Assert.assertEquals("1.0.0", blob.getString("schema_version"));
        Assert.assertEquals("test-uuid-123", blob.getString("session_correlation_id"));
        Assert.assertEquals("non-brokered", blob.getString("onboarding_mode"));
    }

    // --- addStep ---

    @Test
    public void testAddStep_AppearsInFinalizedBlob() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");
        mRecorder.addStep(OnboardingBlobFieldKeys.STEP_AUTHENTICATION_STARTED, "2025-10-29T15:00:00Z");
        mRecorder.addStep(OnboardingBlobFieldKeys.STEP_BROKER_INSTALL_PROMPTED, "2025-10-29T15:00:05Z");

        final JSONObject blob = new JSONObject(mRecorder.finalizeBlob());
        final JSONArray steps = blob.getJSONArray("steps_list");
        Assert.assertEquals(2, steps.length());
        Assert.assertEquals("AuthenticationStarted", steps.getJSONObject(0).getString("step_id"));
        Assert.assertEquals("BrokerInstallPrompted", steps.getJSONObject(1).getString("step_id"));
    }

    @Test
    public void testLastCompletedStep_SetAutomatically() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");
        mRecorder.addStep(OnboardingBlobFieldKeys.STEP_AUTHENTICATION_STARTED, "2025-10-29T15:00:00Z");
        mRecorder.addStep(OnboardingBlobFieldKeys.STEP_BROKER_INSTALL_PROMPTED, "2025-10-29T15:00:01Z");

        final JSONObject blob = new JSONObject(mRecorder.finalizeBlob());
        Assert.assertEquals("BrokerInstallPrompted", blob.getString("last_completed_step"));
    }

    // --- setLastLoadedDomain ---

    @Test
    public void testSetLastLoadedDomain() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");
        mRecorder.setLastLoadedDomain("login.microsoftonline.com");

        final JSONObject blob = new JSONObject(mRecorder.finalizeBlob());
        Assert.assertEquals("login.microsoftonline.com", blob.getString("last_loaded_domain"));
    }

    @Test
    public void testLastLoadedDomain_NotSetByDefault() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");

        final JSONObject blob = new JSONObject(mRecorder.finalizeBlob());
        Assert.assertFalse(blob.has("last_loaded_domain"));
    }

    // --- setProfile ---

    @Test
    public void testSetProfile() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");
        mRecorder.setProfile(OnboardingBlobFieldKeys.PROFILE_WORK);

        final JSONObject blob = new JSONObject(mRecorder.finalizeBlob());
        Assert.assertEquals("workProfile", blob.getString("profile"));
    }

    // --- addUxFlowUsed ---

    @Test
    public void testAddUxFlowUsed() throws Exception {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");
        mRecorder.addUxFlowUsed("MobileOnboardingPhase1");

        final JSONObject blob = new JSONObject(mRecorder.finalizeBlob());
        final JSONArray flows = blob.getJSONArray("ux_flow_used");
        Assert.assertEquals(1, flows.length());
        Assert.assertEquals("MobileOnboardingPhase1", flows.getString(0));
    }

    // --- SharedPreferences persistence ---

    @Test
    public void testAddBlockingError_PersistsToSharedPreferences() {
        mRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");

        // Verify SharedPreferences was written
        final android.content.SharedPreferences prefs =
                ApplicationProvider.getApplicationContext().getSharedPreferences(
                        "com.microsoft.oneauth.session_correlation_cache",
                        android.content.Context.MODE_PRIVATE);
        final String cached = prefs.getString("com.microsoft.oneauth.session_correlation_cache", "");
        Assert.assertFalse("SharedPreferences should contain cached session data", cached.isEmpty());
        Assert.assertTrue("Cached data should contain the session correlation ID",
                cached.contains("test-uuid-123"));
    }
}
