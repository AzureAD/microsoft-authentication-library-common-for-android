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
package com.microsoft.identity.common.nativeauth.internal.controllers.v2

import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SelectAuthMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitMFAChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationStateTestFactory
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the V2 sign-in orchestration added to [NativeAuthV2FlowController]: email-code
 * and password first-factor selection, deferred credential submission, MFA
 * transitions, and the error mapping each step must apply.
 *
 * [NativeAuthV2OAuth2Strategy] and [NativeAuthCIAMAuthority] are mocked so these tests exercise the
 * controller's branching only, never the HTTP or cache layers.
 */
class NativeAuthV2SignInFlowControllerTest {

    private val correlationId = "test-correlation-id"

    private lateinit var mockAuthority: NativeAuthCIAMAuthority
    private lateinit var mockPlatformComponents: IPlatformComponents
    private lateinit var mockStrategy: NativeAuthV2OAuth2Strategy
    private lateinit var controller: NativeAuthV2FlowController

    @Before
    fun setup() {
        mockStrategy = mockk(relaxed = true)
        mockPlatformComponents = mockk(relaxed = true)
        mockAuthority = mockk(relaxed = true)
        every { mockAuthority.createOAuth2StrategyV2(any()) } returns mockStrategy

        controller = NativeAuthV2FlowController()
    }

    // -----------------------------------------------------------------------------------------
    // signInStart
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSignInStartReturnsPasswordRequiredWithoutSubmittingOrCompleting() {
        val passwordState = mockContinuationState()
        stubStartThroughPasswordChallenge(passwordState)

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.PasswordRequired)
        assertEquals(
            passwordState,
            (result as NativeAuthV2CommandResult.PasswordRequired).continuationState
        )
        verify(exactly = 0) {
            mockStrategy.performPasswordVerify(any(), any())
            mockStrategy.performAuthorizeChallengeContinue(any())
            mockStrategy.performTokenRequest(any(), any(), any(), any())
        }
    }

    @Test
    fun testSignInStartOffersMultipleMethodsInServerOrderWithoutSendingChallenge() {
        val challengeState = mockContinuationState()
        val methods = listOf(
            NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"),
            NativeAuthV2AuthMethod("pwd-1", "password", null),
            NativeAuthV2AuthMethod("sms-1", "sms", null)
        )
        stubAuthorizeChallengeStart()
        for (offered in listOf(methods, methods.reversed())) {
            every { mockStrategy.performSignInStart(any(), any()) } returns
                NativeAuthV2InteractionApiResult.ChallengeRequired(
                    correlationId, challengeState, null, offered
                )

            val result = controller.signInStart(signInStartParameters())

            assertTrue(result is NativeAuthV2CommandResult.AuthMethodSelectionRequired)
            result as NativeAuthV2CommandResult.AuthMethodSelectionRequired
            assertEquals(offered, result.authMethods)
            assertSame(challengeState, result.continuationState)
        }
        verify(exactly = 0) {
            mockStrategy.performMethodChallenge(any(), any())
            mockStrategy.performPasswordVerify(any(), any())
        }
    }

    @Test
    fun testSignInStartThenSelectMethodChallengesOnlyExplicitSelection() {
        val challengeState = mockContinuationState()
        val verificationState = mockContinuationState()
        every { challengeState.flowScenario() } returns NativeAuthV2FlowScenario.SIGN_IN
        val methods = listOf(
            NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"),
            NativeAuthV2AuthMethod("pwd-1", "password", null)
        )
        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(correlationId, challengeState, null, methods)
        every { mockStrategy.performMethodChallenge(challengeState, "pwd-1") } returns
            NativeAuthV2InteractionApiResult.PasswordRequired(correlationId, verificationState)
        every { mockStrategy.performMethodChallenge(challengeState, "email-1") } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = verificationState,
                challengeTargetLabel = "u***@contoso.com",
                challengeChannel = "email",
                codeLength = 6
            )

        for (method in methods) {
            val startResult = controller.signInStart(signInStartParameters())
            assertTrue(startResult is NativeAuthV2CommandResult.AuthMethodSelectionRequired)
            val result = controller.selectAuthMethod(
                NativeAuthV2SelectAuthMethodCommandParameters.builder()
                    .authority(mockAuthority)
                    .platformComponents(mockPlatformComponents)
                    .correlationId(correlationId)
                    .scopes(emptyList())
                    .continuationState((startResult as NativeAuthV2CommandResult.AuthMethodSelectionRequired).continuationState)
                    .methodId(method.id)
                    .build()
            )
            if (method.type == "password") {
                assertTrue(result is NativeAuthV2CommandResult.ExistingAccountPasswordRequired)
                assertSame(verificationState, (result as NativeAuthV2CommandResult.ExistingAccountPasswordRequired).continuationState)
            } else {
                assertTrue(result is NativeAuthV2CommandResult.ExistingAccountCodeRequired)
                assertSame(verificationState, (result as NativeAuthV2CommandResult.ExistingAccountCodeRequired).continuationState)
            }
            verify(exactly = 1) { mockStrategy.performMethodChallenge(challengeState, method.id) }
        }
        verify(exactly = 0) { mockStrategy.performPasswordVerify(any(), any()) }
    }

    @Test
    fun testSignInStartIgnoresUnknownMethodAndChallengesOnlyRecognizedMethod() {
        val challengeState = mockContinuationState()
        val codeState = mockContinuationState()

        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                hint = null,
                methods = listOf(
                    NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"),
                    NativeAuthV2AuthMethod("unknown-1", "unknown", null)
                )
            )
        every { mockStrategy.performMethodChallenge(challengeState, "email-1") } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = codeState,
                challengeTargetLabel = "u***@contoso.com",
                challengeChannel = "email",
                codeLength = 8
            )

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.CodeRequired)
        assertEquals(8, (result as NativeAuthV2CommandResult.CodeRequired).codeLength)
        verify(exactly = 1) {
            mockStrategy.performMethodChallenge(challengeState, "email-1")
        }
        verify(exactly = 0) {
            mockStrategy.performPasswordVerify(any(), any())
        }
    }

    @Test
    fun testSignInStartReturnsCodeRequiredWhenOnlyEmailFirstFactorIsOffered() {
        val challengeState = mockContinuationState()
        val codeState = mockContinuationState()

        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                hint = null,
                methods = listOf(NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"))
            )
        every { mockStrategy.performMethodChallenge(challengeState, "email-1") } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = codeState,
                challengeTargetLabel = "u***@contoso.com",
                challengeChannel = "email",
                codeLength = 8
            )

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.CodeRequired)
        result as NativeAuthV2CommandResult.CodeRequired
        assertEquals(codeState, result.continuationState)
        assertEquals("email", result.challengeChannel)
        assertEquals("u***@contoso.com", result.challengeTargetLabel)
        verify(exactly = 1) { mockStrategy.performMethodChallenge(challengeState, "email-1") }
    }

    @Test
    fun testSignInStartDoesNotExposeSmsCodeAsSupportedPrimarySignIn() {
        val challengeState = mockContinuationState()
        val codeState = mockContinuationState()

        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                hint = null,
                methods = listOf(NativeAuthV2AuthMethod("sms-1", "sms", "***1234"))
            )
        every { mockStrategy.performMethodChallenge(challengeState, "sms-1") } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = codeState,
                challengeTargetLabel = "***1234",
                challengeChannel = "sms",
                codeLength = 8
            )

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is INativeAuthCommandResult.APIError)
        verify(exactly = 1) { mockStrategy.performMethodChallenge(challengeState, "sms-1") }
        verify(exactly = 0) { mockStrategy.performPasswordVerify(any(), any()) }
    }

    @Test
    fun testSignInStartMapsUnknownUserToUserNotFound() {
        val details = listOf(mapOf("code" to "userNotFound", "attributeIds" to listOf("email")))
        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.UserNotFound(
                correlationId = correlationId,
                error = "invalid_grant",
                errorDescription = "AADSTS50034: user not found.",
                errorCodes = listOf(50034),
                subError = "userNotFound",
                attributeValidationDetails = details
            )

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.UserNotFound)
        assertEquals(listOf(50034), (result as NativeAuthV2CommandResult.UserNotFound).errorCodes)
        assertEquals("userNotFound", result.subError)
        assertEquals(details, result.attributeValidationDetails)
    }

    @Test
    fun testSignInStartThenSubmitPasswordReturnsMFAWithoutSendingMfaChallenge() {
        val passwordState = mockContinuationState()
        val mfaState = mockContinuationState()
        val methods = listOf(NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"))
        stubStartThroughPasswordChallenge(passwordState)

        every {
            mockStrategy.performPasswordVerify(passwordState, any())
        } returns NativeAuthV2InteractionApiResult.ChallengeRequired(correlationId, mfaState, null, methods)

        val startResult = controller.signInStart(signInStartParameters())
        assertTrue(startResult is NativeAuthV2CommandResult.PasswordRequired)
        verify(exactly = 0) { mockStrategy.performPasswordVerify(any(), any()) }
        val result = controller.submitPassword(
            submitPasswordParameters((startResult as NativeAuthV2CommandResult.PasswordRequired).continuationState)
        )

        assertTrue(result is NativeAuthV2CommandResult.AuthMethodSelectionRequired)
        val mfa = result as NativeAuthV2CommandResult.AuthMethodSelectionRequired
        assertEquals(methods, mfa.authMethods)
        assertEquals(mfaState, mfa.continuationState)
        verify(exactly = 0) { mockStrategy.performMethodChallenge(mfaState, "email-1") }
    }

    @Test
    fun testSignInStartFailsWhenNoRecognizedMethodIsOffered() {
        val challengeState = mockContinuationState()

        stubAuthorizeChallengeStart()
        for (methods in listOf(emptyList(), listOf(NativeAuthV2AuthMethod("unknown-1", "unknown", null)))) {
            every { mockStrategy.performSignInStart(any(), any()) } returns
                NativeAuthV2InteractionApiResult.ChallengeRequired(
                    correlationId, challengeState, null, methods
                )
            val result = controller.signInStart(signInStartParameters())

            assertTrue(result is INativeAuthCommandResult.APIError)
            assertEquals("unsupported_first_factor", (result as INativeAuthCommandResult.APIError).error)
        }
        verify(exactly = 0) { mockStrategy.performMethodChallenge(any(), any()) }
    }

    @Test
    fun testSignInStartMapsBrowserRequiredToRedirect() {
        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.Redirect(correlationId, "redirect_to_web")

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is INativeAuthCommandResult.Redirect)
        assertEquals(
            "redirect_to_web",
            (result as INativeAuthCommandResult.Redirect).redirectReason
        )
    }

    @Test
    fun testSignInStartPreservesAuthorizeChallengeRedirectWithoutStartingSignIn() {
        val response = AuthorizeChallengeApiResult.Redirect(
            correlationId = "server-correlation-id",
            redirectReason = "redirect_to_web",
            errorCodes = listOf(900001),
            traceId = "server-trace-id",
            timestamp = "2026-11-04T00:00:00Z"
        )
        every {
            mockStrategy.performAuthorizeChallengeStart(any(), any(), any(), any(), any())
        } returns response

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is INativeAuthCommandResult.Redirect)
        result as INativeAuthCommandResult.Redirect
        assertEquals(response.correlationId, result.correlationId)
        assertEquals(response.redirectReason, result.redirectReason)
        assertEquals(response.errorCodes, result.errorCodes)
        assertEquals(response.traceId, result.traceId)
        assertEquals(response.timestamp, result.timestamp)
        verify(exactly = 0) {
            mockStrategy.performSignInStart(any(), any())
            mockStrategy.performMethodChallenge(any(), any())
            mockStrategy.performTokenRequest(any(), any(), any(), any())
        }
    }

    @Test
    fun testSignInStartRejectsPrematureAuthorizationCodeWithoutExchangingIt() {
        every {
            mockStrategy.performAuthorizeChallengeStart(any(), any(), any(), any(), any())
        } returns AuthorizeChallengeApiResult.AuthorizationCode(
            "server-correlation-id", "unexpected-auth-code"
        )

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals("unexpected_api_result", result.error)
        assertEquals("server-correlation-id", result.correlationId)
        verify(exactly = 0) {
            mockStrategy.performSignInStart(any(), any())
            mockStrategy.performMethodChallenge(any(), any())
            mockStrategy.performAuthorizeChallengeContinue(any())
            mockStrategy.performTokenRequest(any(), any(), any(), any())
        }
    }

    @Test
    fun testSignInStartPreservesAuthorizeChallengeErrorWithoutStartingSignIn() {
        val response = AuthorizeChallengeApiResult.UnknownError(
            correlationId = "server-correlation-id",
            error = "invalid_request",
            errorDescription = "The sign-in request could not be processed.",
            subError = "invalid_state",
            errorCodes = listOf(900001),
            traceId = "server-trace-id",
            timestamp = "2026-11-04T00:00:00Z",
            attributeValidationDetails = listOf(mapOf("name" to "username", "reason" to "invalid"))
        )
        every {
            mockStrategy.performAuthorizeChallengeStart(any(), any(), any(), any(), any())
        } returns response

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals(response.correlationId, result.correlationId)
        assertEquals(response.error, result.error)
        assertEquals(response.errorDescription, result.errorDescription)
        assertEquals(response.subError, result.subError)
        assertEquals(response.errorCodes, result.errorCodes)
        assertEquals(response.traceId, result.traceId)
        assertEquals(response.timestamp, result.timestamp)
        assertEquals(response.attributeValidationDetails, result.attributeValidationDetails)
        verify(exactly = 0) {
            mockStrategy.performSignInStart(any(), any())
            mockStrategy.performMethodChallenge(any(), any())
            mockStrategy.performTokenRequest(any(), any(), any(), any())
        }
    }

    @Test
    fun testSignInStartPreservesMethodChallengeRedirectWithoutSubmittingPassword() {
        stubStartThroughPasswordChallenge(mockContinuationState())
        val response = NativeAuthV2InteractionApiResult.Redirect(
            correlationId = "server-correlation-id",
            redirectReason = "redirect_to_web",
            errorCodes = listOf(900001),
            traceId = "server-trace-id",
            timestamp = "2026-11-04T00:00:00Z"
        )
        every { mockStrategy.performMethodChallenge(any(), "pwd-1") } returns response

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is INativeAuthCommandResult.Redirect)
        result as INativeAuthCommandResult.Redirect
        assertEquals(response.correlationId, result.correlationId)
        assertEquals(response.redirectReason, result.redirectReason)
        assertEquals(response.errorCodes, result.errorCodes)
        assertEquals(response.traceId, result.traceId)
        assertEquals(response.timestamp, result.timestamp)
        verify(exactly = 1) { mockStrategy.performMethodChallenge(any(), "pwd-1") }
        verify(exactly = 0) {
            mockStrategy.performPasswordVerify(any(), any())
            mockStrategy.performAuthorizeChallengeContinue(any())
            mockStrategy.performTokenRequest(any(), any(), any(), any())
        }
    }

    @Test
    fun testSignInStartCarriesScopesAndClaimsIntoAuthorizeChallengeAndToken() {
        val passwordState = mockContinuationState()
        val requestedScopes = listOf("OPENID", "PROFILE", "OFFLINE_ACCESS", "User.Read", "user.read", "User.Read", "openid", "profile", "offline_access")
        val expectedScopes = listOf("openid", "profile", "offline_access", "User.Read", "user.read", "User.Read")
        val claims = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
        val readyState = mockContinuationState(scopes = expectedScopes, claimsRequestJson = claims)
        val tokenScopes = slot<List<String>>()
        stubStartThroughPasswordChallenge(passwordState)

        every {
            mockStrategy.performPasswordVerify(passwordState, any())
        } returns NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)
        every { mockStrategy.performAuthorizeChallengeContinue(readyState) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), capture(tokenScopes), any(), any())
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "invalid_grant",
            errorDescription = "Expected test stop before cache save.",
            errorCodes = emptyList()
        )

        val startResult = controller.signInStart(
            signInStartParameters(scopes = requestedScopes, claimsRequestJson = claims)
        )
        assertTrue(startResult is NativeAuthV2CommandResult.PasswordRequired)
        verify(exactly = 0) { mockStrategy.performTokenRequest(any(), any(), any(), any()) }
        controller.submitPassword(
            submitPasswordParameters((startResult as NativeAuthV2CommandResult.PasswordRequired).continuationState)
        )

        verify(exactly = 1) {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = correlationId,
                entryRelation = "signIn",
                scenario = any(),
                scopes = expectedScopes,
                claimsRequestJson = claims
            )
        }
        verify(exactly = 1) {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                scopes = any(),
                correlationId = correlationId,
                claimsRequestJson = claims
            )
        }
        assertEquals(expectedScopes, tokenScopes.captured)
    }

    // -----------------------------------------------------------------------------------------
    // submitCode
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSubmitFirstFactorCodeCompletesSignInThroughAuthorizeChallengeContinue() {
        val state = mockContinuationState()
        val readyState = mockContinuationState()

        every { mockStrategy.performVerify(state, "12345678") } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)
        every { mockStrategy.performAuthorizeChallengeContinue(readyState) } returns
            AuthorizeChallengeApiResult.UnknownError(
                correlationId = correlationId,
                error = "expected_test_stop",
                errorDescription = "Expected test stop before cache save."
            )

        val result = controller.submitSignInCode(submitCodeParameters(state, "12345678"))

        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals("expected_test_stop", (result as INativeAuthCommandResult.APIError).error)
        verify(exactly = 1) { mockStrategy.performAuthorizeChallengeContinue(readyState) }
    }

    @Test
    fun testSubmitFirstFactorCodeReturnsMFARequiredLikePasswordVerification() {
        val state = mockContinuationState()
        val mfaState = mockContinuationState()
        val methods = listOf(NativeAuthV2AuthMethod("email-2", "email", "u***@contoso.com"))

        every { mockStrategy.performVerify(state, "12345678") } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(correlationId, mfaState, null, methods)

        val result = controller.submitSignInCode(submitCodeParameters(state, "12345678"))

        assertTrue(result is NativeAuthV2CommandResult.AuthMethodSelectionRequired)
        result as NativeAuthV2CommandResult.AuthMethodSelectionRequired
        assertEquals(mfaState, result.continuationState)
        assertEquals(methods, result.authMethods)
    }

    @Test
    fun testSubmitFirstFactorCodeCompletesWithScopesAndClaimsFromContinuationState() {
        val state = mockContinuationState()
        val retainedClaims = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
        val readyState = mockContinuationState(
            scopes = listOf("User.Read"),
            claimsRequestJson = retainedClaims,
        )
        val tokenScopes = slot<List<String>>()

        every { mockStrategy.performVerify(state, "12345678") } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)
        every { mockStrategy.performAuthorizeChallengeContinue(readyState) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), capture(tokenScopes), any(), any())
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "expected_test_stop",
            errorDescription = "Expected test stop before cache save.",
            errorCodes = emptyList()
        )

        controller.submitSignInCode(
            submitCodeParameters(
                state = state,
                code = "12345678",
                scopes = listOf("Mail.Read"),
                claimsRequestJson = """{"id_token":{"email":null}}"""
            )
        )

        assertTrue(tokenScopes.captured.containsAll(listOf("User.Read", "openid", "offline_access", "profile")))
        assertTrue(!tokenScopes.captured.contains("Mail.Read"))
        verify(exactly = 1) {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                scopes = any(),
                correlationId = correlationId,
                claimsRequestJson = retainedClaims
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // submitPassword
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSubmitPasswordMapsRejectedPasswordToIncorrectPasswordNotInvalidCredentials() {
        val state = mockContinuationState()
        val details = listOf(mapOf("code" to "diagnostic", "attributeIds" to listOf("password")))

        every {
            mockStrategy.performPasswordVerify(state, any())
        } returns NativeAuthV2InteractionApiResult.InvalidCredentials(
            correlationId = correlationId,
            error = "invalidGrant",
            errorDescription = "AADSTS50126: Error validating credentials due to invalid username or password.",
            subError = "invalidUserNameOrPassword",
            errorCodes = listOf(50126),
            traceId = "password-trace",
            timestamp = "2026-11-03 10:08:36Z",
            attributeValidationDetails = details
        )

        val result = controller.submitPassword(submitPasswordParameters(state))

        assertTrue(result is NativeAuthV2CommandResult.IncorrectPassword)
        assertEquals(
            "invalidUserNameOrPassword",
            (result as NativeAuthV2CommandResult.IncorrectPassword).subError
        )
        assertEquals("invalidGrant", result.error)
        assertEquals("AADSTS50126: Error validating credentials due to invalid username or password.", result.errorDescription)
        assertEquals(correlationId, result.correlationId)
        assertEquals(listOf(50126), result.errorCodes)
        assertEquals("password-trace", result.traceId)
        assertEquals("2026-11-03 10:08:36Z", result.timestamp)
        assertEquals(details, result.attributeValidationDetails)
    }

    @Test
    fun testCodeAndMfaSubmissionsPreserveAllTypedErrorDiagnostics() {
        val state = mockContinuationState()
        val details = listOf(mapOf("code" to "diagnostic", "attributeIds" to listOf("code")))
        every { mockStrategy.performVerify(state, "000000") } returns
            NativeAuthV2InteractionApiResult.InvalidCode(
                correlationId, "invalidGrant", "Original diagnostic.", "invalidOneTimeCode",
                listOf(50184), "trace", "time", details
            )
        val results = listOf(
            controller.submitSignInCode(submitCodeParameters(state, "000000")),
            controller.submitMFAChallenge(submitMFAChallengeParameters(state, "000000"))
        )
        results.forEach {
            val result = it as NativeAuthV2CommandResult.IncorrectCode
            assertEquals("invalidGrant", result.error)
            assertEquals("Original diagnostic.", result.errorDescription)
            assertEquals("invalidOneTimeCode", result.subError)
            assertEquals(listOf(50184), result.errorCodes)
            assertEquals("trace", result.traceId)
            assertEquals("time", result.timestamp)
            assertEquals(details, result.attributeValidationDetails)
        }
    }

    @Test
    fun testSubmitPasswordReturnsMFARequiredWhenServerRequiresSecondFactor() {
        val state = mockContinuationState()
        val mfaState = mockContinuationState()
        val methods = listOf(NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"))

        every {
            mockStrategy.performPasswordVerify(state, any())
        } returns NativeAuthV2InteractionApiResult.ChallengeRequired(correlationId, mfaState, null, methods)

        val result = controller.submitPassword(submitPasswordParameters(state))

        assertTrue(result is NativeAuthV2CommandResult.AuthMethodSelectionRequired)
        assertEquals(methods, (result as NativeAuthV2CommandResult.AuthMethodSelectionRequired).authMethods)
    }

    @Test
    fun testSubmitPasswordSubmitsPasswordForVerification() {
        val state = mockContinuationState()

        every {
            mockStrategy.performPasswordVerify(state, any())
        } returns NativeAuthV2InteractionApiResult.Redirect(correlationId, "redirect_to_web")

        controller.submitPassword(submitPasswordParameters(state))

        verify(exactly = 1) { mockStrategy.performPasswordVerify(state, any()) }
    }

    @Test
    fun testSubmitPasswordClearsPasswordBuffer() {
        val state = mockContinuationState()
        val password = "Password123!".toCharArray()

        every {
            mockStrategy.performPasswordVerify(state, any())
        } returns NativeAuthV2InteractionApiResult.Redirect(correlationId, "redirect_to_web")

        controller.submitPassword(submitPasswordParameters(state, password))

        assertArrayEquals(CharArray(password.size), password)
    }

    @Test
    fun testSubmitPasswordCompletesWithScopesAndClaimsFromContinuationState() {
        val state = mockContinuationState()
        val retainedClaims = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
        val readyState = mockContinuationState(
            scopes = listOf("User.Read"),
            claimsRequestJson = retainedClaims
        )
        val tokenScopes = slot<List<String>>()

        every {
            mockStrategy.performPasswordVerify(state, any())
        } returns NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)
        every { mockStrategy.performAuthorizeChallengeContinue(readyState) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), capture(tokenScopes), any(), any())
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "expected_test_stop",
            errorDescription = "Expected test stop before cache save.",
            errorCodes = emptyList()
        )

        controller.submitPassword(
            submitPasswordParameters(
                state = state,
                scopes = listOf("Mail.Read"),
                claimsRequestJson = """{"id_token":{"email":null}}"""
            )
        )

        assertTrue(tokenScopes.captured.containsAll(listOf("User.Read", "openid", "offline_access", "profile")))
        assertTrue(!tokenScopes.captured.contains("Mail.Read"))
        verify(exactly = 1) {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                scopes = any(),
                correlationId = correlationId,
                claimsRequestJson = retainedClaims
            )
        }
    }

    // -----------------------------------------------------------------------------------------
    // selectMFAMethod / submitMFAChallenge
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSharedChallengeRequiresExplicitMfaSelectionEvenWithOneMethod() {
        stubAuthorizeChallengeStart()
        for (scenario in listOf(NativeAuthV2FlowScenario.SIGN_IN, NativeAuthV2FlowScenario.RESET_PASSWORD)) {
            for (channel in listOf("email", "sms")) {
                val state = NativeAuthV2ContinuationStateTestFactory.multiFactor(
                    NativeAuthV2ContinuationStateTestFactory.create(correlationId, scenario = scenario)
                )
                val methods = listOf(NativeAuthV2AuthMethod("selected-id", channel, "***"))
                val challenge = NativeAuthV2InteractionApiResult.ChallengeRequired(correlationId, state, null, methods)
                every { mockStrategy.performSignInStart(any(), any()) } returns challenge
                every { mockStrategy.performResetPasswordStart(any(), any()) } returns challenge

                val result = if (scenario == NativeAuthV2FlowScenario.SIGN_IN) {
                    controller.signInStart(signInStartParameters())
                } else {
                    controller.resetPasswordStart(
                        ResetPasswordV2StartCommandParameters.builder()
                            .authority(mockAuthority)
                            .platformComponents(mockPlatformComponents)
                            .correlationId(correlationId)
                            .scopes(emptyList())
                            .username("user@contoso.com")
                            .build()
                    )
                }

                assertEquals(NativeAuthV2CommandResult.AuthMethodSelectionRequired(correlationId, state, methods), result)
            }
        }
        verify(exactly = 0) { mockStrategy.performMethodChallenge(any(), any()) }
    }

    @Test
    fun testSelectAuthMethodReturnsMfaVerificationForServerClassifiedMultiFactorAcrossFlows() {
        for (scenario in NativeAuthV2FlowScenario.values()) {
            for (channel in listOf("email", "sms")) {
                val state = NativeAuthV2ContinuationStateTestFactory.multiFactor(
                    NativeAuthV2ContinuationStateTestFactory.create(correlationId, scenario = scenario)
                )
                val verificationState = mockContinuationState()
                every { mockStrategy.performMethodChallenge(state, "selected-id") } returns
                    NativeAuthV2InteractionApiResult.CodeRequired(
                        correlationId, verificationState, "***", channel, 7
                    )

                val result = controller.selectAuthMethod(
                    NativeAuthV2SelectAuthMethodCommandParameters.builder()
                        .authority(mockAuthority)
                        .platformComponents(mockPlatformComponents)
                        .correlationId(correlationId)
                        .scopes(emptyList())
                        .continuationState(state)
                        .methodId("selected-id")
                        .build()
                )

                assertTrue(result is NativeAuthV2CommandResult.MFAVerificationRequired)
                result as NativeAuthV2CommandResult.MFAVerificationRequired
                assertSame(verificationState, result.continuationState)
                assertEquals(channel, result.challengeChannel)
                assertEquals("***", result.challengeTargetLabel)
                assertEquals(7, result.codeLength)
            }
        }
        verify(exactly = 0) { mockStrategy.performPasswordVerify(any(), any()) }
    }

    @Test
    fun testSelectAuthMethodFollowsMfaRiskVerificationWithoutLosingFactor() {
        val state = NativeAuthV2ContinuationStateTestFactory.multiFactor(
            NativeAuthV2ContinuationStateTestFactory.create(
                correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN
            )
        )
        val riskState = mockContinuationState()
        val verificationState = mockContinuationState()
        every { mockStrategy.performMethodChallenge(state, "sms-id") } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired(correlationId, riskState)
        every { mockStrategy.performRiskVerification(riskState) } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId, verificationState, "***1234", "sms", 6
            )

        val result = controller.selectAuthMethod(
            NativeAuthV2SelectAuthMethodCommandParameters.builder()
                .authority(mockAuthority)
                .platformComponents(mockPlatformComponents)
                .correlationId(correlationId)
                .scopes(emptyList())
                .continuationState(state)
                .methodId("sms-id")
                .build()
        )

        assertTrue(result is NativeAuthV2CommandResult.MFAVerificationRequired)
        assertSame(verificationState, (result as NativeAuthV2CommandResult.MFAVerificationRequired).continuationState)
        verify(exactly = 1) { mockStrategy.performRiskVerification(riskState) }
    }

    @Test
    fun testSelectMFAMethodReturnsMFAVerificationRequired() {
        val state = mockContinuationState()
        val challengeState = mockContinuationState()

        every { mockStrategy.performMethodChallenge(state, "email-1") } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                challengeTargetLabel = "u***@contoso.com",
                challengeChannel = "email",
                codeLength = 6
            )

        val result = controller.selectAuthMethod(selectMFAMethodParameters(state, "email-1"))

        assertTrue(result is NativeAuthV2CommandResult.MFAVerificationRequired)
        val verification = result as NativeAuthV2CommandResult.MFAVerificationRequired
        assertEquals(6, verification.codeLength)
        assertEquals("email", verification.challengeChannel)
        assertEquals("u***@contoso.com", verification.challengeTargetLabel)
    }

    @Test
    fun testSelectSmsMFAMethodFollowsRiskVerificationAndReturnsServerCodeContract() {
        val state = mockContinuationState()
        val riskState = mockContinuationState()
        val verificationState = mockContinuationState()

        every { mockStrategy.performMethodChallenge(state, "sms-1") } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired(
                correlationId = correlationId,
                continuationState = riskState
            )
        every { mockStrategy.performRiskVerification(riskState) } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = verificationState,
                challengeTargetLabel = "+X XXX XXX 34",
                challengeChannel = "sms",
                codeLength = 7
            )

        val result = controller.selectAuthMethod(selectMFAMethodParameters(state, "sms-1"))

        assertTrue(result is NativeAuthV2CommandResult.MFAVerificationRequired)
        val verification = result as NativeAuthV2CommandResult.MFAVerificationRequired
        assertEquals(verificationState, verification.continuationState)
        assertEquals(7, verification.codeLength)
        assertEquals("sms", verification.challengeChannel)
        assertEquals("+X XXX XXX 34", verification.challengeTargetLabel)
        verify(exactly = 1) { mockStrategy.performRiskVerification(riskState) }
    }

    @Test
    fun testSelectMFAMethodMapsBlockedMethodToApiError() {
        val state = mockContinuationState()

        every { mockStrategy.performMethodChallenge(state, "email-1") } returns
            NativeAuthV2InteractionApiResult.UnknownError(
                correlationId = correlationId,
                error = "accessDenied",
                errorDescription = "blocked"
            )

        val result = controller.selectAuthMethod(selectMFAMethodParameters(state, "email-1"))

        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testSelectMFAMethodMapsStaleMethodToApiError() {
        val state = mockContinuationState()

        every { mockStrategy.performMethodChallenge(state, "stale") } returns
            NativeAuthV2InteractionApiResult.UnknownError(
                correlationId = correlationId,
                error = "invalid_state",
                errorDescription = "The requested authentication method is not available."
            )

        val result = controller.selectAuthMethod(selectMFAMethodParameters(state, "stale"))

        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals("invalid_state", (result as INativeAuthCommandResult.APIError).error)
    }

    @Test
    fun testSubmitMFAChallengeMapsWrongCodeToIncorrectCode() {
        val state = mockContinuationState()

        every { mockStrategy.performVerify(state, "000000") } returns
            NativeAuthV2InteractionApiResult.InvalidCode(
                correlationId = correlationId,
                error = "invalidGrant",
                errorDescription = "AADSTS50184: invalid code.",
                subError = "invalidOneTimeCode",
                errorCodes = listOf(50184)
            )

        val result = controller.submitMFAChallenge(submitMFAChallengeParameters(state, "000000"))

        assertTrue(result is NativeAuthV2CommandResult.IncorrectCode)
        assertEquals(
            "invalidOneTimeCode",
            (result as NativeAuthV2CommandResult.IncorrectCode).subError
        )
    }

    @Test
    fun testSubmitMFAChallengeCompletesThroughAuthorizeChallengeContinue() {
        val state = mockContinuationState()
        val readyState = mockContinuationState()

        every { mockStrategy.performVerify(state, "123456") } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)
        every { mockStrategy.performAuthorizeChallengeContinue(readyState) } returns
            AuthorizeChallengeApiResult.UnknownError(
                correlationId = correlationId,
                error = "expected_test_stop",
                errorDescription = "Expected test stop before cache save."
            )

        val result = controller.submitMFAChallenge(submitMFAChallengeParameters(state, "123456"))

        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals("expected_test_stop", (result as INativeAuthCommandResult.APIError).error)
        verify(exactly = 1) { mockStrategy.performAuthorizeChallengeContinue(readyState) }
    }

    @Test
    fun testSubmitMFAChallengeCompletesWithScopesAndClaimsFromContinuationState() {
        val state = mockContinuationState()
        val retainedClaims = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
        val readyState = mockContinuationState(
            scopes = listOf("User.Read"),
            claimsRequestJson = retainedClaims
        )
        val tokenScopes = slot<List<String>>()

        every { mockStrategy.performVerify(state, "123456") } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)
        every { mockStrategy.performAuthorizeChallengeContinue(readyState) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), capture(tokenScopes), any(), any())
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "expected_test_stop",
            errorDescription = "Expected test stop before cache save.",
            errorCodes = emptyList()
        )

        controller.submitMFAChallenge(
            submitMFAChallengeParameters(
                state = state,
                code = "123456",
                scopes = listOf("Mail.Read"),
                claimsRequestJson = """{"id_token":{"email":null}}"""
            )
        )

        assertTrue(tokenScopes.captured.containsAll(listOf("User.Read", "openid", "offline_access", "profile")))
        assertTrue(!tokenScopes.captured.contains("Mail.Read"))
        verify(exactly = 1) {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                scopes = any(),
                correlationId = correlationId,
                claimsRequestJson = retainedClaims
            )
        }
    }

    @Test
    fun testSignInSubmissionsPreserveUnknownErrorDiagnostics() {
        val state = mockContinuationState()
        val apiError = NativeAuthV2InteractionApiResult.UnknownError(
            correlationId = correlationId,
            error = "service_error",
            errorDescription = "The service could not verify the request.",
            errorCodes = listOf(50126),
            subError = "verification_failed",
            attributeValidationDetails = listOf(mapOf("name" to "email", "reason" to "invalid")),
            traceId = "service-trace",
            timestamp = "service-time"
        )
        every { mockStrategy.performPasswordVerify(state, any()) } returns apiError
        every { mockStrategy.performVerify(state, any()) } returns apiError
        every { mockStrategy.performMethodChallenge(state, "email-1") } returns apiError

        val results = listOf(
            controller.submitPassword(submitPasswordParameters(state)),
            controller.submitSignInCode(submitCodeParameters(state, "123456")),
            controller.selectAuthMethod(selectMFAMethodParameters(state, "email-1")),
            controller.submitMFAChallenge(submitMFAChallengeParameters(state, "123456"))
        )

        results.forEach { result ->
            assertTrue(result is INativeAuthCommandResult.APIError)
            result as INativeAuthCommandResult.APIError
            assertEquals(apiError.correlationId, result.correlationId)
            assertEquals(apiError.error, result.error)
            assertEquals(apiError.errorDescription, result.errorDescription)
            assertEquals(apiError.errorCodes, result.errorCodes)
            assertEquals(apiError.subError, result.subError)
            assertEquals(apiError.attributeValidationDetails, result.attributeValidationDetails)
            assertEquals(apiError.traceId, result.traceId)
            assertEquals(apiError.timestamp, result.timestamp)
        }
    }

    @Test
    fun testSignInSubmissionsPreserveBrowserRedirectDiagnostics() {
        val state = mockContinuationState()
        val redirect = NativeAuthV2InteractionApiResult.Redirect(
            correlationId = correlationId,
            redirectReason = "browser_required",
            traceId = "redirect-trace",
            timestamp = "redirect-time",
            errorCodes = listOf(50076)
        )
        every { mockStrategy.performPasswordVerify(state, any()) } returns redirect
        every { mockStrategy.performVerify(state, any()) } returns redirect
        every { mockStrategy.performMethodChallenge(state, "email-1") } returns redirect

        val results = listOf(
            controller.submitPassword(submitPasswordParameters(state)),
            controller.submitSignInCode(submitCodeParameters(state, "123456")),
            controller.selectAuthMethod(selectMFAMethodParameters(state, "email-1")),
            controller.submitMFAChallenge(submitMFAChallengeParameters(state, "123456"))
        )

        results.forEach { result ->
            assertTrue(result is INativeAuthCommandResult.Redirect)
            result as INativeAuthCommandResult.Redirect
            assertEquals(redirect.correlationId, result.correlationId)
            assertEquals(redirect.redirectReason, result.redirectReason)
            assertEquals(redirect.traceId, result.traceId)
            assertEquals(redirect.timestamp, result.timestamp)
            assertEquals(redirect.errorCodes, result.errorCodes)
        }
    }

    @Test
    fun testSelectMFAMethodPreservesRiskVerificationErrors() {
        val state = mockContinuationState()
        val riskState = mockContinuationState()
        every { mockStrategy.performMethodChallenge(state, "sms-1") } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired(correlationId, riskState)
        every { mockStrategy.performRiskVerification(riskState) } returns
            NativeAuthV2InteractionApiResult.UnknownError(
                correlationId,
                "risk_failed",
                "Risk verification failed.",
                traceId = "risk-trace"
            )

        val result = controller.selectAuthMethod(selectMFAMethodParameters(state, "sms-1"))

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals("risk_failed", result.error)
        assertEquals("risk-trace", result.traceId)
        verify(exactly = 1) { mockStrategy.performRiskVerification(riskState) }
        verify(exactly = 0) { mockStrategy.performVerify(any(), any()) }
    }

    @Test
    fun testSubmitPasswordRethrowsStrategyFailureAndClearsPassword() {
        val state = mockContinuationState()
        val password = "Password123!".toCharArray()
        val failure = IllegalStateException("verification failed")
        every { mockStrategy.performPasswordVerify(state, any()) } throws failure

        try {
            controller.submitPassword(submitPasswordParameters(state, password))
            fail("Expected the strategy exception")
        } catch (exception: IllegalStateException) {
            assertSame(failure, exception)
        }

        assertArrayEquals(CharArray(password.size), password)
    }

    @Test
    fun testMfaCommandsRethrowStrategyFailures() {
        val state = mockContinuationState()
        val failure = IllegalStateException("verification failed")
        every { mockStrategy.performMethodChallenge(state, "email-1") } throws failure
        every { mockStrategy.performVerify(state, any()) } throws failure

        try {
            controller.selectAuthMethod(selectMFAMethodParameters(state, "email-1"))
            fail("Expected the method-selection exception")
        } catch (exception: IllegalStateException) {
            assertSame(failure, exception)
        }
        try {
            controller.submitMFAChallenge(submitMFAChallengeParameters(state, "123456"))
            fail("Expected the challenge-verification exception")
        } catch (exception: IllegalStateException) {
            assertSame(failure, exception)
        }
    }

    // -----------------------------------------------------------------------------------------
    // Helpers
    // -----------------------------------------------------------------------------------------

    private fun stubAuthorizeChallengeStart(
        state: NativeAuthV2ContinuationState = mockContinuationState()
    ) {
        every {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = any(),
                entryRelation = any(),
                scenario = any(),
                scopes = any(),
                claimsRequestJson = any()
            )
        } returns AuthorizeChallengeApiResult.ContinuationRequired(correlationId, state)
    }

    /**
     * Stubs authorize-challenge start, sign-in entry (offering the password method), and the
     * password-method challenge, leaving [passwordState] ready for password verification.
     */
    private fun stubStartThroughPasswordChallenge(passwordState: NativeAuthV2ContinuationState) {
        val challengeState = mockContinuationState()
        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                hint = null,
                methods = listOf(NativeAuthV2AuthMethod("pwd-1", "password", null))
            )
        every { mockStrategy.performMethodChallenge(challengeState, "pwd-1") } returns
            NativeAuthV2InteractionApiResult.PasswordRequired(correlationId, passwordState)
    }

    private fun mockContinuationState(
        id: String = correlationId,
        scopes: List<String> = emptyList(),
        claimsRequestJson: String? = null
    ): NativeAuthV2ContinuationState {
        val state = mockk<NativeAuthV2ContinuationState>(relaxed = true)
        every { state.correlationId } returns id
        every { state.scopesForTokenRequest() } returns scopes
        every { state.claimsRequestJsonForTokenRequest() } returns claimsRequestJson
        return state
    }

    private fun signInStartParameters(
        username: String = "user@contoso.com",
        scopes: List<String> = emptyList(),
        claimsRequestJson: String? = null
    ): SignInV2StartCommandParameters =
        SignInV2StartCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(scopes)
            .claimsRequestJson(claimsRequestJson)
            .username(username)
            .build()

    private fun submitCodeParameters(
        state: NativeAuthV2ContinuationState,
        code: String,
        scopes: List<String> = emptyList(),
        claimsRequestJson: String? = null
    ): NativeAuthV2SubmitCodeCommandParameters =
        NativeAuthV2SubmitCodeCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(scopes)
            .claimsRequestJson(claimsRequestJson)
            .continuationState(state)
            .code(code)
            .build()

    private fun submitPasswordParameters(
        state: NativeAuthV2ContinuationState,
        password: CharArray = "Password123!".toCharArray(),
        scopes: List<String> = emptyList(),
        claimsRequestJson: String? = null
    ): NativeAuthV2SubmitPasswordCommandParameters =
        NativeAuthV2SubmitPasswordCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(scopes)
            .claimsRequestJson(claimsRequestJson)
            .continuationState(state)
            .password(password)
            .build()

    private fun selectMFAMethodParameters(
        state: NativeAuthV2ContinuationState,
        methodId: String
    ): NativeAuthV2SelectAuthMethodCommandParameters {
        every { state.isMultiFactorAuthentication() } returns true
        return NativeAuthV2SelectAuthMethodCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .continuationState(state)
            .methodId(methodId)
            .build()
    }

    private fun submitMFAChallengeParameters(
        state: NativeAuthV2ContinuationState,
        code: String,
        scopes: List<String> = emptyList(),
        claimsRequestJson: String? = null
    ): NativeAuthV2SubmitMFAChallengeCommandParameters =
        NativeAuthV2SubmitMFAChallengeCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(scopes)
            .claimsRequestJson(claimsRequestJson)
            .continuationState(state)
            .code(code)
            .build()
}
