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

import android.net.Uri;
import android.os.Build;
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.ui.AuthorizationConfiguration;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.UUID;

public class AzureActiveDirectoryAuthorizationRequest extends AuthorizationRequest {
    private static final String TAG = StringExtensions.class.getSimpleName();
    /**
     * Passed in from ADAL/MSAL after authority verification.
     */
    private String mAuthorizationEndpoint; //not null
    private URL mAuthority;
    private String mResource; //not null
    private String mLoginHint; //nullable
    private UUID mCorrelationId; //nullable
    private AADPromptBehavior mPromptBehavior; //nullable
    private String mExtraQP; //nullable
    private String mClaimsChallenge; //nullable

    //For broker authorization request.
    private String mCallingPackage;
    private String mSignatureDigest;

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

    public String getCallingPackage() {
        return mCallingPackage;
    }

    public void setCallingPackage(final String callingPackage) {
        mCallingPackage = callingPackage;
    }

    public String getSignatureDigest() {
        return mSignatureDigest;
    }

    public void setSignatureDigest(final String signatureDigest) {
        mSignatureDigest = signatureDigest;
    }

    public URL getAuthority() {
        return mAuthority;
    }

    public void setAuthority(final URL authority) {
        mAuthority = authority;
    }


    //CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "AzureActiveDirectoryAuthorizationRequest{" +
                "mAuthority=" + mAuthorizationEndpoint +
                "} " + super.toString();
    }
    //CHECKSTYLE:ON

    public String getAuthorizationStartUrl() throws UnsupportedEncodingException {
        if (AuthorizationConfiguration.getInstance().isBrokerRequest()) {
            return getBrokerAuthorizationStartUrl();
        } else {
            return getLocalAuthorizationStartUrl();
        }
    }

    private String getLocalAuthorizationStartUrl() {
        try {
            return String.format("%s?%s", getAuthorizationEndpoint(), getAuthorizationEndpointQueryParameters());
        } catch (final UnsupportedEncodingException e) {
            // This encoding issue will happen at the beginning of API call,
            // if it is not supported on this device. ADAL uses one encoding
            // type.
            Logger.error(TAG, "Encoding", e);
            return null;
        }
    }

    private String getBrokerAuthorizationStartUrl() {
        try {
            final String startUrl = getLocalAuthorizationStartUrl();
            if (!StringExtensions.isNullOrBlank(mCallingPackage)
                    && !StringExtensions.isNullOrBlank(mSignatureDigest)) {

                return startUrl + "&package_name="
                        + URLEncoder.encode(mCallingPackage, AuthenticationConstants.ENCODING_UTF8)
                        + "&signature="
                        + URLEncoder.encode(mSignatureDigest, AuthenticationConstants.ENCODING_UTF8);

            }
            return startUrl;
        } catch (final UnsupportedEncodingException e) {
            // This encoding issue will happen at the beginning of API call,
            // if it is not supported on this device. ADAL uses one encoding
            // type.
            Logger.error(TAG, "Encoding", e);
            return null;
        }
    }

    public String getAuthorizationEndpointQueryParameters() throws UnsupportedEncodingException {
        final Uri.Builder queryParameter = new Uri.Builder();
        queryParameter.appendQueryParameter(AuthenticationConstants.OAuth2.RESPONSE_TYPE,
                AuthenticationConstants.OAuth2.CODE)
                .appendQueryParameter(AuthenticationConstants.OAuth2.CLIENT_ID,
                        URLEncoder.encode(getClientId(),
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.AAD.RESOURCE,
                        URLEncoder.encode(mResource,
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.OAuth2.REDIRECT_URI,
                        URLEncoder.encode(getRedirectUri(),
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.OAuth2.STATE, encodeProtocolState());

        if (!StringExtensions.isNullOrBlank(mLoginHint)) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.LOGIN_HINT,
                    URLEncoder.encode(mLoginHint,
                            AuthenticationConstants.ENCODING_UTF8));
        }

        // append device and platform info in the query parameters
        queryParameter.appendQueryParameter(AuthenticationConstants.AAD.ADAL_ID_PLATFORM,
                AuthenticationConstants.AAD.ADAL_ID_PLATFORM_VALUE)
                .appendQueryParameter(AuthenticationConstants.AAD.ADAL_ID_VERSION,
                        URLEncoder.encode(AuthenticationConstants.ADAL_VERSION_NAME,
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.AAD.ADAL_ID_OS_VER,
                        URLEncoder.encode(String.valueOf(Build.VERSION.SDK_INT),
                                AuthenticationConstants.ENCODING_UTF8))
                .appendQueryParameter(AuthenticationConstants.AAD.ADAL_ID_DM,
                        URLEncoder.encode(android.os.Build.MODEL,
                                AuthenticationConstants.ENCODING_UTF8));

        if (mCorrelationId != null) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.CLIENT_REQUEST_ID,
                    URLEncoder.encode(mCorrelationId.toString(),
                            AuthenticationConstants.ENCODING_UTF8));
        }

        // Setting prompt behavior to always will skip the cookies for webview.
        // It is added to authorization url.
        if (mPromptBehavior == AADPromptBehavior.Always) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.QUERY_PROMPT,
                    URLEncoder.encode(AuthenticationConstants.AAD.QUERY_PROMPT_VALUE,
                            AuthenticationConstants.ENCODING_UTF8));
        } else if (mPromptBehavior == AADPromptBehavior.REFRESH_SESSION) {
            queryParameter.appendQueryParameter(AuthenticationConstants.AAD.QUERY_PROMPT,
                    URLEncoder.encode(
                            AuthenticationConstants.AAD.QUERY_PROMPT_REFRESH_SESSION_VALUE,
                            AuthenticationConstants.ENCODING_UTF8));
        }

        // Reading extra qp supplied by developer. append haschrome=1 if developer does not pass as extra qp
        if (StringExtensions.isNullOrBlank(mExtraQP)
                || !mExtraQP.contains(AuthenticationConstants.OAuth2.HAS_CHROME)) {
            queryParameter.appendQueryParameter(AuthenticationConstants.OAuth2.HAS_CHROME, "1");
        }

        // Claims challenge are opaque to the sdk, we're not going to do any merging if both extra qp and claims parameter
        // contain it. Also, if developer sends it in both places, server will fail it.
        if (!StringExtensions.isNullOrBlank(mClaimsChallenge)) {
            queryParameter.appendQueryParameter(AuthenticationConstants.OAuth2.CLAIMS, mClaimsChallenge);
        }

        String requestUrl = queryParameter.build().getQuery();
        if (!StringExtensions.isNullOrBlank(mExtraQP)) {
            String parsedQP = mExtraQP;
            if (!mExtraQP.startsWith("&")) {
                parsedQP = "&" + parsedQP;
            }
            requestUrl += parsedQP;
        }

        return requestUrl;
    }

    private String encodeProtocolState() throws UnsupportedEncodingException {
        String state = String.format("a=%s&r=%s", mAuthorizationEndpoint, mResource);
        return Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
    }

}
