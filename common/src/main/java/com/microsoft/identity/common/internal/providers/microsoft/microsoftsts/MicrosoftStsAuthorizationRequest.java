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
package com.microsoft.identity.common.internal.providers.microsoft.microsoftsts;

import android.os.Build;
import android.support.annotation.NonNull;
import android.support.customtabs.CustomTabsIntent;


import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class MicrosoftStsAuthorizationRequest extends MicrosoftAuthorizationRequest<MicrosoftStsAuthorizationRequest> {
    private static final String TAG = MicrosoftStsAuthorizationRequest.class.getSimpleName();

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 6545759826515911472L;

    /* Constants */
    private static final String CORRELATION_ID = "client-request-id";
    private static final String LOGIN_REQ = "login_req";
    private static final String DOMAIN_REQ = "domain_req";
    //TODO: Should this be in the request or in the oAuth strategy?
    //private static final String[] RESERVED_SCOPES = {"openid", SCOPE_PROFILE, "offline_access"};
    private static final String PLATFORM_VALUE = "MSAL.Android";

    /**
     * Indicates the type of user interaction that is required. The only valid values at this time are 'login', 'none', and 'consent'.
     */
    private String mPrompt;
    private String mUid;
    private String mUtid;
    private String mDisplayableId;
    private String mSliceParameters;

    // TODO private transient InstanceDiscoveryMetadata mInstanceDiscoveryMetadata;
    // TODO private boolean mIsExtendedLifetimeEnabled = false;

    public static final class Prompt {
        /**
         * AcquireToken will send prompt=select_account to the authorize endpoint. Shows a list of users from which can be
         * selected for authentication.
         */
        public static final String SELECT_ACCOUNT = "select_account";

        /**
         * AcquireToken will send prompt=login to the authorize endpoint.  The user will always be prompted for credentials by the service.
         */
        public static final String FORCE_LOGIN = "login";

        /**
         * AcquireToken will send prompt=consent to the authorize endpoint.  The user will be prompted to consent even if consent was granted before.
         */
        public static final String CONSENT = "consent";
    }


    private MicrosoftStsAuthorizationRequest(final Builder builder) {
        super(builder);

        mPrompt = builder.mPrompt;
        mUid = builder.mUid;
        mUtid = builder.mUtid;
        mDisplayableId = builder.mDisplayableId;
        mSliceParameters = builder.mSliceParameters;
    }

    public static final class Builder extends MicrosoftAuthorizationRequest.Builder {
        private String mPrompt;
        private String mUid;
        private String mUtid;
        private String mDisplayableId;
        private String mSliceParameters;

        public Builder(@NonNull final String clientId,
                       @NonNull final String redirectUri,
                       @NonNull final URL authority,
                       @NonNull final String scope,
                       @NonNull final String prompt,
                       @NonNull final PkceChallenge pkceChallenge, //pkceChallenge is required for v2 request.
                       @NonNull final String state) {
            super(clientId, redirectUri, authority);
            setScope(scope);
            setPrompt(prompt);
            setPkceChallenge(pkceChallenge);
            setState(state);
        }

        public Builder setPrompt(String prompt) {
            mPrompt = prompt;
            return this;
        }

        public Builder setUid(String uid) {
            mUid = uid;
            return this;
        }

        public Builder setUtid(String utid) {
            mUtid = utid;
            return this;
        }

        public Builder setDisplayableId(String displayableId) {
            mDisplayableId = displayableId;
            return this;
        }

        public Builder setSliceParameters(String sliceParameters) {
            mSliceParameters = sliceParameters;
            return this;
        }

        public MicrosoftStsAuthorizationRequest build() {
            return new MicrosoftStsAuthorizationRequest(this);
        }
    }

    public String getUid() {
        return mUid;
    }

    public String getUtid() {
        return mUtid;
    }

    public String getDisplayableId() {
        return mDisplayableId;
    }

    public String getPrompt() {
        return mPrompt;
    }

    public String getSliceParameters() {
        return mSliceParameters;
    }

    @Override
    public String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException {
        final String authorizationUrl = StringExtensions.appendQueryParameterToUrl(
                getAuthorizationEndpoint(), createAuthorizationRequestParameters());
        Logger.infoPII(TAG, null, "Request uri to authorize endpoint is: " + authorizationUrl);
        return authorizationUrl;
    }

    /**
     * Generate the authorization request parameters.
     *
     * @return key value pairs of the authorization request parameters.
     * @throws UnsupportedEncodingException
     * @throws ClientException
     */
    private Map<String, String> createAuthorizationRequestParameters() throws UnsupportedEncodingException, ClientException {
        final Map<String, String> requestParameters = new HashMap<>();

        requestParameters.put(AuthenticationConstants.OAuth2.SCOPE, getScope());
        requestParameters.put(AuthenticationConstants.OAuth2.CLIENT_ID, getClientId());
        requestParameters.put(AuthenticationConstants.OAuth2.REDIRECT_URI, getRedirectUri());
        requestParameters.put(AuthenticationConstants.OAuth2.RESPONSE_TYPE, AuthenticationConstants.OAuth2.CODE);
        requestParameters.put(AuthenticationConstants.OAuth2.STATE, getState());
        //TODO Should we set correlation id as a required value?
        if (null != getCorrelationId()) {
            requestParameters.put(CORRELATION_ID, getCorrelationId().toString());
        }

        addDiagnosticParameters(requestParameters);
        //addPromptParameter(requestParameters);
        addPkceChallengeToRequestParameters(requestParameters);
        addUserInfoParameter(requestParameters);
        addExtraQueryParameter(requestParameters);

        return requestParameters;
    }

    private void addDiagnosticParameters(@NonNull final Map<String, String> requestParameters) {
        requestParameters.put(LIB_ID_PLATFORM, PLATFORM_VALUE);
        requestParameters.put(LIB_ID_OS_VER, String.valueOf(Build.VERSION.SDK_INT));
        requestParameters.put(LIB_ID_DM, Build.MODEL);

        if (!StringUtil.isEmpty(getLibraryVersion())) {
            requestParameters.put(LIB_ID_VERSION, getLibraryVersion());
        }

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            requestParameters.put(LIB_ID_CPU, Build.CPU_ABI);
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;
            if (supportedABIs != null && supportedABIs.length > 0) {
                requestParameters.put(LIB_ID_CPU, supportedABIs[0]);
            }
        }
    }

    private void addUserInfoParameter(@NonNull final Map<String, String> requestParameters) {
        if (!StringExtensions.isNullOrBlank(getLoginHint())) {
            requestParameters.put(LOGIN_HINT, getLoginHint());
        }

        // Enforce session continuation if user is provided in the API request
        //TODO can wrap the user info into User class object.
        addExtraQueryParameter(LOGIN_REQ, mUid, requestParameters);
        addExtraQueryParameter(DOMAIN_REQ, mUtid, requestParameters);
        addExtraQueryParameter(LOGIN_HINT, mDisplayableId, requestParameters);
    }

    /**
     * Add the extra query parameters and slice parameters into the request parameter map.
     */
    private void addExtraQueryParameter(@NonNull final Map<String, String> requestParameters) throws ClientException {
        // adding extra query parameters
        if (!StringExtensions.isNullOrBlank(getExtraQueryParam())) {
            appendExtraQueryParameters(getExtraQueryParam(), requestParameters);
        }
        // adding slice parameters
        if (!StringExtensions.isNullOrBlank(mSliceParameters)) {
            appendExtraQueryParameters(mSliceParameters, requestParameters);
        }
    }

    // Add PKCE Challenge
    private void addPkceChallengeToRequestParameters(@NonNull final Map<String, String> requestParameters) throws ClientException {
        // Create our Challenge
//        if (getPkceChallenge() == null) {
//            Logger.verbose(TAG, "PKCE challenge is null. Set the PKCE challenge.");
//            setPkceChallenge(PkceChallenge.newPkceChallenge());
//        }

        // Add it to our Authorization request
        requestParameters.put(CODE_CHALLENGE, getPkceChallenge().getCodeChallenge());
        // The method used to encode the code_verifier for the code_challenge parameter.
        // Can be one of plain or S256.
        // If excluded, code_challenge is assumed to be plaintext if code_challenge is included.
        // Azure AAD v2.0 supports both plain and S256.
        requestParameters.put(CODE_CHALLENGE_METHOD, getPkceChallenge().getCodeChallengeMethod());

    }

    @Override
    public String getAuthorizationEndpoint() {
        //TODO: Need to take authority aliasing via instance discovery into account here
        return "https://login.microsoftonline.com/common/oauth2/v2.0/authorize";
    }
}
