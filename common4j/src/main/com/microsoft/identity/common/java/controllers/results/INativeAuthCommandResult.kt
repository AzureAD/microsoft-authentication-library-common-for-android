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

import com.microsoft.identity.common.java.logging.DiagnosticContext

/**
 * INativeAuthCommandResult interface defines the base class for errors used in Native Auth.
 */
interface INativeAuthCommandResult {
    data class Redirect(val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId) :
        SignInStartCommandResult, SignInWithSLTCommandResult, SignInSubmitCodeCommandResult,
        SignInResendCodeCommandResult, SignInSubmitPasswordCommandResult,
        SignUpStartCommandResult, SignUpSubmitCodeCommandResult,
        SignUpResendCodeCommandResult, SignUpSubmitPasswordCommandResult,
        SignUpSubmitUserAttributesCommandResult,
        ResetPasswordStartCommandResult, ResetPasswordSubmitCodeCommandResult,
        ResetPasswordResendCodeCommandResult

    /**
     * UnknownError is base class to represent various kinds of errors in NativeAuth.
     */
    data class UnknownError(
        override val error: String?,
        override val errorDescription: String?,
        override val details: List<Map<String, String>>? = null,
        //TODO: This initialisation will be removed as part of PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2710164
        override val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId,
        override val errorCodes: List<Int>? = null,
        val exception: Exception? = null
    ): Error(error, errorDescription, details, correlationId, errorCodes), INativeAuthCommandResult,
        SignInStartCommandResult, SignInWithSLTCommandResult, SignInSubmitCodeCommandResult,
        SignInResendCodeCommandResult, SignInSubmitPasswordCommandResult,
        SignUpStartCommandResult, SignUpSubmitUserAttributesCommandResult,
        SignUpSubmitCodeCommandResult, SignUpResendCodeCommandResult,
        SignUpSubmitPasswordCommandResult,
        ResetPasswordStartCommandResult, ResetPasswordSubmitCodeCommandResult,
        ResetPasswordResendCodeCommandResult, ResetPasswordSubmitNewPasswordCommandResult

    open class Error(
        open val error: String?,
        open val errorDescription: String?,
        open val details: List<Map<String, String>>? = null,
        //TODO: This initialisation will be removed as part of PBI https://identitydivision.visualstudio.com/Engineering/_workitems/edit/2710164
        open val correlationId: String = DiagnosticContext.INSTANCE.threadCorrelationId,
        open val errorCodes: List<Int>? = null
    )
}
