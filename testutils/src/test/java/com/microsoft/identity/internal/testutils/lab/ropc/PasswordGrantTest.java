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
package com.microsoft.identity.internal.testutils.lab.ropc;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.internal.providers.oauth2.TokenResult;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.internal.testutils.MicrosoftStsRopcTokenRequest;
import com.microsoft.identity.internal.testutils.authorities.AADTestAuthority;
import com.microsoft.identity.internal.testutils.labutils.Credential;
import com.microsoft.identity.internal.testutils.labutils.LabConstants;
import com.microsoft.identity.internal.testutils.labutils.LabUserHelper;
import com.microsoft.identity.internal.testutils.labutils.LabUserQuery;
import com.microsoft.identity.internal.testutils.strategies.ResourceOwnerPasswordCredentialsTestStrategy;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * This class contains all tests for the password grant.
 * All different success and failure test cases for the password grant can be added here
 */
@RunWith(RobolectricTestRunner.class)
public final class PasswordGrantTest {

    private static final String[] SCOPES = {"user.read", "openid", "offline_access", "profile"};
    private static final String CLIENT_ID = "4b0db8c2-9f26-4417-8bde-3f0e3656f8e0";

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

    private MicrosoftStsRopcTokenRequest createRopcTokenRequest(String[] scopes, String username, String password) {
        String scope = convertScopesArrayToString(scopes);

        final MicrosoftStsRopcTokenRequest tokenRequest = new MicrosoftStsRopcTokenRequest();
        tokenRequest.setClientId(CLIENT_ID);
        tokenRequest.setScope(scope);
        tokenRequest.setUsername(username);
        tokenRequest.setPassword(password);
        tokenRequest.setGrantType(TokenRequest.GrantTypes.PASSWORD);

        return tokenRequest;
    }

    private Credential getCredentialsForManagedUser() {
        LabUserQuery query = new LabUserQuery();
        query.userType = LabConstants.UserType.CLOUD;
        return LabUserHelper.getCredentials(query);
    }

    private TokenResult performRopcTokenRequest(String[] scopes, String username, String password) throws IOException, ClientException {
        final AADTestAuthority aadTestAuthority = new AADTestAuthority();
        final ResourceOwnerPasswordCredentialsTestStrategy testStrategy =
                (ResourceOwnerPasswordCredentialsTestStrategy) aadTestAuthority.createOAuth2Strategy();

        final MicrosoftStsRopcTokenRequest tokenRequest = createRopcTokenRequest(scopes, username, password);

        return testStrategy.requestToken(tokenRequest);
    }

    @Test
    // test that we can successfully perform ROPC if we supply required data to server
    public void testRopcSuccessManagedUser() throws IOException {
        final Credential credential = getCredentialsForManagedUser();

        try {
            final TokenResult tokenResult = performRopcTokenRequest(SCOPES, credential.userName, credential.password);

            assertTrue(tokenResult.getSuccess());
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    // test that ROPC flow fails if username is not provided
    public void testRopcFailureManagedUserNoUsername() throws IOException {
        final Credential credential = getCredentialsForManagedUser();

        try {
            performRopcTokenRequest(SCOPES, null, credential.password);

            fail("Unexpected Success");
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    // test that ROPC flow fails if password is not provided
    public void testRopcFailureManagedUserNoPassword() throws IOException {
        final Credential credential = getCredentialsForManagedUser();

        try {
            performRopcTokenRequest(SCOPES, credential.userName, null);

            fail("Unexpected Success");
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

    @Test(expected = IllegalArgumentException.class)
    // test that ROPC flow fails if scope is not provided
    public void testRopcFailureManagedUserNoScope() throws IOException {
        final Credential credential = getCredentialsForManagedUser();

        try {
            performRopcTokenRequest(null, credential.userName, credential.password);

            fail("Unexpected Success");
        } catch (ClientException exception) {
            fail("Unexpected exception.");
        }
    }

}
