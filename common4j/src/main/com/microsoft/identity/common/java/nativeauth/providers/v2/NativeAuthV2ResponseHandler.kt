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
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.net.HttpResponse

/**
 * Converts raw [HttpResponse] objects into V2 Native Auth typed response models.
 *
 * The defining rule for [getHalApiResponse]: parse the body on every HTTP status. The authorize-
 * challenge `401` is a success signal carrying the continuation token and HAL links; several 4xx
 * bodies carry flow state. Status alone is never treated as terminal — the status code is recorded
 * and the body is always parsed; classification is the parser's responsibility.
 *
 * Empty and non-JSON bodies return a synthetic [NativeAuthV2HalApiResponse] carrying a
 * [NativeAuthV2HalApiResponse.HalServerError] with a safe error code rather than throwing.
 *
 */
class NativeAuthV2ResponseHandler {

    private val TAG: String = NativeAuthV2ResponseHandler::class.java.simpleName

    companion object {
        private val HTTP_REDIRECT_STATUS_CODE_RANGE = 300..399
        private const val EMPTY_BODY_ERROR_CODE = "empty_body_error"
        private const val PARSE_ERROR_CODE = "response_parse_error"
        private const val REDIRECT_RESPONSE_ERROR_CODE = "redirect_response_error"
        private const val EMPTY_BODY_ERROR_MESSAGE = "V2 HAL response body was empty or blank."
        private const val PARSE_ERROR_MESSAGE = "V2 HAL response body was not valid JSON."
        private const val REDIRECT_RESPONSE_ERROR_MESSAGE =
            "Native Auth V2 does not allow HTTP redirect responses."
    }

    /**
     * Converts a raw [HttpResponse] from any V2 Native Auth HAL endpoint into a
     * [NativeAuthV2HalApiResponse]. The body is parsed regardless of HTTP status; a missing or
     * malformed body produces a synthetic safe error response rather than an exception.
     */
    fun getHalApiResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): NativeAuthV2HalApiResponse {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = requestCorrelationId,
            methodName = "$TAG.getHalApiResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        return if (response.statusCode in HTTP_REDIRECT_STATUS_CODE_RANGE) {
            Logger.warn(TAG, "V2 HAL redirect response is not supported for statusCode=${response.statusCode}.")
            buildSyntheticErrorResponse(
                statusCode = response.statusCode,
                correlationId = correlationId,
                errorCode = REDIRECT_RESPONSE_ERROR_CODE,
                errorMessage = REDIRECT_RESPONSE_ERROR_MESSAGE
            )
        } else if (response.body.isNullOrBlank()) {
            Logger.warn(TAG, "V2 HAL response body is empty for statusCode=${response.statusCode}.")
            buildSyntheticErrorResponse(
                statusCode = response.statusCode,
                correlationId = correlationId,
                errorCode = EMPTY_BODY_ERROR_CODE,
                errorMessage = EMPTY_BODY_ERROR_MESSAGE
            )
        } else {
            try {
                val halResource = HalResource.from(response.body)
                NativeAuthV2HalApiResponse.from(
                    halResource = halResource,
                    statusCode = response.statusCode,
                    correlationId = correlationId
                )
            } catch (e: ClientException) {
                Logger.warn(TAG, "V2 HAL response body could not be parsed: ${e.message}")
                buildSyntheticErrorResponse(
                    statusCode = response.statusCode,
                    correlationId = correlationId,
                    errorCode = PARSE_ERROR_CODE,
                    errorMessage = PARSE_ERROR_MESSAGE
                )
            }
        }
    }

    /**
     * Builds a [NativeAuthV2HalApiResponse] carrying a synthetic [NativeAuthV2HalApiResponse.HalServerError]
     * for cases where the response body was absent or could not be parsed. Uses the normal
     * [NativeAuthV2HalApiResponse.from] factory via a synthesized minimal HAL JSON body so
     * invariants enforced by the private constructor are preserved.
     */
    private fun buildSyntheticErrorResponse(
        statusCode: Int,
        correlationId: String,
        errorCode: String,
        errorMessage: String
    ): NativeAuthV2HalApiResponse {
        val syntheticJson = """{"error":{"code":"$errorCode","message":"$errorMessage"}}"""
        val halResource = HalResource.from(syntheticJson)
        return NativeAuthV2HalApiResponse.from(
            halResource = halResource,
            statusCode = statusCode,
            correlationId = correlationId
        )
    }

    private fun retrieveCorrelationId(response: HttpResponse, requestCorrelationId: String): String {
        val responseCorrelationId = response.getHeaderValue(
            AuthenticationConstants.AAD.CLIENT_REQUEST_ID, 0
        )
        return if (responseCorrelationId.isNullOrBlank()) requestCorrelationId else responseCorrelationId
    }
}
