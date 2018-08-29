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
package com.microsoft.identity.common.internal.providers.oauth2;

import android.net.Uri;
import android.support.annotation.NonNull;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.util.Map;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.net.ObjectMapper;

/**
 * A class holding the state of the Authorization Request (OAuth 2.0).
 * https://tools.ietf.org/html/rfc6749#section-4.1.1
 * This should include all fo the required parameters of the authorization request for oAuth2
 * This should provide an extension point for additional parameters to be set
 */
public abstract class AuthorizationRequest<T extends AuthorizationRequest<T>> implements Serializable {
    /**
     * Serial version id.
     */
    private static final long serialVersionUID = 6171895895590170062L;

    /**
     * A required value and must be set to "code".
     */
    @SerializedName("response_type")
    private String mResponseType;

    /**
     * A required value.
     * <p>
     * The client identifier as assigned by the authorization server, when the client was registered.
     */
    @SerializedName("client_id")
    private String mClientId;

    /**
     * Redirect URLs are a critical part of the OAuth flow. After a user successfully authorizes an
     * application, the authorization server will redirect the user back to the application with
     * either an authorization code or access token in the URL.
     */
    @SerializedName("redirect_uri")
    private String mRedirectUri;

    /**
     * A recommended value.
     * <p>
     * A value included in the request that will also be returned in the token response.
     * It can be a string of any content that you wish. A randomly generated unique value is
     * typically used for preventing cross-site request forgery attacks. The value can also
     * encode information about the user's state in the app before the authentication request
     * occurred, such as the page or view they were on.
     */
    @SerializedName("state")
    private String mState;

    /**
     * Scopes scopes that you want the user to consent to is required for V2 auth request.
     */
    @SerializedName("scope")
    private String mScope;

    /**
     * Specifies the method that should be used to send the resulting token back to your app.
     * Can be query, fragment, or form_post. query provides the code as a query string parameter on your redirect URI.
     */
    @SerializedName("response_mode")
    private String mResponseMode;

    /**
     * Constructor of AuthorizationRequest.
     */
    protected AuthorizationRequest(final Builder builder) {
        mResponseType = builder.mResponseType;
        mClientId = builder.mClientId;
        mRedirectUri = builder.mRedirectUri;
        mState = builder.mState;
        mScope = builder.mScope;
        mResponseMode = builder.mResponseMode;
    }

    public static final class ResponseType {
        public static final String CODE = "code";
    }

    public static final class ResponseMode {
        public static final String QUERY = "query";

        public static final String FRAGMENT = "fragment";

        public static final String FORM_POST = "form_post";
    }

    public static abstract class Builder<T> {
        private String mResponseType = ResponseType.CODE; //ResponseType.CODE as default.
        private String mResponseMode = ResponseMode.QUERY; //ResponseMode.QUERY as default.
        private String mClientId;
        private String mRedirectUri;
        private String mState;
        private String mScope;

        public Builder(@NonNull final String clientId,
                       @NonNull final String redirectUri) {
            setClientId(clientId);
            setRedirectUri(redirectUri);
        }

        public Builder setResponseType(String responseType) {
            mResponseType = responseType;
            return this;
        }

        public Builder setResponseMode(String responseMode) {
            mResponseMode = responseMode;
            return this;
        }

        public Builder setClientId(String clientId) {
            mClientId = clientId;
            return this;
        }

        public Builder setRedirectUri(String redirectUri) {
            mRedirectUri = redirectUri;
            return this;
        }

        public Builder setState(String state) {
            mState = state;
            return this;
        }

        public Builder setScope(String scope) {
            mScope = scope;
            return this;
        }

        public abstract T build();
    }
//
//    /**
//     * Return the start URL to load in the web view.
//     *
//     * @return String of start URL.
//     * @throws UnsupportedEncodingException
//     * @throws ClientException
//     */
//    public abstract String getAuthorizationStartUrl() throws UnsupportedEncodingException, ClientException;

    /**
     * @return Response type of the authorization request.
     */
    public String getResponseType() {
        return mResponseType;
    }

    /**
     * @return Response mode of the authorization request.
     */
    public String getResponseMode() {
        return mResponseMode;
    }

    /**
     * @return mClientId of the authorization request.
     */
    public String getClientId() {
        return mClientId;
    }

    /**
     * @return mRedirectUri of the authorization request.
     */
    public String getRedirectUri() {
        return mRedirectUri;
    }

    /**
     * @return mState of the authorization request.
     */
    public String getState() {
        return mState;
    }

    //CHECKSTYLE:OFF
    @Override
    public String toString() {
        return "AuthorizationRequest{" +
                "mResponseType='" + mResponseType + '\'' +
                ", mClientId='" + mClientId + '\'' +
                ", mRedirectUri='" + mRedirectUri + '\'' +
                ", mScope='" + mScope + '\'' +
                ", mState='" + mState + '\'' +
                '}';
    }

    public String getScope() {
        return mScope;
    }

    public abstract String getAuthorizationEndpoint();

    public Uri getAuthorizationRequestAsHttpRequest() throws UnsupportedEncodingException {
        Uri.Builder uriBuilder = Uri.parse(getAuthorizationEndpoint()).buildUpon();
        for (Map.Entry<String, String> entry : ObjectMapper.serializeObjectHashMap(this).entrySet()){
            uriBuilder.appendQueryParameter(entry.getKey(), entry.getValue());
        }

        return uriBuilder.build();
    }
}
