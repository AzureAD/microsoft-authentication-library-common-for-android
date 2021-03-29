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

package com.microsoft.identity.common.internal.platform;

import android.util.Base64;

import org.json.JSONException;
import org.json.JSONObject;

import java.nio.charset.Charset;

public class JweResponse {

    public static final Charset UTF_8 = Charset.forName("UTF-8");

    public static class JweHeader {
        public String mHeaderAlg;

        public String mHeaderType;

        public String mHeaderX509CertificateThumbprint;

        public String mHeaderX509Certificate;

        public String mHeaderKeyID;

        public String mHeaderKeyUse;

        public String mHeaderEncryptionAlgorithm;

        public String mHeaderContext;
    }

    JweHeader mJweHeader;

    String mEncryptedKey;

    String mIv;

    String mPayload;

    String mAuthenticationTag;

    public JweHeader getJweHeader() {
        return mJweHeader;
    }

    public String getEncryptedKey() {
        return mEncryptedKey;
    }

    public String getIV() {
        return mIv;
    }

    public String getPayload() {
        return mPayload;
    }

    public String getAuthenticationTag() {
        return mAuthenticationTag;
    }

    public static JweResponse parseJwe(String jwe) throws JSONException {
        JweResponse response = new JweResponse();

        String[] split = jwe.split("\\.");
        if (split.length < 4) {
            throw new IllegalArgumentException("Invalid JWE");
        }

        String header = split[0];
        response.mEncryptedKey = split[1];
        response.mIv = split[2];
        response.mPayload = split[3];

        if (split.length > 4) {
            response.mAuthenticationTag = split[4];
        }

        byte[] headerDecodedBytes = Base64.decode(header, Base64.URL_SAFE);
        String decodedHeader = new String(headerDecodedBytes, UTF_8);

        JSONObject jsonObject = new JSONObject(decodedHeader);
        JweHeader headerJson = new JweHeader();

        headerJson.mHeaderAlg = jsonObject.optString("alg");
        headerJson.mHeaderType = jsonObject.optString("typ");
        headerJson.mHeaderX509CertificateThumbprint = jsonObject.optString("x5t");
        headerJson.mHeaderX509Certificate = jsonObject.optString("x5c");
        headerJson.mHeaderKeyID = jsonObject.optString("kid");
        headerJson.mHeaderKeyUse = jsonObject.optString("use");
        headerJson.mHeaderEncryptionAlgorithm = jsonObject.optString("enc");
        headerJson.mHeaderContext = jsonObject.optString("ctx");
        response.mJweHeader = headerJson;

        return response;
    }
}
