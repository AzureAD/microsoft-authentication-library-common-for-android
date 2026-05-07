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

/**
 * JSON field keys for the onboarding telemetry blob.
 * All keys use snake_case to match MATS convention — EntityStore prepends "mo_"
 * to produce the final MATS column name (e.g., "blocking_errors" → "mo_blocking_errors").
 * Seed creation + aggregation keys come from OnboardingBlobConstants (Djinni-generated).
 */
public final class OnboardingTelemetryConstants {
    // Field keys for populated blob (written by OnboardingTelemetryRecorder, read by EntityStore with mo_ prefix)
    public static final String BLOCKING_ERRORS = "blocking_errors";
    public static final String LAST_BLOCKING_ERROR = "last_blocking_error";
    public static final String LAST_LOADED_DOMAIN = "last_loaded_domain";
    public static final String LAST_COMPLETED_STEP = "last_completed_step";
    public static final String PROFILE = "profile";
    public static final String UX_FLOW_USED = "ux_flow_used";

    // Step ID values not used in C++ aggregation (no derived duration metric computed from these)
    public static final String STEP_AUTHENTICATION_STARTED = "AuthenticationStarted";
    public static final String STEP_CREDENTIAL_ENTRY_COMPLETED = "CredentialEntryCompleted";
    public static final String STEP_BROKER_INSTALL_PROMPTED = "BrokerInstallPrompted";
    public static final String STEP_BROKER_INSTALL_PROMPTED_FOR_MDM = "BrokerInstallPromptedForMDM";
    public static final String STEP_DEVICE_REGISTRATION_STARTED = "DeviceRegistrationStarted";
    public static final String STEP_DEVICE_REGISTRATION_COMPLETED = "DeviceRegistrationCompleted";
    public static final String STEP_FLOW_COMPLETED = "FlowCompleted";

    // Blocking error values — must match C++ hardcoded strings in InteractiveRequest.cpp
    public static final String BLOCKING_ERROR_BROKER_INSTALL = "BROKER_INSTALLATION_TRIGGERED";
    public static final String BLOCKING_ERROR_MDM_FLOW = "MDM_FLOW";

    // Platform-specific values
    public static final String PROFILE_USER = "userProfile";
    public static final String PROFILE_WORK = "workProfile";

    private OnboardingTelemetryConstants() {} // non-instantiable
}
