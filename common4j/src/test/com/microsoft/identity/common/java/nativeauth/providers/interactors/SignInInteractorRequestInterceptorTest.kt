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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestInterceptor
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInIntrospectRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInTokenRequest
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
 * Tests verifying that [SignInInteractor] correctly wires the request interceptor
 * to each public method. Merge logic, filtering, and edge cases are covered by
 * [RequestInterceptorHeaderUtilsTest] and [com.microsoft.identity.common.java.nativeauth.providers.NativeAuthHeaderValidatorTest].
 */
class SignInInteractorRequestInterceptorTest {

    private val testUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/initiate")

    private val mockHttpClient = mockk<UrlConnectionHttpClient>()
    private val mockRequestProvider = mockk<NativeAuthRequestProvider>()
    private val mockResponseHandler = mockk<NativeAuthResponseHandler>()

    private val baseHeaders = mapOf<String, String?>(
        "Content-Type" to "application/x-www-form-urlencoded",
        "x-client-SKU" to "MSAL.Android"
    )

    private val testInterceptor = object : NativeAuthRequestInterceptor {
        override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
            return mapOf("x-akamai-sensor" to "sensor-data-123")
        }
    }

    private fun createInteractor(
        interceptor: NativeAuthRequestInterceptor? = testInterceptor
    ): SignInInteractor {
        return SignInInteractor(
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

    // region performSignInInitiate
    @Test
    fun testInterceptorHeadersAreMergedInPerformSignInInitiate() {
        val mockRequest = mockk<SignInInitiateRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignInInitiateRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignInInitiateResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performSignInInitiate(mockk<SignInStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
        assertEquals("MSAL.Android", capturedHeaders.captured["x-client-SKU"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeaders() {
        val mockRequest = mockk<SignInInitiateRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignInInitiateRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignInInitiateResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performSignInInitiate(mockk<SignInStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performIntrospect
    @Test
    fun testInterceptorHeadersAreMergedInPerformIntrospect() {
        val mockRequest = mockk<SignInIntrospectRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createIntrospectRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getSignInIntrospectResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performIntrospect(continuationToken = "token", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformIntrospect() {
        val mockRequest = mockk<SignInIntrospectRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createIntrospectRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getSignInIntrospectResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performIntrospect(continuationToken = "token", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performSignInDefaultChallenge
    @Test
    fun testInterceptorHeadersAreMergedInPerformSignInDefaultChallenge() {
        val mockRequest = mockk<SignInChallengeRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignInDefaultChallengeRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getSignInChallengeResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performSignInDefaultChallenge(continuationToken = "token", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformSignInDefaultChallenge() {
        val mockRequest = mockk<SignInChallengeRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignInDefaultChallengeRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getSignInChallengeResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performSignInDefaultChallenge(continuationToken = "token", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performSignInSelectedChallenge
    @Test
    fun testInterceptorHeadersAreMergedInPerformSignInSelectedChallenge() {
        val mockRequest = mockk<SignInChallengeRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignInSelectedChallengeRequest(any(), any(), any()) } returns mockRequest
        every { mockResponseHandler.getSignInChallengeResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performSignInSelectedChallenge(continuationToken = "token", challengeId = "challenge-1", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformSignInSelectedChallenge() {
        val mockRequest = mockk<SignInChallengeRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignInSelectedChallengeRequest(any(), any(), any()) } returns mockRequest
        every { mockResponseHandler.getSignInChallengeResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performSignInSelectedChallenge(continuationToken = "token", challengeId = "challenge-1", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performOOBTokenRequest
    @Test
    fun testInterceptorHeadersAreMergedInPerformOOBTokenRequest() {
        val mockRequest = mockk<SignInTokenRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createOOBTokenRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignInTokenApiResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performOOBTokenRequest(mockk<SignInSubmitCodeCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformOOBTokenRequest() {
        val mockRequest = mockk<SignInTokenRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createOOBTokenRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignInTokenApiResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performOOBTokenRequest(mockk<SignInSubmitCodeCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performContinuationTokenTokenRequest
    @Test
    fun testInterceptorHeadersAreMergedInPerformContinuationTokenTokenRequest() {
        val mockRequest = mockk<SignInTokenRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createContinuationTokenTokenRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignInTokenApiResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performContinuationTokenTokenRequest(mockk<SignInWithContinuationTokenCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformContinuationTokenTokenRequest() {
        val mockRequest = mockk<SignInTokenRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createContinuationTokenTokenRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignInTokenApiResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performContinuationTokenTokenRequest(mockk<SignInWithContinuationTokenCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performPasswordTokenRequest
    @Test
    fun testInterceptorHeadersAreMergedInPerformPasswordTokenRequest() {
        val mockRequest = mockk<SignInTokenRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createPasswordTokenRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignInTokenApiResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performPasswordTokenRequest(mockk<SignInSubmitPasswordCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformPasswordTokenRequest() {
        val mockRequest = mockk<SignInTokenRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createPasswordTokenRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignInTokenApiResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performPasswordTokenRequest(mockk<SignInSubmitPasswordCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion
}
