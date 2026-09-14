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
 * Represents the potential result types returned by [NativeAuthV2ResponseParser.parseInteraction]
 * for a V2 Native Auth mid-flow (post authorize-challenge) response, covering the SSPR
 * (reset-password) and sign-in operations.
 *
 * No error case includes [NativeAuthV2ContinuationState], a continuation token, an href, or an
 * authorization code. The caller retains the state used for an operation when it needs to retry.
 * No case includes sensitive continuation data in either [ApiResult.toString] or
 * [ApiResult.toUnsanitizedString].
 */
sealed interface NativeAuthV2InteractionApiResult : ApiResult {

    /**
     * The flow requires the user to complete a challenge (for example, verifying a contact
     * method) via [continuationState]. [hint] is the server-supplied, possibly PII-bearing,
     * target label for that challenge (e.g. a partially-masked email or phone number) and is
     * therefore omitted from [toString], though present in [toUnsanitizedString].
     *
     * [methods] carries every valid method the server offered, in server order, for flows that
     * select a method explicitly (V2 sign-in). It is empty for flows whose parser already selected
     * the single supported method implicitly (V2 SSPR).
     */
    data class ChallengeRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val hint: String?,
        val methods: List<NativeAuthV2AuthMethod> = emptyList()
    ) : NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "ChallengeRequired(correlationId=$correlationId, " +
                "hint=$hint, methods=${methods.map { it.toUnsanitizedString() }})"
        override fun toString(): String = "ChallengeRequired(correlationId=$correlationId, " +
                "methods=${methods.map { it.toString() }})"
    }

    /**
     * The first factor has been satisfied and the server requires a second factor. [methods] are
     * the multi-factor methods the server offered, in server order; the caller must select one
     * explicitly, and no challenge is sent until it does.
     */
    data class MFARequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val methods: List<NativeAuthV2AuthMethod>
    ) : NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "MFARequired(correlationId=$correlationId, " +
                "methods=${methods.map { it.toUnsanitizedString() }})"
        override fun toString(): String = "MFARequired(correlationId=$correlationId, " +
                "methods=${methods.map { it.toString() }})"
    }

    /**
     * The server challenged the selected password method and is ready to verify a password
     * submitted via [continuationState].
     */
    data class PasswordRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState
    ) : NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "PasswordRequired(correlationId=$correlationId)"
        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The flow requires the user to submit a verification code via [continuationState].
     * [challengeTargetLabel], [challengeChannel], and [codeLength] are all non-null because
     * PR #2547's public result exposes them as non-null values; a response missing any of them is
     * a protocol error ([UnknownError]), not a `0`/empty-string substitution.
     */
    data class CodeRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val challengeTargetLabel: String,
        val challengeChannel: String,
        val codeLength: Int
    ) : NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "CodeRequired(correlationId=$correlationId, " +
                "challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel, " +
                "codeLength=$codeLength)"
        override fun toString(): String = "CodeRequired(correlationId=$correlationId, " +
                "challengeChannel=$challengeChannel, codeLength=$codeLength)"
    }

    /**
     * The flow requires the user to submit an updated value (for example, a new password) via
     * [continuationState].
     */
    data class UpdateRequired(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState
    ) : NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "UpdateRequired(correlationId=$correlationId)"
        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The server is still processing a prior submission; the caller should poll again using
     * [continuationState]. [retryAfterMillis] is the server-suggested delay before the next poll,
     * in milliseconds, or `null` when the server did not suggest one.
     */
    data class PollInProgress(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState,
        val retryAfterMillis: Long?
    ) : NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String =
            "PollInProgress(correlationId=$correlationId, retryAfterMillis=$retryAfterMillis)"
        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The flow has reached a terminal `continue` state and is ready to complete via
     * [continuationState].
     */
    data class ReadyToComplete(
        override val correlationId: String,
        val continuationState: NativeAuthV2ContinuationState
    ) : NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "ReadyToComplete(correlationId=$correlationId)"
        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * The server requires the flow to fall back to a web-based (interactive browser) experience.
     */
    data class Redirect(
        override val correlationId: String,
        val redirectReason: String
    ) : NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String =
            "Redirect(correlationId=$correlationId, redirectReason=$redirectReason)"
        override fun toString(): String = toUnsanitizedString()
    }

    /**
     * No account matches the identifier supplied to reset-password /start.
     */
    data class UserNotFound(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val errorCodes: List<Int>? = null
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "UserNotFound(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"
        override fun toString(): String = "UserNotFound(correlationId=$correlationId, errorCodes=$errorCodes)"
    }

    /**
     * The submitted verification code was invalid.
     */
    data class InvalidCode(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val subError: String,
        override val errorCodes: List<Int>? = null
    ) : ApiErrorResult(
        error = error,
        subError = subError,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "InvalidCode(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"
        override fun toString(): String = "InvalidCode(correlationId=$correlationId, errorCodes=$errorCodes)"
    }

    /**
     * The submitted password was invalid (for example, too weak).
     */
    data class InvalidPassword(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val subError: String,
        override val errorCodes: List<Int>? = null
    ) : ApiErrorResult(
        error = error,
        subError = subError,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "InvalidPassword(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"
        override fun toString(): String = "InvalidPassword(correlationId=$correlationId, errorCodes=$errorCodes)"
    }

    /**
     * The submitted username/password combination was rejected by the service.
     */
    data class InvalidCredentials(
        override val correlationId: String,
        override val error: String,
        override val errorDescription: String,
        override val subError: String,
        override val errorCodes: List<Int>? = null
    ) : ApiErrorResult(
        error = error,
        subError = subError,
        errorDescription = errorDescription,
        errorCodes = errorCodes,
        correlationId = correlationId
    ), NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "InvalidCredentials(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"
        override fun toString(): String = "InvalidCredentials(correlationId=$correlationId, " +
                "errorCodes=$errorCodes)"
    }

    /**
     * The server requested an `action` this SDK version does not recognize. [rawAction] preserves
     * exactly what the server sent, for diagnosis, unlike an unrecognised `_links` relation, which
     * is silently ignored rather than surfaced as an error.
     */
    data class UnsupportedAction(
        override val correlationId: String,
        val rawAction: String,
        override val error: String,
        override val errorDescription: String
    ) : ApiErrorResult(
        error = error,
        errorDescription = errorDescription,
        correlationId = correlationId
    ), NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "UnsupportedAction(correlationId=$correlationId, " +
                "rawAction=$rawAction, error=$error, errorDescription=$errorDescription)"
        override fun toString(): String = "UnsupportedAction(correlationId=$correlationId)"
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
    ), NativeAuthV2InteractionApiResult {
        override fun toUnsanitizedString(): String = "UnknownError(correlationId=$correlationId, " +
                "error=$error, errorDescription=$errorDescription, errorCodes=$errorCodes)"
        override fun toString(): String = "UnknownError(correlationId=$correlationId, errorCodes=$errorCodes)"
    }
}
