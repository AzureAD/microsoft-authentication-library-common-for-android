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
import com.microsoft.identity.common.java.nativeauth.providers.requests.NativeAuthRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2Operation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2ResponseHandler
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2RequestInterceptor
import com.microsoft.identity.common.java.util.ObjectMapper
import com.microsoft.identity.common.java.util.StringUtil

/**
 * Acts as a binding layer between the V2 request provider, response handler, and response parser.
 * Constructs a request for each V2 Native Auth endpoint, applies interceptor headers, serialises
 * the body (JSON or form-encoded to match the content-type the request provider selected),
 * dispatches via [httpClient], hands the raw response to [responseHandler], hands the typed model
 * to [responseParser], logs the safe result, and returns it.
 *
 * Mirrors [ResetPasswordInteractor]'s constructor and method structure so the layering is
 * recognisable.
 *
 * @param httpClient Used for HTTP requests.
 * @param requestProvider Builds V2 request objects from SDK state.
 * @param responseHandler Converts raw HTTP responses into typed V2 response models.
 * @param responseParser Classifies V2 response models into SDK result types.
 * @param requestInterceptor Optional interceptor providing additional custom headers.
 */
class NativeAuthV2Interactor(
    private val httpClient: UrlConnectionHttpClient,
    private val requestProvider: NativeAuthV2RequestProvider,
    private val responseHandler: NativeAuthV2ResponseHandler,
    private val responseParser: NativeAuthV2ResponseParser,
    private val requestInterceptor: OAuth2RequestInterceptor? = null
) {
    private val TAG: String = NativeAuthV2Interactor::class.java.simpleName

    //region /oauth2/v2.0/authorize-challenge (start)
    /**
     * Starts a V2 Native Auth flow at the authorize-challenge endpoint.
     *
     * [entryRelation] and [scenario] are passed through to the parser so it can build the first
     * [NativeAuthV2ContinuationState] from the server response.
     */
    fun performAuthorizeChallengeStart(
        correlationId: String,
        entryRelation: NativeAuthV2LinkRelation,
        scenario: NativeAuthV2FlowScenario,
        scopes: List<String>,
        claimsRequestJson: String? = null
    ): AuthorizeChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "$TAG.performAuthorizeChallengeStart"
        )

        val request = requestProvider.createAuthorizeChallengeStartRequest(correlationId = correlationId)

        Logger.infoWithObject(
            "$TAG.performAuthorizeChallengeStart",
            correlationId,
            "request = ",
            request
        )

        return performAuthorizeChallenge(
            request = request,
            correlationId = correlationId,
            entryRelation = entryRelation,
            scenario = scenario,
            scopes = scopes,
            claimsRequestJson = claimsRequestJson,
            methodName = "$TAG.performAuthorizeChallengeStart"
        )
    }
    //endregion

    //region /oauth2/v2.0/authorize-challenge (continue)
    fun performAuthorizeChallengeContinue(
        state: NativeAuthV2ContinuationState
    ): AuthorizeChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.performAuthorizeChallengeContinue"
        )

        val request = requestProvider.createAuthorizeChallengeContinueRequest(state = state)

        Logger.infoWithObject(
            "$TAG.performAuthorizeChallengeContinue",
            state.correlationId,
            "request = ",
            request
        )

        return performAuthorizeChallenge(
            request = request,
            correlationId = state.correlationId,
            entryRelation = state.entryRelation,
            scenario = state.scenario,
            scopes = state.scopes,
            claimsRequestJson = state.claimsRequestJson,
            methodName = "$TAG.performAuthorizeChallengeContinue"
        )
    }

    private fun performAuthorizeChallenge(
        request: NativeAuthRequest,
        correlationId: String,
        entryRelation: NativeAuthV2LinkRelation,
        scenario: NativeAuthV2FlowScenario,
        scopes: List<String>,
        claimsRequestJson: String?,
        methodName: String
    ): AuthorizeChallengeApiResult {
        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))

        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)
        val halResponse = responseHandler.getHalApiResponse(correlationId, httpResponse)
        val result = responseParser.parseAuthorizeChallenge(
            response = halResponse,
            entryRelation = entryRelation,
            scenario = scenario,
            scopes = scopes,
            claimsRequestJson = claimsRequestJson
        )

        Logger.infoWithObject(
            methodName,
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region reset-password entry
    fun performResetPasswordStart(
        username: String,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.performResetPasswordStart"
        )

        val request = requestProvider.createResetPasswordStartRequest(username = username, state = state)

        Logger.infoWithObject(
            "$TAG.performResetPasswordStart",
            state.correlationId,
            "request = ",
            request
        )

        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToJsonString(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))

        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)
        val halResponse = responseHandler.getHalApiResponse(
            requestCorrelationId = state.correlationId,
            response = httpResponse
        )

        val result = responseParser.parseInteraction(
            response = halResponse,
            previousState = state,
            operation = NativeAuthV2Operation.RESET_PASSWORD_START
        )

        Logger.infoWithObject(
            "$TAG.performResetPasswordStart",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region challenge
    fun performChallenge(
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.performChallenge"
        )

        val request = requestProvider.createChallengeRequest(state = state)

        Logger.infoWithObject(
            "$TAG.performChallenge",
            state.correlationId,
            "request = ",
            request
        )

        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToJsonString(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))

        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)
        val halResponse = responseHandler.getHalApiResponse(
            requestCorrelationId = state.correlationId,
            response = httpResponse
        )

        val result = responseParser.parseInteraction(
            response = halResponse,
            previousState = state,
            operation = NativeAuthV2Operation.CHALLENGE
        )

        Logger.infoWithObject(
            "$TAG.performChallenge",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region resend
    fun performResend(
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.performResend"
        )

        val request = requestProvider.createResendRequest(state = state)

        Logger.infoWithObject(
            "$TAG.performResend",
            state.correlationId,
            "request = ",
            request
        )

        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToJsonString(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))

        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)
        val halResponse = responseHandler.getHalApiResponse(
            requestCorrelationId = state.correlationId,
            response = httpResponse
        )

        val result = responseParser.parseInteraction(
            response = halResponse,
            previousState = state,
            operation = NativeAuthV2Operation.RESEND
        )

        Logger.infoWithObject(
            "$TAG.performResend",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region verify
    fun performVerify(
        state: NativeAuthV2ContinuationState,
        otp: String
    ): NativeAuthV2InteractionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.performVerify"
        )

        val request = requestProvider.createVerifyRequest(state = state, otp = otp)

        Logger.infoWithObject(
            "$TAG.performVerify",
            state.correlationId,
            "request = ",
            request
        )

        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToJsonString(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))

        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)
        val halResponse = responseHandler.getHalApiResponse(
            requestCorrelationId = state.correlationId,
            response = httpResponse
        )

        val result = responseParser.parseInteraction(
            response = halResponse,
            previousState = state,
            operation = NativeAuthV2Operation.VERIFY
        )

        Logger.infoWithObject(
            "$TAG.performVerify",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region update password
    /**
     * Submits a new password via HTTP PUT. The password buffer is zeroed in a `finally` block that
     * runs even if request construction, body serialisation or the network call throws, exactly as
     * [ResetPasswordInteractor.performResetPasswordSubmit] does today.
     */
    fun performUpdatePassword(
        state: NativeAuthV2ContinuationState,
        newPassword: CharArray
    ): NativeAuthV2InteractionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.performUpdatePassword"
        )

        try {
            val request = requestProvider.createUpdatePasswordRequest(state = state, newPassword = newPassword)

            Logger.infoWithObject(
                "$TAG.performUpdatePassword",
                state.correlationId,
                "request = ",
                request
            )

            val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
            val encoded = ObjectMapper.serializeObjectToJsonString(request.parameters)
                .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))

            val httpResponse = httpClient.put(request.requestUrl, headers, encoded)
            val halResponse = responseHandler.getHalApiResponse(
                requestCorrelationId = state.correlationId,
                response = httpResponse
            )

            val result = responseParser.parseInteraction(
                response = halResponse,
                previousState = state,
                operation = NativeAuthV2Operation.UPDATE_PASSWORD
            )

            Logger.infoWithObject(
                "$TAG.performUpdatePassword",
                result.correlationId,
                "result = ",
                result
            )

            return result
        } finally {
            StringUtil.overwriteWithNull(newPassword)
        }
    }
    //endregion

    //region poll
    fun performPoll(
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.performPoll"
        )

        val request = requestProvider.createPollRequest(state = state)

        Logger.infoWithObject(
            "$TAG.performPoll",
            state.correlationId,
            "request = ",
            request
        )

        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToJsonString(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))

        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)
        val halResponse = responseHandler.getHalApiResponse(
            requestCorrelationId = state.correlationId,
            response = httpResponse
        )

        val result = responseParser.parseInteraction(
            response = halResponse,
            previousState = state,
            operation = NativeAuthV2Operation.POLL
        )

        Logger.infoWithObject(
            "$TAG.performPoll",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion

    //region token
    fun performTokenRequest(
        code: String,
        scopes: List<String>,
        correlationId: String,
        claimsRequestJson: String? = null
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "$TAG.performTokenRequest"
        )

        val request = requestProvider.createTokenRequest(
            code = code,
            scopes = scopes,
            correlationId = correlationId,
            claimsRequestJson = claimsRequestJson
        )

        Logger.infoWithObject(
            "$TAG.performTokenRequest",
            correlationId,
            "request = ",
            request
        )

        val headers = applyInterceptorHeaders(request.requestUrl, request.headers, requestInterceptor)
        val encoded = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
            .toByteArray(charset(ObjectMapper.ENCODING_SCHEME))

        val httpResponse = httpClient.post(request.requestUrl, headers, encoded)

        val result = responseHandler.getTokenApiResponse(
            requestCorrelationId = correlationId,
            response = httpResponse
        )

        Logger.infoWithObject(
            "$TAG.performTokenRequest",
            result.correlationId,
            "result = ",
            result
        )

        return result
    }
    //endregion
}
