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
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.NativeAuthMicrosoftStsTokenResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.HalResource
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2HalApiResponse
import com.microsoft.identity.common.java.nativeauth.util.isRedirect
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.util.ObjectMapper
import java.net.HttpURLConnection

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
 * [getTokenApiResponse] follows standard OAuth conventions: 4xx/5xx status codes map through
 * [SignInTokenApiResponse.toErrorResult]; 2xx responses are deserialized as
 * [NativeAuthMicrosoftStsTokenResponse] and returned as [SignInTokenApiResult.Success] (or
 * [SignInTokenApiResult.Redirect] when the service requests a browser redirect).
 */
class NativeAuthV2ResponseHandler {

    private val TAG: String = NativeAuthV2ResponseHandler::class.java.simpleName

    companion object {
        private const val EMPTY_BODY_ERROR_CODE = "empty_body_error"
        private const val PARSE_ERROR_CODE = "response_parse_error"
        private const val EMPTY_BODY_ERROR_MESSAGE = "V2 HAL response body was empty or blank."
        private const val PARSE_ERROR_MESSAGE = "V2 HAL response body was not valid JSON."
        private const val UNAVAILABLE_STATUS_CODE = -1
        private const val UNAVAILABLE_STATUS_ERROR_MESSAGE = "Token response HTTP status code was unavailable."
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

        return if (response.body.isNullOrBlank()) {
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
     * Converts a raw [HttpResponse] from the `/oauth2/v2.0/token` endpoint into a
     * [SignInTokenApiResult]. Follows standard OAuth error conventions: 4xx/5xx status codes map
     * through [SignInTokenApiResponse.toErrorResult]; 2xx responses are deserialized as
     * [NativeAuthMicrosoftStsTokenResponse].
     */
    fun getTokenApiResponse(
        requestCorrelationId: String,
        response: HttpResponse
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = requestCorrelationId,
            methodName = "$TAG.getTokenApiResponse"
        )

        val correlationId = retrieveCorrelationId(response, requestCorrelationId)

        if (response.statusCode == UNAVAILABLE_STATUS_CODE) {
            return buildTokenErrorResult(
                statusCode = response.statusCode,
                correlationId = correlationId,
                errorCode = ApiErrorResult.INVALID_STATE,
                errorMessage = UNAVAILABLE_STATUS_ERROR_MESSAGE
            )
        }

        if (response.statusCode < HttpURLConnection.HTTP_BAD_REQUEST && response.body.isNullOrBlank()) {
            return buildTokenErrorResult(
                statusCode = response.statusCode,
                correlationId = correlationId,
                errorCode = NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR,
                errorMessage = NativeAuthResponseHandler.EMPTY_RESPONSE_ERROR_ERROR_DESCRIPTION
            )
        }

        return if (response.statusCode >= HttpURLConnection.HTTP_BAD_REQUEST) {
            val apiResponse = if (response.body.isNullOrBlank()) {
                SignInTokenApiResponse(
                    statusCode = response.statusCode,
                    correlationId = correlationId,
                    continuationToken = null,
                    error = null,
                    errorDescription = null,
                    errorUri = null,
                    subError = null,
                    errorCodes = null
                )
            } else {
                ObjectMapper.deserializeJsonStringToObject(
                    response.body,
                    SignInTokenApiResponse::class.java
                )
            }
            apiResponse.statusCode = response.statusCode
            apiResponse.correlationId = correlationId
            apiResponse.toErrorResult()
        } else {
            val tokenResponse = ObjectMapper.deserializeJsonStringToObject(
                response.body,
                NativeAuthMicrosoftStsTokenResponse::class.java
            )
            if (tokenResponse.challengeType.isRedirect()) {
                SignInTokenApiResult.Redirect(
                    correlationId = correlationId,
                    redirectReason = tokenResponse.redirectReason.orEmpty()
                )
            } else {
                SignInTokenApiResult.Success(
                    correlationId = correlationId,
                    tokenResponse = tokenResponse
                )
            }
        }
    }

    private fun buildTokenErrorResult(
        statusCode: Int,
        correlationId: String,
        errorCode: String,
        errorMessage: String
    ): SignInTokenApiResult =
        SignInTokenApiResponse(
            statusCode = statusCode,
            correlationId = correlationId,
            continuationToken = null,
            error = errorCode,
            errorDescription = errorMessage,
            errorUri = null,
            subError = null,
            errorCodes = null
        ).toErrorResult()

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
