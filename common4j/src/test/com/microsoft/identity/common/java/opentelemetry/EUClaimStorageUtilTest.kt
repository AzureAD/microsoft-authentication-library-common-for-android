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
package com.microsoft.identity.common.java.opentelemetry

import com.microsoft.identity.common.components.InMemoryStorageSupplier
import com.microsoft.identity.common.java.base64.Base64Flags
import com.microsoft.identity.common.java.base64.Base64Util
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [EUClaimStorageUtil.storeTelemetryRegionByTenant].
 */
class EUClaimStorageUtilTest {

    private lateinit var storageSupplier: InMemoryStorageSupplier

    @Before
    fun setUp() {
        storageSupplier = InMemoryStorageSupplier()
    }

    /**
     * Builds a Base64 URL-safe encoded raw client info string accepted by [ClientInfo].
     * Parameters left null are omitted from the JSON so the field resolves to null inside [ClientInfo].
     */
    private fun buildRawClientInfo(
        uid: String? = null,
        utid: String? = null,
        tdbr: String? = null
    ): String {
        val parts = mutableListOf<String>()
        if (uid != null) parts.add("\"uid\":\"$uid\"")
        if (utid != null) parts.add("\"utid\":\"$utid\"")
        if (tdbr != null) parts.add("\"${ClientInfo.TDBR_CLAIM}\":\"$tdbr\"")
        val json = "{${parts.joinToString(",")}}"
        return Base64Util.encodeToString(json.toByteArray(Charsets.UTF_8), Base64Flags.URL_SAFE)
    }

    @Test
    fun storeTelemetryRegionByTenant_whenTenantIdIsNull_doesNotStoreAnything() {
        // utid field is absent → clientInfo.utid == null
        val clientInfo = ClientInfo(buildRawClientInfo(uid = "test-uid"))

        EUClaimStorageUtil.storeTelemetryRegionByTenant(storageSupplier, clientInfo)

        val store = storageSupplier.getUnencryptedNameValueStore(ClientInfo.TDBR_CLAIM, String::class.java)
        assertTrue(store.getAll().isEmpty())
    }

    @Test
    fun storeTelemetryRegionByTenant_whenTenantIdIsEmpty_doesNotStoreAnything() {
        // utid field is present but empty → StringUtil.isNullOrEmpty returns true
        val clientInfo = ClientInfo(buildRawClientInfo(uid = "test-uid", utid = ""))

        EUClaimStorageUtil.storeTelemetryRegionByTenant(storageSupplier, clientInfo)

        val store = storageSupplier.getUnencryptedNameValueStore(ClientInfo.TDBR_CLAIM, String::class.java)
        assertTrue(store.getAll().isEmpty())
    }

    @Test
    fun storeTelemetryRegionByTenant_whenTdbrClaimIsNull_doesNotStoreAnything() {
        // utid present, xms_tdbr absent → clientInfo.tdbrClaim == null
        val clientInfo = ClientInfo(buildRawClientInfo(uid = "test-uid", utid = "test-utid"))

        EUClaimStorageUtil.storeTelemetryRegionByTenant(storageSupplier, clientInfo)

        val store = storageSupplier.getUnencryptedNameValueStore(ClientInfo.TDBR_CLAIM, String::class.java)
        assertTrue(store.getAll().isEmpty())
    }

    @Test
    fun storeTelemetryRegionByTenant_whenBothFieldsPresent_storesTdbrClaimUnderTenantIdKey() {
        val testTenantId = "test-utid"
        val testTdbrClaim = "eu"
        val clientInfo = ClientInfo(
            buildRawClientInfo(uid = "test-uid", utid = testTenantId, tdbr = testTdbrClaim)
        )

        EUClaimStorageUtil.storeTelemetryRegionByTenant(storageSupplier, clientInfo)

        val store = storageSupplier.getUnencryptedNameValueStore(ClientInfo.TDBR_CLAIM, String::class.java)
        assertEquals(testTdbrClaim, store.get(testTenantId))
    }

    @Test
    fun storeTelemetryRegionByTenant_whenBothFieldsPresent_doesNotStoreUnderOtherKeys() {
        val testTenantId = "test-utid"
        val testTdbrClaim = "eu"
        val clientInfo = ClientInfo(
            buildRawClientInfo(uid = "test-uid", utid = testTenantId, tdbr = testTdbrClaim)
        )

        EUClaimStorageUtil.storeTelemetryRegionByTenant(storageSupplier, clientInfo)

        val store = storageSupplier.getUnencryptedNameValueStore(ClientInfo.TDBR_CLAIM, String::class.java)
        assertNull(store.get("other-tenant"))
    }
}
