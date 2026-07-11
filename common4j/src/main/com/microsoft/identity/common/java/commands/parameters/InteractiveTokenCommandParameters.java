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
package com.microsoft.identity.common.java.commands.parameters;

import com.google.gson.annotations.Expose;
import com.microsoft.identity.common.java.providers.oauth2.OpenIdConnectPromptParameter;
import com.microsoft.identity.common.java.ui.AuthorizationAgent;
import com.microsoft.identity.common.java.ui.BrowserDescriptor;
import com.microsoft.identity.common.java.ui.PreferredAuthMethod;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

import edu.umd.cs.findbugs.annotations.Nullable;

@Getter
@EqualsAndHashCode(callSuper = true)
@SuperBuilder(toBuilder = true)
public class InteractiveTokenCommandParameters extends TokenCommandParameters {

    private final transient List<BrowserDescriptor> browserSafeList;

    private final transient BrowserDescriptor preferredBrowser;

    private final PreferredAuthMethod preferredAuthMethod;
    
    private final transient HashMap<String, String> requestHeaders;

    private final boolean brokerBrowserSupportEnabled;

    @Expose()
    private final OpenIdConnectPromptParameter prompt;

    @Expose()
    private final AuthorizationAgent authorizationAgent;

    @Expose()
    private final boolean isWebViewZoomEnabled;

    @Expose()
    private final boolean isWebViewZoomControlsEnabled;

    @Expose()
    private final boolean handleNullTaskAffinity;

    private final List<Map.Entry<String, String>> extraQueryStringParameters;

    private final List<Map.Entry<String, String>> extraQueryStringParametersForWebApps;

    @Expose()
    private final List<String> extraScopesToConsent;

    /**
     * Transfer token to be used in the Account Transfer request.
     */
    private final String accountTransferToken;

    /**
     * Should suppress broker native account picker UX.
     */
    private final boolean suppressBrokerAccountPicker;

    /**
     * Onboarding telemetry seed JSON blob.
     * Passed through IPC to the broker for step recording and blocking error tracking.
     */
    @Nullable
    private final String onboardingSeedJson;

    /**
     * MAM-CA "Install CP -> Auto-Redirect -> Silent-Broker Retry" auto-resume (Android-only).
     * The broker-install (Play Store) URL captured from the eSTS Conditional-Access challenge
     * ({@code msauth://wpj/?...&app_link=...}, marked {@code intuneAppProtection=1}) and stamped by
     * the OneAuth Android glue onto the request. Non-null only on the Android MAM-CA auto-resume path.
     * When present and no broker is installed yet, the OneAuth broker path ({@code BrokerClient})
     * installs the broker, parks this in-flight request in-memory, and resumes it in a freshly
     * discovered broker context instead of failing the request back to the caller with a terminal
     * broker-installation error.
     */
    @Nullable
    private final String brokerInstallationUrl;

    public boolean getHandleNullTaskAffinity(){
        return handleNullTaskAffinity;
    }

    public List<Map.Entry<String, String>> getExtraQueryStringParameters() {
        return this.extraQueryStringParameters == null ? null : new ArrayList<>(this.extraQueryStringParameters);
    }

    public List<Map.Entry<String, String>> getExtraQueryStringParametersForWebApps() {
        return this.extraQueryStringParametersForWebApps == null ? null : new ArrayList<>(this.extraQueryStringParametersForWebApps);
    }

    public List<String> getExtraScopesToConsent() {
        return this.extraScopesToConsent == null ? null : new ArrayList<>(this.extraScopesToConsent);
    }

    public List<BrowserDescriptor> getBrowserSafeList() {
        return this.browserSafeList == null ? Collections.emptyList() : new ArrayList<>(this.browserSafeList);
    }
}
