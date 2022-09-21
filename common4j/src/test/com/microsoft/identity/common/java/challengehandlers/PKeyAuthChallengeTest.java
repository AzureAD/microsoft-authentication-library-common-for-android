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
package com.microsoft.identity.common.java.challengehandlers;

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_HEADER;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.CHALLENGE_RESPONSE_TYPE;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.PKEYAUTH_VERSION;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_CONTEXT;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_NONCE;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_CERT_AUTHORITIES;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_MOCK_VERSION;

import com.microsoft.identity.common.java.AuthenticationSettings;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.Arrays;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;

@RunWith(JUnit4.class)
public class PKeyAuthChallengeTest {

    @Before
    public void setUp() {
        AuthenticationSettings.INSTANCE.setCertificateLoader(null);
    }

    @After
    public void tearDown() {
        AuthenticationSettings.INSTANCE.setCertificateLoader(null);
    }

    private PKeyAuthChallenge.PKeyAuthChallengeBuilder getBasicChallengeBuilder(){
        return new PKeyAuthChallenge.PKeyAuthChallengeBuilder()
                    .nonce(PKEYAUTH_AUTH_ENDPOINT_NONCE)
                    .context(PKEYAUTH_AUTH_ENDPOINT_CONTEXT)
                    .version(PKEYAUTH_MOCK_VERSION)
                    .submitUrl(PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL);
    }

    /**
     * Verify no error thrown out if certificate loader is not set.
     */
    @Test
    public void testGetChallengeResponseNoDeviceCertLoader() throws ClientException {
        final Map<String, String> header = getBasicChallengeBuilder()
                .certAuthorities(Arrays.asList(PKEYAUTH_CERT_AUTHORITIES))
                .build()
                .getChallengeHeader();

        Assert.assertNotNull(header);
        Assert.assertEquals(1, header.size());
        Assert.assertEquals(
                String.format("%s Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE,
                        PKEYAUTH_AUTH_ENDPOINT_CONTEXT,
                        PKEYAUTH_VERSION
                ),
                header.get(CHALLENGE_RESPONSE_HEADER)
        );
    }

    /**
     * Verify no error thrown out if device is not workplace joined.
     */
    @Test
    public void testGetChallengeResponseDeviceNotWorkplaceJoined() throws ClientException {
        AuthenticationSettings.INSTANCE.setCertificateLoader(new IDeviceCertificateLoader() {
            @Nullable
            @Override
            public IDeviceCertificate loadCertificate(@Nullable String tenantId) {
                return null;
            }
        });

        final Map<String, String> header = getBasicChallengeBuilder()
                .certAuthorities(Arrays.asList(PKEYAUTH_CERT_AUTHORITIES))
                .build()
                .getChallengeHeader();

        Assert.assertNotNull(header);
        Assert.assertEquals(1, header.size());
        Assert.assertEquals(
                String.format("%s Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE,
                        PKEYAUTH_AUTH_ENDPOINT_CONTEXT,
                        PKEYAUTH_VERSION
                ),
                header.get(CHALLENGE_RESPONSE_HEADER)
        );
    }

    /**
     * Verify we get the right response when there is an accessible WPJ entry.
     */
    @Test
    public void testGetChallengeResponseFromValidChallengeRequest() throws ClientException {

        final MockCertLoader mockCertLoader = new MockCertLoader();
        AuthenticationSettings.INSTANCE.setCertificateLoader(mockCertLoader);

        final PKeyAuthChallenge challenge = getBasicChallengeBuilder()
                .certAuthorities(Arrays.asList(PKEYAUTH_CERT_AUTHORITIES))
                .jwsBuilder(mockCertLoader.getMockJwsBuilder(
                        PKEYAUTH_AUTH_ENDPOINT_NONCE,
                        PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL
                ))
                .build();

        final Map<String, String> header = challenge.getChallengeHeader();
        Assert.assertNotNull(header);
        Assert.assertEquals(1, header.size());
        Assert.assertEquals(
                String.format("%s AuthToken=\"%s\",Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE,
                        mockCertLoader.getMockSignedJwt(
                                PKEYAUTH_AUTH_ENDPOINT_NONCE,
                                PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL),
                        PKEYAUTH_AUTH_ENDPOINT_CONTEXT,
                        PKEYAUTH_VERSION
                ),
                header.get(CHALLENGE_RESPONSE_HEADER)
        );
    }

    /**
     * Verify correct response is returned when challenge header doesn't contain both thumbprint and cert
     * authorities, if device is already workplace joined.
     */
    @Test
    public void testGetChallengeResponseFromHeaderBothThumbprintCertAuthorityNotPresent() throws ClientException {

        final Map<String, String> header = getBasicChallengeBuilder()
                .certAuthorities(Arrays.asList(PKEYAUTH_CERT_AUTHORITIES))
                .build()
                .getChallengeHeader();

        Assert.assertNotNull(header);
        Assert.assertEquals(1, header.size());
        Assert.assertEquals(
                String.format("%s Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE,
                        PKEYAUTH_AUTH_ENDPOINT_CONTEXT,
                        PKEYAUTH_VERSION
                ),
                header.get(CHALLENGE_RESPONSE_HEADER)
        );
    }

    /**
     * Verify correct response is returned when the issuer isn't valid.
     */
    @Test
    public void testGetChallengeResponseInvalidIssuer() throws ClientException {
        final MockCertLoader certLoader = new MockCertLoader();
        certLoader.setValidIssuer(false);
        AuthenticationSettings.INSTANCE.setCertificateLoader(certLoader);

        final Map<String, String> header = getBasicChallengeBuilder()
                .certAuthorities(Arrays.asList(PKEYAUTH_CERT_AUTHORITIES))
                .build()
                .getChallengeHeader();

        Assert.assertNotNull(header);
        Assert.assertEquals(1, header.size());
        Assert.assertEquals(
                String.format("%s Context=\"%s\",Version=\"%s\"",
                        CHALLENGE_RESPONSE_TYPE,
                        PKEYAUTH_AUTH_ENDPOINT_CONTEXT,
                        PKEYAUTH_VERSION
                ),
                header.get(CHALLENGE_RESPONSE_HEADER)
        );
    }

    /**
     * Verify correct exception is thrown when certificate is null.
     */
    @Test(expected = NullPointerException.class)
    public void testGetChallengeResponseNullCertificate() throws Exception {
        final MockCertLoader certLoader = new MockCertLoader();
        certLoader.setX509(null);
        AuthenticationSettings.INSTANCE.setCertificateLoader(certLoader);

        getBasicChallengeBuilder()
                .certAuthorities(Arrays.asList(PKEYAUTH_CERT_AUTHORITIES))
                .build()
                .getChallengeHeader();
    }


    /**
     * Verify correct exception is thrown when certificate is null.
     */
    @Test(expected = NullPointerException.class)
    public void testGetChallengeResponseNullX509Certificate() throws Exception {
        final MockCertLoader certLoader = new MockCertLoader();
        certLoader.setX509(null);
        AuthenticationSettings.INSTANCE.setCertificateLoader(certLoader);

        getBasicChallengeBuilder()
                .certAuthorities(Arrays.asList(PKEYAUTH_CERT_AUTHORITIES))
                .build()
                .getChallengeHeader();
    }

    /**
     * Verify correct exception is thrown when cert's private key is null.
     */
    @Test(expected = NullPointerException.class)
    public void testGetChallengeResponseNullPrivateKey() throws Exception {
        final MockCertLoader certLoader = new MockCertLoader();
        certLoader.setPrivateKey(null);
        AuthenticationSettings.INSTANCE.setCertificateLoader(certLoader);

        getBasicChallengeBuilder()
                .certAuthorities(Arrays.asList(PKEYAUTH_CERT_AUTHORITIES))
                .build()
                .getChallengeHeader();
    }
}
