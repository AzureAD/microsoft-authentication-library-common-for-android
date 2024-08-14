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
package com.microsoft.identity.common.nativeauth.internal.controllers

import android.text.TextUtils
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.internal.commands.RefreshOnCommand
import com.microsoft.identity.common.internal.telemetry.Telemetry
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters
import com.microsoft.identity.common.java.configuration.LibraryConfiguration
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.ArgumentException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.commands.parameters.BaseNativeAuthCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.GetAuthMethodsCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFAChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASelectedChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.MFASubmitChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInWithContinuationTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpStartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.GetAuthMethodsCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.MFAChallengeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.MFACommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.MFASubmitChallengeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordSubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.ResetPasswordSubmitNewPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInSubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInSubmitPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignInWithContinuationTokenCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpSubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpSubmitPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpSubmitUserAttributesCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInIntrospectApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpContinueApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signup.SignUpStartApiResult
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.result.AcquireTokenResult
import com.microsoft.identity.common.java.result.LocalAuthenticationResult
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.util.ThreadUtils
import com.microsoft.identity.common.nativeauth.internal.commands.ResetPasswordSubmitNewPasswordCommand
import com.microsoft.identity.common.nativeauth.internal.util.CommandUtil
import lombok.EqualsAndHashCode
import java.io.IOException
import java.net.URL

/**
 * The implementation of MSAL Controller for Native Authentication.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
class NativeAuthMsalController : BaseNativeAuthController() {

    companion object {
        private val TAG = NativeAuthMsalController::class.java.simpleName
    }

    /**
     * Acts as the starting point for the Sign In flow.
     * Starts off with the sign in initiate call, and returns any error results if produced.
     * If successful, continues ot the sign in challenge call, again returning any error results if produced.
     * If the challenge endpoint returns a password required result,
     * a call to the token endpoint will be made with the password provided in the SignInStartCommandParameters,
     * again returning any error results, or returning the token if the call succeeds.
     * If a token is acquired, it is also cached locally as part of this flow.
     */
    fun signInStart(parameters: SignInStartCommandParameters): SignInStartCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signInStart"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)
            val initiateApiResult = performSignInInitiateCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters)

            val hasPassword = parameters.password?.isNotEmpty() == true

            if (hasPassword)
            {
                Logger.verbose(
                    TAG,
                    parameters.getCorrelationId(),
                    "Parameters has password"
                )
                val mergedScopes = addDefaultScopes(parameters.scopes)
                var parametersWithScopes = CommandUtil.createSignInStartCommandParametersWithScopes(
                    parameters,
                    mergedScopes
                )

                try {
                    return processSignInInitiateApiResult(
                        initiateApiResult = initiateApiResult,
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        usePassword = true)
                } finally {
                    StringUtil.overwriteWithNull(parametersWithScopes.password)
                }
            }
            else
            {
                Logger.verbose(
                    TAG,
                    parameters.getCorrelationId(),
                    "Parameters doesn't have password"
                )
                return processSignInInitiateApiResult(
                    initiateApiResult = initiateApiResult,
                    oAuth2Strategy = oAuth2Strategy)
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in signInStart",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the /token endpoint with the provided Continuation Token, and caches the returned token
     * if successful. In case of error [INativeAuthCommandResult.APIError] is returned.
     */
    fun signInWithContinuationToken(parameters: SignInWithContinuationTokenCommandParameters): SignInWithContinuationTokenCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signInWithContinuationToken"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val mergedScopes = addDefaultScopes(parameters.scopes)
            val parametersWithScopes = CommandUtil.createSignInWithContinuationTokenCommandParametersWithScopes(
                parameters,
                mergedScopes
            )

            val tokenApiResult = performContinuationTokenTokenRequest(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parametersWithScopes
            )

            when (tokenApiResult) {
                is SignInTokenApiResult.Success -> {
                    return saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = tokenApiResult
                    )
                }
                is SignInTokenApiResult.InvalidAuthenticationType,
                is SignInTokenApiResult.MFARequired, is SignInTokenApiResult.CodeIncorrect,
                is SignInTokenApiResult.UserNotFound, is SignInTokenApiResult.InvalidCredentials,
                is SignInTokenApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        tokenApiResult.correlationId,
                        "Unexpected result: ",
                        tokenApiResult
                    )
                    tokenApiResult as ApiErrorResult

                    return INativeAuthCommandResult.APIError(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        errorCodes = tokenApiResult.errorCodes,
                        correlationId = tokenApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.correlationId,
                "Exception thrown in signInWithContinuationToken",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the /token endpoint with the provided OOB code, and caches the returned token
     * if successful.  In case of error [INativeAuthCommandResult.APIError] is returned.
     */
    fun signInSubmitCode(parameters: SignInSubmitCodeCommandParameters): SignInSubmitCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signInSubmitCode"
        )

        try {
            // Add default scopes
            val mergedScopes: List<String> = addDefaultScopes(parameters.scopes)

            val parametersWithScopes =
                CommandUtil.createSignInSubmitCodeCommandParametersWithScopes(
                    parameters,
                    mergedScopes
                )

            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val tokenApiResult = performOOBTokenRequest(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parametersWithScopes
            )
            return when (tokenApiResult) {
                is SignInTokenApiResult.Success -> {
                    saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = tokenApiResult
                    )
                }
                is SignInTokenApiResult.CodeIncorrect -> {
                    SignInCommandResult.IncorrectCode(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        errorCodes = tokenApiResult.errorCodes,
                        subError = tokenApiResult.subError,
                        correlationId = tokenApiResult.correlationId
                    )
                }

                is SignInTokenApiResult.UnknownError, is SignInTokenApiResult.InvalidAuthenticationType,
                is SignInTokenApiResult.MFARequired, is SignInTokenApiResult.InvalidCredentials,
                is SignInTokenApiResult.UserNotFound -> {
                    Logger.warnWithObject(
                        TAG,
                        tokenApiResult.correlationId,
                        "Unexpected result: ",
                        tokenApiResult
                    )
                    tokenApiResult as ApiErrorResult
                    INativeAuthCommandResult.APIError(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        errorCodes = tokenApiResult.errorCodes,
                        correlationId = tokenApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.correlationId,
                "Exception thrown in signInSubmitCode",
                e
            )
            throw e
        }
    }

    fun signInSubmitChallenge(parameters: MFASubmitChallengeCommandParameters): MFASubmitChallengeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signInSubmitCode"
        )

        try {
            // Add default scopes
            val mergedScopes: List<String> = addDefaultScopes(parameters.scopes)

            val parametersWithScopes =
                CommandUtil.createMFASubmitChallengeCommandParametersWithScopes(
                    parameters,
                    mergedScopes
                )

            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val tokenApiResult = performSubmitChallengeTokenRequest(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parametersWithScopes
            )
            return when (tokenApiResult) {
                is SignInTokenApiResult.Success -> {
                    saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = tokenApiResult
                    )
                }
                is SignInTokenApiResult.MFARequired -> {
                    SignInCommandResult.MFARequired(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        continuationToken = tokenApiResult.continuationToken,
                        subError = tokenApiResult.subError,
                        errorCodes = tokenApiResult.errorCodes,
                        correlationId = tokenApiResult.correlationId
                    )
                }
                is SignInTokenApiResult.CodeIncorrect -> {
                    SignInCommandResult.IncorrectCode(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        errorCodes = tokenApiResult.errorCodes,
                        subError = tokenApiResult.subError,
                        correlationId = tokenApiResult.correlationId
                    )
                }
                is SignInTokenApiResult.UnknownError, is SignInTokenApiResult.InvalidAuthenticationType,
                is SignInTokenApiResult.InvalidCredentials, is SignInTokenApiResult.UserNotFound -> {
                    Logger.warnWithObject(
                        TAG,
                        tokenApiResult.correlationId,
                        "Unexpected result: ",
                        tokenApiResult
                    )
                    tokenApiResult as ApiErrorResult
                    INativeAuthCommandResult.APIError(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        errorCodes = tokenApiResult.errorCodes,
                        correlationId = tokenApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.correlationId,
                "Exception thrown in signInSubmitCode",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the resend code endpoint, mapping responses returned from the server into a command result.
     */
    fun signInResendCode(parameters: SignInResendCodeCommandParameters): SignInResendCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signInResendCode"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val result = performSignInDefaultChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                continuationToken = parameters.continuationToken,
                correlationId = parameters.correlationId
            )
            return when (result) {
                is SignInChallengeApiResult.OOBRequired -> {
                    SignInCommandResult.CodeRequired(
                        continuationToken = result.continuationToken,
                        codeLength = result.codeLength,
                        challengeTargetLabel = result.challengeTargetLabel,
                        challengeChannel = result.challengeChannel,
                        correlationId = result.correlationId
                    )
                }
                is SignInChallengeApiResult.PasswordRequired -> {
                    Logger.warnWithObject(
                        TAG,
                        result.correlationId,
                        "Unexpected result: ",
                        result
                    )
                    INativeAuthCommandResult.APIError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result",
                        correlationId = result.correlationId
                    )
                }
                is SignInChallengeApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect(
                        correlationId = result.correlationId
                    )
                }
                is SignInChallengeApiResult.IntrospectRequired, is SignInChallengeApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        "Unexpected result: ",
                        result
                    )
                    (result as ApiErrorResult)
                    INativeAuthCommandResult.APIError(
                        error = result.error,
                        errorDescription = result.errorDescription,
                        errorCodes = result.errorCodes,
                        correlationId = result.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.correlationId,
                "Exception thrown in signInResendCode",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the submit password endpoint, mapping responses returned from the server into a command result.
     */
    fun signInSubmitPassword(parameters: SignInSubmitPasswordCommandParameters): SignInSubmitPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signInSubmitPassword"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val mergedScopes = addDefaultScopes(parameters.scopes)
            val parametersWithScopes =
                CommandUtil.createSignInSubmitPasswordCommandParametersWithScopes(
                    parameters,
                    parameters.getCorrelationId(),
                    mergedScopes
                )

            try {
                return performPasswordTokenCall(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parametersWithScopes
                ).toSignInSubmitPasswordCommandResult(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes
                )
            } finally {
                StringUtil.overwriteWithNull(parametersWithScopes.password)
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.correlationId,
                "Exception thrown in signInSubmitPassword",
                e
            )
            throw e
        }
    }

    fun signInChallenge(parameters: MFAChallengeCommandParameters): MFAChallengeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.challengeDefaultAuth()"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val isSelectedChallenge = (parameters is MFASelectedChallengeCommandParameters)

            val result = if (isSelectedChallenge) {
                performSignInSelectedChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    continuationToken = parameters.continuationToken,
                    correlationId = parameters.correlationId,
                    challengeId = (parameters as MFASelectedChallengeCommandParameters).authMethodId
                )
            } else {
                performSignInDefaultChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    continuationToken = parameters.continuationToken,
                    correlationId = parameters.correlationId
                )
            }
            return when (result) {
                is SignInChallengeApiResult.IntrospectRequired -> {
                    return performIntrospectCall(
                        oAuth2Strategy = oAuth2Strategy,
                        continuationToken = parameters.continuationToken,
                        correlationId = parameters.correlationId
                    ).toMFAChallengeCommandResult()
                }
                is SignInChallengeApiResult.OOBRequired -> {
                    MFACommandResult.VerificationRequired(
                        correlationId = result.correlationId,
                        continuationToken = result.continuationToken,
                        challengeChannel = result.challengeChannel,
                        challengeTargetLabel = result.challengeTargetLabel,
                        codeLength = result.codeLength
                    )
                }
                is SignInChallengeApiResult.PasswordRequired -> {
                    Logger.warnWithObject(
                        TAG,
                        result.correlationId,
                        "Unexpected result: ",
                        result
                    )
                    INativeAuthCommandResult.APIError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result",
                        correlationId = result.correlationId
                    )
                }
                is SignInChallengeApiResult.Redirect, is SignInChallengeApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        "Unexpected result: ",
                        result
                    )
                    (result as ApiErrorResult)
                    INativeAuthCommandResult.APIError(
                        error = result.error,
                        errorDescription = result.errorDescription,
                        errorCodes = result.errorCodes,
                        correlationId = result.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.correlationId,
                "Exception thrown in challengeDefaultAuth()",
                e
            )
            throw e
        }
    }

    fun getAuthMethods(parameters: GetAuthMethodsCommandParameters): GetAuthMethodsCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.getAuthMethods()"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val result = performIntrospectCall(
                oAuth2Strategy = oAuth2Strategy,
                continuationToken = parameters.continuationToken,
                correlationId = parameters.correlationId
            )
            return when (result) {
                is SignInIntrospectApiResult.Success -> {
                    MFACommandResult.SelectionRequired(
                        correlationId = result.correlationId,
                        continuationToken = result.continuationToken,
                        authMethods = result.methods
                    )
                }
                is SignInIntrospectApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect(
                        correlationId = result.correlationId
                    )
                }
                is SignInIntrospectApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        "Unexpected result: ",
                        result
                    )
                    (result as ApiErrorResult)
                    INativeAuthCommandResult.APIError(
                        error = result.error,
                        errorDescription = result.errorDescription,
                        errorCodes = result.errorCodes,
                        correlationId = result.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.correlationId,
                "Exception thrown in getAuthMethods()",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the signup/start endpoint, mapping responses returned from the server into a command result.
     * If the call is successful, additionally calls the signup/challenge endpoint, returning the result.
     */
    fun signUpStart(parameters: SignUpStartCommandParameters): SignUpStartCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signUpStart"
        )
        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val signUpStartApiResult = performSignUpStartUsingPasswordRequest(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters
                )
            return when (signUpStartApiResult) {
                is SignUpStartApiResult.Success -> {
                    performSignUpChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        continuationToken = signUpStartApiResult.continuationToken,
                        correlationId = signUpStartApiResult.correlationId
                    ).toSignUpStartCommandResult()
                }
                is SignUpStartApiResult.InvalidPassword -> {
                    SignUpCommandResult.InvalidPassword(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        subError = signUpStartApiResult.subError,
                        correlationId = signUpStartApiResult.correlationId
                    )
                }
                is SignUpStartApiResult.InvalidAttributes -> {
                    SignUpCommandResult.InvalidAttributes(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        invalidAttributes = signUpStartApiResult.invalidAttributes,
                        correlationId = signUpStartApiResult.correlationId
                    )
                }
                is SignUpStartApiResult.UsernameAlreadyExists -> {
                    SignUpCommandResult.UsernameAlreadyExists(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        correlationId = signUpStartApiResult.correlationId
                    )
                }
                is SignUpStartApiResult.InvalidUsername -> {
                    INativeAuthCommandResult.InvalidUsername(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        correlationId = signUpStartApiResult.correlationId
                    )
                }
                is SignUpStartApiResult.AuthNotSupported -> {
                    SignUpCommandResult.AuthNotSupported(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        correlationId = signUpStartApiResult.correlationId
                    )
                }
                is SignUpStartApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect(
                        correlationId = signUpStartApiResult.correlationId
                    )
                }
                is SignUpStartApiResult.UnsupportedChallengeType, is SignUpStartApiResult.UnknownError -> {
                    signUpStartApiResult as ApiErrorResult
                    Logger.warnWithObject(
                        TAG,
                        signUpStartApiResult.correlationId,
                        "Unexpected result: ",
                        signUpStartApiResult
                    )
                    INativeAuthCommandResult.APIError(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        correlationId = signUpStartApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in signUpStart",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the signup/continue endpoint using the provided code, mapping responses returned from the server into a command result.
     */
    fun signUpSubmitCode(parameters: SignUpSubmitCodeCommandParameters): SignUpSubmitCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signUpSubmitCode"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val signUpSubmitCodeResult = performSignUpSubmitCode(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )
            return signUpSubmitCodeResult.toSignUpSubmitCodeCommandResult(oAuth2Strategy)
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in signUpSubmitCode",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the signup/challenge endpoint to trigger a code to be re-sent.
     */
    fun signUpResendCode(parameters: SignUpResendCodeCommandParameters): SignUpResendCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signUpResendCode"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            return performSignUpChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                continuationToken = parameters.continuationToken,
                correlationId = parameters.correlationId
            ).toSignUpStartCommandResult() as SignUpResendCodeCommandResult
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in signUpResendCode",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the signup/continue endpoint with the provided user attributes.
     */
    fun signUpSubmitUserAttributes(parameters: SignUpSubmitUserAttributesCommandParameters): SignUpSubmitUserAttributesCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signUpSubmitUserAttributes"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val signUpContinueApiResult = performSignUpSubmitUserAttributes(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )
            return signUpContinueApiResult.toSignUpSubmitUserAttributesCommandResult(oAuth2Strategy)
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in signUpSubmitUserAttributes",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the signup/continue endpoint with the provided password.
     */
    fun signUpSubmitPassword(parameters: SignUpSubmitPasswordCommandParameters): SignUpSubmitPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.signUpSubmitPassword"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            return performSignUpSubmitPassword(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            ).toSignUpSubmitPasswordCommandResult(oAuth2Strategy)
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in signUpSubmitPassword",
                e
            )
            throw e
        }
    }

    /**
     * Native-auth specific implementation of fetching a token from the cache, and/or refreshing it.
     * Main differences with standard implementation in [LocalMSALController] are:
     * - No scopes are passed in as part of the (developer provided) parameters. The scopes from the
     * Access Token cache are used to make the refresh token call.
     * - When the RT is expired or the refresh token call fails, a different exception is thrown.
     */
    @Throws(
        IOException::class,
        ClientException::class,
        ArgumentException::class,
        ServiceException::class
    )
    override fun acquireTokenSilent(
        parameters: SilentTokenCommandParameters
    ): AcquireTokenResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.acquireTokenSilent"
        )

        val acquireTokenSilentResult = AcquireTokenResult()

        // Validate original AcquireTokenNoScopesCommandParameters parameters
        parameters.validate()

        // Add default scopes
        val mergedScopes = addDefaultScopes(parameters)

        val parametersWithScopes = parameters
            .toBuilder()
            .scopes(mergedScopes)
            .build()

        val targetAccount: AccountRecord = getCachedAccountRecord(parametersWithScopes)

        // Build up params for Strategy construction
        val authScheme = parametersWithScopes.authenticationScheme
        val strategyParameters = OAuth2StrategyParameters.builder()
            .platformComponents(parametersWithScopes.platformComponents)
            .authenticationScheme(authScheme)
            .build()
        val strategy = parametersWithScopes.authority.createOAuth2Strategy(strategyParameters)

        val tokenCache = parametersWithScopes.oAuth2TokenCache

        val cacheRecords = tokenCache.loadWithAggregatedAccountData(
            parametersWithScopes.clientId,
            parametersWithScopes.applicationIdentifier,
            null,
            TextUtils.join(" ", parametersWithScopes.scopes),
            targetAccount,
            authScheme
        ) as List<ICacheRecord>

        // The first element is the 'fully-loaded' CacheRecord which may contain the AccountRecord,
        // AccessTokenRecord, RefreshTokenRecord, and IdTokenRecord... (if all of those artifacts exist)
        // subsequent CacheRecords represent other profiles (projections) of this principal in
        // other tenants. Those tokens will be 'sparse', meaning that their AT/RT will not be loaded
        val fullCacheRecord = cacheRecords[0]

        if (LibraryConfiguration.getInstance().isRefreshInEnabled &&
            fullCacheRecord.accessToken != null && fullCacheRecord.accessToken.refreshOnIsActive()
        ) {
            Logger.info(
                TAG,
                parameters.getCorrelationId(),
                "RefreshOn is active. This will extend your token usage in the rare case servers are not available."
            )
        }
        if (LibraryConfiguration.getInstance().isRefreshInEnabled &&
            fullCacheRecord.accessToken != null && fullCacheRecord.accessToken.shouldRefresh()
        ) {
            if (!fullCacheRecord.accessToken.isExpired) {
                setAcquireTokenResult(acquireTokenSilentResult, parametersWithScopes, cacheRecords)
                val refreshOnCommand =
                    RefreshOnCommand(parametersWithScopes, this.asControllerFactory(), PublicApiId.MSAL_REFRESH_ON)
                CommandDispatcher.submitAndForget(refreshOnCommand)
            } else {
                Logger.warn(
                    TAG,
                    parameters.getCorrelationId(),
                    "Access token is expired. Removing from cache..."
                )
                // Remove the expired token
                tokenCache.removeCredential(fullCacheRecord.accessToken)
                renewAccessToken(
                    parametersWithScopes,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    fullCacheRecord
                )
            }
        } else if (accessTokenIsNull(fullCacheRecord) ||
            refreshTokenIsNull(fullCacheRecord) ||
            parametersWithScopes.isForceRefresh ||
            !isRequestAuthorityRealmSameAsATRealm(
                parametersWithScopes.authority,
                fullCacheRecord.accessToken
            ) ||
            !strategy.validateCachedResult(authScheme, fullCacheRecord)
        ) {
            if (!refreshTokenIsNull(fullCacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                renewAccessToken(
                    parametersWithScopes,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    fullCacheRecord
                )
            } else {
                val exception = ServiceException(
                    ErrorStrings.NO_TOKENS_FOUND,
                    "No refresh token was found.",
                    null
                )

                Telemetry.emit(
                    ApiEndEvent()
                        .putException(exception)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
                )
                throw exception
            }
        } else if (fullCacheRecord.accessToken.isExpired) {
            Logger.warn(
                TAG,
                parameters.getCorrelationId(),
                "Access token is expired. Removing from cache..."
            )

            // Remove the expired token
            tokenCache.removeCredential(fullCacheRecord.accessToken)
            renewAccessToken(
                parametersWithScopes,
                acquireTokenSilentResult,
                tokenCache,
                strategy,
                fullCacheRecord
            )
        } else {
            Logger.verbose(
                TAG,
                parameters.getCorrelationId(),
                "Returning silent result"
            )
            setAcquireTokenResult(acquireTokenSilentResult, parametersWithScopes, cacheRecords)
        }
        Telemetry.emit(
            ApiEndEvent()
                .putResult(acquireTokenSilentResult)
                .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
        )
        return acquireTokenSilentResult
    }

    @Throws(ClientException::class)
    private fun setAcquireTokenResult(
        acquireTokenSilentResult: AcquireTokenResult,
        parametersWithScopes: SilentTokenCommandParameters,
        cacheRecords: List<ICacheRecord>
    ) {
        val fullCacheRecord = cacheRecords[0]
        acquireTokenSilentResult.localAuthenticationResult = LocalAuthenticationResult(
            finalizeCacheRecordForResult(
                fullCacheRecord,
                parametersWithScopes.authenticationScheme
            ),
            cacheRecords,
            SdkType.MSAL,
            true
        )
    }

    /**
     * Makes a call to the resetpassword/start endpoint with the provided username.
     * If successful, makes a call to the resetpassword/challenge endpoint, returning the result of that call.
     */
    fun resetPasswordStart(parameters: ResetPasswordStartCommandParameters): ResetPasswordStartCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.resetPasswordStart"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val resetPasswordStartApiResult = performResetPasswordStartCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (resetPasswordStartApiResult) {
                is ResetPasswordStartApiResult.Success -> {
                    performResetPasswordChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        continuationToken = resetPasswordStartApiResult.continuationToken,
                        correlationId = resetPasswordStartApiResult.correlationId
                    ).toResetPasswordStartCommandResult()
                }
                is ResetPasswordStartApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect(
                        correlationId = resetPasswordStartApiResult.correlationId
                    )
                }
                is ResetPasswordStartApiResult.UserNotFound -> {
                    ResetPasswordCommandResult.UserNotFound(
                        error = resetPasswordStartApiResult.error,
                        errorDescription = resetPasswordStartApiResult.errorDescription,
                        correlationId = resetPasswordStartApiResult.correlationId
                    )
                }
                is ResetPasswordStartApiResult.UnsupportedChallengeType, is ResetPasswordStartApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        parameters.getCorrelationId(),
                        "Unexpected result: ",
                        resetPasswordStartApiResult
                    )
                    resetPasswordStartApiResult as ApiErrorResult
                    INativeAuthCommandResult.APIError(
                        error = resetPasswordStartApiResult.error,
                        errorDescription = resetPasswordStartApiResult.errorDescription,
                        correlationId = resetPasswordStartApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in resetPasswordStart",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the resetpassword/continue endpoint with the provided code and password reset token.
     */
    fun resetPasswordSubmitCode(parameters: ResetPasswordSubmitCodeCommandParameters): ResetPasswordSubmitCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.resetPasswordSubmitCode"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val resetPasswordContinueApiResult = performResetPasswordContinueCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (resetPasswordContinueApiResult) {
                is ResetPasswordContinueApiResult.PasswordRequired -> {
                    ResetPasswordCommandResult.PasswordRequired(
                        continuationToken = resetPasswordContinueApiResult.continuationToken,
                        correlationId = resetPasswordContinueApiResult.correlationId
                    )
                }
                is ResetPasswordContinueApiResult.CodeIncorrect -> {
                    ResetPasswordCommandResult.IncorrectCode(
                        error = resetPasswordContinueApiResult.error,
                        errorDescription = resetPasswordContinueApiResult.errorDescription,
                        subError = resetPasswordContinueApiResult.subError,
                        correlationId = resetPasswordContinueApiResult.correlationId
                    )
                }
                is ResetPasswordContinueApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect(
                        correlationId = resetPasswordContinueApiResult.correlationId
                    )
                }
                is ResetPasswordContinueApiResult.ExpiredToken, is ResetPasswordContinueApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        "Unexpected result: ",
                        resetPasswordContinueApiResult
                    )
                    resetPasswordContinueApiResult as ApiErrorResult
                    INativeAuthCommandResult.APIError(
                        error = resetPasswordContinueApiResult.error,
                        errorDescription = resetPasswordContinueApiResult.errorDescription,
                        correlationId = resetPasswordContinueApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in resetPasswordSubmitCode",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the resetpassword/challenge endpoint to trigger a code to be re-sent.
     */
    fun resetPasswordResendCode(parameters: ResetPasswordResendCodeCommandParameters): ResetPasswordResendCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.resetPasswordResendCode"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val resetPasswordChallengeApiResult = performResetPasswordChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                continuationToken = parameters.continuationToken,
                correlationId = parameters.correlationId
            )

            return when (resetPasswordChallengeApiResult) {
                is ResetPasswordChallengeApiResult.CodeRequired -> {
                    ResetPasswordCommandResult.CodeRequired(
                        continuationToken = resetPasswordChallengeApiResult.continuationToken,
                        codeLength = resetPasswordChallengeApiResult.codeLength,
                        challengeTargetLabel = resetPasswordChallengeApiResult.challengeTargetLabel,
                        challengeChannel = resetPasswordChallengeApiResult.challengeChannel,
                        correlationId = resetPasswordChallengeApiResult.correlationId
                    )
                }
                is ResetPasswordChallengeApiResult.Redirect -> {
                    INativeAuthCommandResult.Redirect(
                        correlationId = resetPasswordChallengeApiResult.correlationId
                    )
                }
                is ResetPasswordChallengeApiResult.ExpiredToken,
                is ResetPasswordChallengeApiResult.UnsupportedChallengeType,
                is ResetPasswordChallengeApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        resetPasswordChallengeApiResult.correlationId,
                        "Unexpected result: ",
                        resetPasswordChallengeApiResult
                    )
                    resetPasswordChallengeApiResult as ApiErrorResult
                    INativeAuthCommandResult.APIError(
                        error = resetPasswordChallengeApiResult.error,
                        errorDescription = resetPasswordChallengeApiResult.errorDescription,
                        correlationId = resetPasswordChallengeApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in resetPasswordResendCode",
                e
            )
            throw e
        }
    }

    /**
     * Makes a call to the resetpassword/submit endpoint with the provided password.
     * If successful, calls the resetpassword/poll_completion endpoint, continuing to do so until a success response is returned, or the polling times out.
     */
    fun resetPasswordSubmitNewPassword(parameters: ResetPasswordSubmitNewPasswordCommandParameters): ResetPasswordSubmitNewPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.resetPasswordSubmitNewPassword"
        )

        try {
            val oAuth2Strategy = createOAuth2Strategy(parameters)

            val resetPasswordSubmitApiResult = performResetPasswordSubmitCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (resetPasswordSubmitApiResult) {
                is ResetPasswordSubmitApiResult.SubmitSuccess -> {
                    resetPasswordPollCompletion(
                        oAuth2Strategy = oAuth2Strategy,
                        continuationToken = resetPasswordSubmitApiResult.continuationToken,
                        pollIntervalInSeconds = resetPasswordSubmitApiResult.pollInterval,
                        correlationId = resetPasswordSubmitApiResult.correlationId
                    )
                }
                is ResetPasswordSubmitApiResult.PasswordInvalid -> {
                    ResetPasswordCommandResult.PasswordNotAccepted(
                        error = resetPasswordSubmitApiResult.error,
                        errorDescription = resetPasswordSubmitApiResult.errorDescription,
                        subError = resetPasswordSubmitApiResult.subError,
                        correlationId = resetPasswordSubmitApiResult.correlationId
                    )
                }

                is ResetPasswordSubmitApiResult.ExpiredToken,
                is ResetPasswordSubmitApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        resetPasswordSubmitApiResult.correlationId,
                        "Unexpected result: ",
                        resetPasswordSubmitApiResult
                    )
                    resetPasswordSubmitApiResult as ApiErrorResult
                    INativeAuthCommandResult.APIError(
                        error = resetPasswordSubmitApiResult.error,
                        errorDescription = resetPasswordSubmitApiResult.errorDescription,
                        correlationId = resetPasswordSubmitApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                parameters.getCorrelationId(),
                "Exception thrown in resetPasswordSubmitNewPassword",
                e
            )
            throw e
        }
    }

    private fun resetPasswordPollCompletion(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        continuationToken: String,
        correlationId: String,
        pollIntervalInSeconds: Int
    ): ResetPasswordSubmitNewPasswordCommandResult {
        fun pollCompletionTimedOut(startTime: Long): Boolean {
            val currentTime = System.currentTimeMillis()
            return currentTime - startTime > ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_IN_MILISECONDS
        }

        val methodTag = "$TAG:resetPasswordPollCompletion"

        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.resetPasswordPollCompletion"
        )

        try {
            val pollWaitInterval: Int = pollIntervalInSeconds * 1000   //Convert seconds into milliseconds

            var pollCompletionApiResult = performResetPasswordPollCompletionCall(
                oAuth2Strategy = oAuth2Strategy,
                continuationToken = continuationToken,
                correlationId = correlationId
            )

            val startTime = System.currentTimeMillis()

            while (pollCompletionApiResult is ResetPasswordPollCompletionApiResult.InProgress) {
                ThreadUtils.sleepSafely(
                    pollWaitInterval,
                    methodTag,
                    "Waiting between reset password polls"
                )

                if (pollCompletionTimedOut(startTime)) {
                    Logger.warn(
                        TAG,
                        pollCompletionApiResult.correlationId,
                        "Reset password completion timed out."
                    )
                    return ResetPasswordCommandResult.PasswordResetFailed(
                        error = ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_CODE,
                        errorDescription = ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_DESCRIPTION,
                        correlationId = pollCompletionApiResult.correlationId
                    )
                }

                pollCompletionApiResult = performResetPasswordPollCompletionCall(
                    oAuth2Strategy = oAuth2Strategy,
                    continuationToken = continuationToken,
                    correlationId = correlationId
                )
            }

            return when (pollCompletionApiResult) {
                is ResetPasswordPollCompletionApiResult.PollingFailed -> {
                    ResetPasswordCommandResult.PasswordResetFailed(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription,
                        correlationId = pollCompletionApiResult.correlationId
                    )
                }

                is ResetPasswordPollCompletionApiResult.PollingSucceeded -> {
                    ResetPasswordCommandResult.Complete(
                        continuationToken = pollCompletionApiResult.continuationToken,
                        expiresIn = pollCompletionApiResult.expiresIn,
                        correlationId = pollCompletionApiResult.correlationId
                    )
                }

                is ResetPasswordPollCompletionApiResult.InProgress -> {
                    Logger.warn(
                        TAG,
                        pollCompletionApiResult.correlationId,
                        "in_progress received after polling, illegal state"
                    )
                    // This should never be reached, theoretically
                    INativeAuthCommandResult.APIError(
                        error = "illegal_state",
                        errorDescription = "in_progress received after polling concluded, illegal state",
                        correlationId = pollCompletionApiResult.correlationId
                    )
                }
                is ResetPasswordPollCompletionApiResult.ExpiredToken,
                is ResetPasswordPollCompletionApiResult.UserNotFound,
                is ResetPasswordPollCompletionApiResult.PasswordInvalid,
                is ResetPasswordPollCompletionApiResult.UnknownError -> {
                    Logger.warnWithObject(
                        TAG,
                        pollCompletionApiResult.correlationId,
                        "Unexpected result: ",
                        pollCompletionApiResult
                    )
                    pollCompletionApiResult as ApiErrorResult
                    INativeAuthCommandResult.APIError(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription,
                        correlationId = pollCompletionApiResult.correlationId
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                TAG,
                correlationId,
                "Exception thrown in resetPasswordPollCompletion",
                e
            )
            throw e
        }
    }

    @VisibleForTesting
    fun performContinuationTokenTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInWithContinuationTokenCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performContinuationTokenTokenRequest"
        )
        return oAuth2Strategy.performContinuationTokenTokenRequest(
            parameters = parameters
        )
    }

    private fun performOOBTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performOOBTokenRequest"
        )
        return oAuth2Strategy.performOOBTokenRequest(
            parameters = parameters
        )
    }

    private fun performSubmitChallengeTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: MFASubmitChallengeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performOOBTokenRequest"
        )
        // Transform MFASubmitChallengeCommandParameters into SignInSubmitCodeCommandParameters,
        // as the API call is the same.
        val signInSubmitCodeCommandParameters = CommandUtil.createSignInSubmitCodeCommandParameters(
            parameters
        )
        return oAuth2Strategy.performOOBTokenRequest(
            parameters = signInSubmitCodeCommandParameters
        )
    }

    @VisibleForTesting
    fun performPasswordTokenCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performPasswordTokenCall"
        )
        return oAuth2Strategy.performPasswordTokenRequest(
            parameters = parameters
        )
    }

    private fun performSignInInitiateCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInStartCommandParameters,
    ): SignInInitiateApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performSignInInitiateCall"
        )
        return oAuth2Strategy.performSignInInitiate(
            parameters = parameters
        )
    }

    private fun performSignInDefaultChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        continuationToken: String,
        correlationId: String
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performSignInChallengeCall"
        )
        return oAuth2Strategy.performSignInDefaultChallenge(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    private fun performSignInSelectedChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        continuationToken: String,
        correlationId: String,
        challengeId: String
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performSignInSelectedChallengeCall"
        )
        return oAuth2Strategy.performSignInSelectedChallenge(
            continuationToken = continuationToken,
            correlationId = correlationId,
            challengeId = challengeId
        )
    }

    private fun performIntrospectCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        continuationToken: String,
        correlationId: String
    ): SignInIntrospectApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performIntrospectCall"
        )
        return oAuth2Strategy.performInIntrospect(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    private fun performResetPasswordStartCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordStartCommandParameters,
    ): ResetPasswordStartApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performResetPasswordStartCall"
        )
        return oAuth2Strategy.performResetPasswordStart(
            parameters = parameters
        )
    }

    private fun performResetPasswordChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        continuationToken: String,
        correlationId: String
    ): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performResetPasswordChallengeCall"
        )
        return oAuth2Strategy.performResetPasswordChallenge(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    private fun performResetPasswordContinueCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordSubmitCodeCommandParameters,
    ): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performResetPasswordContinueCall"
        )
        return oAuth2Strategy.performResetPasswordContinue(
            parameters = parameters
        )
    }

    private fun performResetPasswordSubmitCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordSubmitNewPasswordCommandParameters,
    ): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performResetPasswordSubmitCall"
        )
        return oAuth2Strategy.performResetPasswordSubmit(
            parameters = parameters,
        )
    }

    private fun performResetPasswordPollCompletionCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        continuationToken: String,
        correlationId: String
    ): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performResetPasswordPollCompletionCall"
        )
        return oAuth2Strategy.performResetPasswordPollCompletion(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    private fun saveAndReturnTokens(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: BaseSignInTokenCommandParameters,
        tokenApiResult: SignInTokenApiResult.Success
    ): SignInCommandResult.Complete {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parametersWithScopes.getCorrelationId(),
            methodName = "${TAG}.saveAndReturnTokens"
        )
        val records: List<ICacheRecord> = saveTokens(
            oAuth2Strategy as MicrosoftStsOAuth2Strategy,
            createAuthorizationRequest(
                strategy = oAuth2Strategy,
                scopes = parametersWithScopes.scopes,
                clientId = parametersWithScopes.clientId,
                applicationIdentifier = parametersWithScopes.applicationIdentifier
            ),
            tokenApiResult.tokenResponse,
            parametersWithScopes.oAuth2TokenCache
        )

        // The first element in the returned list is the item we *just* saved, the rest of
        // the elements are necessary to construct the full IAccount + TenantProfile
        val newestRecord = records[0]

        return SignInCommandResult.Complete(
            authenticationResult = LocalAuthenticationResult(
                finalizeCacheRecordForResult(
                    newestRecord,
                    parametersWithScopes.authenticationScheme
                ),
                records,
                SdkType.MSAL,
                false
            ),
            correlationId = tokenApiResult.correlationId
        )
    }

    private fun createAuthorizationRequest(
        strategy: NativeAuthOAuth2Strategy,
        scopes: List<String>,
        clientId: String,
        applicationIdentifier: String
    ): MicrosoftStsAuthorizationRequest {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.createAuthorizationRequest"
        )

        val builder = MicrosoftStsAuthorizationRequest.Builder()
        builder.setAuthority(URL(strategy.getAuthority()))
        builder.setClientId(clientId)
        builder.setScope(StringUtil.join(" ", scopes))
        builder.setApplicationIdentifier(applicationIdentifier)
        return builder.build()
    }

    private fun addDefaultScopes(scopes: List<String>?): List<String> {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = null,
            methodName = "${TAG}.createAuthorizationRequest"
        )
        val requestScopes = scopes?.toMutableList() ?: mutableListOf()
        requestScopes.addAll(AuthenticationConstants.DEFAULT_SCOPES)
        // sanitize empty and null scopes
        requestScopes.removeAll(listOf("", null))
        return requestScopes.toList()
    }

    private fun ResetPasswordChallengeApiResult.toResetPasswordStartCommandResult(): ResetPasswordStartCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = this.correlationId,
            methodName = "${TAG}.createAuthorizationRequest"
        )
        return when (this) {
            is ResetPasswordChallengeApiResult.CodeRequired -> {
                ResetPasswordCommandResult.CodeRequired(
                    continuationToken = this.continuationToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel,
                    correlationId = this.correlationId
                )
            }
            is ResetPasswordChallengeApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = this.correlationId
                )
            }
            is ResetPasswordChallengeApiResult.ExpiredToken -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Expire token result: ",
                    this
                )
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
            is ResetPasswordChallengeApiResult.UnsupportedChallengeType -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unsupported challenge type: ",
                    this
                )
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
            is ResetPasswordChallengeApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unexpected result: ",
                    this
                )
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
        }
    }

    @VisibleForTesting
    fun performSignUpStartUsingPasswordRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignUpStartCommandParameters
    ): SignUpStartApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performSignUpStartUsingPasswordRequest"
        )
        return oAuth2Strategy.performSignUpStart(
            commandParameters = parameters
        )
    }

    private fun performSignUpChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        continuationToken: String,
        correlationId: String
    ): SignUpChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = correlationId,
            methodName = "${TAG}.performSignUpChallengeCall"
        )
        return oAuth2Strategy.performSignUpChallenge(
            continuationToken = continuationToken,
            correlationId = correlationId
        )
    }

    private fun performSignUpSubmitCode(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignUpSubmitCodeCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performSignUpSubmitCode"
        )
        return oAuth2Strategy.performSignUpSubmitCode(commandParameters = parameters)
    }
    private fun performSignUpSubmitPassword(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignUpSubmitPasswordCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performSignUpSubmitPassword"
        )
        return oAuth2Strategy.performSignUpSubmitPassword(commandParameters = parameters)
    }

    @VisibleForTesting
    fun performSignUpSubmitUserAttributes(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignUpSubmitUserAttributesCommandParameters
    ): SignUpContinueApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "${TAG}.performSignUpSubmitUserAttributes"
        )
        return oAuth2Strategy.performSignUpSubmitUserAttributes(commandParameters = parameters)
    }

    /**
     * When the call to /signup/challenge is made from the function processing the result of
     * submit attributes call, the result is converted to [SignUpSubmitUserAttributesCommandResult]
     * object to match the return type of the calling function.
     */
    private fun SignUpChallengeApiResult.toSignUpSubmitUserAttrsCommandResult(): SignUpSubmitUserAttributesCommandResult {
        return when (this) {
            is SignUpChallengeApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = this.correlationId
                )
            }
            is SignUpChallengeApiResult.ExpiredToken, is SignUpChallengeApiResult.UnsupportedChallengeType,
            is SignUpChallengeApiResult.OOBRequired, is SignUpChallengeApiResult.PasswordRequired,
            is SignUpChallengeApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unexpected result: ",
                    this
                )
                this as ApiErrorResult
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
        }
    }

    /**
     * When the call to /signup/challenge is made from the function processing the result of
     * signup call, the result is converted to [SignUpStartCommandResult] object to
     * match the return type of the calling function.
     */
    private fun SignUpChallengeApiResult.toSignUpStartCommandResult(): SignUpStartCommandResult {
        return when (this) {
            is SignUpChallengeApiResult.OOBRequired -> {
                SignUpCommandResult.CodeRequired(
                    continuationToken = this.continuationToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel,
                    correlationId = this.correlationId
                )
            }
            is SignUpChallengeApiResult.PasswordRequired -> {
                SignUpCommandResult.PasswordRequired(
                    continuationToken = this.continuationToken,
                    correlationId = this.correlationId
                )
            }
            is SignUpChallengeApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = this.correlationId
                )
            }
            is SignUpChallengeApiResult.ExpiredToken, is SignUpChallengeApiResult.UnsupportedChallengeType,
            is SignUpChallengeApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unexpected result: ",
                    this
                )
                this as ApiErrorResult
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
        }
    }

    /**
     * Signup continue API is used to submit the oob code. This method converts the result of the API
     * to a more concrete object of type SignUpSubmitCodeCommandResult.
     */
    private fun SignUpContinueApiResult.toSignUpSubmitCodeCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignUpSubmitCodeCommandResult {
        return when (this) {
            is SignUpContinueApiResult.Success -> {
                SignUpCommandResult.Complete(
                    continuationToken = this.continuationToken,
                    expiresIn = this.expiresIn,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.ExpiredToken -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Expire token result: ",
                    this
                )
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.UsernameAlreadyExists -> {
                SignUpCommandResult.UsernameAlreadyExists(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    continuationToken = this.continuationToken,
                    error = this.error,
                    errorDescription = this.errorDescription,
                    requiredAttributes = this.requiredAttributes,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.CredentialRequired -> {
                return performSignUpChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    continuationToken = this.continuationToken,
                    correlationId = this.correlationId
                ).toSignUpStartCommandResult() as SignUpSubmitCodeCommandResult
            }
            is SignUpContinueApiResult.InvalidOOBValue -> {
                SignUpCommandResult.InvalidCode(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    subError = this.subError,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unexpected result: ",
                    this
                )
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }

            is SignUpContinueApiResult.InvalidAttributes, is SignUpContinueApiResult.InvalidPassword -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unexpected result: ",
                    this
                )
                this as ApiErrorResult
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
        }
    }

    private fun SignUpContinueApiResult.toSignUpSubmitUserAttributesCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignUpSubmitUserAttributesCommandResult {
        return when (this) {
            is SignUpContinueApiResult.Success -> {
                SignUpCommandResult.Complete(
                    continuationToken = this.continuationToken,
                    expiresIn = this.expiresIn,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.UsernameAlreadyExists -> {
                SignUpCommandResult.UsernameAlreadyExists(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    continuationToken = this.continuationToken,
                    error = this.error,
                    errorDescription = this.errorDescription,
                    requiredAttributes = this.requiredAttributes,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.CredentialRequired -> {
                return performSignUpChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    continuationToken = this.continuationToken,
                    correlationId = this.correlationId
                ).toSignUpSubmitUserAttrsCommandResult()
            }
            is SignUpContinueApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.InvalidAttributes -> {
                SignUpCommandResult.InvalidAttributes(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    invalidAttributes = this.invalidAttributes,
                    correlationId = this.correlationId
                )
            }

            is SignUpContinueApiResult.InvalidOOBValue, is SignUpContinueApiResult.InvalidPassword,
            is SignUpContinueApiResult.ExpiredToken, is SignUpContinueApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Expire token result: ",
                    this
                )
                this as ApiErrorResult
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
        }
    }

    private fun SignUpContinueApiResult.toSignUpSubmitPasswordCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignUpSubmitPasswordCommandResult {
        return when (this) {
            is SignUpContinueApiResult.Success -> {
                SignUpCommandResult.Complete(
                    continuationToken = this.continuationToken,
                    expiresIn = this.expiresIn,
                    correlationId = this.correlationId
                )
            }

            is SignUpContinueApiResult.UsernameAlreadyExists -> {
                SignUpCommandResult.UsernameAlreadyExists(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    continuationToken = this.continuationToken,
                    error = this.error,
                    errorDescription = this.errorDescription,
                    requiredAttributes = this.requiredAttributes,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.CredentialRequired -> {
                return performSignUpChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    continuationToken = this.continuationToken,
                    correlationId = this.correlationId
                ).toSignUpStartCommandResult() as SignUpSubmitPasswordCommandResult
            }
            is SignUpContinueApiResult.InvalidPassword -> {
                SignUpCommandResult.InvalidPassword(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    subError = this.subError,
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = this.correlationId
                )
            }
            is SignUpContinueApiResult.ExpiredToken, is SignUpContinueApiResult.InvalidOOBValue,
            is SignUpContinueApiResult.InvalidAttributes, is SignUpContinueApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Error in signup continue result: ",
                    this
                )
                this as ApiErrorResult
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    correlationId = this.correlationId
                )
            }
        }
    }

    private fun SignInTokenApiResult.toSignInStartCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: SignInStartCommandParameters,
    ): SignInStartCommandResult {
        return when (this) {
            is SignInTokenApiResult.InvalidCredentials -> {
                SignInCommandResult.InvalidCredentials(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    errorCodes = this.errorCodes,
                    correlationId = this.correlationId
                )
            }
            is SignInTokenApiResult.Success -> {
                saveAndReturnTokens(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes,
                    tokenApiResult = this
                )
            }
            is SignInTokenApiResult.MFARequired -> {
                SignInCommandResult.MFARequired(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    continuationToken = this.continuationToken,
                    subError = this.subError,
                    errorCodes = this.errorCodes,
                    correlationId = this.correlationId
                )
            }
            is SignInTokenApiResult.CodeIncorrect,
            is SignInTokenApiResult.InvalidAuthenticationType, is SignInTokenApiResult.UserNotFound,
            is SignInTokenApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unexpected result: ",
                    this
                )
                this as ApiErrorResult
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    errorCodes = this.errorCodes,
                    correlationId = this.correlationId
                )
            }
        }
    }

    private fun SignInTokenApiResult.toSignInSubmitPasswordCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: SignInSubmitPasswordCommandParameters,
    ): SignInSubmitPasswordCommandResult {
        return when (this) {
            is SignInTokenApiResult.InvalidCredentials -> {
                SignInCommandResult.InvalidCredentials(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    errorCodes = this.errorCodes,
                    correlationId = this.correlationId
                )
            }
            is SignInTokenApiResult.Success -> {
                saveAndReturnTokens(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes,
                    tokenApiResult = this
                )
            }
            is SignInTokenApiResult.MFARequired -> {
                SignInCommandResult.MFARequired(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    continuationToken = this.continuationToken,
                    subError = this.subError,
                    errorCodes = this.errorCodes,
                    correlationId = this.correlationId
                )
            }
            is SignInTokenApiResult.UserNotFound, is SignInTokenApiResult.CodeIncorrect,
            is SignInTokenApiResult.InvalidAuthenticationType, is SignInTokenApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unexpected result: ",
                    this
                )
                this as ApiErrorResult
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    errorCodes = this.errorCodes,
                    correlationId = this.correlationId
                )
            }
        }
    }

    private fun SignInIntrospectApiResult.toMFAChallengeCommandResult(): MFAChallengeCommandResult {
        return when (this) {
            is SignInIntrospectApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = this.correlationId
                )
            }
            is SignInIntrospectApiResult.Success -> {
                MFACommandResult.SelectionRequired(
                    correlationId = this.correlationId,
                    continuationToken = this.continuationToken,
                    authMethods = this.methods
                )
            }
            is SignInIntrospectApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    this.correlationId,
                    "Unexpected result: ",
                    this
                )
                INativeAuthCommandResult.APIError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    errorCodes = this.errorCodes,
                    correlationId = this.correlationId
                )
            }
        }
    }

    @VisibleForTesting
    fun processSignInInitiateApiResult(
        initiateApiResult: SignInInitiateApiResult,
        parametersWithScopes: SignInStartCommandParameters? = null,
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        usePassword: Boolean = false
    ): SignInStartCommandResult {
        return when (initiateApiResult) {
            is SignInInitiateApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = initiateApiResult.correlationId
                )
            }
            is SignInInitiateApiResult.Success -> {
                val signInChallengeResult = performSignInDefaultChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    continuationToken = initiateApiResult.continuationToken,
                    correlationId = initiateApiResult.correlationId
                )
                return processSignInChallengeCall(
                    result = signInChallengeResult,
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes,
                    usePassword = usePassword
                )
            }
            is SignInInitiateApiResult.UserNotFound -> {
                SignInCommandResult.UserNotFound(
                    error = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription,
                    errorCodes = initiateApiResult.errorCodes,
                    correlationId = initiateApiResult.correlationId
                )
            }
            is SignInInitiateApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    initiateApiResult.correlationId,
                    "Unexpected result: ",
                    initiateApiResult
                )
                INativeAuthCommandResult.APIError(
                    error = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription,
                    errorCodes = initiateApiResult.errorCodes,
                    correlationId = initiateApiResult.correlationId
                )
            }
        }
    }

    private fun processSignInChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: SignInStartCommandParameters?,
        result: SignInChallengeApiResult,
        usePassword: Boolean
    ): SignInStartCommandResult {
        return when (result) {
            is SignInChallengeApiResult.OOBRequired -> {
                SignInCommandResult.CodeRequired(
                    continuationToken = result.continuationToken,
                    codeLength = result.codeLength,
                    challengeTargetLabel = result.challengeTargetLabel,
                    challengeChannel = result.challengeChannel,
                    correlationId = result.correlationId
                )
            }
            is SignInChallengeApiResult.PasswordRequired -> {
                if (usePassword) {
                    if (parametersWithScopes == null) {
                        // In password flows, we will be sending the password and scopes to the token
                        // endpoint. So we need this parameter to be set.
                        throw IllegalArgumentException("Parameters must be provided in password flow")
                    }

                    val signInSubmitPasswordCommandParameters =
                        CommandUtil.createSignInSubmitPasswordCommandParameters(
                            parametersWithScopes,
                            result.correlationId,
                            result.continuationToken
                        )
                    try {
                        return performPasswordTokenCall(
                            oAuth2Strategy = oAuth2Strategy,
                            parameters = signInSubmitPasswordCommandParameters
                        ).toSignInStartCommandResult(
                            oAuth2Strategy = oAuth2Strategy,
                            parametersWithScopes = parametersWithScopes
                        )
                    } finally {
                        StringUtil.overwriteWithNull(signInSubmitPasswordCommandParameters.password)
                    }
                } else {
                    SignInCommandResult.PasswordRequired(
                        continuationToken = result.continuationToken,
                        correlationId = result.correlationId
                    )
                }
            }
            is SignInChallengeApiResult.Redirect -> {
                INativeAuthCommandResult.Redirect(
                    correlationId = result.correlationId
                )
            }
            is SignInChallengeApiResult.IntrospectRequired -> {
                INativeAuthCommandResult.APIError(
                    error = result.error,
                    errorDescription = result.errorDescription,
                    errorCodes = result.errorCodes,
                    correlationId = result.correlationId
                )
            }
            is SignInChallengeApiResult.UnknownError -> {
                Logger.warnWithObject(
                    TAG,
                    result.correlationId,
                    "Unexpected result: ",
                    result
                )
                INativeAuthCommandResult.APIError(
                    error = result.error,
                    errorDescription = result.errorDescription,
                    errorCodes = result.errorCodes,
                    correlationId = result.correlationId
                )
            }
        }
    }

    private fun createOAuth2Strategy(parameters: BaseNativeAuthCommandParameters): NativeAuthOAuth2Strategy {
        val strategyParameters = OAuth2StrategyParameters.builder()
            .platformComponents(parameters.platformComponents)
            .challengeTypes(parameters.challengeType)
            .build()

        return parameters
            .authority
            .createOAuth2Strategy(strategyParameters)
    }
}
