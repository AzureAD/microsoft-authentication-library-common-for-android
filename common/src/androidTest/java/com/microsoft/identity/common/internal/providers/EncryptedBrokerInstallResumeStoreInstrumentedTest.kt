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
package com.microsoft.identity.common.internal.providers

import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.microsoft.identity.common.java.providers.BrokerInstallResumeRequest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import java.util.UUID

/**
 * On-device round-trip tests for [EncryptedBrokerInstallResumeStore]. Validates the two POC
 * hardening guarantees the resume feature relies on:
 *
 *  - **D1 — encrypted persistence:** the snapshot is written through the platform's encrypted
 *    name-value storage and survives a fresh [EncryptedBrokerInstallResumeStore.create] (i.e. a
 *    separate store instance reading the same backing file, standing in for process death within
 *    the TTL window).
 *  - **D5 — full-object fidelity:** the *entire* request parameter set round-trips verbatim, so the
 *    resumed request reproduces the original rather than a lossy subset.
 *
 * Plus the interface contract: **single-use** (removed on read) and **TTL-bounded** (expired
 * entries are purged and never returned).
 */
@RunWith(AndroidJUnit4::class)
class EncryptedBrokerInstallResumeStoreInstrumentedTest {

    private val context = ApplicationProvider.getApplicationContext<android.content.Context>()

    private fun newStore() = EncryptedBrokerInstallResumeStore.create(context)

    private fun fullRequest(
        cid: String = UUID.randomUUID().toString(),
        createdAtMs: Long = System.currentTimeMillis(),
        ttlMs: Long = BrokerInstallResumeRequest.DEFAULT_TTL_MS
    ) = BrokerInstallResumeRequest(
        correlationId = cid,
        authority = "https://login.microsoftonline.com/common",
        clientId = "11112222-3333-4444-5555-666677778888",
        redirectUri = "msauth://com.contoso.app/Signature%3D",
        scopes = listOf("User.Read", "Mail.Read"),
        extraScopesToConsent = listOf("Files.Read"),
        loginHint = "idlab1@msidlab4.onmicrosoft.com",
        claims = "{\"access_token\":{\"xms_cc\":{\"values\":[\"CP1\"]}}}",
        prompt = "login",
        extraQueryParameters = "foo=bar&baz=qux",
        createdAtMs = createdAtMs,
        ttlMs = ttlMs
    )

    @Test
    fun fullParams_roundTrip_verbatim_acrossStoreInstances() {
        val original = fullRequest()

        // Persist with one instance; consume with a *fresh* instance reading the same backing
        // encrypted file — stands in for the persist (pre-install) and resume (post-install) paths
        // running in different process lifetimes.
        newStore().save(original)
        val resumed = newStore().consume(original.correlationId, System.currentTimeMillis())

        requireNotNull(resumed) { "Persisted request should survive a fresh store instance." }
        assertEquals(original.correlationId, resumed.correlationId)
        assertEquals(original.authority, resumed.authority)
        assertEquals(original.clientId, resumed.clientId)
        assertEquals(original.redirectUri, resumed.redirectUri)
        assertEquals(original.scopes, resumed.scopes)
        assertEquals(original.extraScopesToConsent, resumed.extraScopesToConsent)
        assertEquals(original.loginHint, resumed.loginHint)
        assertEquals(original.claims, resumed.claims)
        assertEquals(original.prompt, resumed.prompt)
        assertEquals(original.extraQueryParameters, resumed.extraQueryParameters)
        assertEquals(original.createdAtMs, resumed.createdAtMs)
        assertEquals(original.ttlMs, resumed.ttlMs)
        // Full structural equality as a belt-and-suspenders check.
        assertEquals(original, resumed)
    }

    @Test
    fun consume_isSingleUse() {
        val request = fullRequest()
        newStore().save(request)

        val first = newStore().consume(request.correlationId, System.currentTimeMillis())
        requireNotNull(first) { "First consume should return the request." }

        val second = newStore().consume(request.correlationId, System.currentTimeMillis())
        assertNull("Second consume must return null — entry is single-use.", second)
    }

    @Test
    fun consume_expiredEntry_returnsNullAndPurges() {
        // createdAt far enough in the past that nowMs - createdAt >= ttl.
        val expired = fullRequest(
            createdAtMs = System.currentTimeMillis() - (BrokerInstallResumeRequest.DEFAULT_TTL_MS + 1000L)
        )
        newStore().save(expired)

        val resumed = newStore().consume(expired.correlationId, System.currentTimeMillis())
        assertNull("Expired entry must not be returned.", resumed)

        // Even though it was expired, it must have been purged (single-use semantics on read).
        val again = newStore().consume(expired.correlationId, System.currentTimeMillis())
        assertNull("Expired entry must be purged after the first consume.", again)
    }

    @Test
    fun consume_unknownCorrelationId_returnsNull() {
        val resumed = newStore().consume(UUID.randomUUID().toString(), System.currentTimeMillis())
        assertNull("Unknown correlation id must return null.", resumed)
    }

    @Test
    fun consume_justBeforeExpiry_stillReturned() {
        val now = System.currentTimeMillis()
        // createdAt = now - (ttl - 1s) so that now - createdAt = ttl - 1s < ttl -> not expired.
        val request = fullRequest(createdAtMs = now - (BrokerInstallResumeRequest.DEFAULT_TTL_MS - 1000L))
        newStore().save(request)

        val resumed = newStore().consume(request.correlationId, now)
        assertTrue("Request within TTL must be returned.", resumed != null)
    }
}
