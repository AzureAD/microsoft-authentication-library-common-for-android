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

package com.microsoft.identity.common.java.providers.nativeauth

import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartWithPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpContinueCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignInInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignUpInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SsprInteractor
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.challenge.SignUpChallengeResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.cont.SignUpContinueResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.start.SignUpStartResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprSubmitApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters

/**
 * The implementation of native authentication API OAuth2 client.
 */
class NativeAuthOAuth2Strategy(
    private val strategyParameters: OAuth2StrategyParameters,
    private val config: NativeAuthOAuth2Configuration,
    private val signInInteractor: SignInInteractor,
    private val signUpInteractor: SignUpInteractor,
    private val ssprInteractor: SsprInteractor
) :
    MicrosoftStsOAuth2Strategy(config, strategyParameters) {
    private val TAG = NativeAuthOAuth2Strategy::class.java.simpleName

    // Hardcoding so that the environment parameter from the mock API token response matches
    // with the environment retrieved from the (authority) endpoints.
    // TODO fix after mock APIs
    override fun getIssuerCacheIdentifierFromTokenEndpoint(): String {
        return "login.windows.net"
    }

    // TODO unit tests & compare with getAuthorityFromTokenEndpoint()
    fun getAuthority(): String {
        return config.getAuthorityUrl().toString()
    }

    fun performSignUpStart(
        parameters: SignUpStartCommandParameters
    ): SignUpStartResult {
        return signUpInteractor.performSignUpStart(parameters)
    }

    fun performSignUpChallenge(
        signUpToken: String
    ): SignUpChallengeResult {
        return signUpInteractor.performSignUpChallenge(
            signUpToken = signUpToken
        )
    }

    fun performSignUpContinue(
        signUpToken: String,
        commandParameters: SignUpContinueCommandParameters
    ): SignUpContinueResult {
        return signUpInteractor.performSignUpContinue(
            signUpToken = signUpToken,
            commandParameters = commandParameters
        )
    }

    fun performSignInInitiate(
        parameters: SignInStartCommandParameters
    ): SignInInitiateApiResult {
        return signInInteractor.performSignInInitiate(parameters)
    }

    fun performSignInChallenge(
        credentialToken: String,
    ): SignInChallengeApiResult {
        return signInInteractor.performSignInChallenge(
            credentialToken = credentialToken,
        )
    }

    fun performROPCTokenRequest(
        parameters: SignInStartWithPasswordCommandParameters
    ): SignInTokenApiResult {
        return signInInteractor.performROPCTokenRequest(
            parameters = parameters
        )
    }

    fun performOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        return signInInteractor.performOOBTokenRequest(
            parameters = parameters
        )
    }

    fun performPasswordTokenRequest(
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        return signInInteractor.performPasswordTokenRequest(
            parameters = parameters
        )
    }

    fun performSsprStart(
        parameters: SsprStartCommandParameters
    ): SsprStartApiResult {
        return ssprInteractor.performSsprStart(
            parameters = parameters
        )
    }

    fun performSsprChallenge(
        passwordResetToken: String
    ): SsprChallengeApiResult {
        return ssprInteractor.performSsprChallenge(
            passwordResetToken = passwordResetToken
        )
    }

    fun performSsprContinue(
        parameters: SsprSubmitCodeCommandParameters
    ): SsprContinueApiResult {
        return ssprInteractor.performSsprContinue(
            parameters = parameters
        )
    }

    fun performSsprSubmit(
        parameters: SsprSubmitNewPasswordCommandParameters
    ): SsprSubmitApiResult {
        return ssprInteractor.performSsprSubmit(
            commandParameters = parameters
        )
    }

    fun performSsprPollCompletion(
        passwordResetToken: String
    ): SsprPollCompletionApiResult {
        return ssprInteractor.performSsprPollCompletion(
            passwordResetToken = passwordResetToken
        )
    }
}
