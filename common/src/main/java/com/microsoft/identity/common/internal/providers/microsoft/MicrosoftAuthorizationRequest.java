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
package com.microsoft.identity.common.internal.providers.microsoft;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.UUID;

public abstract class MicrosoftAuthorizationRequest extends AuthorizationRequest{
    /* Constants */
    public static final String ENCODING_UTF8 = "UTF_8";
    public static final String CODE_CHALLENGE = "code_challenge";
    public static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    public static final String QUERY_PROMPT = "prompt";
    public static final String QUERY_PROMPT_VALUE = "login";
    public static final String LOGIN_HINT = "login_hint";

    public static final String LIB_ID_PLATFORM = "x-client-SKU";
    public static final String LIB_ID_VERSION = "x-client-Ver";
    public static final String LIB_ID_CPU = "x-client-CPU";
    public static final String LIB_ID_OS_VER = "x-client-OS";
    public static final String LIB_ID_DM = "x-client-DM";

    /**
     * Required.
     */
    private URL mAuthority;
    /**
     * Required value.
     *
     * Passed in from ADAL/MSAL after authority verification.
     */
    private String mAuthorizationEndpoint;
    /**
     * Can be used to pre-fill the username/email address field of the sign-in page for the user, if you know their username ahead of time.
     */
    private String mLoginHint;
    /**
     * Correlation ID.
     */
    private UUID mCorrelationId;
    /**
     *  Used to secure authorization code grants via Proof Key for Code Exchange (PKCE) from a native client.
     */
    private PkceChallenge mPkceChallenge;
    /**
     * Extra query parameters.
     */
    private String mExtraQueryParam;
    /**
     * The version of the calling library.
     */
    private String mLibraryVersion;


    public URL getAuthority() {
        return mAuthority;
    }

    public void setAuthority(final URL authority) {
        mAuthority = authority;
    }

    public String getAuthorizationEndpoint() {
        return mAuthorizationEndpoint;
    }

    public void setAuthorizationEndpoint(final String authorizationEndpoint) {
        mAuthorizationEndpoint = authorizationEndpoint;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public void setLoginHint(final String loginHint) {
        mLoginHint = loginHint;
    }

    public UUID getCorrelationId() {
        return mCorrelationId;
    }

    public void setCorrelationId(final UUID correlationId) {
        mCorrelationId = correlationId;
    }

    public PkceChallenge getPkceChallenge() {
        return mPkceChallenge;
    }

    public void setPkceChallenge(final PkceChallenge pkceChallenge) {
        mPkceChallenge = pkceChallenge;
    }

    public String getExtraQueryParam() {
        return mExtraQueryParam;
    }

    public void setExtraQueryParam(final String extraQueryParam) {
        mExtraQueryParam = extraQueryParam;
    }

    public String getLibraryVersion() {
        return mLibraryVersion;
    }

    public void setLibraryVersion(final String libraryVersion) {
        mLibraryVersion = libraryVersion;
    }

    /**
     * Return the start URL to load in the web view.
     * @return String of start URL.
     * @throws UnsupportedEncodingException
     * @throws ClientException
     */
    public abstract String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException;
}
