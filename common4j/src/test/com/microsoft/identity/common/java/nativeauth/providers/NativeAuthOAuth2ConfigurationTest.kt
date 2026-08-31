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
package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import java.net.URL
import org.junit.Test

/**
 * Covers the Native Auth V2 authority scheme rule in
 * [NativeAuthOAuth2Configuration.getNativeAuthV2AuthorityUrl].
 */
class NativeAuthOAuth2ConfigurationTest {

    @Test
    fun getNativeAuthV2AuthorityUrl_whenAuthorityIsHttps_returnsAuthority() {
        val authorityUrl = configuration(
            authorityUrl = "https://login.contoso.com/tenant",
            useMockApi = false
        ).getNativeAuthV2AuthorityUrl(CORRELATION_ID)

        assertEquals(URL("https://login.contoso.com/tenant"), authorityUrl)
    }

    @Test
    fun getNativeAuthV2AuthorityUrl_whenMockAuthorityIsHttps_returnsMockAuthority() {
        val authorityUrl = configuration(
            authorityUrl = "https://login.contoso.com/tenant",
            useMockApi = true,
            mockAuthorityUrlWithTenant = "https://localhost/mock-tenant"
        ).getNativeAuthV2AuthorityUrl(CORRELATION_ID)

        assertEquals(URL("https://localhost/mock-tenant"), authorityUrl)
    }

    @Test
    fun getNativeAuthV2AuthorityUrl_whenMockAuthorityIsPlaintext_returnsMockAuthority() {
        val authorityUrl = configuration(
            authorityUrl = "https://login.contoso.com/tenant",
            useMockApi = true,
            mockAuthorityUrlWithTenant = "http://localhost/mock-tenant"
        ).getNativeAuthV2AuthorityUrl(CORRELATION_ID)

        assertEquals(URL("http://localhost/mock-tenant"), authorityUrl)
    }

    @Test
    fun getNativeAuthV2AuthorityUrl_whenAuthorityIsPlaintextAndMockDisabled_throwsUnsupportedUrl() {
        val configuration = configuration(
            authorityUrl = "http://login.contoso.com/tenant",
            useMockApi = false
        )

        try {
            configuration.getNativeAuthV2AuthorityUrl(CORRELATION_ID)
            fail("Expected a ClientException for a plaintext authority outside mock mode.")
        } catch (exception: ClientException) {
            assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    @Test
    fun getNativeAuthV2AuthorizeChallengeEndpoint_whenMockAuthorityIsHttps_appendsSuffixToMockAuthority() {
        val endpoint = configuration(
            authorityUrl = "https://login.contoso.com/tenant",
            useMockApi = true,
            mockAuthorityUrlWithTenant = "https://localhost/mock-tenant"
        ).getNativeAuthV2AuthorizeChallengeEndpoint(CORRELATION_ID)

        assertEquals(URL("https://localhost/mock-tenant/oauth2/v2.0/authorize-challenge"), endpoint)
    }

    private fun configuration(
        authorityUrl: String,
        useMockApi: Boolean,
        mockAuthorityUrlWithTenant: String = "https://localhost/mock-tenant"
    ): NativeAuthOAuth2Configuration {
        return NativeAuthOAuth2Configuration(
            authorityUrl = URL(authorityUrl),
            clientId = CLIENT_ID,
            challengeType = CHALLENGE_TYPE,
            capabilities = null,
            requestInterceptor = null,
            useMockApiForNativeAuth = useMockApi,
            MOCK_API_URL_WITH_NATIVE_AUTH_TENANT = mockAuthorityUrlWithTenant
        )
    }

    private companion object {
        private const val CLIENT_ID = "client-id"
        private const val CHALLENGE_TYPE = "oob password redirect"
        private const val CORRELATION_ID = "correlation-id"
    }
}
