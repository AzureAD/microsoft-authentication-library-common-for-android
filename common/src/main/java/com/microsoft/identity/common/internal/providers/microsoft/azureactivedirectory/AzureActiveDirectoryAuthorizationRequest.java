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
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.PKCEChallenge;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class AzureActiveDirectoryAuthorizationRequest extends AuthorizationRequest {
    protected static final String ENCODING_UTF8 = "UTF_8";
    private static final String TAG = StringExtensions.class.getSimpleName();
    private static final String RESOURCE = "resource";
    private static final String LOGIN_HINT = "login_hint";
    private static final String ADAL_ID_PLATFORM = "x-client-SKU";
    private static final String ADAL_ID_VERSION = "x-client-Ver";
    private static final String ADAL_ID_OS_VER = "x-client-OS";
    private static final String ADAL_ID_DM = "x-client-DM";
    private static final String CLIENT_REQUEST_ID = "client-request-id";
    private static final String QUERY_PROMPT = "prompt";
    private static final String CODE_CHALLENGE = "code_challenge";
    private static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    private static final String SCOPE_OPENID_VALUE = "openid";
    private static final String ADAL_ID_PLATFORM_VALUE = "Android";
    private static final String QUERY_PROMPT_VALUE = "login";
    private static final String QUERY_PROMPT_REFRESH_SESSION_VALUE = "refresh_session";

    /**
     * Required.
     */
    private URL mAuthority;
    /**
     * Required.
     *
     * Passed in from ADAL/MSAL after authority verification.
     */
    private String mAuthorizationEndpoint;
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
    private AADPromptBehavior mPromptBehavior;
    /**
     * Optional. Can be used to pre-fill the username/email address field of the sign-in page for the user, if you know their username ahead of time.
     */
    private String mLoginHint;
    private UUID mCorrelationId;
    private String mExtraQP;
    private String mClaimsChallenge;
    private PKCEChallenge mPKCEChallenge;
    private String mLibraryVersion;

    /**
     * @return URL authority
     */
    public String getAuthorizationEndpoint() {
        return mAuthorizationEndpoint;
    }

    /**
     * @param authorizationEndpoint String
     */
    public void setAuthorizationEndpoint(String authorizationEndpoint) {
        mAuthorizationEndpoint = authorizationEndpoint;
    }

    public void setPKCEChallenge(final PKCEChallenge pKCEChallenge) {
        mPKCEChallenge = pKCEChallenge;
    }

    public String getResource() {
        return mResource;
    }

    public void setResource(final String resource) {
        mResource = resource;
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

    public AADPromptBehavior getPromptBehavior() {
        return mPromptBehavior;
    }

    public void setPromptBehavior(final AADPromptBehavior promptBehavior) {
        mPromptBehavior = promptBehavior;
    }

    public String getExtraQP() {
        return mExtraQP;
    }

    public void setExtraQP(final String extraQP) {
        this.mExtraQP = extraQP;
    }

    public String getClaimsChallenge() {
        return mClaimsChallenge;
    }

    public void setClaimsChallenge(final String claimsChallenge) {
        mClaimsChallenge = claimsChallenge;
    }


    public URL getAuthority() {
        return mAuthority;
    }

    public void setAuthority(final URL authority) {
        mAuthority = authority;
    }


    /**
     * @return String of ADAL version name.
     */
    public String getLibraryVersion() {
        return mLibraryVersion;
    }

    /**
     * The ADAL library should pass in its version.
     *
     * @param libraryVersion String of the library version name.
     */
    public void setLibraryVersion(final String libraryVersion) {
        mLibraryVersion = libraryVersion;
    }

    //CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "AzureActiveDirectoryAuthorizationRequest{" +
                "mAuthority=" + mAuthorizationEndpoint +
                "} " + super.toString();
    }
    //CHECKSTYLE:ON

    public String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException {
        final String authorizationUrl = StringExtensions.appendQueryParameterToUrl(
                mAuthorizationEndpoint,
                createAuthorizationRequestParameters());
        Logger.infoPII(TAG, mCorrelationId.toString(), "Request uri to authorize endpoint is: " + authorizationUrl);
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
        requestParameters.put(ADAL_ID_PLATFORM, ADAL_ID_PLATFORM_VALUE);
        requestParameters.put(ADAL_ID_VERSION, URLEncoder.encode(mLibraryVersion, ENCODING_UTF8));
        requestParameters.put(ADAL_ID_OS_VER, URLEncoder.encode(String.valueOf(Build.VERSION.SDK_INT), ENCODING_UTF8));
        requestParameters.put(ADAL_ID_DM, URLEncoder.encode(android.os.Build.MODEL, ENCODING_UTF8) );

        if (!StringExtensions.isNullOrBlank(mLoginHint)) {
            requestParameters.put(LOGIN_HINT, URLEncoder.encode(mLoginHint, ENCODING_UTF8) );
        }

        if (mCorrelationId != null) {
            requestParameters.put(CLIENT_REQUEST_ID, URLEncoder.encode(mCorrelationId.toString(), ENCODING_UTF8));
        }

        // Setting prompt behavior to always will skip the cookies for webview.
        // It is added to authorization url.
        if (mPromptBehavior == AADPromptBehavior.Always) {
            requestParameters.put(QUERY_PROMPT, URLEncoder.encode(QUERY_PROMPT_VALUE, ENCODING_UTF8));
        } else if (mPromptBehavior == AADPromptBehavior.REFRESH_SESSION) {
            requestParameters.put(QUERY_PROMPT, URLEncoder.encode(QUERY_PROMPT_REFRESH_SESSION_VALUE, ENCODING_UTF8));
        }

        if (null != mPKCEChallenge) {
            requestParameters.put(CODE_CHALLENGE, URLEncoder.encode(mPKCEChallenge.getCodeChallenge(), ENCODING_UTF8));
            // The method used to encode the code_verifier for the code_challenge parameter.
            requestParameters.put(CODE_CHALLENGE_METHOD, URLEncoder.encode(mPKCEChallenge.getCodeChallengeMethod(), ENCODING_UTF8));
        }

        // Reading extra qp supplied by developer. append haschrome=1 if developer does not pass as extra qp
        if (StringExtensions.isNullOrBlank(mExtraQP)
                || !mExtraQP.contains(AuthenticationConstants.OAuth2.HAS_CHROME)) {
            requestParameters.put(AuthenticationConstants.OAuth2.HAS_CHROME, "1");
        }

        // Claims challenge are opaque to the sdk, we're not going to do any merging if both extra qp and claims parameter
        // contain it. Also, if developer sends it in both places, server will fail it.
        if (!StringExtensions.isNullOrBlank(mClaimsChallenge)) {
            requestParameters.put(AuthenticationConstants.OAuth2.CLAIMS, mClaimsChallenge);
        }
        if (!StringExtensions.isNullOrBlank(mExtraQP)) {
            appendExtraQueryParameters(mExtraQP, requestParameters);
        }

        return requestParameters;
    }

    private String encodeProtocolState() throws UnsupportedEncodingException {
        String state = String.format("a=%s&r=%s", mAuthorizationEndpoint, mResource);
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
