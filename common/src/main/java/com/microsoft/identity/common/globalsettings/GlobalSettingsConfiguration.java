//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.globalsettings;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.logging.LoggerConfiguration;
import com.microsoft.identity.common.internal.telemetry.TelemetryConfiguration;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;

import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.AUTHORIZATION_IN_CURRENT_TASK;
import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.BROWSER_SAFE_LIST;
import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.HANDLE_TASKS_WITH_NULL_TASKAFFINITY;
import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.LOGGING;
import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.POWER_OPT_CHECK_FOR_NETWORK_REQUEST_ENABLED;
import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.REQUIRED_BROKER_PROTOCOL_VERSION;
import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.TELEMETRY;
import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.WEB_VIEW_ZOOM_CONTROLS_ENABLED;
import static com.microsoft.identity.common.globalsettings.GlobalSettingsConfiguration.SerializedNames.WEB_VIEW_ZOOM_ENABLED;
import java.util.List;

import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;

public class GlobalSettingsConfiguration {
    @SuppressWarnings("PMD")
    private static final String TAG = GlobalSettingsConfiguration.class.getSimpleName();

    public static final class SerializedNames {
        public static final String CLIENT_ID = "client_id";
        public static final String REDIRECT_URI = "redirect_uri";
        public static final String AUTHORITIES = "authorities";
        public static final String AUTHORIZATION_USER_AGENT = "authorization_user_agent";
        public static final String HTTP = "http";
        public static final String LOGGING = "logging";
        public static final String MULTIPLE_CLOUDS_SUPPORTED = "multiple_clouds_supported";
        public static final String USE_BROKER = "broker_redirect_uri_registered";
        public static final String ENVIRONMENT = "environment";
        public static final String REQUIRED_BROKER_PROTOCOL_VERSION = "minimum_required_broker_protocol_version";
        public static final String TELEMETRY = "telemetry";
        public static final String BROWSER_SAFE_LIST = "browser_safelist";
        public static final String ACCOUNT_MODE = "account_mode";
        public static final String CLIENT_CAPABILITIES = "client_capabilities";
        public static final String WEB_VIEW_ZOOM_CONTROLS_ENABLED = "web_view_zoom_controls_enabled";
        public static final String WEB_VIEW_ZOOM_ENABLED = "web_view_zoom_enabled";
        public static final String POWER_OPT_CHECK_FOR_NETWORK_REQUEST_ENABLED = "power_opt_check_for_network_req_enabled";
        public static final String HANDLE_TASKS_WITH_NULL_TASKAFFINITY = "handle_null_taskaffinity";
        public static final String AUTHORIZATION_IN_CURRENT_TASK = "authorization_in_current_task";
    }

    /**
     * The currently configured LoggerConfiguration for use with the PublicClientApplication.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(LOGGING)
    private LoggerConfiguration mLoggerConfiguration;

    /**
     * The minimum required broker protocol version number.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(REQUIRED_BROKER_PROTOCOL_VERSION)
    private String mRequiredBrokerProtocolVersion;

    /**
     * The list of safe browsers.
     */
    @SerializedName(BROWSER_SAFE_LIST)
    @Getter
    @Accessors(prefix = "m")
    private List<BrowserDescriptor> mBrowserSafeList;

    /**
     * The currently configured {@link TelemetryConfiguration} for the PublicClientApplications.
     */
    @Getter
    @Accessors(prefix = "m")
    @SerializedName(TELEMETRY)
    private TelemetryConfiguration mTelemetryConfiguration;

    @Setter
    @SerializedName(WEB_VIEW_ZOOM_CONTROLS_ENABLED)
    private Boolean webViewZoomControlsEnabled;

    @Setter
    @SerializedName(WEB_VIEW_ZOOM_ENABLED)
    private Boolean webViewZoomEnabled;

    @Setter
    @SerializedName(POWER_OPT_CHECK_FOR_NETWORK_REQUEST_ENABLED)
    private Boolean powerOptCheckEnabled;

    @SerializedName(HANDLE_TASKS_WITH_NULL_TASKAFFINITY)
    private Boolean handleNullTaskAffinity;

    /**
     * Controls whether interactive authorization activities (Browser, Embedded, Broker) are
     * launched in the task associated with the activity provided as a parameter to interactive requests
     * The current default behavior of common is to launch the activity in a new Task.
     * This creates effectively 2 task stacks (which can appear as 2 windows in multi-window configurations)
     * The 2 task stacks allows for unexpected user experience when navigating away for authorization UI
     * when the authorization is still in process.
     * Current default as of MSAL 2.0.12 is to use a new task
     */
    @Setter
    @SerializedName(AUTHORIZATION_IN_CURRENT_TASK)
    private Boolean authorizationInCurrentTask;

    /**
     * Determines whether refresh_in feature is enabled by client.
     * Default value for this is false.
     */
    @Setter
    @Getter
    private boolean refreshInEnabled = false;

    public Boolean isWebViewZoomControlsEnabled() {
        return webViewZoomControlsEnabled;
    }

    public Boolean isWebViewZoomEnabled() {
        return webViewZoomEnabled;
    }

    public Boolean isPowerOptCheckEnabled() {
        return powerOptCheckEnabled;
    }

    public Boolean isHandleNullTaskAffinityEnabled() {
        return handleNullTaskAffinity;
    }

    public Boolean isAuthorizationInCurrentTask() {
        return authorizationInCurrentTask;
    }

    void mergeConfiguration(final @NonNull GlobalSettingsConfiguration globalConfig) {
        this.mTelemetryConfiguration = globalConfig.getTelemetryConfiguration() == null ? this.mTelemetryConfiguration : globalConfig.getTelemetryConfiguration();
        this.mRequiredBrokerProtocolVersion = globalConfig.getRequiredBrokerProtocolVersion() == null ? this.mRequiredBrokerProtocolVersion : globalConfig.getRequiredBrokerProtocolVersion();
        this.mBrowserSafeList = globalConfig.getBrowserSafeList() == null ? this.mBrowserSafeList : globalConfig.getBrowserSafeList();
        this.mLoggerConfiguration = globalConfig.getLoggerConfiguration() == null ? this.mLoggerConfiguration : globalConfig.getLoggerConfiguration();
        this.webViewZoomControlsEnabled = globalConfig.isWebViewZoomControlsEnabled() == null ? this.webViewZoomControlsEnabled : globalConfig.isWebViewZoomControlsEnabled();
        this.webViewZoomEnabled = globalConfig.isWebViewZoomEnabled() == null ? this.webViewZoomEnabled : globalConfig.isWebViewZoomEnabled();
        this.powerOptCheckEnabled = globalConfig.isPowerOptCheckEnabled() == null ? this.powerOptCheckEnabled : globalConfig.isPowerOptCheckEnabled();
        this.handleNullTaskAffinity = globalConfig.isHandleNullTaskAffinityEnabled() == null ? this.handleNullTaskAffinity : globalConfig.isHandleNullTaskAffinityEnabled();
        this.authorizationInCurrentTask = globalConfig.isAuthorizationInCurrentTask() == null ? this.authorizationInCurrentTask : globalConfig.isAuthorizationInCurrentTask();
    }
}
