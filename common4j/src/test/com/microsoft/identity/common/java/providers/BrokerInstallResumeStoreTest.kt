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
package com.microsoft.identity.common.java.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerInstallResumeStoreTest {

    private fun request(cid: String, createdAt: Long, ttl: Long = 5 * 60 * 1000L) =
        BrokerInstallResumeRequest(
            correlationId = cid,
            authority = "https://login.microsoftonline.com/common",
            clientId = "client-1",
            redirectUri = "msauth://com.contoso.app",
            scopes = listOf("User.Read"),
            loginHint = "user@contoso.com",
            createdAtMs = createdAt,
            ttlMs = ttl
        )

    @Test
    fun consume_whenWithinTtl_returnsRequest() {
        val store = InMemoryBrokerInstallResumeStore()
        store.save(request("cid", createdAt = 0L))
        assertEquals("cid", store.consume("cid", nowMs = 1000L)?.correlationId)
    }

    @Test
    fun consume_isSingleUse_secondReadReturnsNull() {
        val store = InMemoryBrokerInstallResumeStore()
        store.save(request("cid", createdAt = 0L))
        store.consume("cid", nowMs = 1000L)
        assertNull(store.consume("cid", nowMs = 1000L))
    }

    @Test
    fun consume_whenExpired_returnsNull() {
        val store = InMemoryBrokerInstallResumeStore()
        store.save(request("cid", createdAt = 0L, ttl = 1000L))
        assertNull(store.consume("cid", nowMs = 5000L))
    }

    @Test
    fun consume_unknownCorrelationId_returnsNull() {
        assertNull(InMemoryBrokerInstallResumeStore().consume("missing", nowMs = 0L))
    }

    @Test
    fun isExpired_boundaryIsTtlInclusive() {
        val r = request("cid", createdAt = 0L, ttl = 1000L)
        assertFalse(r.isExpired(999L))
        assertTrue(r.isExpired(1000L))
    }
}
