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
 * Represents header in a JWT. These JWTs can be used in token requests
 */
public final class JwtRequestHeader extends AbstractJwtRequest {

    private static final String JWT_VALUE = "JWT";

    // HMAC using SHA256 - symmetric key signing algorithm
    public static final String ALG_VALUE_HS256 = "HS256";

    // RSA using SHA256 - asymmetric key signing algorithm
    public static final String ALG_VALUE_RS256 = "RS256";

    @SerializedName("typ")
    private String mType;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("alg")
    private String mAlg;

    @Setter
    @Accessors(prefix = "m")
    @SerializedName("kid")
    private String mKId;

    public void setType() {
        mType = JWT_VALUE;
    }
}
