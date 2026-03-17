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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory;

import org.junit.Assert;
import org.junit.Test;

public class AzureActiveDirectoryCloudTest {

    @Test
    public void testBleuCloudHostConstant() {
        Assert.assertEquals("login.sovcloud-identity.fr", AzureActiveDirectoryCloud.BLEU_CLOUD_HOST);
    }

    @Test
    public void testDelosCloudHostConstant() {
        Assert.assertEquals("login.sovcloud-identity.de", AzureActiveDirectoryCloud.DELOS_CLOUD_HOST);
    }

    @Test
    public void testSovsgCloudHostConstant() {
        Assert.assertEquals("login.sovcloud-identity.sg", AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST);
    }

    @Test
    public void testBleuCloudInstance() {
        final AzureActiveDirectoryCloud bleu = AzureActiveDirectoryCloud.BLEU;
        Assert.assertNotNull(bleu);
        Assert.assertEquals(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST, bleu.getPreferredNetworkHostName());
        Assert.assertEquals(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST, bleu.getPreferredCacheHostName());
        Assert.assertNotNull(bleu.getHostAliases());
        Assert.assertEquals(1, bleu.getHostAliases().size());
        Assert.assertEquals(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST, bleu.getHostAliases().get(0));
        Assert.assertTrue(bleu.isValidated());
    }

    @Test
    public void testDelosCloudInstance() {
        final AzureActiveDirectoryCloud delos = AzureActiveDirectoryCloud.DELOS;
        Assert.assertNotNull(delos);
        Assert.assertEquals(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST, delos.getPreferredNetworkHostName());
        Assert.assertEquals(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST, delos.getPreferredCacheHostName());
        Assert.assertNotNull(delos.getHostAliases());
        Assert.assertEquals(1, delos.getHostAliases().size());
        Assert.assertEquals(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST, delos.getHostAliases().get(0));
        Assert.assertTrue(delos.isValidated());
    }

    @Test
    public void testSovsgCloudInstance() {
        final AzureActiveDirectoryCloud sovsg = AzureActiveDirectoryCloud.SOVSG;
        Assert.assertNotNull(sovsg);
        Assert.assertEquals(AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST, sovsg.getPreferredNetworkHostName());
        Assert.assertEquals(AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST, sovsg.getPreferredCacheHostName());
        Assert.assertNotNull(sovsg.getHostAliases());
        Assert.assertEquals(1, sovsg.getHostAliases().size());
        Assert.assertEquals(AzureActiveDirectoryCloud.SOVSG_CLOUD_HOST, sovsg.getHostAliases().get(0));
        Assert.assertTrue(sovsg.isValidated());
    }

    @Test
    public void testSovereignCloudInstancesAreDistinct() {
        Assert.assertNotEquals(AzureActiveDirectoryCloud.BLEU, AzureActiveDirectoryCloud.DELOS);
        Assert.assertNotEquals(AzureActiveDirectoryCloud.BLEU, AzureActiveDirectoryCloud.SOVSG);
        Assert.assertNotEquals(AzureActiveDirectoryCloud.DELOS, AzureActiveDirectoryCloud.SOVSG);
    }
}
