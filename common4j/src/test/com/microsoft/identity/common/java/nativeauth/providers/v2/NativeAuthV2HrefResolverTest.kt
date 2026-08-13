//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.providers.v2

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL

class NativeAuthV2HrefResolverTest {
    private fun resolver(
        authorityUrl: String = "https://login.contoso.com/tenant",
        useMockApi: Boolean = false
    ): NativeAuthV2HrefResolver {
        val config = mockk<NativeAuthOAuth2Configuration> {
            every { getAuthorityUrl() } returns URL(authorityUrl)
            every { useMockApiForNativeAuth } returns useMockApi
        }
        return NativeAuthV2HrefResolver(config)
    }

    @Test
    fun resolve_whenRelativeHrefContainsDotPathSegment_rejectsBeforeTenantPathMerge() {
        listOf(
            "/api/v0.1/auth/./challenge",
            "/api/v0.1/auth/../challenge",
            "/api/v0.1/auth/%2E/challenge",
            "/api/v0.1/auth/%2e%2E/challenge"
        ).forEach { href ->
            val exception = assertClientException { resolver().resolve(href, CORRELATION_ID) }

            assertEquals(ClientException.MALFORMED_URL, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    @Test
    fun resolve_whenRelativeHrefIsValid_preservesTenantPathAndHrefQuery() {
        val url = resolver().resolve(
            "/api/v0.1/auth/challenge?dc=westus&prompt=login",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/tenant/api/v0.1/auth/challenge?dc=westus&prompt=login",
            url.toString()
        )
    }

    @Test
    fun resolve_whenHrefCrossesSecurityBoundary_rejectsWithCorrelationId() {
        mapOf(
            "https://evil.example.com/api/v0.1/auth/challenge" to ClientException.UNKNOWN_AUTHORITY,
            "https://login.contoso.com:8443/api/v0.1/auth/challenge" to ClientException.UNKNOWN_AUTHORITY,
            "http://login.contoso.com/api/v0.1/auth/challenge" to ClientException.UNSUPPORTED_URL,
            "//evil.example.com/api/v0.1/auth/challenge" to ClientException.UNSUPPORTED_URL,
            "https://user@login.contoso.com/api/v0.1/auth/challenge" to ClientException.UNSUPPORTED_URL,
            "https://login.contoso.com/api/v0.1/auth/challenge#fragment" to ClientException.UNSUPPORTED_URL
        ).forEach { (href, expectedErrorCode) ->
            val exception = assertClientException {
                resolver().resolve(href, CORRELATION_ID)
            }

            assertEquals(expectedErrorCode, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    @Test
    fun resolve_whenMockModeUsesConfiguredHttpAuthority_allowsHref() {
        val url = resolver(
            authorityUrl = "http://login.contoso.com/tenant",
            useMockApi = true
        ).resolve(
            "http://login.contoso.com/api/v0.1/auth/challenge",
            CORRELATION_ID
        )

        assertEquals("http://login.contoso.com/api/v0.1/auth/challenge", url.toString())
    }

    @Test
    fun resolve_whenMockModeUsesForeignHostOrPort_rejectsWithCorrelationId() {
        listOf(
            "https://evil.example.com/api/v0.1/auth/challenge",
            "http://login.contoso.com:8080/api/v0.1/auth/challenge"
        ).forEach { href ->
            val exception = assertClientException {
                resolver(
                    authorityUrl = "http://login.contoso.com/tenant",
                    useMockApi = true
                ).resolve(href, CORRELATION_ID)
            }

            assertEquals(ClientException.UNKNOWN_AUTHORITY, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    private fun assertClientException(block: () -> Unit): ClientException {
        try {
            block()
            fail("Expected ClientException")
        } catch (exception: ClientException) {
            return exception
        }
        throw AssertionError("Unreachable")
    }

    private companion object {
        private const val CORRELATION_ID = "correlation-id"
    }
}
