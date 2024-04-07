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
package com.microsoft.identity.labapi.utilities.authentication;

import com.microsoft.identity.labapi.utilities.jwt.IJWTParser;
import com.microsoft.identity.labapi.utilities.jwt.JWTParserFactory;
import com.microsoft.identity.labapi.utilities.TestBuildConfig;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

import java.util.List;

/**
 * A test to validate that we can obtain tokens for Lab API.
 */
public class LabApiAuthenticationClientTest {

    final IJWTParser jwtParser = JWTParserFactory.INSTANCE.getJwtParser();

    @Test
    public void canGetTokenForLabApiUsingClientSecret() {
        final LabApiAuthenticationClient labApiAuthenticationClient =
                new LabApiAuthenticationClient(TestBuildConfig.LAB_CLIENT_SECRET);

        try {
            final String accessToken = labApiAuthenticationClient.getAccessToken();
            Assert.assertNotNull(accessToken);
            Assert.assertNotEquals("", accessToken);
            Assert.assertEquals(
                    LabAuthenticationConstants.LAB_API_TOKEN_AUDIENCE,
                    ((List<String>) jwtParser.parseJWT(accessToken).get(LabAuthenticationConstants.AUDIENCE_CLAIM)).get(0)
            );
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Ignore
    @Test
    public void canGetTokenForLabApiUsingCertificate() {
        final LabApiAuthenticationClient labApiAuthenticationClient =
                new LabApiAuthenticationClient();

        try {
            final String accessToken = labApiAuthenticationClient.getAccessToken();
            Assert.assertNotNull(accessToken);
            Assert.assertNotEquals("", accessToken);
            Assert.assertEquals(
                    LabAuthenticationConstants.LAB_API_TOKEN_AUDIENCE,
                    ((List<String>) jwtParser.parseJWT(accessToken).get(LabAuthenticationConstants.AUDIENCE_CLAIM)).get(0)
            );
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

}
