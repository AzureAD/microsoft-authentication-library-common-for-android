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
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.PKCEChallenge;
import com.microsoft.identity.common.internal.ui.AuthorizationConfiguration;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static android.content.ContentValues.TAG;

public class MicrosoftStsAuthorizationRequest extends AuthorizationRequest {
    protected static final String ENCODING_UTF8 = "UTF_8";
    private static final String CORRELATION_ID = "correlation_id";
    private static final String MSAL_ID_PLATFORM = "x-client-SKU";
    private static final String MSAL_ID_VERSION = "x-client-Ver";
    private static final String MSAL_ID_CPU = "x-client-CPU";
    private static final String MSAL_ID_OS_VER = "x-client-OS";
    private static final String MSAL_ID_DM = "x-client-DM";
    private static final String QUERY_PROMPT = "prompt";
    private static final String LOGIN_HINT = "login_hint";
    private static final String LOGIN_REQ = "login_req";
    private static final String DOMAIN_REQ = "domain_req";
    private static final String CODE_CHALLENGE = "code_challenge";
    private static final String CODE_CHALLENGE_METHOD = "code_challenge_method";
    private static final String SCOPE_PROFILE = "profile";
    private static final String[] RESERVED_SCOPES = {"openid", SCOPE_PROFILE, "offline_access"};
    private static final String PLATFORM_VALUE = "MSAL.Android";
    private static final String PROMPT_LOGIN = "login";
    private static final String PROMPT_SELECT_ACCOUNT = "select_account";
    private static final String PROMPT_CONSENT = "consent";

    private String mAuthorizationEndpoint;
    private URL mAuthority;
    private UUID mCorrelationId;
    private final Set<String> mExtraScopesToConsent = new HashSet<>();
    /**
     * Optional. Indicates the type of user interaction that is required. The only valid values at this time are 'login', 'none', and 'consent'.
     */
    private MicrosoftStsPromptBehavior mPromptBehavior;
    /**
     * Optional. Can be used to pre-fill the username/email address field of the sign-in page for the user, if you know their username ahead of time.
     */
    private String mLoginHint;
    private String mUid;
    private String mUtid;
    private String mDisplayableId;
    /**
     *  Used to secure authorization code grants via Proof Key for Code Exchange (PKCE) from a native client.
     */
    private PKCEChallenge mPKCEChallenge;
    private String mExtraQueryParam;
    private String mSliceParameters;
    private String mLibraryVersion;

    public UUID getCorrelationId() {
        return mCorrelationId;
    }

    public void setCorrelationId(final UUID correlationId) {
        mCorrelationId = correlationId;
    }

    public String getAuthorizationEndpoint() {
        return mAuthorizationEndpoint;
    }

    public void setAuthorizationEndpoint(final String authorizationEndpoint) {
        mAuthorizationEndpoint = authorizationEndpoint;
    }

    public Set<String> getExtraScopesToConsent() {
        return mExtraScopesToConsent;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public void setLoginHint(final String loginHint) {
        mLoginHint = loginHint;
    }

    public String getUid() {
        return mUid;
    }

    public void setUid(final String uid) {
        mUid = uid;
    }

    public String getUtid() {
        return mUtid;
    }

    public void setUtid(final String utid) {
        mUtid = utid;
    }

    public String getDisplayableId() {
        return mDisplayableId;
    }

    public void setDisplayableId(final String displayableId) {
        mDisplayableId = displayableId;
    }

    public MicrosoftStsPromptBehavior getPromptBehavior() {
        return mPromptBehavior;
    }

    public void setPromptBehavior(final MicrosoftStsPromptBehavior promptBehavior) {
        mPromptBehavior = promptBehavior;
    }

    public PKCEChallenge getPKCEChallenge() {
        return mPKCEChallenge;
    }

    public void setPKCEChallenge(final PKCEChallenge pKCEChallenge) {
        mPKCEChallenge = pKCEChallenge;
    }

    public String getExtraQueryParam() {
        return mExtraQueryParam;
    }

    public void setExtraQueryParam(final String extraQueryParam) {
        mExtraQueryParam = extraQueryParam;
    }

    public String getSliceParameters() {
        return mSliceParameters;
    }

    public void setmSliceParameters(final String sliceParameters) {
        mSliceParameters = sliceParameters;
    }

    public URL getAuthority() {
        return mAuthority;
    }

    public void setAuthority(final URL authority) {
        mAuthority = authority;
    }

    /**
     * @return String of MSAL version name.
     */
    public String getLibraryVersion() {
        return mLibraryVersion;
    }

    /**
     * The calling MSAL library should pass in its version name.
     *
     * @param libraryVersion String of the MSAL library version name.
     */
    public void setLibraryVersion(final String libraryVersion) {
        mLibraryVersion = libraryVersion;
    }

    public String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException{
        if(AuthorizationConfiguration.getInstance().isBrokerRequest()) {
            return getBrokerAuthorizationStartUrl();
        } else {
            return getLocalAuthorizationStartUrl();
        }
    }

    private String getBrokerAuthorizationStartUrl() {
        throw new UnsupportedOperationException("Not implemented.");

    }

    private String getLocalAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException {
        String authorizationUrl = StringExtensions.appendQueryParameterToUrl(
                mAuthorizationEndpoint,
                createAuthorizationRequestParameters());
        Logger.infoPII(TAG, mCorrelationId.toString(), "Request uri to authorize endpoint is: " + authorizationUrl);
        return authorizationUrl;
    }

    private Map<String, String> createAuthorizationRequestParameters() throws UnsupportedEncodingException, ClientException {
        final Map<String, String> requestParameters = new HashMap<>();

        final Set<String> scopes = new HashSet<>(getScope());
        scopes.addAll(mExtraScopesToConsent);
        final Set<String> requestedScopes = getDecoratedScope(scopes);
        requestParameters.put(AuthenticationConstants.OAuth2.SCOPE,
                StringExtensions.convertSetToString(requestedScopes, " "));
        requestParameters.put(AuthenticationConstants.OAuth2.CLIENT_ID, getClientId());
        requestParameters.put(AuthenticationConstants.OAuth2.REDIRECT_URI, getRedirectUri());
        requestParameters.put(AuthenticationConstants.OAuth2.RESPONSE_TYPE, AuthenticationConstants.OAuth2.CODE);
        requestParameters.put(CORRELATION_ID, mCorrelationId.toString());

        requestParameters.put(MSAL_ID_PLATFORM, PLATFORM_VALUE);
        requestParameters.put(MSAL_ID_VERSION, mLibraryVersion);
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            requestParameters.put(MSAL_ID_CPU, Build.CPU_ABI);
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;
            if (supportedABIs != null && supportedABIs.length > 0) {
                requestParameters.put(MSAL_ID_CPU, supportedABIs[0]);
            }
        }
        requestParameters.put(MSAL_ID_OS_VER, String.valueOf(Build.VERSION.SDK_INT));
        requestParameters.put(MSAL_ID_DM, Build.MODEL);

        if (!StringExtensions.isNullOrBlank(mLoginHint)) {
            requestParameters.put(LOGIN_HINT, mLoginHint);
        }

        if (mPromptBehavior == MicrosoftStsPromptBehavior.FORCE_LOGIN) {
            requestParameters.put(QUERY_PROMPT, PROMPT_LOGIN);
        } else if (mPromptBehavior == MicrosoftStsPromptBehavior.SELECT_ACCOUNT) {
            requestParameters.put(QUERY_PROMPT,PROMPT_SELECT_ACCOUNT);
        } else if (mPromptBehavior == MicrosoftStsPromptBehavior.CONSENT) {
            requestParameters.put(QUERY_PROMPT, PROMPT_CONSENT);
        }

        // append state in the query parameters
        requestParameters.put(AuthenticationConstants.OAuth2.STATE, encodeProtocolState());

        // Add PKCE Challenge
        addPKCEChallengeToRequestParameters(requestParameters);

        // Enforce session continuation if user is provided in the API request
        //TODO can wrap the user info into User class object.
        addExtraQueryParameter(LOGIN_REQ, mUid, requestParameters);
        addExtraQueryParameter(DOMAIN_REQ, mUtid, requestParameters);
        addExtraQueryParameter(LOGIN_HINT, mDisplayableId, requestParameters);

        // adding extra qp
        if (!StringExtensions.isNullOrBlank(mExtraQueryParam)) {
            appendExtraQueryParameters(mExtraQueryParam, requestParameters);
        }

        if (!StringExtensions.isNullOrBlank(mSliceParameters)) {
            appendExtraQueryParameters(mSliceParameters, requestParameters);
        }

        return requestParameters;
    }

    private String encodeProtocolState() throws UnsupportedEncodingException {
        final String state = String.format("a=%s&r=%s", StringExtensions.urlFormEncode(
                mAuthority.toString()),
                StringExtensions.urlFormEncode(StringExtensions.convertSetToString(
                        getScope(), " ")));
        return Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    private void addExtraQueryParameter(final String key, final String value, final Map<String, String> requestParams) {
        if (!StringExtensions.isNullOrBlank(key) && !StringExtensions.isNullOrBlank(value)) {
            requestParams.put(key, value);
        }
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

    /**
     * Get the decorated scopes. Will combine the input scope and the reserved scope. If client id is provided as scope,
     * it will be removed from the combined scopes.
     *
     * @param inputScopes The input scopes to decorate.
     * @return The combined scopes.
     */
    Set<String> getDecoratedScope(final Set<String> inputScopes) {
        final Set<String> scopes = new HashSet<>(inputScopes);
        final Set<String> reservedScopes = getReservedScopesAsSet();
        scopes.addAll(reservedScopes);
        scopes.remove(getClientId());

        return scopes;
    }

    private Set<String> getReservedScopesAsSet() {
        return new HashSet<>(Arrays.asList(RESERVED_SCOPES));
    }

    private void addPKCEChallengeToRequestParameters(final Map<String, String> requestParameters) throws ClientException {
        // Create our Challenge
        mPKCEChallenge = PKCEChallenge.newPKCEChallenge();

        // Add it to our Authorization request
        requestParameters.put(CODE_CHALLENGE, mPKCEChallenge.getCodeChallenge());
        // The method used to encode the code_verifier for the code_challenge parameter.
        // Can be one of plain or S256.
        // If excluded, code_challenge is assumed to be plaintext if code_challenge is included.
        // Azure AAD v2.0 supports both plain and S256.
        requestParameters.put(CODE_CHALLENGE_METHOD, mPKCEChallenge.getCodeChallengeMethod());
    }



}
