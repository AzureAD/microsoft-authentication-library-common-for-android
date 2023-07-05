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

import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInWithSLTCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignInInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignUpInteractor
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signup.SignUpStartApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters

/**
 * The implementation of native authentication API OAuth2 client.
 */
class NativeAuthOAuth2Strategy(
    private val strategyParameters: OAuth2StrategyParameters,
    @get:VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    val config: NativeAuthOAuth2Configuration,
    private val signInInteractor: SignInInteractor,
    private val signUpInteractor: SignUpInteractor,
    private val resetPasswordInteractor: ResetPasswordInteractor
) :
    MicrosoftStsOAuth2Strategy(config, strategyParameters) {
    private val TAG = NativeAuthOAuth2Strategy::class.java.simpleName

    // Hardcoding so that the environment parameter from the mock API token response matches
    // with the environment retrieved from the (authority) endpoints.
    // TODO fix after mock APIs
    override fun getIssuerCacheIdentifierFromTokenEndpoint(): String {
        if (config.useRealAuthority) {
            return super.getIssuerCacheIdentifierFromTokenEndpoint()
        } else {
            return "login.windows.net"
        }
    }

    // TODO unit tests & compare with getAuthorityFromTokenEndpoint()
    fun getAuthority(): String {
        return config.authorityUrl.toString()
    }

    fun performSignUpStart(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signUpInteractor.performSignUpStart(commandParameters)
    }

    fun performSignUpStartUsingPassword(
        commandParameters: SignUpStartUsingPasswordCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signUpInteractor.performSignUpStartUsingPassword(commandParameters)
    }

    fun performSignUpChallenge(
        signUpToken: String
    ): SignUpChallengeApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signUpInteractor.performSignUpChallenge(
            signUpToken = signUpToken
        )
    }

    fun performSignUpSubmitCode(
        commandParameters: SignUpSubmitCodeCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signUpInteractor.performSignUpSubmitCode(
            commandParameters = commandParameters
        )
    }

    fun performSignUpSubmitPassword(
        commandParameters: SignUpSubmitPasswordCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signUpInteractor.performSignUpSubmitPassword(
            commandParameters = commandParameters
        )
    }

    fun performSignUpSubmitUserAttributes(
        commandParameters: SignUpSubmitUserAttributesCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signUpInteractor.performSignUpSubmitUserAttributes(
            commandParameters = commandParameters
        )
    }

    fun performSignInInitiate(
        parameters: SignInStartCommandParameters
    ): SignInInitiateApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signInInteractor.performSignInInitiate(parameters)
    }

    fun performSignInChallenge(
        credentialToken: String,
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signInInteractor.performSignInChallenge(
            credentialToken = credentialToken,
        )
    }

    fun performSLTTokenRequest(
        parameters: SignInWithSLTCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signInInteractor.performSLTTokenRequest(
            parameters = parameters
        )
    }

    fun performOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signInInteractor.performOOBTokenRequest(
            parameters = parameters
        )
    }

    fun performPasswordTokenRequest(
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        return signInInteractor.performPasswordTokenRequest(
            parameters = parameters
        )
    }

    fun performResetPasswordStart(
        parameters: ResetPasswordStartCommandParameters
    ): ResetPasswordStartApiResult {
        LogSession.logMethodCall(tag = TAG)
        return resetPasswordInteractor.performResetPasswordStart(
            parameters = parameters
        )
    }

    fun performResetPasswordChallenge(
        passwordResetToken: String
    ): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(tag = TAG)
        return resetPasswordInteractor.performResetPasswordChallenge(
            passwordResetToken = passwordResetToken
        )
    }

    fun performResetPasswordContinue(
        parameters: ResetPasswordSubmitCodeCommandParameters
    ): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(tag = TAG)
        return resetPasswordInteractor.performResetPasswordContinue(
            parameters = parameters
        )
    }

    fun performResetPasswordSubmit(
        parameters: ResetPasswordSubmitNewPasswordCommandParameters
    ): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(tag = TAG)
        return resetPasswordInteractor.performResetPasswordSubmit(
            commandParameters = parameters
        )
    }

    fun performResetPasswordPollCompletion(
        passwordResetToken: String
    ): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(tag = TAG)
        return resetPasswordInteractor.performResetPasswordPollCompletion(
            passwordResetToken = passwordResetToken
        )
    }
}
