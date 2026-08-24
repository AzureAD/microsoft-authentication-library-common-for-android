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
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
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
        scopes: List<String>
    ): AuthorizeChallengeApiResult = performAuthorizeChallengeStart(
        correlationId = correlationId,
        entryRelation = entryRelation,
        scopes = scopes,
        claimsRequestJson = null
    )

    /**
     * Starts a V2 Native Auth authorize-challenge flow.
     */
    fun performAuthorizeChallengeStart(
        correlationId: String,
        entryRelation: String,
        scopes: List<String>,
        claimsRequestJson: String?
    ): AuthorizeChallengeApiResult {
        return nativeAuthV2Interactor.performAuthorizeChallengeStart(
            correlationId = correlationId,
            entryRelation = NativeAuthV2LinkRelation(entryRelation),
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

    private companion object {
        private const val CACHE_IDENTIFIER_MOCK = "login.windows.net"
    }
}
