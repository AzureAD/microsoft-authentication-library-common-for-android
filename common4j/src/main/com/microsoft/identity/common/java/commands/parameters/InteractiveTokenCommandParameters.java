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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

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

    @Expose()
    private final List<String> extraScopesToConsent;

    public boolean getHandleNullTaskAffinity(){
        return handleNullTaskAffinity;
    }

    public List<Map.Entry<String, String>> getExtraQueryStringParameters() {
        return this.extraQueryStringParameters == null ? null : new ArrayList<>(this.extraQueryStringParameters);
    }

    public List<String> getExtraScopesToConsent() {
        return this.extraScopesToConsent == null ? null : new ArrayList<>(this.extraScopesToConsent);
    }

    public List<BrowserDescriptor> getBrowserSafeList() {
        return this.browserSafeList == null ? null : new ArrayList<>(this.browserSafeList);
    }
}
