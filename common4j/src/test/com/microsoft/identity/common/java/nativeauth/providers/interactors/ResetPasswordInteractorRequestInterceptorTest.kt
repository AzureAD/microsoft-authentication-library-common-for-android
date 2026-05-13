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

import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestInterceptor
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordPollCompletionRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordSubmitRequest
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
 * Tests verifying that [ResetPasswordInteractor] correctly applies custom headers
 * from a [NativeAuthRequestInterceptor] to outgoing HTTP requests.
 */
class ResetPasswordInteractorRequestInterceptorTest {

    private val testUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/resetpassword/start")

    private val mockHttpClient = mockk<UrlConnectionHttpClient>()
    private val mockRequestProvider = mockk<NativeAuthRequestProvider>()
    private val mockResponseHandler = mockk<NativeAuthResponseHandler>()

    private val baseHeaders = mapOf<String, String?>(
        "Content-Type" to "application/x-www-form-urlencoded",
        "x-client-SKU" to "MSAL.Android"
    )

    private val testInterceptor = object : NativeAuthRequestInterceptor {
        override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
            return mapOf(
                "x-akamai-sensor" to "sensor-data-123",
                "x-fraud-signal" to "signal-abc"
            )
        }
    }

    private fun createInteractor(
        interceptor: NativeAuthRequestInterceptor? = testInterceptor
    ): ResetPasswordInteractor {
        return ResetPasswordInteractor(
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

    // region performResetPasswordStart
    @Test
    fun testInterceptorHeadersAreMergedInPerformResetPasswordStart() {
        val mockRequest = mockk<ResetPasswordStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createResetPasswordStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getResetPasswordStartApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performResetPasswordStart(mockk<ResetPasswordStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
        assertEquals("signal-abc", capturedHeaders.captured["x-fraud-signal"])
        assertEquals("MSAL.Android", capturedHeaders.captured["x-client-SKU"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformResetPasswordStart() {
        val mockRequest = mockk<ResetPasswordStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createResetPasswordStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getResetPasswordStartApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performResetPasswordStart(mockk<ResetPasswordStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performResetPasswordChallenge
    @Test
    fun testInterceptorHeadersAreMergedInPerformResetPasswordChallenge() {
        val mockRequest = mockk<ResetPasswordChallengeRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createResetPasswordChallengeRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getResetPasswordChallengeApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performResetPasswordChallenge(continuationToken = "token", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion

    // region performResetPasswordContinue
    @Test
    fun testInterceptorHeadersAreMergedInPerformResetPasswordContinue() {
        val mockRequest = mockk<ResetPasswordContinueRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createResetPasswordContinueRequest(any()) } returns mockRequest
        every { mockResponseHandler.getResetPasswordContinueApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performResetPasswordContinue(mockk<ResetPasswordSubmitCodeCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion

    // region performResetPasswordSubmit
    @Test
    fun testInterceptorHeadersAreMergedInPerformResetPasswordSubmit() {
        val mockRequestParams = mockk<ResetPasswordSubmitRequest.NativeAuthResetPasswordSubmitRequestParameters>(relaxed = true)
        val mockRequest = mockk<ResetPasswordSubmitRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequest.parameters } returns mockRequestParams
        every { mockRequestProvider.createResetPasswordSubmitRequest(any()) } returns mockRequest
        every { mockResponseHandler.getResetPasswordSubmitApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performResetPasswordSubmit(mockk<ResetPasswordSubmitNewPasswordCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion

    // region performResetPasswordPollCompletion
    @Test
    fun testInterceptorHeadersAreMergedInPerformResetPasswordPollCompletion() {
        val mockRequest = mockk<ResetPasswordPollCompletionRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createResetPasswordPollCompletionRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getResetPasswordPollCompletionApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performResetPasswordPollCompletion(continuationToken = "token", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion

    // region reserved header filtering
    @Test
    fun testInterceptorReservedHeadersAreFilteredInResetPassword() {
        val filteringInterceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
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

        val mockRequest = mockk<ResetPasswordStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createResetPasswordStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getResetPasswordStartApiResponseFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = filteringInterceptor)

        interactor.performResetPasswordStart(mockk<ResetPasswordStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        val headers = capturedHeaders.captured
        assertTrue(headers.containsKey("x-akamai-sensor"))
        assertFalse("x-ms- prefix should be filtered", headers.containsKey("x-ms-evil"))
        assertFalse("x-client- prefix should be filtered", headers.containsKey("x-client-override"))
        assertFalse("x-app- prefix should be filtered", headers.containsKey("x-app-secret"))
        assertFalse("x-broker- prefix should be filtered", headers.containsKey("x-broker-bypass"))
        assertFalse("Non x- prefix should be filtered", headers.containsKey("Authorization"))
    }
    // endregion
}
