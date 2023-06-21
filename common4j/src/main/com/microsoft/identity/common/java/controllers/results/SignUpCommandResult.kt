// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.controllers.results

import com.microsoft.identity.common.java.util.CommonUtils

sealed interface SignUpSubmitCodeCommandResult
sealed interface SignUpSubmitUserAttributesCommandResult
sealed interface SignUpSubmitPasswordCommandResult
sealed interface SignUpResendCodeCommandResult
sealed interface SignUpStartCommandResult

interface SignUpCommandResult {
    data class UsernameAlreadyExists(
        val error: String,
        val errorDescription: String,
        val correlationId: String = CommonUtils.getCurrentThreadCorrelationId()
    ) : SignUpStartCommandResult, SignUpSubmitCodeCommandResult,
        SignUpSubmitUserAttributesCommandResult, SignUpSubmitPasswordCommandResult

    data class Complete (
        val signInSLT: String?,
        val expiresIn: Int?
    ) : SignUpStartCommandResult,
        SignUpSubmitCodeCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult

    data class CodeRequired(
        val signupToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : SignUpStartCommandResult,
        SignUpResendCodeCommandResult

    data class PasswordRequired(
        val signupToken: String
    ) : SignUpStartCommandResult,
        SignUpSubmitCodeCommandResult

    data class AttributesRequired(
        val signupToken: String,
        val error: String,
        val errorDescription: String,
        val requiredAttributes: List<Map<String, String>>,
        val correlationId: String = CommonUtils.getCurrentThreadCorrelationId()
    ) : SignUpStartCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult,
        SignUpSubmitCodeCommandResult

    data class InvalidPassword(
        val error: String,
        val errorDescription: String,
        val correlationId: String = CommonUtils.getCurrentThreadCorrelationId()
    ) : SignUpStartCommandResult, SignUpSubmitPasswordCommandResult

    data class InvalidCode(
        val error: String,
        val errorDescription: String,
        val correlationId: String = CommonUtils.getCurrentThreadCorrelationId()
    ) : SignUpSubmitCodeCommandResult

    data class InvalidAttributes(
        val error: String,
        val errorDescription: String,
        val invalidAttributes: List<Map<String, String>>,
        val correlationId: String = CommonUtils.getCurrentThreadCorrelationId()
    ) : SignUpStartCommandResult, SignUpSubmitUserAttributesCommandResult

    data class AuthNotSupported(
        val error: String,
        val errorDescription: String,
        val correlationId: String = CommonUtils.getCurrentThreadCorrelationId()
    ) : SignUpStartCommandResult
}