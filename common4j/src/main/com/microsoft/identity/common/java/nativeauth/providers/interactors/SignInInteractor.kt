// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.providers.interactors

import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInIntrospectRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInTokenRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInIntrospectApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.java.util.StringUtil

/**
 * Acts as a binding layer between the request providers and response handlers for a given request.
 * The SignInInteractor constructs a request for a given endpoint using the command parameters passed into the method,
 * passes that request to the provided HTTP client, and maps the response from that request.
 * @param UrlConnectionHttpClient Used for making HTTP requests with the request object returned from the NativeAuthRequestProvider
 * @param NativeAuthRequestProvider Constructs a request for a given endpoint using the passed command parameters
 * @param NativeAuthResponseHandler Maps the raw HTTP response into a Kotlin class, handling any errors present in the response
 *
 * Used for performing requests to the /initiate, /challenge, and /token OAuth endpoints.
 */
class SignInInteractor(
    private val httpClient: UrlConnectionHttpClient,
    private val nativeAuthRequestProvider: NativeAuthRequestProvider,
    private val nativeAuthResponseHandler: NativeAuthResponseHandler
) {
    private val TAG:String = SignInInteractor::class.java.simpleName
    //region /oauth/v2.0/initiate
    fun performSignInInitiate(
        parameters: SignInStartCommandParameters
    ): SignInInitiateApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.correlationId,
            methodName = "${TAG}.performSignInInitiate(parameters: SignInStartCommandParameters)"
        )

        val request = nativeAuthRequestProvider.createSignInInitiateRequest(
            commandParameters = parameters
        )

        Logger.infoWithObject(
            "${TAG}.performSignInInitiate",
            parameters.getCorrelationId(),
            "request = ",
            request
        )

        return performSignInInitiate(
            requestCorrelationId = parameters.getCorrelationId(),
            request = request
        )
    }

    private fun performSignInInitiate(
        requestCorrelationId: String,
        request: SignInInitiateRequest
    ): SignInInitiateApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.performSignInInitiate"
        )
        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )

        val rawApiResponse = nativeAuthResponseHandler.getSignInInitiateResultFromHttpResponse(
            requestCorrelationId = requestCorrelationId,
            response = response
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToSignInInitiateApiResponse",
            rawApiResponse.correlationId,
            "rawApiResponse = ",
            rawApiResponse
        )

        val result = rawApiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToSignInInitiateApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region /oauth/v2.0/introspect
    fun performSignInIntrospect(
        continuationToken: String,
        correlationId: String
    ): SignInIntrospectApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performSignInIntrospect(continuationToken: String)"
        )

        val request = nativeAuthRequestProvider.createSignInIntrospectRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        Logger.infoWithObject(
            "${TAG}.performSignInIntrospect",
            correlationId,
            "request = ",
            request
        )

        return performSignInIntrospect(
            requestCorrelationId = correlationId,
            request = request
        )
    }

    private fun performSignInIntrospect(
        requestCorrelationId: String,
        request: SignInIntrospectRequest
    ): SignInIntrospectApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.performSignInIntrospect"
        )
        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignInIntrospectResultFromHttpResponse(
            requestCorrelationId = requestCorrelationId,
            response = response
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToSignInIntrospectApiResponse",
            rawApiResponse.correlationId,
            "rawApiResponse = ",
            rawApiResponse
        )

        val result = rawApiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToSignInIntrospectApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region /oauth/v2.0/challenge
    fun performSignInDefaultChallenge(
        continuationToken: String,
        correlationId: String
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performSignInDefaultChallenge(continuationToken: String)"
        )

        val request = nativeAuthRequestProvider.createSignInDefaultChallengeRequest(
            continuationToken = continuationToken,
            correlationId = correlationId
        )

        Logger.infoWithObject(
            "${TAG}.performSignInDefaultChallenge",
            correlationId,
            "request = ",
            request
        )

        return performSignInChallenge(
            requestCorrelationId = correlationId,
            request = request
        )
    }

    fun performSignInSelectedChallenge(
        continuationToken: String,
        challengeId: String,
        correlationId: String
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performSignInSelectedChallenge(continuationToken: String, challengeId: String)"
        )

        val request = nativeAuthRequestProvider.createSignInSelectedChallengeRequest(
            continuationToken = continuationToken,
            challengeId = challengeId,
            correlationId = correlationId
        )

        Logger.infoWithObject(
            "${TAG}.performSignInSelectedChallenge",
            correlationId,
            "request = ",
            request
        )

        return performSignInChallenge(
            requestCorrelationId = correlationId,
            request = request
        )
    }

    private fun performSignInChallenge(
        requestCorrelationId: String,
        request: SignInChallengeRequest
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.performSignInChallenge"
        )
        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignInChallengeResultFromHttpResponse(
            requestCorrelationId = requestCorrelationId,
            response = response
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToSignInChallengeApiResponse",
            rawApiResponse.correlationId,
            "rawApiResponse = ",
            rawApiResponse
        )

        val result = rawApiResponse.toResult()

        Logger.infoWithObject(
            "${TAG}.rawResponseToSignInChallengeApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region /oauth/v2.0/token
    fun performOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performOOBTokenRequest(parameters: SignInSubmitCodeCommandParameters)"
        )

        val request = nativeAuthRequestProvider.createOOBTokenRequest(
            commandParameters = parameters
        )

        Logger.infoWithObject(
            "${TAG}.performOOBTokenRequest",
            parameters.getCorrelationId(),
            "request = ",
            request
        )

        return performGetToken(
            requestCorrelationId = parameters.getCorrelationId(),
            request = request
        )
    }

    fun performContinuationTokenTokenRequest(
        parameters: SignInWithContinuationTokenCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performContinuationTokenTokenRequest(parameters: SignInWithContinuationTokenCommandParameters)"
        )
        val request = nativeAuthRequestProvider.createContinuationTokenTokenRequest(
            commandParameters = parameters
        )

        Logger.infoWithObject(
            "${TAG}.performContinuationTokenTokenRequest",
            parameters.getCorrelationId(),
            "request = ",
            request
        )

        return performGetToken(
            requestCorrelationId = parameters.getCorrelationId(),
            request = request
        )
    }

    fun performPasswordTokenRequest(
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performPasswordTokenRequest"
        )

        val request = nativeAuthRequestProvider.createPasswordTokenRequest(
            commandParameters = parameters
        )

        Logger.infoWithObject(
            "${TAG}.performPasswordTokenRequest",
            parameters.getCorrelationId(),
            "request = ",
            request
        )

        try {
            return performGetToken(
                requestCorrelationId = parameters.getCorrelationId(),
                request = request
            );
        } finally {
            StringUtil.overwriteWithNull(request.parameters.password)
        }
    }

    private fun performGetToken(
        requestCorrelationId: String,
        request: SignInTokenRequest
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = requestCorrelationId,
            methodName = "${TAG}.performGetToken"
        )

        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )

        val result = nativeAuthResponseHandler.getSignInTokenApiResultFromHttpResponse(
            requestCorrelationId = requestCorrelationId,
            response = response
        )

        Logger.infoWithObject(
            "${TAG}.rawResponseToSignInTokenApiResult",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion
}
