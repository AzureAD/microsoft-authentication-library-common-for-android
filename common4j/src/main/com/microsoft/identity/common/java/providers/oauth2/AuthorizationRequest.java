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
package com.microsoft.identity.common.java.providers.oauth2;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.CommonURIBuilder;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.java.util.StringUtil;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.Getter;
import lombok.experimental.Accessors;

/**
 * A class holding the state of the Authorization Request (OAuth 2.0).
 * https://tools.ietf.org/html/rfc6749#section-4.1.1
 * This should include all fo the required parameters of the authorization request for oAuth2
 * This should provide an extension point for additional parameters to be set
 */
@Getter
@Accessors(prefix = "m")
public abstract class AuthorizationRequest<T extends AuthorizationRequest<T>> implements Serializable {

    private static final String TAG = AuthorizationRequest.class.getSimpleName();

    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 6171895895590170062L;

    /**
     * A required value and must be set to "code".
     */
    @Expose()
    @SerializedName("response_type")
    private final String mResponseType;

    /**
     * A required value.
     * <p>
     * The client identifier as assigned by the authorization server, when the client was registered.
     */
    @Expose()
    @SerializedName("client_id")
    private final String mClientId;

    /**
     * Redirect URLs are a critical part of the OAuth flow. After a user successfully authorizes an
     * application, the authorization server will redirect the user back to the application with
     * either an authorization code or access token in the URL.
     */
    @SerializedName("redirect_uri")
    private final String mRedirectUri;

    /**
     * A required value.
     * <p>
     * The brk client identifier as assigned by the authorization server, when the client was registered (ClientId of the hub app).
     */
    @Expose()
    @SerializedName("brk_client_id")
    private final String mBrkClientId;

    /**
     * Redirect URL of the hub app.
     */
    @SerializedName("brk_redirect_uri")
    private final String mBrkRedirectUri;

    /**
     * A recommended value.
     * <p>
     * A value included in the request that will also be returned in the token response.
     * It can be a string of any content that you wish. A randomly generated unique value is
     * typically used for preventing cross-site request forgery attacks. The value can also
     * encode information about the user's state in the app before the authentication request
     * occurred, such as the page or view they were on.
     * <p>
     * Note that the value stored here will be Base64 encoded
     */
    @Expose()
    @SerializedName("state")
    private final String mState;

    /**
     * Scopes that you want the user to consent to is required for V2 auth request.
     */
    @Expose()
    @SerializedName("scope")
    private final String mScope;

    /**
     * Claims request parameter (Per OIDC spec)
     */
    @Expose()
    @SerializedName("claims")
    private final String mClaims;

    @Expose()
    final transient private boolean mWebViewZoomControlsEnabled;

    @Expose()
    final transient private boolean mWebViewZoomEnabled;

    /**
     * Header of the request.
     */
    private final transient HashMap<String, String> mRequestHeaders;

    /**
     * Extra query parameters.
     */
    private final transient List<Map.Entry<String, String>> mExtraQueryParams;

    /**
     * Constructor of AuthorizationRequest.
     */
    protected AuthorizationRequest(@SuppressWarnings(WarningType.rawtype_warning) final Builder builder) {
        mResponseType = builder.mResponseType;
        mClientId = builder.mClientId;
        mRedirectUri = builder.mRedirectUri;
        mState = builder.mState == null ? null : StringUtil.encodeUrlSafeString(builder.mState);
        mScope = builder.mScope;

        mBrkClientId = builder.mBrkClientId;
        mBrkRedirectUri = builder.mBrkRedirectUri;

        // Suppressing unchecked warning of casting List to List<Pair<String,String>>. This warning is raised as the generic type was not provided during constructing builder object.
        @SuppressWarnings(WarningType.unchecked_warning) final List<Map.Entry<String, String>> extraQueryParams = builder.mExtraQueryParams;
        mExtraQueryParams = extraQueryParams;

        // Suppressing unchecked warning of casting HashMap to HashMap<Pair<String,String>>. This warning is raised as the generic type was not provided during constructing builder object.
        @SuppressWarnings(WarningType.unchecked_warning) final HashMap<String, String> requestHeaders = builder.mRequestHeaders;
        mRequestHeaders = requestHeaders;

        mClaims = builder.mClaims;
        mWebViewZoomEnabled = builder.mWebViewZoomEnabled;
        mWebViewZoomControlsEnabled = builder.mWebViewZoomControlsEnabled;
    }

    public static final class ResponseType {
        public static final String CODE = "code";
    }

    public static abstract class Builder<B extends AuthorizationRequest.Builder<B>> {
        private String mResponseType = ResponseType.CODE; //ResponseType.CODE as default.
        private String mClientId;
        private String mRedirectUri;

        private String mBrkClientId;
        private String mBrkRedirectUri;

        private String mState;
        private String mScope;
        private String mClaims;
        private HashMap<String, String> mRequestHeaders;
        private boolean mWebViewZoomControlsEnabled = false;
        private boolean mWebViewZoomEnabled = false;

        /**
         * Extra query parameters.
         */
        public List<Map.Entry<String, String>> mExtraQueryParams;

        public B setResponseType(String responseType) {
            mResponseType = responseType;
            return self();
        }

        public B setClientId(String clientId) {
            mClientId = clientId;
            return self();
        }

        public B setRedirectUri(String redirectUri) {
            mRedirectUri = redirectUri;
            return self();
        }

        public B setBrkClientId(String brkClientId) {
            mBrkClientId = brkClientId;
            return self();
        }

        public B setBrkRedirectUri(String brkRedirectUri) {
            mBrkRedirectUri = brkRedirectUri;
            return self();
        }

        public B setState(String state) {
            mState = state;
            return self();
        }

        public B setScope(String scope) {
            mScope = scope;
            return self();
        }

        public B setExtraQueryParams(List<Map.Entry<String, String>> extraQueryParams) {
            mExtraQueryParams = extraQueryParams;
            return self();
        }

        public B setClaims(String claims) {
            mClaims = claims;
            return self();
        }

        public B setRequestHeaders(HashMap<String, String> requestHeaders) {
            mRequestHeaders = requestHeaders;
            return self();
        }

        public B setWebViewZoomEnabled(boolean webViewZoomEnabled) {
            mWebViewZoomEnabled = webViewZoomEnabled;
            return self();
        }

        public B setWebViewZoomControlsEnabled(boolean webViewZoomControlsEnabled) {
            mWebViewZoomControlsEnabled = webViewZoomControlsEnabled;
            return self();
        }

        public abstract B self();

        @SuppressWarnings(WarningType.rawtype_warning)
        public abstract AuthorizationRequest build();
    }

    //CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "AuthorizationRequest{" +
                "mResponseType='" + mResponseType + '\'' +
                ", mClientId='" + mClientId + '\'' +
                ", mRedirectUri='" + mRedirectUri + '\'' +
                ", mBrkClientId='" + mBrkClientId + '\'' +
                ", mBrkRedirectUri='" + mBrkRedirectUri + '\'' +
                ", mScope='" + mScope + '\'' +
                ", mState='" + mState + '\'' +
                '}';
    }

    public abstract String getAuthorizationEndpoint() throws ClientException;

    /**
     * Constructs A request URI from this object.
     */
    public URI getAuthorizationRequestAsHttpRequest() throws ClientException {
        try {
            final CommonURIBuilder builder = new CommonURIBuilder(getAuthorizationEndpoint());
            builder.addParametersIfAbsent(ObjectMapper.serializeObjectHashMap(this));
            builder.addParametersIfAbsent(mExtraQueryParams);
            return builder.build();
        } catch (final URISyntaxException e) {
            throw new ClientException(ClientException.MALFORMED_URL, e.getMessage(), e);
        }
    }
}
