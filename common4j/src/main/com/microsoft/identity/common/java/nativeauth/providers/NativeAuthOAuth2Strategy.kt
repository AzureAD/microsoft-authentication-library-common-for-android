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

import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.interactors.ResetPasswordInteractor
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignInInteractor
import com.microsoft.identity.common.java.nativeauth.providers.interactors.SignUpInteractor
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartApiResult
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters

/**
 * The implementation of native authentication API OAuth2 client.
 */
class NativeAuthOAuth2Strategy(
    private val strategyParameters: OAuth2StrategyParameters,
    val config: NativeAuthOAuth2Configuration,
    private val signInInteractor: SignInInteractor,
    private val signUpInteractor: SignUpInteractor,
    private val resetPasswordInteractor: ResetPasswordInteractor
) :
    MicrosoftStsOAuth2Strategy(config, strategyParameters) {
    private val TAG = NativeAuthOAuth2Strategy::class.java.simpleName
    //Cache identifier returned by the mock API
    private val CACHE_IDENTIFIER_MOCK = "login.windows.net"

    /**
     * Returns the issuer cache identifier. For mock APIs, a static value of cache identifier is used.
     */
    override fun getIssuerCacheIdentifierFromTokenEndpoint(): String {
        if (config.useMockApiForNativeAuth) {
            return CACHE_IDENTIFIER_MOCK
        } else {
            return super.getIssuerCacheIdentifierFromTokenEndpoint()
        }
    }

    /**
     * Returns the authority url
     */
    fun getAuthority(): String {
        return config.authorityUrl.toString()
    }

    /**
     * Makes the initial call to the /signup/start.
     * @param commandParameters: Attributes provided by the user
     */
    fun performSignUpStart(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            "${TAG}.performSignUpStart"
        )
        return signUpInteractor.performSignUpStart(commandParameters)
    }

    /**
     * Makes the initial call to the /signup/start when the parameters includes password.
     * @param commandParameters: Attributes provided by the user
     */
    fun performSignUpStartUsingPassword(
        commandParameters: SignUpStartUsingPasswordCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            "${TAG}.performSignUpStartUsingPassword"
        )
        return signUpInteractor.performSignUpStartUsingPassword(commandParameters)
    }

    /**
     * Makes the call to the /signup/challenge for Signup operation.
     * @param signUpToken: Token received from the previous /signup/start call
     */
    fun performSignUpChallenge(
        continuationToken: String,
        correlationId: String?
    ): SignUpChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            "${TAG}.performSignUpChallenge"
        )
        return signUpInteractor.performSignUpChallenge(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    /**
     * Makes the call to the /signup/continue to submit the out of band code.
     * @param commandParameters: Parameters required for this call including oob code
     */
    fun performSignUpSubmitCode(
        commandParameters: SignUpSubmitCodeCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            "${TAG}.performSignUpSubmitCode"
        )
        return signUpInteractor.performSignUpSubmitCode(
            commandParameters = commandParameters
        )
    }

    /**
     * Makes the call to the /signup/continue to submit the user password.
     * @param commandParameters: Parameters required for this call including password
     */
    fun performSignUpSubmitPassword(
        commandParameters: SignUpSubmitPasswordCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            "${TAG}.performSignUpSubmitPassword"
        )
        return signUpInteractor.performSignUpSubmitPassword(
            commandParameters = commandParameters
        )
    }

    /**
     * Makes the call to the /signup/continue to submit the user attributes.
     * @param commandParameters: Attributes provided by the user
     */
    fun performSignUpSubmitUserAttributes(
        commandParameters: SignUpSubmitUserAttributesCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            "${TAG}.performSignUpSubmitUserAttributes"
        )
        return signUpInteractor.performSignUpSubmitUserAttributes(
            commandParameters = commandParameters
        )
    }

    /**
     * Performs the initial API call to /oauth/v2.0/initiate
     */
    fun performSignInInitiate(
        parameters: SignInStartCommandParameters
    ): SignInInitiateApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            "${TAG}.performSignInInitiate"
        )
        return signInInteractor.performSignInInitiate(parameters)
    }

    /**
     * Performs API call to /oauth/v2.0/challenge
     */
    fun performSignInChallenge(
        continuationToken: String,
        correlationId: String?
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            "${TAG}.performSignInChallenge"
        )
        return signInInteractor.performSignInChallenge(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    /**
     * Performs API call to /oauth/v2.0/token with continuation token. Continuation token was created in prior call
     * to signup APIs.
     */
    fun performContinuationTokenTokenRequest(
        parameters: SignInWithContinuationTokenCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            "${TAG}.performContinuationTokenRequest"
        )
        return signInInteractor.performContinuationTokenTokenRequest(
            parameters = parameters
        )
    }

    /**
     * Performs API call to /oauth/v2.0/token with out of band code.
     */
    fun performOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            "${TAG}.performOOBTokenRequest"
        )
        return signInInteractor.performOOBTokenRequest(
            parameters = parameters
        )
    }

    /**
     * Performs API call to /oauth/v2.0/token with password.
     */
    fun performPasswordTokenRequest(
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            "${TAG}.performPasswordTokenRequest"
        )
        return signInInteractor.performPasswordTokenRequest(
            parameters = parameters
        )
    }

    /**
     * Performs the initial API call to /resetpassword/start
     * @param parameters: Parameters required for the call including username
     * @return result of the API call as [ResetPasswordStartApiResult] object
     */
    fun performResetPasswordStart(
        parameters: ResetPasswordStartCommandParameters
    ): ResetPasswordStartApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            "${TAG}.performResetPasswordStart"
        )
        return resetPasswordInteractor.performResetPasswordStart(
            parameters = parameters
        )
    }

    /**
     * Performs the API call to /resetpassword/challenge
     * @param passwordResetToken: Token received from previous /resetpassword/start call
     * @return result of the API call as [ResetPasswordChallengeApiResult] object
     */
    fun performResetPasswordChallenge(
        continuationToken: String,
        correlationId: String?
    ): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            "${TAG}.performResetPasswordChallenge"
        )
        return resetPasswordInteractor.performResetPasswordChallenge(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    /**
     * Performs the API call to /resetpassword/continue to submit out of band code
     * @param parameters: Parameters required for the call including oob code
     * @return result of the API call as [ResetPasswordContinueApiResult] object
     */
    fun performResetPasswordContinue(
        parameters: ResetPasswordSubmitCodeCommandParameters
    ): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            "${TAG}.performResetPasswordContinue"
        )
        return resetPasswordInteractor.performResetPasswordContinue(
            parameters = parameters
        )
    }

    /**
     * Performs the API call to /resetpassword/continue to submit new user password
     * @param parameters: Parameters required for the call including new user password
     * @return result of the API call as [ResetPasswordSubmitApiResult] object
     */
    fun performResetPasswordSubmit(
        parameters: ResetPasswordSubmitNewPasswordCommandParameters
    ): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            "${TAG}.performResetPasswordSubmit"
        )
        return resetPasswordInteractor.performResetPasswordSubmit(
            commandParameters = parameters
        )
    }

    /**
     * Performs the API call to /resetpassword/poll_completion
     * @param passwordResetToken: Token received from previous call
     * @return result of the API call as [ResetPasswordPollCompletionApiResult] object
     */
    fun performResetPasswordPollCompletion(
        continuationToken: String,
        correlationId: String?
    ): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            "${TAG}.performResetPasswordPollCompletion"
        )
        return resetPasswordInteractor.performResetPasswordPollCompletion(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }
}
