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
package com.microsoft.identity.common.ropc;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.internal.testutils.MicrosoftStsRopcTokenRequest;
import com.microsoft.identity.internal.testutils.authorities.AADTestAuthority;
import com.microsoft.identity.common.internal.providers.oauth2.OAuth2Strategy;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.internal.testutils.labutils.Credential;
import com.microsoft.identity.internal.testutils.labutils.Scenario;
import com.microsoft.identity.internal.testutils.labutils.TestConfigurationQuery;

import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * This class contains all tests for the password grant.
 * All different success and failure test cases for the password grant can be added here
 */
@RunWith(RobolectricTestRunner.class)
public final class PasswordGrantTest {

    private static final String[] SCOPES = {"user.read", "openid", "offline_access", "profile"};
    private static final String CLIENT_ID = "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0";

    private Scenario getManagedUserTestScenario() {
        final TestConfigurationQuery query = new TestConfigurationQuery();
        query.userType = "Member";
        query.isFederated = false;
        query.federationProvider = "ADFSv4";

        Scenario scenario = Scenario.GetScenario(query);
        return scenario;
    }

    private boolean isEmpty(String[] arr) {
        return arr == null || arr.length == 0;
    }

    private String convertScopesArrayToString(final String[] scopes) {
        if (isEmpty(scopes)) {
            return null;
        }
        final Set<String> scopesInSet = new HashSet<>(Arrays.asList(scopes));
        return StringUtil.convertSetToString(scopesInSet, " ");
    }

    private TokenRequest createTokenRequest(String[] scopes, String username, String password) {
        String scope = convertScopesArrayToString(scopes);

        final MicrosoftStsRopcTokenRequest tokenRequest = new MicrosoftStsRopcTokenRequest();
        tokenRequest.setClientId(CLIENT_ID);
        tokenRequest.setScope(scope);
        tokenRequest.setUsername(username);
        tokenRequest.setPassword(password);
        tokenRequest.setGrantType(TokenRequest.GrantTypes.PASSWORD);

        return tokenRequest;
    }

    private Credential getUserCredentialsForManagedUser() {
        final Scenario scenario = getManagedUserTestScenario();
        final Credential credential = scenario.getCredential();
        return credential;
    }

    private TokenResult performRopcTokenRequest(String[] scopes, String username, String password) throws IOException, ClientException {
        final AADTestAuthority aadTestAuthority = new AADTestAuthority();
        final OAuth2Strategy testStrategy = aadTestAuthority.createOAuth2Strategy();

        final TokenRequest tokenRequest = createTokenRequest(SCOPES, username, password);
        final TokenResult tokenResult = testStrategy.requestToken(tokenRequest);
        return tokenResult;
    }

    @Test
    // test that we can successfully perform ROPC if we supply required data to server
    public void testRopcSuccessManagedUser() throws IOException {
        final Credential credential = getUserCredentialsForManagedUser();

        try {
            final TokenResult tokenResult = performRopcTokenRequest(SCOPES, credential.userName, credential.password);

            assertEquals(true, tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    // test that ROPC flow fails if username is not provided
    public void testRopcFailureManagedUserNoUsername() throws IOException {
        final Credential credential = getUserCredentialsForManagedUser();

        try {
            final TokenResult tokenResult = performRopcTokenRequest(SCOPES, null, credential.password);

            assertEquals(true, tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    // test that ROPC flow fails if password is not provided
    public void testRopcFailureManagedUserNoPassword() throws IOException {
        final Credential credential = getUserCredentialsForManagedUser();

        try {
            final TokenResult tokenResult = performRopcTokenRequest(SCOPES, credential.userName, null);

            assertEquals(true, tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    @Ignore // this has started to fail for some reason, will need investigation
    public void testRopcFailureManagedUserNoScope() throws IOException {
        final Credential credential = getUserCredentialsForManagedUser();

        try {
            final TokenResult tokenResult = performRopcTokenRequest(null, credential.userName, credential.password);

            assertEquals(true, tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

}
