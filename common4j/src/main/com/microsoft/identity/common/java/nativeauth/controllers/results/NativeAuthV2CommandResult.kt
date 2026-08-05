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

import com.microsoft.identity.common.java.result.ILocalAuthenticationResult

// Per-operation sealed marker interfaces for exhaustive when() dispatch.
sealed interface NativeAuthV2ResetPasswordStartCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SubmitCodeCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2ResendCodeCommandResult : INativeAuthCommandResult
sealed interface NativeAuthV2SubmitNewPasswordCommandResult : INativeAuthCommandResult

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
     * Applies to start and resend-code steps.
     */
    data class CodeRequired(
        override val correlationId: String,
        val continuationToken: String,
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
     * Applies to the submit-code step.
     */
    data class NewPasswordRequired(
        override val correlationId: String,
        val continuationToken: String,
    ) : NativeAuthV2SubmitCodeCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.NewPasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The password reset flow has completed successfully.
     * [authenticationResult] may be non-null when the server returns a sign-in continuation token
     * that was redeemed inline; it is null when no automatic sign-in was performed.
     * Applies to the submit-new-password step.
     */
    data class Complete(
        override val correlationId: String,
        val authenticationResult: ILocalAuthenticationResult?,
        val continuationToken: String?,
        val expiresIn: Int?,
    ) : NativeAuthV2SubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.Complete(correlationId=$correlationId, expiresIn=$expiresIn)"

        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The server rejected the submitted OTP.
     * Applies to the submit-code step.
     */
    data class IncorrectCode(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
    ) : NativeAuthV2SubmitCodeCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.IncorrectCode(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.IncorrectCode(correlationId=$correlationId)"
    }

    /**
     * The supplied password did not meet policy requirements.
     * Applies to the submit-new-password step.
     */
    data class PasswordNotAccepted(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String,
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
    ) : NativeAuthV2ResetPasswordStartCommandResult {
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
        NativeAuthV2SubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String =
            "NativeAuthV2CommandResult.NotImplemented(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String =
            "NativeAuthV2CommandResult.NotImplemented(correlationId=$correlationId)"
    }
}
