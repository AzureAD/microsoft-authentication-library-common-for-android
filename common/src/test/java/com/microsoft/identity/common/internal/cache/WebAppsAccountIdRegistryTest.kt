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
package com.microsoft.identity.common.internal.cache

import com.microsoft.identity.common.components.InMemoryStorageSupplier
import org.junit.Assert
import org.junit.Test

class WebAppsAccountIdRegistryTest {
    private val accountId1 = "11111111-1111-1111-1111-111111111111.aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
    private val accountId2 = "22222222-2222-2222-2222-222222222222.bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
    private val accountId3 = "33333333-3333-3333-3333-333333333333.cccccccc-cccc-cccc-cccc-cccccccccccc"

    private val clientId1 = "99999999-1111-4444-8888-121212121212"
    private val clientId2 = "aaaaaaaa-2222-5555-9999-131313131313"
    private val clientId3 = "bbbbbbbb-3333-6666-aaaa-141414141414"

    @Test
    fun testAddClient_veryBasicTest() {
        val registry = createRegistry()
        registry.addClient(accountId1, clientId1)
        Assert.assertEquals(1, registry.getClients(accountId1).size)
    }

    @Test
    fun testRemoveClient_veryBasicTest() {
        val registry = createRegistry()
        registry.addClient(accountId1, clientId1)
        registry.addClient(accountId1, clientId2)
        Assert.assertEquals(2, registry.getClients(accountId1).size)
        registry.removeClient(accountId1, clientId1)
        Assert.assertEquals(1, registry.getClients(accountId1).size)
    }

    @Test
    fun testRemoveClient_accountEntryCleanedUp() {
        val registry = createRegistry()
        registry.addClient(accountId1, clientId1)
        registry.removeClient(accountId1, clientId1)
        Assert.assertEquals(0, registry.getClients(accountId1).size)
    }

    @Test
    fun testManyCombinedAddAndRemove() {
        val registry = createRegistry()
        registry.addClient(accountId1, clientId1)
        registry.addClient(accountId1, clientId2)
        registry.addClient(accountId2, clientId1)
        registry.addClient(accountId2, clientId3)
        registry.addClient(accountId3, clientId1)
        Assert.assertEquals(2, registry.getClients(accountId1).size)
        Assert.assertEquals(2, registry.getClients(accountId2).size)
        Assert.assertEquals(1, registry.getClients(accountId3).size)

        registry.removeClient(accountId1, clientId1)
        Assert.assertEquals(1, registry.getClients(accountId1).size)

        registry.removeClient(accountId1, clientId2)
        Assert.assertEquals(0, registry.getClients(accountId1).size)

        registry.removeClient(accountId2, clientId3)
        Assert.assertEquals(1, registry.getClients(accountId2).size)

        registry.removeClient(accountId2, clientId1)
        Assert.assertEquals(0, registry.getClients(accountId2).size)

        registry.removeClient(accountId3, clientId1)
        Assert.assertEquals(0, registry.getClients(accountId3).size)
    }

    @Test
    fun testAddClient_addSameClient() {
        val registry = createRegistry()
        registry.addClient(accountId1, clientId1)
        registry.addClient(accountId1, clientId1)
        Assert.assertEquals(1, registry.getClients(accountId1).size)
    }

    @Test
    fun testRemoveAccount_removeAccount() {
        val registry = createRegistry()
        registry.addClient(accountId1, clientId1)
        registry.addClient(accountId1, clientId2)
        registry.addClient(accountId2, clientId1)
        registry.removeAccount(accountId1)
        Assert.assertEquals(0, registry.getClients(accountId1).size)
        Assert.assertEquals(1, registry.getClients(accountId2).size)
    }

    @Test
    fun testContains_containsClientId() {
        val registry = createRegistry()
        registry.addClient(accountId1, clientId1)
        Assert.assertTrue(registry.contains(accountId1, clientId1))
        Assert.assertFalse(registry.contains(accountId1, clientId2))
        Assert.assertFalse(registry.contains(accountId2, clientId1))
    }

    @Test
    fun testPersistenceAcrossInstances() {
        val storageSupplier = InMemoryStorageSupplier()
        val registry1 = WebAppsAccountIdRegistry.create(storageSupplier)
        registry1.addClient(accountId1, clientId1)
        registry1.addClient(accountId1, clientId2)
        registry1.addClient(accountId2, clientId1)

        val registry2 = WebAppsAccountIdRegistry.create(storageSupplier)
        Assert.assertEquals(2, registry2.getClients(accountId1).size)
        Assert.assertEquals(1, registry2.getClients(accountId2).size)

        registry2.removeClient(accountId1, clientId1)
        Assert.assertEquals(1, registry2.getClients(accountId1).size)

        val registry3 = WebAppsAccountIdRegistry.create(storageSupplier)
        Assert.assertEquals(1, registry3.getClients(accountId1).size)
        Assert.assertEquals(1, registry3.getClients(accountId2).size)

        registry3.removeAccount(accountId2)
        Assert.assertEquals(0, registry3.getClients(accountId2).size)
    }

    private fun createRegistry(): WebAppsAccountIdRegistry {
        return WebAppsAccountIdRegistry.create(InMemoryStorageSupplier())
    }
}
