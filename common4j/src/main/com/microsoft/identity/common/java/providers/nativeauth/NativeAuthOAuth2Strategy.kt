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
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignInInteractor
import com.microsoft.identity.common.java.providers.nativeauth.interactors.SignUpInteractor
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
    val config: NativeAuthOAuth2Configuration,
    private val signInInteractor: SignInInteractor,
    private val signUpInteractor: SignUpInteractor,
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

    fun performSignUpStart(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpStart")
        return signUpInteractor.performSignUpStart(commandParameters)
    }

    fun performSignUpStartUsingPassword(
        commandParameters: SignUpStartUsingPasswordCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpStartUsingPassword")
        return signUpInteractor.performSignUpStartUsingPassword(commandParameters)
    }

    fun performSignUpChallenge(
        signUpToken: String
    ): SignUpChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpChallenge")
        return signUpInteractor.performSignUpChallenge(
            signUpToken = signUpToken
        )
    }

    fun performSignUpSubmitCode(
        commandParameters: SignUpSubmitCodeCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpSubmitCode")
        return signUpInteractor.performSignUpSubmitCode(
            commandParameters = commandParameters
        )
    }

    fun performSignUpSubmitPassword(
        commandParameters: SignUpSubmitPasswordCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpSubmitPassword")
        return signUpInteractor.performSignUpSubmitPassword(
            commandParameters = commandParameters
        )
    }

    fun performSignUpSubmitUserAttributes(
        commandParameters: SignUpSubmitUserAttributesCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignUpSubmitUserAttributes")
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
        LogSession.logMethodCall(TAG, "${TAG}.performSignInInitiate")
        return signInInteractor.performSignInInitiate(parameters)
    }

    /**
     * Performs API call to /oauth/v2.0/challenge
     */
    fun performSignInChallenge(
        credentialToken: String,
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSignInChallenge")
        return signInInteractor.performSignInChallenge(
            credentialToken = credentialToken,
        )
    }

    /**
     * Performs API call to /oauth/v2.0/token with short lived token. SLT was created in prior call
     * to signup APIs.
     */
    fun performSLTTokenRequest(
        parameters: SignInWithSLTCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performSLTTokenRequest")
        return signInInteractor.performSLTTokenRequest(
            parameters = parameters
        )
    }

    /**
     * Performs API call to /oauth/v2.0/token with out of band code.
     */
    fun performOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(TAG, "${TAG}.performOOBTokenRequest")
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
        LogSession.logMethodCall(TAG, "${TAG}.performPasswordTokenRequest")
        return signInInteractor.performPasswordTokenRequest(
            parameters = parameters
        )
    }
}
