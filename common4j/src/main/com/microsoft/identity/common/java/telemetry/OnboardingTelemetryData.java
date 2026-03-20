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

import com.google.gson.annotations.SerializedName;

import java.util.List;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * Data model for Mobile Onboarding Telemetry.
 * This is the "onboarding" sub-schema nested within the MATS blob, tracking
 * interactive token request journeys across brokered and non-brokered Android auth flows.
 */
@Builder
@Getter
@Accessors(prefix = "m")
public class OnboardingTelemetryData {

    /**
     * The schema version of this onboarding data. Default is "1.0.0".
     * Serialized as "onboarding_schema_version".
     */
    @SerializedName("onboarding_schema_version")
    @Builder.Default
    private final String mOnboardingSchemaVersion = "1.0.0";

    /**
     * The correlation ID for this auth request.
     * Serialized as "correlation_id".
     */
    @SerializedName("correlation_id")
    private final String mCorrelationId;

    /**
     * The session-scoped correlation ID, grouping multiple auth attempts into one session.
     * Nullable. Serialized as "session_correlation_id".
     */
    @SerializedName("session_correlation_id")
    @Nullable
    private final String mSessionCorrelationId;

    /**
     * The epoch millisecond timestamp when the auth action started.
     * Serialized as "action_start_time".
     */
    @SerializedName("action_start_time")
    private final Long mActionStartTime;

    /**
     * The epoch millisecond timestamp when the auth action ended.
     * Serialized as "action_end_time".
     */
    @SerializedName("action_end_time")
    private final Long mActionEndTime;

    /**
     * The outcome of the auth flow. Expected values: succeeded, failed, canceled, incomplete.
     * Serialized as "auth_outcome".
     */
    @SerializedName("auth_outcome")
    private final String mAuthOutcome;

    /**
     * The error code if the auth flow did not succeed. Nullable.
     * Serialized as "error_code".
     */
    @SerializedName("error_code")
    @Nullable
    private final String mErrorCode;

    /**
     * The client application ID (client_id) making the auth request.
     * Serialized as "client_app_id".
     */
    @SerializedName("client_app_id")
    private final String mClientAppId;

    /**
     * The name of the calling application.
     * Serialized as "app_name".
     */
    @SerializedName("app_name")
    private final String mAppName;

    /**
     * The version of the calling application.
     * Serialized as "app_ver".
     */
    @SerializedName("app_ver")
    private final String mAppVer;

    /**
     * The resource (audience) being requested.
     * Serialized as "resource".
     */
    @SerializedName("resource")
    private final String mResource;

    /**
     * The scope(s) being requested.
     * Serialized as "scope".
     */
    @SerializedName("scope")
    private final String mScope;

    /**
     * Whether a broker application was used for this auth flow.
     * Serialized as "msal_broker_app_used".
     */
    @SerializedName("msal_broker_app_used")
    @Nullable
    private final Boolean mMsalBrokerAppUsed;

    /**
     * The version of MSAL used.
     * Serialized as "msal_version".
     */
    @SerializedName("msal_version")
    private final String mMsalVersion;

    /**
     * The version of the broker app used. May be null if no broker was used.
     * Serialized as "broker_version".
     */
    @SerializedName("broker_version")
    private final String mBrokerVersion;

    /**
     * The version of the underlying SDK.
     * Serialized as "sdk_version".
     */
    @SerializedName("sdk_version")
    private final String mSdkVersion;

    /**
     * The onboarding mode. Expected values: brokered, non-brokered.
     * Serialized as "onboarding_mode".
     */
    @SerializedName("onboarding_mode")
    private final String mOnboardingMode;

    /**
     * The ordered list of UX flows presented to the user.
     * Serialized as "ux_flow_used".
     */
    @SerializedName("ux_flow_used")
    private final List<String> mUxFlowUsed;

    /**
     * Whether remediation was needed to complete the auth flow.
     * Serialized as "remediation_needed".
     */
    @SerializedName("remediation_needed")
    @Nullable
    private final Boolean mRemediationNeeded;

    /**
     * The list of blocking error codes encountered during the flow.
     * Serialized as "blocking_errors".
     */
    @SerializedName("blocking_errors")
    private final List<String> mBlockingErrors;

    /**
     * The ordered list of {@link OnboardingStep} objects representing each step taken.
     * Serialized as "steps_list".
     */
    @SerializedName("steps_list")
    private final List<OnboardingStep> mStepsList;

    /**
     * The total number of steps taken in this onboarding session.
     * Serialized as "step_count".
     */
    @SerializedName("step_count")
    @Nullable
    private final Integer mStepCount;

    /**
     * The last blocking error encountered. Nullable.
     * Serialized as "last_blocking_error".
     */
    @SerializedName("last_blocking_error")
    @Nullable
    private final String mLastBlockingError;

    /**
     * The last loaded domain during the auth flow. Nullable.
     * Serialized as "last_loaded_domain".
     */
    @SerializedName("last_loaded_domain")
    @Nullable
    private final String mLastLoadedDomain;

    /**
     * The last completed step identifier. Nullable.
     * Serialized as "last_completed_step".
     */
    @SerializedName("last_completed_step")
    @Nullable
    private final String mLastCompletedStep;

    /**
     * Epoch millisecond timestamp when MDM enrollment started. Nullable.
     * Serialized as "mdm_enrollment_start_ts".
     */
    @SerializedName("mdm_enrollment_start_ts")
    @Nullable
    private final Long mMdmEnrollmentStartTs;

    /**
     * Epoch millisecond timestamp when MDM enrollment was resumed. Nullable.
     * Serialized as "mdm_enrollment_resume_ts".
     */
    @SerializedName("mdm_enrollment_resume_ts")
    @Nullable
    private final Long mMdmEnrollmentResumeTs;

    /**
     * Epoch millisecond timestamp when compliance remediation started. Nullable.
     * Serialized as "compliance_remediation_start_ts".
     */
    @SerializedName("compliance_remediation_start_ts")
    @Nullable
    private final Long mComplianceRemediationStartTs;

    /**
     * Epoch millisecond timestamp when compliance remediation was resumed. Nullable.
     * Serialized as "compliance_remediation_resume_ts".
     */
    @SerializedName("compliance_remediation_resume_ts")
    @Nullable
    private final Long mComplianceRemediationResumeTs;

    /**
     * The account profile type. Expected values: personal, work.
     * Serialized as "profile".
     */
    @SerializedName("profile")
    private final String mProfile;

    /**
     * Epoch millisecond timestamp when the workplace join (WPJ) credential was created. Nullable.
     * Serialized as "wp_creation_ts".
     */
    @SerializedName("wp_creation_ts")
    @Nullable
    private final Long mWpCreationTs;
}
