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
 * Tests for [Authority.isKnownAuthority] and [Authority.getKnownAuthorityResult]
 * focusing on sovereign cloud recognition via pre-seeded cloud metadata.
 */
class AuthorityKnownAuthorityTest {

    /**
     * Verifies that a sovereign cloud authority (Bleu) is recognized as known
     * through the pre-seeded cloud metadata, without requiring a network call.
     */
    @Test
    fun testIsKnownAuthority_bleuSovereignCloud() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.BLEU_CLOUD_HOST}/common"
        )
        assertTrue(Authority.isKnownAuthority(authority))
    }

    /**
     * Verifies that a sovereign cloud authority (Delos) is recognized as known.
     */
    @Test
    fun testIsKnownAuthority_delosSovereignCloud() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.DELOS_CLOUD_HOST}/common"
        )
        assertTrue(Authority.isKnownAuthority(authority))
    }

    /**
     * Verifies that a sovereign cloud authority (GovSG) is recognized as known.
     */
    @Test
    fun testIsKnownAuthority_govsgSovereignCloud() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.GOVSG_CLOUD_HOST}/common"
        )
        assertTrue(Authority.isKnownAuthority(authority))
    }

    /**
     * Verifies getKnownAuthorityResult returns known=true for a sovereign cloud
     * authority. This exercises the showstopper fix: even if cloud discovery
     * throws (no network), the pre-seeded metadata ensures the authority is
     * still recognized as known.
     */
    @Test
    fun testGetKnownAuthorityResult_bleuSovereignCloud_isKnown() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://${AzureActiveDirectoryCloud.BLEU_CLOUD_HOST}/common"
        )
        val result = Authority.getKnownAuthorityResult(authority)
        assertTrue(result.known)
    }

    /**
     * A completely unknown authority should NOT be recognized as known
     * (assumes no developer-configured knownAuthorities match).
     */
    @Test
    fun testIsKnownAuthority_unknownAuthority() {
        val authority = Authority.getAuthorityFromAuthorityUrl(
            "https://login.unknown-test.example/common"
        )
        assertFalse(Authority.isKnownAuthority(authority))
    }
}
