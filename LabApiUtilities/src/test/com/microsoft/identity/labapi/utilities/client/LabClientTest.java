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
package com.microsoft.identity.labapi.utilities.client;

import com.microsoft.identity.labapi.utilities.TestBuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.constants.ProtectionPolicy;
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;
import com.microsoft.identity.labapi.utilities.rules.RetryTestRule;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;

/**
 * Test Various function calls through the lab api including
 * Temp-User Creation, fetching existing cloud, guest, and federated accounts, as well as
 * password reset, policy enable/disable for temporary users.
 */
public class LabClientTest {

    // Give some time for basic user to finish creation to enable rest of test.
    private final long POST_TEMP_USER_CREATION_WAIT = 15000;

    @Rule
    public RetryTestRule retryRule = new RetryTestRule(3);

    @Test
    public void canFetchCloudAccount() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        final LabQuery query = LabQuery.builder()
                .userType(UserType.CLOUD)
                .build();

        try {
            final ILabAccount labAccount = labClient.getLabAccount(query);
            Assert.assertNotNull(labAccount);
            Assert.assertNotNull(labAccount.getUsername());
            Assert.assertNotNull(labAccount.getPassword());
            Assert.assertNotNull(labAccount.getUserType());
            Assert.assertTrue(labAccount.getUsername().toLowerCase().contains("msidlab4"));
            Assert.assertEquals(UserType.CLOUD, labAccount.getUserType());
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchMSAAccount() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        final LabQuery query = LabQuery.builder()
                .userType(UserType.MSA)
                .build();

        try {
            final ILabAccount labAccount = labClient.getLabAccount(query);
            Assert.assertNotNull(labAccount);
            Assert.assertNotNull(labAccount.getUsername());
            Assert.assertNotNull(labAccount.getPassword());
            Assert.assertNotNull(labAccount.getUserType());
            Assert.assertTrue(labAccount.getUsername().toLowerCase().contains("outlook"));
            Assert.assertEquals(UserType.MSA, labAccount.getUserType());
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchGuestAccount() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        final LabQuery query = LabQuery.builder()
                .userType(UserType.GUEST)
                .build();

        try {
            final ILabAccount labAccount = labClient.getLabAccount(query);
            Assert.assertNotNull(labAccount);
            Assert.assertNotNull(labAccount.getUsername());
            Assert.assertNotNull(labAccount.getPassword());
            Assert.assertNotNull(labAccount.getUserType());
            Assert.assertTrue(labAccount.getUsername().toLowerCase().contains("msidlab4"));
            Assert.assertEquals(UserType.GUEST, labAccount.getUserType());
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchFederatedAccount() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        final LabQuery query = LabQuery.builder()
                .userType(UserType.FEDERATED)
                .build();

        try {
            final ILabAccount labAccount = labClient.getLabAccount(query);
            Assert.assertNotNull(labAccount);
            Assert.assertNotNull(labAccount.getUsername());
            Assert.assertNotNull(labAccount.getPassword());
            Assert.assertNotNull(labAccount.getUserType());
            Assert.assertTrue(labAccount.getUsername().toLowerCase().contains("msidlab4"));
            Assert.assertEquals(UserType.FEDERATED, labAccount.getUserType());
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canCreateBasicTempUser() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        try {
            final ILabAccount labAccount = labClient.createTempAccount(TempUserType.BASIC);
            Assert.assertNotNull(labAccount);
            Assert.assertNotNull(labAccount.getUsername());
            Assert.assertNotNull(labAccount.getPassword());
            Assert.assertNotNull(labAccount.getUserType());
            Assert.assertTrue(labAccount.getUsername().toLowerCase().contains("msidlab4"));
            Assert.assertEquals(UserType.CLOUD, labAccount.getUserType());
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canCreateMAMCATempUser() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        try {
            final ILabAccount labAccount = labClient.createTempAccount(TempUserType.MAM_CA);
            Assert.assertNotNull(labAccount);
            Assert.assertNotNull(labAccount.getUsername());
            Assert.assertNotNull(labAccount.getPassword());
            Assert.assertNotNull(labAccount.getUserType());
            Assert.assertTrue(labAccount.getUsername().toLowerCase().contains("msidlab4"));
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canResetPassword() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        try {
            final ILabAccount labAccount = labClient.createTempAccount(TempUserType.BASIC);
            Thread.sleep(POST_TEMP_USER_CREATION_WAIT);
            Assert.assertTrue(labClient.resetPassword(labAccount.getUsername(), 2));
        } catch (final LabApiException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canEnablePolicy() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        try {
            final ILabAccount labAccount = labClient.createTempAccount(TempUserType.BASIC);
            Thread.sleep(POST_TEMP_USER_CREATION_WAIT);
            Assert.assertTrue(labClient.enablePolicy(labAccount.getUsername(), ProtectionPolicy.MAM_CA));
        } catch (final LabApiException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canDisablePolicy() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        final LabClient labClient = new LabClient(authenticationClient);

        try {
            final ILabAccount labAccount = labClient.createTempAccount(TempUserType.MAM_CA);
            Thread.sleep(POST_TEMP_USER_CREATION_WAIT);
            Assert.assertTrue(labClient.disablePolicy(labAccount.getUsername(), ProtectionPolicy.MAM_CA));
        } catch (final LabApiException | InterruptedException e){
            throw new AssertionError(e);
        }
    }

}
