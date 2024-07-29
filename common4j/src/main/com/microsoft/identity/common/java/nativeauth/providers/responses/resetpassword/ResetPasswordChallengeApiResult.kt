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
 * Represents the potential result types returned from the Reset Password /challenge endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface ResetPasswordChallengeApiResult: ApiResult {
    data class Redirect(
        override val correlationId: String
    ) : ResetPasswordChallengeApiResult {
        override fun toUnsanitizedString(): String {
            return "Redirect(correlationId=$correlationId)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    data class CodeRequired(
        override val correlationId: String,
        val continuationToken: String,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : ResetPasswordChallengeApiResult {
        override fun toUnsanitizedString() = "CodeRequired(correlationId=$correlationId, " +
                "challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel, " +
                "codeLength=$codeLength)"

        override fun toString(): String = "CodeRequired(correlationId=$correlationId, " +
                "challengeChannel=$challengeChannel, " +
                "codeLength=$codeLength)"
    }

    data class UnsupportedChallengeType(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), ResetPasswordChallengeApiResult {
        override fun toUnsanitizedString() = "UnsupportedChallengeType(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "UnsupportedChallengeType(correlationId=$correlationId)"
    }

    data class ExpiredToken(
        override val error: String,
        override val errorDescription: String,
        override val correlationId: String
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), ResetPasswordChallengeApiResult {
        override fun toUnsanitizedString() = "ExpiredToken(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "ExpiredToken(correlationId=$correlationId)"
    }

    data class UnknownError(
        override val error: String,
        override val errorDescription: String,
        override val correlationId: String
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), ResetPasswordChallengeApiResult {
        override fun toUnsanitizedString() = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription)"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }
}
