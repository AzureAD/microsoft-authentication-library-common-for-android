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

import com.microsoft.identity.labapi.utilities.BuildConfig;
import com.microsoft.identity.labapi.utilities.authentication.LabApiAuthenticationClient;
import com.microsoft.identity.labapi.utilities.constants.AzureEnvironment;
import com.microsoft.identity.labapi.utilities.constants.FederationProvider;
import com.microsoft.identity.labapi.utilities.constants.GuestHomedIn;
import com.microsoft.identity.labapi.utilities.constants.UserType;
import com.microsoft.identity.labapi.utilities.exception.LabApiException;

import org.junit.Assert;
import org.junit.Test;

public class LabGuestAccountTest {

    @Test
    public void testCanCreateLabGuestAccount() throws LabApiException {
        final LabApiAuthenticationClient authenticationClient = new LabApiAuthenticationClient(
                BuildConfig.LAB_CLIENT_SECRET
        );
        final LabClient labClient = new LabClient(authenticationClient);

        final LabQuery queryForUser = LabQuery.builder()
                .userType(UserType.GUEST)
                .guestHomedIn(GuestHomedIn.ON_PREM)
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .federationProvider(FederationProvider.ADFS_V4)
                .build();

        final LabGuestAccount user = labClient.loadGuestAccountFromLab(queryForUser);

        Assert.assertNotNull(user.getHomeUpn());
        Assert.assertNotNull(labClient.getPasswordForGuestUser(user));
    }
}
