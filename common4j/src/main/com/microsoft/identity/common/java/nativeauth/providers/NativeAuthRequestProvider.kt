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
package com.microsoft.identity.common.java.nativeauth.providers

import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.eststelemetry.EstsTelemetry
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.net.HttpConstants
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordPollCompletionRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordStartRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.resetpassword.ResetPasswordSubmitRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInInitiateRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signin.SignInTokenRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpChallengeRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpContinueRequest
import com.microsoft.identity.common.java.nativeauth.providers.requests.signup.SignUpStartRequest
import com.microsoft.identity.common.java.platform.Device
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
     * @param commandParameters: command parameters object
     */
    internal fun createSignInInitiateRequest(
        commandParameters: SignInStartCommandParameters
    ): SignInInitiateRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createSignInInitiateRequest"
        )

        return SignInInitiateRequest.create(
            username = commandParameters.username,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signInInitiateEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }
    //endregion

    //region /oauth/v2.0/challenge
    /**
     * Creates request object for /oauth/v2.0/challenge API call from continuation token
     * @param continuationToken: continuation token from a previous signin command
     */
    internal fun createSignInChallengeRequest(
        continuationToken: String,
        correlationId: String
    ): SignInChallengeRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.createSignInChallengeRequest"
        )

        return SignInChallengeRequest.create(
            clientId = config.clientId,
            continuationToken = continuationToken,
            challengeType = config.challengeType,
            requestUrl = signInChallengeEndpoint,
            headers = getRequestHeaders(correlationId)
        )
    }
    //endregion

    //region /oauth/v2.0/token
    /**
     * Creates request object for /oauth/v2.0/token API call from [SignInSubmitCodeCommandParameters]
     * @param commandParameters: command parameters object
     */
    internal fun createOOBTokenRequest(
        commandParameters: SignInSubmitCodeCommandParameters
    ): SignInTokenRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createOOBTokenRequest"
        )

        return SignInTokenRequest.createOOBTokenRequest(
            oob = commandParameters.code,
            scopes = commandParameters.scopes,
            continuationToken = commandParameters.continuationToken,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signInTokenEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }

    /**
     * Creates request object for /oauth/v2.0/token API call from [SignInWithContinuationTokenCommandParameters]
     * @param commandParameters: command parameters object
     */
    internal fun createContinuationTokenTokenRequest(
        commandParameters: SignInWithContinuationTokenCommandParameters
    ): SignInTokenRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createContinuationTokenRequest"
        )

        return SignInTokenRequest.createContinuationTokenRequest(
            continuationToken = commandParameters.continuationToken,
            scopes = commandParameters.scopes,
            clientId = config.clientId,
            username = commandParameters.username,
            challengeType = config.challengeType,
            requestUrl = signInTokenEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }

    /**
     * Creates request object for /oauth/v2.0/token API call from [SignInSubmitPasswordCommandParameters]
     * @param commandParameters: command parameters object
     */
    internal fun createPasswordTokenRequest(
        commandParameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createPasswordTokenRequest"
        )

        return SignInTokenRequest.createPasswordTokenRequest(
            password = commandParameters.password,
            scopes = commandParameters.scopes,
            continuationToken = commandParameters.continuationToken,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signInTokenEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }
    //endregion

    //region /resetpassword/start
    internal fun createResetPasswordStartRequest(
        commandParameters: ResetPasswordStartCommandParameters
    ): ResetPasswordStartRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createResetPasswordStartRequest"
        )

        return ResetPasswordStartRequest.create(
            clientId = config.clientId,
            username = commandParameters.username,
            challengeType = config.challengeType,
            requestUrl = resetPasswordStartEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }
    //endregion

    //region /resetpassword/challenge
    internal fun createResetPasswordChallengeRequest(
        continuationToken: String,
        correlationId: String
    ): ResetPasswordChallengeRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.createResetPasswordChallengeRequest"
        )

        return ResetPasswordChallengeRequest.create(
            clientId = config.clientId,
            continuationToken = continuationToken,
            challengeType = config.challengeType,
            requestUrl = resetPasswordChallengeEndpoint,
            headers = getRequestHeaders(correlationId)
        )
    }
    //endregion

    //region /resetpassword/continue
    internal fun createResetPasswordContinueRequest(
        commandParameters: ResetPasswordSubmitCodeCommandParameters
    ): ResetPasswordContinueRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createResetPasswordContinueRequest"
        )

        return ResetPasswordContinueRequest.create(
            clientId = config.clientId,
            continuationToken = commandParameters.continuationToken,
            oob = commandParameters.code,
            requestUrl = resetPasswordContinueEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }    
    //endregion

    //region /signup/start
    internal fun createSignUpStartRequest(
        commandParameters: SignUpStartCommandParameters
    ): SignUpStartRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createSignUpStartRequest"
        )

        return SignUpStartRequest.create(
            username = commandParameters.username,
            password = commandParameters.password,
            attributes = commandParameters.userAttributes,
            challengeType = config.challengeType,
            clientId = config.clientId,
            requestUrl = signUpStartEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }
    //endregion

    //region /resetpassword/submit
    internal fun createResetPasswordSubmitRequest(
        commandParameters: ResetPasswordSubmitNewPasswordCommandParameters
    ): ResetPasswordSubmitRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createResetPasswordSubmitRequest"
        )

        return ResetPasswordSubmitRequest.create(
            clientId = config.clientId,
            continuationToken = commandParameters.continuationToken,
            newPassword = commandParameters.newPassword,
            requestUrl = resetPasswordSubmitEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }
    //endregion

    //region /resetpassword/pollcompletion
    internal fun createResetPasswordPollCompletionRequest(
        continuationToken: String,
        correlationId: String
    ): ResetPasswordPollCompletionRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.createResetPasswordPollCompletionRequest"
        )

        return ResetPasswordPollCompletionRequest.create(
            clientId = config.clientId,
            continuationToken = continuationToken,
            requestUrl = resetPasswordPollCompletionEndpoint,
            headers = getRequestHeaders(correlationId)
        )
    }
    //endregion
    
    //region /signup/continue
    internal fun createSignUpSubmitCodeRequest(
        commandParameters: SignUpSubmitCodeCommandParameters
    ): SignUpContinueRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createSignUpSubmitCodeRequest"
        )

        return SignUpContinueRequest.create(
            oob = commandParameters.code,
            clientId = config.clientId,
            continuationToken = commandParameters.continuationToken,
            grantType = NativeAuthConstants.GrantType.OOB,
            requestUrl = signUpContinueEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }

    internal fun createSignUpSubmitPasswordRequest(
        commandParameters: SignUpSubmitPasswordCommandParameters
    ): SignUpContinueRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createSignUpSubmitPasswordRequest"
        )

        return SignUpContinueRequest.create(
            password = commandParameters.password,
            clientId = config.clientId,
            continuationToken = commandParameters.continuationToken,
            grantType = NativeAuthConstants.GrantType.PASSWORD,
            requestUrl = signUpContinueEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }

    internal fun createSignUpSubmitUserAttributesRequest(
        commandParameters: SignUpSubmitUserAttributesCommandParameters
    ): SignUpContinueRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = commandParameters.getCorrelationId(),
            methodName = "${TAG}.createSignUpSubmitUserAttributesRequest"
        )

        return SignUpContinueRequest.create(
            attributes = commandParameters.userAttributes,
            clientId = config.clientId,
            continuationToken = commandParameters.continuationToken,
            grantType = NativeAuthConstants.GrantType.ATTRIBUTES,
            requestUrl = signUpContinueEndpoint,
            headers = getRequestHeaders(commandParameters.getCorrelationId())
        )
    }
    //endregion

    //region /signup/challenge
    internal fun createSignUpChallengeRequest(
        continuationToken: String,
        correlationId: String
    ): SignUpChallengeRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.createSignUpChallengeRequest"
        )

        return SignUpChallengeRequest.create(
            continuationToken = continuationToken,
            clientId = config.clientId,
            challengeType = config.challengeType,
            requestUrl = signUpChallengeEndpoint,
            headers = getRequestHeaders(correlationId)
        )
    }
    //endregion

    //region helpers
    private fun getRequestHeaders(correlationId: String): Map<String, String?> {
        val headers: MutableMap<String, String?> = TreeMap()
        headers[AuthenticationConstants.AAD.CLIENT_REQUEST_ID] = correlationId
        headers[AuthenticationConstants.SdkPlatformFields.PRODUCT] = DiagnosticContext.INSTANCE.requestContext[AuthenticationConstants.SdkPlatformFields.PRODUCT]
        headers[AuthenticationConstants.SdkPlatformFields.VERSION] = Device.getProductVersion()
        headers.putAll(Device.getPlatformIdParameters())
        headers.putAll(EstsTelemetry.getInstance().telemetryHeaders)
        headers[HttpConstants.HeaderField.CONTENT_TYPE] = "application/x-www-form-urlencoded"
        return headers
    }
    //endregion
}
