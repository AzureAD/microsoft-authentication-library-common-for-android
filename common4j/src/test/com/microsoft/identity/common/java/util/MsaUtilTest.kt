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
package com.microsoft.identity.common.java.util

import com.microsoft.identity.common.java.authorities.AccountsInOneOrganization
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAuthority
import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryB2CAuthority
import org.junit.Assert
import org.junit.Test

class MsaUtilTest {

    @Test
    fun isMsaRequestWithMsaMegaTenantAuthority() {
        val msaMegaTenantAuthority = AzureActiveDirectoryAuthority(AccountsInOneOrganization(AzureActiveDirectoryAudience.MSA_MEGA_TENANT_ID))

        Assert.assertTrue(MsaUtil.isMsaRequest(msaMegaTenantAuthority))
    }

    @Test
    fun isMsaRequestWithConsumersAuthority() {
        val consumersTenantAuthority = AzureActiveDirectoryAuthority(AccountsInOneOrganization(AzureActiveDirectoryAudience.CONSUMERS))

        Assert.assertTrue(MsaUtil.isMsaRequest(consumersTenantAuthority))
    }

    @Test
    fun isMsaRequestWithNonMsaAuthority() {
        val nonMsaAuthority = AzureActiveDirectoryAuthority(AccountsInOneOrganization(AzureActiveDirectoryAudience.ALL))

        Assert.assertFalse(MsaUtil.isMsaRequest(nonMsaAuthority))
    }

    @Test
    fun isMsaRequestWithNonAzureAuthority() {
        val nonAzureActiveDirectoryAuthority = AzureActiveDirectoryB2CAuthority("mockUrl")

        Assert.assertFalse(MsaUtil.isMsaRequest(nonAzureActiveDirectoryAuthority))
    }
}
