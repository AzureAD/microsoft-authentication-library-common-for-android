package com.microsoft.identity.common.internal.authorities;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.io.IOException;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
public class AzureActiveDirectoryAuthorityTests {
    @Test
    public void isSameCloudAsAuthority_Returns_True_For_Authority_With_ValidAliases_For_SameCloud() throws IOException {
        String[] cloudAliasesWW = new String[] {"https://login.microsoftonline.com", "https://login.windows.net", "https://login.microsoft.com", "https://sts.windows.net"};
        String[] cloudAliasesCN = new String[] {"https://login.chinacloudapi.cn", "https://login.partner.microsoftonline.cn"};
        String[] cloudAliasesUSGov = new String[] {"https://login.microsoftonline.us", "https://login.usgovcloudapi.net"};

        AzureActiveDirectoryAuthority authorityWW = new AzureActiveDirectoryAuthority(new AllAccounts(cloudAliasesWW[0]));
        for (String cloudUrl: cloudAliasesWW) {
            AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(new AnyOrganizationalAccount(cloudUrl));
            assertTrue(authorityWW.isSameCloudAsAuthority(authority));
        }

        AzureActiveDirectoryAuthority authorityCN = new AzureActiveDirectoryAuthority(new AllAccounts(cloudAliasesCN[0]));
        for (String cloudUrl: cloudAliasesCN) {
            AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(new AnyOrganizationalAccount(cloudUrl));
            assertTrue(authorityCN.isSameCloudAsAuthority(authority));
        }

        AzureActiveDirectoryAuthority authorityUSGov = new AzureActiveDirectoryAuthority(new AllAccounts(cloudAliasesUSGov[0]));
        for (String cloudUrl: cloudAliasesUSGov) {
            AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(new AnyOrganizationalAccount(cloudUrl));
            assertTrue(authorityUSGov.isSameCloudAsAuthority(authority));
        }
    }

    @Test
    public void isSameCloudAsAuthority_Returns_False_For_Authorities_From_Different_Clouds() throws IOException {
        AzureActiveDirectoryAuthority authorityWW = new AzureActiveDirectoryAuthority(new AllAccounts("https://login.microsoftonline.com"));
        AzureActiveDirectoryAuthority authorityCN = new AzureActiveDirectoryAuthority(new AllAccounts("https://login.partner.microsoftonline.cn"));
        AzureActiveDirectoryAuthority authorityUSGov = new AzureActiveDirectoryAuthority(new AllAccounts("https://login.microsoftonline.us"));
        assertFalse(authorityWW.isSameCloudAsAuthority(authorityCN));
        assertFalse(authorityCN.isSameCloudAsAuthority(authorityUSGov));
        assertFalse(authorityUSGov.isSameCloudAsAuthority(authorityWW));
    }
}
