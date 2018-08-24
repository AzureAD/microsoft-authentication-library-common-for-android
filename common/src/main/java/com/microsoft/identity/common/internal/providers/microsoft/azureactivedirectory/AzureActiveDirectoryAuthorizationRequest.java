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
package com.microsoft.identity.common.internal.providers.microsoft.azureactivedirectory;

import android.os.Build;
import android.support.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AzureActiveDirectoryAuthorizationRequest extends MicrosoftAuthorizationRequest {
    private static final String TAG = AzureActiveDirectoryAuthorizationRequest.class.getSimpleName();

    /* Constants */
    private static final String RESOURCE = "resource";
    private static final String CLIENT_REQUEST_ID = "client-request-id";
    private static final String SCOPE_OPENID_VALUE = "openid";
    private static final String PLATFORM_VALUE = "ADAL.Android";
    private static final String QUERY_PROMPT_REFRESH_SESSION_VALUE = "refresh_session";

    /**
     * The App ID URI of the target web API.
     * This is required in one of either the authorization or token requests.
     * To ensure fewer authentication prompts place it in the authorization request to
     * ensure consent is received from the user.
     */
    private String mResource;
    //TODO The microsoft doc is different with V1 has currently.
    /**
     * Optional. Indicate the type of user interaction that is required.
     */
    private String mPrompt;

    private String mClaimsChallenge;

    public static final class Prompt {
        /**
         * Acquire token will prompt the user for credentials only when necessary.
         */
        public static final String AUTO = "";

        /**
         * The user will be prompted for credentials even if it is available in the
         * cache or in the form of refresh token. New acquired access token and
         * refresh token will be used to replace previous value. If Settings
         * switched to Auto, new request will use this latest token from cache.
         */
        public static final String ALWAYS = "login";

        /**
         * Re-authorizes (through displaying webview) the resource usage, making
         * sure that the resulting access token contains the updated claims. If user
         * logon cookies are available, the user will not be asked for credentials
         * again and the logon dialog will dismiss automatically. This is equivalent
         * to passing prompt=refresh_session as an extra query parameter during
         * the authorization.
         */
        public static final String REFRESH_SESSION = "refresh_session";

        /**
         * If Azure Authenticator or Company Portal is installed, this flag will have
         * the broker app force the prompt behavior, otherwise it will be same as Always.
         * If using embedded flow, please keep using Always, if FORCE_PROMPT is set for
         * embedded flow, the sdk will re-intepret it to Always.
         */
        public static final String FORCE_PROMPT = "login";
    }

    private AzureActiveDirectoryAuthorizationRequest(final Builder builder) {
        super(builder);
        mResource = builder.mResource;
        mPrompt = builder.mPrompt;
        mClaimsChallenge = builder.mClaimsChallenge;
    }

    public static final class Builder extends MicrosoftAuthorizationRequest.Builder {
        /**
         * The App ID URI of the target web API.
         * This is required in one of either the authorization or token requests.
         * To ensure fewer authentication prompts place it in the authorization request to
         * ensure consent is received from the user.
         */
        private String mResource;

        //TODO The microsoft doc is different with V1 has currently.
        /**
         * Optional. Indicate the type of user interaction that is required.
         */
        private String mPrompt;

        private String mClaimsChallenge;

        public Builder(@NonNull final String clientId,
                       @NonNull final String redirectUri,
                       @NonNull final URL authority,
                       @NonNull final String resource) {
            super(clientId, redirectUri, authority);
            setResource(resource);
        }

        public Builder setResource(final String resource) {
            mResource = resource;
            return this;
        }

        public Builder setPrompt(final String prompt) {
            mPrompt = prompt;
            return this;
        }

        public Builder setClaimsChallenge(final String claimsChallenge) {
            mClaimsChallenge = claimsChallenge;
            return this;
        }

        public AzureActiveDirectoryAuthorizationRequest build() {
            return new AzureActiveDirectoryAuthorizationRequest(this);
        }
    }

    public String getResource() {
        return mResource;
    }

    public String getPrompt() {
        return mPrompt;
    }

    public String getClaimsChallenge() {
        return mClaimsChallenge;
    }

    @Override
    public String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException {
        final String authorizationUrl = StringExtensions.appendQueryParameterToUrl(
                getAuthorizationEndpoint(),
                createAuthorizationRequestParameters());
        Logger.infoPII(TAG, getCorrelationId().toString(), "Request uri to authorize endpoint is: " + authorizationUrl);
        return authorizationUrl;
    }

    private void addDiagnosticParameters(@NonNull final Map<String, String> requestParameters) {
        // append device and platform info in the query parameters
        requestParameters.put(LIB_ID_PLATFORM, PLATFORM_VALUE);
        requestParameters.put(LIB_ID_VERSION, getLibraryVersion());
        requestParameters.put(LIB_ID_OS_VER, String.valueOf(Build.VERSION.SDK_INT));
        requestParameters.put(LIB_ID_DM, android.os.Build.MODEL);
    }

//    private void addPromptParameter(@NonNull final Map<String, String> requestParameters) {
//        // Setting prompt behavior to always will skip the cookies for webview.
//        // It is added to authorization url.
//        if (mPromptBehavior == AzureActiveDirectoryPromptBehavior.ALWAYS) {
//            requestParameters.put(QUERY_PROMPT, QUERY_PROMPT_VALUE);
//        } else if (mPromptBehavior == AzureActiveDirectoryPromptBehavior.REFRESH_SESSION) {
//            requestParameters.put(QUERY_PROMPT, QUERY_PROMPT_REFRESH_SESSION_VALUE);
//        }
//    }

    private void addUserInfoParameter(@NonNull final Map<String, String> requestParameters) {
        if (!StringExtensions.isNullOrBlank(getLoginHint())) {
            requestParameters.put(LOGIN_HINT, getLoginHint());
        }
    }

    private void addPkceChallengeParameters(@NonNull final Map<String, String> requestParameters) {
        if (null != getPkceChallenge()) {
            requestParameters.put(CODE_CHALLENGE, getPkceChallenge().getCodeChallenge());
            // The method used to encode the code_verifier for the code_challenge parameter.
            requestParameters.put(CODE_CHALLENGE_METHOD, getPkceChallenge().getCodeChallengeMethod());
        }
    }

    /**
     * Add the extra query parameters into the request parameter map.
     * And append haschrome=1 if developer does not pass as extra qp.
     */
    private void addExtraQueryParameters(@NonNull final Map<String, String> requestParameters) throws ClientException {
        if (StringExtensions.isNullOrBlank(getExtraQueryParam())
                || !getExtraQueryParam().contains(AuthenticationConstants.OAuth2.HAS_CHROME)) {
            requestParameters.put(AuthenticationConstants.OAuth2.HAS_CHROME, "1");
        } else {
            appendExtraQueryParameters(getExtraQueryParam(), requestParameters);
        }
    }

    private void addClaimsChallengeParameters(@NonNull final Map<String, String> requestParameters) throws ClientException {
        // Claims challenge are opaque to the sdk, we're not going to do any merging if both extra qp and claims parameter
        // contain it. Also, if developer sends it in both places, server will fail it.
        if (!StringExtensions.isNullOrBlank(mClaimsChallenge)) {
            requestParameters.put(AuthenticationConstants.OAuth2.CLAIMS, mClaimsChallenge);
        }
    }

    private Map<String, String> createAuthorizationRequestParameters() throws UnsupportedEncodingException, ClientException {
        final Map<String, String> requestParameters = new HashMap<>();

        requestParameters.put(AuthenticationConstants.OAuth2.RESPONSE_TYPE, AuthenticationConstants.OAuth2.CODE);
        requestParameters.put(AuthenticationConstants.OAuth2.CLIENT_ID, getClientId());
        requestParameters.put(AuthenticationConstants.OAuth2.REDIRECT_URI, getRedirectUri());
        requestParameters.put(AuthenticationConstants.OAuth2.SCOPE, SCOPE_OPENID_VALUE);
        requestParameters.put(RESOURCE, mResource);
        requestParameters.put(AuthenticationConstants.OAuth2.STATE, getState());
        if (getCorrelationId() != null) {
            requestParameters.put(CLIENT_REQUEST_ID, getCorrelationId().toString());
        }

        addDiagnosticParameters(requestParameters);
        //addPromptParameter(requestParameters);
        addUserInfoParameter(requestParameters);
        addPkceChallengeParameters(requestParameters);
        addExtraQueryParameters(requestParameters);
        addClaimsChallengeParameters(requestParameters);

        return requestParameters;
    }

    @Override
    public String getAuthorizationEndpoint() {
        return null;
    }

}
