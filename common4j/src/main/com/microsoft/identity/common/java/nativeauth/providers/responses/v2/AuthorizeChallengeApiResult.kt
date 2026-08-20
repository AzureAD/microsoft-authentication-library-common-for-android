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
package com.microsoft.identity.common.java.nativeauth.providers.responses.v2

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiResult

/**
 * Represents the potential result types returned from the Native Auth V2 authorize-challenge
 * endpoint.
 *
 * No case ever includes [NativeAuthV2ContinuationState] or an authorization code value in either
 * [ApiResult.toString] or [ApiResult.toUnsanitizedString].
 */
sealed interface AuthorizeChallengeApiResult : ApiResult {

    /**
     * The flow requires further interaction. [continuationState] is the opaque mid-flow state
     * needed to build the next request.
     */
    data class ContinuationRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState
    ) : AuthorizeChallengeApiResult {
        override fun toUnsanitizedString(): String = "ContinuationRequired(correlationId=$correlationId)"
        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The server returned an authorization code directly, without requiring further
     * authorize-challenge interaction.
     *
     * [code] is never included in either string form.
     */
    data class AuthorizationCode(
        override val correlationId: String,
        val code: String
    ) : AuthorizeChallengeApiResult {
        override fun toUnsanitizedString(): String =
            "AuthorizationCode(correlationId=$correlationId, hasCode=${code.isNotEmpty()})"
        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The server requires the flow to fall back to a web-based (interactive browser) experience.
     */
    data class Redirect(
        override val correlationId: String,
        val redirectReason: String
    ) : AuthorizeChallengeApiResult {
        override fun toUnsanitizedString(): String =
            "Redirect(correlationId=$correlationId, redirectReason=$redirectReason)"
        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * An error was returned (or the response shape was otherwise unusable) that this SDK version
     * does not map to a more specific case.
     */
    data class UnknownError(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>? = null
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), AuthorizeChallengeApiResult {
        override fun toUnsanitizedString(): String = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"
        override fun toString(): String = "UnknownError(correlationId=$correlationId, errorCodes=$errorCodes)"
    }
}
