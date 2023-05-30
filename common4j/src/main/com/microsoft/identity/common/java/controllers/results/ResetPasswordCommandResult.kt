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
package com.microsoft.identity.common.java.controllers.results

sealed interface ResetPasswordStartCommandResult
sealed interface ResetPasswordSubmitCodeCommandResult
sealed interface ResetPasswordResendCodeCommandResult
sealed interface ResetPasswordSubmitNewPasswordCommandResult

interface ResetPasswordCommandResult {
    data class CodeRequired(
        val passwordResetToken: String,
        val codeLength: Int,
        val challengeTargetLabel: String,
        val challengeChannel: String,
    ) : ResetPasswordStartCommandResult, ResetPasswordResendCodeCommandResult

    data class EmailNotVerified(val errorCode: String, val errorDescription: String) :
        ResetPasswordStartCommandResult

    data class PasswordNotSet(val errorCode: String, val errorDescription: String) :
        ResetPasswordStartCommandResult

    data class UserNotFound(val errorCode: String, val errorDescription: String) :
        ResetPasswordStartCommandResult

    data class PasswordRequired(val passwordSubmitToken: String) : ResetPasswordSubmitCodeCommandResult

    data class IncorrectCode(val errorCode: String, val errorDescription: String) :
        ResetPasswordSubmitCodeCommandResult

    data class PasswordNotAccepted(val errorCode: String, val errorDescription: String) :
        ResetPasswordSubmitNewPasswordCommandResult

    data class PasswordResetFailed(val errorCode: String, val errorDescription: String) :
        ResetPasswordSubmitNewPasswordCommandResult

    object Complete : ResetPasswordSubmitNewPasswordCommandResult
}