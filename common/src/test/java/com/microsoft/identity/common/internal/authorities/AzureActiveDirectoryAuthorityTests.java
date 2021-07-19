//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
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
        final String[] cloudAliasesWW = new String[]{"https://login.microsoftonline.com", "https://login.windows.net", "https://login.microsoft.com", "https://sts.windows.net"};
        final String[] cloudAliasesCN = new String[]{"https://login.chinacloudapi.cn", "https://login.partner.microsoftonline.cn"};
        final String[] cloudAliasesUSGov = new String[]{"https://login.microsoftonline.us", "https://login.usgovcloudapi.net"};

        final  AzureActiveDirectoryAuthority authorityWW = new AzureActiveDirectoryAuthority(new AllAccounts(cloudAliasesWW[0]));
        for (final  String cloudUrl : cloudAliasesWW) {
            final AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(new AnyOrganizationalAccount(cloudUrl));
            assertTrue(authorityWW.isSameCloudAsAuthority(authority));
        }

        final  AzureActiveDirectoryAuthority authorityCN = new AzureActiveDirectoryAuthority(new AllAccounts(cloudAliasesCN[0]));
        for (final String cloudUrl : cloudAliasesCN) {
            final AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(new AnyOrganizationalAccount(cloudUrl));
            assertTrue(authorityCN.isSameCloudAsAuthority(authority));
        }

        final AzureActiveDirectoryAuthority authorityUSGov = new AzureActiveDirectoryAuthority(new AllAccounts(cloudAliasesUSGov[0]));
        for (final String cloudUrl : cloudAliasesUSGov) {
            final AzureActiveDirectoryAuthority authority = new AzureActiveDirectoryAuthority(new AnyOrganizationalAccount(cloudUrl));
            assertTrue(authorityUSGov.isSameCloudAsAuthority(authority));
        }
    }

    @Test
    public void isSameCloudAsAuthority_Returns_False_For_Authorities_From_Different_Clouds() throws IOException {
        final AzureActiveDirectoryAuthority authorityWW = new AzureActiveDirectoryAuthority(new AllAccounts("https://login.microsoftonline.com"));
        final AzureActiveDirectoryAuthority authorityCN = new AzureActiveDirectoryAuthority(new AllAccounts("https://login.partner.microsoftonline.cn"));
        final AzureActiveDirectoryAuthority authorityUSGov = new AzureActiveDirectoryAuthority(new AllAccounts("https://login.microsoftonline.us"));
        assertFalse(authorityWW.isSameCloudAsAuthority(authorityCN));
        assertFalse(authorityCN.isSameCloudAsAuthority(authorityUSGov));
        assertFalse(authorityUSGov.isSameCloudAsAuthority(authorityWW));
    }
}
