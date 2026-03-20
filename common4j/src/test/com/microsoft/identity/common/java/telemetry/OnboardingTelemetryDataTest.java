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
package com.microsoft.identity.common.java.telemetry;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.junit.Assert;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Unit tests verifying JSON serialization/deserialization roundtrip for
 * {@link OnboardingTelemetryData}, {@link OnboardingStep}, and {@link OnboardingStepId}.
 *
 * NOTE: Because these classes use {@code @Accessors(prefix = "m")}, the generated getters and
 * builder setter methods strip the leading {@code m} prefix (e.g., field {@code mCorrelationId}
 * → getter {@code getCorrelationId()}, builder method {@code .correlationId(...)}).
 */
public class OnboardingTelemetryDataTest {

    private static final Gson GSON = new GsonBuilder().create();

    // -------------------------------------------------------------------------
    // OnboardingStep tests
    // -------------------------------------------------------------------------

    @Test
    public void onboardingStep_serialization_usesCorrectJsonKeys() {
        final OnboardingStep step = OnboardingStep.builder()
                .stepId(OnboardingStepId.TokenIssued.name())
                .durationMs(250L)
                .build();

        final String json = GSON.toJson(step);

        Assert.assertTrue("JSON must contain 'step_id' key", json.contains("\"step_id\""));
        Assert.assertTrue("JSON must contain 'duration_ms' key", json.contains("\"duration_ms\""));
        Assert.assertTrue("JSON must contain step id value", json.contains("TokenIssued"));
    }

    @Test
    public void onboardingStep_roundtrip_preservesValues() {
        final OnboardingStep original = OnboardingStep.builder()
                .stepId(OnboardingStepId.AuthenticationStarted.name())
                .durationMs(1234L)
                .build();

        final String json = GSON.toJson(original);
        final OnboardingStep deserialized = GSON.fromJson(json, OnboardingStep.class);

        Assert.assertEquals(OnboardingStepId.AuthenticationStarted.name(), deserialized.getStepId());
        Assert.assertEquals(1234L, deserialized.getDurationMs());
    }

    // -------------------------------------------------------------------------
    // OnboardingTelemetryData tests
    // -------------------------------------------------------------------------

    @Test
    public void onboardingTelemetryData_serialization_usesCorrectJsonKeys() {
        final OnboardingTelemetryData data = buildSampleData();
        final String json = GSON.toJson(data);

        Assert.assertTrue(json.contains("\"onboarding_schema_version\""));
        Assert.assertTrue(json.contains("\"correlation_id\""));
        Assert.assertTrue(json.contains("\"session_correlation_id\""));
        Assert.assertTrue(json.contains("\"action_start_time\""));
        Assert.assertTrue(json.contains("\"action_end_time\""));
        Assert.assertTrue(json.contains("\"auth_outcome\""));
        Assert.assertTrue(json.contains("\"error_code\""));
        Assert.assertTrue(json.contains("\"client_app_id\""));
        Assert.assertTrue(json.contains("\"app_name\""));
        Assert.assertTrue(json.contains("\"app_ver\""));
        Assert.assertTrue(json.contains("\"resource\""));
        Assert.assertTrue(json.contains("\"scope\""));
        Assert.assertTrue(json.contains("\"msal_broker_app_used\""));
        Assert.assertTrue(json.contains("\"msal_version\""));
        Assert.assertTrue(json.contains("\"broker_version\""));
        Assert.assertTrue(json.contains("\"sdk_version\""));
        Assert.assertTrue(json.contains("\"onboarding_mode\""));
        Assert.assertTrue(json.contains("\"ux_flow_used\""));
        Assert.assertTrue(json.contains("\"remediation_needed\""));
        Assert.assertTrue(json.contains("\"blocking_errors\""));
        Assert.assertTrue(json.contains("\"steps_list\""));
        Assert.assertTrue(json.contains("\"step_count\""));
        Assert.assertTrue(json.contains("\"last_blocking_error\""));
        Assert.assertTrue(json.contains("\"last_loaded_domain\""));
        Assert.assertTrue(json.contains("\"last_completed_step\""));
        Assert.assertTrue(json.contains("\"mdm_enrollment_start_ts\""));
        Assert.assertTrue(json.contains("\"mdm_enrollment_resume_ts\""));
        Assert.assertTrue(json.contains("\"compliance_remediation_start_ts\""));
        Assert.assertTrue(json.contains("\"compliance_remediation_resume_ts\""));
        Assert.assertTrue(json.contains("\"profile\""));
        Assert.assertTrue(json.contains("\"wp_creation_ts\""));
    }

    @Test
    public void onboardingTelemetryData_roundtrip_preservesAllFields() {
        final OnboardingTelemetryData original = buildSampleData();

        final String json = GSON.toJson(original);
        final OnboardingTelemetryData deserialized = GSON.fromJson(json, OnboardingTelemetryData.class);

        Assert.assertEquals("1.0.0", deserialized.getOnboardingSchemaVersion());
        Assert.assertEquals("corr-id-123", deserialized.getCorrelationId());
        Assert.assertEquals("session-corr-id-456", deserialized.getSessionCorrelationId());
        Assert.assertEquals(Long.valueOf(1_000_000L), deserialized.getActionStartTime());
        Assert.assertEquals(Long.valueOf(1_005_000L), deserialized.getActionEndTime());
        Assert.assertEquals("succeeded", deserialized.getAuthOutcome());
        Assert.assertEquals("0", deserialized.getErrorCode());
        Assert.assertEquals("client-app-id", deserialized.getClientAppId());
        Assert.assertEquals("TestApp", deserialized.getAppName());
        Assert.assertEquals("2.0.0", deserialized.getAppVer());
        Assert.assertEquals("https://graph.microsoft.com", deserialized.getResource());
        Assert.assertEquals("user.read", deserialized.getScope());
        Assert.assertTrue(deserialized.getMsalBrokerAppUsed());
        Assert.assertEquals("4.0.0", deserialized.getMsalVersion());
        Assert.assertEquals("1.2.3", deserialized.getBrokerVersion());
        Assert.assertEquals("0.0.1", deserialized.getSdkVersion());
        Assert.assertEquals("brokered", deserialized.getOnboardingMode());
        Assert.assertEquals(Arrays.asList("webview", "broker"), deserialized.getUxFlowUsed());
        Assert.assertFalse(deserialized.getRemediationNeeded());
        Assert.assertEquals(Collections.emptyList(), deserialized.getBlockingErrors());
        Assert.assertNotNull(deserialized.getStepsList());
        Assert.assertEquals(1, deserialized.getStepsList().size());
        Assert.assertEquals(OnboardingStepId.TokenIssued.name(), deserialized.getStepsList().get(0).getStepId());
        Assert.assertEquals(Integer.valueOf(1), deserialized.getStepCount());
        Assert.assertEquals("none", deserialized.getLastBlockingError());
        Assert.assertEquals("login.microsoftonline.com", deserialized.getLastLoadedDomain());
        Assert.assertEquals(OnboardingStepId.TokenIssued.name(), deserialized.getLastCompletedStep());
        Assert.assertEquals(Long.valueOf(2_000_000L), deserialized.getMdmEnrollmentStartTs());
        Assert.assertEquals(Long.valueOf(2_001_000L), deserialized.getMdmEnrollmentResumeTs());
        Assert.assertEquals(Long.valueOf(3_000_000L), deserialized.getComplianceRemediationStartTs());
        Assert.assertEquals(Long.valueOf(3_001_000L), deserialized.getComplianceRemediationResumeTs());
        Assert.assertEquals("work", deserialized.getProfile());
        Assert.assertEquals(Long.valueOf(4_000_000L), deserialized.getWpCreationTs());
    }

    @Test
    public void onboardingTelemetryData_defaultSchemaVersion_is_1_0_0() {
        final OnboardingTelemetryData data = OnboardingTelemetryData.builder()
                .correlationId("corr")
                .build();

        Assert.assertEquals("1.0.0", data.getOnboardingSchemaVersion());
    }

    @Test
    public void onboardingTelemetryData_nullableFields_areNullByDefault() {
        final OnboardingTelemetryData data = OnboardingTelemetryData.builder()
                .correlationId("corr")
                .authOutcome("succeeded")
                .build();

        Assert.assertNull(data.getSessionCorrelationId());
        Assert.assertNull(data.getErrorCode());
        Assert.assertNull(data.getLastBlockingError());
        Assert.assertNull(data.getLastLoadedDomain());
        Assert.assertNull(data.getLastCompletedStep());
        Assert.assertNull(data.getMdmEnrollmentStartTs());
        Assert.assertNull(data.getMdmEnrollmentResumeTs());
        Assert.assertNull(data.getComplianceRemediationStartTs());
        Assert.assertNull(data.getComplianceRemediationResumeTs());
        Assert.assertNull(data.getWpCreationTs());
    }

    // -------------------------------------------------------------------------
    // OnboardingStepId enum tests
    // -------------------------------------------------------------------------

    @Test
    public void onboardingStepId_allExpectedValuesPresent() {
        final List<String> expected = Arrays.asList(
                "AccountSelectionStarted", "AuthenticationStarted", "CredentialEntryCompleted",
                "PasskeyAuthStarted", "CertBasedAuthStarted", "StrongAuthSetupStarted",
                "StrongAuthSetupCompleted", "AuthenticatorMfaLinkingStarted", "CABlockReceived",
                "InterruptFlowStarted", "ConsentPromptShown", "TermsOfUseShown",
                "PasswordResetRequired", "BrokerInstallPrompted", "DeviceRegistrationStarted",
                "DeviceRegistrationCompleted", "DeviceRegistrationUpgradeStarted",
                "MDMEnrollmentStarted", "CompanyPortalLaunched", "WebCpEnrollmentStarted",
                "GoogleEnrollmentStarted", "IntuneAppProtectionRequired",
                "ComplianceRemediationStarted", "ComplianceRemediationCompleted",
                "PrtAcquired", "TokenIssued", "UserCanceled", "AuthorizationTimedOut"
        );

        for (final String name : expected) {
            // Will throw IllegalArgumentException if the enum constant is missing.
            OnboardingStepId.valueOf(name);
        }

        Assert.assertEquals(expected.size(), OnboardingStepId.values().length);
    }

    // -------------------------------------------------------------------------
    // Helper
    // -------------------------------------------------------------------------

    private static OnboardingTelemetryData buildSampleData() {
        final OnboardingStep step = OnboardingStep.builder()
                .stepId(OnboardingStepId.TokenIssued.name())
                .durationMs(500L)
                .build();

        return OnboardingTelemetryData.builder()
                .correlationId("corr-id-123")
                .sessionCorrelationId("session-corr-id-456")
                .actionStartTime(1_000_000L)
                .actionEndTime(1_005_000L)
                .authOutcome("succeeded")
                .errorCode("0")
                .clientAppId("client-app-id")
                .appName("TestApp")
                .appVer("2.0.0")
                .resource("https://graph.microsoft.com")
                .scope("user.read")
                .msalBrokerAppUsed(true)
                .msalVersion("4.0.0")
                .brokerVersion("1.2.3")
                .sdkVersion("0.0.1")
                .onboardingMode("brokered")
                .uxFlowUsed(Arrays.asList("webview", "broker"))
                .remediationNeeded(false)
                .blockingErrors(Collections.<String>emptyList())
                .stepsList(Collections.singletonList(step))
                .stepCount(1)
                .lastBlockingError("none")
                .lastLoadedDomain("login.microsoftonline.com")
                .lastCompletedStep(OnboardingStepId.TokenIssued.name())
                .mdmEnrollmentStartTs(2_000_000L)
                .mdmEnrollmentResumeTs(2_001_000L)
                .complianceRemediationStartTs(3_000_000L)
                .complianceRemediationResumeTs(3_001_000L)
                .profile("work")
                .wpCreationTs(4_000_000L)
                .build();
    }
}
