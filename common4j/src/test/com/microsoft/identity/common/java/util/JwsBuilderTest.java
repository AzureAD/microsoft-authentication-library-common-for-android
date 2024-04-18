//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.util;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.challengehandlers.IDeviceCertificate;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.util.base64.Base64Flags;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.EnumSet;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.NonNull;

@RunWith(JUnit4.class)
public class JwsBuilderTest {

    private final String MOCK_SIGNATURE = "-signed-";
    private final String MOCK_NONCE = "nonce_mock";
    private final String MOCK_AUDIENCE =  "aud_mock";
    private final String MOCK_ENCODED_CERT_VALUE = "EncodedCertValue";
    private final long MOCK_TIME = 123456789L;

    @AllArgsConstructor
    public static class JWSBuilderMock extends JWSBuilder {

        private final long mockTimeInSeconds;

        @Override
        protected long getCurrentTimeInSeconds() {
            return mockTimeInSeconds;
        }

        @Override
        protected String encodeUrlSafeString(byte[] dataToEncode) {
            // Do nothing.
            return StringUtil.fromByteArray(dataToEncode);
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateSignedJwt_EmptyNonce() throws Exception {
        new JWSBuilderMock(MOCK_TIME).generateSignedJWT(
                "",
                MOCK_AUDIENCE,
                getMockCertificate(MOCK_ENCODED_CERT_VALUE, MOCK_SIGNATURE));
    }

    @Test(expected = NullPointerException.class)
    public void testGenerateSignedJwt_NullNonce() throws Exception {
        new JWSBuilderMock(MOCK_TIME).generateSignedJWT(
                null,
                MOCK_AUDIENCE,
                getMockCertificate(MOCK_ENCODED_CERT_VALUE, MOCK_SIGNATURE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void testGenerateSignedJwt_EmptyAudience() throws Exception {
        new JWSBuilderMock(MOCK_TIME).generateSignedJWT(
                MOCK_AUDIENCE,
                "",
                getMockCertificate(MOCK_ENCODED_CERT_VALUE, MOCK_SIGNATURE));
    }

    @Test(expected = NullPointerException.class)
    public void testGenerateSignedJwt_NullAudience() throws Exception {
        new JWSBuilderMock(MOCK_TIME).generateSignedJWT(
                MOCK_AUDIENCE,
                null,
                getMockCertificate(MOCK_ENCODED_CERT_VALUE, MOCK_SIGNATURE));
    }

    @Test
    public void testGenerateSignedJwt_MalformedCert() throws Exception {
        final JWSBuilder builder = new JWSBuilderMock(MOCK_TIME);

        final X509Certificate mockX509 = mock(X509Certificate.class);
        when(
                mockX509.getEncoded()
        ).thenThrow(
                new CertificateEncodingException("Failed to encode cert for some reason.")
        );

        try {
            builder.generateSignedJWT(
                    MOCK_NONCE,
                    MOCK_AUDIENCE,
                    getMockCertificate(mockX509, MOCK_SIGNATURE));
            Assert.fail();
        } catch (final ClientException e) {
            Assert.assertEquals(ErrorStrings.CERTIFICATE_ENCODING_ERROR, e.getErrorCode());
        }
    }

    @Test
    public void testGenerateSignedJwt_MockEncoding() throws Exception{
        final JWSBuilder builder = new JWSBuilderMock(MOCK_TIME);
        final String result = builder.generateSignedJWT(
                MOCK_NONCE,
                MOCK_AUDIENCE,
                getMockCertificate(MOCK_ENCODED_CERT_VALUE, MOCK_SIGNATURE));
        Assert.assertEquals(
                getMockNonEncodedResponse(
                        MOCK_TIME,
                        MOCK_NONCE,
                        MOCK_AUDIENCE,
                        MOCK_ENCODED_CERT_VALUE,
                        MOCK_SIGNATURE),
                result);
    }

    private static String getMockNonEncodedResponse(final long mockCurrentTime,
                                                    final String nonce,
                                                    final String aud,
                                                    final String encodedCert,
                                                    final String signature){
        final String headerJsonString = "{\"alg\":\"RS256\",\"typ\":\"JWT\",\"x5c\":[\"" + encodedCert + "\"]}";
        final String claimsJsonString = "{\"aud\":\"" + aud + "\",\"iat\":" + mockCurrentTime + ",\"nonce\":\"" + nonce + "\"}";
        return headerJsonString + "." + claimsJsonString + "." + signature;
    }

    private static IDeviceCertificate getMockCertificate(@NonNull final String encodedCertValue,
                                                         @NonNull final String signature) throws Exception{
        final X509Certificate x509 = mock(X509Certificate.class);
        when(
                x509.getEncoded()
        ).thenReturn(
                Base64.decode(encodedCertValue, EnumSet.of(Base64Flags.NO_WRAP))
        );

        return getMockCertificate(x509, signature);
    }

    private static IDeviceCertificate getMockCertificate(@NonNull final X509Certificate mockX509,
                                                         @NonNull final String signature) {
        return new IDeviceCertificate() {
            @Override
            public boolean isValidIssuer(List<String> certAuthorities) {
                return true;
            }

            @Override
            public @NonNull X509Certificate getX509() {
                return mockX509;
            }

            @Override
            public byte[] sign(@NonNull final String algorithm, final byte[] dataToBeSigned) throws ClientException {
                // Do nothing.
                return StringUtil.toByteArray(signature);
            }
        };
    }
}
