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
package com.microsoft.identity.common.java.nativeauth.providers.responses.jit

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiResult

/**
 * Represents the potential result types returned from the register/challenge endpoint,
 * including a case for unexpected errors received from the server.
 */
sealed interface JITChallengeApiResult: ApiResult {
    data class Redirect(
        override val correlationId: String,
        val redirectReason: String
    ) : JITChallengeApiResult {
        override fun toUnsanitizedString(): String {
            return "Redirect(correlationId=$correlationId, redirectReason=$redirectReason)"
        }

        override fun toString(): String = toUnsanitizedString()
    }

    data class OOBRequired(
        override val correlationId: String,
        val continuationToken: String,
        val challengeType: String,
        val bindingMethod: String?,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : JITChallengeApiResult {
        override fun toUnsanitizedString(): String {
            return "OOBRequired(correlationId=$correlationId " +
                    "challengeType=$challengeType, " +
                    "bindingMethod=$bindingMethod, " +
                    "challengeTargetLabel=$challengeTargetLabel, " +
                    "challengeChannel=$challengeChannel, " +
                    "codeLength=$codeLength)"
        }

        override fun toString(): String {
            return "OOBRequired(correlationId=$correlationId, " +
                    "challengeType=$challengeType, " +
                    "bindingMethod=$bindingMethod, " +
                    "codeLength=$codeLength)"
        }
    }

    data class Preverified(
        override val correlationId: String,
        val continuationToken: String
    ) : JITChallengeApiResult {
        override fun toUnsanitizedString(): String {
            return "Preverified(correlationId=$correlationId)"
        }

        override fun toString(): String {
            return toUnsanitizedString()
        }
    }

    data class InvalidVerificationContact(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), JITChallengeApiResult {
        override fun toUnsanitizedString() = "InvalidVerificationContact(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, subError=$subError)"

        override fun toString(): String = "InvalidVerificationContact(correlationId=$correlationId)"
    }

    data class UnknownError(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), JITChallengeApiResult {
        override fun toUnsanitizedString() = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"

        override fun toString(): String = "UnknownError(correlationId=$correlationId)"
    }
}