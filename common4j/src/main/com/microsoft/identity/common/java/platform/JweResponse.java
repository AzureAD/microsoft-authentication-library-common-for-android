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
import edu.umd.cs.findbugs.annotations.Nullable;
import io.opentelemetry.api.trace.Span;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;


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

    @NonNull
    private JweHeader mJweHeader;

    @NonNull
    private String mEncryptedKey;

    @NonNull
    private String mIv;

    @NonNull
    private String mPayload;

    /**
     * Tag may not be present for all algorithms. Hence marked nullable
     */
    @Nullable
    private String mAuthenticationTag;

    /**
     * Additional Authenticated Data. This is encoded header read as ASCII.
     */
    private String mAAD;

    public JweHeader getJweHeader() {
        return mJweHeader;
    }

    public byte[] getEncryptedKey() {
        return StringUtil.base64Decode(this.mEncryptedKey, Base64.URL_SAFE, "Encrypted key is not base64 url-encoded");
    }

    public byte[] getIv() {
        return StringUtil.base64Decode(this.mIv, Base64.URL_SAFE, "IV not base64 url-encoded.");
    }

    public byte[] getPayload() {
        return StringUtil.base64Decode(this.mPayload, Base64.URL_SAFE, "Payload is not base64 url-encoded.");
    }

    public byte[] getAuthenticationTag() {
        if (this.mAuthenticationTag != null) {
            return StringUtil.base64Decode(this.mAuthenticationTag, Base64.URL_SAFE, "Tag is not base64 url-encoded");
        }

        return null;
    }

    public byte[] getAAD() {
        return this.mAAD.getBytes(AuthenticationConstants.CHARSET_ASCII);
    }

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
        response.mEncryptedKey = split[1];
        response.mIv = split[2];
        response.mPayload = split[3];

        // AAD is header read as ASCII
        response.mAAD = header;

        if (split.length > 4) {
            response.mAuthenticationTag = split[4];
        }

        final byte[] headerDecodedBytes = StringUtil.base64Decode(header, Base64.URL_SAFE, "Header is not base url-encoded");
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
}
