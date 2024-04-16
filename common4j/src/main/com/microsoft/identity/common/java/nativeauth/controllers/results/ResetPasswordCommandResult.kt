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

import com.microsoft.identity.common.java.nativeauth.util.Logging


sealed interface ResetPasswordStartCommandResult: INativeAuthCommandResult, Logging
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
        override fun toSafeString(mayContainPii: Boolean): String {
            return if (mayContainPii) {
                "CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel)"
            } else {
                "CodeRequired(correlationId=$correlationId, codeLength=$codeLength, challengeChannel=$challengeChannel)"
            }
        }

        override fun containsPii(): Boolean = true

        override fun toString(): String = toSafeString(false)
    }

    data class EmailNotVerified(
        val error: String,
        val errorDescription: String,
        override val correlationId: String
    ) : ResetPasswordStartCommandResult {
        override fun toSafeString(mayContainPii: Boolean): String {
            return if (mayContainPii) {
                "EmailNotVerified(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"
            } else {
                "EmailNotVerified(correlationId=$correlationId)"
            }
        }

        override fun containsPii(): Boolean = true

        override fun toString(): String = toSafeString(false)
    }

    data class PasswordNotSet(
        val error: String,
        val errorDescription: String,
        override val correlationId: String
    ) : ResetPasswordStartCommandResult {
        override fun toSafeString(mayContainPii: Boolean): String {
            return if (mayContainPii) {
                "PasswordNotSet(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"
            } else {
                "PasswordNotSet(correlationId=$correlationId)"
            }
        }

        override fun containsPii(): Boolean = true

        override fun toString(): String = toSafeString(false)
    }

    data class UserNotFound(
        val error: String,
        val errorDescription: String,
        override val correlationId: String
    ) : ResetPasswordStartCommandResult, ResetPasswordSubmitNewPasswordCommandResult {
        override fun toSafeString(mayContainPii: Boolean): String {
            return if (mayContainPii) {
                "UserNotFound(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"
            } else {
                "UserNotFound(correlationId=$correlationId)"
            }
        }

        override fun containsPii(): Boolean = true

        override fun toString(): String = toSafeString(false)
    }

    data class PasswordRequired(
        override val correlationId: String,
        val continuationToken: String
    ) : ResetPasswordSubmitCodeCommandResult

    data class IncorrectCode(
        val error: String,
        val errorDescription: String,
        override val correlationId: String,
        val subError: String
    ) :
        ResetPasswordSubmitCodeCommandResult

    data class PasswordNotAccepted(
        val error: String,
        val errorDescription: String,
        override val correlationId: String,
        val subError: String
    ) :
        ResetPasswordSubmitNewPasswordCommandResult

    data class PasswordResetFailed(
        val error: String,
        val errorDescription: String,
        override val correlationId: String
    ) :
        ResetPasswordSubmitNewPasswordCommandResult

    data class Complete (
        val continuationToken: String?,
        val expiresIn: Int?,
        override val correlationId: String
    ) : ResetPasswordSubmitNewPasswordCommandResult
}