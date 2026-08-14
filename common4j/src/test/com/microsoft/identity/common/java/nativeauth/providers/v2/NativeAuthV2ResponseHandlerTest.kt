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

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.net.HttpResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NativeAuthV2ResponseHandlerTest {
    private val handler = NativeAuthV2ResponseHandler()

    // region getHalApiResponse

    @Test
    fun getHalApiResponse_whenBodyIsValidJson_returnsMappedResponse() {
        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(
                statusCode = 200,
                body = """{"continuationToken":"tok-1","_links":{"resetPassword":{"href":"/reset"}}}"""
            )
        )

        assertEquals(CORRELATION_ID, result.correlationId)
        assertEquals(200, result.statusCode)
        assertEquals("tok-1", result.continuationToken)
        assertEquals("/reset", result.links["resetPassword"])
        assertNull(result.serverError)
    }

    @Test
    fun getHalApiResponse_whenBodyContainsServerError_returnsResponseWithServerError() {
        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(
                statusCode = 400,
                body = """{"error":{"code":"invalid_grant","message":"Bad request."}}"""
            )
        )

        assertEquals(400, result.statusCode)
        assertEquals("invalid_grant", result.serverError?.code)
        assertEquals("Bad request.", result.serverError?.message)
    }

    @Test
    fun getHalApiResponse_whenBodyIsBlank_returnsSyntheticEmptyBodyError() {
        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 200, body = "   ")
        )

        assertEquals(CORRELATION_ID, result.correlationId)
        assertEquals("empty_body_error", result.serverError?.code)
        assertEquals("V2 HAL response body was empty or blank.", result.serverError?.message)
    }

    @Test
    fun getHalApiResponse_whenBodyIsNull_returnsSyntheticEmptyBodyError() {
        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = HttpResponse(200, null, emptyMap())
        )

        assertEquals(CORRELATION_ID, result.correlationId)
        assertEquals("empty_body_error", result.serverError?.code)
    }

    @Test
    fun getHalApiResponse_whenBodyIsMalformedJson_returnsSyntheticParseError() {
        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 200, body = "{not-json")
        )

        assertEquals(CORRELATION_ID, result.correlationId)
        assertEquals("response_parse_error", result.serverError?.code)
        assertEquals("V2 HAL response body was not valid JSON.", result.serverError?.message)
    }

    @Test
    fun getHalApiResponse_whenResponseHeaderHasCorrelationId_usesHeaderValueOverRequestCorrelationId() {
        val headers = mapOf(AuthenticationConstants.AAD.CLIENT_REQUEST_ID to listOf("header-correlation-id"))

        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = HttpResponse(200, """{"continuationToken":"tok-1"}""", headers)
        )

        assertEquals("header-correlation-id", result.correlationId)
    }

    // endregion

    private fun response(statusCode: Int, body: String): HttpResponse =
        HttpResponse(statusCode, body, emptyMap())

    private companion object {
        private const val CORRELATION_ID = "correlation-id"
    }
}
