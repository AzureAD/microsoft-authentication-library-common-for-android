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

import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.commands.parameters.JITChallengeAuthMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.jit.JITChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.jit.JITChallengeApiResult
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.util.ObjectMapper

/**
 * Acts as a binding layer between the request providers and response handlers for a given request.
 * The JITInteractor constructs a request for a given endpoint using the command parameters passed into the method,
 * passes that request to the provided HTTP client, and maps the response from that request.
 * @param UrlConnectionHttpClient Used for making HTTP requests with the request object returned from the NativeAuthRequestProvider
 * @param NativeAuthRequestProvider Constructs a request for a given endpoint using the passed command parameters
 * @param NativeAuthResponseHandler Maps the raw HTTP response into a Kotlin class, handling any errors present in the response
 *
 * Used for performing requests to the /introspect, /challenge, and /continue register endpoints.
 */
class JITInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    private val TAG: String = this::class.java.simpleName

    //region /register/challenge
    fun performChallenge(
        parameters: JITChallengeAuthMethodCommandParameters
    ): JITChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.correlationId,
            methodName = "${TAG}.performChallenge(parameters: JITChallengeAuthMethodCommandParameters)"
        )

        val request = nativeAuthRequestProvider.createJITChallengeRequest(
            continuationToken = parameters.continuationToken,
            correlationId = parameters.correlationId,
            challengeType = parameters.authMethodChallengeType,
            challengeTarget = parameters.verificationContact,
            challengeChannel = parameters.challengeChannel
        )

        Logger.infoWithObject(
            "${TAG}.performChallenge",
            parameters.correlationId,
            "request = ",
            request
        )

        return performChallenge(
            requestCorrelationId = parameters.correlationId,
            request = request
        )
    }

    private fun performChallenge(
        requestCorrelationId: String,
        request: JITChallengeRequest
    ): JITChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.performJITChallenge"
        )
        val encodedRequest: String =
            ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getJITChallengeResponseFromHttpResponse(
            requestCorrelationId = requestCorrelationId,
            response = response
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToJITChallengeApiResponse",
            rawApiResponse.correlationId,
            "rawApiResponse = ",
            rawApiResponse
        )

        val result = rawApiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToJITChallengeApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion
}