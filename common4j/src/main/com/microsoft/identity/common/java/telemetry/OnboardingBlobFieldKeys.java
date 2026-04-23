// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.telemetry;

/**
 * JSON field keys for the onboarding telemetry blob.
 * All keys use snake_case to match MATS convention — EntityStore prepends "mo_"
 * to produce the final MATS column name (e.g., "blocking_errors" → "mo_blocking_errors").
 * Seed creation + aggregation keys come from OnboardingBlobConstants (Djinni-generated).
 */
public final class OnboardingBlobFieldKeys {
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

    private OnboardingBlobFieldKeys() {} // non-instantiable
}
