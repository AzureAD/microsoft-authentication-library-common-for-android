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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SelectMFAMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitMFAChallengeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignInV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the V2 sign-in orchestration added to [NativeAuthV2FlowController]: password
 * first-factor selection, entry versus deferred password submission, email MFA transitions, and
 * the error mapping each step must apply.
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
    fun testSignInStartWithoutPasswordReturnsPasswordRequired() {
        val passwordState = mockContinuationState()
        stubStartThroughPasswordChallenge(passwordState)

        val result = controller.signInStart(signInStartParameters(password = null))

        assertTrue(result is NativeAuthV2CommandResult.PasswordRequired)
        assertEquals(
            passwordState,
            (result as NativeAuthV2CommandResult.PasswordRequired).continuationState
        )
        verify(exactly = 0) {
            mockStrategy.performPasswordVerify(any(), any(), any())
        }
    }

    @Test
    fun testSignInStartWithEmptyPasswordReturnsPasswordRequiredWithoutSubmitting() {
        val passwordState = mockContinuationState()
        stubStartThroughPasswordChallenge(passwordState)
        every {
            mockStrategy.performPasswordVerify(passwordState, any(), false)
        } returns NativeAuthV2InteractionApiResult.Redirect(
            correlationId,
            "unexpected_empty_password_submission"
        )

        val result = controller.signInStart(signInStartParameters(password = CharArray(0)))

        assertTrue(result is NativeAuthV2CommandResult.PasswordRequired)
        assertEquals(
            passwordState,
            (result as NativeAuthV2CommandResult.PasswordRequired).continuationState
        )
        verify(exactly = 0) {
            mockStrategy.performPasswordVerify(any(), any(), any())
        }
    }

    @Test
    fun testSignInStartWithPasswordVerifiesImmediatelyAndDoesNotSurfacePasswordRequired() {
        val passwordState = mockContinuationState()
        val readyState = mockContinuationState()
        stubStartThroughPasswordChallenge(passwordState)

        every {
            mockStrategy.performPasswordVerify(passwordState, any(), false)
        } returns NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)
        every { mockStrategy.performAuthorizeChallengeContinue(readyState) } returns
            AuthorizeChallengeApiResult.UnknownError(
                correlationId = correlationId,
                error = "expected_test_stop",
                errorDescription = "Expected test stop before cache save."
            )

        val result = controller.signInStart(signInStartParameters())

        // Reaching the terminal path proves the entry password was auto-submitted rather than
        // surfaced as PasswordRequired.
        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals("expected_test_stop", (result as INativeAuthCommandResult.APIError).error)
        verify(exactly = 1) {
            mockStrategy.performPasswordVerify(passwordState, any(), false)
        }
    }

    @Test
    fun testSignInStartSelectsPasswordMethodEvenWhenEmailIsOfferedFirst() {
        val challengeState = mockContinuationState()
        val passwordState = mockContinuationState()

        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                hint = null,
                methods = listOf(
                    NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"),
                    NativeAuthV2AuthMethod("pwd-1", "password", null)
                )
            )
        every { mockStrategy.performPasswordMethodChallenge(challengeState, "pwd-1") } returns
            NativeAuthV2InteractionApiResult.PasswordRequired(correlationId, passwordState)

        val result = controller.signInStart(signInStartParameters(password = null))

        assertTrue(result is NativeAuthV2CommandResult.PasswordRequired)
        verify(exactly = 1) {
            mockStrategy.performPasswordMethodChallenge(challengeState, "pwd-1")
        }
    }

    @Test
    fun testSignInStartWithEmptyPasswordSelectsPasswordWhenEmailIsOfferedFirst() {
        val challengeState = mockContinuationState()
        val passwordState = mockContinuationState()

        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                hint = null,
                methods = listOf(
                    NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"),
                    NativeAuthV2AuthMethod("pwd-1", "password", null)
                )
            )
        every { mockStrategy.performPasswordMethodChallenge(challengeState, "pwd-1") } returns
            NativeAuthV2InteractionApiResult.PasswordRequired(correlationId, passwordState)
        every {
            mockStrategy.performPasswordVerify(passwordState, any(), false)
        } returns NativeAuthV2InteractionApiResult.Redirect(
            correlationId,
            "unexpected_empty_password_submission"
        )

        val result = controller.signInStart(signInStartParameters(password = CharArray(0)))

        assertTrue(result is NativeAuthV2CommandResult.PasswordRequired)
        assertEquals(
            passwordState,
            (result as NativeAuthV2CommandResult.PasswordRequired).continuationState
        )
        verify(exactly = 1) {
            mockStrategy.performPasswordMethodChallenge(challengeState, "pwd-1")
        }
        verify(exactly = 0) {
            mockStrategy.performPasswordVerify(any(), any(), any())
        }
    }

    @Test
    fun testSignInStartFailsDeterministicallyWhenOnlyEmailFirstFactorIsOffered() {
        val challengeState = mockContinuationState()

        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                hint = null,
                methods = listOf(NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"))
            )

        val result = controller.signInStart(signInStartParameters(password = null))

        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals(
            "unsupported_first_factor",
            (result as INativeAuthCommandResult.APIError).error
        )
        verify(exactly = 0) { mockStrategy.performPasswordMethodChallenge(any(), any()) }
        verify(exactly = 0) { mockStrategy.performMfaMethodChallenge(any(), any()) }
    }

    @Test
    fun testSignInStartMapsUnknownUserToUserNotFound() {
        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.UserNotFound(
                correlationId = correlationId,
                error = "invalid_grant",
                errorDescription = "AADSTS50034: user not found.",
                errorCodes = listOf(50034)
            )

        val result = controller.signInStart(signInStartParameters(password = null))

        assertTrue(result is NativeAuthV2CommandResult.UserNotFound)
        assertEquals(listOf(50034), (result as NativeAuthV2CommandResult.UserNotFound).errorCodes)
    }

    @Test
    fun testSignInStartMapsRejectedEntryPasswordToInvalidCredentials() {
        val passwordState = mockContinuationState()
        stubStartThroughPasswordChallenge(passwordState)

        every {
            mockStrategy.performPasswordVerify(passwordState, any(), false)
        } returns NativeAuthV2InteractionApiResult.InvalidCredentials(
            correlationId = correlationId,
            error = "invalid_grant",
            errorDescription = "AADSTS50126: invalid username or password.",
            subError = "invalidUserNameOrPassword",
            deferredSubmission = false,
            errorCodes = listOf(50126)
        )

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.InvalidCredentials)
        val invalid = result as NativeAuthV2CommandResult.InvalidCredentials
        assertEquals("invalidUserNameOrPassword", invalid.subError)
        assertEquals(listOf(50126), invalid.errorCodes)
    }

    @Test
    fun testSignInStartReturnsMFARequiredWithServerMethodsAndSendsNoChallenge() {
        val passwordState = mockContinuationState()
        val mfaState = mockContinuationState()
        val methods = listOf(NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"))
        stubStartThroughPasswordChallenge(passwordState)

        every {
            mockStrategy.performPasswordVerify(passwordState, any(), false)
        } returns NativeAuthV2InteractionApiResult.MFARequired(correlationId, mfaState, methods)

        val result = controller.signInStart(signInStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.MFARequired)
        val mfa = result as NativeAuthV2CommandResult.MFARequired
        assertEquals(methods, mfa.authMethods)
        assertEquals(mfaState, mfa.continuationState)
        verify(exactly = 0) { mockStrategy.performMfaMethodChallenge(any(), any()) }
    }

    @Test
    fun testSignInStartClearsEntryPasswordBuffer() {
        val passwordState = mockContinuationState()
        val password = "Password123!".toCharArray()
        stubStartThroughPasswordChallenge(passwordState)

        // The interactor is mocked here, so it never clears the buffer; the controller's own
        // finally block must still do so.
        every {
            mockStrategy.performPasswordVerify(passwordState, any(), false)
        } returns NativeAuthV2InteractionApiResult.InvalidCredentials(
            correlationId = correlationId,
            error = "invalid_grant",
            errorDescription = "invalid",
            subError = "invalidUserNameOrPassword",
            deferredSubmission = false
        )

        controller.signInStart(signInStartParameters(password = password))

        assertArrayEquals(CharArray(password.size), password)
    }

    @Test
    fun testSignInStartClearsEntryPasswordBufferWhenFirstFactorIsUnsupported() {
        val challengeState = mockContinuationState()
        val password = "Password123!".toCharArray()

        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                hint = null,
                methods = listOf(NativeAuthV2AuthMethod("email-1", "email", null))
            )

        controller.signInStart(signInStartParameters(password = password))

        assertArrayEquals(CharArray(password.size), password)
    }

    @Test
    fun testSignInStartMapsBrowserRequiredToRedirect() {
        stubAuthorizeChallengeStart()
        every { mockStrategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.Redirect(correlationId, "redirect_to_web")

        val result = controller.signInStart(signInStartParameters(password = null))

        assertTrue(result is INativeAuthCommandResult.Redirect)
        assertEquals(
            "redirect_to_web",
            (result as INativeAuthCommandResult.Redirect).redirectReason
        )
    }

    @Test
    fun testSignInStartCarriesScopesAndClaimsIntoAuthorizeChallengeAndToken() {
        val passwordState = mockContinuationState()
        val readyState = mockContinuationState()
        val claims = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
        val tokenScopes = slot<List<String>>()
        stubStartThroughPasswordChallenge(passwordState)

        every {
            mockStrategy.performPasswordVerify(passwordState, any(), false)
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

        controller.signInStart(
            signInStartParameters(scopes = listOf("User.Read"), claimsRequestJson = claims)
        )

        verify(exactly = 1) {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = correlationId,
                entryRelation = "signIn",
                scenario = any(),
                scopes = listOf("User.Read"),
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
        assertTrue(tokenScopes.captured.containsAll(listOf("User.Read", "openid", "offline_access", "profile")))
    }

    // -----------------------------------------------------------------------------------------
    // submitPassword
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSubmitPasswordMapsRejectedPasswordToIncorrectPasswordNotInvalidCredentials() {
        val state = mockContinuationState()

        every {
            mockStrategy.performPasswordVerify(state, any(), true)
        } returns NativeAuthV2InteractionApiResult.InvalidCredentials(
            correlationId = correlationId,
            error = "invalid_grant",
            errorDescription = "AADSTS50126: invalid username or password.",
            subError = "invalidUserNameOrPassword",
            deferredSubmission = true,
            errorCodes = listOf(50126)
        )

        val result = controller.submitPassword(submitPasswordParameters(state))

        assertTrue(result is NativeAuthV2CommandResult.IncorrectPassword)
        assertEquals(
            "invalidUserNameOrPassword",
            (result as NativeAuthV2CommandResult.IncorrectPassword).subError
        )
    }

    @Test
    fun testSubmitPasswordReturnsMFARequiredWhenServerRequiresSecondFactor() {
        val state = mockContinuationState()
        val mfaState = mockContinuationState()
        val methods = listOf(NativeAuthV2AuthMethod("email-1", "email", "u***@contoso.com"))

        every {
            mockStrategy.performPasswordVerify(state, any(), true)
        } returns NativeAuthV2InteractionApiResult.MFARequired(correlationId, mfaState, methods)

        val result = controller.submitPassword(submitPasswordParameters(state))

        assertTrue(result is NativeAuthV2CommandResult.MFARequired)
        assertEquals(methods, (result as NativeAuthV2CommandResult.MFARequired).authMethods)
    }

    @Test
    fun testSubmitPasswordMarksSubmissionAsDeferred() {
        val state = mockContinuationState()
        val deferred = slot<Boolean>()

        every {
            mockStrategy.performPasswordVerify(state, any(), capture(deferred))
        } returns NativeAuthV2InteractionApiResult.Redirect(correlationId, "redirect_to_web")

        controller.submitPassword(submitPasswordParameters(state))

        assertTrue(deferred.captured)
    }

    @Test
    fun testSubmitPasswordClearsPasswordBuffer() {
        val state = mockContinuationState()
        val password = "Password123!".toCharArray()

        every {
            mockStrategy.performPasswordVerify(state, any(), true)
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
            mockStrategy.performPasswordVerify(state, any(), true)
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
    fun testSelectMFAMethodReturnsMFAVerificationRequired() {
        val state = mockContinuationState()
        val challengeState = mockContinuationState()

        every { mockStrategy.performMfaMethodChallenge(state, "email-1") } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = challengeState,
                challengeTargetLabel = "u***@contoso.com",
                challengeChannel = "email",
                codeLength = 6
            )

        val result = controller.selectMFAMethod(selectMFAMethodParameters(state, "email-1"))

        assertTrue(result is NativeAuthV2CommandResult.MFAVerificationRequired)
        val verification = result as NativeAuthV2CommandResult.MFAVerificationRequired
        assertEquals(6, verification.codeLength)
        assertEquals("email", verification.challengeChannel)
        assertEquals("u***@contoso.com", verification.challengeTargetLabel)
    }

    @Test
    fun testSelectMFAMethodMapsBlockedMethod() {
        val state = mockContinuationState()

        every { mockStrategy.performMfaMethodChallenge(state, "email-1") } returns
            NativeAuthV2InteractionApiResult.AuthMethodBlocked(
                correlationId = correlationId,
                error = "accessDenied",
                errorDescription = "blocked",
                subError = "providerBlockedByRep"
            )

        val result = controller.selectMFAMethod(selectMFAMethodParameters(state, "email-1"))

        assertTrue(result is NativeAuthV2CommandResult.AuthMethodBlocked)
    }

    @Test
    fun testSelectMFAMethodMapsStaleMethodToApiError() {
        val state = mockContinuationState()

        every { mockStrategy.performMfaMethodChallenge(state, "stale") } returns
            NativeAuthV2InteractionApiResult.UnknownError(
                correlationId = correlationId,
                error = "invalid_state",
                errorDescription = "The requested authentication method is not available."
            )

        val result = controller.selectMFAMethod(selectMFAMethodParameters(state, "stale"))

        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals("invalid_state", (result as INativeAuthCommandResult.APIError).error)
    }

    @Test
    fun testSubmitMFAChallengeMapsWrongCodeToIncorrectCode() {
        val state = mockContinuationState()

        every { mockStrategy.performMfaVerify(state, "000000") } returns
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

        every { mockStrategy.performMfaVerify(state, "123456") } returns
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

        every { mockStrategy.performMfaVerify(state, "123456") } returns
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
        every { mockStrategy.performPasswordMethodChallenge(challengeState, "pwd-1") } returns
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
        password: CharArray? = "Password123!".toCharArray(),
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
            .password(password)
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
    ): NativeAuthV2SelectMFAMethodCommandParameters =
        NativeAuthV2SelectMFAMethodCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .continuationState(state)
            .methodId(methodId)
            .build()

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
