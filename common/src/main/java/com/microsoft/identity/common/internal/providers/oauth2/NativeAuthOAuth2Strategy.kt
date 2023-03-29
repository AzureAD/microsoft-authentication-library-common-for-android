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

package com.microsoft.identity.common.internal.providers.oauth2

import com.microsoft.identity.common.internal.commands.parameters.SignUpChallengeCommandParameters
import com.microsoft.identity.common.internal.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.BaseNativeAuthOAuth2Strategy
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.interactors.SignUpInteractor
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpChallengeResult
import com.microsoft.identity.common.internal.providers.oauth2.nativeauth.responses.signup.SignUpStartResult
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.ClientInfo
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAccessToken
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAccount
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsRefreshToken
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.oauth2.IDToken
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.java.providers.oauth2.TokenResult

/**
 * The implementation of native authentication API OAuth2 client.
 */
class NativeAuthOAuth2Strategy(
    private val strategyParameters: OAuth2StrategyParameters,
    private val config: NativeAuthOAuth2Configuration,
    private val signUpInteractor: SignUpInteractor
) :
    BaseNativeAuthOAuth2Strategy<MicrosoftStsAccessToken,
        MicrosoftStsAccount,
        NativeAuthOAuth2Configuration,
        MicrosoftStsRefreshToken,
        TokenResult>(config, strategyParameters) {
    private val TAG = NativeAuthOAuth2Strategy::class.java.simpleName

    // TODO fix after mock APIs
    fun getIssuerCacheIdentifierFromAuthority(): String {
        return "login.windows.net"
    }

    // TODO unit tests & compare with getAuthorityFromTokenEndpoint()
    fun getAuthority(): String {
        return config.getAuthorityUrl().toString()
    }

    fun performSignUpStart(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartResult {
        return signUpInteractor.performSignUpStart(commandParameters)
    }

    fun performSignUpChallenge(
        signUpToken: String,
        commandParameters: SignUpChallengeCommandParameters
    ): SignUpChallengeResult {
        return signUpInteractor.performSignUpChallenge(
            signUpToken = signUpToken,
            commandParameters = commandParameters
        )
    }

    override fun createAccount(response: MicrosoftStsTokenResponse): MicrosoftStsAccount {
        val methodName = ":createAccount"
        Logger.verbose(
            TAG + methodName,
            "Creating account from TokenResponse..."
        )
        lateinit var idToken: IDToken
        lateinit var clientInfo: ClientInfo

        try {
            idToken = IDToken(response.idToken)
            clientInfo = ClientInfo(response.clientInfo)
        } catch (ccse: ServiceException) {
            Logger.error(
                TAG + methodName,
                "Failed to construct IDToken or ClientInfo",
                null
            )
            Logger.errorPII(
                TAG + methodName,
                "Failed with Exception",
                ccse
            )
            throw RuntimeException()
        }

        val account = MicrosoftStsAccount(idToken, clientInfo)

        account.environment = getIssuerCacheIdentifierFromAuthority()

        return account
    }
}
