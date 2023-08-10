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
package com.microsoft.identity.common.java.jwt;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

@Getter
@Setter
@Accessors(prefix = "m")
public abstract class AbstractJwtRequest {

    public static class ClaimNames {
        public static final String CTX = "ctx";
        public static final String REFRESH_TOKEN = "refresh_token";
        public static final String X5C = "x5c";
        public static final String CLIENT_ID = "child_client_id";
        public static final String BRK_CLIENT_ID = "client_id";
        public static final String SCOPE = "scope";
        public static final String AUDIENCE = "aud";
        public static final String ISSUER = "iss";
        public static final String GRANT_TYPE = "grant_type";
        public static final String NONCE = "request_nonce";
        public static final String REDIRECT_URI = "child_redirect_uri";
        public static final String BRK_REDIRECT_URI = "redirect_uri";
        public static final String RESOURCE = "resource";
        public static final String USE = "use";
        public static final String ALG = "alg";
        public static final String KID = "kid";
        public static final String TYPE = "typ";
        public static final String IAT = "iat";
        public static final String NBF = "nbf";
        public static final String EXP = "exp";
        public static final String ASSERTION = "assertion";
    }

    @SerializedName(ClaimNames.REFRESH_TOKEN)
    private String mRefreshToken;

    @SerializedName(ClaimNames.X5C)
    private String mCert;

    @SerializedName(ClaimNames.CLIENT_ID)
    private String mClientId;

    @SerializedName(ClaimNames.BRK_CLIENT_ID)
    private String mBrkClientId;

    @SerializedName(ClaimNames.USE)
    private String mUse;

    @SerializedName(ClaimNames.RESOURCE)
    private String mResource;

    // HACKHACK: once AAD fixes the bug where it cannot accept scopes unless there is a resource,
    // remove this and fix all compiler errors and unused variable warnings
    public void setResource(final String resource) {
        if (!StringUtil.isNullOrEmpty(resource)) {
            mResource = resource;
        }
    }
}
