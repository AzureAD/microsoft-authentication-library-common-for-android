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
package com.microsoft.identity.common.java.nativeauth.controllers.results

import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult

// Per-operation sealed marker interfaces for exhaustive when() dispatch.
sealed interface NativeAuthV2ResetPasswordStartCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SubmitCodeCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2ResendCodeCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SubmitNewPasswordCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SignInAfterResetPasswordCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SignInStartCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SubmitPasswordCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SelectMFAMethodCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SubmitMFAChallengeCommandResult : INativeAuthCommandResult

/**
 * Results producible by the shared V2 terminal path (authorize-challenge continue →
 * authorization-code token exchange → cache persistence), which every V2 flow that reaches
 * completion funnels through. Grouping them lets that path keep a single return type instead of
 * repeating the same three outcomes per operation.
 */
sealed interface NativeAuthV2FlowCompletionCommandResult :
    NativeAuthV2SignInAfterResetPasswordCommandResult,
    NativeAuthV2SignInStartCommandResult,
    NativeAuthV2SubmitPasswordCommandResult,
    NativeAuthV2SubmitMFAChallengeCommandResult

/**
 * Reflects the possible results from the V2 SSPR (self-service password reset) command flow.
 *
 * Conforms to [INativeAuthCommandResult] and covers all terminal and continuation states
 * returned by the V2 reset-password protocol endpoints.
 *
 * Error cases reuse the shared [INativeAuthCommandResult.APIError] and
 * [INativeAuthCommandResult.Redirect] types declared in [INativeAuthCommandResult]; those types
 * are wired to the marker interfaces above in [INativeAuthCommandResult].
 */
interface NativeAuthV2CommandResult {

    /**
     * The server has sent a one-time code to the user's registered channel.
     * [continuationState] is the opaque mid-flow state to be passed back on the submit-code step.
     * Applies to start and resend-code steps.
     */
    data class CodeRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val codeLength: Int,
        val challengeTargetLabel: String,
        val challengeChannel: String,
    ) : NativeAuthV2ResetPasswordStartCommandResult, NativeAuthV2ResendCodeCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }

    /**
     * The server has accepted the OTP and is requesting a new password.
     * [continuationState] is the opaque mid-flow state to be passed back on the submit-new-password step.
     * Applies to the submit-code step.
     */
    data class NewPasswordRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
    ) : NativeAuthV2SubmitCodeCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.NewPasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The password reset has completed server-side. The app must explicitly invoke the
     * sign-in-after-reset command with [continuationState] to exchange it for tokens and persist
     * them to cache; no token exchange or cache write happens until then.
     *
     * Applies only to the submit-new-password step, including its fast-forward and poll-completion
     * paths. It is deliberately not reachable from the start, challenge, or submit-code steps: no
     * new password has been supplied at those points, so the flow rejects a server-side completion
     * signal there rather than returning a state that is exchangeable for tokens.
     */
    data class SignInAfterResetPasswordRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
    ) : NativeAuthV2SubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.SignInAfterResetPasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The password reset flow has completed successfully.
     * [authenticationResult] may be non-null when the server returns a sign-in continuation token
     * that was redeemed inline; it is null when no automatic sign-in was performed.
     * Applies to any step that reaches terminal completion, including the explicit
     * sign-in-after-reset command.
     */
    data class Complete(
        override val correlationId: String,
        val authenticationResult: ILocalAuthenticationResult?,
        val continuationToken: String?,
        val expiresIn: Int?,
    ) : NativeAuthV2ResetPasswordStartCommandResult,
        NativeAuthV2SubmitCodeCommandResult,
        NativeAuthV2ResendCodeCommandResult,
        NativeAuthV2SubmitNewPasswordCommandResult,
        NativeAuthV2FlowCompletionCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.Complete(correlationId=$correlationId, expiresIn=$expiresIn)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The server rejected the submitted OTP.
     * The developer can retry through the state object used for the failed call.
     * Applies to the submit-code step.
     */
    data class IncorrectCode(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
        val errorCodes: List<Int>? = null,
    ) : NativeAuthV2SubmitCodeCommandResult, NativeAuthV2SubmitMFAChallengeCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.IncorrectCode(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.IncorrectCode(correlationId=$correlationId)"
    }

    /**
     * The server offered the password first factor and no password was supplied at the sign-in
     * entry point, so the app must collect one and submit it through [continuationState].
     * Applies to the sign-in start step.
     */
    data class PasswordRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
    ) : NativeAuthV2SignInStartCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.PasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The first factor succeeded and the server requires a second factor. [authMethods] are the
     * methods the server offered, in server order; the app must select one explicitly before any
     * challenge is sent.
     * Applies to the sign-in start and submit-password steps.
     */
    data class MFARequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val authMethods: List<NativeAuthV2AuthMethod>,
    ) : NativeAuthV2SignInStartCommandResult, NativeAuthV2SubmitPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.MFARequired(correlationId=$correlationId, authMethods=${authMethods.map { it.toUnsanitizedString() }})"

        override fun toString(): String =
            "NativeAuthV2CommandResult.MFARequired(correlationId=$correlationId, authMethods=${authMethods.map { it.toString() }})"
    }

    /**
     * The server sent a multi-factor challenge to the selected method and awaits the code.
     * Applies to the select-MFA-method step.
     */
    data class MFAVerificationRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val codeLength: Int,
        val challengeTargetLabel: String,
        val challengeChannel: String,
    ) : NativeAuthV2SelectMFAMethodCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.MFAVerificationRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.MFAVerificationRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }

    /**
     * The server rejected the username/password combination supplied at the sign-in entry point.
     * The developer must restart the flow with corrected credentials.
     * Applies to the sign-in start step.
     */
    data class InvalidCredentials(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
        val errorCodes: List<Int>? = null,
    ) : NativeAuthV2SignInStartCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.InvalidCredentials(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.InvalidCredentials(correlationId=$correlationId)"
    }

    /**
     * The server rejected a password submitted from the password-required state. The developer can
     * retry through the state object used for the failed call.
     * Applies to the submit-password step.
     */
    data class IncorrectPassword(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
        val errorCodes: List<Int>? = null,
    ) : NativeAuthV2SubmitPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.IncorrectPassword(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.IncorrectPassword(correlationId=$correlationId)"
    }

    /**
     * The supplied password did not meet policy requirements.
     * The developer can retry through the state object used for the failed call.
     * Applies to the submit-new-password step.
     */
    data class PasswordNotAccepted(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
        val errorCodes: List<Int>? = null,
    ) : NativeAuthV2SubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.PasswordNotAccepted(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.PasswordNotAccepted(correlationId=$correlationId)"
    }

    /**
     * The server rejected the reset attempt after the new password was submitted.
     * Applies to the submit-new-password step.
     */
    data class PasswordResetFailed(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : NativeAuthV2SubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.PasswordResetFailed(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.PasswordResetFailed(correlationId=$correlationId)"
    }

    /**
     * The username provided is not registered in the tenant.
     * Applies to the start step.
     */
    data class UserNotFound(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val errorCodes: List<Int>? = null,
    ) : NativeAuthV2ResetPasswordStartCommandResult, NativeAuthV2SignInStartCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.UserNotFound(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.UserNotFound(correlationId=$correlationId)"
    }

    /**
     * The operation is not yet implemented server-side or is unsupported for this tenant.
     * May apply to any step.
     */
    data class NotImplemented(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : NativeAuthV2ResetPasswordStartCommandResult,
        NativeAuthV2SubmitCodeCommandResult,
        NativeAuthV2ResendCodeCommandResult,
        NativeAuthV2SubmitNewPasswordCommandResult,
        NativeAuthV2SignInStartCommandResult,
        NativeAuthV2SubmitPasswordCommandResult,
        NativeAuthV2SelectMFAMethodCommandResult,
        NativeAuthV2SubmitMFAChallengeCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.NotImplemented(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.NotImplemented(correlationId=$correlationId)"
    }
}
