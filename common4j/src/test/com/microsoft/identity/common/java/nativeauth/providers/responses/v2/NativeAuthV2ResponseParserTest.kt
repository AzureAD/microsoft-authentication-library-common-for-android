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
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2ResponseParserTest {
    private val parser = NativeAuthV2ResponseParser()

    @Test
    fun parseInteraction_whenActionMissing_returnsUnknownErrorForMissingActionField() {
        val response = responseFrom("""{"continuationToken":"token"}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.VERIFY)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        assertFalse(result is NativeAuthV2InteractionApiResult.UnsupportedAction)

        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("action"))
    }

    @Test
    fun parseInteraction_whenActionUnknown_returnsUnsupportedActionWithExactRawValue() {
        val response = responseFrom(
            """{"continuationToken":"token","action":"mystery-action"}"""
        )

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.VERIFY)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnsupportedAction)

        val unsupported = result as NativeAuthV2InteractionApiResult.UnsupportedAction
        assertEquals("mystery-action", unsupported.rawAction)
        assertEquals(ApiErrorResult.INVALID_STATE, unsupported.error)
        assertTrue(unsupported.errorDescription.contains("mystery-action"))
    }

    @Test
    fun parseAuthorizeChallenge_whenEntryLinksAreFlatSnakeCaseProperties_returnsContinuationRequired() {
        // The authorize-challenge start response returns the entry links as flat top-level
        // snake_case properties (siblings of continuation_token), not under a HAL `_links` object.
        val response = responseFrom(
            """
            {
              "continuation_token": "flow-token",
              "reset_password": "/tenant/api/v0.1/auth/resetpassword?dc=TEST",
              "sign_in": "/tenant/api/v0.1/signin/start?dc=TEST",
              "sign_up": "/tenant/api/v0.1/signup/start?dc=TEST"
            }
            """.trimIndent()
        )

        val result = parser.parseAuthorizeChallenge(
            response = response,
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.ContinuationRequired)
        val continuation = result as AuthorizeChallengeApiResult.ContinuationRequired
        assertEquals("flow-token", continuation.continuationState.continuationToken)
        assertEquals(
            "/tenant/api/v0.1/auth/resetpassword?dc=TEST",
            continuation.continuationState.href(NativeAuthV2LinkRelation.RESET_PASSWORD)
        )
    }

    @Test
    fun parseAuthorizeChallenge_whenWebFallbackRequired_returnsRedirect() {
        listOf(
            """{"error":{"code":"redirect_to_web","message":"Browser required."}}""" to "redirect_to_web",
            """{"state":"webFallbackRequired"}""" to "webFallbackRequired"
        ).forEach { (json, expectedReason) ->
            val result = parser.parseAuthorizeChallenge(
                response = responseFrom(json),
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = listOf("User.Read")
            )

            assertTrue(result is AuthorizeChallengeApiResult.Redirect)
            val redirect = result as AuthorizeChallengeApiResult.Redirect
            assertEquals(CORRELATION_ID, redirect.correlationId)
            assertEquals(expectedReason, redirect.redirectReason)
        }
    }

    @Test
    fun parseInteraction_whenVerifyMetadataIsEmbedded_prefersEmbeddedMethodValues() {
        val response = responseFrom(
            """
            {
              "continuationToken": "next-token",
              "action": "verify",
              "codeLength": 6,
              "hint": "top-level-hint",
              "type": "top-level-type",
              "_links": {
                "verify": {"href": "/api/v0.1/auth/top-level/verify"}
              },
              "_embedded": {
                "methods": [{
                  "hint": "embedded-hint",
                  "type": "embedded-type",
                  "_links": {
                    "verify": {"href": "/api/v0.1/auth/embedded/verify"}
                  }
                }]
              }
            }
            """.trimIndent()
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.CHALLENGE
        ) as NativeAuthV2InteractionApiResult.CodeRequired

        assertEquals("embedded-hint", result.challengeTargetLabel)
        assertEquals("embedded-type", result.challengeChannel)
        assertEquals(
            "/api/v0.1/auth/embedded/verify",
            result.continuationState.href(NativeAuthV2LinkRelation.VERIFY)
        )
        assertEquals(6, result.codeLength)
    }

    @Test
    fun parseInteraction_whenVerifyMetadataIsTopLevel_usesTopLevelValues() {
        val response = responseFrom(
            """
            {
              "continuationToken": "next-token",
              "action": "verify",
              "codeLength": 8,
              "hint": "m***@contoso.com",
              "type": "email",
              "_links": {
                "verify": {"href": "/api/v0.1/auth/top-level/verify"}
              }
            }
            """.trimIndent()
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.CHALLENGE
        ) as NativeAuthV2InteractionApiResult.CodeRequired

        assertEquals("m***@contoso.com", result.challengeTargetLabel)
        assertEquals("email", result.challengeChannel)
        assertEquals(
            "/api/v0.1/auth/top-level/verify",
            result.continuationState.href(NativeAuthV2LinkRelation.VERIFY)
        )
        assertEquals(8, result.codeLength)
    }

    @Test
    fun parseInteraction_whenContinuationTokenIsRejected_returnsNonRetryableUnknownError() {
        val response = responseFrom(
            """{"error":{"code":"invalid_grant","message":"Flow state expired.","innerError":{"code":"invalid_continuation_token"}}}"""
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.VERIFY
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals("invalid_grant", error.error)
        assertEquals("Flow state expired.", error.errorDescription)
    }

    @Test
    fun parseInteraction_whenVerificationCodeIsInvalid_returnsRetryableInvalidCode() {
        val previousState = previousState()
        val response = responseFrom(
            """{"error":{"code":"invalid_grant","message":"Code is invalid.","innerError":{"code":"invalid_oob_value"}}}"""
        )

        val result = parser.parseInteraction(
            response,
            previousState,
            NativeAuthV2Operation.VERIFY
        ) as NativeAuthV2InteractionApiResult.InvalidCode

        assertEquals("invalid_oob_value", result.subError)
        assertSame(previousState, result.retryState)
    }

    @Test
    fun parseInteraction_whenPasswordTooWeak_preservesSubError() {
        val response = responseFrom(
            """{"error":{"code":"invalid_request","message":"Password is too weak.","innerError":{"code":"password_too_weak"}}}"""
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.UPDATE_PASSWORD
        ) as NativeAuthV2InteractionApiResult.InvalidPassword

        assertEquals("password_too_weak", result.subError)
    }

    @Test
    fun parseInteraction_whenVerifyInvalidGrantOmitsInnerErrorCode_returnsUnknownError() {
        val response = responseFrom(
            """{"error":{"code":"invalid_grant","message":"Code is invalid.","innerError":{}}}"""
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.VERIFY
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals("invalid_grant", error.error)
        assertEquals("Code is invalid.", error.errorDescription)
    }

    @Test
    fun parseInteraction_whenVerifyInvalidGrantHasInvalidUsernameOrPasswordInnerError_returnsUnknownError() {
        val response = responseFrom(
            """{"error":{"code":"invalid_grant","message":"Credential validation failed.","innerError":{"code":"invalid_username_or_password"}}}"""
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.VERIFY
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        assertFalse(result is NativeAuthV2InteractionApiResult.InvalidCode)

        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals("invalid_grant", error.error)
        assertEquals("Credential validation failed.", error.errorDescription)
    }

    // region parseAuthorizeChallenge - additional success/error relations

    @Test
    fun parseAuthorizeChallenge_whenAuthorizationCodePresent_returnsAuthorizationCode() {
        val response = responseFrom("""{"code":"auth-code-123"}""")

        val result = parser.parseAuthorizeChallenge(
            response = response,
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.AuthorizationCode)
        val authorizationCode = result as AuthorizeChallengeApiResult.AuthorizationCode
        assertEquals(CORRELATION_ID, authorizationCode.correlationId)
        assertEquals("auth-code-123", authorizationCode.code)
    }

    @Test
    fun parseAuthorizeChallenge_whenServerErrorPresent_returnsUnknownErrorWithExtractedAadstsCodes() {
        val response = responseFrom(
            """{"error":{"code":"temporarily_unavailable","message":"Service is busy. AADSTS90001"}}"""
        )

        val result = parser.parseAuthorizeChallenge(
            response = response,
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.UnknownError)
        val error = result as AuthorizeChallengeApiResult.UnknownError
        assertEquals("temporarily_unavailable", error.error)
        assertEquals("Service is busy. AADSTS90001", error.errorDescription)
        assertEquals(listOf(90001), error.errorCodes)
    }

    @Test
    fun parseAuthorizeChallenge_whenContinuationTokenPresentButEntryLinkMissing_returnsUnknownError() {
        // A response carrying a continuation token (blank or not) with no matching entry-point
        // link is a malformed "success" payload: the server accepted the request but omitted the
        // href the caller needs to continue the flow.
        listOf("flow-token", "").forEach { token ->
            val response = responseFrom("""{"continuationToken":"$token"}""")

            val result = parser.parseAuthorizeChallenge(
                response = response,
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
                scopes = listOf("User.Read")
            )

            assertTrue(result is AuthorizeChallengeApiResult.UnknownError)
            val error = result as AuthorizeChallengeApiResult.UnknownError
            assertEquals(ApiErrorResult.INVALID_STATE, error.error)
            assertTrue(error.errorDescription.contains("resetPassword"))
        }
    }

    @Test
    fun parseAuthorizeChallenge_whenNeitherAuthorizationCodeNorContinuationTokenPresent_returnsUnknownError() {
        val response = responseFrom("""{}""")

        val result = parser.parseAuthorizeChallenge(
            response = response,
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.UnknownError)
        val error = result as AuthorizeChallengeApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("neither an authorization code nor a continuation token"))
    }

    // endregion

    // region parseInteraction - web fallback, continuation state, and missing-token

    @Test
    fun parseInteraction_whenWebFallbackRequired_returnsRedirect() {
        listOf(
            """{"error":{"code":"redirect_to_web","message":"Browser required."}}""" to "redirect_to_web",
            """{"state":"webFallbackRequired"}""" to "webFallbackRequired"
        ).forEach { (json, expectedReason) ->
            val result = parser.parseInteraction(responseFrom(json), previousState(), NativeAuthV2Operation.VERIFY)

            assertTrue(result is NativeAuthV2InteractionApiResult.Redirect)
            val redirect = result as NativeAuthV2InteractionApiResult.Redirect
            assertEquals(CORRELATION_ID, redirect.correlationId)
            assertEquals(expectedReason, redirect.redirectReason)
        }
    }

    @Test
    fun parseInteraction_whenStateIsContinueAndTokenPresent_returnsReadyToComplete() {
        val response = responseFrom("""{"state":"continue","continuationToken":"next-token"}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.VERIFY)

        assertTrue(result is NativeAuthV2InteractionApiResult.ReadyToComplete)
        val readyToComplete = result as NativeAuthV2InteractionApiResult.ReadyToComplete
        assertEquals("next-token", readyToComplete.continuationState.continuationToken)
    }

    @Test
    fun parseInteraction_whenStateIsContinueButTokenMissing_returnsUnknownError() {
        val response = responseFrom("""{"state":"continue"}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.VERIFY)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("continuation token"))
    }

    @Test
    fun parseInteraction_whenContinuationTokenMissingAndActionPresent_returnsUnknownError() {
        val response = responseFrom("""{"action":"challenge"}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.CHALLENGE)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("continuation token"))
    }

    // endregion

    // region parseInteraction - action = challenge

    @Test
    fun parseInteraction_whenActionIsChallenge_andLinkEmbedded_returnsChallengeRequiredWithEmbeddedHint() {
        val response = responseFrom(
            """
            {
              "continuationToken": "next-token",
              "action": "challenge",
              "hint": "top-level-hint",
              "_embedded": {
                "methods": [{
                  "hint": "embedded-hint",
                  "_links": {
                    "challenge": {"href": "/api/v0.1/auth/embedded/challenge"}
                  }
                }]
              }
            }
            """.trimIndent()
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.CHALLENGE
        ) as NativeAuthV2InteractionApiResult.ChallengeRequired

        assertEquals("embedded-hint", result.hint)
        assertEquals(
            "/api/v0.1/auth/embedded/challenge",
            result.continuationState.href(NativeAuthV2LinkRelation.CHALLENGE)
        )
    }

    @Test
    fun parseInteraction_whenActionIsChallenge_andLinkTopLevel_returnsChallengeRequiredWithTopLevelHint() {
        val response = responseFrom(
            """
            {
              "continuationToken": "next-token",
              "action": "challenge",
              "hint": "top-level-hint",
              "_links": {
                "challenge": {"href": "/api/v0.1/auth/top-level/challenge"}
              }
            }
            """.trimIndent()
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.CHALLENGE
        ) as NativeAuthV2InteractionApiResult.ChallengeRequired

        assertEquals("top-level-hint", result.hint)
        assertEquals(
            "/api/v0.1/auth/top-level/challenge",
            result.continuationState.href(NativeAuthV2LinkRelation.CHALLENGE)
        )
    }

    @Test
    fun parseInteraction_whenActionIsChallengeButLinkMissing_returnsUnknownError() {
        val response = responseFrom("""{"continuationToken":"next-token","action":"challenge"}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.CHALLENGE)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("challenge"))
    }

    // endregion

    // region parseInteraction - action = verify, missing link/field cases

    @Test
    fun parseInteraction_whenActionIsVerifyButLinkMissing_returnsUnknownError() {
        val response = responseFrom(
            """{"continuationToken":"next-token","action":"verify","codeLength":6,"hint":"h","type":"email"}"""
        )

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.VERIFY)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("verify"))
    }

    @Test
    fun parseInteraction_whenVerifyMissingRequiredField_returnsUnknownError() {
        listOf(
            // codeLength missing
            """{"continuationToken":"t","action":"verify","hint":"h","type":"email","_links":{"verify":{"href":"/x"}}}""" to "codeLength",
            // challengeTargetLabel (hint) missing
            """{"continuationToken":"t","action":"verify","codeLength":6,"type":"email","_links":{"verify":{"href":"/x"}}}""" to "challengeTargetLabel",
            // challengeChannel (type) missing
            """{"continuationToken":"t","action":"verify","codeLength":6,"hint":"h","_links":{"verify":{"href":"/x"}}}""" to "challengeChannel"
        ).forEach { (json, missingField) ->
            val result = parser.parseInteraction(responseFrom(json), previousState(), NativeAuthV2Operation.VERIFY)

            assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
            val error = result as NativeAuthV2InteractionApiResult.UnknownError
            assertEquals(ApiErrorResult.INVALID_STATE, error.error)
            assertTrue(error.errorDescription.contains(missingField))
        }
    }

    // endregion

    // region parseInteraction - action = update

    @Test
    fun parseInteraction_whenActionIsUpdate_returnsUpdateRequired() {
        listOf(
            """{"continuationToken":"t","action":"update","_links":{"update":{"href":"/u"}}}""",
            """{"continuationToken":"t","action":"update","_links":{"self":{"href":"/s"}}}"""
        ).forEach { json ->
            val result = parser.parseInteraction(responseFrom(json), previousState(), NativeAuthV2Operation.UPDATE_PASSWORD)

            assertTrue(result is NativeAuthV2InteractionApiResult.UpdateRequired)
        }
    }

    @Test
    fun parseInteraction_whenActionIsUpdateButNoUpdateOrSelfLink_returnsUnknownError() {
        val response = responseFrom("""{"continuationToken":"t","action":"update"}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.UPDATE_PASSWORD)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("update"))
    }

    // endregion

    // region parseInteraction - action = poll

    @Test
    fun parseInteraction_whenActionIsPoll_returnsPollInProgressWithNullRetryAfter() {
        val response = responseFrom("""{"continuationToken":"t","action":"poll","_links":{"poll":{"href":"/p"}}}""")

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.POLL
        ) as NativeAuthV2InteractionApiResult.PollInProgress

        assertNull(result.retryAfterMillis)
        assertEquals("/p", result.continuationState.href(NativeAuthV2LinkRelation.POLL))
    }

    @Test
    fun parseInteraction_whenActionIsPollButLinkMissing_returnsUnknownError() {
        val response = responseFrom("""{"continuationToken":"t","action":"poll"}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.POLL)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("poll"))
    }

    // endregion

    // region mapInteractionError - additional error relations

    @Test
    fun parseInteraction_whenResetPasswordStartUserNotFound_returnsUserNotFoundWithExtractedAadstsCode() {
        val response = responseFrom(
            """{"error":{"code":"invalid_grant","message":"AADSTS50034: User account does not exist."}}"""
        )

        val result = parser.parseInteraction(
            response,
            previousState(),
            NativeAuthV2Operation.RESET_PASSWORD_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UserNotFound)
        val userNotFound = result as NativeAuthV2InteractionApiResult.UserNotFound
        assertEquals("invalid_grant", userNotFound.error)
        assertEquals(listOf(50034), userNotFound.errorCodes)
    }

    @Test
    fun parseInteraction_whenUnknownServiceErrorHasNoMessage_returnsUnknownErrorWithNullErrorCodes() {
        // No inner error, no message: exercises the generic/"unknown service error" (e.g. transient
        // or rate-limit-like) fallback, and confirms a missing message yields null errorCodes rather
        // than an empty list.
        val response = responseFrom("""{"error":{"code":"service_unavailable"}}""")

        val result = parser.parseInteraction(response, previousState(), NativeAuthV2Operation.POLL)

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals("service_unavailable", error.error)
        assertEquals("", error.errorDescription)
        assertNull(error.errorCodes)
    }

    // endregion

    private fun responseFrom(json: String): NativeAuthV2HalApiResponse =
        NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(json),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )

    private fun previousState(): NativeAuthV2ContinuationState {
        val seedResponse = responseFrom("""{"continuationToken":"seed"}""")
        return NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = seedResponse,
            scopes = listOf("User.Read"),
            scenario = NativeAuthV2FlowScenario.RESET_PASSWORD
        )!!
    }

    private companion object {
        private const val CORRELATION_ID = "corr-123"
    }
}
