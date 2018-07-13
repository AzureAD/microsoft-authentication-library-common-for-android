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

import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Set;

/**
 * A class holding the state of the Authorization Request (OAuth 2.0).
 * https://tools.ietf.org/html/rfc6749#section-4.1.1
 * This should include all fo the required parameters of the authorization request for oAuth2
 * This should provide an extension point for additional parameters to be set
 */
public abstract class AuthorizationRequest implements Serializable {
    private static final long serialVersionUID = 1L;

    /**
     * A required value and must be set to "code".
     */
    private String mResponseType;

    /**
     * A required value.
     * <p>
     * The client identifier as assigned by the authorization server, when the client was registered.
     */
    private String mClientId;

    /**
     * Redirect URLs are a critical part of the OAuth flow. After a user successfully authorizes an
     * application, the authorization server will redirect the user back to the application with
     * either an authorization code or access token in the URL.
     */
    private String mRedirectUri;

    /**
     * A recommended value.
     * <p>
     * A value included in the request that will also be returned in the token response.
     * It can be a string of any content that you wish. A randomly generated unique value is
     * typically used for preventing cross-site request forgery attacks. The value can also
     * encode information about the user's state in the app before the authentication request
     * occurred, such as the page or view they were on.
     */
    private String mState;

    /**
     * Scopes scopes that you want the user to consent to is required for V2 auth request.
     */
    private Set<String> mScope;

    /**
     * Constructor of AuthorizationRequest.
     */
    public AuthorizationRequest(final String responseType,
                                @NonNull final String clientId,
                                final String redirectUri,
                                final String state,
                                final Set<String> scope) {
        //validate client id
        if (StringUtil.isEmpty(clientId)) {
            throw new IllegalArgumentException("clientId is empty.");
        }

        mResponseType = responseType;
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mState = state;
        mScope = scope;
    }

    /**
     * Return the start URL to load in the web view.
     *
     * @return String of start URL.
     * @throws UnsupportedEncodingException
     * @throws ClientException
     */
    public abstract String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException;

    /**
     * @return mResponseType of the authorization request.
     */
    public String getResponseType() {
        return mResponseType;
    }

    /**
     * @param responseType response type of the authorization request.
     */
    public void setResponseType(final String responseType) {
        mResponseType = responseType;
    }

    /**
     * @return mClientId of the authorization request.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * @param clientId client ID of the authorization request.
     */
    public void setClientId(final String clientId) {
        mClientId = clientId;
    }

    /**
     * @return mRedirectUri of the authorization request.
     */
    public String getRedirectUri() {
        return mRedirectUri;
    }

    /**
     * @param redirectUri redirect URI of the authorization request.
     */
    public void setRedirectUri(final String redirectUri) {
        mRedirectUri = redirectUri;
    }

    /**
     * @return mState of the authorization request.
     */
    public String getState() {
        return mState;
    }

    /**
     * @param state state of the authorization request.
     */
    public void setState(final String state) {
        mState = state;
    }

    public Set<String> getScope() {
        return mScope;
    }

    public void setScope(final Set<String> scope) {
        mScope = new HashSet<>(scope);
    }
}
