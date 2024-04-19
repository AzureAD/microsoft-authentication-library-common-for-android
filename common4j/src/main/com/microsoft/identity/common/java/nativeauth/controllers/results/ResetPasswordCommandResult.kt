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


sealed interface ResetPasswordStartCommandResult: INativeAuthCommandResult
sealed interface ResetPasswordSubmitCodeCommandResult: INativeAuthCommandResult
sealed interface ResetPasswordResendCodeCommandResult: INativeAuthCommandResult
sealed interface ResetPasswordSubmitNewPasswordCommandResult: INativeAuthCommandResult

/**
 * Reflects the possible results from reset password commands.
 * Conforms to the INativeAuthCommandResult interface, and is mapped from the respective API result classes returned for each endpoint.
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordStartApiResult
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordChallengeApiResult
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordContinueApiResult
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordSubmitApiResult
 * @see com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword.ResetPasswordPollCompletionApiResult
 */
interface ResetPasswordCommandResult {
    data class CodeRequired(
        override val correlationId: String,
        val continuationToken: String,
        val codeLength: Int,
        val challengeTargetLabel: String,
        val challengeChannel: String,
    ) : ResetPasswordStartCommandResult, ResetPasswordResendCodeCommandResult {
        override fun toUnsanitizedString(): String = "CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"

        override fun toString(): String = "CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
    }

    data class EmailNotVerified(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : ResetPasswordStartCommandResult {
        override fun toUnsanitizedString(): String = "EmailNotVerified(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "EmailNotVerified(correlationId=$correlationId)"
    }

    data class PasswordNotSet(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : ResetPasswordStartCommandResult {
        override fun toUnsanitizedString(): String = "PasswordNotSet(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "PasswordNotSet(correlationId=$correlationId)"
    }

    data class UserNotFound(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : ResetPasswordStartCommandResult, ResetPasswordSubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String = "UserNotFound(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "UserNotFound(correlationId=$correlationId)"
    }

    data class PasswordRequired(
        override val correlationId: String,
        val continuationToken: String
    ) : ResetPasswordSubmitCodeCommandResult {
        override fun toUnsanitizedString(): String = "PasswordRequired(correlationId=$correlationId)"

        override fun toString(): String = toUnsanitizedString()
    }

    data class IncorrectCode(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String
    ) : ResetPasswordSubmitCodeCommandResult {
        override fun toUnsanitizedString(): String = "IncorrectCode(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String = "IncorrectCode(correlationId=$correlationId)"
    }

    data class PasswordNotAccepted(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
        val subError: String
    ) : ResetPasswordSubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String = "PasswordNotAccepted(correlationId=$correlationId, error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String = "PasswordNotAccepted(correlationId=$correlationId)"
    }

    data class PasswordResetFailed(
        override val correlationId: String,
        val error: String,
        val errorDescription: String,
    ) : ResetPasswordSubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String = "PasswordResetFailed(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "PasswordResetFailed(correlationId=$correlationId)"
    }

    data class Complete (
        override val correlationId: String,
        val continuationToken: String?,
        val expiresIn: Int?,
    ) : ResetPasswordSubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String = "Complete(correlationId=$correlationId, expiresIn=$expiresIn)"

        override fun toString(): String = toUnsanitizedString()
    }
}