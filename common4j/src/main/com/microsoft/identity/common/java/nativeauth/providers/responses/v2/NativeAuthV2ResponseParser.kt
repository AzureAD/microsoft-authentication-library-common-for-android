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
import java.util.Locale

/**
 * Parses [NativeAuthV2HalApiResponse] wire models into the typed [AuthorizeChallengeApiResult] and
 * [NativeAuthV2InteractionApiResult] result families.
 *
 * This is business logic layered on top of T3's mechanical wire extraction
 * ([NativeAuthV2HalApiResponse.from]): no HAL/JSON parsing happens here, only interpretation of
 * the already-extracted fields (`state`, `action`, links, embedded methods, error) into an SDK
 * outcome.
 *
 * Both entry points are declared `internal`, not `public`, because the parser remains module-local
 * request/response plumbing.
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
     * mapped to [NativeAuthV2InteractionApiResult.Redirect]; otherwise a server error is mapped
     * from its V2 wire values; a `state == "continue"` response requires a continuation token and
     * becomes [NativeAuthV2InteractionApiResult.ReadyToComplete]; any other response requires a
     * continuation token before its `action` is switched on to produce
     * [NativeAuthV2InteractionApiResult.ChallengeRequired], [NativeAuthV2InteractionApiResult.CodeRequired],
     * [NativeAuthV2InteractionApiResult.UpdateRequired], or [NativeAuthV2InteractionApiResult.PollInProgress];
     * an `action` this SDK version does not recognise becomes
     * [NativeAuthV2InteractionApiResult.UnsupportedAction] rather than being folded into a generic
     * error.
     *
     * @param response The parsed HAL wire model for this response.
     * @param previousState The continuation state that led to this response.
     */
    internal fun parseInteraction(
        response: NativeAuthV2HalApiResponse,
        previousState: NativeAuthV2ContinuationState
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
                serverError = serverError
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
        val methods = when (val parsed = parseAuthMethods(response)) {
            is ParsedMethods.Failure -> return parsed.error
            is ParsedMethods.Success -> parsed.methods
        }

        val successor = NativeAuthV2ContinuationState.next(
            previous = previousState,
            response = response,
            selectedMethod = null
        ) ?: return missingContinuationTokenError(response.correlationId)

        return when {
            response.isSingleFactorChallenge -> NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = response.correlationId,
                continuationState = successor,
                hint = response.challengeTargetLabel,
                methods = methods
            )
            response.isMultiFactorChallenge -> NativeAuthV2InteractionApiResult.MFARequired(
                correlationId = response.correlationId,
                continuationState = successor,
                methods = methods
            )
            else -> invalidAuthenticationFactorError(response.correlationId)
        }
    }

    /**
     * Validates and normalizes every method embedded in [response], preserving server order.
     *
     * A method is valid only when it carries a nonblank ID, a nonblank type, and a `challenge`
     * link; anything else is a protocol error rather than a silently-skipped entry. A duplicate ID
     * keeps the first occurrence, matching the per-method link map the continuation state retains.
     */
    private fun parseAuthMethods(response: NativeAuthV2HalApiResponse): ParsedMethods {
        if (response.methods.isEmpty()) {
            Logger.warn(TAG, response.correlationId, "Native Auth V2 challenge offered no authentication methods.")
            return ParsedMethods.Failure(
                NativeAuthV2InteractionApiResult.UnknownError(
                    correlationId = response.correlationId,
                    error = ApiErrorResult.INVALID_STATE,
                    errorDescription = "Native Auth V2 challenge response offered no authentication methods."
                )
            )
        }

        val methods = LinkedHashMap<String, NativeAuthV2AuthMethod>()
        response.methods.forEach { method ->
            val id = method.id?.takeUnless { it.isBlank() }
                ?: return ParsedMethods.Failure(malformedMethodError(response.correlationId, METHOD_ID_FIELD))
            val type = method.type?.takeUnless { it.isBlank() }
                ?: return ParsedMethods.Failure(malformedMethodError(response.correlationId, METHOD_TYPE_FIELD))
            if (method.links[NativeAuthV2LinkRelation.CHALLENGE.value].isNullOrBlank()) {
                return ParsedMethods.Failure(
                    missingLinkError(response.correlationId, NativeAuthV2LinkRelation.CHALLENGE)
                )
            }
            if (methods.containsKey(id)) {
                Logger.warn(TAG, response.correlationId, "Native Auth V2 challenge repeated an authentication method ID; keeping the first.")
                return@forEach
            }
            methods[id] = NativeAuthV2AuthMethod(
                id = id,
                type = type.lowercase(Locale.ROOT),
                hint = method.hint
            )
        }

        return ParsedMethods.Success(methods.values.toList())
    }

    private fun malformedMethodError(
        correlationId: String,
        fieldName: String
    ): NativeAuthV2InteractionApiResult.UnknownError {
        Logger.warn(TAG, correlationId, "Native Auth V2 challenge offered a malformed authentication method.")
        return NativeAuthV2InteractionApiResult.UnknownError(
            correlationId = correlationId,
            error = ApiErrorResult.INVALID_STATE,
            errorDescription = "Native Auth V2 challenge response contains an authentication " +
                    "method missing required field '$fieldName'."
        )
    }

    private fun invalidAuthenticationFactorError(
        correlationId: String
    ): NativeAuthV2InteractionApiResult.UnknownError = NativeAuthV2InteractionApiResult.UnknownError(
        correlationId = correlationId,
        error = ApiErrorResult.INVALID_STATE,
        errorDescription = "Native Auth V2 challenge response contains an invalid value for field " +
                "'authenticationFactor'."
    )

    private sealed interface ParsedMethods {
        data class Success(val methods: List<NativeAuthV2AuthMethod>) : ParsedMethods
        data class Failure(val error: NativeAuthV2InteractionApiResult.UnknownError) : ParsedMethods
    }

    private fun parseVerify(
        response: NativeAuthV2HalApiResponse,
        previousState: NativeAuthV2ContinuationState
    ): NativeAuthV2InteractionApiResult {
        val selectedMethod = response.methods.singleOrNull()
        if (selectedMethod?.links?.get(NativeAuthV2LinkRelation.VERIFY.value) == null &&
            response.links[NativeAuthV2LinkRelation.VERIFY.value] == null
        ) {
            return missingLinkError(response.correlationId, NativeAuthV2LinkRelation.VERIFY)
        }

        val challengeChannel = selectedMethod?.type ?: response.challengeChannel
            ?: return missingFieldError(response.correlationId, CHALLENGE_CHANNEL_FIELD)
        val successor = NativeAuthV2ContinuationState.next(previousState, response, selectedMethod)
            ?: return missingContinuationTokenError(response.correlationId)

        if (challengeChannel.isPasswordChannel()) {
            // A password challenge is only meaningful as the first factor. Reaching one on a
            // second-factor step means the server and this SDK disagree about where the flow is,
            // which is unrecoverable: honouring it would re-prompt for a credential the user has
            // already proven.
            if (!previousState.isFirstFactor) {
                return passwordOutsideFirstFactorError(response)
            }
            return NativeAuthV2InteractionApiResult.PasswordRequired(
                correlationId = response.correlationId,
                continuationState = successor
            )
        }

        val codeLength = response.codeLength
            ?: return missingFieldError(response.correlationId, CODE_LENGTH_FIELD)
        if (codeLength <= 0) {
            return invalidFieldError(response.correlationId, CODE_LENGTH_FIELD)
        }
        val challengeTargetLabel = selectedMethod?.hint ?: response.challengeTargetLabel
            ?: return missingFieldError(response.correlationId, CHALLENGE_TARGET_LABEL_FIELD)
        if (!challengeChannel.isEmailChannel()) {
            return unsupportedChallengeMethodError(response)
        }

        return NativeAuthV2InteractionApiResult.CodeRequired(
            correlationId = response.correlationId,
            continuationState = successor,
            challengeTargetLabel = challengeTargetLabel,
            challengeChannel = challengeChannel,
            codeLength = codeLength
        )
    }

    private fun String?.isEmailChannel(): Boolean =
        this?.equals(NativeAuthConstants.ChallengeChannel.EMAIL, ignoreCase = true) == true

    private fun String?.isPasswordChannel(): Boolean =
        this?.equals(NativeAuthConstants.ChallengeType.PASSWORD, ignoreCase = true) == true

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

    /**
     * Error for a password challenge that arrived on a step the server did not classify as the
     * first authentication factor. Distinct from [unsupportedChallengeMethodError] so telemetry can
     * tell "the server offered a channel we do not implement" apart from "the server offered a
     * password at a point in the flow where a password must never be requested".
     */
    private fun passwordOutsideFirstFactorError(
        response: NativeAuthV2HalApiResponse
    ): NativeAuthV2InteractionApiResult.UnknownError {
        Logger.warn(
            TAG,
            response.correlationId,
            "Native Auth V2 returned a password challenge outside the first authentication factor."
        )
        return NativeAuthV2InteractionApiResult.UnknownError(
            correlationId = response.correlationId,
            error = ApiErrorResult.INVALID_STATE,
            errorDescription = "Native Auth V2 returned a password challenge outside the first " +
                    "authentication factor. A password is only accepted as the first factor."
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

    private fun mapInteractionError(
        correlationId: String,
        serverError: NativeAuthV2HalApiResponse.HalServerError
    ): NativeAuthV2InteractionApiResult {
        val code = serverError.code
        val innerErrorCode = serverError.innerErrorCode
        val message = serverError.message
        val errorCodes = extractAadstsCodes(message)

        return when {
            code == ERROR_INVALID_GRANT && innerErrorCode == INNER_ERROR_INVALID_ONE_TIME_CODE ->
                NativeAuthV2InteractionApiResult.InvalidCode(
                    correlationId = correlationId,
                    error = code.orEmpty(),
                    errorDescription = message.orEmpty(),
                    subError = innerErrorCode.orEmpty(),
                    errorCodes = errorCodes
                )

            innerErrorCode == INNER_ERROR_INVALID_CONTINUATION_TOKEN ->
                unknownInteractionError(correlationId, code, message, errorCodes)

            code == ERROR_INVALID_REQUEST && innerErrorCode in INNER_ERROR_INVALID_PASSWORD ->
                NativeAuthV2InteractionApiResult.InvalidPassword(
                    correlationId = correlationId,
                    error = code.orEmpty(),
                    errorDescription = message.orEmpty(),
                    subError = innerErrorCode.orEmpty(),
                    errorCodes = errorCodes
                )

            code == ERROR_INVALID_GRANT &&
                    innerErrorCode == INNER_ERROR_INVALID_USERNAME_OR_PASSWORD ->
                NativeAuthV2InteractionApiResult.InvalidCredentials(
                    correlationId = correlationId,
                    error = code.orEmpty(),
                    errorDescription = message.orEmpty(),
                    subError = innerErrorCode.orEmpty(),
                    errorCodes = errorCodes
                )

            // The account does not exist in the directory. This response carries no inner error
            // code, so it is identified by the AADSTS code in the message. Gate on the outer code
            // as well so that a message which merely mentions AADSTS50034 cannot shadow a
            // recoverable credentials error, which is reported under a different outer code.
            code == ERROR_INVALID_REQUEST && message?.contains(AADSTS_USER_NOT_FOUND) == true ->
                NativeAuthV2InteractionApiResult.UserNotFound(
                    correlationId = correlationId,
                    error = code.orEmpty(),
                    errorDescription = message.orEmpty(),
                    errorCodes = errorCodes
                )

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
        private const val METHOD_ID_FIELD = "id"
        private const val METHOD_TYPE_FIELD = "type"

        private const val ERROR_INVALID_GRANT = "invalidGrant"
        private const val ERROR_INVALID_REQUEST = "invalidRequest"
        private const val INNER_ERROR_INVALID_CONTINUATION_TOKEN = "invalidContinuationToken"

        private const val INNER_ERROR_INVALID_ONE_TIME_CODE = "invalidOneTimeCode"
        private val INNER_ERROR_INVALID_PASSWORD = setOf(
            "passwordTooWeak",
            "passwordTooShort",
            "passwordTooLong",
            "passwordIsInvalid",
            "passwordRecentlyUsed",
            "passwordBanned"
        )
        private const val INNER_ERROR_INVALID_USERNAME_OR_PASSWORD = "invalidUserNameOrPassword"

        private const val AADSTS_USER_NOT_FOUND = "AADSTS50034"
    }
}
