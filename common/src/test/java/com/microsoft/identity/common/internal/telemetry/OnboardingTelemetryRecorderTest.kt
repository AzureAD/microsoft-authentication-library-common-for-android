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
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.java.telemetry.IOnboardingTelemetryRecorder
import com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingTelemetryRecorderTest {

    private lateinit var recorder: OnboardingTelemetryRecorder

    @Before
    fun setup() {
        recorder = OnboardingTelemetryRecorder(
            SEED_JSON, CLIENT_ID, TARGET,
            ApplicationProvider.getApplicationContext()
        )
    }

    // --- Constructor / seed parsing ---

    @Test
    fun testGetSessionCorrelationId() {
        Assert.assertEquals("test-uuid-123", recorder.sessionCorrelationId)
    }

    @Test
    fun testConstructorWithCorruptedSeedJson() {
        val r = OnboardingTelemetryRecorder(
            "not valid json", CLIENT_ID, TARGET,
            ApplicationProvider.getApplicationContext()
        )
        Assert.assertEquals("", r.sessionCorrelationId)
    }

    @Test
    fun testConstructorWithEmptySeedJson() {
        val r = OnboardingTelemetryRecorder(
            "{}", CLIENT_ID, TARGET,
            ApplicationProvider.getApplicationContext()
        )
        Assert.assertEquals("", r.sessionCorrelationId)
    }

    // --- finalizeBlob ---

    @Test
    fun testFinalizeBlob_AccessibleViaInterfaceType() {
        // finalizeBlob() is promoted onto IOnboardingTelemetryRecorder (AB#3647677), so callers
        // that program to the interface (e.g. AccountChooserActivity) can finalize without an
        // instanceof downcast to the concrete recorder. Exercise the whole recording surface —
        // addStep, addBlockingError, finalizeBlob — purely through the interface-typed reference.
        val interfaceRecorder: IOnboardingTelemetryRecorder = recorder
        interfaceRecorder.addStep(OnboardingTelemetryConstants.STEP_AUTHENTICATION_STARTED)
        interfaceRecorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")

        val result = interfaceRecorder.finalizeBlob()
        Assert.assertFalse(result.isEmpty())

        val blob = JSONObject(result)
        Assert.assertEquals("test-uuid-123", blob.getString("session_correlation_id"))
        Assert.assertEquals("AuthenticationStarted", blob.getString("last_completed_step"))
        Assert.assertEquals("BROKER_INSTALLATION_TRIGGERED", blob.getString("last_blocking_error"))
    }

    @Test
    fun testFinalizeBlob_DefaultMethod_ReturnsEmptyBlobForMinimalImplementer() {
        // finalizeBlob() is a default method on IOnboardingTelemetryRecorder (AB#3647677) so that
        // adding it to the already-released interface stays binary-compatible: an implementation
        // that only implements the recording surface (addStep / addBlockingError) inherits the
        // default, which returns the documented empty-blob sentinel instead of throwing
        // AbstractMethodError. Verify a minimal implementer round-trips through finalizeBlob().
        val minimalRecorder = object : IOnboardingTelemetryRecorder {
            override fun addStep(stepId: String) = Unit
            override fun addBlockingError(errorCode: String) = Unit
        }

        Assert.assertEquals("", minimalRecorder.finalizeBlob())
    }

    @Test
    fun testFinalizeBlob_NoBlockingErrors_StillEmitsBlobWithSeedFields() {
        // When a valid seed was provided but no blocking errors occurred (smooth-success
        // flow), the recorder still emits a populated blob so consumers (OneAuth) can
        // correlate the session and count it toward smooth-success metrics. The decision
        // to forward to MATS belongs to the consumer based on blob content, not to the
        // broker / common layer.
        val result = recorder.finalizeBlob()
        Assert.assertFalse(result.isEmpty())

        val blob = JSONObject(result)
        Assert.assertEquals("1.0.0", blob.getString("schema_version"))
        Assert.assertEquals("test-uuid-123", blob.getString("session_correlation_id"))
        // blocking_errors stays as an empty array (schema-stable), not absent.
        Assert.assertEquals(0, blob.getJSONArray("blocking_errors").length())
        // last_blocking_error MUST be absent when no errors were recorded.
        Assert.assertFalse(blob.has("last_blocking_error"))
    }

    @Test
    fun testFinalizeBlob_WithBlockingError_ReturnsPopulatedJson() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")

        val result = recorder.finalizeBlob()
        Assert.assertFalse(result.isEmpty())

        val blob = JSONObject(result)
        Assert.assertEquals("1.0.0", blob.getString("schema_version"))
        Assert.assertEquals("test-uuid-123", blob.getString("session_correlation_id"))
        Assert.assertEquals("non-brokered", blob.getString("onboarding_mode"))

        val errors = blob.getJSONArray("blocking_errors")
        Assert.assertEquals(1, errors.length())
        Assert.assertEquals("BROKER_INSTALLATION_TRIGGERED", errors.getString(0))
        Assert.assertEquals("BROKER_INSTALLATION_TRIGGERED", blob.getString("last_blocking_error"))
    }

    @Test
    fun testFinalizeBlob_MultipleBlockingErrors() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")
        recorder.addBlockingError("MDM_FLOW")

        val blob = JSONObject(recorder.finalizeBlob())
        val errors = blob.getJSONArray("blocking_errors")
        Assert.assertEquals(2, errors.length())
        Assert.assertEquals("MDM_FLOW", blob.getString("last_blocking_error"))
    }

    @Test
    fun testFinalizeBlob_ContainsSeedFields() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")

        val blob = JSONObject(recorder.finalizeBlob())
        Assert.assertEquals("1.0.0", blob.getString("schema_version"))
        Assert.assertEquals("test-uuid-123", blob.getString("session_correlation_id"))
        Assert.assertEquals("non-brokered", blob.getString("onboarding_mode"))
    }

    @Test
    fun testFinalizeBlob_EmptySessionCorrelationId_ReturnsEmptyBlob() {
        // A corrupted/missing seed leaves sessionCorrelationId empty. finalizeBlob() must
        // refuse to emit in that case: a blob without a sessionCorrelationId cannot be joined
        // with the broker side or with retries, so emitting it would be unattributable noise.
        // Recording a blocking error first also exercises the persistSessionCorrelation()
        // no-op guard for the empty-sessionCorrelationId path.
        val r = OnboardingTelemetryRecorder(
            "not valid json", CLIENT_ID, TARGET,
            ApplicationProvider.getApplicationContext()
        )
        Assert.assertEquals("", r.sessionCorrelationId)
        r.addBlockingError("BROKER_INSTALLATION_TRIGGERED")

        Assert.assertEquals("", r.finalizeBlob())
    }

    // --- addStep ---

    @Test
    fun testAddStep_AppearsInFinalizedBlob() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")
        recorder.addStep(OnboardingTelemetryConstants.STEP_AUTHENTICATION_STARTED)
        recorder.addStep(OnboardingTelemetryConstants.STEP_BROKER_INSTALL_PROMPTED)

        val blob = JSONObject(recorder.finalizeBlob())
        val steps = blob.getJSONArray("steps_list")
        Assert.assertEquals(2, steps.length())
        Assert.assertEquals("AuthenticationStarted", steps.getJSONObject(0).getString("step_id"))
        Assert.assertTrue(steps.getJSONObject(0).has("ts"))
        Assert.assertEquals("BrokerInstallPrompted", steps.getJSONObject(1).getString("step_id"))
        Assert.assertTrue(steps.getJSONObject(1).has("ts"))
    }

    @Test
    fun testLastCompletedStep_SetAutomatically() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")
        recorder.addStep(OnboardingTelemetryConstants.STEP_AUTHENTICATION_STARTED)
        recorder.addStep(OnboardingTelemetryConstants.STEP_BROKER_INSTALL_PROMPTED)

        val blob = JSONObject(recorder.finalizeBlob())
        Assert.assertEquals("BrokerInstallPrompted", blob.getString("last_completed_step"))
    }

    // --- setLastLoadedDomain ---

    @Test
    fun testSetLastLoadedDomain() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")
        recorder.setLastLoadedDomain("login.microsoftonline.com")

        val blob = JSONObject(recorder.finalizeBlob())
        Assert.assertEquals("login.microsoftonline.com", blob.getString("last_loaded_domain"))
    }

    @Test
    fun testLastLoadedDomain_NotSetByDefault() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")

        val blob = JSONObject(recorder.finalizeBlob())
        Assert.assertFalse(blob.has("last_loaded_domain"))
    }

    // --- setProfile ---

    @Test
    fun testSetProfile() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")
        recorder.setProfile(OnboardingTelemetryConstants.PROFILE_WORK)

        val blob = JSONObject(recorder.finalizeBlob())
        Assert.assertEquals("workProfile", blob.getString("profile"))
    }

    // --- addUxFlowUsed ---

    @Test
    fun testAddUxFlowUsed() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")
        recorder.addUxFlowUsed("MobileOnboardingPhase1")

        val blob = JSONObject(recorder.finalizeBlob())
        val flows = blob.getJSONArray("ux_flow_used")
        Assert.assertEquals(1, flows.length())
        Assert.assertEquals("MobileOnboardingPhase1", flows.getString(0))
    }

    // --- SharedPreferences persistence ---

    @Test
    fun testAddBlockingError_PersistsToSharedPreferences() {
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED")

        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(
                "com.microsoft.oneauth.session_correlation_cache",
                Context.MODE_PRIVATE
            )
        val cached = prefs.getString("com.microsoft.oneauth.session_correlation_cache", "") ?: ""
        Assert.assertFalse("SharedPreferences should contain cached session data", cached.isEmpty())
        Assert.assertTrue(
            "Cached data should contain the session correlation ID",
            cached.contains("test-uuid-123")
        )
    }

    companion object {
        private const val SEED_JSON =
            "{\"schema_version\":\"1.0.0\"," +
                "\"session_correlation_id\":\"test-uuid-123\"," +
                "\"onboarding_mode\":\"non-brokered\"}"
        private const val CLIENT_ID = "test-client-id"
        private const val TARGET = "scope1 scope2"
    }
}
