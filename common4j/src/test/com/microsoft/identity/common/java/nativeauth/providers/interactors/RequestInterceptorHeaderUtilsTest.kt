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
package com.microsoft.identity.common.java.nativeauth.providers.interactors

import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestInterceptor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

class RequestInterceptorHeaderUtilsTest {

    private val requestUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/initiate")

    @Test
    fun testApplyInterceptorHeadersReturnsOriginalMapWhenInterceptorIsNull() {
        val headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")

        val result = applyInterceptorHeaders(requestUrl, headers, null)

        assertSame(headers, result)
    }

    @Test
    fun testApplyInterceptorHeadersMergesValidCustomHeaders() {
        val headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String> {
                return mapOf("x-test-header" to "value")
            }
        }

        val result = applyInterceptorHeaders(requestUrl, headers, interceptor)

        assertEquals(2, result.size)
        assertEquals("application/x-www-form-urlencoded", result["Content-Type"])
        assertEquals("value", result["x-test-header"])
    }

    @Test
    fun testApplyInterceptorHeadersFiltersReservedAndNonCustomHeaders() {
        val headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String> {
                return mapOf(
                    "x-ms-client-request-id" to "reserved",
                    "Authorization" to "rejected",
                    "x-valid" to "kept"
                )
            }
        }

        val result = applyInterceptorHeaders(requestUrl, headers, interceptor)

        assertEquals(2, result.size)
        assertTrue(result.containsKey("x-valid"))
        assertFalse(result.containsKey("Authorization"))
        assertFalse(result.containsKey("x-ms-client-request-id"))
    }

    @Test
    fun testApplyInterceptorHeadersMergesCaseInsensitiveWithBaseHeaders() {
        val headers = mapOf(
            "X-Custom-Header" to "base",
            "Content-Type" to "application/x-www-form-urlencoded"
        )
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String> {
                return mapOf("x-custom-header" to "override")
            }
        }

        val result = applyInterceptorHeaders(requestUrl, headers, interceptor)

        assertEquals(2, result.size)
        assertEquals("override", result["x-custom-header"])
        assertFalse(result.containsKey("X-Custom-Header"))
    }

    @Test
    fun testApplyInterceptorHeadersPassesRequestUrlToInterceptor() {
        var capturedUrl: URL? = null
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                capturedUrl = requestUrl
                return emptyMap()
            }
        }

        applyInterceptorHeaders(
            requestUrl = requestUrl,
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
            requestInterceptor = interceptor
        )

        assertEquals(requestUrl, capturedUrl)
    }
}
