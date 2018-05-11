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
package com.microsoft.identity.common.internal.providers.oauth2;

import com.google.gson.annotations.SerializedName;

/**
 * A class holding the state of the Token Request (oAuth2)
 * OAuth2 Spec: https://tools.ietf.org/html/rfc6749#section-4.1.3
 * OAuth2 Client Authentication: https://tools.ietf.org/html/rfc7521#section-4.2
 * This should include all fo the required parameters of the token request for oAuth2
 * This should provide an extension point for additional parameters to be set
 * <p>
 * Includes support for client assertions per the specs:
 * https://tools.ietf.org/html/rfc7521
 * https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-protocols-oauth-client-creds
 */
public class TokenRequest {

    @SerializedName("grant_type")
    private String mGrantType;

    @SerializedName("code")
    private String mCode;

    @SerializedName("redirect_uri")
    private String mRedirectUri;

    @SerializedName("client_id")
    private String mClientId;

    @SerializedName("client_secret")
    private String mClientSecret;

    @SerializedName("client_assertion_type")
    private String mClientAssertionType;

    @SerializedName("client_assertion")
    private String mClientAssertion;

    @SerializedName("scope")
    private String mScope;


    public String getCode() {
        return mCode;
    }

    public void setCode(String mCode) {
        this.mCode = mCode;
    }

    public String getRedirectUri() {
        return mRedirectUri;
    }

    public void setRedirectUri(String mRedirectUri) {
        this.mRedirectUri = mRedirectUri;
    }

    public String getClientId() {
        return mClientId;
    }

    public void setClientId(String mClientId) {
        this.mClientId = mClientId;
    }

    public String getGrantType() {
        return mGrantType;
    }

    public void setGrantType(String mGrantType) {
        this.mGrantType = mGrantType;
    }

    public void setClientSecret(String clientSecret) {
        this.mClientSecret = clientSecret;
    }

    public String getClientSecret() {
        return mClientSecret;
    }


    public String getClientAssertionType() {
        return mClientAssertionType;
    }

    public void setClientAssertionType(String clientAssertionType) {
        this.mClientAssertionType = clientAssertionType;
    }

    public String getClientAssertion() {
        return mClientAssertion;
    }

    public void setClientAssertion(String clientAssertion) {
        this.mClientAssertion = clientAssertion;
    }

    public String getScope() {
        return mScope;
    }

    public void setScope(String scope) {
        this.mScope = scope;
    }
}
