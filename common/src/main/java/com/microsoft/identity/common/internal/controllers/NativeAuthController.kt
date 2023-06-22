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
package com.microsoft.identity.common.internal.controllers

import com.microsoft.identity.common.internal.commands.RefreshOnCommand
import com.microsoft.identity.common.internal.commands.ResetPasswordSubmitNewPasswordCommand
import com.microsoft.identity.common.internal.telemetry.Telemetry
import com.microsoft.identity.common.internal.telemetry.events.ApiEndEvent
import com.microsoft.identity.common.internal.util.CommandUtil
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.AcquireTokenNoFixedScopesCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseNativeAuthCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.ResetPasswordSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInWithSLTCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartUsingPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitUserAttributesCommandParameters
import com.microsoft.identity.common.java.configuration.LibraryConfiguration
import com.microsoft.identity.common.java.controllers.CommandDispatcher
import com.microsoft.identity.common.java.controllers.results.CommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.ResetPasswordSubmitNewPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInStartCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInSubmitPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInWithSLTCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpStartCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitUserAttributesCommandResult
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.eststelemetry.PublicApiId
import com.microsoft.identity.common.java.exception.ArgumentException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.exception.RefreshTokenNotFoundException
import com.microsoft.identity.common.java.exception.ServiceException
import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
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
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.result.AcquireTokenResult
import com.microsoft.identity.common.java.result.LocalAuthenticationResult
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.util.ThreadUtils
import lombok.EqualsAndHashCode
import java.io.IOException
import java.net.URL

/**
 * The implementation of MSAL Controller for Native Authentication.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
class NativeAuthController : BaseNativeAuthController() {

    companion object {
        private val TAG = NativeAuthController::class.java.simpleName
    }

    fun signInStart(parameters: BaseSignInStartCommandParameters): SignInStartCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .challengeTypes(parameters.challengeType)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val isROPCCall = parameters is SignInStartUsingPasswordCommandParameters

            return if (isROPCCall) {
                signInStartROPC(
                    parameters as SignInStartUsingPasswordCommandParameters,
                    oAuth2Strategy
                )
            } else {
                signInStartNonROPC(parameters as SignInStartCommandParameters, oAuth2Strategy)
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    private fun signInStartROPC(
        parameters: SignInStartUsingPasswordCommandParameters,
        oAuth2Strategy: NativeAuthOAuth2Strategy,
    ): SignInStartCommandResult {
        LogSession.logMethodCall(tag = TAG)

        val mergedScopes = addDefaultScopes(parameters.scopes)
        val parametersWithScopes =
            CommandUtil.createSignInStartUsingPasswordCommandParametersWithScopes(
                parameters,
                mergedScopes
            )

        val tokenApiResult = performROPCTokenRequest(
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

            is SignInTokenApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $tokenApiResult"
                )
                CommandResult.UnknownError(
                    error = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription,
                    details = tokenApiResult.details,
                    errorCodes = tokenApiResult.errorCodes
                )
            }

            is SignInTokenApiResult.InvalidCredentials -> {
                SignInCommandResult.InvalidCredentials(
                    error = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription,
                    errorCodes = tokenApiResult.errorCodes
                )
            }

            is SignInTokenApiResult.UserNotFound -> {
                SignInCommandResult.UserNotFound(
                    error = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription,
                    errorCodes = tokenApiResult.errorCodes
                )
            }

            is SignInTokenApiResult.InvalidAuthenticationType -> {
                signInStartAfterInvalidAuthenticationMethod(
                    signInStartCommandParameters = parameters,
                    oAuth2Strategy = oAuth2Strategy
                )
            }

            is SignInTokenApiResult.CodeIncorrect -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $tokenApiResult"
                )
                CommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $tokenApiResult",
                    errorCodes = tokenApiResult.errorCodes
                )
            }
        }
    }

    private fun signInStartNonROPC(
        parameters: SignInStartCommandParameters,
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignInStartCommandResult {
        LogSession.logMethodCall(tag = TAG)

        val mergedScopes = addDefaultScopes(parameters.scopes)
        val parametersWithScopes = CommandUtil.createSignInStartCommandParametersWithScopes(
            parameters,
            mergedScopes
        )

        val initiateApiResult = performSignInInitiateCall(
            oAuth2Strategy = oAuth2Strategy,
            parameters = parametersWithScopes
        )

        return processSignInInitiateApiResult(
            initiateApiResult = initiateApiResult,
            oAuth2Strategy = oAuth2Strategy
        )
    }

    fun signInWithSLT(parameters: SignInWithSLTCommandParameters): SignInWithSLTCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val mergedScopes = addDefaultScopes(parameters.scopes)
            val parametersWithScopes = CommandUtil.createSignInWithSLTCommandParametersWithScopes(
                parameters,
                mergedScopes
            )

            val tokenApiResult = performSLTTokenRequest(
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

                is SignInTokenApiResult.UnknownError -> {
                    return CommandResult.UnknownError(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        details = tokenApiResult.details,
                        errorCodes = tokenApiResult.errorCodes
                    )
                }

                is SignInTokenApiResult.InvalidAuthenticationType -> {
                    return SignInCommandResult.InvalidAuthenticationType(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        errorCodes = tokenApiResult.errorCodes
                    )
                }

                is SignInTokenApiResult.CodeIncorrect -> {
                    // This shouldn't be possible in SLT, throw unknown error
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $tokenApiResult"
                    )
                    return CommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $tokenApiResult",
                        errorCodes = tokenApiResult.errorCodes
                    )
                }
                is SignInTokenApiResult.UserNotFound -> {
                    // This shouldn't be possible in SLT, throw unknown error
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $tokenApiResult"
                    )
                    return CommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $tokenApiResult",
                        errorCodes = tokenApiResult.errorCodes
                    )
                }
                is SignInTokenApiResult.InvalidCredentials -> {
                    // This shouldn't be possible in SLT, throw unknown error
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $tokenApiResult"
                    )
                    return CommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $tokenApiResult",
                        errorCodes = tokenApiResult.errorCodes
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    fun signInSubmitCode(parameters: SignInSubmitCodeCommandParameters): SignInSubmitCodeCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            // Add default scopes
            val mergedScopes: List<String> = addDefaultScopes(parameters.scopes)

            val parametersWithScopes =
                CommandUtil.createSignInSubmitCodeCommandParametersWithScopes(
                    parameters,
                    mergedScopes
                )

            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parametersWithScopes
                .authority
                .createOAuth2Strategy(strategyParameters)

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
                        errorCodes = tokenApiResult.errorCodes
                    )
                }

                is SignInTokenApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $tokenApiResult"
                    )
                    CommandResult.UnknownError(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        details = tokenApiResult.details,
                        errorCodes = tokenApiResult.errorCodes
                    )
                }

                is SignInTokenApiResult.InvalidAuthenticationType -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $tokenApiResult"
                    )
                    CommandResult.UnknownError(
                        error = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription,
                        errorCodes = tokenApiResult.errorCodes
                    )
                }

                is SignInTokenApiResult.InvalidCredentials -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $tokenApiResult"
                    )
                    CommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $tokenApiResult",
                        errorCodes = tokenApiResult.errorCodes
                    )
                }
                is SignInTokenApiResult.UserNotFound -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $tokenApiResult"
                    )
                    CommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $tokenApiResult",
                        errorCodes = tokenApiResult.errorCodes
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    fun signInResendCode(parameters: SignInResendCodeCommandParameters): SignInResendCodeCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .challengeTypes(parameters.challengeType)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val result = performSignInChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                credentialToken = parameters.credentialToken
            )
            return when (result) {
                is SignInChallengeApiResult.OOBRequired -> {
                    SignInCommandResult.CodeRequired(
                        credentialToken = result.credentialToken,
                        codeLength = result.codeLength,
                        challengeTargetLabel = result.challengeTargetLabel,
                        challengeChannel = result.challengeChannel,
                    )
                }

                is SignInChallengeApiResult.PasswordRequired -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $result"
                    )
                    CommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result"
                    )
                }

                SignInChallengeApiResult.Redirect -> {
                    CommandResult.Redirect()
                }

                is SignInChallengeApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $result"
                    )
                    CommandResult.UnknownError(
                        error = result.error,
                        errorDescription = result.errorDescription,
                        details = result.details,
                        errorCodes = result.errorCodes
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    fun signInSubmitPassword(parameters: SignInSubmitPasswordCommandParameters): SignInSubmitPasswordCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .challengeTypes(parameters.challengeType)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val mergedScopes = addDefaultScopes(parameters.scopes)
            val parametersWithScopes =
                CommandUtil.createSignInSubmitPasswordCommandParametersWithScopes(
                    parameters,
                    mergedScopes
                )

            val result = performPasswordTokenCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parametersWithScopes
            )
            return when (result) {
                is SignInTokenApiResult.InvalidCredentials -> {
                    SignInCommandResult.InvalidCredentials(
                        error = result.error,
                        errorDescription = result.errorDescription,
                        errorCodes = result.errorCodes
                    )
                }

                is SignInTokenApiResult.Success -> {
                    saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = result
                    )
                }

                is SignInTokenApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $result"
                    )
                    CommandResult.UnknownError(
                        error = result.error,
                        errorDescription = result.errorDescription,
                        details = result.details,
                        errorCodes = result.errorCodes
                    )
                }

                is SignInTokenApiResult.InvalidAuthenticationType -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $result"
                    )
                    CommandResult.UnknownError(
                        error = result.error,
                        errorDescription = result.errorDescription,
                        errorCodes = result.errorCodes
                    )
                }

                is SignInTokenApiResult.CodeIncorrect -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $result"
                    )
                    CommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result",
                        errorCodes = result.errorCodes
                    )
                }

                is SignInTokenApiResult.UserNotFound -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $result"
                    )
                    CommandResult.UnknownError(
                        error = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result",
                        errorCodes = result.errorCodes
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    fun resetPasswordStart(parameters: ResetPasswordStartCommandParameters): ResetPasswordStartCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .challengeTypes(parameters.challengeType)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val startApiResult = performResetPasswordStartCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (startApiResult) {
                ResetPasswordStartApiResult.Redirect -> {
                    CommandResult.Redirect()
                }

                is ResetPasswordStartApiResult.Success -> {
                    performResetPasswordChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        passwordResetToken = startApiResult.passwordResetToken
                    ).toResetPasswordStartCommandResult()
                }

                is ResetPasswordStartApiResult.UserNotFound -> {
                    ResetPasswordCommandResult.UserNotFound(
                        error = startApiResult.error,
                        errorDescription = startApiResult.errorDescription
                    )
                }

                is ResetPasswordStartApiResult.UnsupportedChallengeType -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $startApiResult"
                    )
                    CommandResult.UnknownError(
                        error = startApiResult.error,
                        errorDescription = startApiResult.errorDescription
                    )
                }

                is ResetPasswordStartApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $startApiResult"
                    )
                    CommandResult.UnknownError(
                        error = startApiResult.error,
                        errorDescription = startApiResult.errorDescription,
                        details = startApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    fun resetPasswordSubmitCode(parameters: ResetPasswordSubmitCodeCommandParameters): ResetPasswordSubmitCodeCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val continueApiResult = performResetPasswordContinueCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (continueApiResult) {
                ResetPasswordContinueApiResult.Redirect -> {
                    CommandResult.Redirect()
                }

                is ResetPasswordContinueApiResult.PasswordRequired -> {
                    ResetPasswordCommandResult.PasswordRequired(
                        passwordSubmitToken = continueApiResult.passwordSubmitToken
                    )
                }

                is ResetPasswordContinueApiResult.CodeIncorrect -> {
                    ResetPasswordCommandResult.IncorrectCode(
                        error = continueApiResult.error,
                        errorDescription = continueApiResult.errorDescription
                    )
                }

                is ResetPasswordContinueApiResult.ExpiredToken -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Expire token result: $this"
                    )
                    CommandResult.UnknownError(
                        error = continueApiResult.error,
                        errorDescription = continueApiResult.errorDescription
                    )
                }

                is ResetPasswordContinueApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $continueApiResult"
                    )

                    CommandResult.UnknownError(
                        error = continueApiResult.error,
                        errorDescription = continueApiResult.errorDescription,
                        details = continueApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    fun resetPasswordResendCode(parameters: ResetPasswordResendCodeCommandParameters): ResetPasswordResendCodeCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val resetPasswordChallengeApiResult = performResetPasswordChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                passwordResetToken = parameters.passwordResetToken
            )

            return when (resetPasswordChallengeApiResult) {
                is ResetPasswordChallengeApiResult.CodeRequired -> {
                    ResetPasswordCommandResult.CodeRequired(
                        passwordResetToken = resetPasswordChallengeApiResult.passwordResetToken,
                        codeLength = resetPasswordChallengeApiResult.codeLength,
                        challengeTargetLabel = resetPasswordChallengeApiResult.challengeTargetLabel,
                        challengeChannel = resetPasswordChallengeApiResult.challengeChannel
                    )
                }

                ResetPasswordChallengeApiResult.Redirect -> {
                    CommandResult.Redirect()
                }

                is ResetPasswordChallengeApiResult.ExpiredToken -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Expire token result: $this"
                    )
                    CommandResult.UnknownError(
                        error = resetPasswordChallengeApiResult.error,
                        errorDescription = resetPasswordChallengeApiResult.errorDescription
                    )
                }

                is ResetPasswordChallengeApiResult.UnsupportedChallengeType -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $resetPasswordChallengeApiResult"
                    )
                    CommandResult.UnknownError(
                        error = resetPasswordChallengeApiResult.error,
                        errorDescription = resetPasswordChallengeApiResult.errorDescription
                    )
                }

                is ResetPasswordChallengeApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $resetPasswordChallengeApiResult"
                    )
                    CommandResult.UnknownError(
                        error = resetPasswordChallengeApiResult.error,
                        errorDescription = resetPasswordChallengeApiResult.errorDescription,
                        details = resetPasswordChallengeApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    fun resetPasswordSubmitNewPassword(parameters: ResetPasswordSubmitNewPasswordCommandParameters): ResetPasswordSubmitNewPasswordCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val submitApiResult = performResetPasswordSubmitCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (submitApiResult) {
                is ResetPasswordSubmitApiResult.SubmitSuccess -> {
                    resetPasswordPollCompletion(
                        oAuth2Strategy = oAuth2Strategy,
                        passwordResetToken = submitApiResult.passwordResetToken,
                        pollIntervalInSeconds = submitApiResult.pollInterval
                    )
                }

                is ResetPasswordSubmitApiResult.PasswordInvalid -> {
                    ResetPasswordCommandResult.PasswordNotAccepted(
                        error = submitApiResult.error,
                        errorDescription = submitApiResult.errorDescription
                    )
                }

                is ResetPasswordSubmitApiResult.ExpiredToken -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Expire token result: $this"
                    )
                    CommandResult.UnknownError(
                        error = submitApiResult.error,
                        errorDescription = submitApiResult.errorDescription
                    )
                }

                is ResetPasswordSubmitApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $submitApiResult"
                    )

                    CommandResult.UnknownError(
                        error = submitApiResult.error,
                        errorDescription = submitApiResult.errorDescription,
                        details = submitApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    private fun resetPasswordPollCompletion(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String,
        pollIntervalInSeconds: Int?
    ): ResetPasswordSubmitNewPasswordCommandResult {
        fun pollCompletionTimedOut(startTime: Long): Boolean {
            val currentTime = System.currentTimeMillis()
            return currentTime - startTime > ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_IN_MILISECONDS
        }

        fun pollIntervalIsAppropriate(pollIntervalInSeconds: Int?): Boolean {
            return pollIntervalInSeconds != null && pollIntervalInSeconds <= 15 && pollIntervalInSeconds >= 1
        }

        val methodTag = "$TAG:resetPasswordPollCompletion"

        LogSession.logMethodCall(tag = TAG)

        try {
            val pollWaitInterval: Int = if (!pollIntervalIsAppropriate(pollIntervalInSeconds)) {
                ResetPasswordSubmitNewPasswordCommand.DEFAULT_POLL_COMPLETION_INTERVAL_IN_MILISECONDS
            } else {
                pollIntervalInSeconds!! * 1000
            }

            var pollCompletionApiResult = performResetPasswordPollCompletionCall(
                oAuth2Strategy = oAuth2Strategy,
                passwordResetToken = passwordResetToken
            )

            val startTime = System.currentTimeMillis()

            while (pollCompletionApiResult is ResetPasswordPollCompletionApiResult.InProgress) {
                // TODO: This will use coroutines, most likely shouldn't use thread sleep here
                ThreadUtils.sleepSafely(
                    pollWaitInterval,
                    methodTag,
                    "Waiting between reset password polls"
                )

                if (pollCompletionTimedOut(startTime)) {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Reset password completion timed out."
                    )
                    return ResetPasswordCommandResult.PasswordResetFailed(
                        error = ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_CODE,
                        errorDescription = ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_DESCRIPTION
                    )
                }

                LogSession.logMethodCall(tag = TAG)

                pollCompletionApiResult = performResetPasswordPollCompletionCall(
                    oAuth2Strategy = oAuth2Strategy,
                    passwordResetToken = passwordResetToken
                )
            }

            return when (pollCompletionApiResult) {
                is ResetPasswordPollCompletionApiResult.PollingFailed -> {
                    ResetPasswordCommandResult.PasswordResetFailed(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription
                    )
                }

                is ResetPasswordPollCompletionApiResult.PollingSucceeded -> {
                    ResetPasswordCommandResult.Complete
                }

                is ResetPasswordPollCompletionApiResult.InProgress -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "in_progress received after polling, illegal state"
                    )
                    // This should never be reached, theoretically
                    CommandResult.UnknownError(
                        error = "illegal_state",
                        errorDescription = "in_progress received after polling concluded, illegal state"
                    )
                }

                is ResetPasswordPollCompletionApiResult.ExpiredToken -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Expire token result: $this"
                    )
                    CommandResult.UnknownError(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription
                    )
                }

                is ResetPasswordPollCompletionApiResult.UserNotFound -> {
                    // This should be caught earlier in the flow
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $pollCompletionApiResult"
                    )

                    CommandResult.UnknownError(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription
                    )
                }

                is ResetPasswordPollCompletionApiResult.PasswordInvalid -> {
                    // This should be caught in /submit
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $pollCompletionApiResult"
                    )

                    CommandResult.UnknownError(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription
                    )
                }

                is ResetPasswordPollCompletionApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $pollCompletionApiResult"
                    )

                    CommandResult.UnknownError(
                        error = pollCompletionApiResult.error,
                        errorDescription = pollCompletionApiResult.errorDescription,
                        details = pollCompletionApiResult.details
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.logException(tag = TAG, throwable = e)
            throw e
        }
    }

    private fun performROPCTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInStartUsingPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performROPCTokenRequest(
            parameters = parameters
        )
    }

    private fun performSLTTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInWithSLTCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performSLTTokenRequest(
            parameters = parameters
        )
    }

    private fun performOOBTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performOOBTokenRequest(
            parameters = parameters
        )
    }

    private fun performPasswordTokenCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performPasswordTokenRequest(
            parameters = parameters
        )
    }

    private fun performSignInInitiateCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInStartCommandParameters,
    ): SignInInitiateApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performSignInInitiate(
            parameters = parameters
        )
    }

    private fun performSignInChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        credentialToken: String
    ): SignInChallengeApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performSignInChallenge(credentialToken = credentialToken)
    }

    private fun performResetPasswordStartCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordStartCommandParameters,
    ): ResetPasswordStartApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performResetPasswordStart(
            parameters = parameters
        )
    }

    private fun performResetPasswordChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String
    ): ResetPasswordChallengeApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performResetPasswordChallenge(
            passwordResetToken = passwordResetToken
        )
    }

    private fun performResetPasswordContinueCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordSubmitCodeCommandParameters,
    ): ResetPasswordContinueApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performResetPasswordContinue(
            parameters = parameters
        )
    }

    private fun performResetPasswordSubmitCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordSubmitNewPasswordCommandParameters,
    ): ResetPasswordSubmitApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performResetPasswordSubmit(
            parameters = parameters,
        )
    }

    private fun performResetPasswordPollCompletionCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String,
    ): ResetPasswordPollCompletionApiResult {
        LogSession.logMethodCall(tag = TAG)
        return oAuth2Strategy.performResetPasswordPollCompletion(
            passwordResetToken = passwordResetToken
        )
    }

    private fun saveAndReturnTokens(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: BaseSignInTokenCommandParameters,
        tokenApiResult: SignInTokenApiResult.Success
    ): SignInCommandResult.Complete {
        LogSession.logMethodCall(tag = TAG)
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
            )
        )
    }

    private fun createAuthorizationRequest(
        strategy: NativeAuthOAuth2Strategy,
        scopes: List<String>,
        clientId: String,
        applicationIdentifier: String
    ): MicrosoftStsAuthorizationRequest {
        LogSession.logMethodCall(tag = TAG)
        val builder = MicrosoftStsAuthorizationRequest.Builder()
        builder.setAuthority(URL(strategy.getAuthority()))
        builder.setClientId(clientId)
        builder.setScope(StringUtil.join(" ", scopes))
        builder.setApplicationIdentifier(applicationIdentifier)
        return builder.build()
    }

    private fun addDefaultScopes(scopes: List<String>?): List<String> {
        LogSession.logMethodCall(tag = TAG)
        val requestScopes = scopes?.toMutableList() ?: mutableListOf()
        requestScopes.addAll(AuthenticationConstants.DEFAULT_SCOPES)
        // sanitize empty and null scopes
        requestScopes.removeAll(listOf("", null))
        return requestScopes.toList()
    }

    private fun SignInChallengeApiResult.toSignInStartCommandResult(): SignInStartCommandResult {
        LogSession.logMethodCall(tag = TAG)
        return when (this) {
            is SignInChallengeApiResult.OOBRequired -> {
                SignInCommandResult.CodeRequired(
                    credentialToken = this.credentialToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel
                )
            }

            is SignInChallengeApiResult.PasswordRequired -> {
                SignInCommandResult.PasswordRequired(
                    credentialToken = this.credentialToken
                )
            }

            SignInChallengeApiResult.Redirect -> {
                CommandResult.Redirect()
            }

            is SignInChallengeApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    details = this.details,
                    errorCodes = this.errorCodes
                )
            }
        }
    }

    private fun SignInChallengeApiResult.toSignInSubmitPasswordCommandResult(): SignInSubmitPasswordCommandResult {
        LogSession.logMethodCall(tag = TAG)
        return when (this) {
            SignInChallengeApiResult.Redirect -> {
                CommandResult.Redirect()
            }

            is SignInChallengeApiResult.PasswordRequired -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this"
                )
            }

            is SignInChallengeApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    details = this.details,
                    errorCodes = this.errorCodes
                )
            }
            is SignInChallengeApiResult.OOBRequired -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this"
                )
            }
        }
    }

    private fun ResetPasswordChallengeApiResult.toResetPasswordStartCommandResult(): ResetPasswordStartCommandResult {
        LogSession.logMethodCall(tag = TAG)
        return when (this) {
            is ResetPasswordChallengeApiResult.CodeRequired -> {
                ResetPasswordCommandResult.CodeRequired(
                    passwordResetToken = this.passwordResetToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel
                )
            }

            ResetPasswordChallengeApiResult.Redirect -> {
                CommandResult.Redirect()
            }

            is ResetPasswordChallengeApiResult.ExpiredToken -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Expire token result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is ResetPasswordChallengeApiResult.UnsupportedChallengeType -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is ResetPasswordChallengeApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    details = this.details
                )
            }
        }
    }

    fun signUpStart(parameters: BaseSignUpStartCommandParameters): SignUpStartCommandResult {
        LogSession.logMethodCall(tag = TAG)
        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val isWithPassword = parameters is SignUpStartUsingPasswordCommandParameters

            val signUpStartApiResult = if (isWithPassword) {
                performSignUpStartUsingPasswordRequest(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = (parameters as SignUpStartUsingPasswordCommandParameters)
                )
            } else {
                performSignUpStartRequest(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = (parameters as SignUpStartCommandParameters)
                )
            }
            return when (signUpStartApiResult) {
                is SignUpStartApiResult.VerificationRequired -> {
                    val challengeApiResult = performSignUpChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        signupToken = signUpStartApiResult.signupToken
                    )
                    processSignUpChallengeApiResult(challengeApiResult)
                }

                is SignUpStartApiResult.UnsupportedChallengeType -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $signUpStartApiResult"
                    )
                    CommandResult.UnknownError(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription
                    )
                }

                is SignUpStartApiResult.InvalidPassword -> {
                    SignUpCommandResult.InvalidPassword(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription
                    )
                }

                is SignUpStartApiResult.InvalidAttributes -> {
                    SignUpCommandResult.InvalidAttributes(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        invalidAttributes = signUpStartApiResult.invalidAttributes
                    )
                }

                is SignUpStartApiResult.Redirect -> {
                    CommandResult.Redirect()
                }

                is SignUpStartApiResult.UnknownError -> {
                    LogSession.log(
                        tag = TAG,
                        logLevel = Logger.LogLevel.WARN,
                        message = "Unexpected result: $signUpStartApiResult"
                    )
                    CommandResult.UnknownError(
                        error = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        details = signUpStartApiResult.details
                    )
                }

                is SignUpStartApiResult.UsernameAlreadyExists -> {
                    SignUpCommandResult.UsernameAlreadyExists(
                        error = signUpStartApiResult.errorCode,
                        errorDescription = signUpStartApiResult.errorDescription
                    )
                }

                is SignUpStartApiResult.AuthNotSupported -> {
                    SignUpCommandResult.AuthNotSupported(
                        error = signUpStartApiResult.errorCode,
                        errorDescription = signUpStartApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            LogSession.log(
                tag = TAG,
                logLevel = Logger.LogLevel.ERROR,
                message = "Error occurred while performing sign-up start"
            )
            throw e
        }
    }

    fun signUpSubmitCode(parameters: SignUpSubmitCodeCommandParameters): SignUpSubmitCodeCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val signUpContinueApiResult = performSignUpContinueCall(
                oAuth2Strategy = oAuth2Strategy,
                signupToken = parameters.signupToken,
                parameters = parameters
            )
            return signUpContinueApiResult.toSignUpSubmitCodeCommandResult(oAuth2Strategy)
        } catch (e: Exception) {
            LogSession.log(
                tag = TAG,
                logLevel = Logger.LogLevel.ERROR,
                message = "Error occurred while performing sign-up submit code"
            )
            throw e
        }
    }

    fun signUpResendCode(parameters: SignUpResendCodeCommandParameters): SignUpResendCodeCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val signUpChallengeApiResult = performSignUpChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                signupToken = parameters.signupToken
            )
            return processSignUpChallengeApiResult(signUpChallengeApiResult) as SignUpResendCodeCommandResult
        } catch (e: Exception) {
            LogSession.log(
                tag = TAG,
                logLevel = Logger.LogLevel.ERROR,
                message = "Error occurred while performing sign-up resend code"
            )
            throw e
        }
    }

    fun signUpSubmitUserAttributes(parameters: SignUpSubmitUserAttributesCommandParameters): SignUpSubmitUserAttributesCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val signUpContinueApiResult = performSignUpContinueCall(
                oAuth2Strategy = oAuth2Strategy,
                signupToken = parameters.signupToken,
                parameters = parameters
            )
            return signUpContinueApiResult.toSignUpSubmitUserAttributesCommandResult(oAuth2Strategy)
        } catch (e: Exception) {
            LogSession.log(
                tag = TAG,
                logLevel = Logger.LogLevel.ERROR,
                message = "Error occurred while performing sign-up submit user attributes"
            )
            throw e
        }
    }

    fun signUpSubmitPassword(parameters: SignUpSubmitPasswordCommandParameters): SignUpSubmitPasswordCommandResult {
        LogSession.logMethodCall(tag = TAG)

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val signUpContinueApiResult = performSignUpContinueCall(
                oAuth2Strategy = oAuth2Strategy,
                signupToken = parameters.signupToken,
                parameters = parameters
            )
            return signUpContinueApiResult.toSignUpSubmitPasswordCommandResult(oAuth2Strategy)
        } catch (e: Exception) {
            LogSession.log(
                tag = TAG,
                logLevel = Logger.LogLevel.ERROR,
                message = "Error occurred while performing sign-up submit password"
            )
            throw e
        }
    }

    /**
     * Native-auth specific implementation of fetching a token from the cache, and/or refreshing it.
     * Main differences with standard implementation in [LocalMSALController] are:
     * - No scopes are passed in as part of the (developer provided) parameters. The scopes from the
     * AT are used to make the refresh token call.
     * - When the RT is expired or the refresh token call fails, a different exception is thrown.
     */
    @Throws(
        IOException::class,
        ClientException::class,
        ArgumentException::class,
        ServiceException::class
    )
    fun acquireTokenSilent(
        parameters: AcquireTokenNoFixedScopesCommandParameters
    ): AcquireTokenResult {
        LogSession.logMethodCall(tag = TAG)

        val acquireTokenSilentResult = AcquireTokenResult()

        // Validate original AcquireTokenNoScopesCommandParameters parameters
        parameters.validate()

        // Convert AcquireTokenNoScopesCommandParameters into SilentTokenCommandParameters
        // so we can re-use existing MSAL logic
        val silentTokenCommandParameters =
            CommandUtil.convertAcquireTokenNoFixedScopesCommandParameters(
                parameters
            )
        // Not adding any (default) scopes, because we want to retrieve all tokens (regardless of scope).
        // Scopes will be added later in the flow, if necessary.
        val targetAccount: AccountRecord = getCachedAccountRecord(silentTokenCommandParameters)

        // Build up params for Strategy construction
        val authScheme = silentTokenCommandParameters.authenticationScheme
        val strategyParameters = OAuth2StrategyParameters.builder()
            .platformComponents(parameters.platformComponents)
            .authenticationScheme(authScheme)
            .build()
        val strategy = silentTokenCommandParameters.authority.createOAuth2Strategy(strategyParameters)

        val tokenCache = silentTokenCommandParameters.oAuth2TokenCache
        val cacheRecords = tokenCache.loadWithAggregatedAccountData(
            silentTokenCommandParameters.clientId,
            parameters.applicationIdentifier,
            parameters.mamEnrollmentId,
            null, // TODO see where else this is needed
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
            LogSession.log(
                tag = TAG,
                logLevel = Logger.LogLevel.INFO,
                message = "RefreshOn is active. This will extend your token usage in the rare case servers are not available."
            )
        }
        if (LibraryConfiguration.getInstance().isRefreshInEnabled &&
            fullCacheRecord.accessToken != null && fullCacheRecord.accessToken.shouldRefresh()
        ) {
            if (!fullCacheRecord.accessToken.isExpired) {
                setAcquireTokenResult(acquireTokenSilentResult, silentTokenCommandParameters, cacheRecords)
                val refreshOnCommand =
                    RefreshOnCommand(parameters, this, PublicApiId.MSAL_REFRESH_ON)
                CommandDispatcher.submitAndForget(refreshOnCommand)
            } else {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Access token is expired. Removing from cache..."

                )
                // Remove the expired token
                tokenCache.removeCredential(fullCacheRecord.accessToken)
                renewAT(
                    silentTokenCommandParameters,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    fullCacheRecord
                )
            }
        } else if (accessTokenIsNull(fullCacheRecord) ||
            refreshTokenIsNull(fullCacheRecord) ||
            silentTokenCommandParameters.isForceRefresh ||
            !isRequestAuthorityRealmSameAsATRealm(
                    silentTokenCommandParameters.authority,
                    fullCacheRecord.accessToken
                ) ||
            !strategy.validateCachedResult(authScheme, fullCacheRecord)
        ) {
            if (!refreshTokenIsNull(fullCacheRecord)) {
                // No AT found, but the RT checks out, so we'll use it
                renewAT(
                    silentTokenCommandParameters,
                    acquireTokenSilentResult,
                    tokenCache,
                    strategy,
                    fullCacheRecord
                )
            } else {
                val exception = RefreshTokenNotFoundException(
                    ErrorStrings.NO_TOKENS_FOUND,
                    "No refresh token was found."
                )
                Telemetry.emit(
                    ApiEndEvent()
                        .putException(exception)
                        .putApiId(TelemetryEventStrings.Api.LOCAL_ACQUIRE_TOKEN_SILENT)
                )
                throw exception
            }
        } else if (fullCacheRecord.accessToken.isExpired) {
            LogSession.log(
                tag = TAG,
                logLevel = Logger.LogLevel.WARN,
                message = "Access token is expired. Removing from cache..."

            )
            // Remove the expired token
            tokenCache.removeCredential(fullCacheRecord.accessToken)
            renewAT(
                silentTokenCommandParameters,
                acquireTokenSilentResult,
                tokenCache,
                strategy,
                fullCacheRecord
            )
        } else {
            LogSession.log(
                tag = TAG,
                logLevel = Logger.LogLevel.VERBOSE,
                message = "Returning silent result"
            )
            setAcquireTokenResult(acquireTokenSilentResult, silentTokenCommandParameters, cacheRecords)
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

    @Throws(
        IOException::class,
        ClientException::class,
        ServiceException::class
    )
    private fun renewAT(
        parameters: SilentTokenCommandParameters,
        acquireTokenSilentResult: AcquireTokenResult,
        tokenCache: OAuth2TokenCache<*, *, *>,
        strategy: OAuth2Strategy<*, *, *, *, *, *, *, *, *, *, *, *, *>,
        cacheRecord: ICacheRecord
    ) {
        LogSession.log(
            tag = TAG,
            logLevel = Logger.LogLevel.VERBOSE,
            message = "Renewing access token..."
        )
        // Add the AT's scopes to the parameters so that they can be used to perform the refresh
        // token call.
        val accessTokenScopes = cacheRecord.accessToken
            .target?.split(" ".toRegex())?.dropLastWhile { it.isEmpty() }?.toSet()

        val parametersWithScopes = parameters.toBuilder()
            .scopes(accessTokenScopes)
            .build()

        renewAccessToken(
            parametersWithScopes,
            acquireTokenSilentResult,
            tokenCache,
            strategy,
            cacheRecord
        )
    }

    private fun performSignUpStartRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignUpStartCommandParameters
    ): SignUpStartApiResult {
        return oAuth2Strategy.performSignUpStart(
            commandParameters = parameters
        )
    }

    private fun performSignUpStartUsingPasswordRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignUpStartUsingPasswordCommandParameters
    ): SignUpStartApiResult {
        return oAuth2Strategy.performSignUpStartUsingPassword(
            commandParameters = parameters
        )
    }

    private fun performSignUpChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        signupToken: String
    ): SignUpChallengeApiResult {
        return oAuth2Strategy.performSignUpChallenge(signUpToken = signupToken)
    }

    private fun performSignUpContinueCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        signupToken: String,
        parameters: BaseNativeAuthCommandParameters
    ): SignUpContinueApiResult {
        return oAuth2Strategy.performSignUpContinue(
            signUpToken = signupToken,
            commandParameters = parameters
        )
    }

    private fun processSignUpChallengeApiResult(signUpChallengeApiResult: SignUpChallengeApiResult): SignUpStartCommandResult {
        return when (signUpChallengeApiResult) {
            is SignUpChallengeApiResult.OOBRequired -> {
                SignUpCommandResult.CodeRequired(
                    signupToken = signUpChallengeApiResult.signupToken,
                    codeLength = signUpChallengeApiResult.codeLength,
                    challengeTargetLabel = signUpChallengeApiResult.challengeTargetLabel,
                    challengeChannel = signUpChallengeApiResult.challengeChannel
                )
            }

            is SignUpChallengeApiResult.PasswordRequired -> {
                SignUpCommandResult.PasswordRequired(
                    signupToken = signUpChallengeApiResult.signupToken
                )
            }

            is SignUpChallengeApiResult.ExpiredToken -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Expire token result: $this"
                )
                CommandResult.UnknownError(
                    error = signUpChallengeApiResult.error,
                    errorDescription = signUpChallengeApiResult.errorDescription
                )
            }

            is SignUpChallengeApiResult.UnsupportedChallengeType -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $signUpChallengeApiResult"
                )
                CommandResult.UnknownError(
                    error = signUpChallengeApiResult.errorCode,
                    errorDescription = signUpChallengeApiResult.errorDescription
                )
            }

            SignUpChallengeApiResult.Redirect -> {
                CommandResult.Redirect()
            }

            is SignUpChallengeApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $signUpChallengeApiResult"
                )
                CommandResult.UnknownError(
                    error = signUpChallengeApiResult.error,
                    errorDescription = signUpChallengeApiResult.errorDescription,
                    details = signUpChallengeApiResult.details
                )
            }
        }
    }

    private fun SignUpContinueApiResult.toSignUpSubmitCodeCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignUpSubmitCodeCommandResult {
        return when (this) {
            is SignUpContinueApiResult.Success -> {
                SignUpCommandResult.Complete(
                    signInSLT = this.signInSLT,
                    expiresIn = this.expiresIn
                )
            }

            is SignUpContinueApiResult.ExpiredToken -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Expire token result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is SignUpContinueApiResult.UsernameAlreadyExists -> {
                SignUpCommandResult.UsernameAlreadyExists(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    signupToken = this.signupToken,
                    error = this.error,
                    errorDescription = this.errorDescription,
                    requiredAttributes = this.requiredAttributes
                )
            }

            is SignUpContinueApiResult.CredentialRequired -> {
                return processSignUpChallengeApiResult(
                    performSignUpChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        signupToken = this.signupToken
                    )
                ) as SignUpSubmitCodeCommandResult
            }

            is SignUpContinueApiResult.InvalidOOBValue -> {
                SignUpCommandResult.InvalidCode(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is SignUpContinueApiResult.Redirect -> {
                CommandResult.Redirect()
            }

            is SignUpContinueApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    details = this.details
                )
            }

            is SignUpContinueApiResult.InvalidAttributes, is SignUpContinueApiResult.InvalidPassword -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this"
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
                    signInSLT = this.signInSLT,
                    expiresIn = this.expiresIn
                )
            }

            is SignUpContinueApiResult.ExpiredToken -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Expire token result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is SignUpContinueApiResult.UsernameAlreadyExists -> {
                SignUpCommandResult.UsernameAlreadyExists(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    signupToken = this.signupToken,
                    error = this.error,
                    errorDescription = this.errorDescription,
                    requiredAttributes = this.requiredAttributes
                )
            }

            is SignUpContinueApiResult.CredentialRequired -> {
                processSignUpChallengeApiResult(
                    performSignUpChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        signupToken = this.signupToken
                    )
                ) as SignUpSubmitUserAttributesCommandResult // TODO can we find something more graceful than a runtime cast?
            }

            is SignUpContinueApiResult.Redirect -> {
                CommandResult.Redirect()
            }

            is SignUpContinueApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    details = this.details
                )
            }

            is SignUpContinueApiResult.InvalidAttributes -> {
                SignUpCommandResult.InvalidAttributes(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    invalidAttributes = this.invalidAttributes
                )
            }

            is SignUpContinueApiResult.InvalidOOBValue, is SignUpContinueApiResult.InvalidPassword -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this"
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
                    signInSLT = this.signInSLT,
                    expiresIn = this.expiresIn
                )
            }

            is SignUpContinueApiResult.ExpiredToken -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Expire token result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is SignUpContinueApiResult.UsernameAlreadyExists -> {
                SignUpCommandResult.UsernameAlreadyExists(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    signupToken = this.signupToken,
                    error = this.error,
                    errorDescription = this.errorDescription,
                    requiredAttributes = this.requiredAttributes
                )
            }

            is SignUpContinueApiResult.CredentialRequired -> {
                processSignUpChallengeApiResult(
                    performSignUpChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        signupToken = this.signupToken
                    )
                ) as SignUpSubmitPasswordCommandResult
            }

            is SignUpContinueApiResult.Redirect -> {
                CommandResult.Redirect()
            }

            is SignUpContinueApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = this.error,
                    errorDescription = this.errorDescription,
                    details = details
                )
            }

            is SignUpContinueApiResult.InvalidPassword -> {
                SignUpCommandResult.InvalidPassword(
                    error = this.error,
                    errorDescription = this.errorDescription
                )
            }

            is SignUpContinueApiResult.InvalidOOBValue, is SignUpContinueApiResult.InvalidAttributes -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this"
                )
            }
        }
    }

    private fun signInStartAfterInvalidAuthenticationMethod(
        signInStartCommandParameters: SignInStartUsingPasswordCommandParameters,
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignInStartCommandResult {
        LogSession.logMethodCall(tag = TAG)

        val initiateApiResult = performSignInInitiateCall(
            oAuth2Strategy = oAuth2Strategy,
            parameters = signInStartCommandParameters as SignInStartCommandParameters
        )
        return processSignInInitiateApiResult(
            initiateApiResult = initiateApiResult,
            oAuth2Strategy = oAuth2Strategy,
            isInvalidAuthenticationMethod = true
        )
    }

    private fun SignInChallengeApiResult.toSignInStartCommandResultAfterInvalidAuthenticationMethod():
        SignInStartCommandResult {
        LogSession.logMethodCall(tag = TAG)
        return when (this) {
            is SignInChallengeApiResult.OOBRequired -> {
                SignInCommandResult.InvalidAuthenticationType(
                    error = "invalid_grant",
                    errorDescription = "The user is not configured for this authentication method. Please repeat the request using a different method.",
                    errorCodes = listOf(400002),
                )
            }

            is SignInChallengeApiResult.UnknownError,
            is SignInChallengeApiResult.PasswordRequired -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $this"
                )
                CommandResult.UnknownError(
                    error = "unexpected_api_result",
                    errorDescription = "Unexpected result: $this"
                )
            }

            SignInChallengeApiResult.Redirect -> {
                CommandResult.Redirect()
            }
        }
    }

    private fun processSignInInitiateApiResult(
        initiateApiResult: SignInInitiateApiResult,
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        isInvalidAuthenticationMethod: Boolean = false
    ): SignInStartCommandResult {
        return when (initiateApiResult) {
            SignInInitiateApiResult.Redirect -> {
                CommandResult.Redirect()
            }

            is SignInInitiateApiResult.Success -> {
                if (isInvalidAuthenticationMethod) {
                    performSignInChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        credentialToken = initiateApiResult.credentialToken
                    ).toSignInStartCommandResultAfterInvalidAuthenticationMethod()
                } else {
                    performSignInChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        credentialToken = initiateApiResult.credentialToken
                    ).toSignInStartCommandResult()
                }
            }

            is SignInInitiateApiResult.UnknownError -> {
                LogSession.log(
                    tag = TAG,
                    logLevel = Logger.LogLevel.WARN,
                    message = "Unexpected result: $initiateApiResult"
                )
                CommandResult.UnknownError(
                    error = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription,
                    errorCodes = initiateApiResult.errorCodes
                )
            }

            is SignInInitiateApiResult.UserNotFound -> {
                SignInCommandResult.UserNotFound(
                    error = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription,
                    errorCodes = initiateApiResult.errorCodes
                )
            }
        }
    }
}
