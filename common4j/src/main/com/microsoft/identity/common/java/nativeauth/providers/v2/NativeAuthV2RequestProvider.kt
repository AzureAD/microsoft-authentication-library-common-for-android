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
 *
 * The surface is verbs, not endpoints: mid-flow methods take an opaque
 * [NativeAuthV2ContinuationState] and this provider is the only place that selects the required
 * `_links` relation and resolves its href (via [hrefResolver]) immediately before attaching the
 * continuation token to the outgoing request. Callers — including the controller — never receive
 * a raw href or continuation token as a separate argument.
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
            challengeType = config.challengeType,
            requestUrl = config.getAuthorizeChallengeEndpoint().toString(),
            headers = getV2RequestHeaders(correlationId, FORM_URL_ENCODED_CONTENT_TYPE)
        )
    }

    /**
     * Creates the request object for a subsequent call of a Native Auth V2 flow to
     * `/oauth2/v2.0/authorize-challenge`, continuing with [state]'s continuation token.
     */
    fun createAuthorizeChallengeContinueRequest(state: NativeAuthV2ContinuationState): AuthorizeChallengeContinueRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createAuthorizeChallengeContinueRequest"
        )

        return AuthorizeChallengeContinueRequest.create(
            clientId = config.clientId,
            continuationToken = state.continuationToken,
            requestUrl = config.getAuthorizeChallengeEndpoint().toString(),
            headers = getV2RequestHeaders(state.correlationId, FORM_URL_ENCODED_CONTENT_TYPE)
        )
    }

    /**
     * Creates the request object for a flow's `challenge` call, resolved via the
     * [NativeAuthV2LinkRelation.CHALLENGE] relation on [state].
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
     * Creates the request object for a flow's `resend` call, resolved via the
     * [NativeAuthV2LinkRelation.RESEND] relation on [state]. This reuses [NativeAuthV2ChallengeRequest]
     * because `resend` shares the same body shape and content type as `challenge`.
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
     * Creates the request object for a flow's `verify` call, resolved via the
     * [NativeAuthV2LinkRelation.VERIFY] relation on [state].
     */
    fun createVerifyRequest(state: NativeAuthV2ContinuationState, otp: String): NativeAuthV2VerifyRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createVerifyRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.VERIFY)
        return NativeAuthV2VerifyRequest.create(
            clientId = config.clientId,
            continuationToken = state.continuationToken,
            otp = otp,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, JSON_CONTENT_TYPE)
        )
    }

    /**
     * Creates the request object for a flow's `poll` call, resolved via the
     * [NativeAuthV2LinkRelation.POLL] relation on [state].
     */
    fun createPollRequest(state: NativeAuthV2ContinuationState): NativeAuthV2PollRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createPollRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.POLL)
        return NativeAuthV2PollRequest.create(
            clientId = config.clientId,
            continuationToken = state.continuationToken,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, JSON_CONTENT_TYPE)
        )
    }

    /**
     * Creates the request object exchanging a Native Auth V2 authorization [code] for tokens at
     * the existing `/oauth2/v2.0/token` endpoint.
     */
    fun createTokenRequest(code: String, scopes: List<String>, correlationId: String): NativeAuthV2TokenRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "$TAG.createTokenRequest"
        )

        return NativeAuthV2TokenRequest.create(
            clientId = config.clientId,
            code = code,
            scopes = scopes,
            requestUrl = config.getSignInTokenEndpoint().toString(),
            headers = getV2RequestHeaders(correlationId, FORM_URL_ENCODED_CONTENT_TYPE)
        )
    }

    /**
     * Shared implementation for [createChallengeRequest] and [createResendRequest]: both resolve a
     * relation on [state] and build the same request shape, differing only in which relation is
     * required.
     */
    private fun createChallengeRequest(
        state: NativeAuthV2ContinuationState,
        relation: NativeAuthV2LinkRelation
    ): NativeAuthV2ChallengeRequest {
        val requestUrl = resolveHref(state, relation)
        return NativeAuthV2ChallengeRequest.create(
            clientId = config.clientId,
            continuationToken = state.continuationToken,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, JSON_CONTENT_TYPE)
        )
    }

    /**
     * Selects [relation] from [state] and resolves it to an absolute URL via [hrefResolver],
     * immediately before the caller attaches the continuation token to the outgoing request.
     *
     * @throws ClientException with [ClientException.MISSING_PARAMETER] if [state] does not carry
     * an href for [relation].
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
     * Builds the standard SDK headers for a Native Auth V2 request, mirroring
     * [com.microsoft.identity.common.java.nativeauth.providers.NativeAuthRequestProvider]'s header
     * construction but with a caller-supplied [contentType] rather than a single fixed value,
     * since V2 requests are a mix of JSON and form-encoded bodies.
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
