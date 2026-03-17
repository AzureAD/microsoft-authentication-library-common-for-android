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
package com.microsoft.identity.common.java.authorities;

import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud;

import org.junit.Assert;
import org.junit.Test;

/**
 * Tests for {@link AzureActiveDirectoryAuthority#isSameCloudAsAuthority} with
 * sovereign clouds. Since sovereign clouds are pre-seeded in the cloud metadata
 * cache, these tests do not require network access.
 */
public class AzureActiveDirectoryAuthorityCloudTest {

    private static AzureActiveDirectoryAuthority createAuthority(final String host) {
        return (AzureActiveDirectoryAuthority) Authority.getAuthorityFromAuthorityUrl(
                "https://" + host + "/common");
    }

    @Test
    public void testIsSameCloudAsAuthority_bothBleu_returnsTrue() throws Exception {
        final AzureActiveDirectoryAuthority authority1 = createAuthority(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST);
        final AzureActiveDirectoryAuthority authority2 = createAuthority(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST);
        Assert.assertTrue(authority1.isSameCloudAsAuthority(authority2));
    }

    @Test
    public void testIsSameCloudAsAuthority_bothDelos_returnsTrue() throws Exception {
        final AzureActiveDirectoryAuthority authority1 = createAuthority(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST);
        final AzureActiveDirectoryAuthority authority2 = createAuthority(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST);
        Assert.assertTrue(authority1.isSameCloudAsAuthority(authority2));
    }

    @Test
    public void testIsSameCloudAsAuthority_bothSovsg_returnsTrue() throws Exception {
        final AzureActiveDirectoryAuthority authority1 = createAuthority(AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST);
        final AzureActiveDirectoryAuthority authority2 = createAuthority(AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST);
        Assert.assertTrue(authority1.isSameCloudAsAuthority(authority2));
    }

    @Test
    public void testIsSameCloudAsAuthority_bleuVsDelos_returnsFalse() throws Exception {
        final AzureActiveDirectoryAuthority bleu = createAuthority(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST);
        final AzureActiveDirectoryAuthority delos = createAuthority(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST);
        Assert.assertFalse(bleu.isSameCloudAsAuthority(delos));
    }

    @Test
    public void testIsSameCloudAsAuthority_bleuVsSovsg_returnsFalse() throws Exception {
        final AzureActiveDirectoryAuthority bleu = createAuthority(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST);
        final AzureActiveDirectoryAuthority sovsg = createAuthority(AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST);
        Assert.assertFalse(bleu.isSameCloudAsAuthority(sovsg));
    }

    @Test
    public void testIsSameCloudAsAuthority_delosVsSovsg_returnsFalse() throws Exception {
        final AzureActiveDirectoryAuthority delos = createAuthority(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST);
        final AzureActiveDirectoryAuthority sovsg = createAuthority(AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST);
        Assert.assertFalse(delos.isSameCloudAsAuthority(sovsg));
    }
}
