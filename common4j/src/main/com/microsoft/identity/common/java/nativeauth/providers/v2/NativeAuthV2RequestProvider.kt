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

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthContentType
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthSdkHeaders
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.AuthorizeChallengeContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.AuthorizeChallengeStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2ChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2EntryRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2PasswordVerifyRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2PollRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2SignUpStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2SubmitAttributesRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2TokenRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2UpdatePasswordRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.v2.NativeAuthV2VerifyRequest
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.net.HttpConstants
import java.net.URL

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
            headers = getV2RequestHeaders(correlationId, NativeAuthContentType.FORM_URL_ENCODED)
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
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.FORM_URL_ENCODED)
        )
    }

    /**
     * Creates the request object for the reset-password flow's entry (`/start`-equivalent) call,
     * resolved via the [NativeAuthV2LinkRelation.RESET_PASSWORD] relation on [state].
     */
    fun createResetPasswordStartRequest(username: String, state: NativeAuthV2ContinuationState): NativeAuthV2EntryRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createResetPasswordStartRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.RESET_PASSWORD)
        return NativeAuthV2EntryRequest.create(
            clientId = config.clientId,
            username = username,
            continuationToken = state.continuationToken,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
        )
    }

    /**
     * Creates the request object for the sign-up flow's entry (`signup/start`) call, resolved via
     * the [NativeAuthV2LinkRelation.SIGN_UP] relation on [state]. Unlike sign-in, the body carries
     * only `continuationToken`; the username is supplied later as an attribute.
     */
    fun createSignUpStartRequest(state: NativeAuthV2ContinuationState): NativeAuthV2SignUpStartRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createSignUpStartRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.SIGN_UP)
        return NativeAuthV2SignUpStartRequest.create(
            continuationToken = state.continuationToken,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
        )
    }

    /**
     * Creates the request object for the sign-up flow's `submitattributes` call, resolved via the
     * [NativeAuthV2LinkRelation.SUBMIT_ATTRIBUTES] relation on [state]. The body carries
     * `continuationToken` and the [attributes] map of attribute name to value.
     */
    fun createSubmitAttributesRequest(
        state: NativeAuthV2ContinuationState,
        attributes: Map<String, String>
    ): NativeAuthV2SubmitAttributesRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createSubmitAttributesRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.SUBMIT_ATTRIBUTES)
        return NativeAuthV2SubmitAttributesRequest.create(
            continuationToken = state.continuationToken,
            attributes = attributes,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
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
     * Creates the request object for the sign-in flow's entry (`signin/start`) call, resolved via
     * the [NativeAuthV2LinkRelation.SIGN_IN] relation on [state]. The body carries `username` and
     * `continuationToken`.
     */
    fun createSignInStartRequest(username: String, state: NativeAuthV2ContinuationState): NativeAuthV2EntryRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createSignInStartRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.SIGN_IN)
        return NativeAuthV2EntryRequest.create(
            clientId = config.clientId,
            username = username,
            continuationToken = state.continuationToken,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
        )
    }

    /**
     * Creates the request object for a flow's password `verify` call, resolved via the
     * [NativeAuthV2LinkRelation.VERIFY] relation the password-method challenge attached to [state].
     * [password] is passed through as the caller's own array, not copied, so the interactor's
     * `finally` block can clear the same buffer it passed in.
     */
    fun createPasswordVerifyRequest(
        state: NativeAuthV2ContinuationState,
        password: CharArray
    ): NativeAuthV2PasswordVerifyRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createPasswordVerifyRequest"
        )

        val requestUrl = resolveHref(state, NativeAuthV2LinkRelation.VERIFY)
        return NativeAuthV2PasswordVerifyRequest.create(
            continuationToken = state.continuationToken,
            password = password,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
        )
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
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
        )
    }

    /**
     * Creates the request object for a flow's `update` call, resolved via the
     * [NativeAuthV2LinkRelation.UPDATE] relation on [state]. [newPassword] is passed through as the
     * caller's own array, not copied, so the interactor's `finally` block can clear the same buffer
     * it passed in.
     */
    fun createUpdatePasswordRequest(state: NativeAuthV2ContinuationState, newPassword: CharArray): NativeAuthV2UpdatePasswordRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.createUpdatePasswordRequest"
        )

        val requestUrl = state.href(NativeAuthV2LinkRelation.UPDATE)?.let {
            hrefResolver.resolve(it, state.correlationId)
        } ?: throw missingRelationException(NativeAuthV2LinkRelation.UPDATE, state.correlationId)
        return NativeAuthV2UpdatePasswordRequest.create(
            clientId = config.clientId,
            continuationToken = state.continuationToken,
            newPassword = newPassword,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
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
            continuationToken = state.continuationToken,
            requestUrl = requestUrl.toString(),
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
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
            headers = getV2RequestHeaders(correlationId, NativeAuthContentType.FORM_URL_ENCODED)
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
            headers = getV2RequestHeaders(state.correlationId, NativeAuthContentType.JSON)
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
     * Builds the headers for a Native Auth V2 request: the shared SDK identity block plus the
     * V2-specific `Content-Type`. Future V2-only headers (a HAL `Accept` value, caller-supplied
     * interceptor headers, V2 telemetry) belong here rather than in [NativeAuthSdkHeaders], so they
     * cannot affect the shipped V1 flows.
     */
    private fun getV2RequestHeaders(correlationId: String, contentType: NativeAuthContentType): Map<String, String?> {
        val headers = NativeAuthSdkHeaders.base(correlationId)
        headers[HttpConstants.HeaderField.CONTENT_TYPE] = contentType.value
        return headers
    }

    private companion object {
        private val TAG: String = NativeAuthV2RequestProvider::class.java.simpleName
    }
}
