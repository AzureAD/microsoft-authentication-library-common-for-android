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

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

import com.google.gson.Gson;
import com.microsoft.identity.common.java.base64.Base64Util;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.util.MsaUtil;

import java.util.Arrays;


import javax.annotation.Nullable;

import lombok.NonNull;
import lombok.experimental.UtilityClass;

/**
 * Utility class to hold methods related to JWT processing.
 */
@UtilityClass
public final class JwtUtils {

    private static final String TAG = JwtUtils.class.getSimpleName();

    /**
     * Helper method to generate generic String key/value pair JWT.
     * This method combines the header and the body of the JWT and returns
     * a combined string to the caller.
     *
     * @param header Jwt request header
     * @param body Jwt request body
     * @return String Base64URLSafe(mJweHeader)+Base64URLSafe(body)
     */
    public static String generateJWT(@NonNull final JwtRequestHeader header,
                                     @NonNull final JwtRequestBody body) {
        final String methodTag = TAG + ":generateJWT";
        Logger.verbose(methodTag, "Generating JWT.");
        final String headerJson = new Gson().toJson(header);
        final String bodyJson = new Gson().toJson(body);
        final String encodedJwt =
                Base64Util.encodeUrlSafeString(headerJson.getBytes(ENCODING_UTF8)) + "." + Base64Util.encodeUrlSafeString(bodyJson.getBytes(ENCODING_UTF8));
        return encodedJwt;
    }

    /**
     * Generate a JWT request header to be used in MSA DR flows.
     * @param keyContext key context to include in header
     * @return a request headers object
     */
    @NonNull
    public static JwtRequestHeader generateJwtRequestHeaderForMsaDR(@Nullable final byte[] keyContext) {
        final JwtRequestHeader jwtRequestHeader = new JwtRequestHeader();
        jwtRequestHeader.setType();
        jwtRequestHeader.setAlg(JwtRequestHeader.ALG_VALUE_HS256);
        jwtRequestHeader.setKId(JwtRequestHeader.KID_VALUE_ECDH);
        jwtRequestHeader.setCtx(Arrays.toString(keyContext));

        return jwtRequestHeader;
    }

    /**
     * Generate a JWT request body to be used in MSA DR flows.
     * @param deviceToken device token to include in body
     * @return a request body object
     */
    @NonNull
    public static JwtRequestBody generateJwtRequestBodyForMsaDR(@Nullable final String audienceUrl, @Nullable final String nonce, @Nullable final String deviceToken) {
        final JwtRequestBody jwtRequestBody = new JwtRequestBody();
        jwtRequestBody.setAudience(audienceUrl);
        jwtRequestBody.setNonce(nonce);
        jwtRequestBody.setPurpose(MsaUtil.Companion.getJwtPurpose());
        jwtRequestBody.setGrantType(TokenRequest.GrantTypes.DEVICE_AUTH);
        jwtRequestBody.setDeviceToken(deviceToken);

        return jwtRequestBody;
    }
}

