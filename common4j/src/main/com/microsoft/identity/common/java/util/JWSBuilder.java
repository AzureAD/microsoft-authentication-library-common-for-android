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

package com.microsoft.identity.common.java.util;

import static com.microsoft.identity.common.java.AuthenticationConstants.ENCODING_UTF8;

import com.google.gson.Gson;
import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.java.crypto.DefaultCryptoFactory;
import com.microsoft.identity.common.java.crypto.ISigner;
import com.microsoft.identity.common.java.crypto.BasicSigner;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.logging.Logger;

import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;

import cz.msebera.android.httpclient.extras.Base64;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * JWS response builder for certificate challenge response.
 */
public class JWSBuilder {
    private static final long SECONDS_MS = 1000L;

    /**
     * Algorithm is fixed to RSA PKCS v1.5.
     */
    private static final String JWS_HEADER_ALG = "RS256";

    /**
     * Algorithm name for signing.
     */
    private static final String SIGNING_ALGORITHM = "SHA256withRSA";

    private static final String TAG = "JWSBuilder";

    // TODO[FIPS] exposes a constructor that takes in an ISigner/ICryptoFactory.
    private static final ISigner sSigner = new BasicSigner(new DefaultCryptoFactory());

    /**
     * Payload for JWS.
     */
    @SuppressFBWarnings({"URF_UNREAD_FIELD"})
    static final class Claims {
        @SerializedName("aud")
        private String mAudience;

        @SerializedName("iat")
        private long mIssueAt;

        @SerializedName("nonce")
        private String mNonce;

        /**
         * No args constructor for use in serialization for Gson to prevent usage of sun.misc.Unsafe.
         */
        @SuppressWarnings("unused")
        private Claims() {
        }
    }

    /**
     * Header that includes algorithm, type, thumbprint, keys, and keyid.
     */
    @SuppressFBWarnings({"URF_UNREAD_FIELD"})
    static final class JwsHeader {
        @SerializedName("alg")
        private String mAlgorithm;

        @SerializedName("typ")
        private String mType;

        @SerializedName("x5c")
        private String[] mCert;

        /**
         * No args constructor for use in serialization for Gson to prevent usage of sun.misc.Unsafe.
         */
        @SuppressWarnings("unused")
        private JwsHeader() {
        }
    }

    /**
     * Generate the signed JWT.
     */
    public String generateSignedJWT(String nonce, String audience, PrivateKey privateKey,
                                    PublicKey pubKey, X509Certificate cert) throws ClientException {
        // http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-25
        // In the JWS Compact Serialization, a JWS object is represented as the
        // combination of these three string values,
        // BASE64URL(UTF8(JWS Protected Header)),
        // BASE64URL(JWS Payload), and
        // BASE64URL(JWS Signature),
        // concatenated in that order, with the three strings being separated by
        // two period ('.') characters.
        // Base64 encoding without padding, wrapping and urlsafe.
        final String methodName = ":generateSignedJWT";
        if (StringUtil.isNullOrEmpty(nonce)) {
            throw new IllegalArgumentException("nonce");
        }
        if (StringUtil.isNullOrEmpty(audience)) {
            throw new IllegalArgumentException("audience");
        }
        if (privateKey == null) {
            throw new IllegalArgumentException("privateKey");
        }
        if (pubKey == null) {
            throw new IllegalArgumentException("pubKey");
        }

        Gson gson = new Gson();
        Claims claims = new Claims();
        claims.mNonce = nonce;
        claims.mAudience = audience;
        claims.mIssueAt = System.currentTimeMillis() / SECONDS_MS;

        JwsHeader header = new JwsHeader();
        header.mAlgorithm = JWS_HEADER_ALG;
        header.mType = "JWT"; // recommended UpperCase in JWT Spec

        final String signingInput;
        final String signature;
        try {

            // Server side expects x5c in the header to verify the signer and
            // lookup the certificate from device registration
            // Each string in the array is a base64
            // encoded ([RFC4648] Section 4 -- not base64url encoded) DER
            // [ITU.X690.1994] PKIX certificate value. The certificate
            // containing the public key corresponding to the key used
            // to digitally sign the JWS MUST be the first certificate
            // http://tools.ietf.org/html/draft-ietf-jose-json-web-signature-27
            header.mCert = new String[1];
            header.mCert[0] = Base64.encodeToString(cert.getEncoded(), Base64.NO_WRAP);

            // redundant but current ADFS code base is looking for
            String headerJsonString = gson.toJson(header);
            String claimsJsonString = gson.toJson(claims);
            Logger.verbose(TAG + methodName, "Generate client certificate challenge response JWS Header. ");
            signingInput = StringUtil.encodeUrlSafeString(headerJsonString)
                    + "."
                    + StringUtil.encodeUrlSafeString(claimsJsonString);
            signature = StringUtil.encodeUrlSafeString(
                    sSigner.sign(privateKey, SIGNING_ALGORITHM, signingInput.getBytes(ENCODING_UTF8)));
        } catch (final CertificateEncodingException e) {
            throw new ClientException(ErrorStrings.CERTIFICATE_ENCODING_ERROR,
                    "Certificate encoding error", e);
        }
        return signingInput + "." + signature;
    }
}