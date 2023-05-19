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

sealed interface SsprStartCommandResult
data class SsprCodeRequired(
    val passwordResetToken: String,
    val codeLength: Int,
    val challengeTargetLabel: String,
    val challengeChannel: String,
): SsprStartCommandResult, SsprResendCodeCommandResult
data class EmailNotVerified(val errorCode: String, val errorDescription: String): SsprStartCommandResult
data class PasswordNotSet(val errorCode: String, val errorDescription: String): SsprStartCommandResult
data class SsprUserNotFound(val errorCode: String, val errorDescription: String): SsprStartCommandResult

// Add interfaces for oob commands if we are keeping them separate from other flows
sealed interface SsprSubmitCodeCommandResult
data class SsprPasswordRequired(val passwordSubmitToken: String): SsprSubmitCodeCommandResult
data class SsprIncorrectCode(val errorCode: String, val errorDescription: String):
    SsprSubmitCodeCommandResult

sealed interface SsprResendCodeCommandResult

sealed interface SsprSubmitNewPasswordCommandResult
data class PasswordNotAccepted(val errorCode: String, val errorDescription: String): SsprSubmitNewPasswordCommandResult
data class PasswordResetFailed(val errorCode: String, val errorDescription: String): SsprSubmitNewPasswordCommandResult
object SsprComplete: SsprSubmitNewPasswordCommandResult
