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

import com.microsoft.identity.common.java.util.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.extras.Base64;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;

/***
 * This class represents JSON Web Encryption (JWE) content serialized using the JWE Compact
 * Serialization Format
 *
 * FORMAT:
 * BASE64URL(UTF8(JWE Protected Header)) || '.' ||
 * BASE64URL(JWE Encrypted Key) || '.' ||
 * BASE64URL(JWE Initialization Vector) || '.' ||
 * BASE64URL(JWE Ciphertext) || '.' ||
 * BASE64URL(JWE Authentication Tag)
 *
 * The JWE Spec describing the JWE Compact Serialization Format is found <a href="https://datatracker.ietf.org/doc/html/rfc7516#section-3.1">here</a>
 */
public class JweCompactSerialization {

    @Builder
    @Getter
    @Accessors(prefix = "m")
    @EqualsAndHashCode
    public static class JweProtectedHeader {
        private final String mAlgorithm;

        private final String mType;

        private final String mX509CertificateThumbprint;

        private final String mX509Certificate;

        private final String mKeyID;

        private final String mKeyUse;

        private final String mEncryptionAlgorithm;

        private final String mContext;
    }

    JweProtectedHeader mJweProtectedHeader;

    String mEncryptedKey;

    String mInitializationVector;

    String mCipherText;

    String mAuthenticationTag;

    public JweProtectedHeader getJweProtectedHeader() {
        return mJweProtectedHeader;
    }

    public String getEncryptedKey() {
        return mEncryptedKey;
    }

    public String getInitializationVector() {
        return mInitializationVector;
    }

    public String getCipherText() {
        return mCipherText;
    }

    public String getAuthenticationTag() {
        return mAuthenticationTag;
    }

    public static JweCompactSerialization parseJwe(String jwe) throws JSONException {
        JweCompactSerialization response = new JweCompactSerialization();

        String[] split = jwe.split("\\.");
        if (split.length < 4) {
            throw new IllegalArgumentException("Invalid JWE");
        }

        String header = split[0];
        response.mEncryptedKey = split[1];
        response.mInitializationVector = split[2];
        response.mCipherText = split[3];

        if (split.length > 4) {
            response.mAuthenticationTag = split[4];
        }

        byte[] headerDecodedBytes = Base64.decode(header, Base64.URL_SAFE);
        String decodedHeader = StringUtil.fromByteArray(headerDecodedBytes);

        JSONObject jsonObject = new JSONObject(decodedHeader);
        response.mJweProtectedHeader = JweProtectedHeader.builder()
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
