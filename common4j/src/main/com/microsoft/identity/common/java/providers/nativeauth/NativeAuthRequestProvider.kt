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
package com.microsoft.identity.common.java.providers.nativeauth

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.commands.parameters.nativeauth.*
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.providers.nativeauth.requests.NativeAuthGrantType
import com.microsoft.identity.common.java.providers.nativeauth.requests.signin.SignInChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signin.SignInTokenRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpContinueRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.signup.SignUpStartRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprChallengeRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprContinueRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprPollCompletionRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprStartRequest
import com.microsoft.identity.common.java.providers.nativeauth.requests.sspr.SsprSubmitRequest
import java.util.TreeMap

class NativeAuthRequestProvider(private val config: NativeAuthOAuth2Configuration) {
    private val TAG = NativeAuthRequestProvider::class.java.simpleName

    private val signUpStartEndpoint = config.getSignUpStartEndpoint().toString()
    private val signUpChallengeEndpoint = config.getSignUpChallengeEndpoint().toString()
    private val signUpContinueEndpoint = config.getSignUpContinueEndpoint().toString()
    private val signInInitiateEndpoint = config.getSignInInitiateEndpoint().toString()
    private val signInChallengeEndpoint = config.getSignInChallengeEndpoint().toString()
    private val signInTokenEndpoint = config.getSignInTokenEndpoint().toString()
    private val ssprStartEndpoint = config.getSsprStartEndpoint().toString()
    private val ssprChallengeEndpoint = config.getSsprChallengeEndpoint().toString()
    private val ssprContinueEndpoint = config.getSsprContinueEndpoint().toString()
    private val ssprSubmitEndpoint = config.getSsprSubmitEndpoint().toString()
    private val ssprPollCompletionEndpoint = config.getSsprPollCompletionEndpoint().toString()

    //region /signup/start
    fun createSignUpStartRequest(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartRequest {
        val methodName = ":createSignUpStartRequest"
        Logger.verbose(
            TAG + methodName,
            "Creating SignUpStartRequest..."
        )

        return SignUpStartRequest.create(
            username = commandParameters.email,
            password = commandParameters.password,
            attributes = commandParameters.userAttributes,
            challengeType = config.challengeType,
            clientId = config.clientId,
            requestUrl = signUpStartEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /signup/challenge
    fun createSignUpChallengeRequest(
        signUpToken: String
    ): SignUpChallengeRequest {
        val methodName = ":createSignUpChallengeRequest"
        Logger.verbose(
            TAG + methodName,
            "Creating SignUpChallengeRequest..."
        )

        return SignUpChallengeRequest.create(
            signUpToken = signUpToken,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signUpChallengeEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /oauth/v2.0/initiate
    fun createSignInInitiateRequest(
        parameters: SignInStartCommandParameters
    ): SignInInitiateRequest {
        val methodName = ":createSignInInitiateRequest"
        Logger.verbose(
            TAG + methodName,
            "Creating SignInInitiateRequest..."
        )

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
    fun createSignInChallengeRequest(
        credentialToken: String
    ): SignInChallengeRequest {
        val methodName = ":createSignInChallengeRequest"
        Logger.verbose(
            TAG + methodName,
            "Creating SignInChallengeRequest..."
        )

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
    fun createROPCTokenRequest(
        parameters: SignInStartWithPasswordCommandParameters
    ): SignInTokenRequest {
        val methodName = ":createROPCTokenRequest"
        Logger.verbose(
            TAG + methodName,
            "Creating ROPC token request..."
        )
        return SignInTokenRequest.createROPCTokenRequest(
            username = parameters.username,
            password = parameters.password,
            scopes = parameters.scopes,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signInTokenEndpoint,
            headers = getRequestHeaders()
        )
    }

    fun createOOBTokenRequest(
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenRequest {
        val methodName = ":createOOBTokenRequest"
        Logger.verbose(
            TAG + methodName,
            "Creating OOB token request..."
        )
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
    //endregion

    //region /signup/continue
    fun createSignUpContinueRequest(
        signUpToken: String,
        commandParameters: SignUpContinueCommandParameters
    ): SignUpContinueRequest {
        var grantType = ""
        if (!commandParameters.password.isNullOrBlank()) {
            grantType = NativeAuthGrantType.PASSWORD.jsonValue
        } else if (!commandParameters.oob.isNullOrEmpty()) {
            grantType = NativeAuthGrantType.PASSWORDLESS_OTP.jsonValue
        } else if (commandParameters.userAttributes != null) {
            grantType = NativeAuthGrantType.ATTRIBUTES.jsonValue
        }

        return SignUpContinueRequest.create(
            password = commandParameters.password,
            attributes = commandParameters.userAttributes,
            oob = commandParameters.oob,
            clientId = config.clientId,
            signUpToken = signUpToken,
            grantType = grantType,
            requestUrl = signUpContinueEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/start
    fun createSsprStartRequest(
        parameters: SsprStartCommandParameters
    ): SsprStartRequest {
        return SsprStartRequest.create(
            clientId = config.clientId,
            username = parameters.username,
            challengeType = config.challengeType,
            requestUrl = ssprStartEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/challenge
    fun createSsprChallengeRequest(
        passwordResetToken: String
    ): SsprChallengeRequest {
        return SsprChallengeRequest.create(
            clientId = config.clientId,
            passwordResetToken = passwordResetToken,
            challengeType = config.challengeType,
            requestUrl = ssprChallengeEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/continue
    fun createSsprContinueRequest(
        parameters: SsprSubmitCodeCommandParameters
    ): SsprContinueRequest {
        return SsprContinueRequest.create(
            clientId = config.clientId,
            passwordResetToken = parameters.passwordResetToken,
            oob = parameters.code,
            requestUrl = ssprContinueEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/submit
    fun createSsprSubmitRequest(
        commandParameters: SsprSubmitNewPasswordCommandParameters
    ): SsprSubmitRequest {
        return SsprSubmitRequest.create(
            clientId = config.clientId,
            passwordSubmitToken = commandParameters.passwordSubmitToken,
            newPassword = commandParameters.newPassword,
            requestUrl = ssprSubmitEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region /resetpassword/pollcompletion
    fun createSsprPollCompletionRequest(
        passwordResetToken: String
    ): SsprPollCompletionRequest {
        return SsprPollCompletionRequest.create(
            clientId = config.clientId,
            passwordResetToken = passwordResetToken,
            requestUrl = ssprPollCompletionEndpoint,
            headers = getRequestHeaders()
        )
    }
    //endregion

    //region helpers
    private fun getRequestHeaders(): Map<String, String?> {
        val headers: MutableMap<String, String?> = TreeMap()
        headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID] =
            DiagnosticContext.INSTANCE.requestContext[DiagnosticContext.CORRELATION_ID]
        // TODO remove this
//        headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID] = "12345abcd"
        headers[HttpConstants.HeaderField.CONTENT_TYPE] = "application/x-www-form-urlencoded"
        return headers
    }
    //endregion
}
