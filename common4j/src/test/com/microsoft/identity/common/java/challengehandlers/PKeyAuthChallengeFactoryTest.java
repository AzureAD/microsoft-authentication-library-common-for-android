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

import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_URL;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_MOCK_VERSION;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_CERT_AUTHORITIES;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_CONTEXT;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_NONCE;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_TOKEN_ENDPOINT_CHALLENGE_HEADER;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_TOKEN_ENDPOINT_CONTEXT;
import static com.microsoft.identity.common.java.challengehandlers.MockData.PKEYAUTH_TOKEN_ENDPOINT_NONCE;
import static com.microsoft.identity.common.java.exception.ErrorStrings.DEVICE_CERTIFICATE_REQUEST_INVALID;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.UnsupportedEncodingException;

@RunWith(JUnit4.class)
public class PKeyAuthChallengeFactoryTest {

    @Test
    public void testParsingChallengeUrl() throws ClientException {
        final PKeyAuthChallenge challenge = new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromWebViewRedirect(PKEYAUTH_AUTH_ENDPOINT_URL);
        Assert.assertArrayEquals(PKEYAUTH_CERT_AUTHORITIES, challenge.getCertAuthorities().toArray());
        Assert.assertEquals(PKEYAUTH_MOCK_VERSION, challenge.getVersion());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_CONTEXT, challenge.getContext());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_NONCE, challenge.getNonce());
        Assert.assertEquals(PKEYAUTH_AUTH_ENDPOINT_SUBMIT_URL, challenge.getSubmitUrl());
        Assert.assertNull(challenge.getThumbprint());
    }

    @Test
    public void testParsingChallengeUrl_Malformed() {
        try{
            new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromWebViewRedirect(
                    "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4"
            );
            Assert.fail("Exception is expected");
        } catch (final ClientException e) {
            Assert.assertEquals(DEVICE_CERTIFICATE_REQUEST_INVALID, e.getErrorCode());
        }
    }

    @Test
    public void testParsingChallengeHeader() throws UnsupportedEncodingException, ClientException {
        final PKeyAuthChallenge challenge = new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromTokenEndpointResponse(
                PKEYAUTH_TOKEN_ENDPOINT_CHALLENGE_HEADER,
                PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY);
        Assert.assertArrayEquals(PKEYAUTH_CERT_AUTHORITIES, challenge.getCertAuthorities().toArray());
        Assert.assertEquals(PKEYAUTH_MOCK_VERSION, challenge.getVersion());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_CONTEXT, challenge.getContext());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_NONCE, challenge.getNonce());
        Assert.assertEquals(PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY, challenge.getSubmitUrl());
        Assert.assertNull(challenge.getThumbprint());
    }

    @Test
    public void testParsingChallengeHeader_Malformed() throws UnsupportedEncodingException {
        try{
            new PKeyAuthChallengeFactory().getPKeyAuthChallengeFromTokenEndpointResponse(
                    "urn:http-auth:PKeyAuth?CertAuthorities=OU%3d82dbaca4",
                    PKEYAUTH_TOKEN_ENDPOINT_AUTHORITY
            );
            Assert.fail("Exception is expected");
        } catch (final ClientException e) {
            Assert.assertEquals(DEVICE_CERTIFICATE_REQUEST_INVALID, e.getErrorCode());
        }
    }
}
