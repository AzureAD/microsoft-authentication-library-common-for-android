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
package com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory

import org.junit.Assert.*
import org.junit.Test

class AzureActiveDirectoryCloudTest {

    @Test
    fun testBleuCloudHostConstant() {
        assertEquals("login.sovcloud-identity.fr", AzureActiveDirectoryCloud.BLEU_CLOUD_HOST)
    }

    @Test
    fun testDelosCloudHostConstant() {
        assertEquals("login.sovcloud-identity.de", AzureActiveDirectoryCloud.DELOS_CLOUD_HOST)
    }

    @Test
    fun testGovsgCloudHostConstant() {
        assertEquals("login.sovcloud-identity.sg", AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST)
    }

    @Test
    fun testBleuCloudInstance() {
        val bleu = AzureActiveDirectoryCloud.BLEU
        assertNotNull(bleu)
        assertEquals(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST, bleu.preferredNetworkHostName)
        assertEquals(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST, bleu.preferredCacheHostName)
        assertNotNull(bleu.hostAliases)
        assertEquals(1, bleu.hostAliases.size)
        assertEquals(AzureActiveDirectoryCloud.BLEU_CLOUD_HOST, bleu.hostAliases[0])
        assertTrue(bleu.isValidated)
    }

    @Test
    fun testDelosCloudInstance() {
        val delos = AzureActiveDirectoryCloud.DELOS
        assertNotNull(delos)
        assertEquals(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST, delos.preferredNetworkHostName)
        assertEquals(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST, delos.preferredCacheHostName)
        assertNotNull(delos.hostAliases)
        assertEquals(1, delos.hostAliases.size)
        assertEquals(AzureActiveDirectoryCloud.DELOS_CLOUD_HOST, delos.hostAliases[0])
        assertTrue(delos.isValidated)
    }

    @Test
    fun testGovsgCloudInstance() {
        val govsg = AzureActiveDirectoryCloud.GOVSG
        assertNotNull(govsg)
        assertEquals(AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST, govsg.preferredNetworkHostName)
        assertEquals(AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST, govsg.preferredCacheHostName)
        assertNotNull(govsg.hostAliases)
        assertEquals(1, govsg.hostAliases.size)
        assertEquals(AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST, govsg.hostAliases[0])
        assertTrue(govsg.isValidated)
    }

    @Test
    fun testSovereignCloudInstancesAreDistinct() {
        assertNotEquals(AzureActiveDirectoryCloud.BLEU, AzureActiveDirectoryCloud.DELOS)
        assertNotEquals(AzureActiveDirectoryCloud.BLEU, AzureActiveDirectoryCloud.GOVSG)
        assertNotEquals(AzureActiveDirectoryCloud.DELOS, AzureActiveDirectoryCloud.GOVSG)
    }
}
