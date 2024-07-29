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
package com.microsoft.identity.common.java.nativeauth.providers.responses.resetpassword

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiResult

/**
 * Represents the potential result types returned from the Reset Password /submit endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface ResetPasswordSubmitApiResult: ApiResult {
    data class SubmitSuccess(
        val continuationToken: String,
        val pollInterval: Int,
        override val correlationId: String,
    ) : ResetPasswordSubmitApiResult {
        override fun toUnsanitizedString() = "SubmitSuccess(correlationId=$correlationId, " +
                "pollInterval=$pollInterval)"

        override fun toString(): String = "SubmitSuccess(correlationId=$correlationId)"
    }

    data class PasswordInvalid(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        val subError: String
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), ResetPasswordSubmitApiResult {
        override fun toUnsanitizedString() = "PasswordInvalid(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String = "PasswordInvalid(correlationId=$correlationId)"
    }

    data class ExpiredToken(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), ResetPasswordSubmitApiResult {
        override fun toUnsanitizedString() = "ExpiredToken(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "ExpiredToken(correlationId=$correlationId)"
    }

    data class UnknownError(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), ResetPasswordSubmitApiResult {
        override fun toUnsanitizedString() = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }
}
