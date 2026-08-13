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

import com.microsoft.identity.common.java.nativeauth.providers.INativeAuthApiResponse

/**
 * Single wire model for every V2 HAL Native Auth API response body. There are no response
 * subclasses per state/action pair; [state] and [action] are the discriminator that the parser
 * (T4) resolves into a typed SDK outcome.
 *
 * Two shapes are intentionally adapted from the design vocabulary to fit [INativeAuthApiResponse],
 * common4j's existing (non-open) V1 response base class:
 * - [INativeAuthApiResponse.correlationId] is a non-`open` `var` on the base class, so it cannot be
 *   overridden here. It is instead re-declared under a different name, [correlationIdValue] (an
 *   `internal` property, forwarded to the base constructor), and the inherited `correlationId`
 *   member remains the one other common4j code should read.
 * - the HAL server error is exposed as [serverError] rather than overriding the base class's
 *   `error: String?`, because [HalServerError] carries structured detail that is not
 *   assignment-compatible with that member's type.
 *
 * Instances are only ever produced via [from]; the primary constructor is private so that
 * [isWebFallbackRequired] can never be constructed out of sync with [serverError] and [state].
 */
data class NativeAuthV2HalApiResponse private constructor(
    override val statusCode: Int,
    internal val correlationIdValue: String,
    override val continuationToken: String?,
    val state: String?,
    val action: NativeAuthV2HalAction?,
    val links: Map<String, String>,
    val methods: List<EmbeddedAuthMethod>,
    val codeLength: Int?,
    val challengeTargetLabel: String?,
    val challengeChannel: String?,
    val authorizationCode: String?,
    val serverError: HalServerError?,
    val isWebFallbackRequired: Boolean
) : INativeAuthApiResponse(statusCode, correlationIdValue, continuationToken) {

    data class EmbeddedAuthMethod(
        val id: String?,
        val type: String?,
        val hint: String?,
        val links: Map<String, String>
    )

    data class HalServerError(
        val code: String?,
        val message: String?,
        val innerErrorCode: String?,
        val correlationId: String?
    )

    /**
     * PII-bearing string. Still never includes [continuationToken], [authorizationCode], any href
     * value (from [links] or an embedded method's links), or a raw [HalResource] property.
     */
    override fun toUnsanitizedString(): String = "NativeAuthV2HalApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId, state=$state, action=${action?.value}, " +
            "linkRelations=${links.keys}, methodCount=${methods.size}, codeLength=$codeLength, " +
            "challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel, " +
            "hasAuthorizationCode=${authorizationCode != null}, " +
            "error=${serverError?.let { "(code=${it.code}, innerErrorCode=${it.innerErrorCode})" }}, " +
            "isWebFallbackRequired=$isWebFallbackRequired)"

    /**
     * Production-log-safe string. Omits [challengeTargetLabel] (a server-supplied hint that can
     * carry PII, e.g. a partially-masked email or phone number) in addition to the secrets always
     * excluded from both string methods.
     */
    override fun toString(): String = "NativeAuthV2HalApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId, state=$state, action=${action?.value}, " +
            "linkRelations=${links.keys}, methodCount=${methods.size}, codeLength=$codeLength, " +
            "challengeChannel=$challengeChannel, hasAuthorizationCode=${authorizationCode != null}, " +
            "error=${serverError?.let { "(code=${it.code}, innerErrorCode=${it.innerErrorCode})" }}, " +
            "isWebFallbackRequired=$isWebFallbackRequired)"

    companion object {
        private const val STATE_KEY = "state"
        private const val ACTION_KEY = "action"
        private const val CODE_LENGTH_KEY = "codeLength"
        private const val HINT_KEY = "hint"
        private const val TYPE_KEY = "type"
        private const val AUTHORIZATION_CODE_KEY = "authorizationCode"

        /**
         * The authorize-challenge response returns the authorization code as a top-level `code`
         * property. This is distinct from [ERROR_CODE_KEY], which is only ever read from inside
         * the nested `error` object, so the two cannot collide.
         */
        private const val AUTHORIZATION_CODE_SHORT_KEY = "code"
        private const val CONTINUATION_TOKEN_CAMEL_KEY = "continuationToken"
        private const val CONTINUATION_TOKEN_SNAKE_KEY = "continuation_token"
        private const val METHODS_RELATION = "methods"
        private const val ERROR_KEY = "error"
        private const val INNER_ERROR_KEY = "innerError"
        private const val ERROR_CODE_KEY = "code"
        private const val ERROR_MESSAGE_KEY = "message"
        private const val ERROR_CORRELATION_ID_KEY = "correlationId"
        private const val REDIRECT_TO_WEB_ERROR_CODE = "redirect_to_web"
        private const val WEB_FALLBACK_REQUIRED_STATE = "webFallbackRequired"

        /**
         * Builds a [NativeAuthV2HalApiResponse] by mechanically mapping [halResource]'s wire
         * shape onto this model's fields (including handling both continuation-token spellings
         * the service may use). This performs no state/action-based interpretation, error mapping,
         * or operation-specific business logic; that is the parser's job (T4/T6). [halResource]
         * itself is never retained or logged.
         */
        internal fun from(
            halResource: HalResource,
            statusCode: Int,
            correlationId: String
        ): NativeAuthV2HalApiResponse {
            val serverError = extractServerError(halResource)
            val state = halResource.string(STATE_KEY)
            val actionRaw = halResource.string(ACTION_KEY)

            return NativeAuthV2HalApiResponse(
                statusCode = statusCode,
                correlationIdValue = correlationId,
                continuationToken = halResource.string(CONTINUATION_TOKEN_CAMEL_KEY)
                    ?: halResource.string(CONTINUATION_TOKEN_SNAKE_KEY),
                state = state,
                action = actionRaw?.let { NativeAuthV2HalAction(it) },
                links = mergeEntryLinks(halResource),
                methods = halResource.embeddedResources(METHODS_RELATION).map { toEmbeddedAuthMethod(it) },
                codeLength = halResource.int(CODE_LENGTH_KEY),
                challengeTargetLabel = halResource.string(HINT_KEY),
                challengeChannel = halResource.string(TYPE_KEY),
                authorizationCode = halResource.string(AUTHORIZATION_CODE_KEY)
                    ?: halResource.string(AUTHORIZATION_CODE_SHORT_KEY),
                serverError = serverError,
                isWebFallbackRequired = serverError?.code == REDIRECT_TO_WEB_ERROR_CODE ||
                        state == WEB_FALLBACK_REQUIRED_STATE
            )
        }

        private fun toEmbeddedAuthMethod(resource: HalResource): EmbeddedAuthMethod = EmbeddedAuthMethod(
            id = resource.string("id"),
            type = resource.string(TYPE_KEY),
            hint = resource.string(HINT_KEY),
            links = flattenFirstHref(resource.links)
        )

        private fun flattenFirstHref(links: Map<String, List<HalLink>>): Map<String, String> =
            links.mapNotNull { (relation, halLinks) -> halLinks.firstOrNull()?.let { relation to it.href } }
                .toMap()

        /**
         * Flat top-level link properties (snake_case) the authorize-challenge *start* response
         * returns as siblings of `continuation_token`, rather than under a HAL `_links` object,
         * mapped to this SDK's relation keys.
         */
        private val FLAT_ENTRY_LINK_PROPERTIES: Map<String, String> = mapOf(
            "reset_password" to NativeAuthV2LinkRelation.RESET_PASSWORD.value,
            "sign_in" to NativeAuthV2LinkRelation.SIGN_IN.value,
            "sign_up" to NativeAuthV2LinkRelation.SIGN_UP.value
        )

        /**
         * Builds the relation-to-href map from both wire shapes the service uses: the flat
         * top-level entry links returned by authorize-challenge start, and standard HAL `_links`
         * used by mid-flow responses. HAL `_links` take precedence on a relation collision.
         */
        private fun mergeEntryLinks(halResource: HalResource): Map<String, String> {
            val links = LinkedHashMap<String, String>()
            FLAT_ENTRY_LINK_PROPERTIES.forEach { (property, relation) ->
                halResource.string(property)?.takeIf { it.isNotBlank() }?.let { href ->
                    links[relation] = href
                }
            }
            links.putAll(flattenFirstHref(halResource.links))
            return links
        }

        private fun extractServerError(halResource: HalResource): HalServerError? {
            val errorMap = halResource.properties[ERROR_KEY] as? Map<*, *> ?: return null
            val innerErrorMap = errorMap[INNER_ERROR_KEY] as? Map<*, *>
            return HalServerError(
                code = errorMap[ERROR_CODE_KEY] as? String,
                message = errorMap[ERROR_MESSAGE_KEY] as? String,
                innerErrorCode = innerErrorMap?.get(ERROR_CODE_KEY) as? String,
                correlationId = errorMap[ERROR_CORRELATION_ID_KEY] as? String
            )
        }
    }
}
