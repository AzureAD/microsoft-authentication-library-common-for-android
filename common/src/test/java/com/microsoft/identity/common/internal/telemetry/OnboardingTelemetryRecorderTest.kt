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
import android.content.ContextWrapper
import android.content.SharedPreferences
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.java.telemetry.IOnboardingTelemetryRecorder
import com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants
import org.json.JSONObject
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Collections

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

    @Test
    fun testExistingBlob_ImportsHistoryAndAppendsWithoutReplacingTimestampsOrDuplicates() {
        val existing = JSONObject(SEED_JSON).apply {
            put("steps_list", org.json.JSONArray().apply {
                put(JSONObject().put("step_id", "AuthenticationStarted")
                    .put("ts", "2025-01-01T00:00:00.000Z").put("source", "core"))
                put(JSONObject().put("step_id", "BrokerInstallPrompted")
                    .put("ts", "2025-01-01T00:01:00.000Z"))
            })
            put("blocking_errors", org.json.JSONArray().put("MDM_FLOW").put("MDM_FLOW"))
            put("last_blocking_error", "stale")
            put("last_completed_step", "stale")
            put("ux_flow_used", org.json.JSONArray().put("phase1").put("phase1"))
            put("last_loaded_domain", "initial.example")
            put("profile", "workProfile")
            put("future_field", JSONObject().put("enabled", true))
        }
        val r = OnboardingTelemetryRecorder(
            existing.toString(), CLIENT_ID, TARGET, ApplicationProvider.getApplicationContext()
        )
        val initial = JSONObject(r.finalizeBlob())
        Assert.assertEquals("BrokerInstallPrompted", initial.getString("last_completed_step"))
        Assert.assertEquals("MDM_FLOW", initial.getString("last_blocking_error"))
        Assert.assertEquals("initial.example", initial.getString("last_loaded_domain"))
        Assert.assertEquals("workProfile", initial.getString("profile"))

        r.addStep("TokenIssued")
        r.addBlockingError("BROKER_INSTALLATION_TRIGGERED")
        r.addUxFlowUsed("phase2")
        r.setLastLoadedDomain("final.example")
        r.setProfile("userProfile")

        val blob = JSONObject(r.finalizeBlob())
        val steps = blob.getJSONArray("steps_list")
        Assert.assertEquals(3, steps.length())
        Assert.assertEquals("2025-01-01T00:00:00.000Z", steps.getJSONObject(0).getString("ts"))
        Assert.assertEquals("core", steps.getJSONObject(0).getString("source"))
        Assert.assertEquals("2025-01-01T00:01:00.000Z", steps.getJSONObject(1).getString("ts"))
        Assert.assertTrue(steps.getJSONObject(2).getString("ts").isNotEmpty())
        Assert.assertEquals("TokenIssued", blob.getString("last_completed_step"))
        val errors = blob.getJSONArray("blocking_errors")
        Assert.assertEquals(3, errors.length())
        Assert.assertEquals("MDM_FLOW", errors.getString(0))
        Assert.assertEquals("MDM_FLOW", errors.getString(1))
        Assert.assertEquals("BROKER_INSTALLATION_TRIGGERED", errors.getString(2))
        Assert.assertEquals("BROKER_INSTALLATION_TRIGGERED", blob.getString("last_blocking_error"))
        val flows = blob.getJSONArray("ux_flow_used")
        Assert.assertEquals(3, flows.length())
        Assert.assertEquals("phase1", flows.getString(0))
        Assert.assertEquals("phase1", flows.getString(1))
        Assert.assertEquals("phase2", flows.getString(2))
        Assert.assertEquals("final.example", blob.getString("last_loaded_domain"))
        Assert.assertEquals("userProfile", blob.getString("profile"))
        Assert.assertTrue(blob.getJSONObject("future_field").getBoolean("enabled"))
        Assert.assertEquals(3, JSONObject(r.finalizeBlob()).getJSONArray("steps_list").length())
    }

    @Test
    fun testExistingBlob_InvalidEntriesAreSkippedAndStaleDerivedFieldsAreRemoved() {
        val existing = JSONObject(SEED_JSON).apply {
            put("steps_list", org.json.JSONArray().put(JSONObject().put("step_id", "missingTs"))
                .put("invalid").put(JSONObject().put("step_id", 42).put("ts", "timestamp")))
            put("blocking_errors", org.json.JSONArray().put(42).put(JSONObject.NULL))
            put("ux_flow_used", org.json.JSONArray().put(true))
            put("last_completed_step", "stale")
            put("last_blocking_error", "stale")
            put("last_loaded_domain", JSONObject.NULL)
            put("profile", 12)
        }
        val r = OnboardingTelemetryRecorder(
            existing.toString(), CLIENT_ID, TARGET, ApplicationProvider.getApplicationContext()
        )
        val blob = JSONObject(r.finalizeBlob())
        Assert.assertEquals(0, blob.getJSONArray("steps_list").length())
        Assert.assertEquals(0, blob.getJSONArray("blocking_errors").length())
        Assert.assertEquals(0, blob.getJSONArray("ux_flow_used").length())
        Assert.assertFalse(blob.has("last_completed_step"))
        Assert.assertFalse(blob.has("last_blocking_error"))
        Assert.assertFalse(blob.has("last_loaded_domain"))
        Assert.assertFalse(blob.has("profile"))
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
    fun testFinalizeBlob_DuplicateBlockingErrorsArePreservedAndLastWins() {
        recorder.addBlockingError("530003")
        recorder.addBlockingError("530003")
        recorder.addBlockingError("53003")

        val blob = JSONObject(recorder.finalizeBlob())
        val errors = blob.getJSONArray("blocking_errors")
        Assert.assertEquals(3, errors.length())
        Assert.assertEquals("530003", errors.getString(0))
        Assert.assertEquals("530003", errors.getString(1))
        Assert.assertEquals("53003", errors.getString(2))
        Assert.assertEquals("53003", blob.getString("last_blocking_error"))
    }

    @Test
    fun testConcurrentBlockingErrorAppendAndFinalizationIsSafe() {
        val failures = Collections.synchronizedList(mutableListOf<Throwable>())
        val writer = Thread {
            try {
                repeat(100) { recorder.addBlockingError("530003") }
            } catch (throwable: Throwable) {
                failures.add(throwable)
            }
        }
        val reader = Thread {
            try {
                repeat(100) { JSONObject(recorder.finalizeBlob()) }
            } catch (throwable: Throwable) {
                failures.add(throwable)
            }
        }

        writer.start()
        reader.start()
        writer.join()
        reader.join()

        Assert.assertTrue(failures.toString(), failures.isEmpty())
        val finalBlob = JSONObject(recorder.finalizeBlob())
        Assert.assertEquals(100, finalBlob.getJSONArray("blocking_errors").length())
        Assert.assertEquals("530003", finalBlob.getString("last_blocking_error"))
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

    @Test
    fun testAddBlockingError_CorruptCacheStillPersistsNewEntry() {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences(PREFS_FILE, Context.MODE_PRIVATE)
        prefs.edit().putString(PREFS_FILE, "not valid json").commit()

        recorder.addBlockingError("MDM_FLOW")

        val cache = JSONObject(prefs.getString(PREFS_FILE, "")!!)
        Assert.assertEquals("test-uuid-123", cache.getJSONObject("$CLIENT_ID|$TARGET").getString("id"))
    }

    @Test
    fun testAddBlockingError_PersistenceFailureDoesNotLoseTelemetry() {
        val application = ApplicationProvider.getApplicationContext<Context>()
        val deniedContext = object : ContextWrapper(application) {
            override fun getApplicationContext(): Context = this

            override fun getSharedPreferences(name: String?, mode: Int): SharedPreferences {
                throw SecurityException("Storage unavailable")
            }
        }
        val r = OnboardingTelemetryRecorder(SEED_JSON, CLIENT_ID, TARGET, deniedContext)

        r.addBlockingError("MDM_FLOW")

        val blob = JSONObject(r.finalizeBlob())
        Assert.assertEquals("MDM_FLOW", blob.getJSONArray("blocking_errors").getString(0))
        Assert.assertEquals("MDM_FLOW", blob.getString("last_blocking_error"))
    }

    companion object {
        private const val PREFS_FILE = "com.microsoft.oneauth.session_correlation_cache"
        private const val SEED_JSON =
            "{\"schema_version\":\"1.0.0\"," +
                "\"session_correlation_id\":\"test-uuid-123\"," +
                "\"onboarding_mode\":\"non-brokered\"}"
        private const val CLIENT_ID = "test-client-id"
        private const val TARGET = "scope1 scope2"
    }
}
