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

import com.google.gson.JsonObject;
import com.google.gson.annotations.SerializedName;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Represents body of JWT. These JWTs can be used in token requests.
 */
@Getter
@Setter
@Accessors(prefix = "m")
public final class JwtRequestBody extends AbstractJwtRequest {

    @SerializedName(ClaimNames.SCOPE)
    private String mJwtScope;

    @SerializedName(ClaimNames.AUDIENCE)
    private String mAudience;

    @SerializedName(ClaimNames.ISSUER)
    private String mIssuer;

    @SerializedName(ClaimNames.ASSERTION)
    private String mAssertion;

    @SerializedName(ClaimNames.GRANT_TYPE)
    private String mGrantType;

    @SerializedName(ClaimNames.NONCE)
    private String mNonce;

    @SerializedName(ClaimNames.REDIRECT_URI)
    private String mRedirectUri;


    /**
     * The hub/brk redirectUri for the request.
     * <a href="https://identitydivision.visualstudio.com/DevEx/_git/AuthLibrariesApiReview/pullrequest/7876">...</a>
     */
    @SerializedName(ClaimNames.BRK_REDIRECT_URI)
    private String mBrkRedirectUri;

    @Setter(AccessLevel.NONE)
    @SerializedName(ClaimNames.IAT)
    private String mIat;

    @Setter(AccessLevel.NONE)
    @SerializedName(ClaimNames.NBF)
    private String mNbf;

    @Setter(AccessLevel.NONE)
    @SerializedName(ClaimNames.EXP)
    private String mExp;

    @SerializedName(ClaimNames.JWE_CRYPTO)
    private JsonObject mJweCrypto;

    @SerializedName(ClaimNames.SESSION_KEY_CRYPTO)
    private JsonObject mSessionKeyCrypto;

    @SerializedName(ClaimNames.PURPOSE)
    private String mPurpose;

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
