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
package com.microsoft.identity.common.java.nativeauth.providers.interactors

import com.microsoft.identity.common.java.providers.oauth2.OAuth2RequestInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

/**
 * Unit tests for [applyInterceptorHeaders], the shared helper that merges
 * interceptor-provided custom headers into base request headers.
 *
 * These tests cover the helper's contract directly, so interactor-level tests
 * only need to verify that each interactor method passes headers through
 * to the HTTP client (i.e., the wiring, not the merge logic).
 */
class RequestInterceptorHeaderUtilsTest {

    private val testUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/initiate")

    private val baseHeaders = mapOf<String, String?>(
        "Content-Type" to "application/x-www-form-urlencoded",
        "x-client-SKU" to "MSAL.Android",
        "Accept" to "application/json"
    )

    // region null / empty interceptor scenarios

    @Test
    fun testNullInterceptorReturnsSameHeaders() {
        val result = applyInterceptorHeaders(testUrl, baseHeaders, null)
        assertSame("Null interceptor should return the exact same map instance", baseHeaders, result)
    }

    @Test
    fun testInterceptorReturningNullReturnsSameHeaders() {
        val interceptor = object : OAuth2RequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? = null
        }
        val result = applyInterceptorHeaders(testUrl, baseHeaders, interceptor)
        assertSame("Interceptor returning null should return the exact same map instance", baseHeaders, result)
    }

    @Test
    fun testInterceptorReturningEmptyMapReturnsSameHeaders() {
        val interceptor = object : OAuth2RequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? = emptyMap()
        }
        val result = applyInterceptorHeaders(testUrl, baseHeaders, interceptor)
        assertSame("Interceptor returning empty map should return the exact same map instance", baseHeaders, result)
    }

    // endregion

    // region valid header merge

    @Test
    fun testValidCustomHeadersAreMergedWithBaseHeaders() {
        val interceptor = object : OAuth2RequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String> {
                return mapOf(
                    "x-akamai-sensor" to "sensor-data-123",
                    "x-fraud-signal" to "signal-abc"
                )
            }
        }

        val result = applyInterceptorHeaders(testUrl, baseHeaders, interceptor)

        assertEquals(5, result.size)
        assertEquals("sensor-data-123", result["x-akamai-sensor"])
        assertEquals("signal-abc", result["x-fraud-signal"])
        assertEquals("application/x-www-form-urlencoded", result["Content-Type"])
        assertEquals("MSAL.Android", result["x-client-SKU"])
        assertEquals("application/json", result["Accept"])
    }

    // endregion

    // region reserved header filtering (integration with NativeAuthHeaderValidator)

    @Test
    fun testReservedPrefixHeadersAreFiltered() {
        val interceptor = object : OAuth2RequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String> {
                return mapOf(
                    "x-akamai-sensor" to "valid",
                    "x-ms-evil" to "should-be-filtered",
                    "x-client-override" to "should-be-filtered",
                    "x-app-secret" to "should-be-filtered",
                    "x-broker-bypass" to "should-be-filtered",
                    "Authorization" to "should-be-filtered"
                )
            }
        }

        val result = applyInterceptorHeaders(testUrl, baseHeaders, interceptor)

        assertTrue(result.containsKey("x-akamai-sensor"))
        assertFalse("x-ms- prefix should be filtered", result.containsKey("x-ms-evil"))
        assertFalse("x-client- prefix should be filtered", result.containsKey("x-client-override"))
        assertFalse("x-app- prefix should be filtered", result.containsKey("x-app-secret"))
        assertFalse("x-broker- prefix should be filtered", result.containsKey("x-broker-bypass"))
        assertFalse("Non x- prefix should be filtered", result.containsKey("authorization"))
    }

    @Test
    fun testInterceptorCannotOverwriteReservedBaseHeaders() {
        val interceptor = object : OAuth2RequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String> {
                return mapOf(
                    "x-client-SKU" to "Evil.SDK",
                    "x-ms-request-id" to "fake-id",
                    "x-akamai-sensor" to "valid-data"
                )
            }
        }

        val result = applyInterceptorHeaders(testUrl, baseHeaders, interceptor)

        // Reserved prefix headers from interceptor should be filtered, preserving base values
        assertEquals("MSAL.Android", result["x-client-SKU"])
        assertFalse("x-ms- prefix should be filtered", result.containsKey("x-ms-request-id"))
        // Valid custom header should be merged
        assertEquals("valid-data", result["x-akamai-sensor"])
    }

    @Test
    fun testAllInvalidHeadersReturnsSameBaseSize() {
        val interceptor = object : OAuth2RequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String> {
                return mapOf(
                    "x-ms-evil" to "filtered",
                    "Authorization" to "filtered",
                    "Content-Type" to "filtered"
                )
            }
        }

        val result = applyInterceptorHeaders(testUrl, baseHeaders, interceptor)

        assertEquals(baseHeaders.size, result.size)
        assertEquals("application/x-www-form-urlencoded", result["Content-Type"])
        assertEquals("MSAL.Android", result["x-client-SKU"])
        assertEquals("application/json", result["Accept"])
    }

    // endregion

    // region case-insensitive merge

    @Test
    fun testCaseInsensitiveHeaderMerge() {
        val baseHeadersWithCustom = mapOf<String, String?>(
            "Content-Type" to "application/x-www-form-urlencoded",
            "x-client-SKU" to "MSAL.Android",
            "x-existing-custom" to "old-value"
        )

        val interceptor = object : OAuth2RequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String> {
                return mapOf(
                    "X-Existing-Custom" to "new-value",
                    "x-new-header" to "new-data"
                )
            }
        }

        val result = applyInterceptorHeaders(testUrl, baseHeadersWithCustom, interceptor)

        // Original casing key should be replaced by the normalized (lowercase) key from validator
        assertFalse(
            "Original and new casing keys should not both exist",
            result.containsKey("x-existing-custom") && result.containsKey("X-Existing-Custom")
        )
        assertEquals("new-value", result["x-existing-custom"])
        assertEquals("new-data", result["x-new-header"])
        assertEquals("MSAL.Android", result["x-client-SKU"])
    }

    // endregion

    // region URL passthrough

    @Test
    fun testInterceptorReceivesCorrectRequestUrl() {
        val capturedUrls = mutableListOf<URL>()
        val interceptor = object : OAuth2RequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                capturedUrls.add(requestUrl)
                return mapOf("x-test" to "value")
            }
        }

        applyInterceptorHeaders(testUrl, baseHeaders, interceptor)

        assertEquals(1, capturedUrls.size)
        assertEquals(testUrl, capturedUrls[0])
    }

    // endregion
}
