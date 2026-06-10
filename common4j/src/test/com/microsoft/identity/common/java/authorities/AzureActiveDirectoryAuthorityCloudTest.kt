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
package com.microsoft.identity.common.java.authorities

import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud
import org.junit.Assert.*
import org.junit.Test

/**
 * Tests for [AzureActiveDirectoryAuthority.isSameCloudAsAuthority] with
 * sovereign clouds. Since sovereign clouds are pre-seeded in the cloud metadata
 * cache, these tests do not require network access.
 */
class AzureActiveDirectoryAuthorityCloudTest {

    private fun createAuthority(host: String): AzureActiveDirectoryAuthority =
        Authority.getAuthorityFromAuthorityUrl("https://$host/common") as AzureActiveDirectoryAuthority

    @Test
    fun testIsSameCloudAsAuthority_bothBleu_returnsTrue() {
        val authority1 = createAuthority(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST)
        val authority2 = createAuthority(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST)
        assertTrue(authority1.isSameCloudAsAuthority(authority2))
    }

    @Test
    fun testIsSameCloudAsAuthority_bothDelos_returnsTrue() {
        val authority1 = createAuthority(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST)
        val authority2 = createAuthority(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST)
        assertTrue(authority1.isSameCloudAsAuthority(authority2))
    }

    @Test
    fun testIsSameCloudAsAuthority_bothGovsg_returnsTrue() {
        val authority1 = createAuthority(AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST)
        val authority2 = createAuthority(AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST)
        assertTrue(authority1.isSameCloudAsAuthority(authority2))
    }

    @Test
    fun testIsSameCloudAsAuthority_bleuVsDelos_returnsFalse() {
        val bleu = createAuthority(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST)
        val delos = createAuthority(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST)
        assertFalse(bleu.isSameCloudAsAuthority(delos))
    }

    @Test
    fun testIsSameCloudAsAuthority_bleuVsGovsg_returnsFalse() {
        val bleu = createAuthority(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST)
        val govsg = createAuthority(AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST)
        assertFalse(bleu.isSameCloudAsAuthority(govsg))
    }

    @Test
    fun testIsSameCloudAsAuthority_delosVsGovsg_returnsFalse() {
        val delos = createAuthority(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST)
        val govsg = createAuthority(AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST)
        assertFalse(delos.isSameCloudAsAuthority(govsg))
    }
}
