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

package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.nativeauth.providers.interactors.NativeAuthV2Interactor
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters

/**
 * Native Authentication V2 OAuth2 strategy.
 */
class NativeAuthV2OAuth2Strategy(
    strategyParameters: OAuth2StrategyParameters,
    val config: NativeAuthOAuth2Configuration,
    private val nativeAuthV2Interactor: NativeAuthV2Interactor,
) : MicrosoftStsOAuth2Strategy(config, strategyParameters) {

    /**
     * Returns the issuer cache identifier. For mock APIs, a static cache identifier is used.
     */
    override fun getIssuerCacheIdentifierFromTokenEndpoint(): String {
        return if (config.useMockApiForNativeAuth) {
            CACHE_IDENTIFIER_MOCK
        } else {
            super.getIssuerCacheIdentifierFromTokenEndpoint()
        }
    }

    /**
     * Returns the configured authority URL.
     */
    fun getAuthority(): String = config.authorityUrl.toString()

    /**
     * Starts a V2 Native Auth authorize-challenge flow.
     */
    fun performAuthorizeChallengeStart(
        correlationId: String,
        entryRelation: String,
        scenario: NativeAuthV2FlowScenario,
        scopes: List<String>
    ): AuthorizeChallengeApiResult = performAuthorizeChallengeStart(
        correlationId = correlationId,
        entryRelation = entryRelation,
        scenario = scenario,
        scopes = scopes,
        claimsRequestJson = null
    )

    /**
     * Starts a V2 Native Auth authorize-challenge flow.
     *
     * [entryRelation] is accepted as a raw relation string at this public boundary for Java
     * interoperability, then wrapped immediately into [NativeAuthV2LinkRelation] for internal
     * type safety. [scopes] are retained only for the later authorization-code token exchange;
     * they are not sent on the authorize-challenge request itself.
     */
    fun performAuthorizeChallengeStart(
        correlationId: String,
        entryRelation: String,
        scenario: NativeAuthV2FlowScenario,
        scopes: List<String>,
        claimsRequestJson: String?
    ): AuthorizeChallengeApiResult {
        return nativeAuthV2Interactor.performAuthorizeChallengeStart(
            correlationId = correlationId,
            entryRelation = NativeAuthV2LinkRelation(entryRelation),
            scenario = scenario,
            scopes = scopes,
            claimsRequestJson = claimsRequestJson
        )
    }

    /**
     * Continues a V2 Native Auth authorize-challenge flow.
     */
    fun performAuthorizeChallengeContinue(
        state: NativeAuthV2ContinuationState
    ): AuthorizeChallengeApiResult {
        return nativeAuthV2Interactor.performAuthorizeChallengeContinue(state = state)
    }

    /**
     * Starts the V2 Native Auth reset-password flow.
     */
    fun performResetPasswordStart(
        username: String,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        return nativeAuthV2Interactor.performResetPasswordStart(
            username = username,
            state = state
        )
    }

    /**
     * Performs a V2 Native Auth challenge request.
     */
    fun performChallenge(
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        return nativeAuthV2Interactor.performChallenge(state = state)
    }

    /**
     * Performs a V2 Native Auth resend request.
     */
    fun performResend(
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        return nativeAuthV2Interactor.performResend(state = state)
    }

    /**
     * Performs a V2 Native Auth verify request.
     */
    fun performVerify(
        state: NativeAuthV2ContinuationState,
        otp: String
    ): NativeAuthV2InteractionApiResult {
        return nativeAuthV2Interactor.performVerify(
            state = state,
            otp = otp
        )
    }

    /**
     * Performs a V2 Native Auth update-password request.
     */
    fun performUpdatePassword(
        state: NativeAuthV2ContinuationState,
        newPassword: CharArray
    ): NativeAuthV2InteractionApiResult {
        return nativeAuthV2Interactor.performUpdatePassword(
            state = state,
            newPassword = newPassword
        )
    }

    /**
     * Performs a V2 Native Auth poll request.
     */
    fun performPoll(
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        return nativeAuthV2Interactor.performPoll(state = state)
    }

    /**
     * Exchanges a V2 Native Auth authorization code for tokens.
     */
    fun performTokenRequest(
        code: String,
        scopes: List<String>,
        correlationId: String,
        claimsRequestJson: String? = null
    ): SignInTokenApiResult {
        return nativeAuthV2Interactor.performTokenRequest(
            code = code,
            scopes = scopes,
            correlationId = correlationId,
            claimsRequestJson = claimsRequestJson
        )
    }

    private companion object {
        private const val CACHE_IDENTIFIER_MOCK = "login.windows.net"
    }
}
