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
import com.microsoft.identity.common.java.commands.parameters.IHasExtraParameters;

import java.util.Map;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.NonNull;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;
import edu.umd.cs.findbugs.annotations.Nullable;

/**
 * A class holding the state of the Token Request (oAuth2).
 * OAuth2 Spec: https://tools.ietf.org/html/rfc6749#section-4.1.3
 * OAuth2 Client Authentication: https://tools.ietf.org/html/rfc7521#section-4.2
 * This should include all fo the required parameters of the token request for oAuth2
 * This should provide an extension point for additional parameters to be set
 * <p>
 * Includes support for client assertions per the specs:
 * https://tools.ietf.org/html/rfc7521
 * https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-v2-protocols-oauth-client-creds
 */
@SuperBuilder
@AllArgsConstructor
@NoArgsConstructor
@Setter
@Getter
@Accessors(prefix="m")
public class TokenRequest implements IHasExtraParameters {

    private static final Names[] names = Names.values();

    enum Names implements Function<TokenRequest, String> {
        grant_type {
            @Override
            public String apply(TokenRequest r) {
                return r.getGrantType();
            }
        },
        code {
            @Override
            public String apply(TokenRequest r) {
                return r.getCode();
            }
        },
        redirect_uri {
            @Override
            public String apply(TokenRequest r) {
                return r.getRedirectUri();
            }
        },
        client_id {
            @Override
            public String apply(TokenRequest r) {
                return r.getClientId();
            }
        },
        client_secret {
            @Override
            public String apply(TokenRequest r) {
                return r.getClientSecret();
            }
        },
        client_assertion_type {
            @Override
            public String apply(TokenRequest r) {
                return r.getClientAssertionType();
            }
        },
        client_assertion {
            @Override
            public String apply(TokenRequest r) {
                return r.getClientAssertion();
            }
        },
        scope {
            @Override
            public String apply(TokenRequest r) {
                return r.getScope();
            }
        },
        refresh_token {
            @Override
            public String apply(TokenRequest r) {
                return r.getRefreshToken();
            }
        },
        token_type {
            @Override
            public String apply(TokenRequest r) {
                return r.getTokenType();
            }
        },
        req_cnf {
            @Override
            public String apply(TokenRequest r) {
                return r.getRequestConfirmation();
            }
        };
    }

    public String get(@NonNull String paramName) {
        if (getExtraParameters() != null) {
            for (Map.Entry<String, String> e : getExtraParameters()) {
                if (paramName.equals(e.getKey())) {
                    return e.getValue();
                }
            }
        }
        for (Names name: names) {
            if (name.name().equals(paramName)) {
                return name.apply(this);
            }
        }
        return null;
    }

    @Expose()
    @SerializedName("grant_type")
    private String mGrantType;

    /**
     * Auth code to use for a token request using auth code flow.
     */
    @SerializedName("code")
    private String mCode;

    /**
     * The redirect uri of the client making the token request.
     */
    @Expose()
    @SerializedName("redirect_uri")
    private String mRedirectUri;

    /**
     * The client id of the application on behalf of which this request is being made.
     */
    @Expose()
    @SerializedName("client_id")
    private String mClientId;

    /**
     * The client secret to use.
     */
    @SerializedName("client_secret")
    private String mClientSecret;

    /**
     * The client assertions.
     */
    @Expose()
    @SerializedName("client_assertion_type")
    private String mClientAssertionType;

    @SerializedName("client_assertion")
    private String mClientAssertion;

    @Expose()
    @SerializedName("scope")
    private String mScope;

    @SerializedName("refresh_token")
    private String mRefreshToken;

    /**
     * The token type.
     */
    @Expose()
    @SerializedName("token_type")
    private String mTokenType;

    @SerializedName("req_cnf")
    private String mRequestConfirmation;

    public String getRequestConfirmation() {
        return mRequestConfirmation;
    }

    public void setRequestConfirmation(@Nullable final String requestConfirmation) {
        mRequestConfirmation = requestConfirmation;
    }

    private transient Iterable<Map.Entry<String, String>> mExtraParameters;

    @Override
    public synchronized Iterable<Map.Entry<String, String>> getExtraParameters() {
        return mExtraParameters;
    }

    @Override
    public synchronized void setExtraParameters(final Iterable<Map.Entry<String, String>> extraParams) {
        mExtraParameters = extraParams;
    }

    public static class GrantTypes {
        public static final String AUTHORIZATION_CODE = "authorization_code";
        public static final String REFRESH_TOKEN = "refresh_token";
        public static final String PASSWORD = "password";
        public static final String DEVICE_CODE = "urn:ietf:params:oauth:grant-type:device_code";
        public final static String CLIENT_CREDENTIALS = "client_credentials";
    }

    public static class TokenType {
        public static final String POP = "pop";
    }

}
