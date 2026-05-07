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
    const val STEP_CREDENTIAL_ENTRY_COMPLETED = "CredentialEntryCompleted"
    const val STEP_BROKER_INSTALL_PROMPTED = "BrokerInstallPrompted"
    const val STEP_BROKER_INSTALL_PROMPTED_FOR_MDM = "BrokerInstallPromptedForMDM"
    const val STEP_DEVICE_REGISTRATION_STARTED = "DeviceRegistrationStarted"
    const val STEP_DEVICE_REGISTRATION_COMPLETED = "DeviceRegistrationCompleted"
    const val STEP_FLOW_COMPLETED = "FlowCompleted"

    // Blocking error values — must match C++ hardcoded strings in InteractiveRequest.cpp
    const val BLOCKING_ERROR_BROKER_INSTALL = "BROKER_INSTALLATION_TRIGGERED"
    const val BLOCKING_ERROR_MDM_FLOW = "MDM_FLOW"

    // Platform-specific values
    const val PROFILE_USER = "userProfile"
    const val PROFILE_WORK = "workProfile"
}
