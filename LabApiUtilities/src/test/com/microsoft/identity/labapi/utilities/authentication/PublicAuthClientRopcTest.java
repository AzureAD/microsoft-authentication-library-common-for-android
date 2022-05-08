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

import com.microsoft.identity.labapi.utilities.TestBuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.adal4j.Adal4jAuthClient;
import com.microsoft.identity.labapi.utilities.authentication.client.IPublicAuthClient;
import com.microsoft.identity.labapi.utilities.authentication.msal4j.Msal4jAuthClient;
import com.microsoft.identity.labapi.utilities.client.ILabAccount;
import com.microsoft.identity.labapi.utilities.client.LabClient;
import com.microsoft.identity.labapi.utilities.client.LabQuery;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.jwt.IJWTParser;
import com.microsoft.identity.labapi.utilities.jwt.JWTParserFactory;

import org.junit.Assert;
import org.junit.Test;

import java.util.Map;

import lombok.NonNull;

/**
 * A test to validate MSAL 4J Public Auth Client implementation.
 */
public class PublicAuthClientRopcTest {

    final IJWTParser jwtParser = JWTParserFactory.INSTANCE.getJwtParser();
    final IPublicAuthClient msal4jAuthClient = new Msal4jAuthClient();
    final IPublicAuthClient adal4jAuthClient = new Adal4jAuthClient();

    final LabApiAuthenticationClient labApiAuthenticationClient = new LabApiAuthenticationClient(
            TestBuildConfig.LAB_CLIENT_SECRET
    );

    final LabClient labClient = new LabClient(labApiAuthenticationClient);

    final String SCOPE_USER_READ = "User.Read";

    final String RESOURCE_GRAPH = "https://graph.windows.net";

    final String AUTHORITY_URL_ROPC_V1 = "https://login.microsoftonline.com/common";
    final String AUTHORITY_URL_ROPC_V2 = "https://login.microsoftonline.com/organizations";

    @Test
    public void canGetTokenUsingRopcWithMsal4j() throws LabApiException {
        performTestWithAuthClient(msal4jAuthClient);
    }

    @Test
    public void canGetTokenUsingRopcWithAdal4j() throws LabApiException {
        final IAuthenticationResult result = performTestWithAuthClient(adal4jAuthClient);

        // adal should supply refresh token too
        Assert.assertTrue(result instanceof IRefreshTokenSupplier);

        final IRefreshTokenSupplier refreshTokenSupplier = (IRefreshTokenSupplier) result;
        Assert.assertNotNull(refreshTokenSupplier.getRefreshToken());
    }

    private IAuthenticationResult performTestWithAuthClient(@NonNull final IPublicAuthClient publicAuthClient) throws LabApiException {
        final LabQuery query = LabQuery.builder()
                .userType(UserType.CLOUD)
                .build();

        final ILabAccount labAccount = labClient.getLabAccount(query);

        final TokenParameters.TokenParametersBuilder tokenParametersBuilder = TokenParameters.builder()
                .clientId(labAccount.getAssociatedClientId());

        if (publicAuthClient instanceof Msal4jAuthClient) {
            tokenParametersBuilder.authority(AUTHORITY_URL_ROPC_V2);
            tokenParametersBuilder.scope(SCOPE_USER_READ);
        } else {
            tokenParametersBuilder.authority(AUTHORITY_URL_ROPC_V1);
            tokenParametersBuilder.resource(RESOURCE_GRAPH);
        }

        final IAuthenticationResult result = publicAuthClient.acquireToken(
                labAccount.getUsername(),
                labAccount.getPassword(),
                tokenParametersBuilder.build()
        );

        Assert.assertNotNull(result);
        Assert.assertNotNull(result.getAccessToken());
        Assert.assertNotNull(result.getIdToken());
        Assert.assertNotNull(result.getExpiresOnDate());

        final Map<String, ?> claims = jwtParser.parseJWT(result.getAccessToken());

        Assert.assertEquals(
                labAccount.getUsername(),
                claims.get(LabAuthenticationConstants.UPN_CLAIM)
        );

        return result;
    }
}
