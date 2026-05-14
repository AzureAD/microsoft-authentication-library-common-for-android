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

import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestInterceptor
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpStartRequest
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
 * Tests verifying that [SignUpInteractor] correctly applies custom headers
 * from a [NativeAuthRequestInterceptor] to outgoing HTTP requests.
 */
class SignUpInteractorRequestInterceptorTest {

    private val testUrl = URL("https://contoso.ciamlogin.com/oauth2/v2.0/signup/start")

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
    ): SignUpInteractor {
        return SignUpInteractor(
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

    // region performSignUpStart
    @Test
    fun testInterceptorHeadersAreMergedInPerformSignUpStart() {
        val mockRequest = mockk<SignUpStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpStartResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performSignUpStart(mockk<SignUpStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
        assertEquals("signal-abc", capturedHeaders.captured["x-fraud-signal"])
        assertEquals("MSAL.Android", capturedHeaders.captured["x-client-SKU"])
    }

    @Test
    fun testNullInterceptorDoesNotModifyHeadersInPerformSignUpStart() {
        val mockRequest = mockk<SignUpStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpStartResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = null)

        interactor.performSignUpStart(mockk<SignUpStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals(2, capturedHeaders.captured.size)
        assertFalse(capturedHeaders.captured.containsKey("x-akamai-sensor"))
    }
    // endregion

    // region performSignUpChallenge
    @Test
    fun testInterceptorHeadersAreMergedInPerformSignUpChallenge() {
        val mockRequest = mockk<SignUpChallengeRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpChallengeRequest(any(), any()) } returns mockRequest
        every { mockResponseHandler.getSignUpChallengeResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performSignUpChallenge(continuationToken = "token", correlationId = "corr-id")

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
        assertEquals("signal-abc", capturedHeaders.captured["x-fraud-signal"])
    }
    // endregion

    // region performSignUpSubmitCode
    @Test
    fun testInterceptorHeadersAreMergedInPerformSignUpSubmitCode() {
        val mockRequest = mockk<SignUpContinueRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpSubmitCodeRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpContinueResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performSignUpSubmitCode(mockk<SignUpSubmitCodeCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion

    // region performSignUpSubmitPassword
    @Test
    fun testInterceptorHeadersAreMergedInPerformSignUpSubmitPassword() {
        val mockRequest = mockk<SignUpContinueRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpSubmitPasswordRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpContinueResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performSignUpSubmitPassword(mockk<SignUpSubmitPasswordCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion

    // region performSignUpSubmitUserAttributes
    @Test
    fun testInterceptorHeadersAreMergedInPerformSignUpSubmitUserAttributes() {
        val mockRequest = mockk<SignUpContinueRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpSubmitUserAttributesRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpContinueResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor()

        interactor.performSignUpSubmitUserAttributes(mockk<SignUpSubmitUserAttributesCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        assertEquals("sensor-data-123", capturedHeaders.captured["x-akamai-sensor"])
    }
    // endregion

    // region reserved header filtering
    @Test
    fun testInterceptorReservedHeadersAreFilteredInSignUp() {
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

        val mockRequest = mockk<SignUpStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpStartResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = filteringInterceptor)

        interactor.performSignUpStart(mockk<SignUpStartCommandParameters>(relaxed = true))

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

    // region interceptor receives correct URL
    @Test
    fun testInterceptorReceivesCorrectRequestUrl() {
        val capturedUrls = mutableListOf<URL>()
        val urlCapturingInterceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                capturedUrls.add(requestUrl)
                return mapOf("x-test" to "value")
            }
        }

        val mockRequest = mockk<SignUpStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpStartResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)
        every {
            mockHttpClient.post(any<URL>(), any<Map<String, String>>(), any<ByteArray>())
        } returns HttpResponse(200, "{}", emptyMap())

        val interactor = createInteractor(interceptor = urlCapturingInterceptor)

        interactor.performSignUpStart(mockk<SignUpStartCommandParameters>(relaxed = true))

        assertEquals(1, capturedUrls.size)
        assertEquals(testUrl, capturedUrls[0])
    }
    // endregion

    // region reserved header overwrite protection
    @Test
    fun testInterceptorCannotOverwriteReservedBaseHeaders() {
        val overwriteInterceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                return mapOf(
                    "x-client-SKU" to "Evil.SDK",
                    "x-ms-request-id" to "fake-id",
                    "x-akamai-sensor" to "valid-data"
                )
            }
        }

        val mockRequest = mockk<SignUpStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeaders
        every { mockRequestProvider.createSignUpStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpStartResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = overwriteInterceptor)

        interactor.performSignUpStart(mockk<SignUpStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        val headers = capturedHeaders.captured
        // Reserved prefix headers from interceptor should be filtered, preserving base values
        assertEquals("MSAL.Android", headers["x-client-SKU"])
        assertFalse("x-ms- prefix should be filtered", headers.containsKey("x-ms-request-id"))
        // Valid custom header should be merged
        assertEquals("valid-data", headers["x-akamai-sensor"])
    }
    // endregion

    // region case-insensitive header merge
    @Test
    fun testCaseInsensitiveHeaderMerge() {
        val baseHeadersWithCustom = mapOf<String, String?>(
            "Content-Type" to "application/x-www-form-urlencoded",
            "x-client-SKU" to "MSAL.Android",
            "x-existing-custom" to "old-value"
        )

        val caseInterceptor = object : NativeAuthRequestInterceptor {
            override fun additionalHeaders(requestUrl: URL): Map<String, String>? {
                return mapOf(
                    "X-Existing-Custom" to "new-value",
                    "x-new-header" to "new-data"
                )
            }
        }

        val mockRequest = mockk<SignUpStartRequest>(relaxed = true)
        every { mockRequest.requestUrl } returns testUrl
        every { mockRequest.headers } returns baseHeadersWithCustom
        every { mockRequestProvider.createSignUpStartRequest(any()) } returns mockRequest
        every { mockResponseHandler.getSignUpStartResultFromHttpResponse(any(), any()) } returns mockk(relaxed = true)

        val capturedHeaders = setupHttpClientCapture()
        val interactor = createInteractor(interceptor = caseInterceptor)

        interactor.performSignUpStart(mockk<SignUpStartCommandParameters>(relaxed = true))

        assertTrue(capturedHeaders.isCaptured)
        val headers = capturedHeaders.captured
        // The original casing key should be replaced by the normalized (lowercase) key from validator
        assertFalse(
            "Original casing key should be removed",
            headers.containsKey("x-existing-custom") && headers.containsKey("x-Existing-Custom")
        )
        // The value should be the interceptor's new value (validator normalizes to lowercase)
        assertEquals("new-value", headers["x-existing-custom"])
        // New header should be added
        assertEquals("new-data", headers["x-new-header"])
        // Base reserved headers should be preserved
        assertEquals("MSAL.Android", headers["x-client-SKU"])
    }
    // endregion
}
