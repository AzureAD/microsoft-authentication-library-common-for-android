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

import com.microsoft.identity.common.internal.commands.ResetPasswordSubmitNewPasswordCommand
import com.microsoft.identity.common.internal.util.CommandUtil
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.cache.ICacheRecord
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
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartWithPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpStartWithPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignUpSubmitUserAttributesCommandParameters
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
import com.microsoft.identity.common.java.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpStartCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.SignUpSubmitUserAttributesCommandResult
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
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import com.microsoft.identity.common.java.request.SdkType
import com.microsoft.identity.common.java.result.LocalAuthenticationResult
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.util.ThreadUtils
import lombok.EqualsAndHashCode
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
        val methodTag = "$TAG:signInStart"

        Logger.verbose(
            methodTag,
            "Performing sign-in start..."
        )
        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .challengeTypes(parameters.challengeType)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val isROPCCall = parameters is SignInStartWithPasswordCommandParameters

            return if (isROPCCall) {
                signInStartROPC(
                    parameters as SignInStartWithPasswordCommandParameters,
                    oAuth2Strategy
                )
            } else {
                signInStartNonROPC(parameters as SignInStartCommandParameters, oAuth2Strategy)
            }
            // TODO add case for use of signInSLT
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sign-in start",
                e
            )
            throw e
        }
    }

    private fun signInStartROPC(
        parameters: SignInStartWithPasswordCommandParameters,
        oAuth2Strategy: NativeAuthOAuth2Strategy,
    ): SignInStartCommandResult {
        val methodTag = "$TAG:signInRocp"

        Logger.verbose(
            methodTag,
            "ROPC flow initiated"
        )

        val mergedScopes = addDefaultScopes(parameters.scopes)
        val parametersWithScopes =
            CommandUtil.createSignInStartWithPasswordCommandParametersWithScopes(
                parameters,
                mergedScopes
            )

        val tokenApiResult = performROPCTokenRequest(
            oAuth2Strategy = oAuth2Strategy,
            parameters = parametersWithScopes
        )
        return when (tokenApiResult) {
            is SignInTokenApiResult.CredentialRequired -> {
                performSignInChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    credentialToken = tokenApiResult.credentialToken
                ).toSignInStartCommandResult()
            }
            SignInTokenApiResult.Redirect -> {
                CommandResult.Redirect
            }
            is SignInTokenApiResult.Success -> {
                saveAndReturnTokens(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes,
                    tokenApiResult = tokenApiResult
                )
            }
            is SignInTokenApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription
                )
            }
            is SignInTokenApiResult.PasswordIncorrect -> {
                SignInCommandResult.PasswordIncorrect(
                    errorCode = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription
                )
            }
            is SignInTokenApiResult.UserNotFound -> {
                SignInCommandResult.UserNotFound(
                    errorCode = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription
                )
            }
            is SignInTokenApiResult.CodeIncorrect -> {
                // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                Logger.warn(
                    TAG,
                    "Unexpected API result: $tokenApiResult",
                )
                CommandResult.UnknownError(
                    errorCode = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $tokenApiResult"
                )
            }
        }
    }

    private fun signInStartNonROPC(
        parameters: SignInStartCommandParameters,
        oAuth2Strategy: NativeAuthOAuth2Strategy,
    ): SignInStartCommandResult {
        val methodTag = "$TAG:signInStartNonROPC"

        Logger.verbose(
            methodTag,
            "non-ROPC flow initiated"
        )

        val mergedScopes = addDefaultScopes(parameters.scopes)
        val parametersWithScopes = CommandUtil.createSignInStartCommandParametersWithScopes(
            parameters,
            mergedScopes
        )

        val initiateApiResult = performSignInInitiateCall(
            oAuth2Strategy = oAuth2Strategy,
            parameters = parametersWithScopes
        )
        return when (initiateApiResult) {
            SignInInitiateApiResult.Redirect -> {
                CommandResult.Redirect
            }
            is SignInInitiateApiResult.Success -> {
                performSignInChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    credentialToken = initiateApiResult.credentialToken
                ).toSignInStartCommandResult()
            }
            is SignInInitiateApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription
                )
            }
            is SignInInitiateApiResult.UserNotFound -> {
                SignInCommandResult.UserNotFound(
                    errorCode = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription
                )
            }
        }
    }

    fun signInSubmitCode(parameters: SignInSubmitCodeCommandParameters): SignInSubmitCodeCommandResult {
        val methodTag = "$TAG:signInSubmitCode"

        Logger.verbose(
            methodTag,
            "Performing sign-in submit code..."
        )
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
                SignInTokenApiResult.Redirect -> {
                    CommandResult.Redirect
                }
                is SignInTokenApiResult.Success -> {
                    saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = tokenApiResult
                    )
                }
                is SignInTokenApiResult.CodeIncorrect -> {
                    SignInCommandResult.IncorrectCode(
                        errorCode = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription
                    )
                }
                is SignInTokenApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription
                    )
                }
                is SignInTokenApiResult.CredentialRequired, is SignInTokenApiResult.PasswordIncorrect,
                is SignInTokenApiResult.UserNotFound -> {
                    // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                    Logger.warn(
                        TAG,
                        "Unexpected API result: $tokenApiResult",
                    )
                    CommandResult.UnknownError(
                        errorCode = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $tokenApiResult"
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sign-in submit code",
                e
            )
            throw e
        }
    }

    fun signInResendCode(parameters: SignInResendCodeCommandParameters): SignInResendCodeCommandResult {
        val methodTag = "$TAG:signInResendCode"

        Logger.verbose(
            methodTag,
            "Performing sign-in resend code..."
        )
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
                    // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                    Logger.warn(
                        TAG,
                        "Unexpected API result: $result",
                    )
                    CommandResult.UnknownError(
                        errorCode = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result"
                    )
                }
                SignInChallengeApiResult.Redirect -> {
                    CommandResult.Redirect
                }
                is SignInChallengeApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = result.error,
                        errorDescription = result.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sign-in resend code",
                e
            )
            throw e
        }
    }

    fun signInSubmitPassword(parameters: SignInSubmitPasswordCommandParameters): SignInSubmitPasswordCommandResult {
        val methodTag = "$TAG:signInSubmitPassword"

        Logger.verbose(
            methodTag,
            "Performing sign-in submit password..."
        )
        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .challengeTypes(parameters.challengeType)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val mergedScopes = addDefaultScopes(parameters.scopes)
            val parametersWithScopes = CommandUtil.createSignInSubmitPasswordCommandParametersWithScopes(
                parameters,
                mergedScopes
            )

            val result = performPasswordTokenCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parametersWithScopes
            )
            return when (result) {
                is SignInTokenApiResult.PasswordIncorrect -> {
                    SignInCommandResult.PasswordIncorrect(
                        errorCode = result.error,
                        errorDescription = result.errorDescription
                    )
                }
                is SignInTokenApiResult.Success -> {
                    saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = result
                    )
                }
                SignInTokenApiResult.Redirect -> {
                    CommandResult.Redirect
                }
                is SignInTokenApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = result.error,
                        errorDescription = result.errorDescription
                    )
                }
                is SignInTokenApiResult.CredentialRequired -> {
                    performSignInChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        credentialToken = result.credentialToken
                    ).toSignInSubmitPasswordCommandResult()
                }

                is SignInTokenApiResult.CodeIncorrect, is SignInTokenApiResult.UserNotFound -> {
                    // TODO add correlation ID
                    Logger.warn(
                        TAG,
                        "Unexpected API result: $result",
                    )
                    CommandResult.UnknownError(
                        errorCode = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result"
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sign-in submit password",
                e
            )
            throw e
        }
    }

    fun resetPasswordStart(parameters: ResetPasswordStartCommandParameters): ResetPasswordStartCommandResult {
        val methodTag = "$TAG:resetPasswordStart"

        Logger.verbose(
            methodTag,
            "Performing reset password start..."
        )

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
                    CommandResult.Redirect
                }
                is ResetPasswordStartApiResult.Success -> {
                    performResetPasswordChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        passwordResetToken = startApiResult.passwordResetToken
                    ).toResetPasswordStartCommandResult()
                }
                is ResetPasswordStartApiResult.UserNotFound -> {
                    ResetPasswordCommandResult.UserNotFound(
                        errorCode = startApiResult.errorCode,
                        errorDescription = startApiResult.errorDescription
                    )
                }
                is ResetPasswordStartApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = startApiResult.errorCode,
                        errorDescription = startApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing reset password start",
                e
            )
            throw e
        }
    }

    fun resetPasswordSubmitCode(parameters: ResetPasswordSubmitCodeCommandParameters): ResetPasswordSubmitCodeCommandResult {
        val methodTag = "$TAG:resetPasswordSubmitCode"

        Logger.verbose(
            methodTag,
            "Performing reset password submit code..."
        )
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
                    CommandResult.Redirect
                }
                is ResetPasswordContinueApiResult.PasswordRequired -> {
                    ResetPasswordCommandResult.PasswordRequired(passwordSubmitToken = continueApiResult.passwordSubmitToken)
                }
                is ResetPasswordContinueApiResult.CodeIncorrect -> {
                    ResetPasswordCommandResult.IncorrectCode(
                        errorCode = continueApiResult.errorCode,
                        errorDescription = continueApiResult.errorDescription
                    )
                }
                is ResetPasswordContinueApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = continueApiResult.errorCode,
                        errorDescription = continueApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing reset password submit code",
                e
            )
            throw e
        }
    }

    fun resetPasswordResendCode(parameters: ResetPasswordResendCodeCommandParameters): ResetPasswordResendCodeCommandResult {
        val methodTag = "$TAG:resetPasswordResendCode"

        Logger.verbose(
            methodTag,
            "Performing reset password resend code..."
        )
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
                    CommandResult.Redirect
                }
                is ResetPasswordChallengeApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = resetPasswordChallengeApiResult.errorCode,
                        errorDescription = resetPasswordChallengeApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing reset password resend code",
                e
            )
            throw e
        }
    }

    fun resetPasswordSubmitNewPassword(parameters: ResetPasswordSubmitNewPasswordCommandParameters): ResetPasswordSubmitNewPasswordCommandResult {
        val methodTag = "$TAG:resetPasswordSubmitNewPassword"

        Logger.verbose(
            methodTag,
            "Performing reset password password submit..."
        )

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
                        errorCode = submitApiResult.errorCode,
                        errorDescription = submitApiResult.errorDescription
                    )
                }
                is ResetPasswordSubmitApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = submitApiResult.errorCode,
                        errorDescription = submitApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing reset password submit new password",
                e
            )
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

        Logger.verbose(
            methodTag,
            "Performing reset password poll completion..."
        )
        try {
            val pollWaitInterval: Int

            if (!pollIntervalIsAppropriate(pollIntervalInSeconds)) {
                pollWaitInterval =
                    ResetPasswordSubmitNewPasswordCommand.DEFAULT_POLL_COMPLETION_INTERVAL_IN_MILISECONDS
            } else {
                pollWaitInterval = pollIntervalInSeconds!! * 1000
            }

            var pollCompletionApiResult = performResetPasswordPollCompletionCall(
                oAuth2Strategy = oAuth2Strategy,
                passwordResetToken = passwordResetToken
            )

            val startTime = System.currentTimeMillis()

            while (pollCompletionApiResult is ResetPasswordPollCompletionApiResult.InProgress) {
                // TODO: This will use coroutines, most likely shouldn't use thread sleep here
                ThreadUtils.sleepSafely(pollWaitInterval, methodTag, "Waiting between reset password polls")

                if (pollCompletionTimedOut(startTime)) {
                    Logger.warn(
                        methodTag,
                        "Reset password poll completion timed out.",
                        null
                    )
                    return ResetPasswordCommandResult.PasswordResetFailed(
                        errorCode = ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_CODE,
                        errorDescription = ResetPasswordSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_DESCRIPTION
                    )
                }

                Logger.verbose(
                    methodTag,
                    "Calling /poll_complete again..."
                )
                pollCompletionApiResult = performResetPasswordPollCompletionCall(
                    oAuth2Strategy = oAuth2Strategy,
                    passwordResetToken = passwordResetToken
                )
            }

            return when (pollCompletionApiResult) {
                is ResetPasswordPollCompletionApiResult.PollingFailed -> {
                    ResetPasswordCommandResult.PasswordResetFailed(
                        errorCode = pollCompletionApiResult.errorCode,
                        errorDescription = pollCompletionApiResult.errorDescription
                    )
                }
                is ResetPasswordPollCompletionApiResult.PollingSucceeded -> {
                    ResetPasswordCommandResult.Complete
                }
                is ResetPasswordPollCompletionApiResult.InProgress -> {
                    // This should never be reached, theoretically
                    // TODO feel free to change this errorCode if it's not appropriate
                    CommandResult.UnknownError(
                        errorCode = "illegal_state",
                        errorDescription = "in_progress received after polling, illegal state"
                    )
                }
                is ResetPasswordPollCompletionApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = pollCompletionApiResult.errorCode,
                        errorDescription = pollCompletionApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing reset password poll completion",
                e
            )
            throw e
        }
    }

    private fun performROPCTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInStartWithPasswordCommandParameters
    ): SignInTokenApiResult {
        return oAuth2Strategy.performROPCTokenRequest(
            parameters = parameters
        )
    }

    private fun performOOBTokenRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitCodeCommandParameters
    ): SignInTokenApiResult {
        return oAuth2Strategy.performOOBTokenRequest(
            parameters = parameters
        )
    }

    private fun performPasswordTokenCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInSubmitPasswordCommandParameters
    ): SignInTokenApiResult {
        return oAuth2Strategy.performPasswordTokenRequest(
            parameters = parameters
        )
    }

    private fun performSignInInitiateCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignInStartCommandParameters,
    ): SignInInitiateApiResult {
        return oAuth2Strategy.performSignInInitiate(
            parameters = parameters
        )
    }

    private fun performSignInChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        credentialToken: String
    ): SignInChallengeApiResult {
        return oAuth2Strategy.performSignInChallenge(credentialToken = credentialToken)
    }

    private fun performResetPasswordStartCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordStartCommandParameters,
    ): ResetPasswordStartApiResult {
        return oAuth2Strategy.performResetPasswordStart(
            parameters = parameters
        )
    }

    private fun performResetPasswordChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String
    ): ResetPasswordChallengeApiResult {
        return oAuth2Strategy.performResetPasswordChallenge(
            passwordResetToken = passwordResetToken
        )
    }

    private fun performResetPasswordContinueCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordSubmitCodeCommandParameters,
    ): ResetPasswordContinueApiResult {
        return oAuth2Strategy.performResetPasswordContinue(
            parameters = parameters
        )
    }

    private fun performResetPasswordSubmitCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: ResetPasswordSubmitNewPasswordCommandParameters,
    ): ResetPasswordSubmitApiResult {
        return oAuth2Strategy.performResetPasswordSubmit(
            parameters = parameters,
        )
    }

    private fun performResetPasswordPollCompletionCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String,
    ): ResetPasswordPollCompletionApiResult {
        return oAuth2Strategy.performResetPasswordPollCompletion(
            passwordResetToken = passwordResetToken
        )
    }

    private fun saveAndReturnTokens(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: BaseSignInTokenCommandParameters,
        tokenApiResult: SignInTokenApiResult.Success
    ): SignInCommandResult.Complete {
        val records: List<ICacheRecord> = saveTokens(
            oAuth2Strategy as MicrosoftStsOAuth2Strategy,
            createAuthorizationRequest(
                strategy = oAuth2Strategy,
                scopes = parametersWithScopes.scopes,
                clientId = parametersWithScopes.clientId
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
        clientId: String
    ): MicrosoftStsAuthorizationRequest {
        val builder = MicrosoftStsAuthorizationRequest.Builder()
        builder.setAuthority(URL(strategy.getAuthority()))
        builder.setClientId(clientId)
        builder.setScope(StringUtil.join(" ", scopes))
        return builder.build()
    }

    private fun addDefaultScopes(scopes: List<String>?): List<String> {
        val requestScopes = scopes?.toMutableList() ?: mutableListOf<String>()
        requestScopes.addAll(AuthenticationConstants.DEFAULT_SCOPES)
        // sanitize empty and null scopes
        requestScopes.removeAll(listOf("", null))
        return requestScopes.toList()
    }

    private fun SignInChallengeApiResult.toSignInStartCommandResult(): SignInStartCommandResult {
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
                CommandResult.Redirect
            }
            is SignInChallengeApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = this.error,
                    errorDescription = this.errorDescription
                )
            }
        }
    }

    private fun SignInChallengeApiResult.toSignInSubmitPasswordCommandResult(): SignInSubmitPasswordCommandResult {
        return when (this) {
            is SignInChallengeApiResult.OOBRequired -> {
                SignInCommandResult.CodeRequired(
                    credentialToken = this.credentialToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel
                )
            }
            SignInChallengeApiResult.Redirect -> {
                CommandResult.Redirect
            }
            is SignInChallengeApiResult.PasswordRequired -> {
                // TODO add correlation ID
                Logger.warn(
                    TAG,
                    "Unexpected API result: $this",
                )
                CommandResult.UnknownError(
                    errorCode = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this"
                )
            }
            is SignInChallengeApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = this.error,
                    errorDescription = this.errorDescription
                )
            }
        }
    }

    private fun ResetPasswordChallengeApiResult.toResetPasswordStartCommandResult(): ResetPasswordStartCommandResult {
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
                CommandResult.Redirect
            }
            is ResetPasswordChallengeApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = this.errorCode,
                    errorDescription = this.errorDescription
                )
            }
        }
    }

    fun signUpStart(parameters: BaseSignUpStartCommandParameters): SignUpStartCommandResult {
        val methodTag = "$TAG:signUpStart"

        Logger.verbose(
            methodTag,
            "Performing sign-up start..."
        )
        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val isWithPassword = parameters is SignUpStartWithPasswordCommandParameters

            val signUpStartApiResult = if (isWithPassword) {
                performSignUpStartWithPasswordRequest(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = (parameters as SignUpStartWithPasswordCommandParameters)
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
                is SignUpStartApiResult.InvalidPassword -> {
                    SignUpCommandResult.InvalidPassword(
                        errorCode = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription
                    )
                }
                is SignUpStartApiResult.InvalidAttributes -> {
                    SignUpCommandResult.InvalidAttributes(
                        errorCode = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        invalidAttributes = signUpStartApiResult.invalidAttributes
                    )
                }
                is SignUpStartApiResult.Redirect -> {
                    CommandResult.Redirect
                }
                is SignUpStartApiResult.UnknownError -> {
                    CommandResult.UnknownError(
                        errorCode = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                    )
                }
                is SignUpStartApiResult.UserNameAlreadyExists -> {
                    SignUpCommandResult.UsernameAlreadyExists(
                        errorCode = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription,
                        details = signUpStartApiResult.details
                    )
                }
                is SignUpStartApiResult.AuthNotSupported -> {
                    SignUpCommandResult.AuthNotSupported(
                        errorCode = signUpStartApiResult.error,
                        errorDescription = signUpStartApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sign-up start",
                e
            )
            throw e
        }
    }

    fun signUpSubmitCode(parameters: SignUpSubmitCodeCommandParameters): SignUpSubmitCodeCommandResult {
        val methodTag = "$TAG:signUpSubmitCode"

        Logger.verbose(
            methodTag,
            "Performing sign-up submit code..."
        )
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
            Logger.error(
                methodTag,
                "Error occurred while performing sign-up submit code",
                e
            )
            throw e
        }
    }

    fun signUpResendCode(parameters: SignUpResendCodeCommandParameters): SignUpResendCodeCommandResult {
        val methodTag = "$TAG:signUpResendCode"

        Logger.verbose(
            methodTag,
            "Performing sign-up resend code..."
        )
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
            Logger.error(
                methodTag,
                "Error occurred while performing sign-up resend oode",
                e
            )
            throw e
        }
    }

    fun signUpSubmitUserAttributes(parameters: SignUpSubmitUserAttributesCommandParameters): SignUpSubmitUserAttributesCommandResult {
        val methodTag = "$TAG:signUpSubmitUserAttributes"

        Logger.verbose(
            methodTag,
            "Performing sign-up submit user attributes..."
        )
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
            Logger.error(
                methodTag,
                "Error occurred while performing sign-up submit user attributes",
                e
            )
            throw e
        }
    }

    fun signUpSubmitPassword(parameters: SignUpSubmitPasswordCommandParameters): SignUpSubmitPasswordCommandResult {
        val methodTag = "$TAG:signUpSubmitPassword"

        Logger.verbose(
            methodTag,
            "Performing sign-up submit password..."
        )
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
            Logger.error(
                methodTag,
                "Error occurred while performing sign-up submit password",
                e
            )
            throw e
        }
    }

    private fun performSignUpStartRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignUpStartCommandParameters
    ): SignUpStartApiResult {
        return oAuth2Strategy.performSignUpStart(
            commandParameters = parameters
        )
    }

    private fun performSignUpStartWithPasswordRequest(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SignUpStartWithPasswordCommandParameters
    ): SignUpStartApiResult {
        return oAuth2Strategy.performSignUpStartWithPassword(
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
            SignUpChallengeApiResult.Redirect -> {
                CommandResult.Redirect
            }
            is SignUpChallengeApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = signUpChallengeApiResult.error,
                    errorDescription = signUpChallengeApiResult.errorDescription,
                )
            }
        }
    }

    private fun SignUpContinueApiResult.toSignUpSubmitCodeCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignUpSubmitCodeCommandResult {
        return when (this) {
            is SignUpContinueApiResult.Success -> {
                SignUpCommandResult.Complete
            }
            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    signupToken = this.signupToken,
                    errorCode = this.error,
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
                    errorCode = this.error,
                    errorDescription = this.errorDescription,
                    details = this.details
                )
            }
            is SignUpContinueApiResult.Redirect -> {
                CommandResult.Redirect
            }
            is SignUpContinueApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = this.error,
                    errorDescription = this.errorDescription,
                )
            }
            is SignUpContinueApiResult.InvalidAttributes, is SignUpContinueApiResult.InvalidPassword -> {
                // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                Logger.warn(
                    TAG,
                    "Unexpected API result: $this",
                )
                CommandResult.UnknownError(
                    errorCode = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this",
                )
            }
        }
    }

    private fun SignUpContinueApiResult.toSignUpSubmitUserAttributesCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignUpSubmitUserAttributesCommandResult {
        return when (this) {
            is SignUpContinueApiResult.Success -> {
                SignUpCommandResult.Complete
            }
            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    signupToken = this.signupToken,
                    errorCode = this.error,
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
                CommandResult.Redirect
            }
            is SignUpContinueApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = this.error,
                    errorDescription = this.errorDescription,
                )
            }
            is SignUpContinueApiResult.InvalidAttributes -> {
                SignUpCommandResult.InvalidAttributes(
                    errorCode = this.error,
                    errorDescription = this.errorDescription,
                    invalidAttributes = this.invalidAttributes
                )
            }
            is SignUpContinueApiResult.InvalidOOBValue, is SignUpContinueApiResult.InvalidPassword -> {
                // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                Logger.warn(
                    TAG,
                    "Unexpected API result: $this",
                )
                CommandResult.UnknownError(
                    errorCode = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this",
                )
            }
        }
    }

    private fun SignUpContinueApiResult.toSignUpSubmitPasswordCommandResult(
        oAuth2Strategy: NativeAuthOAuth2Strategy
    ): SignUpSubmitPasswordCommandResult {
        return when (this) {
            is SignUpContinueApiResult.Success -> {
                SignUpCommandResult.Complete
            }
            is SignUpContinueApiResult.AttributesRequired -> {
                SignUpCommandResult.AttributesRequired(
                    signupToken = this.signupToken,
                    errorCode = this.error,
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
                CommandResult.Redirect
            }
            is SignUpContinueApiResult.UnknownError -> {
                CommandResult.UnknownError(
                    errorCode = this.error,
                    errorDescription = this.errorDescription,
                )
            }
            is SignUpContinueApiResult.InvalidPassword -> {
                SignUpCommandResult.InvalidPassword(
                    errorCode = this.error,
                    errorDescription = this.errorDescription
                )
            }
            is SignUpContinueApiResult.InvalidOOBValue, is SignUpContinueApiResult.InvalidAttributes -> {
                // TODO add correlation ID https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2503124
                Logger.warn(
                    TAG,
                    "Unexpected API result: $this",
                )
                CommandResult.UnknownError(
                    errorCode = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this",
                )
            }
        }
    }
}
