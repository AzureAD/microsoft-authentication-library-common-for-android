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

package com.microsoft.identity.common.java.platform;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.util.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.extras.Base64;
import io.opentelemetry.api.trace.Span;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

@Getter
@Accessors(prefix = "m")
public class JweResponse {

    private static final String TAG = JweResponse.class.getSimpleName();
    /**
     * The code defines a valid JWE as one that has at least the header, encryptedKey, IV,
     * and Payload.
     */
    private static final int LENGTH_OF_VALID_JWE = 4;

    @Builder
    @Getter
    @Accessors(prefix = "m")
    @EqualsAndHashCode
    public static class JweHeader {
        private final String mAlgorithm;

        private final String mType;

        private final String mX509CertificateThumbprint;

        private final String mX509Certificate;

        private final String mKeyID;

        private final String mKeyUse;

        private final String mEncryptionAlgorithm;

        private final String mContext;
    }

    JweHeader mJweHeader;

    byte[] mEncryptedKey;

    byte[] mIv;

    byte[] mPayload;

    byte[] mAuthenticationTag;

    /**
     * Additional Authenticated Data
     */
    byte[] mAAD;

    public static JweResponse parseJwe(@NonNull final String jwe) throws JSONException {
        final Span span = SpanExtension.current();
        final JweResponse response = new JweResponse();

        final String[] split = jwe.split("\\.");

        span.setAttribute(AttributeName.jwt_valid.name(), split.length >= LENGTH_OF_VALID_JWE);

        if (split.length < LENGTH_OF_VALID_JWE) {
            throw new IllegalArgumentException("Invalid JWE");
        }

        // NOTE: EVOsts sends mIv and mPayload as Base64UrlEncoded
        final String header = split[0];
        response.mEncryptedKey = base64Decode(split[1], Base64.URL_SAFE, "Encrypted key is not base64 url-encoded");
        response.mIv = base64Decode(split[2], Base64.URL_SAFE, "IV not base64 url-encoded.");
        response.mPayload = base64Decode(split[3], Base64.URL_SAFE, "Payload is not base64 url-encoded.");

        // AAD is header read as ASCII
        response.mAAD = header.getBytes(AuthenticationConstants.CHARSET_ASCII);

        if (split.length > 4) {
            response.mAuthenticationTag = Base64.decode(split[4], Base64.URL_SAFE);
        }

        final byte[] headerDecodedBytes = Base64.decode(header, Base64.URL_SAFE);
        final String decodedHeader = StringUtil.fromByteArray(headerDecodedBytes);

        final JSONObject jsonObject = new JSONObject(decodedHeader);

        span.setAttribute(AttributeName.jwt_alg.name(), jsonObject.optString("alg"));

        response.mJweHeader = JweHeader.builder()
                .algorithm(jsonObject.optString("alg"))
                .type(jsonObject.optString("typ"))
                .x509CertificateThumbprint(jsonObject.optString("x5t"))
                .x509Certificate(jsonObject.optString("x5c"))
                .keyID(jsonObject.optString("kid"))
                .keyUse(jsonObject.optString("use"))
                .encryptionAlgorithm(jsonObject.optString("enc"))
                .context(jsonObject.optString("ctx"))
                .build();

        return response;
    }

    /***
     * Helper to perform base64 decoding with logging.
     * @param input Input string
     * @param flags
     * @param failureMessage The message to log in case of failure.
     */
    public static byte[] base64Decode(@NonNull final String input, int flags, @NonNull final String failureMessage) {
        final String methodTag = TAG + ":base64Decode";
        try {
            return Base64.decode(input, flags);
        } catch (IllegalArgumentException e) {
            Logger.error(methodTag, failureMessage + " " + e.getMessage(), null);
            throw e;
        }
    }
}
