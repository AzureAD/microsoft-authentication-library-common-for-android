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
package com.microsoft.identity.common.nativeauth.providers

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignInWithSLTCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignUpStartUsingPasswordCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.nativeauth.providers.requests.resetpassword.ResetPasswordChallengeRequest
import com.microsoft.identity.common.nativeauth.providers.requests.resetpassword.ResetPasswordContinueRequest
import com.microsoft.identity.common.nativeauth.providers.requests.resetpassword.ResetPasswordPollCompletionRequest
import com.microsoft.identity.common.nativeauth.providers.requests.resetpassword.ResetPasswordStartRequest
import com.microsoft.identity.common.nativeauth.providers.requests.resetpassword.ResetPasswordSubmitRequest
import com.microsoft.identity.common.nativeauth.providers.requests.signin.SignInChallengeRequest
import com.microsoft.identity.common.nativeauth.providers.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.nativeauth.providers.requests.signin.SignInTokenRequest
import com.microsoft.identity.common.nativeauth.providers.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.nativeauth.providers.requests.signup.SignUpContinueRequest
import com.microsoft.identity.common.nativeauth.providers.requests.signup.SignUpStartRequest
import java.util.TreeMap

/**
 * NativeAuthRequestProvider creates request objects that encapsulate all information required
 * for making REST API calls to Native Auth.
 */
class NativeAuthRequestProvider(private val config: NativeAuthOAuth2Configuration) {
    private val TAG = NativeAuthRequestProvider::class.java.simpleName

    private val signUpStartEndpoint = config.getSignUpStartEndpoint().toString()
    private val signUpChallengeEndpoint = config.getSignUpChallengeEndpoint().toString()
    private val signUpContinueEndpoint = config.getSignUpContinueEndpoint().toString()
    private val signInInitiateEndpoint = config.getSignInInitiateEndpoint().toString()
    private val signInChallengeEndpoint = config.getSignInChallengeEndpoint().toString()
    private val signInTokenEndpoint = config.getSignInTokenEndpoint().toString()
    private val resetPasswordStartEndpoint = config.getResetPasswordStartEndpoint().toString()
    private val resetPasswordChallengeEndpoint = config.getResetPasswordChallengeEndpoint().toString()
    private val resetPasswordContinueEndpoint = config.getResetPasswordContinueEndpoint().toString()
    private val resetPasswordSubmitEndpoint = config.getResetPasswordSubmitEndpoint().toString()
    private val resetPasswordPollCompletionEndpoint = config.getResetPasswordPollCompletionEndpoint().toString()

    //region /oauth/v2.0/initiate
    /**
     * Creates request object for /oauth/v2.0/initiate API call from [SignInStartCommandParameters]
     * @param parameters: command parameters object
     */
    internal fun createSignInInitiateRequest(
        parameters: SignInStartCommandParameters
    ): SignInInitiateRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSignInInitiateRequest")

        return SignInInitiateRequest.create(
            username = parameters.username,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signInInitiateEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /oauth/v2.0/challenge
    /**
     * Creates request object for /oauth/v2.0/challenge API call from credential token
     * @param credentialToken: credential token from a previous signin command
     */
    internal fun createSignInChallengeRequest(
        credentialToken: String
    ): SignInChallengeRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSignInChallengeRequest")

        return SignInChallengeRequest.create(
            clientId = config.clientId,
            credentialToken = credentialToken,
            challengeType = config.challengeType,
            requestUrl = signInChallengeEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /oauth/v2.0/token
    /**
     * Creates request object for /oauth/v2.0/token API call from [SignInSubmitCodeCommandParameters]
     * @param parameters: command parameters object
     */
    internal fun createOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createOOBTokenRequest")

        return SignInTokenRequest.createOOBTokenRequest(
            oob = parameters.code,
            scopes = parameters.scopes,
            credentialToken = parameters.credentialToken,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signInTokenEndpoint,
            headers = getRequestHeaders()
        )
    }

    /**
     * Creates request object for /oauth/v2.0/token API call from [SignInWithSLTCommandParameters]
     * @param parameters: command parameters object
     */
    internal fun createSLTTokenRequest(
        parameters: SignInWithSLTCommandParameters
    ): SignInTokenRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSLTTokenRequest")

        return SignInTokenRequest.createSltTokenRequest(
            signInSlt = parameters.signInSLT,
            scopes = parameters.scopes,
            clientId = config.clientId,
            username = parameters.username,
            challengeType = config.challengeType,
            requestUrl = signInTokenEndpoint,
            headers = getRequestHeaders()
        )
    }

    /**
     * Creates request object for /oauth/v2.0/token API call from [SignInSubmitPasswordCommandParameters]
     * @param parameters: command parameters object
     */
    internal fun createPasswordTokenRequest(
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createPasswordTokenRequest")

        return SignInTokenRequest.createPasswordTokenRequest(
            password = parameters.password,
            scopes = parameters.scopes,
            credentialToken = parameters.credentialToken,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signInTokenEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/start
    internal fun createResetPasswordStartRequest(
        parameters: ResetPasswordStartCommandParameters
    ): ResetPasswordStartRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createResetPasswordStartRequest")

        return ResetPasswordStartRequest.create(
            clientId = config.clientId,
            username = parameters.username,
            challengeType = config.challengeType,
            requestUrl = resetPasswordStartEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/challenge
    internal fun createResetPasswordChallengeRequest(
        passwordResetToken: String
    ): ResetPasswordChallengeRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createResetPasswordChallengeRequest")

        return ResetPasswordChallengeRequest.create(
            clientId = config.clientId,
            passwordResetToken = passwordResetToken,
            challengeType = config.challengeType,
            requestUrl = resetPasswordChallengeEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/continue
    internal fun createResetPasswordContinueRequest(
        parameters: ResetPasswordSubmitCodeCommandParameters
    ): ResetPasswordContinueRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createResetPasswordContinueRequest")

        return ResetPasswordContinueRequest.create(
            clientId = config.clientId,
            passwordResetToken = parameters.passwordResetToken,
            oob = parameters.code,
            requestUrl = resetPasswordContinueEndpoint,
            headers = getRequestHeaders()
        )
    }    
    //endregion

    //region /signup/start
    internal fun createSignUpStartRequest(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSignUpStartRequest")

        return SignUpStartRequest.create(
            username = commandParameters.username,
            attributes = commandParameters.userAttributes,
            challengeType = config.challengeType,
            clientId = config.clientId,
            requestUrl = signUpStartEndpoint,
            headers = getRequestHeaders()
        )
    }

    internal fun createSignUpUsingPasswordStartRequest(
        commandParameters: SignUpStartUsingPasswordCommandParameters
    ): SignUpStartRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSignUpUsingPasswordStartRequest")

        if (commandParameters.password.isEmpty() || commandParameters.password.all { it.isWhitespace() })
        {
            var msg = "password can't be empty or consists solely of whitespace characters"
            throw ClientException("$TAG $msg", msg)
        }

        return SignUpStartRequest.create(
            username = commandParameters.username,
            password = commandParameters.password,
            attributes = commandParameters.userAttributes,
            challengeType = config.challengeType,
            clientId = config.clientId,
            requestUrl = signUpStartEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/submit
    internal fun createResetPasswordSubmitRequest(
        commandParameters: ResetPasswordSubmitNewPasswordCommandParameters
    ): ResetPasswordSubmitRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createResetPasswordSubmitRequest")

        return ResetPasswordSubmitRequest.create(
            clientId = config.clientId,
            passwordSubmitToken = commandParameters.passwordSubmitToken,
            newPassword = commandParameters.newPassword,
            requestUrl = resetPasswordSubmitEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/pollcompletion
    internal fun createResetPasswordPollCompletionRequest(
        passwordResetToken: String
    ): ResetPasswordPollCompletionRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createResetPasswordPollCompletionRequest")

        return ResetPasswordPollCompletionRequest.create(
            clientId = config.clientId,
            passwordResetToken = passwordResetToken,
            requestUrl = resetPasswordPollCompletionEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion
    
    //region /signup/continue
    internal fun createSignUpSubmitCodeRequest(
        commandParameters: SignUpSubmitCodeCommandParameters
    ): SignUpContinueRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSignUpSubmitCodeRequest")

        return SignUpContinueRequest.create(
            oob = commandParameters.code,
            clientId = config.clientId,
            signUpToken = commandParameters.signupToken,
            grantType = NativeAuthConstants.GrantType.OOB,
            requestUrl = signUpContinueEndpoint,
            headers = getRequestHeaders()
        )
    }

    internal fun createSignUpSubmitPasswordRequest(
        commandParameters: SignUpSubmitPasswordCommandParameters
    ): SignUpContinueRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSignUpSubmitPasswordRequest")

        return SignUpContinueRequest.create(
            password = commandParameters.password,
            clientId = config.clientId,
            signUpToken = commandParameters.signupToken,
            grantType = NativeAuthConstants.GrantType.PASSWORD,
            requestUrl = signUpContinueEndpoint,
            headers = getRequestHeaders()
        )
    }

    internal fun createSignUpSubmitUserAttributesRequest(
        commandParameters: SignUpSubmitUserAttributesCommandParameters
    ): SignUpContinueRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSignUpSubmitUserAttributesRequest")

        return SignUpContinueRequest.create(
            attributes = commandParameters.userAttributes,
            clientId = config.clientId,
            signUpToken = commandParameters.signupToken,
            grantType = NativeAuthConstants.GrantType.ATTRIBUTES,
            requestUrl = signUpContinueEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /signup/challenge
    internal fun createSignUpChallengeRequest(
        signUpToken: String
    ): SignUpChallengeRequest {
        LogSession.logMethodCall(TAG, "${TAG}.createSignUpChallengeRequest")

        return SignUpChallengeRequest.create(
            signUpToken = signUpToken,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signUpChallengeEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region helpers
    private fun getRequestHeaders(): Map<String, String?> {
        val headers: MutableMap<String, String?> = TreeMap()
        headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID] =
            DiagnosticContext.INSTANCE.requestContext[DiagnosticContext.CORRELATION_ID]
        headers[HttpConstants.HeaderField.CONTENT_TYPE] = "application/x-www-form-urlencoded"
        return headers
    }
    //endregion
}
