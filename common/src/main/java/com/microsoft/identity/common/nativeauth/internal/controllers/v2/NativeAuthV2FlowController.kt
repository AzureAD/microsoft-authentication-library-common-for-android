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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterSignUpCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitAttributesCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2FlowCompletionCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResetPasswordStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2ResendCodeCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SelectMFAMethodCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInAfterResetPasswordCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInAfterSignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignInStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SignUpStartCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2SubmitAttributesCommandResult
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
 * V2 Native Auth flow controller for the SSPR and sign-in flows.
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

        /** Normalized method type identifying the password authentication method. */
        private const val METHOD_TYPE_PASSWORD = "password"

        /** Reserved sign-up attribute name for the account's email (the username). */
        private const val ATTRIBUTE_NAME_EMAIL = "email"

        /** Reserved sign-up attribute name for the account's password. */
        private const val ATTRIBUTE_NAME_PASSWORD = "password"

        /**
         * Returned when, during sign-up, the server re-requests an attribute that was already
         * submitted (including the always-upfront `email`) and therefore cannot be collected again.
         */
        private const val ATTRIBUTE_ALREADY_SUBMITTED_ERROR = "attribute_already_submitted"
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

            val afterStartState = when (startResult) {
                is NativeAuthV2InteractionApiResult.ChallengeRequired -> startResult.continuationState
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

            val challengeResult = oAuth2Strategy.performChallenge(state = afterStartState)

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
     * Submits the one-time code.
     *
     * For SSPR the server accepts the code and requests a new password, returning
     * [NativeAuthV2CommandResult.NewPasswordRequired]; an [NativeAuthV2InteractionApiResult.InvalidCode]
     * becomes [NativeAuthV2CommandResult.IncorrectCode] (carrying the input state).
     *
     * For sign-up the same verify endpoint may instead request further attributes
     * ([NativeAuthV2InteractionApiResult.AttributesRequired] → a password-required or
     * attributes-required outcome via [handleSignUpAttributesRequired]) or signal server-side
     * completion of a code-only sign-up ([NativeAuthV2InteractionApiResult.ReadyToComplete] →
     * [NativeAuthV2CommandResult.SignInAfterSignUpRequired]). The flow scenario is opaque to this
     * module, so these branches are distinguished by response shape rather than scenario; SSPR
     * never returns those shapes at this step.
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
                is NativeAuthV2InteractionApiResult.AttributesRequired -> {
                    val signUpResult = handleSignUpAttributesRequired(
                        oAuth2Strategy = oAuth2Strategy,
                        attributesRequired = verifyResult,
                        upfront = null
                    )
                    signUpResult as? NativeAuthV2SubmitCodeCommandResult
                        ?: unexpectedSignUpApiError(signUpResult, verifyResult.correlationId)
                }
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> NativeAuthV2CommandResult.SignInAfterSignUpRequired(
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
     * With an entry-supplied password the flow verifies it immediately and returns
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

            val passwordChallengeResult = oAuth2Strategy.performPasswordMethodChallenge(
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
                ?: return NativeAuthV2CommandResult.PasswordRequired(
                    correlationId = passwordChallengeResult.correlationId,
                    continuationState = passwordState
                )

            val verifyResult = oAuth2Strategy.performPasswordVerify(
                state = passwordState,
                password = entryPassword,
                deferredSubmission = false
            )

            return when (verifyResult) {
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeSignIn(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parameters,
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
                password = parameters.password,
                deferredSubmission = true
            )

            return when (verifyResult) {
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeSignIn(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parameters,
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

            val challengeResult = oAuth2Strategy.performMfaMethodChallenge(
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
                is NativeAuthV2InteractionApiResult.AuthMethodBlocked -> NativeAuthV2CommandResult.AuthMethodBlocked(
                    correlationId = challengeResult.correlationId,
                    error = challengeResult.error,
                    errorDescription = challengeResult.errorDescription,
                    subError = challengeResult.subError,
                    errorCodes = challengeResult.errorCodes
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

            val verifyResult = oAuth2Strategy.performMfaVerify(
                state = parameters.continuationState,
                otp = parameters.code
            )

            return when (verifyResult) {
                is NativeAuthV2InteractionApiResult.ReadyToComplete -> completeSignIn(
                    oAuth2Strategy = oAuth2Strategy,
                    parameters = parameters,
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
            parametersWithScopes = parameters.toBuilder()
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
        parameters: SignInV2StartCommandParameters,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2FlowCompletionCommandResult {
        val scopes = addDefaultScopes(parameters.scopes)
        return exchangeCodeAndSaveTokens(
            oAuth2Strategy = oAuth2Strategy,
            parametersWithScopes = parameters.toBuilder()
                .scopes(scopes)
                .claimsRequestJson(parameters.claimsRequestJson?.takeUnless { it.isBlank() })
                .build(),
            state = state
        )
    }

    private fun completeSignIn(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        parameters: NativeAuthV2SubmitPasswordCommandParameters,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2FlowCompletionCommandResult {
        val scopes = addDefaultScopes(parameters.scopes)
        return exchangeCodeAndSaveTokens(
            oAuth2Strategy = oAuth2Strategy,
            parametersWithScopes = parameters.toBuilder()
                .scopes(scopes)
                .claimsRequestJson(parameters.claimsRequestJson?.takeUnless { it.isBlank() })
                .build(),
            state = state
        )
    }

    private fun completeSignIn(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        parameters: NativeAuthV2SubmitMFAChallengeCommandParameters,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2FlowCompletionCommandResult {
        val scopes = addDefaultScopes(parameters.scopes)
        return exchangeCodeAndSaveTokens(
            oAuth2Strategy = oAuth2Strategy,
            parametersWithScopes = parameters.toBuilder()
                .scopes(scopes)
                .claimsRequestJson(parameters.claimsRequestJson?.takeUnless { it.isBlank() })
                .build(),
            state = state
        )
    }

    /**
     * Shared terminal path: continues the authorize-challenge interaction, exchanges the resulting
     * authorization code for tokens using [parametersWithScopes]' merged scopes and claims, and
     * saves the account and tokens to the cache.
     */
    private fun exchangeCodeAndSaveTokens(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        parametersWithScopes: BaseSignInTokenCommandParameters,
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
            scopes = parametersWithScopes.scopes ?: emptyList(),
            correlationId = state.correlationId,
            claimsRequestJson = parametersWithScopes.claimsRequestJson
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
            parametersWithScopes = parametersWithScopes,
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
    // Sign-up
    // -----------------------------------------------------------------------------------------

    /**
     * Starts the V2 sign-up flow: authorize-challenge start (sign-up scenario) → sign-up entry →
     * first collect-attributes step.
     *
     * The sign-up entry posts only the continuation token; the username, an optional password, and
     * any app-supplied attributes are submitted upfront on the first collect-attributes step via
     * [handleSignUpInteractionResult] (see [upfrontAttributeValues]). Depending on what the server
     * requests next this returns [NativeAuthV2CommandResult.CodeRequired] (email one-time code
     * verification), [NativeAuthV2CommandResult.PasswordRequired],
     * [NativeAuthV2CommandResult.AttributesRequired], [NativeAuthV2CommandResult.UserAlreadyExists],
     * or [NativeAuthV2CommandResult.AttributesInvalid], or an
     * [INativeAuthCommandResult.Redirect] / [INativeAuthCommandResult.APIError] on failure.
     */
    fun signUpStart(parameters: SignUpV2StartCommandParameters): NativeAuthV2SignUpStartCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.signUpStart"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)
            val correlationId = parameters.getCorrelationId()

            val authChallengeResult = oAuth2Strategy.performAuthorizeChallengeStart(
                correlationId = correlationId,
                entryRelation = NativeAuthV2LinkRelation.SIGN_UP.value,
                scenario = NativeAuthV2FlowScenario.SIGN_UP,
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

            val startResult = oAuth2Strategy.performSignUpStart(state = initialState)

            val result = handleSignUpInteractionResult(
                oAuth2Strategy = oAuth2Strategy,
                result = startResult,
                retryState = null,
                upfront = parameters
            )
            return result as? NativeAuthV2SignUpStartCommandResult
                ?: unexpectedSignUpApiError(result, correlationId)
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in signUpStart", e)
            throw e
        } finally {
            // The interactor sends the password as part of the attribute map and does not own the
            // input buffer; clear it here so it never outlives the request.
            StringUtil.overwriteWithNull(parameters.password)
        }
    }

    // -----------------------------------------------------------------------------------------
    // submitAttributes
    // -----------------------------------------------------------------------------------------

    /**
     * Submits app-collected attributes from the deferred attributes-required state and routes the
     * server's response through [handleSignUpInteractionResult]. Returns a further
     * [NativeAuthV2CommandResult.AttributesRequired] / [NativeAuthV2CommandResult.PasswordRequired]
     * when more information is needed, [NativeAuthV2CommandResult.SignInAfterSignUpRequired] once
     * the sign-up completes server-side, [NativeAuthV2CommandResult.AttributesInvalid] when a value
     * is rejected (retryable through the same state), [NativeAuthV2CommandResult.UserAlreadyExists],
     * or an [INativeAuthCommandResult.Redirect] / [INativeAuthCommandResult.APIError] on failure.
     */
    fun submitAttributes(parameters: NativeAuthV2SubmitAttributesCommandParameters): NativeAuthV2SubmitAttributesCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.submitAttributes"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)
            val result = performSignUpSubmitAttributes(
                oAuth2Strategy = oAuth2Strategy,
                state = parameters.continuationState,
                attributes = parameters.attributes,
                upfront = null
            )
            return result as? NativeAuthV2SubmitAttributesCommandResult
                ?: unexpectedSignUpApiError(result, parameters.getCorrelationId())
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in submitAttributes", e)
            throw e
        }
    }

    // -----------------------------------------------------------------------------------------
    // signInAfterSignUp
    // -----------------------------------------------------------------------------------------

    /**
     * Explicit app-invoked sign-in step following a completed V2 sign-up flow. This is the only
     * entry point that triggers the token exchange and cache persistence for sign-up; the sign-up
     * steps above never invoke it automatically — they return
     * [NativeAuthV2CommandResult.SignInAfterSignUpRequired] and wait for this command.
     *
     * Returns [NativeAuthV2CommandResult.Complete] on success, or an
     * [INativeAuthCommandResult.Redirect] / [INativeAuthCommandResult.APIError] on failure.
     */
    fun signInAfterSignUp(parameters: NativeAuthV2SignInAfterSignUpCommandParameters): NativeAuthV2SignInAfterSignUpCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = parameters.getCorrelationId(),
            methodName = "$TAG.signInAfterSignUp"
        )

        try {
            val oAuth2Strategy = createNativeAuthV2Strategy(parameters)
            return completeFlowSignUp(
                oAuth2Strategy = oAuth2Strategy,
                parameters = parameters,
                state = parameters.continuationState
            )
        } catch (e: Exception) {
            Logger.error(TAG, parameters.getCorrelationId(), "Exception in signInAfterSignUp", e)
            throw e
        }
    }

    // -----------------------------------------------------------------------------------------
    // Sign-up interaction handling (shared by signUpStart, submitAttributes, and submitCode)
    // -----------------------------------------------------------------------------------------

    /**
     * Maps a sign-up [NativeAuthV2InteractionApiResult] to a command result. [retryState], when
     * non-null, is the continuation state that was just submitted; it is attached to
     * [NativeAuthV2CommandResult.AttributesInvalid] so the app can retry submit-attributes against
     * the same state with corrected values (the server's validation-error body carries no fresh
     * continuation token). [upfront], when non-null, drives the upfront attribute submission on the
     * first collect-attributes step (see [handleSignUpAttributesRequired]).
     *
     * Returns [INativeAuthCommandResult]; callers narrow it to their specific command-result type.
     */
    private fun handleSignUpInteractionResult(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        result: NativeAuthV2InteractionApiResult,
        retryState: NativeAuthV2ContinuationState?,
        upfront: SignUpV2StartCommandParameters?
    ): INativeAuthCommandResult {
        return when (result) {
            is NativeAuthV2InteractionApiResult.AttributesRequired -> handleSignUpAttributesRequired(
                oAuth2Strategy = oAuth2Strategy,
                attributesRequired = result,
                upfront = upfront
            )
            is NativeAuthV2InteractionApiResult.CodeRequired -> NativeAuthV2CommandResult.CodeRequired(
                correlationId = result.correlationId,
                continuationState = result.continuationState,
                codeLength = result.codeLength,
                challengeTargetLabel = result.challengeTargetLabel,
                challengeChannel = result.challengeChannel
            )
            is NativeAuthV2InteractionApiResult.ReadyToComplete -> NativeAuthV2CommandResult.SignInAfterSignUpRequired(
                correlationId = result.correlationId,
                continuationState = result.continuationState
            )
            is NativeAuthV2InteractionApiResult.UserAlreadyExists -> NativeAuthV2CommandResult.UserAlreadyExists(
                correlationId = result.correlationId,
                error = result.error,
                errorDescription = result.errorDescription,
                errorCodes = result.errorCodes
            )
            is NativeAuthV2InteractionApiResult.InvalidAttributes -> if (retryState != null) {
                NativeAuthV2CommandResult.AttributesInvalid(
                    correlationId = result.correlationId,
                    continuationState = retryState,
                    invalidAttributes = result.invalidAttributes,
                    error = result.error,
                    errorDescription = result.errorDescription,
                    errorCodes = result.errorCodes
                )
            } else {
                mapInteractionError(result)
            }
            is NativeAuthV2InteractionApiResult.Redirect -> INativeAuthCommandResult.Redirect(
                correlationId = result.correlationId,
                redirectReason = result.redirectReason
            )
            else -> mapInteractionError(result)
        }
    }

    /**
     * Handles a sign-up `collectAttributes` step.
     *
     * On the first step ([upfront] is non-null, right after sign-up start) every value supplied
     * upfront — `email` (the username), `password` if provided, and any app attributes — is
     * submitted in a single request, regardless of which attributes the server asked for, and the
     * submitted names are recorded on the continuation state by the interactor.
     *
     * On any later step ([upfront] is null) the server is requesting more information. A `password`
     * not yet submitted is surfaced as [NativeAuthV2CommandResult.PasswordRequired] so the app can
     * collect it; any other attribute not yet submitted is surfaced as
     * [NativeAuthV2CommandResult.AttributesRequired]. If the server re-requests `email` (always sent
     * upfront) or any attribute already submitted, that is treated as an
     * [INativeAuthCommandResult.APIError].
     */
    private fun handleSignUpAttributesRequired(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        attributesRequired: NativeAuthV2InteractionApiResult.AttributesRequired,
        upfront: SignUpV2StartCommandParameters?
    ): INativeAuthCommandResult {
        val nextState = attributesRequired.continuationState

        if (upfront != null) {
            return performSignUpSubmitAttributes(
                oAuth2Strategy = oAuth2Strategy,
                state = nextState,
                attributes = upfrontAttributeValues(upfront),
                upfront = null
            )
        }

        val requestsPassword = attributesRequired.requiredAttributes.any {
            it.name.equals(ATTRIBUTE_NAME_PASSWORD, ignoreCase = true)
        }
        if (requestsPassword && !nextState.hasSubmittedAttribute(ATTRIBUTE_NAME_PASSWORD)) {
            return NativeAuthV2CommandResult.PasswordRequired(
                correlationId = attributesRequired.correlationId,
                continuationState = nextState
            )
        }

        val alreadySubmitted = attributesRequired.requiredAttributes.firstOrNull {
            nextState.hasSubmittedAttribute(it.name)
        }
        if (alreadySubmitted != null) {
            Logger.warn(TAG, attributesRequired.correlationId, "Server re-requested an already-submitted sign-up attribute.")
            return INativeAuthCommandResult.APIError(
                error = ATTRIBUTE_ALREADY_SUBMITTED_ERROR,
                errorDescription = "The server requested attribute '${alreadySubmitted.name}' that was already submitted or cannot be collected.",
                correlationId = attributesRequired.correlationId
            )
        }

        return NativeAuthV2CommandResult.AttributesRequired(
            correlationId = attributesRequired.correlationId,
            continuationState = nextState,
            requiredAttributes = attributesRequired.requiredAttributes
        )
    }

    /**
     * Posts [attributes] to the sign-up submit-attributes href carried by [state] and routes the
     * response through [handleSignUpInteractionResult], attaching [state] as the retry state for a
     * possible attribute-validation error.
     */
    private fun performSignUpSubmitAttributes(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        state: NativeAuthV2ContinuationState,
        attributes: Map<String, String>,
        upfront: SignUpV2StartCommandParameters?
    ): INativeAuthCommandResult {
        val result = oAuth2Strategy.performSubmitAttributes(state = state, attributes = attributes)
        return handleSignUpInteractionResult(
            oAuth2Strategy = oAuth2Strategy,
            result = result,
            retryState = state,
            upfront = upfront
        )
    }

    /**
     * Builds the attribute map submitted upfront: `email` (the username) plus any password and app
     * attributes supplied to sign-up. The SDK-owned `email` and `password` keys cannot be
     * overridden by app-supplied attributes; any such attribute (matched case-insensitively) is
     * ignored.
     */
    private fun upfrontAttributeValues(parameters: SignUpV2StartCommandParameters): Map<String, String> {
        val values = LinkedHashMap<String, String>()
        values[ATTRIBUTE_NAME_EMAIL] = parameters.username

        val password = parameters.password
        if (password != null && password.isNotEmpty()) {
            values[ATTRIBUTE_NAME_PASSWORD] = String(password)
        }

        parameters.attributes?.forEach { (name, value) ->
            if (name.lowercase() == ATTRIBUTE_NAME_EMAIL || name.lowercase() == ATTRIBUTE_NAME_PASSWORD) {
                Logger.warn(TAG, parameters.getCorrelationId(), "Ignoring app-supplied sign-up attribute because it uses a reserved SDK attribute name.")
            } else {
                values[name] = value
            }
        }

        return values
    }

    /**
     * Completes the V2 sign-up flow. Shares [exchangeCodeAndSaveTokens] with SSPR and sign-in, so
     * scopes, claims, correlation ID, token exchange, and cache persistence behave identically
     * across all flows.
     */
    private fun completeFlowSignUp(
        oAuth2Strategy: NativeAuthV2OAuth2Strategy,
        parameters: NativeAuthV2SignInAfterSignUpCommandParameters,
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2SignInAfterSignUpCommandResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = state.correlationId,
            methodName = "$TAG.completeFlowSignUp"
        )

        val scopes = addDefaultScopes(parameters.scopes)
        val claimsRequestJson = parameters.claimsRequestJson?.takeUnless { it.isBlank() }
        return exchangeCodeAndSaveTokens(
            oAuth2Strategy = oAuth2Strategy,
            parametersWithScopes = parameters.toBuilder()
                .scopes(scopes)
                .claimsRequestJson(claimsRequestJson)
                .build(),
            state = state
        )
    }

    /**
     * Logs and wraps a sign-up outcome that does not conform to the caller's expected command
     * result type — a defensive guard for a misbehaving server; the controlled flows above never
     * produce such an outcome.
     */
    private fun unexpectedSignUpApiError(
        result: INativeAuthCommandResult,
        correlationId: String
    ): INativeAuthCommandResult.APIError {
        Logger.warn(TAG, correlationId, "Unexpected sign-up result: $result")
        return INativeAuthCommandResult.APIError(
            error = UNEXPECTED_RESULT,
            errorDescription = "Unexpected sign-up result.",
            correlationId = correlationId
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
