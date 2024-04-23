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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartApiResult
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters as SignUpSubmitPasswordCommandParameters1

/**
 * Acts as a binding layer between the request providers and response handlers for a given request.
 * The SignUpInteractor constructs a request for a given endpoint using the command parameters passed into the method,
 * passes that request to the provided HTTP client, and maps the response from that request.
 * @param UrlConnectionHttpClient Used for making HTTP requests with the request object returned from the NativeAuthRequestProvider
 * @param NativeAuthRequestProvider Constructs a request for a given endpoint using the passed command parameters
 * @param NativeAuthResponseHandler Maps the raw HTTP response into a Kotlin class, handling any errors present in the response
 *
 * Used for performing requests to the /start, /challenge, and /continue Sign Up endpoints.
 */
class SignUpInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    private val TAG:String = SignUpInteractor::class.java.simpleName

    //region /signup/start
    fun performSignUpStart(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.performSignUpStart"
        )

        val request = nativeAuthRequestProvider.createSignUpStartRequest(
            commandParameters = commandParameters
        )
        try {
            return performSignUpStart(
                requestCorrelationId = commandParameters.getCorrelationId(),
                request = request
            )
        } finally {
            StringUtil.overwriteWithNull(request.parameters.password)
        }
    }

    private fun performSignUpStart(
        requestCorrelationId: String,
        request: SignUpStartRequest
    ): SignUpStartApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.performSignUpStart"
        )

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignUpStartResultFromHttpResponse(
            response = response,
            requestCorrelationId = requestCorrelationId
        )
        return rawApiResponse.toResult()
    }
    //endregion

    //region /signup/challenge
    fun performSignUpChallenge(
        continuationToken: String,
        correlationId: String
    ): SignUpChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performSignUpChallenge"
        )

        val request = nativeAuthRequestProvider.createSignUpChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
        return performSignUpChallenge(
            requestCorrelationId = correlationId,
            request = request
        )
    }

    private fun performSignUpChallenge(
        requestCorrelationId: String,
        request: SignUpChallengeRequest
    ): SignUpChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.performSignUpChallenge"
        )

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignUpChallengeResultFromHttpResponse(
            response = response,
            requestCorrelationId = requestCorrelationId
        )
        return rawApiResponse.toResult()
    }
    //endregion

    //region /signup/continue
    fun performSignUpSubmitCode(commandParameters: SignUpSubmitCodeCommandParameters): SignUpContinueApiResult {
        val request = nativeAuthRequestProvider.createSignUpSubmitCodeRequest(
            commandParameters = commandParameters
        )

        return performSignUpContinue(
            requestCorrelationId = commandParameters.getCorrelationId(),
            request = request
        )
    }

    fun performSignUpSubmitPassword(commandParameters: SignUpSubmitPasswordCommandParameters1): SignUpContinueApiResult {
        val request = nativeAuthRequestProvider.createSignUpSubmitPasswordRequest(
            commandParameters = commandParameters
        )

        try {
            return performSignUpContinue(
                requestCorrelationId = commandParameters.getCorrelationId(),
                request = request
            )
        } finally {
            StringUtil.overwriteWithNull(request.parameters.password)
        }
    }

    fun performSignUpSubmitUserAttributes(commandParameters: SignUpSubmitUserAttributesCommandParameters): SignUpContinueApiResult {
        val request = nativeAuthRequestProvider.createSignUpSubmitUserAttributesRequest(
            commandParameters = commandParameters
        )

        return performSignUpContinue(
            requestCorrelationId = commandParameters.getCorrelationId(),
            request = request
        )
    }

    private fun performSignUpContinue(
        requestCorrelationId: String,
        request: SignUpContinueRequest
    ): SignUpContinueApiResult {
        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignUpContinueResultFromHttpResponse(
            response = response,
            requestCorrelationId = requestCorrelationId
        )
        return rawApiResponse.toResult()
    }
    //endregion
}
