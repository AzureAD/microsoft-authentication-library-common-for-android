package com.microsoft.identity.labapi.utilities.client;

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
        final LabQuery queryForUserB = LabQuery.builder()
                .userType(UserType.GUEST)
                .guestHomedIn(GuestHomedIn.ON_PREM)
                .azureEnvironment(AzureEnvironment.AZURE_CLOUD)
                .federationProvider(FederationProvider.ADFS_V4)
                .build();

        final LabGuestAccount userB = LabGuestAccountHelper.loadGuestAccountFromLab(queryForUserB);

        Assert.assertNotNull(userB.getHomeUpn());
        Assert.assertNotNull(LabGuestAccountHelper.getPasswordForGuestUser(userB));
    }
}
