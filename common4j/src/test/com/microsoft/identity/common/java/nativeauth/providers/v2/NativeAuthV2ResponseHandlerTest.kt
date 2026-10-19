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

import com.google.gson.JsonSyntaxException
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.net.HttpResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2ResponseHandlerTest {
    private val handler = NativeAuthV2ResponseHandler()

    @Test
    fun getTokenApiResponse_explicitErrorCodesIncludingEmptyArrayOverrideMessage() {
        for (codes in listOf(emptyList(), listOf(123, 456))) {
            val result = handler.getTokenApiResponse(
                CORRELATION_ID,
                response(400, """{"error":"invalid_grant",
                    "error_description":"AADSTS50126: Credentials rejected.","error_codes":$codes,
                    "trace_id":123,"timestamp":{}}""")
            ) as SignInTokenApiResult.UnknownError
            assertEquals(codes, result.errorCodes)
            assertNull(result.traceId)
            assertNull(result.timestamp)
        }
    }

    @Test
    fun getTokenApiResponse_preservesDiagnosticsForRecoverableTokenErrors() {
        val cases = listOf(
            "mfa_required" to SignInTokenApiResult.MFARequired::class.java,
            "registration_required" to SignInTokenApiResult.JITRequired::class.java,
            "invalid_oob_value" to SignInTokenApiResult.CodeIncorrect::class.java
        )
        for ((subError, expectedClass) in cases) {
            val result = handler.getTokenApiResponse(
                CORRELATION_ID,
                response(400, """{"error":"invalid_grant","suberror":"$subError",
                    "continuation_token":"token","error_description":"AADSTS123: Diagnostic.",
                    "trace_id":"recoverable-trace","timestamp":"raw-time"}""")
            )
            assertEquals(expectedClass, result.javaClass)
            val error = result as ApiErrorResult
            assertEquals(listOf(123), error.errorCodes)
            assertEquals("recoverable-trace", error.traceId)
            assertEquals("raw-time", error.timestamp)
        }
    }

    @Test
    fun getTokenApiResponse_invalidErrorCodesFallBackToMessageWithoutChangingClassification() {
        for (codes in listOf("null", "\"bad\"", "[100,\"200\"]", "[1.5]", "[2147483648]", "[2147483647.0000001]", "[null]")) {
            val result = handler.getTokenApiResponse(
                CORRELATION_ID,
                response(
                    400,
                    """{"error":"invalid_grant","error_description":"AADSTS50126: Credentials rejected.",
                        "error_codes":$codes}"""
                )
            )

            assertTrue(codes, result is SignInTokenApiResult.UnknownError)
            assertEquals(codes, listOf(50126), (result as ApiErrorResult).errorCodes)
        }
    }

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
    fun getHalApiResponse_whenStatusIsRedirect_parsesBodyNormally() {
        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(
                statusCode = 302,
                body = """{"continuationToken":"tok-1","_links":{"resetPassword":{"href":"/reset"}}}"""
            )
        )

        assertEquals(CORRELATION_ID, result.correlationId)
        assertEquals(302, result.statusCode)
        assertNull(result.serverError)
        assertEquals("tok-1", result.continuationToken)
        assertEquals("/reset", result.links["resetPassword"])
    }

    @Test
    fun getHalApiResponse_whenBodyIsBlank_syntheticErrorCarriesCorrelationId() {
        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 200, body = "")
        )

        assertEquals(CORRELATION_ID, result.serverError?.correlationId)
        assertNull(result.serverError?.innerErrorCode)
    }

    @Test
    fun getHalApiResponse_whenResponseHeaderHasCorrelationId_usesHeaderValueOverRequestCorrelationId() {        val headers = mapOf(AuthenticationConstants.AAD.CLIENT_REQUEST_ID to listOf("header-correlation-id"))

        val result = handler.getHalApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = HttpResponse(200, """{"continuationToken":"tok-1"}""", headers)
        )

        assertEquals("header-correlation-id", result.correlationId)
    }

    // endregion

    // region getTokenApiResponse - success/error bodies

    @Test
    fun getTokenApiResponse_whenSuccessStatusHasValidBody_returnsSuccessWithTokenResponse() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 200, body = """{"access_token":"AT","token_type":"Bearer"}""")
        )

        assertTrue(result is SignInTokenApiResult.Success)
        val success = result as SignInTokenApiResult.Success
        assertEquals(CORRELATION_ID, success.correlationId)
        assertEquals("AT", success.tokenResponse.accessToken)
    }

    @Test
    fun getTokenApiResponse_whenSuccessStatusIndicatesRedirect_returnsRedirectResult() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(
                statusCode = 200,
                body = """{"challenge_type":"redirect","redirect_reason":"sms_not_supported"}"""
            )
        )

        assertTrue(result is SignInTokenApiResult.Redirect)
        val redirect = result as SignInTokenApiResult.Redirect
        assertEquals(CORRELATION_ID, redirect.correlationId)
        assertEquals("sms_not_supported", redirect.redirectReason)
    }

    @Test
    fun getTokenApiResponse_whenErrorStatusHasBody_returnsMappedErrorResult() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(
                statusCode = 400,
                body = """{"error":"invalid_request","error_description":"Bad request."}"""
            )
        )

        assertTrue(result is SignInTokenApiResult.UnknownError)
        val error = result as SignInTokenApiResult.UnknownError
        assertEquals(CORRELATION_ID, error.correlationId)
        assertEquals("invalid_request", error.error)
        assertEquals("Bad request.", error.errorDescription)
        assertNull(error.subError)
    }

    @Test
    fun getTokenApiResponse_preservesSubErrorsAcrossTokenErrorMappings() {
        val cases = listOf(
            Triple("invalid_request", emptyList(), SignInTokenApiResult.UnknownError::class.java),
            Triple("invalid_request", listOf(90000), SignInTokenApiResult.UnknownError::class.java),
            Triple("invalid_request", listOf(50142), SignInTokenApiResult.UnknownError::class.java),
            Triple("invalid_grant", emptyList(), SignInTokenApiResult.UnknownError::class.java),
            Triple("invalid_grant", listOf(50126), SignInTokenApiResult.InvalidCredentials::class.java),
            Triple("invalid_grant", listOf(400002), SignInTokenApiResult.InvalidAuthenticationType::class.java),
            Triple("user_not_found", listOf(50034), SignInTokenApiResult.UserNotFound::class.java),
            Triple("future_error", listOf(90000), SignInTokenApiResult.UnknownError::class.java)
        )

        for ((errorCode, errorCodes, resultClass) in cases) {
            for (subError in listOf("FutureServiceSubError", "", null)) {
                val context = "$errorCode, $errorCodes, suberror=$subError"
                val subErrorJson = subError?.let { "\"$it\"" } ?: "null"
                val result = handler.getTokenApiResponse(
                    requestCorrelationId = CORRELATION_ID,
                    response = response(
                        statusCode = 400,
                        body = """{
                            "error":"$errorCode",
                            "error_description":"Service diagnostic.",
                            "error_codes":$errorCodes,
                            "trace_id":"token-trace",
                            "timestamp":"2026-10-15 11:00:20Z",
                            "suberror":$subErrorJson
                        }"""
                    )
                )

                assertEquals(context, resultClass, result.javaClass)
                val error = result as ApiErrorResult
                assertEquals(context, subError, error.subError)
                assertEquals(context, "token-trace", error.traceId)
                assertEquals(context, "2026-10-15 11:00:20Z", error.timestamp)
                assertEquals(context, errorCode, error.error)
                assertEquals(context, errorCodes, error.errorCodes)
                assertEquals(context, CORRELATION_ID, error.correlationId)
                assertTrue(context, error.errorDescription.orEmpty().endsWith("Service diagnostic."))
            }
        }
    }

    @Test
    fun getTokenApiResponse_whenRequiredContinuationTokenIsMissing_preservesSubError() {
        for (subError in listOf("mfa_required", "registration_required")) {
            val result = handler.getTokenApiResponse(
                requestCorrelationId = CORRELATION_ID,
                response = response(
                    statusCode = 400,
                    body = """{"error":"invalid_grant","suberror":"$subError",
                        "trace_id":"token-trace","timestamp":"raw-time"}"""
                )
            )

            assertTrue(result is SignInTokenApiResult.UnknownError)
            val error = result as ApiErrorResult
            assertEquals(subError, error.subError)
            assertEquals("token-trace", error.traceId)
            assertEquals("raw-time", error.timestamp)
            assertEquals(ApiErrorResult.INVALID_STATE, error.error)
            assertEquals("oauth/v2.0/token did not return a continuation token", error.errorDescription)
        }
    }

    @Test
    fun getTokenApiResponse_whenErrorStatusHasBlankBody_returnsSyntheticErrorResult() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 400, body = "")
        )

        assertTrue(result is SignInTokenApiResult.UnknownError)
        val error = result as SignInTokenApiResult.UnknownError
        assertEquals(CORRELATION_ID, error.correlationId)
        assertEquals("", error.error)
        assertEquals("", error.errorDescription)
    }

    @Test
    fun getTokenApiResponse_whenRedirectStatusHasTokenBody_returnsTypedError() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(
                statusCode = 302,
                body = """{"access_token":"AT","token_type":"Bearer"}"""
            )
        )

        assertTrue(result is SignInTokenApiResult.UnknownError)
        assertEquals(CORRELATION_ID, result.correlationId)
    }

    @Test
    fun getTokenApiResponse_whenSuccessBodyIsJsonNull_returnsTypedError() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 200, body = "null")
        )

        assertTrue(result is SignInTokenApiResult.UnknownError)
        assertEquals(CORRELATION_ID, result.correlationId)
    }

    @Test
    fun getTokenApiResponse_whenErrorBodyIsJsonNull_returnsTypedError() {
        val result = handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 400, body = "null")
        )

        assertTrue(result is SignInTokenApiResult.UnknownError)
        assertEquals(CORRELATION_ID, result.correlationId)
    }

    @Test(expected = JsonSyntaxException::class)
    fun getTokenApiResponse_whenSuccessBodyIsMalformedJson_propagatesDeserializationException() {
        handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 200, body = "{not-json")
        )
    }

    @Test(expected = JsonSyntaxException::class)
    fun getTokenApiResponse_whenErrorStatusBodyIsMalformedJson_propagatesDeserializationException() {
        handler.getTokenApiResponse(
            requestCorrelationId = CORRELATION_ID,
            response = response(statusCode = 400, body = "{not-json")
        )
    }

    // endregion

    private fun response(statusCode: Int, body: String): HttpResponse =
        HttpResponse(statusCode, body, emptyMap())

    private companion object {
        private const val CORRELATION_ID = "correlation-id"
    }
}
