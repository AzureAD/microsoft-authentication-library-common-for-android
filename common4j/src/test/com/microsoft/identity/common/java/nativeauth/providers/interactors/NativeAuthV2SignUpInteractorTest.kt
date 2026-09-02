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

import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2SignUpStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2SubmitAttributesRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2Operation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2ResponseHandler
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2RequestInterceptor
import io.mockk.CapturingSlot
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.URL
import java.nio.charset.StandardCharsets

/**
 * Covers the V2 sign-up interactor operations: the sign-up entry call and the submit-attributes
 * call. The submit-attributes test also asserts the interactor records the submitted attribute
 * names on the state used for parsing, so the successor state inherits the merged set.
 */
class NativeAuthV2SignUpInteractorTest {

    private val signUpUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/signup/start")
    private val submitAttributesUrl = URL("https://contoso.ciamlogin.com/nativeauth/v2/signup/submitattributes")

    private val httpClient = mockk<UrlConnectionHttpClient>()
    private val requestProvider = mockk<NativeAuthV2RequestProvider>()
    private val responseHandler = mockk<NativeAuthV2ResponseHandler>()
    private val responseParser = mockk<NativeAuthV2ResponseParser>()

    private val requestInterceptor = object : OAuth2RequestInterceptor {
        override fun additionalHeaders(requestUrl: URL): Map<String, String> =
            mapOf(INTERCEPTOR_HEADER to INTERCEPTOR_VALUE)
    }

    private fun createInteractor(): NativeAuthV2Interactor = NativeAuthV2Interactor(
        httpClient = httpClient,
        requestProvider = requestProvider,
        responseHandler = responseHandler,
        responseParser = responseParser,
        requestInterceptor = requestInterceptor
    )

    @Test
    fun performSignUpStart_postsContinuationTokenAndParsesAsSignUpStart() {
        val state = continuationState(NativeAuthV2LinkRelation.SIGN_UP to "/signup/start")
        val request = NativeAuthV2SignUpStartRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            requestUrl = signUpUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"action":"collectAttributes"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = mockk<NativeAuthV2InteractionApiResult.AttributesRequired>(relaxed = true)
        val captured = capturePost(httpResponse)

        every { requestProvider.createSignUpStartRequest(state) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(halResponse, state, NativeAuthV2Operation.SIGN_UP_START)
        } returns expected

        val actual = createInteractor().performSignUpStart(state)

        assertSame(expected, actual)
        assertEquals(signUpUrl, captured.url.captured)
        assertMergedHeaders(captured.headers.captured)
        assertJsonBody(
            captured.body.captured,
            mapOf("continuationToken" to CONTINUATION_TOKEN)
        )
    }

    @Test
    fun performSubmitAttributes_postsAttributesAndRecordsThemOnTheParsedState() {
        val state = continuationState(NativeAuthV2LinkRelation.SUBMIT_ATTRIBUTES to "/signup/submitattributes")
        val attributes = linkedMapOf("city" to "Seattle", "country" to "US")
        val request = NativeAuthV2SubmitAttributesRequest.create(
            continuationToken = CONTINUATION_TOKEN,
            attributes = attributes,
            requestUrl = submitAttributesUrl.toString(),
            headers = jsonHeaders()
        )
        val httpResponse = HttpResponse(200, """{"state":"continue"}""", emptyMap())
        val halResponse = mockk<NativeAuthV2HalApiResponse>(relaxed = true)
        val expected = mockk<NativeAuthV2InteractionApiResult.ReadyToComplete>(relaxed = true)
        val captured = capturePost(httpResponse)
        val parsedState = slot<NativeAuthV2ContinuationState>()

        every { requestProvider.createSubmitAttributesRequest(state, attributes) } returns request
        every { responseHandler.getHalApiResponse(CORRELATION_ID, httpResponse) } returns halResponse
        every {
            responseParser.parseInteraction(halResponse, capture(parsedState), NativeAuthV2Operation.SUBMIT_ATTRIBUTES)
        } returns expected

        val actual = createInteractor().performSubmitAttributes(state, attributes)

        assertSame(expected, actual)
        assertEquals(submitAttributesUrl, captured.url.captured)

        // The state passed to the parser carries the just-submitted attribute names, so the
        // successor inherits the merged set (the original state is left unchanged).
        assertTrue(parsedState.captured.hasSubmittedAttribute("city"))
        assertTrue(parsedState.captured.hasSubmittedAttribute("country"))
        assertFalse(state.hasSubmittedAttribute("city"))

        val body = JSONObject(String(captured.body.captured, StandardCharsets.UTF_8))
        assertEquals(CONTINUATION_TOKEN, body.getString("continuationToken"))
        assertEquals("Seattle", body.getJSONObject("attributes").getString("city"))
        assertEquals("US", body.getJSONObject("attributes").getString("country"))
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    private fun assertMergedHeaders(headers: Map<String, String?>) {
        assertEquals(
            HttpConstants.MediaType.APPLICATION_JSON,
            headers[HttpConstants.HeaderField.CONTENT_TYPE]
        )
        assertEquals(INTERCEPTOR_VALUE, headers[INTERCEPTOR_HEADER])
    }

    private fun assertJsonBody(body: ByteArray, expected: Map<String, String>) {
        val json = JSONObject(String(body, StandardCharsets.UTF_8))
        assertEquals(expected.size, json.length())
        expected.forEach { (key, value) -> assertEquals(value, json.getString(key)) }
    }

    private fun capturePost(httpResponse: HttpResponse): HttpRequestCapture {
        val capturedUrl = slot<URL>()
        val capturedHeaders = slot<Map<String, String?>>()
        val capturedBody = slot<ByteArray>()
        every {
            httpClient.post(capture(capturedUrl), capture(capturedHeaders), capture(capturedBody))
        } returns httpResponse
        return HttpRequestCapture(capturedUrl, capturedHeaders, capturedBody)
    }

    private fun continuationState(
        vararg links: Pair<NativeAuthV2LinkRelation, String>
    ): NativeAuthV2ContinuationState {
        val linksJson = links.joinToString(",") { (relation, href) ->
            "\"${relation.value}\":{\"href\":\"$href\"}"
        }
        val response = NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from("""{"continuationToken":"$CONTINUATION_TOKEN","_links":{$linksJson}}"""),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )
        return NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = response,
            continuationToken = CONTINUATION_TOKEN,
            entryRelation = NativeAuthV2LinkRelation.SIGN_UP,
            scopes = listOf("openid", "offline_access"),
            scenario = NativeAuthV2FlowScenario.SIGN_UP
        )
    }

    private fun jsonHeaders(): Map<String, String?> = mapOf(
        HttpConstants.HeaderField.CONTENT_TYPE to HttpConstants.MediaType.APPLICATION_JSON
    )

    private data class HttpRequestCapture(
        val url: CapturingSlot<URL>,
        val headers: CapturingSlot<Map<String, String?>>,
        val body: CapturingSlot<ByteArray>
    )

    private companion object {
        private const val CORRELATION_ID = "correlation-id"
        private const val CONTINUATION_TOKEN = "continuation-token"
        private const val INTERCEPTOR_HEADER = "x-akamai-sensor"
        private const val INTERCEPTOR_VALUE = "sensor-data-123"
    }
}
