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

import android.net.Uri;
import android.os.Build;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

/**
 * A class holding the state of the Authorization Request (OAuth 2.0).
 * https://tools.ietf.org/html/rfc6749#section-4.1.1
 * This should include all fo the required parameters of the authorization request for oAuth2
 * This should provide an extension point for additional parameters to be set
 */
public abstract class AuthorizationRequest {
    private String mResponseType;
    private String mClientId;
    private String mRedirectUri;
    private String mScope;
    private String mState;

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
     * @return mScope of the authorization request.
     */
    public String getScope() {
        return mScope;
    }

    /**
     * @param scope scope of the authorization request.
     */
    public void setScope(final String scope) {
        mScope = scope;
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

    //CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "AuthorizationRequest{" +
                "mResponseType='" + mResponseType + '\'' +
                ", mClientId='" + mClientId + '\'' +
                ", mRedirectUri='" + mRedirectUri + '\'' +
                ", mScope='" + mScope + '\'' +
                ", mState='" + mState + '\'' +
                '}';
    }
    //CHECKSTYLE:ON

}
