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
package java.com.microsoft.identity.labapi.utilities.authentication.common.port;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jose.util.Base64;
import com.nimbusds.jose.util.Base64URL;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * This class is used to create a client assertion per the following documentation.
 * https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-certificate-credentials
 */
@Getter
@Accessors(prefix = "m")
public class MicrosoftClientAssertion extends ClientAssertion {

    private static final String CLIENT_ASSERTION_TYPE = "urn:ietf:params:oauth:client-assertion-type:jwt-bearer";
    private static final String THUMBPRINT_ALGORITHM = "SHA-1";
    private static final int ONE_MINUTE_MILLIS = Math.toIntExact(TimeUnit.MINUTES.toMillis(1));

    private MicrosoftClientAssertion(@NonNull final String clientAssertion,
                                     @NonNull final String clientAssertionType) {
        super(clientAssertion, clientAssertionType);
    }

    public static MicrosoftClientAssertionBuilder builder() {
        return new MicrosoftClientAssertionBuilder();
    }

    public static class MicrosoftClientAssertionBuilder {
        private String clientId;
        private String audience;
        private CertificateCredential certificateCredential;

        MicrosoftClientAssertionBuilder() {
        }

        public MicrosoftClientAssertion build() {
            final SignedJWT assertion = createSignedJwt(clientId, audience, certificateCredential);
            return new MicrosoftClientAssertion(assertion.serialize(), CLIENT_ASSERTION_TYPE);
        }

        public MicrosoftClientAssertionBuilder clientId(String clientId) {
            this.clientId = clientId;
            return this;
        }

        public MicrosoftClientAssertionBuilder audience(String audience) {
            this.audience = audience;
            return this;
        }

        public MicrosoftClientAssertionBuilder certificateCredential(CertificateCredential certificateCredential) {
            this.certificateCredential = certificateCredential;
            return this;
        }

        public String toString() {
            return "MicrosoftClientAssertion.MicrosoftClientAssertionBuilder(clientId=" + this.clientId + ", audience=" + this.audience + ", certificateCredential=" + this.certificateCredential + ")";
        }

        @SuppressWarnings("deprecation")
        private SignedJWT createSignedJwt(String clientId, String audience, CertificateCredential credential) {

            final long time = System.currentTimeMillis();

            final JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .audience(audience)
                    .issuer(clientId)
                    .notBeforeTime(new Date(time))
                    .expirationTime(new Date(time
                            + ONE_MINUTE_MILLIS))
                    .subject(clientId)
                    .build();

            SignedJWT jwt;

            try {
                JWSHeader.Builder builder = new JWSHeader.Builder(JWSAlgorithm.RS256);
                List<Base64> certs = new ArrayList<Base64>();
                certs.add(Base64.encode(credential.getPublicCertificate().getEncoded()));
                builder.x509CertChain(certs);
                //x509CertThumbprint has been deprecated.  We have to keep using this since this is the only thing that AAD accepts.
                builder.x509CertThumbprint(createSHA1ThumbPrint(credential.getPublicCertificate()));

                jwt = new SignedJWT(builder.build(), claimsSet);
                final RSASSASigner signer = new RSASSASigner(credential.getPrivateKey());

                jwt.sign(signer);
            } catch (final Exception e) {
                throw new RuntimeException("exception in createSignedJwt", e);
            }

            return jwt;
        }

        private Base64URL createSHA1ThumbPrint(X509Certificate clientCertificate)
                throws CertificateEncodingException, NoSuchAlgorithmException {

            Base64URL thumbprint;

            MessageDigest mdSha1 = MessageDigest.getInstance(THUMBPRINT_ALGORITHM);
            mdSha1.reset();
            mdSha1.update(clientCertificate.getEncoded());
            thumbprint = new Base64URL(Base64.encode(mdSha1.digest()).toString());

            return thumbprint;
        }
    }
}