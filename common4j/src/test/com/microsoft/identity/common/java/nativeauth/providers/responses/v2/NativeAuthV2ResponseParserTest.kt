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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2ResponseParserTest {
    private val parser = NativeAuthV2ResponseParser()

    @Test
    fun parseAuthorizeChallenge_whenEntryLinksAreFlatSnakeCaseProperties_returnsContinuationRequired() {
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
                scopes = listOf("User.Read")
            )

            assertTrue(result is AuthorizeChallengeApiResult.Redirect)
            val redirect = result as AuthorizeChallengeApiResult.Redirect
            assertEquals(CORRELATION_ID, redirect.correlationId)
            assertEquals(expectedReason, redirect.redirectReason)
        }
    }

    @Test
    fun parseAuthorizeChallenge_whenAuthorizationCodePresent_returnsAuthorizationCode() {
        val result = parser.parseAuthorizeChallenge(
            response = responseFrom("""{"code":"auth-code-123"}"""),
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.AuthorizationCode)
        val authorizationCode = result as AuthorizeChallengeApiResult.AuthorizationCode
        assertEquals(CORRELATION_ID, authorizationCode.correlationId)
        assertEquals("auth-code-123", authorizationCode.code)
    }

    @Test
    fun parseAuthorizeChallenge_whenAuthorizationCodeIsBlank_returnsUnknownError() {
        listOf("", "   ").forEach { code ->
            assertInvalidState("""{"code":"$code"}""")
        }
    }

    @Test
    fun parseAuthorizeChallenge_whenAuthorizationCodeIsBlankAndContinuationIsValid_returnsContinuationRequired() {
        val result = parser.parseAuthorizeChallenge(
            response = responseFrom(
                """
                {
                  "code": "   ",
                  "continuation_token": "flow-token",
                  "reset_password": "/tenant/api/v0.1/auth/resetpassword"
                }
                """.trimIndent()
            ),
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.ContinuationRequired)
    }

    @Test
    fun parseAuthorizeChallenge_whenCodeAndContinuationTokenPresent_prefersAuthorizationCode() {
        val result = parser.parseAuthorizeChallenge(
            response = responseFrom(
                """
                {
                  "code": "auth-code-123",
                  "continuation_token": "flow-token",
                  "reset_password": "/tenant/api/v0.1/auth/resetpassword"
                }
                """.trimIndent()
            ),
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.AuthorizationCode)
        assertEquals(
            "auth-code-123",
            (result as AuthorizeChallengeApiResult.AuthorizationCode).code
        )
    }

    @Test
    fun parseAuthorizeChallenge_whenServerErrorPresent_returnsUnknownErrorWithExtractedAadstsCodes() {
        val result = parser.parseAuthorizeChallenge(
            response = responseFrom(
                """{"error":{"code":"temporarily_unavailable","message":"Service is busy. AADSTS90001"}}"""
            ),
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
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
        val result = parser.parseAuthorizeChallenge(
            response = responseFrom("""{"continuationToken":"flow-token"}"""),
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.UnknownError)
        val error = result as AuthorizeChallengeApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(error.errorDescription.contains("resetPassword"))
    }

    @Test
    fun parseAuthorizeChallenge_whenContinuationTokenIsBlankAndEntryLinkPresent_returnsUnknownError() {
        listOf("", "   ").forEach { token ->
            assertInvalidState(
                """
                {
                  "continuation_token": "$token",
                  "reset_password": "/tenant/api/v0.1/auth/resetpassword"
                }
                """.trimIndent()
            )
        }
    }

    @Test
    fun parseAuthorizeChallenge_whenEntryLinkIsBlank_returnsUnknownError() {
        assertInvalidState(
            """
            {
              "continuation_token": "flow-token",
              "_links": {
                "resetPassword": {
                  "href": "   "
                }
              }
            }
            """.trimIndent()
        )
    }

    @Test
    fun parseAuthorizeChallenge_whenNeitherAuthorizationCodeNorContinuationTokenPresent_returnsUnknownError() {
        val result = parser.parseAuthorizeChallenge(
            response = responseFrom("""{}"""),
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scopes = listOf("User.Read")
        )

        assertTrue(result is AuthorizeChallengeApiResult.UnknownError)
        val error = result as AuthorizeChallengeApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(
            error.errorDescription.contains(
                "neither an authorization code nor a continuation token"
            )
        )
    }

    private fun responseFrom(json: String): NativeAuthV2HalApiResponse =
        NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(json),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )

    private fun assertInvalidState(json: String) {
        val parsed = runCatching {
            parser.parseAuthorizeChallenge(
                response = responseFrom(json),
                entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
                scopes = listOf("User.Read")
            )
        }

        assertTrue(
            "Parser should return UnknownError instead of throwing ${parsed.exceptionOrNull()}",
            parsed.isSuccess
        )
        val result = parsed.getOrThrow()
        assertTrue(result is AuthorizeChallengeApiResult.UnknownError)
        assertEquals(
            ApiErrorResult.INVALID_STATE,
            (result as AuthorizeChallengeApiResult.UnknownError).error
        )
    }

    private companion object {
        private const val CORRELATION_ID = "corr-123"
    }
}
