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
package com.microsoft.identity.common.internal.ui.webview;

import androidx.annotation.NonNull;

/**
 * Contains telemetry property values for Device CA URL launch routing.
 */
final class DeviceCaUrlLaunchTelemetryProperties {
    private DeviceCaUrlLaunchTelemetryProperties() {
    }

    enum DeviceManagementOwner {
        NOT_EVALUATED("not_evaluated"),
        DEVICE_POLICY_MANAGER_UNAVAILABLE("device_policy_manager_unavailable"),
        COMPANY_PORTAL_PROFILE("company_portal_profile_owner"),
        GOOGLE_DPC_PROFILE("google_dpc_profile_owner"),
        GOOGLE_DPC_DEVICE("google_dpc_device_owner"),
        NONE("none");

        private final String mTelemetryValue;

        DeviceManagementOwner(@NonNull final String telemetryValue) {
            mTelemetryValue = telemetryValue;
        }

        @NonNull
        String getTelemetryValue() {
            return mTelemetryValue;
        }
    }

    enum AppLinkLaunchOutcome {
        HANDLER_NOT_FOUND("handler_not_found"),
        LAUNCH_SUCCEEDED("launch_succeeded"),
        LAUNCH_FAILED("launch_failed");

        private final String mTelemetryValue;

        AppLinkLaunchOutcome(@NonNull final String telemetryValue) {
            mTelemetryValue = telemetryValue;
        }

        @NonNull
        String getTelemetryValue() {
            return mTelemetryValue;
        }
    }

    enum DeviceCaUrlRoutingOutcome {
        LEGACY_COMPANY_PORTAL_SUCCEEDED("legacy_company_portal_launch_succeeded"),
        NATIVE_HANDOFF_SUCCEEDED("native_handoff_succeeded"),
        NATIVE_FAILED_APP_LINK_SUCCEEDED("native_handoff_failed_app_link_fallback_succeeded"),
        NATIVE_APP_LINK_FAILED_GENERIC_HTTPS_SUCCEEDED(
                "native_handoff_app_link_failed_generic_https_fallback_succeeded"),
        COMPATIBILITY_COMPANY_PORTAL_SUCCEEDED("compatibility_company_portal_launch_succeeded"),
        BROWSER_LAUNCH_SUCCEEDED("browser_launch_succeeded"),
        BROWSER_LAUNCH_FAILED("browser_launch_failed"),
        WEBVIEW_LOAD_SUCCEEDED("webview_load_succeeded"),
        WEBVIEW_LOAD_FAILED("webview_load_failed"),
        UNEXPECTED_ROUTING_FAILURE("unexpected_routing_failure");

        private final String mTelemetryValue;

        DeviceCaUrlRoutingOutcome(@NonNull final String telemetryValue) {
            mTelemetryValue = telemetryValue;
        }

        @NonNull
        String getTelemetryValue() {
            return mTelemetryValue;
        }
    }
}