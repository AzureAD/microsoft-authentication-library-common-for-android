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

import com.microsoft.identity.common.java.result.ILocalAuthenticationResult
import com.microsoft.identity.common.java.util.CommonUtils

sealed interface SignInStartCommandResult
sealed interface SignInWithSLTCommandResult
sealed interface SignInSubmitCodeCommandResult
sealed interface SignInResendCodeCommandResult
sealed interface SignInSubmitPasswordCommandResult

interface SignInCommandResult {
    data class Complete(val authenticationResult: ILocalAuthenticationResult) :
        SignInStartCommandResult, SignInWithSLTCommandResult, SignInSubmitCodeCommandResult,
        SignInSubmitPasswordCommandResult

    data class InvalidAuthenticationType(val error: String, val errorDescription: String, val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(), val errorCodes: List<Int>) :
        SignInStartCommandResult, SignInWithSLTCommandResult

    data class PasswordRequired(val credentialToken: String) :
        SignInStartCommandResult, SignInWithSLTCommandResult

    data class CodeRequired(
        val credentialToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) :
        SignInStartCommandResult, SignInWithSLTCommandResult, SignInResendCodeCommandResult, SignInSubmitPasswordCommandResult

    data class UserNotFound(val error: String, val errorDescription: String, val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(), val errorCodes: List<Int>) :
        SignInStartCommandResult

    data class InvalidCredentials(val error: String, val errorDescription: String, val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(), val errorCodes: List<Int>) :
        SignInStartCommandResult, SignInSubmitPasswordCommandResult

    data class IncorrectCode(val error: String, val errorDescription: String, val correlationId: String = CommonUtils.getCurrentThreadCorrelationId(), val errorCodes: List<Int>) :
        SignInSubmitCodeCommandResult
}
