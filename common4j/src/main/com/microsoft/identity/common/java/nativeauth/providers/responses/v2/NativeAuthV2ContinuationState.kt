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

import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.nativeauth.util.ILoggable
import com.microsoft.identity.common.java.util.ArgUtils
import java.io.Serializable
import java.util.Collections

/**
 * Opaque, common4j-owned mid-flow state for V2 Native Auth. Carries the latest continuation
 * token, the server-provided relation-to-href map, the per-method relation-to-href maps of the
 * methods the server offered, the requested scopes, the correlation ID, the flow entry relation,
 * and the internal flow scenario that produced it.
 *
 * Higher layers (Common's non-`common4j` code and MSAL) may only retain and transport this DTO;
 * they cannot inspect [continuationToken], [links], [methodLinks], [scopes], [entryRelation], or
 * [scenario], because those members are `internal` to this module. Only common4j Native Auth V2
 * protocol code (this package and `nativeauth.providers.v2`) can read them, e.g. to build the next
 * request.
 * [toString] and [toUnsanitizedString] deliberately reveal none of this state, not even to
 * internal callers, since accidentally logging this object anywhere would otherwise be a single
 * point of failure for a continuation-token leak.
 *
 * [Serializable] so MSAL can retain it across process death via `Parcel.writeSerializable` without
 * reading its `internal` members, as V1 already does for its opaque
 * `NativeAuthPublicClientApplicationConfiguration`. `serialVersionUID` is pinned rather than
 * derived from the class shape.
 */
class NativeAuthV2ContinuationState private constructor(
    internal val continuationToken: String,
    internal val links: Map<String, String>,
    internal val methodLinks: Map<String, Map<String, String>>,
    internal val scopes: List<String>,
    internal val claimsRequestJson: String?,
    val correlationId: String,
    internal val entryRelation: NativeAuthV2LinkRelation,
    internal val scenario: NativeAuthV2FlowScenario
) : ILoggable, Serializable {

    /**
     * Returns a defensive copy of the scopes this state was created with, for the later
     * authorization-code token request at flow completion. Controllers outside common4j access
     * scopes only via this method, keeping the internal [scopes] field opaque.
     */
    fun scopesForTokenRequest(): List<String> = ArrayList(scopes)

    /**
     * Returns the optional claims request retained for the authorization-code token exchange.
     */
    fun claimsRequestJsonForTokenRequest(): String? = claimsRequestJson

    /**
     * Returns the href retained for [relation], or `null` if that relation was not present, or was
     * present but not one of the relations this state retains.
     */
    internal fun href(relation: NativeAuthV2LinkRelation): String? = links[relation.value]

    /**
     * Returns a successor state in which the links of the server method identified by [methodId]
     * have been promoted into the state's own relation map, so the next request follows exactly
     * the href the server attached to that method. Returns `null` when [methodId] is not one of
     * the methods this state retained, so a stale or fabricated method identifier fails
     * deterministically instead of falling back to another method's href.
     */
    internal fun withSelectedMethod(methodId: String): NativeAuthV2ContinuationState? {
        val selectedLinks = methodLinks[methodId] ?: return null
        val merged = LinkedHashMap<String, String>(links)
        merged.putAll(selectedLinks)
        return NativeAuthV2ContinuationState(
            continuationToken = continuationToken,
            links = retainSupportedRelations(merged),
            methodLinks = methodLinks,
            scopes = defensiveCopy(scopes),
            claimsRequestJson = claimsRequestJson,
            correlationId = correlationId,
            entryRelation = entryRelation,
            scenario = scenario
        )
    }

    override fun toUnsanitizedString(): String = REDACTED_STRING

    override fun toString(): String = REDACTED_STRING

    companion object {
        private const val serialVersionUID = 1L
        private const val REDACTED_STRING = "NativeAuthV2ContinuationState(<redacted>)"

        /**
         * Link relations this SDK version follows. An unsupported/unrecognised relation is
         * dropped defensively here rather than carried forward indefinitely in mid-flow state.
         */
        private val SUPPORTED_RELATIONS: Set<String> = setOf(
            NativeAuthV2LinkRelation.CHALLENGE.value,
            NativeAuthV2LinkRelation.VERIFY.value,
            NativeAuthV2LinkRelation.RESEND.value,
            NativeAuthV2LinkRelation.UPDATE.value,
            NativeAuthV2LinkRelation.POLL.value,
            NativeAuthV2LinkRelation.CONTINUE.value,
            NativeAuthV2LinkRelation.RESET_PASSWORD.value,
            NativeAuthV2LinkRelation.SIGN_IN.value,
            NativeAuthV2LinkRelation.SIGN_UP.value
        )

        /**
         * Builds the first continuation state from an authorize-challenge [response].
         */
        internal fun fromAuthorizeChallengeResponse(
            response: NativeAuthV2HalApiResponse,
            continuationToken: String,
            scopes: List<String>,
            claimsRequestJson: String? = null,
            entryRelation: NativeAuthV2LinkRelation,
            scenario: NativeAuthV2FlowScenario
        ): NativeAuthV2ContinuationState {
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            return NativeAuthV2ContinuationState(
                continuationToken = continuationToken,
                links = retainSupportedRelations(response.links),
                methodLinks = retainMethodLinks(response),
                scopes = defensiveCopy(scopes),
                claimsRequestJson = claimsRequestJson,
                correlationId = response.correlationId,
                entryRelation = entryRelation,
                scenario = scenario
            )
        }

        /**
         * Builds a successor continuation state from [previous] plus a new mid-flow [response], or
         * `null` if [response] did not carry a nonblank continuation token.
         *
         * [selectedMethod] merges a single method's links into the successor's own relation map, as
         * the SSPR flow needs when the parser implicitly selects the one supported method. The V2
         * sign-in flow instead defers selection and relies on [withSelectedMethod], which reads the
         * per-method links this factory always retains from [response].
         */
        internal fun next(
            previous: NativeAuthV2ContinuationState,
            response: NativeAuthV2HalApiResponse,
            selectedMethod: NativeAuthV2HalApiResponse.EmbeddedAuthMethod? = response.methods.firstOrNull()
        ): NativeAuthV2ContinuationState? {
            val token = response.continuationToken?.takeUnless { it.isBlank() } ?: return null
            val merged = LinkedHashMap<String, String>()
            merged.putAll(response.links)
            selectedMethod?.links?.let { merged.putAll(it) }
            return NativeAuthV2ContinuationState(
                continuationToken = token,
                links = retainSupportedRelations(merged),
                methodLinks = retainMethodLinks(response),
                scopes = defensiveCopy(previous.scopes),
                claimsRequestJson = previous.claimsRequestJson,
                correlationId = response.correlationId,
                entryRelation = previous.entryRelation,
                scenario = previous.scenario
            )
        }

        /**
         * Retains the supported links of every embedded method that carries a nonblank ID, keyed by
         * that ID, so [withSelectedMethod] can later follow the exact href the server attached to
         * the method the caller chose. A duplicate ID keeps the first occurrence, matching the
         * server-order preference the parser applies when it surfaces the methods themselves.
         */
        private fun retainMethodLinks(
            response: NativeAuthV2HalApiResponse
        ): Map<String, Map<String, String>> {
            val methodLinks = LinkedHashMap<String, Map<String, String>>()
            response.methods.forEach { method ->
                val id = method.id?.takeUnless { it.isBlank() } ?: return@forEach
                if (methodLinks.containsKey(id)) {
                    return@forEach
                }
                methodLinks[id] = retainSupportedRelations(method.links)
            }
            return Collections.unmodifiableMap(methodLinks)
        }

        private fun retainSupportedRelations(links: Map<String, String>): Map<String, String> =
            Collections.unmodifiableMap(
                LinkedHashMap(links.filterKeys { it in SUPPORTED_RELATIONS })
            )

        private fun defensiveCopy(values: List<String>): List<String> =
            Collections.unmodifiableList(ArrayList(values))
    }
}
