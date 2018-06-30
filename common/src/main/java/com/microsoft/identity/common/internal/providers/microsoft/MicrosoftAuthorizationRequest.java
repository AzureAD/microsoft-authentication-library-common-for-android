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

import android.net.UrlQuerySanitizer;
import android.support.annotation.NonNull;
import android.util.Base64;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationRequest;
import com.microsoft.identity.common.internal.providers.oauth2.PkceChallenge;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import static com.microsoft.identity.common.adal.internal.util.StringExtensions.isNullOrBlank;
import static com.microsoft.identity.common.adal.internal.util.StringExtensions.urlFormDecode;

public abstract class MicrosoftAuthorizationRequest extends AuthorizationRequest {
    /* Constants */
    private static final String TAG = MicrosoftAuthorizationRequest.class.getSimpleName();
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
     * <p>
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
     * Used to secure authorization code grants via Proof Key for Code Exchange (PKCE) from a native client.
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

    /**
     * Constructor of MicrosoftAuthorizationRequest.
     */
    public MicrosoftAuthorizationRequest(final String responseType,
                                         @NonNull final String clientId,
                                         @NonNull final String redirectUri,
                                         final String state,
                                         final Set<String> scope,
                                         @NonNull final URL authority,
                                         @NonNull final String authorizationEndpoint,
                                         final String loginHint,
                                         final UUID correlationId,
                                         final PkceChallenge pkceChallenge,
                                         final String extraQueryParam,
                                         final String libraryVersion) {
        super(responseType, clientId, redirectUri, state, scope);

        if (StringUtil.isEmpty(redirectUri)) {
            throw new IllegalArgumentException("redirect Uri is empty");
        }

        if (StringUtil.isEmpty(authorizationEndpoint)) {
            throw new IllegalArgumentException("Authorization endpoint is empty");
        }

        mAuthority = authority;
        mAuthorizationEndpoint = authorizationEndpoint;
        mLoginHint = loginHint;
        mCorrelationId = correlationId;
        mPkceChallenge = pkceChallenge;
        mExtraQueryParam = extraQueryParam;
        mLibraryVersion = libraryVersion;
    }

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
     * Convert the query parameters string to pair and add into the request parameters map.
     */
    protected void appendExtraQueryParameters(final String queryParams, final Map<String, String> requestParams) throws ClientException {
        final Map<String, String> extraQps = constructQueryParamsMap(queryParams);
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

    protected String generateState() {
        final UUID stateUUID1 = UUID.randomUUID();
        final UUID stateUUID2 = UUID.randomUUID();
        return stateUUID1.toString() + "-" + stateUUID2.toString();
    }


    protected void addExtraQueryParameter(final String key, final String value, final Map<String, String> requestParams) {
        if (!isNullOrBlank(key) && !isNullOrBlank(value)) {
            requestParams.put(key, value);
        }
    }


    /**
     * Sanitizes the query portion of a URL and convert the query parameters into a map.
     *
     * @param url The url to decode for.
     * @return A map of all the query parameter pairs in the URL's query portion.
     */
    private static Map<String, String> constructQueryParamsMap(final String url) {
        final Map<String, String> decodedUrlMap = new HashMap<>();

        if (isNullOrBlank(url)) {
            return decodedUrlMap;
        }

        final StringTokenizer tokenizer = new StringTokenizer(url, "&");
        while (tokenizer.hasMoreTokens()) {
            final String pair = tokenizer.nextToken();
            final String[] elements = pair.split("=");

            if (elements.length != 2) {
                continue;
            }

            try {
                final String key = urlFormDecode(elements[0]);
                final String value = urlFormDecode(elements[1]);

                if (!isNullOrBlank(key) && !isNullOrBlank(value)) {
                    decodedUrlMap.put(key, value);
                }

            } catch (final UnsupportedEncodingException e) {
                Logger.error(TAG, null, "Decode failed.", e);
            }
        }

        return decodedUrlMap;
    }
}
