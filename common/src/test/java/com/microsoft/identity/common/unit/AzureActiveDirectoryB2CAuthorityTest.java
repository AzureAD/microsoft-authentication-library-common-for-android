package com.microsoft.identity.common.unit;

import com.microsoft.identity.common.internal.authorities.Authority;
import com.microsoft.identity.common.internal.authorities.AzureActiveDirectoryB2CAuthority;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class AzureActiveDirectoryB2CAuthorityTest {

    private static final String POLICY_NAME = "policyName";
    private static final String TENANT_NAME = "tenantName";
    private static final String TENANT_ID = "tenantID";
    private static final String AAD_B2C_HOSTNAME = "azureADB2CHostname";
    private static final String[] B2CAuthorityUrls = new String[]{
            "https://"+AAD_B2C_HOSTNAME+"/tfp/"+TENANT_NAME+"/"+POLICY_NAME,
            "https://"+TENANT_NAME+".b2clogin.com/tfp/"+TENANT_ID+"/"+POLICY_NAME
    };

    @Test
    public void testGetB2CPolicyName() {
        for (final String authorityUrl : B2CAuthorityUrls) {
            final Authority authority = Authority.getAuthorityFromAuthorityUrl(authorityUrl);
            Assert.assertTrue(authority instanceof AzureActiveDirectoryB2CAuthority);
            final String policyName = ((AzureActiveDirectoryB2CAuthority) authority).getB2CPolicyName();
            Assert.assertEquals(policyName, POLICY_NAME);
        }
    }
}
