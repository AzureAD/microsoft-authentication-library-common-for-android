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

import com.microsoft.identity.common.internal.commands.SsprSubmitNewPasswordCommand
import com.microsoft.identity.common.internal.util.CommandUtil
import com.microsoft.identity.common.java.AuthenticationConstants
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInStartWithPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SignInSubmitPasswordCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprResendCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprStartCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitCodeCommandParameters
import com.microsoft.identity.common.java.commands.parameters.nativeauth.SsprSubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.controllers.results.CodeRequired
import com.microsoft.identity.common.java.controllers.results.Complete
import com.microsoft.identity.common.java.controllers.results.IncorrectCode
import com.microsoft.identity.common.java.controllers.results.PasswordIncorrect
import com.microsoft.identity.common.java.controllers.results.PasswordNotAccepted
import com.microsoft.identity.common.java.controllers.results.PasswordResetFailed
import com.microsoft.identity.common.java.controllers.results.Redirect
import com.microsoft.identity.common.java.controllers.results.SignInPasswordRequired
import com.microsoft.identity.common.java.controllers.results.SignInResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInStartCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInSubmitPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.SignInUserNotFound
import com.microsoft.identity.common.java.controllers.results.SsprCodeRequired
import com.microsoft.identity.common.java.controllers.results.SsprComplete
import com.microsoft.identity.common.java.controllers.results.SsprIncorrectCode
import com.microsoft.identity.common.java.controllers.results.SsprPasswordRequired
import com.microsoft.identity.common.java.controllers.results.SsprResendCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SsprStartCommandResult
import com.microsoft.identity.common.java.controllers.results.SsprSubmitCodeCommandResult
import com.microsoft.identity.common.java.controllers.results.SsprSubmitNewPasswordCommandResult
import com.microsoft.identity.common.java.controllers.results.SsprUserNotFound
import com.microsoft.identity.common.java.controllers.results.UnknownError
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInInitiateApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprChallengeApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprContinueApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprPollCompletionApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprStartApiResult
import com.microsoft.identity.common.java.providers.nativeauth.responses.sspr.SsprSubmitApiResult
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
                .challengeType(parameters.challengeType)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val isROPCCall = parameters is SignInStartWithPasswordCommandParameters

            return if (isROPCCall) {
                signInStartROPC(parameters as SignInStartWithPasswordCommandParameters, oAuth2Strategy)
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
        val parametersWithScopes = CommandUtil.createSignInStartWithPasswordCommandParametersWithScopes(
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
                Redirect
            }
            is SignInTokenApiResult.Success -> {
                saveAndReturnTokens(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersWithScopes = parametersWithScopes,
                    tokenApiResult = tokenApiResult
                )
            }
            is SignInTokenApiResult.UnknownError -> {
                UnknownError(
                    errorCode = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription
                )
            }
            is SignInTokenApiResult.PasswordIncorrect -> {
                PasswordIncorrect(
                    errorCode = tokenApiResult.error,
                    errorDescription = tokenApiResult.errorDescription
                )
            }
            is SignInTokenApiResult.UserNotFound -> {
                SignInUserNotFound(
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
                UnknownError(
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
                Redirect
            }
            is SignInInitiateApiResult.Success -> {
                performSignInChallengeCall(
                    oAuth2Strategy = oAuth2Strategy,
                    credentialToken = initiateApiResult.credentialToken
                ).toSignInStartCommandResult()
            }
            is SignInInitiateApiResult.UnknownError -> {
                UnknownError(
                    errorCode = initiateApiResult.error,
                    errorDescription = initiateApiResult.errorDescription
                )
            }
            is SignInInitiateApiResult.UserNotFound -> {
                SignInUserNotFound(
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

            val parametersWithScopes = CommandUtil.createSignInSubmitCodeCommandParametersWithScopes(
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
                    Redirect
                }
                is SignInTokenApiResult.Success -> {
                    saveAndReturnTokens(
                        oAuth2Strategy = oAuth2Strategy,
                        parametersWithScopes = parametersWithScopes,
                        tokenApiResult = tokenApiResult
                    )
                }
                is SignInTokenApiResult.CodeIncorrect -> {
                    IncorrectCode(
                        errorCode = tokenApiResult.error,
                        errorDescription = tokenApiResult.errorDescription
                    )
                }
                is SignInTokenApiResult.UnknownError -> {
                    UnknownError(
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
                    UnknownError(
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
                .challengeType(parameters.challengeType)
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
                    CodeRequired(
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
                    UnknownError(
                        errorCode = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result"
                    )
                }
                SignInChallengeApiResult.Redirect -> {
                    Redirect
                }
                is SignInChallengeApiResult.UnknownError -> {
                    UnknownError(
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
                .challengeType(parameters.challengeType)
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
                    PasswordIncorrect(
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
                    Redirect
                }
                is SignInTokenApiResult.UnknownError -> {
                    UnknownError(
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
                    return UnknownError(
                        errorCode = "unexpected_api_result",
                        errorDescription = "API returned unexpected result: $result"
                    )
                }
            }
        } catch (e: Exception) {
            com.microsoft.identity.common.logging.Logger.error(
                methodTag,
                "Error occurred while performing sign-in submit password",
                e
            )
            throw e
        }
    }

    fun ssprStart(parameters: SsprStartCommandParameters): SsprStartCommandResult {
        val methodTag = "$TAG:ssprStart"

        Logger.verbose(
            methodTag,
            "Performing sspr start..."
        )

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .challengeType(parameters.challengeType)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val startApiResult = performSsprStartCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (startApiResult) {
                SsprStartApiResult.Redirect -> {
                    Redirect
                }
                is SsprStartApiResult.Success -> {
                    performSsprChallengeCall(
                        oAuth2Strategy = oAuth2Strategy,
                        passwordResetToken = startApiResult.passwordResetToken
                    ).toSsprStartCommandResult()
                }
                is SsprStartApiResult.UserNotFound -> {
                    SsprUserNotFound(
                        errorCode = startApiResult.errorCode,
                        errorDescription = startApiResult.errorDescription
                    )
                }
                is SsprStartApiResult.UnknownError -> {
                    UnknownError(
                        errorCode = startApiResult.errorCode,
                        errorDescription = startApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sspr start",
                e
            )
            throw e
        }
    }

    fun ssprSubmitCode(parameters: SsprSubmitCodeCommandParameters): SsprSubmitCodeCommandResult {
        val methodTag = "$TAG:ssprSubmitCode"

        Logger.verbose(
            methodTag,
            "Performing sspr submit code..."
        )
        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val continueApiResult = performSsprContinueCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (continueApiResult) {
                SsprContinueApiResult.Redirect -> {
                    Redirect
                }
                is SsprContinueApiResult.PasswordRequired -> {
                    SsprPasswordRequired(passwordSubmitToken = continueApiResult.passwordSubmitToken)
                }
                is SsprContinueApiResult.OOBIncorrect -> {
                    SsprIncorrectCode(
                        errorCode = continueApiResult.errorCode,
                        errorDescription = continueApiResult.errorDescription
                    )
                }
                is SsprContinueApiResult.UnknownError -> {
                    UnknownError(
                        errorCode = continueApiResult.errorCode,
                        errorDescription = continueApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sspr submit code",
                e
            )
            throw e
        }
    }

    fun ssprResendCode(parameters: SsprResendCodeCommandParameters): SsprResendCodeCommandResult {
        val methodTag = "$TAG:ssprResendCode"

        Logger.verbose(
            methodTag,
            "Performing sspr resend code..."
        )
        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val ssprChallengeApiResult = performSsprChallengeCall(
                oAuth2Strategy = oAuth2Strategy,
                passwordResetToken = parameters.passwordResetToken
            )

            return when (ssprChallengeApiResult) {
                is SsprChallengeApiResult.OOBRequired -> {
                    SsprCodeRequired(
                        passwordResetToken = ssprChallengeApiResult.passwordResetToken,
                        codeLength = ssprChallengeApiResult.codeLength,
                        challengeTargetLabel = ssprChallengeApiResult.challengeTargetLabel,
                        challengeChannel = ssprChallengeApiResult.challengeChannel
                    )
                }
                SsprChallengeApiResult.Redirect -> {
                    Redirect
                }
                is SsprChallengeApiResult.UnknownError -> {
                    UnknownError(
                        errorCode = ssprChallengeApiResult.errorCode,
                        errorDescription = ssprChallengeApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sspr resend code",
                e
            )
            throw e
        }
    }

    fun ssprSubmitNewPassword(parameters: SsprSubmitNewPasswordCommandParameters): SsprSubmitNewPasswordCommandResult {
        val methodTag = "$TAG:ssprSubmitNewPassword"

        Logger.verbose(
            methodTag,
            "Performing sspr password submit..."
        )

        try {
            val strategyParameters = OAuth2StrategyParameters.builder()
                .platformComponents(parameters.platformComponents)
                .build()

            val oAuth2Strategy = parameters
                .authority
                .createOAuth2Strategy(strategyParameters)

            val submitApiResult = performSsprSubmitCall(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters
            )

            return when (submitApiResult) {
                is SsprSubmitApiResult.SubmitSuccess -> {
                    ssprPollCompletion(
                        oAuth2Strategy = oAuth2Strategy,
                        passwordResetToken = submitApiResult.passwordResetToken,
                        pollIntervalInSeconds = submitApiResult.pollInterval
                    )
                }
                is SsprSubmitApiResult.PasswordInvalid -> {
                    PasswordNotAccepted(
                        errorCode = submitApiResult.errorCode,
                        errorDescription = submitApiResult.errorDescription
                    )
                }
                is SsprSubmitApiResult.UnknownError -> {
                    UnknownError(
                        errorCode = submitApiResult.errorCode,
                        errorDescription = submitApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sspr submit new password",
                e
            )
            throw e
        }
    }

    private fun ssprPollCompletion(oAuth2Strategy: NativeAuthOAuth2Strategy, passwordResetToken: String, pollIntervalInSeconds: Int?): SsprSubmitNewPasswordCommandResult {
        fun pollCompletionTimedOut(startTime: Long): Boolean {
            val currentTime = System.currentTimeMillis()
            return currentTime - startTime > SsprSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_IN_MILISECONDS
        }

        fun pollIntervalIsAppropriate(pollIntervalInSeconds: Int?): Boolean {
            return pollIntervalInSeconds != null && pollIntervalInSeconds <= 15 && pollIntervalInSeconds >= 1
        }

        val methodTag = "$TAG:ssprPollCompletion"

        Logger.verbose(
            methodTag,
            "Performing sspr poll completion..."
        )
        try {
            val pollWaitInterval: Int

            if (!pollIntervalIsAppropriate(pollIntervalInSeconds)) {
                pollWaitInterval = SsprSubmitNewPasswordCommand.DEFAULT_POLL_COMPLETION_INTERVAL_IN_MILISECONDS
            } else {
                pollWaitInterval = pollIntervalInSeconds!! * 1000
            }

            var ssprPollCompletionApiResult = performSsprPollCompletionCall(
                oAuth2Strategy = oAuth2Strategy,
                passwordResetToken = passwordResetToken
            )

            val startTime = System.currentTimeMillis()

            while (ssprPollCompletionApiResult is SsprPollCompletionApiResult.InProgress) {
                // TODO: This will use coroutines, most likely shouldn't use thread sleep here
                ThreadUtils.sleepSafely(pollWaitInterval, methodTag, "Waiting between sspr polls")

                if (pollCompletionTimedOut(startTime)) {
                    Logger.warn(
                        methodTag,
                        "Sspr poll completion timed out.",
                        null
                    )
                    return PasswordResetFailed(
                        errorCode = SsprSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_CODE,
                        errorDescription = SsprSubmitNewPasswordCommand.POLL_COMPLETION_TIMEOUT_ERROR_DESCRIPTION
                    )
                }

                Logger.verbose(
                    methodTag,
                    "Calling /poll_complete again..."
                )
                ssprPollCompletionApiResult = performSsprPollCompletionCall(
                    oAuth2Strategy = oAuth2Strategy,
                    passwordResetToken = passwordResetToken
                )
            }

            return when (ssprPollCompletionApiResult) {
                is SsprPollCompletionApiResult.PollingFailed -> {
                    PasswordResetFailed(
                        errorCode = ssprPollCompletionApiResult.errorCode,
                        errorDescription = ssprPollCompletionApiResult.errorDescription
                    )
                }
                is SsprPollCompletionApiResult.PollingSucceeded -> {
                    SsprComplete
                }
                is SsprPollCompletionApiResult.InProgress -> {
                    // This should never be reached, theoretically
                    // TODO feel free to change this errorCode if it's not appropriate
                    UnknownError(
                        errorCode = "illegal_state",
                        errorDescription = "in_progress received after polling, illegal state"
                    )
                }
                is SsprPollCompletionApiResult.UnknownError -> {
                    UnknownError(
                        errorCode = ssprPollCompletionApiResult.errorCode,
                        errorDescription = ssprPollCompletionApiResult.errorDescription
                    )
                }
            }
        } catch (e: Exception) {
            Logger.error(
                methodTag,
                "Error occurred while performing sspr poll completion",
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

    private fun performSsprStartCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SsprStartCommandParameters,
    ): SsprStartApiResult {
        return oAuth2Strategy.performSsprStart(
            parameters = parameters
        )
    }

    private fun performSsprChallengeCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String
    ): SsprChallengeApiResult {
        return oAuth2Strategy.performSsprChallenge(
            passwordResetToken = passwordResetToken
        )
    }

    private fun performSsprContinueCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SsprSubmitCodeCommandParameters,
    ): SsprContinueApiResult {
        return oAuth2Strategy.performSsprContinue(
            parameters = parameters
        )
    }

    private fun performSsprSubmitCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: SsprSubmitNewPasswordCommandParameters,
    ): SsprSubmitApiResult {
        return oAuth2Strategy.performSsprSubmit(
            parameters = parameters,
        )
    }

    private fun performSsprPollCompletionCall(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        passwordResetToken: String,
    ): SsprPollCompletionApiResult {
        return oAuth2Strategy.performSsprPollCompletion(
            passwordResetToken = passwordResetToken
        )
    }

    private fun saveAndReturnTokens(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parametersWithScopes: BaseSignInTokenCommandParameters,
        tokenApiResult: SignInTokenApiResult.Success
    ): Complete {
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

        return Complete(
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
                CodeRequired(
                    credentialToken = this.credentialToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel
                )
            }
            is SignInChallengeApiResult.PasswordRequired -> {
                SignInPasswordRequired(
                    credentialToken = this.credentialToken
                )
            }
            SignInChallengeApiResult.Redirect -> {
                Redirect
            }
            is SignInChallengeApiResult.UnknownError -> {
                UnknownError(
                    errorCode = this.error,
                    errorDescription = this.errorDescription
                )
            }
        }
    }

    private fun SignInChallengeApiResult.toSignInSubmitPasswordCommandResult(): SignInSubmitPasswordCommandResult {
        return when (this) {
            is SignInChallengeApiResult.OOBRequired -> {
                CodeRequired(
                    credentialToken = this.credentialToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel
                )
            }
            SignInChallengeApiResult.Redirect -> {
                Redirect
            }
            is SignInChallengeApiResult.PasswordRequired -> {
                // TODO add correlation ID
                Logger.warn(
                    TAG,
                    "Unexpected API result: $this",
                )
                UnknownError(
                    errorCode = "unexpected_api_result",
                    errorDescription = "API returned unexpected result: $this"
                )
            }
            is SignInChallengeApiResult.UnknownError -> {
                UnknownError(
                    errorCode = this.error,
                    errorDescription = this.errorDescription
                )
            }
        }
    }

    private fun SsprChallengeApiResult.toSsprStartCommandResult(): SsprStartCommandResult {
        return when (this) {
            is SsprChallengeApiResult.OOBRequired -> {
                SsprCodeRequired(
                    passwordResetToken = this.passwordResetToken,
                    codeLength = this.codeLength,
                    challengeTargetLabel = this.challengeTargetLabel,
                    challengeChannel = this.challengeChannel
                )
            }
            SsprChallengeApiResult.Redirect -> {
                Redirect
            }
            is SsprChallengeApiResult.UnknownError -> {
                UnknownError(
                    errorCode = this.errorCode,
                    errorDescription = this.errorDescription
                )
            }
        }
    }
}
