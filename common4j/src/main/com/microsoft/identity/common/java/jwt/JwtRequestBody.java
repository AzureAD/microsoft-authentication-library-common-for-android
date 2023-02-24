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

import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Represents body of JWT. These JWTs can be used in token requests.
 */
public final class JwtRequestBody extends AbstractJwtRequest {

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("scope")
    private String mJwtScope;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("aud")
    private String mAudience;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("iss")
    private String mIssuer;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("assertion")
    private String mAssertion;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("grant_type")
    private String mGrantType;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("request_nonce")
    private String mNonce;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("redirect_uri")
    private String mRedirectUri;

    @SerializedName("iat")
    private String mIat;

    @SerializedName("nbf")
    private String mNbf;

    @SerializedName("exp")
    private String mExp;

    public void setIat(final long iat) {
        mIat = String.valueOf(iat);
    }

    public void setNBF(final long nbf) {
        mNbf = String.valueOf(nbf);
    }

    public void setExp(final long exp, final long buffer) {
        mExp = String.valueOf(exp + buffer);
    }
}
