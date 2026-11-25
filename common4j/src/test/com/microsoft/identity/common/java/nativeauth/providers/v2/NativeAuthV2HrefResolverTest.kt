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
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.BuildValues
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.net.URL
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

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
    fun resolve_whenRelativeHrefIsValid_usesOnlyAuthorityOriginAndHrefQuery() {
        val url = resolver().resolve(
            "/api/v0.1/auth/challenge?dc=westus&prompt=login",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/api/v0.1/auth/challenge?dc=westus&prompt=login",
            url.toString()
        )
    }

    @Test
    fun resolve_whenHrefCrossesSecurityBoundary_rejectsWithCorrelationId() {
        mapOf(
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
    fun resolve_whenMockModeUsesForeignHttpHostOrPort_rejectsWithCorrelationId() {
        listOf(
            "http://other.example.com/api/v0.1/auth/challenge",
            "http://login.contoso.com:8080/api/v0.1/auth/challenge"
        ).forEach { href ->
            val exception = assertClientException {
                resolver(
                    authorityUrl = "http://login.contoso.com/tenant",
                    useMockApi = true
                ).resolve(href, CORRELATION_ID)
            }

            assertEquals(ClientException.UNSUPPORTED_URL, exception.errorCode)
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
    fun resolve_whenRelativeHrefUsesOauthPathWithoutLeadingSlash_preservesReturnedPath() {
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
    fun resolve_whenRelativeHrefUsesTenantIdPrefix_preservesItInsteadOfConfiguredTenantPath() {
        val url = resolver(
            authorityUrl = "https://login.contoso.com/tenant-alias"
        ).resolve(
            "/00000000-0000-0000-0000-000000000001/api/v0.1/auth/resetpassword?dc=westus",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/00000000-0000-0000-0000-000000000001/api/v0.1/auth/resetpassword?dc=westus",
            url.toString()
        )
    }

    @Test
    fun resolve_whenRelativeHrefUsesUnexpectedPrefixBeforeSupportedPath_rejectsUnsupportedUrl() {
        listOf(
            "/unexpected/custom/challenge",
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
            "https://login.contoso.com/api/v0.1/auth/challenge/",
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
            "https://login.contoso.com/unexpected/custom/challenge",
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
    fun resolve_whenDcIsConfigured_ignoresItWithoutChangingOriginalQuery() {
        BuildValues.setDC("west us")

        val url = resolver().resolve(
            "/api/v0.1/auth/challenge?item=one&item=two&=unnamed",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/api/v0.1/auth/challenge" +
                "?item=one&item=two&=unnamed",
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
            "https://login.contoso.com/api/v0.1/auth/challenge?DC=eastus&item=one",
            relativeUrl.toString()
        )
        assertEquals(
            "https://login.contoso.com/api/v0.1/auth/challenge?dc=eastus&item=one",
            absoluteUrl.toString()
        )
    }

    @Test
    fun resolve_whenAbsoluteHrefHasNoQueryAndDcConfigured_doesNotAppendDcQuery() {
        BuildValues.setDC("westus")

        val url = resolver().resolve(
            "https://login.contoso.com/api/v0.1/auth/challenge",
            CORRELATION_ID
        )

        assertEquals(
            "https://login.contoso.com/api/v0.1/auth/challenge",
            url.toString()
        )
    }

    @Test
    fun resolve_forCustomAndCiamAuthorities_usesReturnedTenantAndRawQuery() {
        BuildValues.setDC("developer-slice")
        val tenantId = "4710d5e4-43bb-4ff9-89af-30ed8fe31c6d"
        val tenantDomain = "nativeauthsample.onmicrosoft.com"
        val queries = listOf(
            "",
            "?dc=ESTS-PUB-SEASLR1-FD000-TEST1-100",
            "?dc=service&item=one%2Ftwo&item=%2B&=unnamed&flag&empty=",
            "?"
        )
        listOf("https://custom.example.com", "https://nativeauthsample.ciamlogin.com")
            .forEach { origin ->
                listOf(tenantId, tenantDomain).forEach { configuredTenant ->
                    listOf(tenantId, tenantDomain).forEach { returnedTenant ->
                        queries.forEach { query ->
                            val href = "/$returnedTenant/api/v2.0/signin/start$query"
                            val result = resolver("$origin/$configuredTenant?dc=authority-slice")
                                .resolve(href, CORRELATION_ID)
                            assertEquals("$origin$href", result.toString())
                        }
                    }
                }
            }
    }

    @Test
    fun resolve_absoluteContinuation_usesExactServiceOriginTenantAndQuery() {
        BuildValues.setDC("ignored")
        val href = "https://nativeauthsample.ciamlogin.com:8443/" +
            "4710d5e4-43bb-4ff9-89af-30ed8fe31c6d/oauth2/v2.0/authorize-challenge" +
            "?dc=service&item=one%2Ftwo&item=%2B&=unnamed"

        assertEquals(
            href,
            resolver("https://custom.example.com/nativeauthsample.onmicrosoft.com")
                .resolve(href, CORRELATION_ID).toString()
        )
    }

    @Test
    fun resolve_preservesEncodedPathBytes() {
        val href = "/tenant.onmicrosoft.com/api/v2.0/methods/email%2Btest/challenge?value=%2f"
        assertEquals(
            "https://login.contoso.com$href",
            resolver().resolve(href, CORRELATION_ID).toString()
        )
    }

    @Test
    fun resolve_logsOriginMismatchWithoutLoggingUrlOrQueryValues() {
        val warnings = captureWarnings {
            resolver().resolve(
                "https://different.example.com/other-tenant/api/v2.0/signin/start?secret=private-value",
                CORRELATION_ID
            )
        }

        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("authority differs"))
        assertTrue(warnings.single().contains(CORRELATION_ID))
        assertFalse(warnings.single().contains("different.example.com"))
        assertFalse(warnings.single().contains("private-value"))
    }

    @Test
    fun resolve_logsSliceMismatchIncludingMissingSliceInEitherDirection() {
        listOf("developer" to "service", "developer" to "", "" to "service").forEach { (configured, returned) ->
            BuildValues.setDC(configured)
            val query = if (returned.isEmpty()) "" else "?dc=$returned"
            val warnings = captureWarnings {
                resolver().resolve("/tenant/api/v2.0/signin/start$query", CORRELATION_ID)
            }
            assertEquals(1, warnings.size)
            assertTrue(warnings.single().contains("test slice differs"))
        }
    }

    @Test
    fun resolve_matchingAuthorityAndSlice_doesNotWarnForCaseOrExplicitDefaultPort() {
        BuildValues.setDC("service")
        val warnings = captureWarnings {
            resolver("https://login.contoso.com/tenant.onmicrosoft.com").resolve(
                "https://LOGIN.CONTOSO.COM:443/TENANT.ONMICROSOFT.COM/" +
                    "api/v2.0/signin/start?dc=service",
                CORRELATION_ID
            )
        }
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun resolve_logsTenantMismatchButStillUsesReturnedTenantPath() {
        val warnings = captureWarnings {
            val result = resolver("https://login.contoso.com/alias.onmicrosoft.com").resolve(
                "/4710d5e4-43bb-4ff9-89af-30ed8fe31c6d/api/v2.0/signin/start",
                CORRELATION_ID
            )
            assertEquals(
                "https://login.contoso.com/4710d5e4-43bb-4ff9-89af-30ed8fe31c6d/api/v2.0/signin/start",
                result.toString()
            )
        }
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("authority differs"))
    }

    @Test
    fun resolve_comparesAuthorityQuerySliceWhenDeveloperSliceIsAbsent() {
        val warnings = captureWarnings {
            resolver("https://login.contoso.com/tenant?dc=expected").resolve(
                "/tenant/api/v2.0/signin/start?dc=returned", CORRELATION_ID
            )
        }
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("test slice differs"))
    }

    private fun captureWarnings(block: () -> Unit): List<String> {
        val warnings = CopyOnWriteArrayList<String>()
        val delivered = CountDownLatch(1)
        val identifier = "NativeAuthV2HrefResolverTest-${System.nanoTime()}"
        val startTag = "$identifier-start"
        val fenceTag = "$identifier-fence"
        Logger.setLogger(identifier) { tag, level, message, _ ->
            if (tag == startTag) warnings.clear()
            if (tag == fenceTag) delivered.countDown()
            if (tag == NativeAuthV2HrefResolver::class.java.simpleName && level == Logger.LogLevel.WARN) {
                warnings.add(message)
            }
        }
        try {
            Logger.warn(startTag, "Start warning log capture.")
            block()
            Logger.warn(fenceTag, "Drain warning logs.")
            assertTrue("Warning log delivery timed out.", delivered.await(30, TimeUnit.SECONDS))
        } finally {
            Logger.setLogger(identifier, null)
        }
        return warnings.toList()
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
