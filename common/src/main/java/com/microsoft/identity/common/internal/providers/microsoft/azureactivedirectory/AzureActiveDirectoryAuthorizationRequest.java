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
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.MicrosoftAuthorizationRequest;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class AzureActiveDirectoryAuthorizationRequest extends MicrosoftAuthorizationRequest {
    private static final String TAG = StringExtensions.class.getSimpleName();

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
    private AzureActiveDirectoryPromptBehavior mPromptBehavior;
    private String mClaimsChallenge;

    public String getResource() {
        return mResource;
    }

    public void setResource(final String resource) {
        mResource = resource;
    }

    public AzureActiveDirectoryPromptBehavior getPromptBehavior() {
        return mPromptBehavior;
    }

    public void setPromptBehavior(final AzureActiveDirectoryPromptBehavior promptBehavior) {
        mPromptBehavior = promptBehavior;
    }

    public String getClaimsChallenge() {
        return mClaimsChallenge;
    }

    public void setClaimsChallenge(final String claimsChallenge) {
        mClaimsChallenge = claimsChallenge;
    }

    public String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException {
        final String authorizationUrl = StringExtensions.appendQueryParameterToUrl(
                getAuthorizationEndpoint(),
                createAuthorizationRequestParameters());
        Logger.infoPII(TAG, getCorrelationId().toString(), "Request uri to authorize endpoint is: " + authorizationUrl);
        return authorizationUrl;
    }

    private Map<String, String> createAuthorizationRequestParameters() throws UnsupportedEncodingException, ClientException {
        final Map<String, String> requestParameters = new HashMap<>();
        requestParameters.put(AuthenticationConstants.OAuth2.RESPONSE_TYPE, AuthenticationConstants.OAuth2.CODE);
        requestParameters.put(AuthenticationConstants.OAuth2.CLIENT_ID,
                URLEncoder.encode(getClientId(), ENCODING_UTF8));
        requestParameters.put(RESOURCE, URLEncoder.encode(mResource, ENCODING_UTF8));
        requestParameters.put(AuthenticationConstants.OAuth2.REDIRECT_URI,
                URLEncoder.encode(getRedirectUri(), ENCODING_UTF8));
        requestParameters.put(AuthenticationConstants.OAuth2.STATE, encodeProtocolState());
        requestParameters.put(AuthenticationConstants.OAuth2.SCOPE, SCOPE_OPENID_VALUE);

        // append device and platform info in the query parameters
        requestParameters.put(LIB_ID_PLATFORM, PLATFORM_VALUE);
        requestParameters.put(LIB_ID_VERSION, URLEncoder.encode(getLibraryVersion(), ENCODING_UTF8));
        requestParameters.put(LIB_ID_OS_VER, URLEncoder.encode(String.valueOf(Build.VERSION.SDK_INT), ENCODING_UTF8));
        requestParameters.put(LIB_ID_DM, URLEncoder.encode(android.os.Build.MODEL, ENCODING_UTF8) );

        if (!StringExtensions.isNullOrBlank(getLoginHint())) {
            requestParameters.put(LOGIN_HINT, URLEncoder.encode(getLoginHint(), ENCODING_UTF8) );
        }

        if (getCorrelationId() != null) {
            requestParameters.put(CLIENT_REQUEST_ID, URLEncoder.encode(getCorrelationId().toString(), ENCODING_UTF8));
        }

        // Setting prompt behavior to always will skip the cookies for webview.
        // It is added to authorization url.
        if (mPromptBehavior == AzureActiveDirectoryPromptBehavior.ALWAYS) {
            requestParameters.put(QUERY_PROMPT, URLEncoder.encode(QUERY_PROMPT_VALUE, ENCODING_UTF8));
        } else if (mPromptBehavior == AzureActiveDirectoryPromptBehavior.REFRESH_SESSION) {
            requestParameters.put(QUERY_PROMPT, URLEncoder.encode(QUERY_PROMPT_REFRESH_SESSION_VALUE, ENCODING_UTF8));
        }

        if (null != getPkceChallenge()) {
            requestParameters.put(CODE_CHALLENGE, URLEncoder.encode(getPkceChallenge().getCodeChallenge(), ENCODING_UTF8));
            // The method used to encode the code_verifier for the code_challenge parameter.
            requestParameters.put(CODE_CHALLENGE_METHOD, URLEncoder.encode(getPkceChallenge().getCodeChallengeMethod(), ENCODING_UTF8));
        }

        // Reading extra qp supplied by developer. append haschrome=1 if developer does not pass as extra qp
        if (StringExtensions.isNullOrBlank(getExtraQueryParam())
                || !getExtraQueryParam().contains(AuthenticationConstants.OAuth2.HAS_CHROME)) {
            requestParameters.put(AuthenticationConstants.OAuth2.HAS_CHROME, "1");
        }

        // Claims challenge are opaque to the sdk, we're not going to do any merging if both extra qp and claims parameter
        // contain it. Also, if developer sends it in both places, server will fail it.
        if (!StringExtensions.isNullOrBlank(mClaimsChallenge)) {
            requestParameters.put(AuthenticationConstants.OAuth2.CLAIMS, mClaimsChallenge);
        }
        if (!StringExtensions.isNullOrBlank(getExtraQueryParam())) {
            appendExtraQueryParameters(getExtraQueryParam(), requestParameters);
        }

        return requestParameters;
    }

    private String encodeProtocolState() throws UnsupportedEncodingException {
        String state = String.format("a=%s&r=%s", getAuthorizationEndpoint(), mResource);
        return Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private void appendExtraQueryParameters(final String queryParams, final Map<String, String> requestParams) throws ClientException {
        final Map<String, String> extraQps = StringExtensions.decodeUrlToMap(queryParams, "&");
        final Set<Map.Entry<String, String>> extraQpEntries = extraQps.entrySet();
        for (final Map.Entry<String, String> extraQpEntry : extraQpEntries) {
            if (requestParams.containsKey(extraQpEntry.getKey())) {
                throw new ClientException(ErrorStrings.DUPLICATE_QUERY_PARAMETER,
                        "Extra query parameter " + extraQpEntry.getKey() + " is already sent by "
                                + "the SDK. ");
            }

            requestParams.put(extraQpEntry.getKey(), extraQpEntry.getValue());
        }
    }
}
