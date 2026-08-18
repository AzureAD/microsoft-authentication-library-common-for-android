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
import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult

/**
 * Parses [NativeAuthV2HalApiResponse] wire models into the typed [AuthorizeChallengeApiResult] and
 * result family.
 *
 * This is business logic layered on top of T3's mechanical wire extraction
 * ([NativeAuthV2HalApiResponse.from]): no HAL/JSON parsing happens here, only interpretation of
 * the already-extracted fields (`state`, `action`, links, embedded methods, error) into an SDK
 * outcome.
 *
 * The entry point is `internal`, not `public`, because the parser remains module-local
 * request/response plumbing. This mirrors the same constraint applied to
 * [NativeAuthV2ContinuationState]'s factories.
 */
class NativeAuthV2ResponseParser {

    /**
     * Parses the response from the Native Auth V2 authorize-challenge endpoint.
     *
     * Evaluated in order: [NativeAuthV2HalApiResponse.isWebFallbackRequired] always wins and is
     * mapped to [AuthorizeChallengeApiResult.Redirect]; otherwise a server error is mapped to
     * [AuthorizeChallengeApiResult.UnknownError]; an [NativeAuthV2HalApiResponse.authorizationCode]
     * wins over a continuation token; a continuation token requires [entryRelation] to be present
     * in [NativeAuthV2HalApiResponse.links] (else [AuthorizeChallengeApiResult.UnknownError]) before a successor
     * [NativeAuthV2ContinuationState] is built and returned as [AuthorizeChallengeApiResult.ContinuationRequired];
     * a response with none of the above is also [AuthorizeChallengeApiResult.UnknownError].
     *
     * @param response The parsed HAL wire model for this response.
     * @param entryRelation The `_links` relation the next request must follow (flow-specific entry
     * point, e.g. `resetPassword`).
     * @param scopes The scopes requested for this flow, retained only for the later
     * authorization-code token exchange. They are not sent on authorize-challenge requests.
     */
    internal fun parseAuthorizeChallenge(
        response: NativeAuthV2HalApiResponse,
        entryRelation: NativeAuthV2LinkRelation,
        scopes: List<String>
    ): AuthorizeChallengeApiResult {
        LogSession.logMethodCall(
            tag = TAG,
            correlationId = response.correlationId,
            methodName = "$TAG.parseAuthorizeChallenge"
        )

        if (response.isWebFallbackRequired) {
            return AuthorizeChallengeApiResult.Redirect(
                correlationId = response.correlationId,
                redirectReason = response.serverError?.code ?: response.state ?: WEB_FALLBACK_REDIRECT_REASON
            )
        }

        response.serverError?.let { serverError ->
            return AuthorizeChallengeApiResult.UnknownError(
                correlationId = response.correlationId,
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

        val continuationToken = response.continuationToken
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
                entryRelation = entryRelation
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

        private const val WEB_FALLBACK_REDIRECT_REASON = "web_fallback_required"
    }
}
