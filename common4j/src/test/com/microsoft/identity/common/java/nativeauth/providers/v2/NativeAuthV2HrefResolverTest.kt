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
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL

class NativeAuthV2HrefResolverTest {
    @After
    fun tearDown() {
        BuildValues.setDC("")
    }

    private fun resolver(
        authorityUrl: String = "https://login.contoso.com/tenant",
        useMockApi: Boolean = false
    ): NativeAuthV2HrefResolver {
        val config = NativeAuthOAuth2Configuration(
            authorityUrl = URL(authorityUrl),
            clientId = "client-id",
            challengeType = "oob",
            capabilities = null,
            requestInterceptor = null,
            useMockApiForNativeAuth = useMockApi,
            MOCK_API_URL_WITH_NATIVE_AUTH_TENANT = authorityUrl
        )
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

    @Test
    fun resolve_whenHrefIsBlankOrOnlyTenantTemplate_rejectsMalformedUrl() {
        listOf("", "   ", "{tenant}", "/{tenant}").forEach { href ->
            val exception = assertClientException {
                resolver().resolve(href, CORRELATION_ID)
            }

            assertEquals(ClientException.MALFORMED_URL, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    @Test
    fun resolve_whenHrefIsMalformedOrAbsoluteHostIsMissing_rejectsMalformedUrl() {
        listOf(
            "/api/v0.1/auth/challenge?value=%",
            "https:/api/v0.1/auth/challenge"
        ).forEach { href ->
            val exception = assertClientException {
                resolver().resolve(href, CORRELATION_ID)
            }

            assertEquals(ClientException.MALFORMED_URL, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    @Test
    fun resolve_whenRelativeHrefHasNoSupportedApiPath_rejectsUnsupportedUrl() {
        val exception = assertClientException {
            resolver().resolve("/tenant/custom/challenge", CORRELATION_ID)
        }

        assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun resolve_whenRelativeHrefContainsInternalEmptySegment_rejectsMalformedUrl() {
        val exception = assertClientException {
            resolver().resolve("/api/v0.1//challenge", CORRELATION_ID)
        }

        assertEquals(ClientException.MALFORMED_URL, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun resolve_whenRelativeHrefUsesOauthPathWithoutLeadingSlash_preservesTenantPath() {
        val url = resolver().resolve(
            "tenant/oauth2/v2.0/token?client_info=1",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/tenant/oauth2/v2.0/token?client_info=1",
            url.toString()
        )
    }

    @Test
    fun resolve_whenRelativeHrefUsesTenantIdPrefix_replacesItWithConfiguredTenantPath() {
        val url = resolver(
            authorityUrl = "https://login.contoso.com/tenant-alias"
        ).resolve(
            "/00000000-0000-0000-0000-000000000001/api/v0.1/auth/resetpassword?dc=westus",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/tenant-alias/api/v0.1/auth/resetpassword?dc=westus",
            url.toString()
        )
    }

    @Test
    fun resolve_whenRelativeHrefUsesUnexpectedPrefixBeforeSupportedPath_rejectsUnsupportedUrl() {
        listOf(
            "/unexpected/api/v0.1/auth/challenge",
            "/unexpected/tenant/oauth2/v2.0/token"
        ).forEach { href ->
            val exception = assertClientException {
                resolver().resolve(href, CORRELATION_ID)
            }

            assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    @Test
    fun resolve_whenRelativeHrefHasTrailingSlash_allowsTrailingSlash() {
        val url = resolver().resolve("/api/v0.1/auth/challenge/", CORRELATION_ID)

        assertEquals(
            "https://login.contoso.com/tenant/api/v0.1/auth/challenge/",
            url.toString()
        )
    }

    @Test
    fun resolve_whenAbsoluteHrefMatchesConfiguredAuthority_allowsDefaultAndExplicitPort() {
        val defaultPortUrl = resolver().resolve(
            "https://LOGIN.CONTOSO.COM/api/v0.1/auth/challenge",
            CORRELATION_ID
        )
        val explicitPortUrl = resolver(
            authorityUrl = "https://login.contoso.com:8443/tenant"
        ).resolve(
            "https://login.contoso.com:8443/api/v0.1/auth/challenge",
            CORRELATION_ID
        )

        assertEquals(
            "https://LOGIN.CONTOSO.COM/api/v0.1/auth/challenge",
            defaultPortUrl.toString()
        )
        assertEquals(
            "https://login.contoso.com:8443/api/v0.1/auth/challenge",
            explicitPortUrl.toString()
        )
    }

    @Test
    fun resolve_whenAbsoluteHrefUsesTenantPrefixedSupportedPath_preservesPathAndQueryBytes() {
        val url = resolver().resolve(
            "https://login.contoso.com/tenant/api/v0.1/auth/challenge?value=%2B&item=one%2Ftwo",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/tenant/api/v0.1/auth/challenge?value=%2B&item=one%2Ftwo",
            url.toString()
        )
    }

    @Test
    fun resolve_whenAbsoluteHrefUsesUnexpectedPrefixBeforeSupportedPath_rejectsUnsupportedUrl() {
        listOf(
            "https://login.contoso.com/unexpected/api/v0.1/auth/challenge",
            "https://login.contoso.com/00000000-0000-0000-0000-000000000001/api/v0.1/auth/challenge",
            "https://login.contoso.com/unexpected/tenant/oauth2/v2.0/token"
        ).forEach { href ->
            val exception = assertClientException {
                resolver().resolve(href, CORRELATION_ID)
            }

            assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    @Test
    fun resolve_whenAbsoluteHrefContainsDotOrEmptySegments_rejectsMalformedUrl() {
        listOf(
            "https://login.contoso.com/api/v0.1//challenge",
            "https://login.contoso.com/api/v0.1/auth/%2E/challenge",
            "https://login.contoso.com/tenant/oauth2/v2.0/%2e%2E/token"
        ).forEach { href ->
            val exception = assertClientException {
                resolver().resolve(href, CORRELATION_ID)
            }

            assertEquals(ClientException.MALFORMED_URL, exception.errorCode)
            assertEquals(CORRELATION_ID, exception.correlationId)
        }
    }

    @Test
    fun resolve_whenProductionAuthorityUsesHttp_rejectsRelativeHref() {
        val exception = assertClientException {
            resolver(
                authorityUrl = "http://login.contoso.com/tenant",
                useMockApi = false
            ).resolve("/api/v0.1/auth/challenge", CORRELATION_ID)
        }

        assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun resolve_whenMockModeUsesHttpsAuthority_rejectsHttpHref() {
        val exception = assertClientException {
            resolver(
                authorityUrl = "https://login.contoso.com/tenant",
                useMockApi = true
            ).resolve("http://login.contoso.com/api/v0.1/auth/challenge", CORRELATION_ID)
        }

        assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
        assertEquals(CORRELATION_ID, exception.correlationId)
    }

    @Test
    fun resolve_whenDcIsConfigured_appendsEncodedDcWithoutChangingOriginalQuery() {
        BuildValues.setDC("west us")

        val url = resolver().resolve(
            "/api/v0.1/auth/challenge?item=one&item=two&=unnamed",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/tenant/api/v0.1/auth/challenge" +
                "?item=one&item=two&=unnamed&dc=west%20us",
            url.toString()
        )
    }

    @Test
    fun resolve_whenDcQueryAlreadyExists_doesNotAppendDuplicate() {
        BuildValues.setDC("westus")

        val relativeUrl = resolver().resolve(
            "/api/v0.1/auth/challenge?DC=eastus&item=one",
            CORRELATION_ID
        )
        val absoluteUrl = resolver().resolve(
            "https://login.contoso.com/api/v0.1/auth/challenge?dc=eastus&item=one",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/tenant/api/v0.1/auth/challenge?DC=eastus&item=one",
            relativeUrl.toString()
        )
        assertEquals(
            "https://login.contoso.com/api/v0.1/auth/challenge?dc=eastus&item=one",
            absoluteUrl.toString()
        )
    }

    @Test
    fun resolve_whenAbsoluteHrefHasNoQueryAndDcConfigured_appendsDcQuery() {
        BuildValues.setDC("westus")

        val url = resolver().resolve(
            "https://login.contoso.com/api/v0.1/auth/challenge",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/api/v0.1/auth/challenge?dc=westus",
            url.toString()
        )
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
