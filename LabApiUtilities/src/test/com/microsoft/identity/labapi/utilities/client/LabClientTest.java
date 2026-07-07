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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Map;

/**
 * Test Various function calls through the lab api including
 * Temp-User Creation, fetching existing cloud, guest, and federated accounts, as well as
 * password reset, policy enable/disable for temporary users.
 */
public class LabClientTest {

    // Give some time for basic user to finish creation to enable rest of test.
    private final long POST_TEMP_USER_CREATION_WAIT = 15000;
    private LabClient mLabClient;

    @Rule
    public RetryTestRule retryRule = new RetryTestRule(3);

    private final String DEFAULT_LAB_NAME = "id4slab2";
    private final String GUEST_LAB_NAME = "id4slab1";

    @Before
    public void setup() {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                TestBuildConfig.LAB_CLIENT_SECRET
        );

        mLabClient = new LabClient(authenticationClient);
    }

    @After
    public void cleanup() {
        mLabClient = null;
    }

    @Test
    public void canFetchBasicAccount() {
        try {
            final ILabAccount labAccount = mLabClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.BASIC);
            assertLabAccount(labAccount, UserType.BASIC, DEFAULT_LAB_NAME);
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchMSAAccount() {
        try {
            final ILabAccount labAccount = mLabClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.MSA);
            assertLabAccount(labAccount, UserType.MSA, "outlook");
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchGuestAccount() {
        try {
            final ILabAccount labAccount = mLabClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.GUEST);
            assertLabAccount(labAccount, UserType.GUEST, GUEST_LAB_NAME);
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchFederatedAccount() {
        try {
            final ILabAccount labAccount = mLabClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.FEDERATED);
            assertLabAccount(labAccount, UserType.FEDERATED, DEFAULT_LAB_NAME);
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchUsGovAccount() {
        try {
            final ILabAccount labAccount = mLabClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.USGOV);
            assertLabAccount(labAccount, UserType.USGOV, "arlmsidlab1");
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchChinaAccount() {
        try {
            final ILabAccount labAccount = mLabClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.CHINA);
            assertLabAccount(labAccount, UserType.CHINA, "mncmsidlab1");
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canCreateBasicTempUser() {
        try {
            final ILabAccount labAccount = mLabClient.createTempAccount(TempUserType.BASIC);
            assertLabAccount(labAccount, UserType.CLOUD, DEFAULT_LAB_NAME);
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canCreateMAMCATempUser() {
        try {
            final ILabAccount labAccount = mLabClient.createTempAccount(TempUserType.MAM_CA);
            assertLabAccount(labAccount, UserType.CLOUD, DEFAULT_LAB_NAME);
        } catch (final LabApiException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canResetPassword() {
        try {
            final ILabAccount labAccount = mLabClient.createTempAccount(TempUserType.BASIC);
            Thread.sleep(POST_TEMP_USER_CREATION_WAIT);
            Assert.assertTrue(mLabClient.resetPassword(labAccount.getUsername(), 2));
        } catch (final LabApiException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canEnablePolicy() {
        try {
            final ILabAccount labAccount = mLabClient.createTempAccount(TempUserType.BASIC);
            Thread.sleep(POST_TEMP_USER_CREATION_WAIT);
            Assert.assertTrue(mLabClient.enablePolicy(labAccount.getUsername(), ProtectionPolicy.MAM_CA));
        } catch (final LabApiException | InterruptedException e) {
            throw new AssertionError(e);
        }
    }

    @Test
    public void canDisablePolicy() {
        try {
            final ILabAccount labAccount = mLabClient.createTempAccount(TempUserType.MAM_CA);
            Thread.sleep(POST_TEMP_USER_CREATION_WAIT);
            Assert.assertTrue(mLabClient.disablePolicy(labAccount.getUsername(), ProtectionPolicy.MAM_CA));
        } catch (final LabApiException | InterruptedException e){
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchPasswordFromKeyVault() {
        try {
            final String password = mLabClient.getPasswordSecretFromLabsKeyVault("ID4SLAB2");
            Assert.assertTrue(password != null && !password.isEmpty());
        } catch (final LabApiException e){
            throw new AssertionError(e);
        }
    }

    @Test
    public void canFetchAccountUpnJsonStringOtherAccounts() {
        try {
            final Map<String, LabJsonStringAccountEntry> accountUpnMap = mLabClient.getAccountMapJsonFromMobileBuildKeyVault();
            Assert.assertTrue(accountUpnMap != null && !accountUpnMap.isEmpty());

            final ILabAccount resourceAccount = mLabClient.getAccountFromLabJsonStringInMobileBuildVault(UserType.RESOURCE_ACCOUNT_1);
            Assert.assertNotNull(resourceAccount);
            Assert.assertNotNull(resourceAccount.getUsername());
            Assert.assertNotNull(resourceAccount.getPassword());
            Assert.assertNotNull(resourceAccount.getHomeObjectId());
            Assert.assertNotNull(resourceAccount.getHomeTenantId());
        } catch (final LabApiException e){
            throw new AssertionError(e);
        }
    }

    // Helper to assert common properties of a lab account
    private void assertLabAccount(final ILabAccount labAccount,
                                  final UserType expectedUserType,
                                  final String expectedUsernameContains) {
        Assert.assertNotNull(labAccount);
        Assert.assertNotNull(labAccount.getUsername());
        Assert.assertNotNull(labAccount.getPassword());
        Assert.assertNotNull(labAccount.getUserType());
        if (expectedUsernameContains != null) {
            Assert.assertTrue(labAccount.getUsername().toLowerCase().contains(expectedUsernameContains));
        }
        if (expectedUserType != null) {
            Assert.assertEquals(expectedUserType, labAccount.getUserType());
        }
        Assert.assertNotNull(labAccount.getHomeObjectId());
        Assert.assertNotNull(labAccount.getHomeTenantId());
    }
}
