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

import com.microsoft.identity.common.java.logging.DiagnosticContext
import com.microsoft.identity.common.java.nativeauth.util.ILoggable

/**
 * INativeAuthCommandResult interface defines the base class for errors used in Native Auth.
 */
interface INativeAuthCommandResult : ILoggable {
    val correlationId: String
    data class Redirect(
        override val correlationId: String,
    ) : Error(error = BROWSER_REQUIRED_ERROR, errorDescription = BROWSER_REQUIRED_ERROR_DESCRIPTION, correlationId = correlationId),
        SignInStartCommandResult, SignInWithContinuationTokenCommandResult, SignInSubmitCodeCommandResult, SignInResendCodeCommandResult,
        SignInSubmitPasswordCommandResult, SignUpStartCommandResult, SignUpSubmitCodeCommandResult,
        SignUpResendCodeCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult,
        ResetPasswordStartCommandResult, ResetPasswordSubmitCodeCommandResult,
        ResetPasswordResendCodeCommandResult {
            companion object {
                private const val BROWSER_REQUIRED_ERROR: String = "browser_required"
                private const val BROWSER_REQUIRED_ERROR_DESCRIPTION: String = "The client's authentication capabilities are insufficient. Please redirect to the browser to complete authentication"
            }


        override fun toUnsanitizedString(): String = "Redirect(correlationId=$correlationId, error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "Redirect(correlationId=$correlationId"
    }

    /**
     * UnknownError is base class to represent various kinds of errors in NativeAuth.
     */
    data class UnknownError(
        override val error: String?,
        override val errorDescription: String?,
        override val details: List<Map<String, String>>? = null,
        override val correlationId: String,
        override val errorCodes: List<Int>? = null,
        val exception: Exception? = null
    ): Error(error, errorDescription, details, correlationId, errorCodes), INativeAuthCommandResult,
        SignInStartCommandResult, SignInWithContinuationTokenCommandResult, SignInSubmitCodeCommandResult,
        SignInResendCodeCommandResult, SignInSubmitPasswordCommandResult,
        SignUpStartCommandResult, SignUpSubmitUserAttributesCommandResult,
        SignUpSubmitCodeCommandResult, SignUpResendCodeCommandResult,
        SignUpSubmitPasswordCommandResult,
        ResetPasswordStartCommandResult, ResetPasswordSubmitCodeCommandResult,
        ResetPasswordResendCodeCommandResult, ResetPasswordSubmitNewPasswordCommandResult {
        override fun toUnsanitizedString(): String = "UnknownError(correlationId=$correlationId, error=$error, errorDescription=$errorDescription), details=$details, errorCodes=$errorCodes"

        override fun toString(): String =  "UnknownError(correlationId=$correlationId)"
        }

    abstract class Error(
        open val error: String?,
        open val errorDescription: String?,
        open val details: List<Map<String, String>>? = null,
        open val correlationId: String,
        open val errorCodes: List<Int>? = null
    ) : ILoggable

    data class InvalidUsername(
        override val error: String?,
        override val errorDescription: String?,
        override val details: List<Map<String, String>>? = null,
        override val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId,
        override val errorCodes: List<Int>? = null,
        val exception: Exception? = null
    ) : Error(error, errorDescription, details, correlationId, errorCodes),
        INativeAuthCommandResult, SignInStartCommandResult, SignUpStartCommandResult, SignUpSubmitPasswordCommandResult, ResetPasswordStartCommandResult {
        override fun toUnsanitizedString(): String = "UnknownError(correlationId=$correlationId, error=$error, errorDescription=$errorDescription), details=$details, errorCodes=$errorCodes"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }
}
