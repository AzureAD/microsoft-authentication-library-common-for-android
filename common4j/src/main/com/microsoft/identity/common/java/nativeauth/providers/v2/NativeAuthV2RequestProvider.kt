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
import com.microsoft.identity.common.java.logging.LibraryInfoHelper
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.AuthorizeChallengeContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.AuthorizeChallengeStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2ChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2PollRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2TokenRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2VerifyRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.platform.Device
import java.net.URL
import java.util.TreeMap

/**
 * Creates request objects that encapsulate all information required for making Native Auth V2 REST
 * API calls.
 */
class NativeAuthV2RequestProvider(
    private val config: NativeAuthOAuth2Configuration,
    private val hrefResolver: NativeAuthV2HrefResolver = NativeAuthV2HrefResolver(config)
) {

    /**
     * Creates the request object for the first call of a Native Auth V2 flow to
     * `/oauth2/v2.0/authorize-challenge`.
     */
    fun createAuthorizeChallengeStartRequest(correlationId: String): AuthorizeChallengeStartRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "$TAG.createAuthorizeChallengeStartRequest"
        )

        return AuthorizeChallengeStartRequest.create(
            clientId = config.clientId,
            requestUrl = config.getNativeAuthV2AuthorizeChallengeEndpoint(correlationId).toString(),
            headers = getV2RequestHeaders(correlationId, FORM_URL_ENCODED_CONTENT_TYPE)
        )
    }

    /**
     * Creates the request object for a subsequent call of a Native Auth V2 flow to
     * `/oauth2/v2.0/authorize-challenge`.
     */
    fun createAuthorizeChallengeContinueRequest(state: NativeAuthV2ContinuationState): AuthorizeChallengeContinueRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createAuthorizeChallengeContinueRequest"
        )

        return AuthorizeChallengeContinueRequest.create(
            continuationToken = state.continuationToken,
            requestUrl = config.getNativeAuthV2AuthorizeChallengeEndpoint(state.correlationId).toString(),
            headers = getV2RequestHeaders(state.correlationId, FORM_URL_ENCODED_CONTENT_TYPE)
        )
    }

    /**
     * Creates the request object for a flow's `challenge` call.
     */
    fun createChallengeRequest(state: NativeAuthV2ContinuationState): NativeAuthV2ChallengeRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createChallengeRequest"
        )

        return createChallengeRequest(state, NativeAuthV2LinkRelation.CHALLENGE)
    }

    /**
     * Creates the request object for a flow's `resend` call.
     */
    fun createResendRequest(state: NativeAuthV2ContinuationState): NativeAuthV2ChallengeRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createResendRequest"
        )

        return createChallengeRequest(state, NativeAuthV2LinkRelation.RESEND)
    }

    /**
     * Creates the request object for a flow's `verify` call.
     */
    fun createVerifyRequest(state: NativeAuthV2ContinuationState, otp: String): NativeAuthV2VerifyRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createVerifyRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.VERIFY)
        return NativeAuthV2VerifyRequest.create(
            continuationToken = state.continuationToken,
            otp = otp,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, JSON_CONTENT_TYPE)
        )
    }

    /**
     * Creates the request object for a flow's `poll` call.
     */
    fun createPollRequest(state: NativeAuthV2ContinuationState): NativeAuthV2PollRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createPollRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.POLL)
        return NativeAuthV2PollRequest.create(
            continuationToken = state.continuationToken,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, JSON_CONTENT_TYPE)
        )
    }

    /**
     * Creates the request object for a flow's token exchange.
     */
    fun createTokenRequest(
        code: String,
        scopes: List<String>,
        correlationId: String,
        claimsRequestJson: String? = null
    ): NativeAuthV2TokenRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "$TAG.createTokenRequest"
        )

        return NativeAuthV2TokenRequest.create(
            clientId = config.clientId,
            code = code,
            scopes = scopes,
            claimsRequestJson = claimsRequestJson,
            requestUrl = config.getNativeAuthV2TokenEndpoint(correlationId).toString(),
            headers = getV2RequestHeaders(correlationId, FORM_URL_ENCODED_CONTENT_TYPE)
        )
    }

    /**
     * Shared implementation for [createChallengeRequest] and [createResendRequest].
     */
    private fun createChallengeRequest(
        state: NativeAuthV2ContinuationState,
        relation: NativeAuthV2LinkRelation
    ): NativeAuthV2ChallengeRequest {
        val requestUrl = resolveHref(state, relation)
        return NativeAuthV2ChallengeRequest.create(
            continuationToken = state.continuationToken,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, JSON_CONTENT_TYPE)
        )
    }

    /**
     * Selects [relation] from [state] and resolves it to an absolute URL.
     */
    private fun resolveHref(state: NativeAuthV2ContinuationState, relation: NativeAuthV2LinkRelation): URL {
        val href = state.href(relation) ?: throw missingRelationException(relation, state.correlationId)
        return hrefResolver.resolve(href, state.correlationId)
    }

    private fun missingRelationException(relation: NativeAuthV2LinkRelation, correlationId: String): ClientException {
        val exception = ClientException(
            ClientException.MISSING_PARAMETER,
            "Native Auth V2 continuation state is missing the required '${relation.value}' relation."
        )
        exception.setCorrelationId(correlationId)
        return exception
    }

    /**
     * Builds the standard SDK headers for a Native Auth V2 request.
     */
    private fun getV2RequestHeaders(correlationId: String, contentType: String): Map<String, String?> {
        val headers: MutableMap<String, String?> = TreeMap()
        if (correlationId != UNSET_CORRELATION_ID) {
            headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID] = correlationId
        }
        headers[AuthenticationConstants.SdkPlatformFields.PRODUCT] = LibraryInfoHelper.getLibraryName()
        headers[AuthenticationConstants.SdkPlatformFields.VERSION] = LibraryInfoHelper.getLibraryVersion()
        headers.putAll(Device.getPlatformIdParameters())
        headers[HttpConstants.HeaderField.CONTENT_TYPE] = contentType
        return headers
    }

    private companion object {
        private val TAG: String = NativeAuthV2RequestProvider::class.java.simpleName
        private const val UNSET_CORRELATION_ID = "UNSET"
        private const val JSON_CONTENT_TYPE = HttpConstants.MediaType.APPLICATION_JSON
        private const val FORM_URL_ENCODED_CONTENT_TYPE = "application/x-www-form-urlencoded"
    }
}
