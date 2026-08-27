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

import com.microsoft.identity.common.java.logging.LogSession
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthConstants
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario

/**
 * Parses [NativeAuthV2HalApiResponse] wire models into the typed [AuthorizeChallengeApiResult] and
 * [NativeAuthV2InteractionApiResult] result families, including SSPR-scoped error mapping.
 *
 * This is business logic layered on top of T3's mechanical wire extraction
 * ([NativeAuthV2HalApiResponse.from]): no HAL/JSON parsing happens here, only interpretation of
 * the already-extracted fields (`state`, `action`, links, embedded methods, error) into an SDK
 * outcome.
 *
 * Both entry points are declared `internal`, not `public`, because the parser remains module-local
 * request/response plumbing, and [NativeAuthV2Operation] is still `internal`. This mirrors the
 * same constraint T3 already applied to [NativeAuthV2ContinuationState]'s factories.
 */
class NativeAuthV2ResponseParser {

    /**
     * Parses the response from the Native Auth V2 authorize-challenge endpoint.
     *
     * @param response The parsed HAL wire model for this response.
     * @param entryRelation The `_links` relation the next request must follow (flow-specific entry
     * point, e.g. `resetPassword`).
     * @param scenario The Native Auth V2 flow that issued this authorize-challenge call.
     * @param scopes The scopes requested for this flow, retained only for the later
     * authorization-code token exchange.
     */
    internal fun parseAuthorizeChallenge(
        response: NativeAuthV2HalApiResponse,
        entryRelation: NativeAuthV2LinkRelation,
        scenario: NativeAuthV2FlowScenario,
        scopes: List<String>,
        claimsRequestJson: String? = null
    ): AuthorizeChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = response.correlationId,
            methodName = "$TAG.parseAuthorizeChallenge"
        )

        if (response.isWebFallbackRequired) {
            return AuthorizeChallengeApiResult.Redirect(
                correlationId = response.serverError?.correlationId
                    ?.takeUnless { it.isBlank() } ?: response.correlationId,
                redirectReason = response.serverError?.code ?: response.state ?: WEB_FALLBACK_REDIRECT_REASON
            )
        }

        val continuationToken = response.continuationToken
        val hasValidContinuation = !continuationToken.isNullOrBlank() &&
            !response.links[entryRelation.value].isNullOrBlank()

        response.serverError?.takeUnless { hasValidContinuation }?.let { serverError ->
            return AuthorizeChallengeApiResult.UnknownError(
                correlationId = serverError.correlationId
                    ?.takeUnless { it.isBlank() } ?: response.correlationId,
                error = serverError.code ?: ApiErrorResult.INVALID_STATE,
                errorDescription = serverError.message.orEmpty(),
                errorCodes = extractAadstsCodes(serverError.message)
            )
        }

        response.authorizationCode?.takeUnless { it.isBlank() }?.let { code ->
            return AuthorizeChallengeApiResult.AuthorizationCode(
                correlationId = response.correlationId,
                code = code
            )
        }

        if (continuationToken != null) {
            if (continuationToken.isBlank()) {
                return AuthorizeChallengeApiResult.UnknownError(
                    correlationId = response.correlationId,
                    error = ApiErrorResult.INVALID_STATE,
                    errorDescription = "Native Auth V2 authorize-challenge response contains a " +
                            "blank continuation token."
                )
            }

            if (response.links[entryRelation.value].isNullOrBlank()) {
                return AuthorizeChallengeApiResult.UnknownError(
                    correlationId = response.correlationId,
                    error = ApiErrorResult.INVALID_STATE,
                    errorDescription = "Native Auth V2 authorize-challenge response is missing " +
                            "required link relation '${entryRelation.value}'."
                )
            }

            val continuationState = NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
                response = response,
                continuationToken = continuationToken,
                scopes = scopes,
                claimsRequestJson = claimsRequestJson,
                entryRelation = entryRelation,
                scenario = scenario
            )

            return AuthorizeChallengeApiResult.ContinuationRequired(
                correlationId = response.correlationId,
                continuationState = continuationState
            )
        }

        return AuthorizeChallengeApiResult.UnknownError(
            correlationId = response.correlationId,
            error = ApiErrorResult.INVALID_STATE,
            errorDescription = "Native Auth V2 authorize-challenge response contained neither an " +
                    "authorization code nor a continuation token."
        )
    }

    /**
     * Parses a V2 Native Auth mid-flow (post authorize-challenge) response.
     *
     * Evaluated in order: [NativeAuthV2HalApiResponse.isWebFallbackRequired] always wins and is
     * mapped to [NativeAuthV2InteractionApiResult.Redirect]; otherwise a server error is mapped via
     * the [operation]-scoped SSPR error table; a `state == "continue"` response requires a
     * continuation token and becomes [NativeAuthV2InteractionApiResult.ReadyToComplete]; any other
     * response requires a continuation token before its `action` is switched on to produce
     * [NativeAuthV2InteractionApiResult.ChallengeRequired], [NativeAuthV2InteractionApiResult.CodeRequired],
     * [NativeAuthV2InteractionApiResult.UpdateRequired], or [NativeAuthV2InteractionApiResult.PollInProgress];
     * an `action` this SDK version does not recognise becomes
     * [NativeAuthV2InteractionApiResult.UnsupportedAction] rather than being folded into a generic
     * error.
     *
     * @param response The parsed HAL wire model for this response.
     * @param previousState The continuation state that led to this response, used to build the
     * successor state.
     * @param operation The SDK-issued operation that produced [response], used only for
     * operation-scoped error mapping.
     */
    internal fun parseInteraction(
        response: NativeAuthV2HalApiResponse,
        previousState: NativeAuthV2ContinuationState,
        operation: NativeAuthV2Operation
    ): NativeAuthV2InteractionApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = response.correlationId,
            methodName = "$TAG.parseInteraction"
        )

        val errorCorrelationId = response.serverError?.correlationId
            ?.takeUnless { it.isBlank() } ?: response.correlationId

        if (response.isWebFallbackRequired) {
            return NativeAuthV2InteractionApiResult.Redirect(
                correlationId = errorCorrelationId,
                redirectReason = response.serverError?.code ?: response.state ?: WEB_FALLBACK_REDIRECT_REASON
            )
        }

        response.serverError?.let { serverError ->
            return mapInteractionError(
                correlationId = errorCorrelationId,
                serverError = serverError,
                operation = operation
            )
        }

        if (response.state == STATE_CONTINUE) {
            val successor = NativeAuthV2ContinuationState.next(previousState, response)
                ?: return missingContinuationTokenError(response.correlationId)
            return NativeAuthV2InteractionApiResult.ReadyToComplete(
                correlationId = response.correlationId,
                continuationState = successor
            )
        }

        if (response.continuationToken == null) {
            return missingContinuationTokenError(response.correlationId)
        }

        return when (val action = response.action) {
            null -> missingActionError(response.correlationId)
            NativeAuthV2HalAction.CHALLENGE -> parseChallenge(response, previousState)
            NativeAuthV2HalAction.VERIFY -> parseVerify(response, previousState)
            NativeAuthV2HalAction.UPDATE -> parseUpdate(response, previousState)
            NativeAuthV2HalAction.POLL -> parsePoll(response, previousState)
            else -> unsupportedAction(response.correlationId, action.value)
        }
    }

    private fun parseChallenge(
        response: NativeAuthV2HalApiResponse,
        previousState: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        val selectedMethod = selectEmailMethod(response)
        if (selectedMethod == null && response.methods.isNotEmpty()) {
            return unsupportedChallengeMethodError(response)
        }

        selectedMethod?.links?.get(NativeAuthV2LinkRelation.CHALLENGE.value)
            ?: response.links[NativeAuthV2LinkRelation.CHALLENGE.value]
            ?: return missingLinkError(response.correlationId, NativeAuthV2LinkRelation.CHALLENGE)

        val successor = NativeAuthV2ContinuationState.next(previousState, response, selectedMethod)
            ?: return missingContinuationTokenError(response.correlationId)

        return NativeAuthV2InteractionApiResult.ChallengeRequired(
            correlationId = response.correlationId,
            continuationState = successor,
            hint = selectedMethod?.hint ?: response.challengeTargetLabel
        )
    }

    private fun parseVerify(
        response: NativeAuthV2HalApiResponse,
        previousState: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        val selectedMethod = selectEmailMethod(response)
        if (selectedMethod == null && response.methods.isNotEmpty()) {
            return unsupportedChallengeMethodError(response)
        }

        if (selectedMethod?.links?.get(NativeAuthV2LinkRelation.VERIFY.value) == null &&
            response.links[NativeAuthV2LinkRelation.VERIFY.value] == null
        ) {
            return missingLinkError(response.correlationId, NativeAuthV2LinkRelation.VERIFY)
        }

        val codeLength = response.codeLength
            ?: return missingFieldError(response.correlationId, CODE_LENGTH_FIELD)
        if (codeLength <= 0) {
            return invalidFieldError(response.correlationId, CODE_LENGTH_FIELD)
        }
        val challengeTargetLabel = selectedMethod?.hint ?: response.challengeTargetLabel
            ?: return missingFieldError(response.correlationId, CHALLENGE_TARGET_LABEL_FIELD)
        val challengeChannel = selectedMethod?.type ?: response.challengeChannel
            ?: return missingFieldError(response.correlationId, CHALLENGE_CHANNEL_FIELD)

        // V2 SSPR is a code-first, email-only flow. A non-email channel cannot be verified here, so
        // surface it as an error rather than prompting the user on an unsupported channel.
        if (!challengeChannel.isEmailChannel()) {
            return unsupportedChallengeMethodError(response)
        }

        val successor = NativeAuthV2ContinuationState.next(previousState, response, selectedMethod)
            ?: return missingContinuationTokenError(response.correlationId)

        return NativeAuthV2InteractionApiResult.CodeRequired(
            correlationId = response.correlationId,
            continuationState = successor,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = challengeChannel,
            codeLength = codeLength
        )
    }

    /**
     * Selects the first email-capable authentication method offered by [response], or `null` when
     * the response embeds no email method. V2 SSPR only supports email one-time codes, so no
     * fallback to another channel is attempted.
     */
    private fun selectEmailMethod(
        response: NativeAuthV2HalApiResponse
    ): NativeAuthV2HalApiResponse.EmbeddedAuthMethod? =
        response.methods.firstOrNull { it.type.isEmailChannel() }

    private fun String?.isEmailChannel(): Boolean =
        this?.equals(NativeAuthConstants.ChallengeChannel.EMAIL, ignoreCase = true) == true

    private fun unsupportedChallengeMethodError(
        response: NativeAuthV2HalApiResponse
    ): NativeAuthV2InteractionApiResult.UnknownError {
        Logger.warn(
            TAG,
            "Native Auth V2 response did not offer a supported email authentication method."
        )
        return NativeAuthV2InteractionApiResult.UnknownError(
            correlationId = response.correlationId,
            error = ApiErrorResult.INVALID_STATE,
            errorDescription = "Native Auth V2 response did not offer a supported email " +
                    "authentication method. Only email one-time codes are supported."
        )
    }

    private fun parseUpdate(
        response: NativeAuthV2HalApiResponse,
        previousState: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        if (response.links[NativeAuthV2LinkRelation.UPDATE.value] == null) {
            return missingLinkError(response.correlationId, NativeAuthV2LinkRelation.UPDATE)
        }

        val successor = NativeAuthV2ContinuationState.next(previousState, response)
            ?: return missingContinuationTokenError(response.correlationId)

        return NativeAuthV2InteractionApiResult.UpdateRequired(
            correlationId = response.correlationId,
            continuationState = successor
        )
    }

    private fun parsePoll(
        response: NativeAuthV2HalApiResponse,
        previousState: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        if (response.links[NativeAuthV2LinkRelation.POLL.value] == null) {
            return missingLinkError(response.correlationId, NativeAuthV2LinkRelation.POLL)
        }

        val successor = NativeAuthV2ContinuationState.next(previousState, response)
            ?: return missingContinuationTokenError(response.correlationId)

        return NativeAuthV2InteractionApiResult.PollInProgress(
            correlationId = response.correlationId,
            continuationState = successor,
            retryAfterMillis = response.pollIntervalMillis?.takeIf { it > 0 }?.toLong()
        )
    }

    private fun unsupportedAction(
        correlationId: String,
        rawAction: String
    ): NativeAuthV2InteractionApiResult {
        Logger.warn(TAG, "Native Auth V2 response requested an unsupported action.")
        return NativeAuthV2InteractionApiResult.UnsupportedAction(
            correlationId = correlationId,
            rawAction = rawAction,
            error = ApiErrorResult.INVALID_STATE,
            errorDescription = "Native Auth V2 response requested unsupported action '$rawAction'."
        )
    }

    /**
     * Maps a HAL server error onto [NativeAuthV2InteractionApiResult], scoped to SSPR operations.
     * See the T4 design brief's error-mapping table for the exact condition ordering reproduced
     * here; conditions are evaluated top to bottom and the first match wins.
     */
    private fun mapInteractionError(
        correlationId: String,
        serverError: NativeAuthV2HalApiResponse.HalServerError,
        operation: NativeAuthV2Operation
    ): NativeAuthV2InteractionApiResult {
        val code = serverError.code
        val innerErrorCode = serverError.innerErrorCode
        val message = serverError.message
        val errorCodes = extractAadstsCodes(message)

        return when {
            operation == NativeAuthV2Operation.VERIFY &&
                    code in ERROR_INVALID_GRANT &&
                    innerErrorCode in INNER_ERROR_INVALID_ONE_TIME_CODE ->
                // The user supplied the wrong one-time code; the app can prompt for a new one.
                NativeAuthV2InteractionApiResult.InvalidCode(
                    correlationId = correlationId,
                    error = code.orEmpty(),
                    errorDescription = message.orEmpty(),
                    subError = innerErrorCode.orEmpty(),
                    errorCodes = errorCodes
                )

            innerErrorCode in INNER_ERROR_INVALID_CONTINUATION_TOKEN ->
                // SDK-managed continuation-token state the app cannot act on.
                unknownInteractionError(correlationId, code, message, errorCodes)

            operation == NativeAuthV2Operation.UPDATE_PASSWORD &&
                    innerErrorCode in INNER_ERROR_INVALID_PASSWORD ->
                NativeAuthV2InteractionApiResult.InvalidPassword(
                    correlationId = correlationId,
                    error = code.orEmpty(),
                    errorDescription = message.orEmpty(),
                    subError = innerErrorCode.orEmpty(),
                    errorCodes = errorCodes
                )

            operation == NativeAuthV2Operation.RESET_PASSWORD_START &&
                    message?.contains(AADSTS_USER_NOT_FOUND) == true ->
                NativeAuthV2InteractionApiResult.UserNotFound(
                    correlationId = correlationId,
                    error = code.orEmpty(),
                    errorDescription = message.orEmpty(),
                    errorCodes = errorCodes
                )

            innerErrorCode in INNER_ERROR_INVALID_USERNAME_OR_PASSWORD ||
                    message?.contains(AADSTS_INVALID_USERNAME_OR_PASSWORD) == true ->
                // Sign-in concern for SSPR; revisit once V2 sign-in lands.
                unknownInteractionError(correlationId, code, message, errorCodes)

            else -> unknownInteractionError(correlationId, code, message, errorCodes)
        }
    }

    private fun unknownInteractionError(
        correlationId: String,
        code: String?,
        message: String?,
        errorCodes: List<Int>?
    ): NativeAuthV2InteractionApiResult.UnknownError = NativeAuthV2InteractionApiResult.UnknownError(
        correlationId = correlationId,
        error = code ?: ApiErrorResult.INVALID_STATE,
        errorDescription = message.orEmpty(),
        errorCodes = errorCodes
    )

    private fun missingLinkError(
        correlationId: String,
        relation: NativeAuthV2LinkRelation
    ): NativeAuthV2InteractionApiResult.UnknownError = NativeAuthV2InteractionApiResult.UnknownError(
        correlationId = correlationId,
        error = ApiErrorResult.INVALID_STATE,
        errorDescription = "Native Auth V2 response is missing required link relation '${relation.value}'."
    )

    private fun missingFieldError(
        correlationId: String,
        fieldName: String
    ): NativeAuthV2InteractionApiResult.UnknownError = NativeAuthV2InteractionApiResult.UnknownError(
        correlationId = correlationId,
        error = ApiErrorResult.INVALID_STATE,
        errorDescription = "Native Auth V2 'verify' response is missing required field '$fieldName'."
    )

    private fun invalidFieldError(
        correlationId: String,
        fieldName: String
    ): NativeAuthV2InteractionApiResult.UnknownError = NativeAuthV2InteractionApiResult.UnknownError(
        correlationId = correlationId,
        error = ApiErrorResult.INVALID_STATE,
        errorDescription = "Native Auth V2 'verify' response contains an invalid value for field '$fieldName'."
    )

    private fun missingActionError(
        correlationId: String
    ): NativeAuthV2InteractionApiResult.UnknownError = NativeAuthV2InteractionApiResult.UnknownError(
        correlationId = correlationId,
        error = ApiErrorResult.INVALID_STATE,
        errorDescription = "Native Auth V2 response is missing required field 'action'."
    )

    private fun missingContinuationTokenError(
        correlationId: String
    ): NativeAuthV2InteractionApiResult.UnknownError = NativeAuthV2InteractionApiResult.UnknownError(
        correlationId = correlationId,
        error = ApiErrorResult.INVALID_STATE,
        errorDescription = "Native Auth V2 response is missing a continuation token."
    )

    /**
     * Extracts every `AADSTSnnnnn` numeric code present in [message], matching V1's convention of
     * surfacing service error codes via [ApiErrorResult.errorCodes]. Returns `null` (rather than
     * an empty list) when none are present, matching [ApiErrorResult.errorCodes]'s own default.
     */
    private fun extractAadstsCodes(message: String?): List<Int>? {
        if (message.isNullOrBlank()) {
            return null
        }
        val codes = AADSTS_CODE_REGEX.findAll(message).mapNotNull { it.groupValues[1].toIntOrNull() }.toList()
        return codes.ifEmpty { null }
    }

    private companion object {
        private val TAG: String = NativeAuthV2ResponseParser::class.java.simpleName

        private val AADSTS_CODE_REGEX = Regex("AADSTS(\\d+)")

        private const val STATE_CONTINUE = "continue"
        private const val WEB_FALLBACK_REDIRECT_REASON = "web_fallback_required"

        private const val CODE_LENGTH_FIELD = "codeLength"
        private const val CHALLENGE_TARGET_LABEL_FIELD = "challengeTargetLabel"
        private const val CHALLENGE_CHANNEL_FIELD = "challengeChannel"

        private val ERROR_INVALID_GRANT = setOf("invalidGrant", "invalid_grant")
        private val INNER_ERROR_INVALID_CONTINUATION_TOKEN =
            setOf("invalidContinuationToken", "invalid_continuation_token")

        /**
         * Sub-error codes that indicate the user supplied an incorrect one-time code.
         * `invalidOneTimeCode` is the Native Auth V2 wire value (AADSTS50184) and is the only
         * spelling MSAL iOS/macOS accepts. `invalid_oob_value` is the legacy V1 spelling, retained
         * here for back-compat so a V1-shaped response is still classified as a recoverable code
         * error rather than a terminal one.
         */
        private val INNER_ERROR_INVALID_ONE_TIME_CODE =
            setOf("invalidOneTimeCode", "invalid_oob_value")
        private val INNER_ERROR_INVALID_PASSWORD = setOf(
            "passwordTooWeak",
            "password_too_weak",
            "passwordTooShort",
            "password_too_short",
            "passwordTooLong",
            "password_too_long",
            "passwordIsInvalid",
            "password_is_invalid",
            "passwordRecentlyUsed",
            "password_recently_used",
            "passwordBanned",
            "password_banned"
        )
        private val INNER_ERROR_INVALID_USERNAME_OR_PASSWORD =
            setOf("invalidUserNameOrPassword", "invalid_username_or_password")
        private const val AADSTS_USER_NOT_FOUND = "AADSTS50034"
        private const val AADSTS_INVALID_USERNAME_OR_PASSWORD = "AADSTS50126"
    }
}
