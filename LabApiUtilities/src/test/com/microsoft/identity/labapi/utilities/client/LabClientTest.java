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
import com.microsoft.identity.labapi.utilities.constants.TempUserType;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

public class LabClientTest {

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

}
