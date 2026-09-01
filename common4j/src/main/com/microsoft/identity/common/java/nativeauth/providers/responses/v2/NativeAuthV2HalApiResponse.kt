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
 * resolves into a typed SDK outcome.
 */
class NativeAuthV2HalApiResponse private constructor(
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
    val pollIntervalMillis: Int?,
    val authenticationFactor: String?,
    val serverError: HalServerError?,
    // Attributes the server requested via a top-level `attributes` array on a `collectAttributes`
    // response during sign-up. Empty for every response that does not carry that array.
    val requiredAttributes: List<RequiredAttribute>
) : INativeAuthApiResponse(statusCode, correlationIdValue, continuationToken) {

    data class EmbeddedAuthMethod(
        val id: String?,
        val type: String?,
        val hint: String?,
        val links: Map<String, String>
    )

    /**
     * A single entry of a sign-up `collectAttributes` response's top-level `attributes` array.
     * Only the fields the SDK acts on are modeled: [attributeId] (the wire name of the attribute),
     * [inputType] (for example `text` or `password`), and whether it is [required].
     */
    data class RequiredAttribute(
        val attributeId: String?,
        val inputType: String?,
        val required: Boolean?
    )

    data class HalServerError(
        val code: String?,
        val message: String?,
        val innerErrorCode: String?,
        val correlationId: String?,
        // Per-attribute error details from `error.innerError.details`. Empty when the server sent
        // no such array. Used to attribute a sign-up failure (for example `userAlreadyExists` or
        // `passwordPolicyViolation`) to the specific attribute(s) it concerns.
        val details: List<HalErrorDetail>
    )

    /**
     * A single entry of `error.innerError.details`, pairing an error [code] (for example
     * `userAlreadyExists` or `passwordPolicyViolation`) with the [attributeIds] it applies to.
     */
    data class HalErrorDetail(
        val code: String?,
        val attributeIds: List<String>
    )

    val isWebFallbackRequired: Boolean
        get() = serverError?.code == REDIRECT_TO_WEB_ERROR_CODE ||
                state == WEB_FALLBACK_REQUIRED_STATE

    /**
     * `true` when the server declared this challenge to be a multi-factor (second-factor) step via
     * `challengeContext.authenticationFactor`. Matching is case-insensitive so a casing change on
     * the wire cannot silently downgrade an MFA challenge to a first-factor one.
     */
    val isMultiFactorChallenge: Boolean
        get() = authenticationFactor.equals(MULTI_FACTOR, ignoreCase = true)

    /**
     * `true` when the server declared this challenge to be the single (first) factor via
     * `challengeContext.authenticationFactor`.
     */
    val isSingleFactorChallenge: Boolean
        get() = authenticationFactor.equals(SINGLE_FACTOR, ignoreCase = true)

    /**
     * PII-bearing string. Still never includes [continuationToken], [authorizationCode], any href
     * value (from [links] or an embedded method's links), or a raw [HalResource] property.
     */
    override fun toUnsanitizedString(): String = "NativeAuthV2HalApiResponse(statusCode=$statusCode, " +
            "correlationId=$correlationId, state=$state, action=${action?.value}, " +
            "linkRelations=${links.keys}, methodCount=${methods.size}, codeLength=$codeLength, " +
            "challengeTargetLabel=$challengeTargetLabel, challengeChannel=$challengeChannel, " +
            "authenticationFactor=$authenticationFactor, " +
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
            "challengeChannel=$challengeChannel, authenticationFactor=$authenticationFactor, " +
            "hasAuthorizationCode=${authorizationCode != null}, " +
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
         * Wrapper object carrying challenge metadata the SDK needs to interpret a `challenge`
         * action, most importantly whether the challenge is the first factor or a second factor.
         */
        private const val CHALLENGE_CONTEXT_KEY = "challengeContext"
        private const val AUTHENTICATION_FACTOR_KEY = "authenticationFactor"

        /** `challengeContext.authenticationFactor` value for a first-factor challenge. */
        const val SINGLE_FACTOR = "singleFactor"

        /** `challengeContext.authenticationFactor` value for a second-factor (MFA) challenge. */
        const val MULTI_FACTOR = "multiFactor"

        /**
         * Server-suggested delay, in milliseconds, before the next poll of an in-progress
         * operation.
         */
        private const val POLL_INTERVAL_KEY = "pollInterval"

        /**
         * The authorize-challenge response returns the authorization code as a top-level `code`
         * property.
         */
        private const val AUTHORIZATION_CODE_SHORT_KEY = "code"
        private const val CONTINUATION_TOKEN_CAMEL_KEY = "continuationToken"
        private const val CONTINUATION_TOKEN_SNAKE_KEY = "continuation_token"
        private const val METHODS_RELATION = "methods"
        private const val ERROR_KEY = "error"
        private const val INNER_ERROR_KEY = "innerError"
        private const val ERROR_CODE_KEY = "code"
        private const val ERROR_DETAILS_KEY = "details"
        private const val ERROR_ATTRIBUTE_IDS_KEY = "attributeIds"
        private const val ATTRIBUTES_KEY = "attributes"
        private const val ATTRIBUTE_ID_KEY = "attributeId"
        private const val ATTRIBUTE_INPUT_TYPE_KEY = "inputType"
        private const val ATTRIBUTE_REQUIRED_KEY = "required"
        private const val ERROR_MESSAGE_KEY = "message"
        private const val ERROR_DESCRIPTION_KEY = "error_description"
        private const val ERROR_CORRELATION_ID_KEY = "correlationId"
        private const val ERROR_CORRELATION_ID_SNAKE_KEY = "correlation_id"
        private const val REDIRECT_TO_WEB_ERROR_CODE = "redirect_to_web"
        private const val WEB_FALLBACK_REQUIRED_STATE = "webFallbackRequired"

        /**
         * Builds a [NativeAuthV2HalApiResponse] by mapping [halResource]'s wire shape onto this
         * model's fields.
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
                pollIntervalMillis = halResource.int(POLL_INTERVAL_KEY),
                authenticationFactor = extractAuthenticationFactor(halResource),
                serverError = serverError,
                requiredAttributes = extractRequiredAttributes(halResource)
            )
        }

        /**
         * Builds a [NativeAuthV2HalApiResponse] carrying only a client-side [HalServerError], for
         * responses the SDK rejects before (or instead of) mapping a server body: an empty body, a
         * body that is not valid JSON, or a status the SDK refuses to body-parse.
         *
         * Constructs the model directly rather than round-tripping a synthesised JSON document
         * through [HalResource], which would re-parse data the caller already holds and would break
         * on any [errorMessage] containing JSON metacharacters.
         */
        internal fun error(
            statusCode: Int,
            correlationId: String,
            errorCode: String,
            errorMessage: String
        ): NativeAuthV2HalApiResponse = NativeAuthV2HalApiResponse(
            statusCode = statusCode,
            correlationIdValue = correlationId,
            continuationToken = null,
            state = null,
            action = null,
            links = emptyMap(),
            methods = emptyList(),
            codeLength = null,
            challengeTargetLabel = null,
            challengeChannel = null,
            authorizationCode = null,
            pollIntervalMillis = null,
            authenticationFactor = null,
            serverError = HalServerError(
                code = errorCode,
                message = errorMessage,
                innerErrorCode = null,
                correlationId = correlationId,
                details = emptyList()
            ),
            requiredAttributes = emptyList()
        )

        private fun toEmbeddedAuthMethod(resource: HalResource): EmbeddedAuthMethod = EmbeddedAuthMethod(
            id = resource.string("id"),
            type = resource.string(TYPE_KEY),
            hint = resource.string(HINT_KEY),
            links = flattenFirstHref(resource.links)
        )

        /**
         * Reads `challengeContext.authenticationFactor`, accepting only a nested object shape.
         * Any other shape leaves the factor unset, so the parser treats the challenge as
         * unclassified rather than guessing.
         */
        private fun extractAuthenticationFactor(halResource: HalResource): String? {
            val challengeContext = halResource.properties[CHALLENGE_CONTEXT_KEY] as? Map<*, *>
                ?: return null
            return (challengeContext[AUTHENTICATION_FACTOR_KEY] as? String)?.takeUnless { it.isBlank() }
        }

        private fun flattenFirstHref(links: Map<String, List<HalLink>>): Map<String, String> =
            links.mapNotNull { (relation, halLinks) ->
                halLinks.firstOrNull { isFollowable(it) }?.let { relation to it.href }
            }
                .toMap()

        private fun isFollowable(link: HalLink): Boolean =
            !link.templated || isSupportedTenantTemplate(link.href)

        private fun isSupportedTenantTemplate(href: String): Boolean {
            val withoutLeadingSlash = href.removePrefix("/")
            if (!withoutLeadingSlash.startsWith("$TENANT_TEMPLATE/")) {
                return false
            }
            val remainder = withoutLeadingSlash.removePrefix(TENANT_TEMPLATE)
            return !remainder.contains('{') && !remainder.contains('}')
        }

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

        private const val TENANT_TEMPLATE = "{tenant}"

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
            return when (val errorValue = halResource.properties[ERROR_KEY]) {
                is Map<*, *> -> {
                    val innerErrorMap = errorValue[INNER_ERROR_KEY] as? Map<*, *>
                    HalServerError(
                        code = errorValue[ERROR_CODE_KEY] as? String,
                        message = errorValue[ERROR_MESSAGE_KEY] as? String,
                        innerErrorCode = innerErrorMap?.get(ERROR_CODE_KEY) as? String,
                        correlationId = errorValue[ERROR_CORRELATION_ID_KEY] as? String,
                        details = extractErrorDetails(innerErrorMap)
                    )
                }

                is String -> HalServerError(
                    code = errorValue,
                    message = halResource.string(ERROR_DESCRIPTION_KEY),
                    innerErrorCode = null,
                    correlationId = halResource.string(ERROR_CORRELATION_ID_SNAKE_KEY)
                        ?: halResource.string(ERROR_CORRELATION_ID_KEY),
                    details = emptyList()
                )

                else -> null
            }
        }

        /**
         * Parses `error.innerError.details` into [HalErrorDetail]s, keeping only entries that carry
         * a usable shape. A detail with no `attributeIds` is retained with an empty list so an
         * error keyed purely on its `code` (for example a code without a specific attribute) is not
         * silently dropped.
         */
        private fun extractErrorDetails(innerErrorMap: Map<*, *>?): List<HalErrorDetail> {
            val details = innerErrorMap?.get(ERROR_DETAILS_KEY) as? List<*> ?: return emptyList()
            return details.mapNotNull { detail ->
                val detailMap = detail as? Map<*, *> ?: return@mapNotNull null
                val attributeIds = (detailMap[ERROR_ATTRIBUTE_IDS_KEY] as? List<*>)
                    ?.mapNotNull { it as? String }
                    .orEmpty()
                HalErrorDetail(
                    code = detailMap[ERROR_CODE_KEY] as? String,
                    attributeIds = attributeIds
                )
            }
        }

        /**
         * Parses a sign-up `collectAttributes` response's top-level `attributes` array into
         * [RequiredAttribute]s. Returns an empty list when the property is absent or not an array.
         */
        private fun extractRequiredAttributes(halResource: HalResource): List<RequiredAttribute> {
            val attributes = halResource.properties[ATTRIBUTES_KEY] as? List<*> ?: return emptyList()
            return attributes.mapNotNull { attribute ->
                val attributeMap = attribute as? Map<*, *> ?: return@mapNotNull null
                RequiredAttribute(
                    attributeId = attributeMap[ATTRIBUTE_ID_KEY] as? String,
                    inputType = attributeMap[ATTRIBUTE_INPUT_TYPE_KEY] as? String,
                    required = attributeMap[ATTRIBUTE_REQUIRED_KEY] as? Boolean
                )
            }
        }
    }
}
