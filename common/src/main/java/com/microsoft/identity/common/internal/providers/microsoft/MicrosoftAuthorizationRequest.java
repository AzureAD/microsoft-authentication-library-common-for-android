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

import com.google.gson.annotations.SerializedName;
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
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.UUID;

import static com.microsoft.identity.common.adal.internal.util.StringExtensions.isNullOrBlank;
import static com.microsoft.identity.common.adal.internal.util.StringExtensions.urlFormDecode;

public abstract class MicrosoftAuthorizationRequest<T extends MicrosoftAuthorizationRequest<T>> extends AuthorizationRequest<T> {
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 6873634931996113294L;

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
     * Can be used to pre-fill the username/email address field of the sign-in page for the user, if you know their username ahead of time.
     */
    private String mLoginHint;
    /**
     * Correlation ID.
     */
    @SerializedName("")
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
    protected MicrosoftAuthorizationRequest(final Builder builder) {
        super(builder);
        mAuthority = builder.mAuthority;
        mLoginHint = builder.mLoginHint;
        mCorrelationId = builder.mCorrelationId;
        mPkceChallenge = builder.mPkceChallenge;
        mExtraQueryParam = builder.mExtraQueryParam;
        mLibraryVersion = builder.mLibraryVersion;
    }

    public static abstract class Builder<T extends MicrosoftAuthorizationRequest> extends AuthorizationRequest.Builder<MicrosoftAuthorizationRequest> {
        /**
         * Required.
         */
        private URL mAuthority;

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

        public Builder(@NonNull final String clientId,
                       @NonNull final String redirectUri,
                       @NonNull final URL authority) {
            super(clientId, redirectUri);
            setAuthority(authority);
        }

        public Builder setAuthority(URL authority) {
            mAuthority = authority;
            return this;
        }

        public Builder setLoginHint(String loginHint) {
            mLoginHint = loginHint;
            return this;
        }

        public Builder setCorrelationId(UUID correlationId) {
            mCorrelationId = correlationId;
            return this;
        }

        public Builder setPkceChallenge(PkceChallenge pkceChallenge) {
            mPkceChallenge = pkceChallenge;
            return this;
        }

        public Builder setExtraQueryParam(String extraQueryParam) {
            mExtraQueryParam = extraQueryParam;
            return this;
        }

        public Builder setLibraryVersion(String libraryVersion) {
            mLibraryVersion = libraryVersion;
            return this;
        }

        public abstract T build();
    }

    public URL getAuthority() {
        return mAuthority;
    }

    public String getLoginHint() {
        return mLoginHint;
    }

    public UUID getCorrelationId() {
        return mCorrelationId;
    }

    public PkceChallenge getPkceChallenge() {
        return mPkceChallenge;
    }

    public String getExtraQueryParam() {
        return mExtraQueryParam;
    }

    public String getLibraryVersion() {
        return mLibraryVersion;
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

    public String generateEncodedState() throws UnsupportedEncodingException {
        final UUID stateUUID1 = UUID.randomUUID();
        final UUID stateUUID2 = UUID.randomUUID();
        final String state = stateUUID1.toString() + "-" + stateUUID2.toString();
        return Base64.encodeToString(state.getBytes("UTF-8"), Base64.NO_PADDING | Base64.URL_SAFE);
    }

    public String decodeState(final String encodedState) {
        if (StringUtil.isEmpty(encodedState)) {
            Logger.warn(TAG, "Decode state failed because the input state is empty.");
            return null;
        }

        final byte[] stateBytes = Base64.decode(encodedState, Base64.NO_PADDING | Base64.URL_SAFE);
        return new String(stateBytes, Charset.defaultCharset());
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
