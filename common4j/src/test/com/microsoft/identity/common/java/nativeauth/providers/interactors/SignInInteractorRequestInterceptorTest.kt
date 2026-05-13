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

import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestInterceptor
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

/**
 * Tests verifying that [SignInInteractor] correctly applies custom headers
 * from a [NativeAuthRequestInterceptor] to outgoing HTTP requests.
 */
class SignInInteractorRequestInterceptorTest {

    private val testUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/initiate")

    private val mockHttpClient = mockk<UrlConnectionHttpClient>()
    private val mockRequestProvider = mockk<NativeAuthRequestProvider>()
    private val mockResponseHandler = mockk<NativeAuthResponseHandler>()

    private fun createMockRequest(
        url: URL = testUrl,
        headers: Map<String, String?> = mapOf("Content-Type" to "application/x-www-form-urlencoded")
    ): SignInInitiateRequest {
        val mockRequest = mockk<SignInInitiateRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns url
        every { mockRequest.headers } returns headers
        return mockRequest
    }

    private fun setupMocks(mockRequest: SignInInitiateRequest) {
        every { mockRequestProvider.createSignInInitiateRequest(any()) } returns mockRequest
        every {
            mockResponseHandler.getSignInInitiateResultFromHttpResponse(any(), any())
        } returns mockk(relaxed = true)
    }

    @Test
    fun testInterceptorHeadersAreMergedIntoRequest() {
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                return mapOf(
                    "x-akamai-sensor" to "sensor-data-123",
                    "x-fraud-signal" to "signal-abc"
                )
            }
        }

        val baseHeaders = mapOf<String, String?>(
            "Content-Type" to "application/x-www-form-urlencoded",
            "x-client-SKU" to "MSAL.Android"
        )
        val mockRequest = createMockRequest(headers = baseHeaders)
        setupMocks(mockRequest)

        val capturedHeaders = slot<Map<String, String>>()
        every {
            mockHttpClient.post(any<URL>(), capture(capturedHeaders), any<ByteArray>())
        } returns HttpResponse(200, "{}", emptyMap())

        val interactor = SignInInteractor(
            httpClient = mockHttpClient,
            nativeAuthRequestProvider = mockRequestProvider,
            nativeAuthResponseHandler = mockResponseHandler,
            requestInterceptor = interceptor
        )

        val mockParameters = mockk<SignInStartCommandParameters>(relaxed = true)
        interactor.performSignInInitiate(mockParameters)

        assertTrue("Headers should have been captured", capturedHeaders.isCaptured)
        val headers = capturedHeaders.captured
        assertEquals("sensor-data-123", headers["x-akamai-sensor"])
        assertEquals("signal-abc", headers["x-fraud-signal"])
        assertEquals("application/x-www-form-urlencoded", headers["Content-Type"])
        assertEquals("MSAL.Android", headers["x-client-SKU"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeaders() {
        val baseHeaders = mapOf<String, String?>(
            "Content-Type" to "application/x-www-form-urlencoded"
        )
        val mockRequest = createMockRequest(headers = baseHeaders)
        setupMocks(mockRequest)

        val capturedHeaders = slot<Map<String, String>>()
        every {
            mockHttpClient.post(any<URL>(), capture(capturedHeaders), any<ByteArray>())
        } returns HttpResponse(200, "{}", emptyMap())

        val interactor = SignInInteractor(
            httpClient = mockHttpClient,
            nativeAuthRequestProvider = mockRequestProvider,
            nativeAuthResponseHandler = mockResponseHandler,
            requestInterceptor = null
        )

        val mockParameters = mockk<SignInStartCommandParameters>(relaxed = true)
        interactor.performSignInInitiate(mockParameters)

        assertTrue("Headers should have been captured", capturedHeaders.isCaptured)
        val headers = capturedHeaders.captured
        assertEquals(1, headers.size)
        assertEquals("application/x-www-form-urlencoded", headers["Content-Type"])
    }

    @Test
    fun testInterceptorReturningNullDoesNotModifyHeaders() {
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? = null
        }

        val baseHeaders = mapOf<String, String?>(
            "Content-Type" to "application/x-www-form-urlencoded"
        )
        val mockRequest = createMockRequest(headers = baseHeaders)
        setupMocks(mockRequest)

        val capturedHeaders = slot<Map<String, String>>()
        every {
            mockHttpClient.post(any<URL>(), capture(capturedHeaders), any<ByteArray>())
        } returns HttpResponse(200, "{}", emptyMap())

        val interactor = SignInInteractor(
            httpClient = mockHttpClient,
            nativeAuthRequestProvider = mockRequestProvider,
            nativeAuthResponseHandler = mockResponseHandler,
            requestInterceptor = interceptor
        )

        val mockParameters = mockk<SignInStartCommandParameters>(relaxed = true)
        interactor.performSignInInitiate(mockParameters)

        assertTrue("Headers should have been captured", capturedHeaders.isCaptured)
        val headers = capturedHeaders.captured
        assertEquals(1, headers.size)
    }

    @Test
    fun testInterceptorReservedHeadersAreFiltered() {
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                return mapOf(
                    "x-akamai-sensor" to "valid",
                    "x-ms-evil" to "should-be-filtered",
                    "x-client-override" to "should-be-filtered",
                    "Authorization" to "should-be-filtered"
                )
            }
        }

        val mockRequest = createMockRequest()
        setupMocks(mockRequest)

        val capturedHeaders = slot<Map<String, String>>()
        every {
            mockHttpClient.post(any<URL>(), capture(capturedHeaders), any<ByteArray>())
        } returns HttpResponse(200, "{}", emptyMap())

        val interactor = SignInInteractor(
            httpClient = mockHttpClient,
            nativeAuthRequestProvider = mockRequestProvider,
            nativeAuthResponseHandler = mockResponseHandler,
            requestInterceptor = interceptor
        )

        val mockParameters = mockk<SignInStartCommandParameters>(relaxed = true)
        interactor.performSignInInitiate(mockParameters)

        assertTrue("Headers should have been captured", capturedHeaders.isCaptured)
        val headers = capturedHeaders.captured
        assertTrue(headers.containsKey("x-akamai-sensor"))
        assertEquals("valid", headers["x-akamai-sensor"])
        assertFalse("x-ms- prefix should be filtered", headers.containsKey("x-ms-evil"))
        assertFalse("x-client- prefix should be filtered", headers.containsKey("x-client-override"))
        assertFalse("Non x- prefix should be filtered", headers.containsKey("Authorization"))
    }

    @Test
    fun testInterceptorReceivesCorrectRequestUrl() {
        val capturedUrls = mutableListOf<URL>()
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                capturedUrls.add(requestUrl)
                return mapOf("x-test" to "value")
            }
        }

        val mockRequest = createMockRequest()
        setupMocks(mockRequest)

        every {
            mockHttpClient.post(any<URL>(), any<Map<String, String>>(), any<ByteArray>())
        } returns HttpResponse(200, "{}", emptyMap())

        val interactor = SignInInteractor(
            httpClient = mockHttpClient,
            nativeAuthRequestProvider = mockRequestProvider,
            nativeAuthResponseHandler = mockResponseHandler,
            requestInterceptor = interceptor
        )

        val mockParameters = mockk<SignInStartCommandParameters>(relaxed = true)
        interactor.performSignInInitiate(mockParameters)

        assertEquals("Interceptor should have been called once", 1, capturedUrls.size)
        assertEquals(testUrl, capturedUrls[0])
    }

    @Test
    fun testInterceptorEmptyHeadersDoNotModifyRequest() {
        val interceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                return emptyMap()
            }
        }

        val baseHeaders = mapOf<String, String?>(
            "Content-Type" to "application/x-www-form-urlencoded"
        )
        val mockRequest = createMockRequest(headers = baseHeaders)
        setupMocks(mockRequest)

        val capturedHeaders = slot<Map<String, String>>()
        every {
            mockHttpClient.post(any<URL>(), capture(capturedHeaders), any<ByteArray>())
        } returns HttpResponse(200, "{}", emptyMap())

        val interactor = SignInInteractor(
            httpClient = mockHttpClient,
            nativeAuthRequestProvider = mockRequestProvider,
            nativeAuthResponseHandler = mockResponseHandler,
            requestInterceptor = interceptor
        )

        val mockParameters = mockk<SignInStartCommandParameters>(relaxed = true)
        interactor.performSignInInitiate(mockParameters)

        assertTrue("Headers should have been captured", capturedHeaders.isCaptured)
        val headers = capturedHeaders.captured
        assertEquals(1, headers.size)
        assertEquals("application/x-www-form-urlencoded", headers["Content-Type"])
    }
}
