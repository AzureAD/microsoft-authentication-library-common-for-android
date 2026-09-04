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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SelectMFAMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterResetPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitMFAChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2FlowCompletionCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SelectMFAMethodCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInAfterResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitMFAChallengeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitNewPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.java.util.ThreadUtils
import com.microsoft.identity.common.nativeauth.internal.controllers.BaseNativeAuthController
import lombok.EqualsAndHashCode

/**
 * V2 Native Auth flow controller
 */
@EqualsAndHashCode(callSuper = true, onlyExplicitlyIncluded = true)
class NativeAuthV2FlowController : BaseNativeAuthController() {

    companion object {
        private val TAG = NativeAuthV2FlowController::class.java.simpleName

        /** Maximum number of poll attempts before surfacing a timeout error. */
        const val MAX_POLL_ATTEMPTS = 5

        /** Fallback inter-poll delay when the server does not supply a retry-after hint. */
        const val FALLBACK_POLL_DELAY_MS = 1500L

        /** Maximum server-suggested delay honored between poll attempts. */
        const val MAX_POLL_DELAY_MS = 30_000L

        private const val POLL_TIMEOUT_ERROR = "poll_timeout"
        private const val POLL_TIMEOUT_DESCRIPTION = "Password reset completion polling timed out after $MAX_POLL_ATTEMPTS attempts."
        private const val POLL_INTERRUPTED_ERROR = "poll_interrupted"
        private const val POLL_INTERRUPTED_DESCRIPTION = "Password reset completion polling was interrupted."
        private const val UNEXPECTED_RESULT = "unexpected_api_result"

        /**
         * Returned when the server does not offer the password first factor. This scoped V2 API
         * only supports password as the first factor; an email one-time code first factor is
         * deliberately not treated as a fallback.
         */
        private const val UNSUPPORTED_FIRST_FACTOR = "unsupported_first_factor"
        private const val UNSUPPORTED_CHALLENGE_METHOD = "unsupported_challenge_method"

        /** Normalized method type identifying the password authentication method. */
        private const val METHOD_TYPE_PASSWORD = "password"
        private const val METHOD_TYPE_EMAIL = "email"
    }

    // -----------------------------------------------------------------------------------------
    // resetPasswordStart
    // -----------------------------------------------------------------------------------------

    /**
     * Starts the V2 SSPR flow: authorize-challenge start → reset-password entry → challenge.
     *
     * Returns [NativeAuthV2CommandResult.CodeRequired] when the server issues a one-time code,
     * [INativeAuthCommandResult.Redirect] for browser-redirect outcomes, or an
     * [INativeAuthCommandResult.APIError] / [NativeAuthV2CommandResult.UserNotFound] for errors.
     *
     * A [NativeAuthV2InteractionApiResult.ReadyToComplete] at either the reset-password entry or
     * the challenge step is rejected as an [INativeAuthCommandResult.APIError]: no credential has
     * been proven at that point, so the flow must not hand back a state that is exchangeable for
     * tokens.
     */
    fun resetPasswordStart(parameters: ResetPasswordV2StartCommandParameters): NativeAuthV2ResetPasswordStartCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.resetPasswordStart"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)
            val correlationId = parameters.getCorrelationId()

            val authChallengeResult = oAuth2Strategy.performAuthorizeChallengeStart(
                correlationId = correlationId,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD.value,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = emptyList(),
                claimsRequestJson = null
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

            val startResult = oAuth2Strategy.performResetPasswordStart(
                username = parameters.username,
                state = initialState
            )

            val challenge = when (startResult) {
                is NativeAuthV2InteractionApiResult.ChallengeRequired -> startResult
                is NativeAuthV2InteractionApiResult.UserNotFound -> return NativeAuthV2CommandResult.UserNotFound(
                    correlationId = startResult.correlationId,
                    error = startResult.error,
                    errorDescription = startResult.errorDescription,
                    errorCodes = startResult.errorCodes
                )
                is NativeAuthV2InteractionApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                    correlationId = startResult.correlationId,
                    redirectReason = startResult.redirectReason
                )
                else -> return mapInteractionError(startResult)
            }

            val emailMethod = challenge.methods.firstOrNull { it.type == METHOD_TYPE_EMAIL }
                ?: return INativeAuthCommandResult.APIError(
                    error = UNSUPPORTED_CHALLENGE_METHOD,
                    errorDescription = "Native Auth V2 password reset requires an email " +
                            "authentication method, which the server did not offer.",
                    correlationId = challenge.correlationId
                )

            val challengeResult = oAuth2Strategy.performMethodChallenge(
                state = challenge.continuationState,
                methodId = emailMethod.id
            )

            return when (challengeResult) {
                is NativeAuthV2InteractionApiResult.CodeRequired -> NativeAuthV2CommandResult.CodeRequired(
                    correlationId = challengeResult.correlationId,
                    continuationState = challengeResult.continuationState,
                    codeLength = challengeResult.codeLength,
                    challengeTargetLabel = challengeResult.challengeTargetLabel,
                    challengeChannel = challengeResult.challengeChannel
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
     * success or [NativeAuthV2CommandResult.IncorrectCode] (carrying the input state) on a bad
     * code.
     *
     * A [NativeAuthV2InteractionApiResult.ReadyToComplete] here is rejected as an
     * [INativeAuthCommandResult.APIError]: the reset cannot have completed before a new password
     * was submitted.
     */
    fun submitCode(parameters: NativeAuthV2SubmitCodeCommandParameters): NativeAuthV2SubmitCodeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.submitCode"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)

            val verifyResult = oAuth2Strategy.performVerify(
                state = parameters.continuationState,
                otp = parameters.code
            )

            return when (verifyResult) {
                is NativeAuthV2InteractionApiResult.UpdateRequired -> NativeAuthV2CommandResult.NewPasswordRequired(
                    correlationId = verifyResult.correlationId,
                    continuationState = verifyResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.InvalidCode -> NativeAuthV2CommandResult.IncorrectCode(
                    correlationId = verifyResult.correlationId,
                    error = verifyResult.error,
                    errorDescription = verifyResult.errorDescription,
                    subError = verifyResult.subError,
                    errorCodes = verifyResult.errorCodes
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
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)

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
     * Returns [NativeAuthV2CommandResult.SignInAfterResetPasswordRequired] on success,
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
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)

            val updateResult = oAuth2Strategy.performUpdatePassword(
                state = parameters.continuationState,
                newPassword = parameters.newPassword
            )

            return when (updateResult) {
                is NativeAuthV2InteractionApiResult.PollInProgress -> pollUntilComplete(
                    oAuth2Strategy = oAuth2Strategy,
                    pollState = updateResult.continuationState,
                    delayMs = updateResult.retryAfterMillis ?: FALLBACK_POLL_DELAY_MS
                )
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(
                    correlationId = updateResult.correlationId,
                    continuationState = updateResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.InvalidPassword -> NativeAuthV2CommandResult.PasswordNotAccepted(
                    correlationId = updateResult.correlationId,
                    error = updateResult.error,
                    errorDescription = updateResult.errorDescription,
                    subError = updateResult.subError,
                    errorCodes = updateResult.errorCodes
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
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        pollState: NativeAuthV2ContinuationState,
        delayMs: Long
    ): NativeAuthV2SubmitNewPasswordCommandResult {
        var currentState = pollState
        var currentDelay = delayMs

        repeat(MAX_POLL_ATTEMPTS) { attempt ->
            ThreadUtils.sleepSafely(
                currentDelay.coerceIn(0L, MAX_POLL_DELAY_MS).toInt(),
                TAG,
                "Waiting between reset password polls"
            )

            if (Thread.currentThread().isInterrupted) {
                Logger.warn(TAG, currentState.correlationId, "Poll interrupted at attempt ${attempt + 1}.")
                return INativeAuthCommandResult.APIError(
                    error = POLL_INTERRUPTED_ERROR,
                    errorDescription = POLL_INTERRUPTED_DESCRIPTION,
                    correlationId = currentState.correlationId
                )
            }

            val pollResult = oAuth2Strategy.performPoll(state = currentState)

            when (pollResult) {
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> return NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(
                    correlationId = pollResult.correlationId,
                    continuationState = pollResult.continuationState
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
    // signInAfterResetPassword
    // -----------------------------------------------------------------------------------------

    /**
     * Explicit app-invoked sign-in step following a completed V2 SSPR flow. This is the only
     * entry point that triggers [completeFlow]'s token exchange and cache persistence; the
     * reset-password steps above never invoke it automatically — they return
     * [NativeAuthV2CommandResult.SignInAfterResetPasswordRequired] and wait for this command.
     *
     * Returns [NativeAuthV2CommandResult.Complete] on success, or an
     * [INativeAuthCommandResult.Redirect] / [INativeAuthCommandResult.APIError] on failure.
     */
    fun signInAfterResetPassword(parameters: NativeAuthV2SignInAfterResetPasswordCommandParameters): NativeAuthV2SignInAfterResetPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.signInAfterResetPassword"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)
            return completeFlow(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters,
                state = parameters.continuationState
            )
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in signInAfterResetPassword", e)
            throw e
        }
    }

    // -----------------------------------------------------------------------------------------
    // signInStart
    // -----------------------------------------------------------------------------------------

    /**
     * Starts the V2 sign-in flow: authorize-challenge start → sign-in entry → password-method
     * challenge → optional password verification.
     *
     * This scoped API always drives the password first factor. When the server does not offer a
     * password method the flow fails deterministically rather than falling back to an email
     * one-time code, which is out of scope for this increment.
     *
     * With a non-empty entry-supplied password the flow verifies it immediately and returns
     * [NativeAuthV2CommandResult.Complete] or [NativeAuthV2CommandResult.MFARequired]; without one
     * it returns [NativeAuthV2CommandResult.PasswordRequired] and waits for [submitPassword].
     */
    fun signInStart(parameters: SignInV2StartCommandParameters): NativeAuthV2SignInStartCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.signInStart"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)
            val correlationId = parameters.getCorrelationId()

            val authChallengeResult = oAuth2Strategy.performAuthorizeChallengeStart(
                correlationId = correlationId,
                entryRelation = NativeAuthV2LinkRelation.SIGN_IN.value,
                scenario = NativeAuthV2FlowScenario.SIGN_IN,
                scopes = parameters.scopes ?: emptyList(),
                claimsRequestJson = parameters.claimsRequestJson
            )

            val initialState = when (authChallengeResult) {
                is AuthorizeChallengeApiResult.ContinuationRequired -> authChallengeResult.continuationState
                is AuthorizeChallengeApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                    correlationId = authChallengeResult.correlationId,
                    redirectReason = authChallengeResult.redirectReason
                )
                is AuthorizeChallengeApiResult.AuthorizationCode -> {
                    // No credential has been proven yet, so a code here must not be exchanged.
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

            val startResult = oAuth2Strategy.performSignInStart(
                username = parameters.username,
                state = initialState
            )

            val firstFactorChallenge = when (startResult) {
                is NativeAuthV2InteractionApiResult.ChallengeRequired -> startResult
                is NativeAuthV2InteractionApiResult.UserNotFound -> return NativeAuthV2CommandResult.UserNotFound(
                    correlationId = startResult.correlationId,
                    error = startResult.error,
                    errorDescription = startResult.errorDescription,
                    errorCodes = startResult.errorCodes
                )
                is NativeAuthV2InteractionApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                    correlationId = startResult.correlationId,
                    redirectReason = startResult.redirectReason
                )
                else -> return mapInteractionError(startResult)
            }

            val passwordMethod = firstFactorChallenge.methods
                .firstOrNull { it.type == METHOD_TYPE_PASSWORD }
                ?: run {
                    Logger.warn(TAG, firstFactorChallenge.correlationId, "Server did not offer the password first factor.")
                    return INativeAuthCommandResult.APIError(
                        error = UNSUPPORTED_FIRST_FACTOR,
                        errorDescription = "Native Auth V2 sign-in requires the password first " +
                                "factor, which the server did not offer for this account.",
                        correlationId = firstFactorChallenge.correlationId
                    )
                }

            val passwordChallengeResult = oAuth2Strategy.performMethodChallenge(
                state = firstFactorChallenge.continuationState,
                methodId = passwordMethod.id
            )

            val passwordState = when (passwordChallengeResult) {
                is NativeAuthV2InteractionApiResult.PasswordRequired -> passwordChallengeResult.continuationState
                is NativeAuthV2InteractionApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                    correlationId = passwordChallengeResult.correlationId,
                    redirectReason = passwordChallengeResult.redirectReason
                )
                else -> return mapInteractionError(passwordChallengeResult)
            }

            val entryPassword = parameters.password
            if (entryPassword == null || entryPassword.isEmpty()) {
                return NativeAuthV2CommandResult.PasswordRequired(
                    correlationId = passwordChallengeResult.correlationId,
                    continuationState = passwordState
                )
            }

            val verifyResult = oAuth2Strategy.performPasswordVerify(
                state = passwordState,
                password = entryPassword
            )

            return when (verifyResult) {
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeSignIn(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersBuilder = parameters.toBuilder(),
                    scopes = parameters.scopes,
                    claimsRequestJson = parameters.claimsRequestJson,
                    state = verifyResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.MFARequired -> NativeAuthV2CommandResult.MFARequired(
                    correlationId = verifyResult.correlationId,
                    continuationState = verifyResult.continuationState,
                    authMethods = verifyResult.methods
                )
                is NativeAuthV2InteractionApiResult.InvalidCredentials -> NativeAuthV2CommandResult.InvalidCredentials(
                    correlationId = verifyResult.correlationId,
                    error = verifyResult.error,
                    errorDescription = verifyResult.errorDescription,
                    subError = verifyResult.subError,
                    errorCodes = verifyResult.errorCodes
                )
                is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                    correlationId = verifyResult.correlationId,
                    redirectReason = verifyResult.redirectReason
                )
                else -> mapInteractionError(verifyResult)
            }
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in signInStart", e)
            throw e
        } finally {
            // The interactor already clears the buffer it sent; this also covers the paths that
            // never reached it (unsupported first factor, redirect, protocol error).
            StringUtil.overwriteWithNull(parameters.password)
        }
    }

    // -----------------------------------------------------------------------------------------
    // submitPassword
    // -----------------------------------------------------------------------------------------

    /**
     * Submits a password from the deferred password-required state.
     *
     * A rejected password becomes [NativeAuthV2CommandResult.IncorrectPassword] rather than
     * [NativeAuthV2CommandResult.InvalidCredentials], preserving the submission context the public
     * layer needs to match the iOS V2 error taxonomy.
     */
    fun submitPassword(parameters: NativeAuthV2SubmitPasswordCommandParameters): NativeAuthV2SubmitPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.submitPassword"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)

            val verifyResult = oAuth2Strategy.performPasswordVerify(
                state = parameters.continuationState,
                password = parameters.password
            )

            return when (verifyResult) {
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeSignIn(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersBuilder = parameters.toBuilder(),
                    scopes = verifyResult.continuationState.scopesForTokenRequest(),
                    claimsRequestJson = verifyResult.continuationState.claimsRequestJsonForTokenRequest(),
                    state = verifyResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.MFARequired -> NativeAuthV2CommandResult.MFARequired(
                    correlationId = verifyResult.correlationId,
                    continuationState = verifyResult.continuationState,
                    authMethods = verifyResult.methods
                )
                is NativeAuthV2InteractionApiResult.InvalidCredentials -> NativeAuthV2CommandResult.IncorrectPassword(
                    correlationId = verifyResult.correlationId,
                    error = verifyResult.error,
                    errorDescription = verifyResult.errorDescription,
                    subError = verifyResult.subError,
                    errorCodes = verifyResult.errorCodes
                )
                is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                    correlationId = verifyResult.correlationId,
                    redirectReason = verifyResult.redirectReason
                )
                else -> mapInteractionError(verifyResult)
            }
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in submitPassword", e)
            throw e
        } finally {
            StringUtil.overwriteWithNull(parameters.password)
        }
    }

    // -----------------------------------------------------------------------------------------
    // selectMFAMethod
    // -----------------------------------------------------------------------------------------

    /**
     * Challenges the multi-factor method the app selected, following the href the server attached
     * to that method. No challenge is ever sent without an explicit selection.
     */
    fun selectMFAMethod(parameters: NativeAuthV2SelectMFAMethodCommandParameters): NativeAuthV2SelectMFAMethodCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.selectMFAMethod"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)

            val challengeResult = oAuth2Strategy.performMethodChallenge(
                state = parameters.continuationState,
                methodId = parameters.methodId
            )

            return when (challengeResult) {
                is NativeAuthV2InteractionApiResult.CodeRequired -> NativeAuthV2CommandResult.MFAVerificationRequired(
                    correlationId = challengeResult.correlationId,
                    continuationState = challengeResult.continuationState,
                    codeLength = challengeResult.codeLength,
                    challengeTargetLabel = challengeResult.challengeTargetLabel,
                    challengeChannel = challengeResult.challengeChannel
                )
                is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                    correlationId = challengeResult.correlationId,
                    redirectReason = challengeResult.redirectReason
                )
                else -> mapInteractionError(challengeResult)
            }
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in selectMFAMethod", e)
            throw e
        }
    }

    // -----------------------------------------------------------------------------------------
    // submitMFAChallenge
    // -----------------------------------------------------------------------------------------

    /**
     * Submits the multi-factor one-time code and, on success, completes the flow through
     * authorize-challenge continue, the authorization-code token exchange, and cache persistence.
     * A wrong code becomes [NativeAuthV2CommandResult.IncorrectCode], which the app can retry from
     * the state it already holds.
     */
    fun submitMFAChallenge(parameters: NativeAuthV2SubmitMFAChallengeCommandParameters): NativeAuthV2SubmitMFAChallengeCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.submitMFAChallenge"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)

            val verifyResult = oAuth2Strategy.performVerify(
                state = parameters.continuationState,
                otp = parameters.code
            )

            return when (verifyResult) {
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeSignIn(
                    oAuth2Strategy = oAuth2Strategy,
                    parametersBuilder = parameters.toBuilder(),
                    scopes = verifyResult.continuationState.scopesForTokenRequest(),
                    claimsRequestJson = verifyResult.continuationState.claimsRequestJsonForTokenRequest(),
                    state = verifyResult.continuationState
                )
                is NativeAuthV2InteractionApiResult.InvalidCode -> NativeAuthV2CommandResult.IncorrectCode(
                    correlationId = verifyResult.correlationId,
                    error = verifyResult.error,
                    errorDescription = verifyResult.errorDescription,
                    subError = verifyResult.subError,
                    errorCodes = verifyResult.errorCodes
                )
                is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                    correlationId = verifyResult.correlationId,
                    redirectReason = verifyResult.redirectReason
                )
                else -> mapInteractionError(verifyResult)
            }
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in submitMFAChallenge", e)
            throw e
        }
    }

    // -----------------------------------------------------------------------------------------
    // completeFlow — shared terminal path
    // -----------------------------------------------------------------------------------------

    /**
     * Completes the V2 SSPR flow by continuing the authorize-challenge interaction, exchanging
     * the resulting authorization code for tokens, and saving them to the cache. This is the
     * shared terminal path invoked only by [signInAfterResetPassword], never automatically by the
     * reset-password steps above — those return [NativeAuthV2CommandResult.SignInAfterResetPasswordRequired]
     * instead and defer this work until the app explicitly signs in.
     *
     * Returns [NativeAuthV2CommandResult.Complete] with the resulting [ILocalAuthenticationResult]
     * on success, or a typed [INativeAuthCommandResult.Redirect] / [INativeAuthCommandResult.APIError]
     * on any failure at this stage.
     */
    private fun completeFlow(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        parameters: NativeAuthV2SignInAfterResetPasswordCommandParameters,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2SignInAfterResetPasswordCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.completeFlow"
        )

        val scopes = addDefaultScopes(parameters.scopes)
        val claimsRequestJson = parameters.claimsRequestJson?.takeUnless { it.isBlank() }
        return exchangeCodeAndSaveTokens(
            oAuth2Strategy = oAuth2Strategy,
            tokenCommandParameters = parameters.toBuilder()
                .scopes(scopes)
                .claimsRequestJson(claimsRequestJson)
                .build(),
            state = state
        )
    }

    /**
     * Completes the V2 sign-in flow once the server has signalled that every required factor is
     * satisfied. Shares [exchangeCodeAndSaveTokens] with SSPR, so scopes, claims, correlation ID,
     * token exchange, and cache persistence behave identically across both flows.
     */
    private fun completeSignIn(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        parametersBuilder: BaseSignInTokenCommandParameters.BaseSignInTokenCommandParametersBuilder<*, *>,
        scopes: List<String>?,
        claimsRequestJson: String?,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2FlowCompletionCommandResult {
        return exchangeCodeAndSaveTokens(
            oAuth2Strategy = oAuth2Strategy,
            tokenCommandParameters = parametersBuilder
                .scopes(addDefaultScopes(scopes))
                .claimsRequestJson(claimsRequestJson?.takeUnless { it.isBlank() })
                .build(),
            state = state
        )
    }

    /**
     * Shared terminal path: continues the authorize-challenge interaction, exchanges the resulting
     * authorization code for tokens using [tokenCommandParameters]' merged scopes and claims, and
     * saves the account and tokens to the cache.
     */
    private fun exchangeCodeAndSaveTokens(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        tokenCommandParameters: BaseSignInTokenCommandParameters,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2FlowCompletionCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.exchangeCodeAndSaveTokens"
        )

        val continueResult = oAuth2Strategy.performAuthorizeChallengeContinue(state = state)
        val code = when (continueResult) {
            is AuthorizeChallengeApiResult.AuthorizationCode -> continueResult.code
            is AuthorizeChallengeApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                correlationId = continueResult.correlationId,
                redirectReason = continueResult.redirectReason
            )
            is AuthorizeChallengeApiResult.ContinuationRequired -> return INativeAuthCommandResult.APIError(
                error = UNEXPECTED_RESULT,
                errorDescription = "ContinuationRequired returned unexpectedly at authorize-challenge continue.",
                correlationId = continueResult.correlationId
            )
            is AuthorizeChallengeApiResult.UnknownError -> {
                return INativeAuthCommandResult.APIError(
                    error = continueResult.error,
                    errorDescription = continueResult.errorDescription,
                    errorCodes = continueResult.errorCodes,
                    correlationId = continueResult.correlationId
                )
            }
        }

        val tokenResult = oAuth2Strategy.performTokenRequest(
            code = code,
            scopes = tokenCommandParameters.scopes ?: emptyList(),
            correlationId = state.correlationId,
            claimsRequestJson = tokenCommandParameters.claimsRequestJson
        )

        val successTokenResult = when (tokenResult) {
            is SignInTokenApiResult.Success -> tokenResult
            is SignInTokenApiResult.Redirect -> return INativeAuthCommandResult.Redirect(
                correlationId = tokenResult.correlationId,
                redirectReason = tokenResult.redirectReason
            )
            else -> {
                tokenResult as ApiErrorResult
                return INativeAuthCommandResult.APIError(
                    error = tokenResult.error,
                    errorDescription = tokenResult.errorDescription,
                    errorCodes = tokenResult.errorCodes,
                    correlationId = tokenResult.correlationId
                )
            }
        }

        val complete = saveAndReturnTokens(
            oAuth2Strategy = oAuth2Strategy,
            parametersWithScopes = tokenCommandParameters,
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
        val error: String?
        val errorDescription: String?
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
