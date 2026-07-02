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

import com.google.gson.Gson
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockkStatic
import io.mockk.spyk
import io.mockk.unmockkAll
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests for [Authority.isKnownAuthority] and [Authority.getKnownAuthorityResult].
 */
class AuthorityKnownAuthorityTest {

    @Before
    fun setUp() {
        unmockkAll()
        // Re-seed sovereign cloud metadata in case a previous test's mockkStatic
        // on AzureActiveDirectory cleared the static map.
        AzureActiveDirectory.setEnvironment(Environment.Production)
    }

    @After
    fun tearDown() {
        unmockkAll()
        AzureActiveDirectory.setEnvironment(Environment.Production)
        // Clear any authorities added during tests to prevent leaking state.
        Authority.clearKnownAuthorities()
    }

    // ---- isKnownAuthority tests ----

    @Test
    fun testIsKnownAuthority_bleuSovereignCloud() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.BLEU_CLOUD_HOST}/common"
        )
        assertTrue(Authority.isKnownAuthority(authority))
    }

    @Test
    fun testIsKnownAuthority_delosSovereignCloud() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.DELOS_CLOUD_HOST}/common"
        )
        assertTrue(Authority.isKnownAuthority(authority))
    }

    @Test
    fun testIsKnownAuthority_govsgSovereignCloud() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST}/common"
        )
        assertTrue(Authority.isKnownAuthority(authority))
    }

    @Test
    fun testIsKnownAuthority_unknownAuthority() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://login.unknown-test.example/common"
        )
        assertFalse(Authority.isKnownAuthority(authority))
    }

    @Test
    fun testIsKnownAuthority_nullAuthorityUrl_returnsFalse() {
        val authority = spyk(Authority.getAuthorityFromAuthorityUrl(
            "https://login.microsoftonline.com/common"
        ))
        every { authority.authorityURL } returns null
        assertFalse(Authority.isKnownAuthority(authority))
    }

    // ---- getKnownAuthorityResult tests ----

    @Test
    fun testGetKnownAuthorityResult_bleuSovereignCloud_isKnown() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.BLEU_CLOUD_HOST}/common"
        )
        val result = Authority.getKnownAuthorityResult(authority)
        assertTrue(result.known)
    }

    @Test
    fun testIsKnownAuthority_nullAuthority_returnsFalse() {
        assertFalse(Authority.isKnownAuthority(null))
    }

    /**
     * Developer-configured authority should be recognized even if discovery fails.
     * Registers the authority in knownAuthorities so isKnownAuthority finds it.
     */
    @Test
    fun testGetKnownAuthorityResult_developerConfigured_discoveryFails_stillKnown() {
        // Simulate a GSON-deserialized authority by constructing from JSON,
        // which populates mAuthorityUrlString (used by isKnownAuthority matching).
        val json = """{"type":"AAD","authority_url":"https://login.developer-configured.example/common"}"""
        val configuredAuthority = Gson().fromJson(json, AzureActiveDirectoryAuthority::class.java)
        Authority.addKnownAuthorities(listOf(configuredAuthority))

        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://login.developer-configured.example/common"
        )

        mockkStatic(AzureActiveDirectory::class)
        every {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(any<Authority>())
        } throws ClientException(ClientException.IO_ERROR, "Network unavailable")
        every {
            AzureActiveDirectory.hasCloudHost(any())
        } returns false

        val result = Authority.getKnownAuthorityResult(authority)

        assertTrue(result.known)
        assertNull(result.clientException)
    }

    /**
     * When discovery fails and the authority is NOT known via any source,
     * the original discovery exception should be propagated.
     */
    @Test
    fun testGetKnownAuthorityResult_discoveryFails_unknownAuthority_propagatesDiscoveryError() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://login.unknown-test.example/common"
        )
        mockkStatic(AzureActiveDirectory::class)
        every {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(any<Authority>())
        } throws ClientException(ClientException.IO_ERROR, "Network unavailable")
        every {
            AzureActiveDirectory.hasCloudHost(any())
        } returns false

        val result = Authority.getKnownAuthorityResult(authority)

        assertFalse(result.known)
        assertNotNull(result.clientException)
        assertEquals(
            "Should propagate IO_ERROR, not UNKNOWN_AUTHORITY",
            ClientException.IO_ERROR,
            result.clientException.errorCode
        )
    }

    /**
     * When discovery succeeds but the authority is genuinely not known,
     * should report UNKNOWN_AUTHORITY.
     */
    @Test
    fun testGetKnownAuthorityResult_discoverySucceeds_unknownAuthority_reportsUnknownAuthority() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://login.unknown-test.example/common"
        )
        mockkStatic(AzureActiveDirectory::class)
        every {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(any<Authority>())
        } just Runs
        every {
            AzureActiveDirectory.hasCloudHost(any())
        } returns false

        val result = Authority.getKnownAuthorityResult(authority)

        assertFalse(result.known)
        assertNotNull(result.clientException)
        assertEquals(
            ClientException.UNKNOWN_AUTHORITY,
            result.clientException.errorCode
        )
    }

    /**
     * When discovery fails but the authority IS in pre-seeded metadata,
     * it should still be known (isKnownAuthority falls back to cache).
     */
    @Test
    fun testGetKnownAuthorityResult_discoveryFails_preSeededAuthority_stillKnown() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.BLEU_CLOUD_HOST}/common"
        )
        mockkStatic(AzureActiveDirectory::class)
        every {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(any<Authority>())
        } throws ClientException(ClientException.IO_ERROR, "Network unavailable")
        every {
            AzureActiveDirectory.hasCloudHost(any())
        } returns true

        val result = Authority.getKnownAuthorityResult(authority)

        assertTrue(result.known)
        assertNull(result.clientException)
    }

    /**
     * When discovery succeeds and the authority is known, result should be known.
     */
    @Test
    fun testGetKnownAuthorityResult_discoverySucceeds_knownAuthority_isKnown() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.BLEU_CLOUD_HOST}/common"
        )
        mockkStatic(AzureActiveDirectory::class)
        every {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(any<Authority>())
        } just Runs
        every {
            AzureActiveDirectory.hasCloudHost(any())
        } returns true

        val result = Authority.getKnownAuthorityResult(authority)

        assertTrue(result.known)
        assertNull(result.clientException)
        // Verify discovery was called
        verify(exactly = 1) {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(any<Authority>())
        }
    }

    /**
     * MALFORMED_URL error from discovery should be propagated as-is.
     */
    @Test
    fun testGetKnownAuthorityResult_discoveryFails_malformedUrl_propagatesOriginalError() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://login.unknown-test.example/common"
        )
        mockkStatic(AzureActiveDirectory::class)
        every {
            AzureActiveDirectory.ensureCloudDiscoveryForAuthority(any<Authority>())
        } throws ClientException(ClientException.MALFORMED_URL, "Bad URL")
        every {
            AzureActiveDirectory.hasCloudHost(any())
        } returns false

        val result = Authority.getKnownAuthorityResult(authority)

        assertFalse(result.known)
        assertEquals(
            ClientException.MALFORMED_URL,
            result.clientException.errorCode
        )
    }
}
