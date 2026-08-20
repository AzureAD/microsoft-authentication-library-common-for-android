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
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2ResponseHandler
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2RequestInterceptor
import com.microsoft.identity.common.java.util.ObjectMapper

/**
 * Executes flow-agnostic Native Auth V2 authorize-challenge and token requests.
 */
class NativeAuthV2Interactor(
    private val httpClient: UrlConnectionHttpClient,
    private val requestProvider: NativeAuthV2RequestProvider,
    private val responseHandler: NativeAuthV2ResponseHandler,
    private val responseParser: NativeAuthV2ResponseParser,
    private val requestInterceptor: OAuth2RequestInterceptor? = null
) {
    private val TAG: String = NativeAuthV2Interactor::class.java.simpleName

    /**
     * Starts a V2 Native Auth flow at the authorize-challenge endpoint.
     */
    fun performAuthorizeChallengeStart(
        correlationId: String,
        entryRelation: NativeAuthV2LinkRelation,
        scopes: List<String>,
        claimsRequestJson: String? = null
    ): AuthorizeChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "$TAG.performAuthorizeChallengeStart"
        )

        val request = requestProvider.createAuthorizeChallengeStartRequest(correlationId)
        Logger.infoWithObject(
            "$TAG.performAuthorizeChallengeStart",
            correlationId,
            "request = ",
            request
        )

        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)
        val halResponse = responseHandler.getHalApiResponse(correlationId, httpResponse)
        val result = responseParser.parseAuthorizeChallenge(
            response = halResponse,
            entryRelation = entryRelation,
            scopes = scopes,
            claimsRequestJson = claimsRequestJson
        )

        Logger.infoWithObject(
            "$TAG.performAuthorizeChallengeStart",
            result.correlationId,
            "result = ",
            result
        )
        return result
    }

    /**
     * Continues a V2 Native Auth authorize-challenge exchange.
     */
    fun performAuthorizeChallengeContinue(
        state: NativeAuthV2ContinuationState
    ): AuthorizeChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.performAuthorizeChallengeContinue"
        )

        val request = requestProvider.createAuthorizeChallengeContinueRequest(state)
        Logger.infoWithObject(
            "$TAG.performAuthorizeChallengeContinue",
            state.correlationId,
            "request = ",
            request
        )

        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))
        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)
        val halResponse = responseHandler.getHalApiResponse(state.correlationId, httpResponse)
        val result = responseParser.parseAuthorizeChallenge(
            response = halResponse,
            entryRelation = state.entryRelation,
            scopes = state.scopes,
            claimsRequestJson = state.claimsRequestJson
        )

        Logger.infoWithObject(
            "$TAG.performAuthorizeChallengeContinue",
            result.correlationId,
            "result = ",
            result
        )
        return result
    }

}
