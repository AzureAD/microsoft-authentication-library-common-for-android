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

import com.microsoft.identity.common.java.nativeauth.util.ILoggable
import com.microsoft.identity.common.java.util.ArgUtils
import java.io.Serializable
import java.util.Collections

/**
 * Opaque, common4j-owned mid-flow state for V2 Native Auth. Carries the latest continuation
 * token, the server-provided relation-to-href map, the requested scopes, the correlation ID, and
 * the entry relation that started the flow. The retained scopes exist only so the SDK can later
 * exchange a returned authorization code for tokens; they are never re-sent on authorize-
 * challenge calls.
 *
 * Higher layers (Common's non-`common4j` code and MSAL) may only retain and transport this DTO;
 * they cannot inspect [continuationToken], [links], [scopes], or [entryRelation], because those
 * members are `internal` and therefore visible throughout common4j but not outside that module.
 * [toString] and
 * [toUnsanitizedString] deliberately reveal none of this state, not even to internal callers,
 * since accidentally logging this object anywhere would otherwise be a single point of failure
 * for a continuation-token leak.
 */
class NativeAuthV2ContinuationState private constructor(
    internal val continuationToken: String,
    internal val links: Map<String, String>,
    internal val scopes: List<String>,
    val correlationId: String,
    internal val entryRelation: NativeAuthV2LinkRelation
) : ILoggable, Serializable {

    /**
     * Returns a defensive copy of the scopes this state was created with, for the later
     * authorization-code token request at flow completion. Controllers outside common4j access
     * scopes only via this method, keeping the internal [scopes] field opaque.
     */
    fun scopesForTokenRequest(): List<String> = ArrayList(scopes)

    /**
     * Returns the href retained for [relation], or `null` if that relation was not present, or was
     * present but not one of the relations this state retains.
     */
    internal fun href(relation: NativeAuthV2LinkRelation): String? = links[relation.value]

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
            NativeAuthV2LinkRelation.SELF.value,
            NativeAuthV2LinkRelation.RESET_PASSWORD.value,
            NativeAuthV2LinkRelation.SIGN_IN.value,
            NativeAuthV2LinkRelation.SIGN_UP.value
        )

        /**
         * Builds the first continuation state from an authorize-challenge [response] and its
         * already-validated, nonblank [continuationToken].
         *
         * @throws com.microsoft.identity.common.java.exception.ClientException if
         * [continuationToken] is blank.
         */
        internal fun fromAuthorizeChallengeResponse(
            response: NativeAuthV2HalApiResponse,
            continuationToken: String,
            scopes: List<String>,
            entryRelation: NativeAuthV2LinkRelation
        ): NativeAuthV2ContinuationState {
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            return NativeAuthV2ContinuationState(
                continuationToken = continuationToken,
                links = retainSupportedRelations(response.links),
                scopes = defensiveCopy(scopes),
                correlationId = response.correlationId,
                entryRelation = entryRelation
            )
        }

        /**
         * Builds a successor continuation state from [previous] plus a new mid-flow [response], or
         * `null` if [response] did not carry a nonblank continuation token.
         *
         * [selectedMethod]'s links (if any) are merged with [response]'s top-level links, with the
         * selected embedded-method links taking precedence on a relation collision, before the
         * merged map is filtered down to [SUPPORTED_RELATIONS]. It defaults to the first embedded
         * method on [response], which is sufficient while a response only ever carries a single
         * contact method; callers that need a different method (e.g. once the SDK supports choosing
         * among several) can pass it explicitly.
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
                scopes = defensiveCopy(previous.scopes),
                correlationId = response.correlationId,
                entryRelation = previous.entryRelation
            )
        }

        private fun retainSupportedRelations(links: Map<String, String>): Map<String, String> =
            Collections.unmodifiableMap(
                LinkedHashMap(links.filterKeys { it in SUPPORTED_RELATIONS })
            )

        private fun defensiveCopy(values: List<String>): List<String> =
            Collections.unmodifiableList(ArrayList(values))
    }
}
