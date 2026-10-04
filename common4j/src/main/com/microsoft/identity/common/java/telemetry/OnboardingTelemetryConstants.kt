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
package com.microsoft.identity.common.java.telemetry

/**
 * JSON field keys for the onboarding telemetry blob.
 * All keys use snake_case to match MATS convention — EntityStore prepends "mo_"
 * to produce the final MATS column name (e.g., "blocking_errors" → "mo_blocking_errors").
 * Seed creation + aggregation keys come from OnboardingBlobConstants (Djinni-generated).
 */
object OnboardingTelemetryConstants {
    // Field keys for populated blob (written by OnboardingTelemetryRecorder, read by EntityStore with mo_ prefix)
    const val BLOCKING_ERRORS = "blocking_errors"
    const val LAST_BLOCKING_ERROR = "last_blocking_error"
    const val LAST_LOADED_DOMAIN = "last_loaded_domain"
    const val LAST_COMPLETED_STEP = "last_completed_step"
    const val PROFILE = "profile"
    const val UX_FLOW_USED = "ux_flow_used"

    // Step ID values not used in C++ aggregation (no derived duration metric computed from these)
    const val STEP_AUTHENTICATION_STARTED = "AuthenticationStarted"
    const val STEP_ACCOUNT_SELECTION_STARTED = "AccountSelectionStarted"
    const val STEP_CREDENTIAL_ENTRY_COMPLETED = "CredentialEntryCompleted"
    const val STEP_PASSKEY_AUTH_STARTED = "PasskeyAuthStarted"
    const val STEP_CERT_BASED_AUTH_STARTED = "CertBasedAuthStarted"

    // MFA / Strong Auth Setup
    const val STEP_STRONG_AUTH_SETUP_STARTED = "StrongAuthSetupStarted"
    const val STEP_STRONG_AUTH_SETUP_COMPLETED = "StrongAuthSetupCompleted"
    const val STEP_AUTHENTICATOR_MFA_LINKING_STARTED = "AuthenticatorMfaLinkingStarted"

    // Conditional Access Block & Remediation
    const val STEP_CA_BLOCK_RECEIVED = "CABlockReceived"
    const val STEP_INTERRUPT_FLOW_STARTED = "InterruptFlowStarted"
    const val STEP_CONSENT_PROMPT_SHOWN = "ConsentPromptShown"
    const val STEP_TERMS_OF_USE_SHOWN = "TermsOfUseShown"
    const val STEP_PASSWORD_RESET_REQUIRED = "PasswordResetRequired"

    // Broker Installation
    const val STEP_BROKER_INSTALL_PROMPTED = "BrokerInstallPrompted"
    const val STEP_BROKER_INSTALL_PROMPTED_FOR_MDM = "BrokerInstallPromptedForMDM"

    // Device Registration (WPJ)
    const val STEP_DEVICE_REGISTRATION_STARTED = "DeviceRegistrationStarted"
    const val STEP_DEVICE_REGISTRATION_COMPLETED = "DeviceRegistrationCompleted"
    const val STEP_DEVICE_REGISTRATION_UPGRADE_STARTED = "DeviceRegistrationUpgradeStarted"

    // MDM Enrollment (PP → WP transition)
    const val STEP_MDM_ENROLLMENT_STARTED = "MDMEnrollmentStarted"
    const val STEP_COMPANY_PORTAL_LAUNCHED = "CompanyPortalLaunched"
    const val STEP_WEB_CP_ENROLLMENT_STARTED = "WebCpEnrollmentStarted"
    const val STEP_GOOGLE_ENROLLMENT_STARTED = "GoogleEnrollmentStarted"

    // Intune App Protection (MAM)
    const val STEP_INTUNE_APP_PROTECTION_REQUIRED = "IntuneAppProtectionRequired"

    // Compliance Remediation
    const val STEP_COMPLIANCE_REMEDIATION_STARTED = "ComplianceRemediationStarted"
    const val STEP_COMPLIANCE_REMEDIATION_COMPLETED = "ComplianceRemediationCompleted"

    // Token Acquisition & Completion
    const val STEP_PRT_ACQUIRED = "PrtAcquired"
    const val STEP_TOKEN_ISSUED = "TokenIssued"
    const val STEP_FLOW_COMPLETED = "FlowCompleted"

    // Termination (Non-Success)
    const val STEP_USER_CANCELED = "UserCanceled"
    const val STEP_AUTHORIZATION_TIMED_OUT = "AuthorizationTimedOut"

    // Blocking error values — must match C++ hardcoded strings in InteractiveRequest.cpp
    const val BLOCKING_ERROR_BROKER_INSTALL = "BROKER_INSTALLATION_TRIGGERED"
    const val BLOCKING_ERROR_MDM_FLOW = "MDM_FLOW"

    // Device-registration blocking errors — one per BrokerExceptionClassifier.Category
    // (see broker4j BrokerExceptionClassifier + InteractiveRequestAcquireTokenErrorHandler).
    const val BLOCKING_ERROR_DEVICE_REGISTRATION_NEEDED = "DEVICE_REGISTRATION_NEEDED"
    const val BLOCKING_ERROR_STRONG_DEVICE_REGISTRATION_NEEDED = "STRONG_DEVICE_REGISTRATION_NEEDED"
    const val BLOCKING_ERROR_INSUFFICIENT_DEVICE_REGISTRATION = "INSUFFICIENT_DEVICE_REGISTRATION"

    // Platform-specific values
    const val PROFILE_USER = "userProfile"
    const val PROFILE_WORK = "workProfile"
}
