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
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the V2 sign-in additions to [NativeAuthV2ResponseParser]: first-factor and multi-factor
 * challenge interpretation, method validation, password verify readiness, and the sign-in error
 * table. SSPR behaviour is asserted separately in [NativeAuthV2ResponseParserTest].
 */
class NativeAuthV2SignInResponseParserTest {

    private val parser = NativeAuthV2ResponseParser()

    // region first-factor challenge

    @Test
    fun parseInteraction_whenSignInStartOffersSingleFactorMethods_returnsChallengeRequiredWithAllMethods() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "action": "challenge",
                  "challengeContext": { "authenticationFactor": "singleFactor" },
                  "_embedded": {
                    "methods": [
                      {
                        "id": "pwd-1",
                        "type": "Password",
                        "_links": { "challenge": { "href": "/tenant/password/pwd-1/challenge" } }
                      },
                      {
                        "id": "email-1",
                        "type": "EMAIL",
                        "hint": "u***@contoso.com",
                        "_links": { "challenge": { "href": "/tenant/email/email-1/challenge" } }
                      }
                    ]
                  }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.ChallengeRequired)
        val challenge = result as NativeAuthV2InteractionApiResult.ChallengeRequired

        assertEquals(
            listOf("pwd-1", "email-1"),
            challenge.methods.map { it.id }
        )
        // Server order is preserved and the type is normalized, so callers match without
        // repeating case handling.
        assertEquals(listOf("password", "email"), challenge.methods.map { it.type })
        assertEquals("u***@contoso.com", challenge.methods[1].hint)
    }

    @Test
    fun parseInteraction_whenSignInChallengeIsMultiFactor_returnsMFARequired() {
        val result = parser.parseInteraction(
            response = responseFrom(MULTI_FACTOR_EMAIL_CHALLENGE_JSON),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_PASSWORD_VERIFY
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.MFARequired)
        val mfa = result as NativeAuthV2InteractionApiResult.MFARequired
        assertEquals(listOf("email-1"), mfa.methods.map { it.id })
        assertEquals("email", mfa.methods.single().type)
    }

    @Test
    fun parseInteraction_whenDeferredPasswordProducesMultiFactorChallenge_returnsMFARequired() {
        val result = parser.parseInteraction(
            response = responseFrom(MULTI_FACTOR_EMAIL_CHALLENGE_JSON),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SUBMIT_PASSWORD
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.MFARequired)
    }

    @Test
    fun parseInteraction_whenMultiFactorCasingDiffers_returnsProtocolError() {
        assertInvalidAuthenticationFactor(
            MULTI_FACTOR_EMAIL_CHALLENGE_JSON.replace("multiFactor", "MULTIFACTOR"),
            NativeAuthV2Operation.SIGN_IN_PASSWORD_VERIFY
        )
    }

    @Test
    fun parseInteraction_whenSingleFactorCasingDiffers_returnsProtocolError() {
        assertInvalidAuthenticationFactor(
            SINGLE_FACTOR_PASSWORD_CHALLENGE_JSON.replace("singleFactor", "SINGLEFACTOR")
        )
    }

    @Test
    fun parseInteraction_whenAuthenticationFactorIsMissing_returnsProtocolError() {
        assertInvalidAuthenticationFactor(
            SINGLE_FACTOR_PASSWORD_CHALLENGE_JSON.replace(
                """"challengeContext": { "authenticationFactor": "singleFactor" },""",
                ""
            )
        )
    }

    @Test
    fun parseInteraction_whenAuthenticationFactorIsBlank_returnsProtocolError() {
        assertInvalidAuthenticationFactor(
            SINGLE_FACTOR_PASSWORD_CHALLENGE_JSON.replace("singleFactor", "  ")
        )
    }

    @Test
    fun parseInteraction_whenAuthenticationFactorIsMalformed_returnsProtocolError() {
        assertInvalidAuthenticationFactor(
            SINGLE_FACTOR_PASSWORD_CHALLENGE_JSON.replace(
                """"authenticationFactor": "singleFactor"""",
                """"authenticationFactor": 123"""
            )
        )
    }

    @Test
    fun parseInteraction_whenAuthenticationFactorIsUnknown_returnsProtocolError() {
        assertInvalidAuthenticationFactor(
            SINGLE_FACTOR_PASSWORD_CHALLENGE_JSON.replace("singleFactor", "biometricFactor")
        )
    }

    @Test
    fun parseInteraction_whenSelectedMethodHrefIsRetained_isFollowableFromContinuationState() {
        val result = parser.parseInteraction(
            response = responseFrom(SINGLE_FACTOR_PASSWORD_CHALLENGE_JSON),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        ) as NativeAuthV2InteractionApiResult.ChallengeRequired

        val selected = result.continuationState.withSelectedMethod("pwd-1")

        assertNotNull(selected)
        assertEquals(
            "/tenant/password/pwd-1/challenge",
            selected!!.href(NativeAuthV2LinkRelation.CHALLENGE)
        )
        // A stale or fabricated method ID must not silently fall back to another method's href.
        assertNull(result.continuationState.withSelectedMethod("not-a-method"))
    }

    @Test
    fun parseInteraction_whenChallengeRequiredIsLogged_neverExposesHrefsOrContinuationToken() {
        val result = parser.parseInteraction(
            response = responseFrom(SINGLE_FACTOR_PASSWORD_CHALLENGE_JSON),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        listOf(result.toString(), result.toUnsanitizedString()).forEach { rendered ->
            assertFalse(rendered.contains("ct-1"))
            assertFalse(rendered.contains("/tenant/password"))
        }
    }

    @Test
    fun parseInteraction_whenChallengeRequiredIsLogged_omitsHintFromSanitizedString() {
        val result = parser.parseInteraction(
            response = responseFrom(MULTI_FACTOR_EMAIL_CHALLENGE_JSON),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_PASSWORD_VERIFY
        )

        assertFalse(result.toString().contains("u***@contoso.com"))
        assertTrue(result.toUnsanitizedString().contains("u***@contoso.com"))
    }

    // endregion

    // region method validation

    @Test
    fun parseInteraction_whenSignInChallengeHasNoMethods_returnsDeterministicProtocolError() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """{"continuationToken":"ct-1","action":"challenge"}"""
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertInvalidState(result, "offered no authentication methods")
    }

    @Test
    fun parseInteraction_whenMethodIsMissingId_returnsDeterministicProtocolError() {
        val result = parser.parseInteraction(
            response = responseFrom(
                methodChallengeJson(
                    """{"type":"password","_links":{"challenge":{"href":"/tenant/password/challenge"}}}"""
                )
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertInvalidState(result, "missing required field 'id'")
    }

    @Test
    fun parseInteraction_whenMethodIsMissingType_returnsDeterministicProtocolError() {
        val result = parser.parseInteraction(
            response = responseFrom(
                methodChallengeJson(
                    """{"id":"pwd-1","_links":{"challenge":{"href":"/tenant/password/challenge"}}}"""
                )
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertInvalidState(result, "missing required field 'type'")
    }

    @Test
    fun parseInteraction_whenMethodIsMissingChallengeLink_returnsDeterministicProtocolError() {
        val result = parser.parseInteraction(
            response = responseFrom(
                methodChallengeJson("""{"id":"pwd-1","type":"password"}""")
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertInvalidState(result, "missing required link relation 'challenge'")
    }

    @Test
    fun parseInteraction_whenSignInChallengeHasNoContinuationToken_returnsProtocolError() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "action": "challenge",
                  "_embedded": {
                    "methods": [
                      {
                        "id": "pwd-1",
                        "type": "password",
                        "_links": { "challenge": { "href": "/tenant/password/pwd-1/challenge" } }
                      }
                    ]
                  }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertInvalidState(result, "missing a continuation token")
    }

    // endregion

    // region password verify readiness

    @Test
    fun parseInteraction_whenPasswordChallengeReturnsVerifyLink_returnsPasswordRequired() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-2",
                  "action": "verify",
                  "_links": { "verify": { "href": "/tenant/password/pwd-1/verify" } }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_PASSWORD_CHALLENGE
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.PasswordRequired)
        val passwordRequired = result as NativeAuthV2InteractionApiResult.PasswordRequired
        assertEquals(
            "/tenant/password/pwd-1/verify",
            passwordRequired.continuationState.href(NativeAuthV2LinkRelation.VERIFY)
        )
    }

    @Test
    fun parseInteraction_whenPasswordChallengeHasNoVerifyLink_returnsProtocolError() {
        val result = parser.parseInteraction(
            response = responseFrom("""{"continuationToken":"ct-2","action":"verify"}"""),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_PASSWORD_CHALLENGE
        )

        assertInvalidState(result, "missing required link relation 'verify'")
    }

    @Test
    fun parseInteraction_whenVerifyActionArrivesForAnUnexpectedSignInOperation_returnsProtocolError() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """{"continuationToken":"ct-2","action":"verify","_links":{"verify":{"href":"/v"}}}"""
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertInvalidState(result, "SIGN_IN_START")
    }

    @Test
    fun parseInteraction_whenMfaMethodChallengeReturnsCodeContract_returnsCodeRequired() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-3",
                  "action": "verify",
                  "type": "email",
                  "hint": "u***@contoso.com",
                  "codeLength": 6,
                  "_links": { "verify": { "href": "/tenant/email/email-1/verify" } }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.MFA_METHOD_CHALLENGE
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.CodeRequired)
        val code = result as NativeAuthV2InteractionApiResult.CodeRequired
        assertEquals(6, code.codeLength)
        assertEquals("email", code.challengeChannel)
        assertEquals("u***@contoso.com", code.challengeTargetLabel)
    }

    @Test
    fun parseInteraction_whenSignInReachesContinueState_returnsReadyToComplete() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """{"continuationToken":"ct-4","state":"continue","_links":{"continue":{"href":"/c"}}}"""
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_PASSWORD_VERIFY
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.ReadyToComplete)
    }

    // endregion

    // region error mapping

    @Test
    fun parseInteraction_whenSignInStartReportsUnknownUser_returnsUserNotFoundWithAadstsCode() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "error": {
                    "code": "invalid_grant",
                    "message": "AADSTS50034: The user account does not exist in the directory.",
                    "correlationId": "server-corr"
                  }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UserNotFound)
        val notFound = result as NativeAuthV2InteractionApiResult.UserNotFound
        assertEquals(listOf(50034), notFound.errorCodes)
        assertEquals("server-corr", notFound.correlationId)
    }

    @Test
    fun parseInteraction_whenEntrySuppliedPasswordIsRejected_returnsInvalidCredentialsNotDeferred() {
        val result = parser.parseInteraction(
            response = responseFrom(INVALID_USERNAME_OR_PASSWORD_JSON),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_PASSWORD_VERIFY
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.InvalidCredentials)
        val invalid = result as NativeAuthV2InteractionApiResult.InvalidCredentials
        assertFalse(invalid.deferredSubmission)
        assertEquals("invalidUserNameOrPassword", invalid.subError)
        assertEquals(listOf(50126), invalid.errorCodes)
        assertEquals("server-corr", invalid.correlationId)
    }

    @Test
    fun parseInteraction_whenDeferredPasswordIsRejected_returnsInvalidCredentialsMarkedDeferred() {
        val result = parser.parseInteraction(
            response = responseFrom(INVALID_USERNAME_OR_PASSWORD_JSON),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SUBMIT_PASSWORD
        )

        val invalid = result as NativeAuthV2InteractionApiResult.InvalidCredentials
        assertTrue(invalid.deferredSubmission)
    }

    @Test
    fun parseInteraction_whenOnlyAadstsCodeIdentifiesBadCredentials_stillReturnsInvalidCredentials() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "error": {
                    "code": "invalid_grant",
                    "message": "AADSTS50126: Error validating credentials due to invalid username or password."
                  }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_PASSWORD_VERIFY
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.InvalidCredentials)
    }

    @Test
    fun parseInteraction_whenCredentialsRejectedOutsidePasswordSubmission_staysUnknownError() {
        // Preserves the pre-existing SSPR behaviour: a credential rejection at a step where the app
        // did not submit a password is not actionable and must not become InvalidCredentials.
        val result = parser.parseInteraction(
            response = responseFrom(INVALID_USERNAME_OR_PASSWORD_JSON),
            previousState = signInState(),
            operation = NativeAuthV2Operation.RESET_PASSWORD_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
    }

    @Test
    fun parseInteraction_whenMfaCodeIsWrong_returnsInvalidCode() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "error": {
                    "code": "invalidGrant",
                    "message": "AADSTS50184: The one-time code is invalid.",
                    "innerError": { "code": "invalidOneTimeCode" }
                  }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.MFA_VERIFY
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.InvalidCode)
        assertEquals(
            "invalidOneTimeCode",
            (result as NativeAuthV2InteractionApiResult.InvalidCode).subError
        )
    }

    @Test
    fun parseInteraction_whenAuthMethodIsBlocked_returnsAuthMethodBlocked() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "error": {
                    "code": "accessDenied",
                    "message": "The authentication method is blocked.",
                    "innerError": { "code": "providerBlockedByRep" }
                  }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.MFA_METHOD_CHALLENGE
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.AuthMethodBlocked)
    }

    @Test
    fun parseInteraction_whenSignInResponseRequiresWebFallback_redirectWinsOverEverythingElse() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "action": "challenge",
                  "error": { "code": "redirect_to_web", "message": "Use the browser." }
                }
                """.trimIndent()
            ),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.Redirect)
        assertEquals(
            "redirect_to_web",
            (result as NativeAuthV2InteractionApiResult.Redirect).redirectReason
        )
    }

    @Test
    fun parseInteraction_whenSignInActionIsUnknown_returnsUnsupportedAction() {
        val result = parser.parseInteraction(
            response = responseFrom("""{"continuationToken":"ct-1","action":"teleport"}"""),
            previousState = signInState(),
            operation = NativeAuthV2Operation.SIGN_IN_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UnsupportedAction)
        assertEquals(
            "teleport",
            (result as NativeAuthV2InteractionApiResult.UnsupportedAction).rawAction
        )
    }

    // endregion

    private fun assertInvalidState(
        result: NativeAuthV2InteractionApiResult,
        expectedDescriptionFragment: String
    ) {
        assertTrue(
            "Expected UnknownError but was $result",
            result is NativeAuthV2InteractionApiResult.UnknownError
        )
        val error = result as NativeAuthV2InteractionApiResult.UnknownError
        assertEquals(ApiErrorResult.INVALID_STATE, error.error)
        assertTrue(
            "Expected '$expectedDescriptionFragment' in '${error.errorDescription}'",
            error.errorDescription.contains(expectedDescriptionFragment)
        )
    }

    private fun assertInvalidAuthenticationFactor(
        json: String,
        operation: NativeAuthV2Operation = NativeAuthV2Operation.SIGN_IN_START
    ) {
        val result = parser.parseInteraction(
            response = responseFrom(json),
            previousState = signInState(),
            operation = operation
        )

        assertInvalidState(result, "invalid value for field 'authenticationFactor'")
    }

    private fun responseFrom(json: String): NativeAuthV2HalApiResponse =
        NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(json),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )

    private fun signInState(): NativeAuthV2ContinuationState =
        NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = responseFrom("""{"continuationToken":"seed","signIn":"/tenant/signin/start"}"""),
            continuationToken = "seed",
            entryRelation = NativeAuthV2LinkRelation.SIGN_IN,
            scopes = listOf("User.Read"),
            scenario = NativeAuthV2FlowScenario.SIGN_IN
        )

    private fun methodChallengeJson(methodJson: String): String =
        """
        {
          "continuationToken": "ct-1",
          "action": "challenge",
          "_embedded": { "methods": [ $methodJson ] }
        }
        """.trimIndent()

    private companion object {
        private const val CORRELATION_ID = "corr-123"

        private val SINGLE_FACTOR_PASSWORD_CHALLENGE_JSON = """
            {
              "continuationToken": "ct-1",
              "action": "challenge",
              "challengeContext": { "authenticationFactor": "singleFactor" },
              "_embedded": {
                "methods": [
                  {
                    "id": "pwd-1",
                    "type": "password",
                    "_links": { "challenge": { "href": "/tenant/password/pwd-1/challenge" } }
                  }
                ]
              }
            }
        """.trimIndent()

        private val MULTI_FACTOR_EMAIL_CHALLENGE_JSON = """
            {
              "continuationToken": "ct-2",
              "action": "challenge",
              "challengeContext": { "authenticationFactor": "multiFactor" },
              "_embedded": {
                "methods": [
                  {
                    "id": "email-1",
                    "type": "email",
                    "hint": "u***@contoso.com",
                    "_links": { "challenge": { "href": "/tenant/email/email-1/challenge" } }
                  }
                ]
              }
            }
        """.trimIndent()

        private val INVALID_USERNAME_OR_PASSWORD_JSON = """
            {
              "error": {
                "code": "invalid_grant",
                "message": "AADSTS50126: Error validating credentials due to invalid username or password.",
                "innerError": { "code": "invalidUserNameOrPassword" },
                "correlationId": "server-corr"
              }
            }
        """.trimIndent()
    }
}
