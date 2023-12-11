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
package com.microsoft.identity.common.nativeauth.providers.interactors

import com.microsoft.identity.common.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignInWithSLTCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.nativeauth.providers.NativeAuthRequestProvider
import com.microsoft.identity.common.nativeauth.providers.NativeAuthResponseHandler
import com.microsoft.identity.common.nativeauth.providers.requests.signin.SignInChallengeRequest
import com.microsoft.identity.common.nativeauth.providers.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.nativeauth.providers.requests.signin.SignInTokenRequest
import com.microsoft.identity.common.nativeauth.providers.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.nativeauth.providers.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.nativeauth.providers.responses.signin.SignInTokenApiResult
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
        LogSession.logMethodCall(TAG, "${TAG}.performSignInInitiate(parameters: SignInStartCommandParameters)")
        val request = nativeAuthRequestProvider.createSignInInitiateRequest(
            parameters = parameters
        )
        return performSignInInitiate(request)
    }

    private fun performSignInInitiate(request: SignInInitiateRequest): SignInInitiateApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignInInitiate")
        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val rawApiResponse = nativeAuthResponseHandler.getSignInInitiateResultFromHttpResponse(
            response = response
        )
        return rawApiResponse.toResult()
    }
    //endregion

    //region /oauth/v2.0/challenge
    fun performSignInChallenge(
        credentialToken: String,
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignInChallenge(credentialToken: String)")
        val request = nativeAuthRequestProvider.createSignInChallengeRequest(
            credentialToken = credentialToken
        )
        return performSignInChallenge(request)
    }

    private fun performSignInChallenge(request: SignInChallengeRequest): SignInChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignInChallenge")
        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        val result = nativeAuthResponseHandler.getSignInChallengeResultFromHttpResponse(
            response = response
        )
        return result.toResult()
    }
    //endregion

    //region /oauth/v2.0/token
    fun performOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performOOBTokenRequest")
        val request = nativeAuthRequestProvider.createOOBTokenRequest(
            parameters = parameters
        )
        return performGetToken(request)
    }

    fun performSLTTokenRequest(
        parameters: SignInWithSLTCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSLTTokenRequest")
        val request = nativeAuthRequestProvider.createSLTTokenRequest(
            parameters = parameters
        )
        return performGetToken(request)
    }

    fun performPasswordTokenRequest(
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performPasswordTokenRequest")
        val request = nativeAuthRequestProvider.createPasswordTokenRequest(
            parameters = parameters
        )
        try {
            return performGetToken(request);
        } finally {
            StringUtil.overwriteWithNull(request.parameters.password)
        }
    }

    private fun performGetToken(request: SignInTokenRequest): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performGetToken")
        val encodedRequest: String = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        val headers = request.headers
        val requestUrl = request.requestUrl

        val response = httpClient.post(
            requestUrl,
            headers,
            encodedRequest.toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        )
        return nativeAuthResponseHandler.getSignInTokenApiResultFromHttpResponse(
            response = response
        )
    }
    //endregion
}
