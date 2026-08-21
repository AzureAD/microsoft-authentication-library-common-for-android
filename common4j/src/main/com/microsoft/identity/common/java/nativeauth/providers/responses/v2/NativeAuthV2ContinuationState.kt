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
 * Opaque, common4j-owned mid-flow state for V2 Native Auth.
 *
 * [Serializable] so MSAL can retain it across process death via `Parcel.writeSerializable` without
 * reading its `internal` members, as V1 already does for its opaque
 * `NativeAuthPublicClientApplicationConfiguration`. `serialVersionUID` is pinned rather than
 * derived from the class shape.
 */
class NativeAuthV2ContinuationState private constructor(
    internal val continuationToken: String,
    internal val links: Map<String, String>,
    internal val scopes: List<String>,
    internal val claimsRequestJson: String?,
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
     * Returns the optional claims request retained for the authorization-code token exchange.
     */
    fun claimsRequestJsonForTokenRequest(): String? = claimsRequestJson

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
            entryRelation: NativeAuthV2LinkRelation
        ): NativeAuthV2ContinuationState {
            ArgUtils.validateNonNullArg(continuationToken, "continuationToken")
            return NativeAuthV2ContinuationState(
                continuationToken = continuationToken,
                links = retainSupportedRelations(response.links),
                scopes = defensiveCopy(scopes),
                claimsRequestJson = claimsRequestJson,
                correlationId = response.correlationId,
                entryRelation = entryRelation
            )
        }

        /**
         * Builds a successor continuation state from [previous] plus a new mid-flow [response], or
         * `null` if [response] did not carry a nonblank continuation token.
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
                claimsRequestJson = previous.claimsRequestJson,
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
