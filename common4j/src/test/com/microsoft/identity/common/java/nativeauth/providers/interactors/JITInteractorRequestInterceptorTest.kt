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

import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.commands.parameters.JITChallengeAuthMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.JITContinueCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.JITIntrospectCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestInterceptor
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.jit.JITChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.jit.JITContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.jit.JITIntrospectRequest
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL

/**
 * Tests verifying that [JITInteractor] correctly wires the request interceptor
 * to each public method. Merge logic, filtering, and edge cases are covered by
 * [RequestInterceptorHeaderUtilsTest] and [com.microsoft.identity.common.java.nativeauth.providers.NativeAuthHeaderValidatorTest].
 */
class JITInteractorRequestInterceptorTest {

    private val testUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/register/introspect")

    private val mockHttpClient = mockk<UrlConnectionHttpClient>()
    private val mockRequestProvider = mockk<NativeAuthRequestProvider>()
    private val mockResponseHandler = mockk<NativeAuthResponseHandler>()

    private val baseHeaders = mapOf<String, String?>(
        "Content-Type" to "application/x-www-form-urlencoded",
        "x-client-SKU" to "MSAL.Android",
        "Accept" to "application/json"
    )

    private val testInterceptor = object : NativeAuthRequestInterceptor {
        override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
            return mapOf("x-akamai-sensor" to "sensor-data-123")
        }
    }

    private fun createInteractor(
        interceptor: NativeAuthRequestInterceptor? = testInterceptor
    ): JITInteractor {
        return JITInteractor(
            httpClient = mockHttpClient,
            nativeAuthRequestProvider = mockRequestProvider,
            nativeAuthResponseHandler = mockResponseHandler,
            requestInterceptor = interceptor
        )
    }

    private fun setupHttpClientCapture(): io.mockk.CapturingSlot<Map<String, String>> {
        val capturedHeaders = slot<Map<String, String>>()
        every {
            mockHttpClient.post(any<URL>(), capture(capturedHeaders), any<ByteArray>())
        } returns HttpResponse(200, "{}", emptyMap())
        return capturedHeaders
    }

    private val mockPlatformComponents = mockk<IPlatformComponents>(relaxed = true)

    private fun createJITIntrospectParams(): JITIntrospectCommandParameters {
        return JITIntrospectCommandParameters.builder()
            .platformComponents(mockPlatformComponents)
            .correlationId("test-correlation-id")
            .continuationToken("test-continuation-token")
            .build()
    }

    private fun createJITChallengeParams(): JITChallengeAuthMethodCommandParameters {
        return JITChallengeAuthMethodCommandParameters.builder()
            .platformComponents(mockPlatformComponents)
            .correlationId("test-correlation-id")
            .continuationToken("test-continuation-token")
            .authMethodChallengeType("oob")
            .verificationContact("user@contoso.com")
            .challengeChannel("email")
            .build()
    }

    private fun createJITContinueParams(): JITContinueCommandParameters {
        return JITContinueCommandParameters.builder()
            .platformComponents(mockPlatformComponents)
            .correlationId("test-correlation-id")
            .continuationToken("test-continuation-token")
            .grantType("oob")
            .code("123456")
            .build()
    }

    // region performIntrospect
    @Test
    fun testInterceptorHeadersAreMergedInPerformIntrospect() {
        val mockRequest = mockk<JITIntrospectRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createJITIntrospectRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getJITIntrospectApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performIntrospect(createJITIntrospectParams())

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
        assertEquals("MSAL.Android", capturedHeaders.captured["x-client-SKU"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformIntrospect() {
        val mockRequest = mockk<JITIntrospectRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createJITIntrospectRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getJITIntrospectApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performIntrospect(createJITIntrospectParams())

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(3, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performChallenge
    @Test
    fun testInterceptorHeadersAreMergedInPerformChallenge() {
        val mockRequest = mockk<JITChallengeRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createJITChallengeRequest(any(), any(), any(), any(), any()) } returns mockRequest
        every { mockResponseHandler.getJITChallengeApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performChallenge(createJITChallengeParams())

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion

    // region performContinue
    @Test
    fun testInterceptorHeadersAreMergedInPerformContinue() {
        val mockRequest = mockk<JITContinueRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createJITContinueRequest(any(), any(), any(), any()) } returns mockRequest
        every { mockResponseHandler.getJITContinueApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performContinue(createJITContinueParams())

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion
}
