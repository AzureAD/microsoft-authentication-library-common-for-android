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
 * The defining rule for [getHalApiResponse]: an HTTP 3xx status is rejected outright and never
 * body-parsed. V2 request bodies carry the continuation token (and the OTP on verify), and
 * [NativeAuthV2HrefResolver] is the only authority check applied to a request target, so following
 * a redirect would route a secret-bearing body around that check.
 *
 * Note this is transport-level redirect *rejection*, and is unrelated to
 * [com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult.Redirect],
 * which is the application-level web-fallback signal produced by the parser.
 *
 * All non-3xx statuses still parse the body: the authorize-challenge `401` is a success signal
 * carrying the continuation token and HAL links, and several 4xx bodies carry flow state. Status
 * alone is otherwise not treated as terminal — the status code is recorded and the body is parsed;
 * classification is the parser's responsibility.
 *
 * Empty and non-JSON bodies return a synthetic [NativeAuthV2HalApiResponse] carrying a
 * [NativeAuthV2HalApiResponse.HalServerError] with a safe error code rather than throwing.
 *
 */
class NativeAuthV2ResponseHandler {

    private val TAG: String = NativeAuthV2ResponseHandler::class.java.simpleName

    companion object {
        private val UNSUPPORTED_REDIRECT_STATUS_CODES = 300..399
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
     * [NativeAuthV2HalApiResponse].
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

        return if (response.statusCode in UNSUPPORTED_REDIRECT_STATUS_CODES) {
            Logger.warn(TAG, "Rejecting Native Auth V2 redirect response; redirects are not followed. statusCode=${response.statusCode}.")
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
     * for cases where the response body was absent or could not be parsed.
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
