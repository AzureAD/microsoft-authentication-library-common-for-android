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
package com.microsoft.identity.common.nativeauth.internal.controllers.v2

import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.commands.parameters.BaseSignInTokenCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2ResendCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitNewPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.nativeauth.internal.controllers.BaseNativeAuthController
import lombok.EqualsAndHashCode

/**
 * V2 Native Auth SSPR flow controller. Sibling of [com.microsoft.identity.common.nativeauth.internal.controllers.NativeAuthMsalController];
 * does not modify V1 controller behaviour.
 *
 * Each public method is a single command: it builds strategy from parameters, calls the relevant
 * V2 strategy pass-through(s), maps protocol results to command results, and returns. The
 * controller never inspects the continuation token, hrefs, or scopes embedded inside
 * [NativeAuthV2ContinuationState]; it transports the state opaquely between commands.
 *
 * @param sleeper Injected sleep abstraction used by the bounded poll loop in [submitNewPassword].
 *   The default is the production implementation; tests substitute a no-op fake.
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
class NativeAuthV2FlowController(
    private val sleeper: NativeAuthV2PollingSleeper = ProductionNativeAuthV2PollingSleeper()
) : BaseNativeAuthController() {

    companion object {
        private val TAG = NativeAuthV2FlowController::class.java.simpleName

        /** Maximum number of poll attempts before surfacing a timeout error. */
        const val MAX_POLL_ATTEMPTS = 5

        /** Fallback inter-poll delay when the server does not supply a retry-after hint. */
        const val FALLBACK_POLL_DELAY_MS = 1500L

        private const val POLL_TIMEOUT_ERROR = "poll_timeout"
        private const val POLL_TIMEOUT_DESCRIPTION = "Password reset completion polling timed out after $MAX_POLL_ATTEMPTS attempts."
        private const val POLL_INTERRUPTED_ERROR = "poll_interrupted"
        private const val POLL_INTERRUPTED_DESCRIPTION = "Password reset completion polling was interrupted."
        private const val UNEXPECTED_RESULT = "unexpected_api_result"
    }

    // -----------------------------------------------------------------------------------------
    // resetPasswordStart
    // -----------------------------------------------------------------------------------------

    /**
     * Starts the V2 SSPR flow: authorize-challenge start → reset-password entry → challenge.
     *
     * Returns [NativeAuthV2CommandResult.CodeRequired] when the server issues a one-time code,
     * [NativeAuthV2CommandResult.Complete] if the challenge step fast-forwards to completion,
     * [INativeAuthCommandResult.Redirect] for browser-redirect outcomes, or an
     * [INativeAuthCommandResult.APIError] / [NativeAuthV2CommandResult.UserNotFound] for errors.
     */
    fun resetPasswordStart(parameters: ResetPasswordV2StartCommandParameters): NativeAuthV2ResetPasswordStartCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.resetPasswordStart"
        )

        try {
            val oAuth2Strategy = createNativeAuthStrategy(parameters)
            val mergedScopes = addDefaultScopes(parameters.scopes)
            val correlationId = parameters.getCorrelationId()

            // Step 1 — authorize-challenge start
            val authChallengeResult = oAuth2Strategy.performAuthorizeChallengeStart(
                correlationId = correlationId,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = mergedScopes
            )

            val initialState = when (authChallengeResult) {
                is AuthorizeChallengeApiResult.ContinuationRequired -> authChallengeResult.continuationState
                is AuthorizeChallengeApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                    correlationId = authChallengeResult.correlationId,
                    redirectReason = authChallengeResult.redirectReason
                )
                is AuthorizeChallengeApiResult.AuthorizationCode -> {
                    // Unexpected at start — server returned code without any mid-flow interaction.
                    Logger.warn(TAG, authChallengeResult.correlationId, "Unexpected AuthorizationCode at authorize-challenge start.")
                    return INativeAuthCommandResult.APIError(
                        error = UNEXPECTED_RESULT,
                        errorDescription = "AuthorizationCode returned unexpectedly at authorize-challenge start.",
                        correlationId = authChallengeResult.correlationId
                    )
                }
                is AuthorizeChallengeApiResult.UnknownError -> {
                    Logger.warnWithObject(TAG, authChallengeResult.correlationId, "Unexpected result at authorize-challenge start: ", authChallengeResult)
                    return INativeAuthCommandResult.APIError(
                        error = authChallengeResult.error,
                        errorDescription = authChallengeResult.errorDescription,
                        errorCodes = authChallengeResult.errorCodes,
                        correlationId = authChallengeResult.correlationId
                    )
                }
            }

            // Step 2 — reset-password entry (/start equivalent)
            val startResult = oAuth2Strategy.performResetPasswordStart(
                username = parameters.username,
                state = initialState
            )

            val afterStartState = when (startResult) {
                is NativeAuthV2InteractionApiResult.ChallengeRequired -> startResult.continuationState
                is NativeAuthV2InteractionApiResult.UserNotFound -> return NativeAuthV2CommandResult.UserNotFound(
                    correlationId = startResult.correlationId,
                    error = startResult.error,
                    errorDescription = startResult.errorDescription
                )
                is NativeAuthV2InteractionApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                    correlationId = startResult.correlationId,
                    redirectReason = startResult.redirectReason
                )
                else -> return mapInteractionError(startResult)
            }

            // Step 3 — challenge
            val challengeResult = oAuth2Strategy.performChallenge(state = afterStartState)

            return when (challengeResult) {
                is NativeAuthV2InteractionApiResult.CodeRequired -> NativeAuthV2CommandResult.CodeRequired(
                    correlationId = challengeResult.correlationId,
                    continuationState = challengeResult.continuationState,
                    codeLength = challengeResult.codeLength,
                    challengeTargetLabel = challengeResult.challengeTargetLabel,
                    challengeChannel = challengeResult.challengeChannel
                )
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeFlow(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parameters,
                    state = challengeResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                    correlationId = challengeResult.correlationId,
                    redirectReason = challengeResult.redirectReason
                )
                else -> mapInteractionError(challengeResult)
            }
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in resetPasswordStart", e)
            throw e
        }
    }

    // -----------------------------------------------------------------------------------------
    // submitCode
    // -----------------------------------------------------------------------------------------

    /**
     * Submits the one-time code. Returns [NativeAuthV2CommandResult.NewPasswordRequired] on
     * success, [NativeAuthV2CommandResult.IncorrectCode] (carrying the input state) on a bad
     * code, or [NativeAuthV2CommandResult.Complete] if the server fast-forwards.
     */
    fun submitCode(parameters: NativeAuthV2SubmitCodeCommandParameters): NativeAuthV2SubmitCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.submitCode"
        )

        try {
            val oAuth2Strategy = createNativeAuthStrategy(parameters)

            val verifyResult = oAuth2Strategy.performVerify(
                state = parameters.continuationState,
                otp = parameters.code
            )

            return when (verifyResult) {
                is NativeAuthV2InteractionApiResult.UpdateRequired -> NativeAuthV2CommandResult.NewPasswordRequired(
                    correlationId = verifyResult.correlationId,
                    continuationState = verifyResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeFlow(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parameters,
                    state = verifyResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.InvalidCode -> NativeAuthV2CommandResult.IncorrectCode(
                    correlationId = verifyResult.correlationId,
                    error = verifyResult.error,
                    errorDescription = verifyResult.errorDescription,
                    subError = "",
                    retryState = verifyResult.retryState
                )
                is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                    correlationId = verifyResult.correlationId,
                    redirectReason = verifyResult.redirectReason
                )
                else -> mapInteractionError(verifyResult)
            }
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in submitCode", e)
            throw e
        }
    }

    // -----------------------------------------------------------------------------------------
    // resendCode
    // -----------------------------------------------------------------------------------------

    /**
     * Resends the one-time code by calling the resend relation on the continuation state.
     * Returns [NativeAuthV2CommandResult.CodeRequired] with the successor state.
     */
    fun resendCode(parameters: NativeAuthV2ResendCodeCommandParameters): NativeAuthV2ResendCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.resendCode"
        )

        try {
            val oAuth2Strategy = createNativeAuthStrategy(parameters)

            val resendResult = oAuth2Strategy.performResend(state = parameters.continuationState)

            return when (resendResult) {
                is NativeAuthV2InteractionApiResult.CodeRequired -> NativeAuthV2CommandResult.CodeRequired(
                    correlationId = resendResult.correlationId,
                    continuationState = resendResult.continuationState,
                    codeLength = resendResult.codeLength,
                    challengeTargetLabel = resendResult.challengeTargetLabel,
                    challengeChannel = resendResult.challengeChannel
                )
                is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                    correlationId = resendResult.correlationId,
                    redirectReason = resendResult.redirectReason
                )
                else -> mapInteractionError(resendResult)
            }
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in resendCode", e)
            throw e
        }
    }

    // -----------------------------------------------------------------------------------------
    // submitNewPassword
    // -----------------------------------------------------------------------------------------

    /**
     * Submits the new password and polls until the server completes the reset or times out.
     *
     * Returns [NativeAuthV2CommandResult.Complete] on success,
     * [NativeAuthV2CommandResult.PasswordNotAccepted] (carrying the input state) if the password
     * is rejected, [NativeAuthV2CommandResult.PasswordResetFailed] on timeout, or an
     * [INativeAuthCommandResult.APIError] on interruption or server error.
     */
    fun submitNewPassword(parameters: NativeAuthV2SubmitNewPasswordCommandParameters): NativeAuthV2SubmitNewPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.submitNewPassword"
        )

        try {
            val oAuth2Strategy = createNativeAuthStrategy(parameters)

            val updateResult = oAuth2Strategy.performUpdatePassword(
                state = parameters.continuationState,
                newPassword = parameters.newPassword
            )

            return when (updateResult) {
                is NativeAuthV2InteractionApiResult.PollInProgress -> pollUntilComplete(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parameters,
                    pollState = updateResult.continuationState,
                    delayMs = updateResult.retryAfterMillis ?: FALLBACK_POLL_DELAY_MS
                )
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeFlow(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parameters,
                    state = updateResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.InvalidPassword -> NativeAuthV2CommandResult.PasswordNotAccepted(
                    correlationId = updateResult.correlationId,
                    error = updateResult.error,
                    errorDescription = updateResult.errorDescription,
                    subError = "",
                    retryState = updateResult.retryState
                )
                is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                    correlationId = updateResult.correlationId,
                    redirectReason = updateResult.redirectReason
                )
                else -> mapInteractionError(updateResult)
            }
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in submitNewPassword", e)
            throw e
        } finally {
            StringUtil.overwriteWithNull(parameters.newPassword)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Bounded poll loop
    // -----------------------------------------------------------------------------------------

    private fun pollUntilComplete(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: BaseSignInTokenCommandParameters,
        pollState: NativeAuthV2ContinuationState,
        delayMs: Long
    ): NativeAuthV2SubmitNewPasswordCommandResult {
        var currentState = pollState
        var currentDelay = delayMs

        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            if (!sleeper.sleep(currentDelay)) {
                Logger.warn(TAG, currentState.correlationId, "Poll interrupted at attempt ${attempt + 1}.")
                return INativeAuthCommandResult.APIError(
                    error = POLL_INTERRUPTED_ERROR,
                    errorDescription = POLL_INTERRUPTED_DESCRIPTION,
                    correlationId = currentState.correlationId
                )
            }

            val pollResult = oAuth2Strategy.performPoll(state = currentState)

            when (pollResult) {
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> return completeFlow(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parameters,
                    state = pollResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.PollInProgress -> {
                    currentState = pollResult.continuationState
                    currentDelay = pollResult.retryAfterMillis ?: FALLBACK_POLL_DELAY_MS
                }
                is NativeAuthV2InteractionApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                    correlationId = pollResult.correlationId,
                    redirectReason = pollResult.redirectReason
                )
                else -> return mapInteractionError(pollResult)
            }
        }

        Logger.warn(TAG, currentState.correlationId, "Poll exhausted after $MAX_POLL_ATTEMPTS attempts.")
        return NativeAuthV2CommandResult.PasswordResetFailed(
            correlationId = currentState.correlationId,
            error = POLL_TIMEOUT_ERROR,
            errorDescription = POLL_TIMEOUT_DESCRIPTION
        )
    }

    // -----------------------------------------------------------------------------------------
    // completeFlow — shared terminal path
    // -----------------------------------------------------------------------------------------

    /**
     * Completes the V2 SSPR flow by continuing the authorize-challenge interaction, exchanging
     * the resulting authorization code for tokens, and saving them to the cache.
     *
     * Returns [NativeAuthV2CommandResult.Complete] with the resulting [ILocalAuthenticationResult]
     * on success, or an error result on any failure at this stage.
     */
    private fun completeFlow(
        oAuth2Strategy: NativeAuthOAuth2Strategy,
        parameters: BaseSignInTokenCommandParameters,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2CommandResult.Complete {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.completeFlow"
        )

        // Step 1 — authorize-challenge continue → authorization code
        val continueResult = oAuth2Strategy.performAuthorizeChallengeContinue(state = state)
        val code = when (continueResult) {
            is AuthorizeChallengeApiResult.AuthorizationCode -> continueResult.code
            is AuthorizeChallengeApiResult.Redirect -> throw IllegalStateException(
                "Redirect at authorize-challenge continue is not recoverable here; " +
                        "caller should surface Redirect before invoking completeFlow."
            )
            else -> {
                continueResult as ApiErrorResult
                throw IllegalStateException(
                    "authorize-challenge continue failed: ${continueResult.error} — ${continueResult.errorDescription}"
                )
            }
        }

        // Step 2 — token request
        val tokenResult = oAuth2Strategy.performTokenRequest(
            code = code,
            scopes = state.scopesForTokenRequest(),
            correlationId = state.correlationId
        )

        val successTokenResult = when (tokenResult) {
            is SignInTokenApiResult.Success -> tokenResult
            else -> throw IllegalStateException(
                "Token request at flow completion failed: $tokenResult"
            )
        }

        // Step 3 — save tokens and build result
        val complete = saveAndReturnTokens(
            oAuth2Strategy = oAuth2Strategy,
            parametersWithScopes = parameters,
            tokenApiResult = successTokenResult
        )

        return NativeAuthV2CommandResult.Complete(
            correlationId = complete.correlationId,
            authenticationResult = complete.authenticationResult,
            continuationToken = null,
            expiresIn = null
        )
    }

    // -----------------------------------------------------------------------------------------
    // Error mapping helpers
    // -----------------------------------------------------------------------------------------

    /**
     * Maps any [NativeAuthV2InteractionApiResult] that is not handled by the calling when-block
     * to an [INativeAuthCommandResult.APIError]. Called for unexpected outcomes (UnsupportedAction,
     * UnknownError, etc.) as well as outcomes that are valid protocol responses but not expected at
     * a given step (e.g. UpdateRequired returned by the challenge endpoint).
     */
    private fun mapInteractionError(result: NativeAuthV2InteractionApiResult): INativeAuthCommandResult.APIError {
        Logger.warnWithObject(TAG, result.correlationId, "Unexpected interaction result: ", result)
        val error: String
        val errorDescription: String
        val errorCodes: List<Int>?
        if (result is ApiErrorResult) {
            error = result.error
            errorDescription = result.errorDescription
            errorCodes = result.errorCodes
        } else {
            error = UNEXPECTED_RESULT
            errorDescription = "Unexpected API result: $result"
            errorCodes = null
        }
        return INativeAuthCommandResult.APIError(
            error = error,
            errorDescription = errorDescription,
            errorCodes = errorCodes,
            correlationId = result.correlationId
        )
    }
}
