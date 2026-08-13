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

import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.net.HttpResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2ResponseHandlerTest {
    private val handler = NativeAuthV2ResponseHandler()

    @Test
    fun getTokenApiResponse_whenStatusCodeUnavailable_returnsTypedUnknownError() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = -1, body = "{not-json")
        )

        assertTrue(result is SignInTokenApiResult.UnknownError)
        val error = result as SignInTokenApiResult.UnknownError
        assertEquals(CORRELATION_ID, error.correlationId)
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
    }

    @Test
    fun getTokenApiResponse_whenSuccessfulStatusHasBlankBody_returnsTypedUnknownError() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 200, body = " ")
        )

        assertTrue(result is SignInTokenApiResult.UnknownError)
        val error = result as SignInTokenApiResult.UnknownError
        assertEquals(CORRELATION_ID, error.correlationId)
        assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR, error.error)
        assertEquals(NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION, error.errorDescription)
    }

    private fun response(statusCode: Int, body: String): HttpResponse =
        HttpResponse(statusCode, body, emptyMap())

    private companion object {
        private const val CORRELATION_ID = "correlation-id"
    }
}
