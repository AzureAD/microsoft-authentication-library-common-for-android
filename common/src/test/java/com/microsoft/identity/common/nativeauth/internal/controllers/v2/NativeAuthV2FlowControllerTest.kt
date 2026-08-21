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

import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterResetPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Focused unit tests for [NativeAuthV2FlowController], covering the app-triggered
 * sign-in-after-reset behaviour added to match the first-pass iOS E2E completion logic:
 *
 * - A successful password reset (start / submit-code / submit-new-password, including both the
 *   fast-forward and poll-completion paths) now returns
 *   [NativeAuthV2CommandResult.SignInAfterResetPasswordRequired] instead of eagerly performing the
 *   token exchange.
 * - [NativeAuthV2FlowController.signInAfterResetPassword] is the only entry point that performs
 *   the token exchange (via the shared, unchanged `completeFlow` path), and now returns typed
 *   [INativeAuthCommandResult.Redirect] / [INativeAuthCommandResult.APIError] results instead of
 *   throwing.
 *
 * [NativeAuthV2OAuth2Strategy] and [NativeAuthCIAMAuthority] are mocked so these tests exercise only
 * the controller's own branching/mapping logic, not the (unchanged) HTTP or cache layers
 * underneath `completeFlow`'s success path.
 */
class NativeAuthV2FlowControllerTest {

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

    private fun mockContinuationState(id: String = correlationId): NativeAuthV2ContinuationState {
        val state = mockk<NativeAuthV2ContinuationState>(relaxed = true)
        every { state.correlationId } returns id
        return state
    }

    private fun resetPasswordStartParameters(
        username: String = "user@contoso.com",
        claimsRequestJson: String? = null
    ): ResetPasswordV2StartCommandParameters =
        ResetPasswordV2StartCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .claimsRequestJson(claimsRequestJson)
            .username(username)
            .build()

    private fun submitCodeParameters(
        state: NativeAuthV2ContinuationState,
        code: String = "123456"
    ): NativeAuthV2SubmitCodeCommandParameters =
        NativeAuthV2SubmitCodeCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .continuationState(state)
            .code(code)
            .build()

    private fun submitNewPasswordParameters(
        state: NativeAuthV2ContinuationState,
        newPassword: CharArray = "newPassword1".toCharArray()
    ): NativeAuthV2SubmitNewPasswordCommandParameters =
        NativeAuthV2SubmitNewPasswordCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .continuationState(state)
            .newPassword(newPassword)
            .build()

    private fun signInAfterResetPasswordParameters(
        state: NativeAuthV2ContinuationState,
        scopes: List<String> = emptyList(),
        claimsRequestJson: String? = null
    ): NativeAuthV2SignInAfterResetPasswordCommandParameters =
        NativeAuthV2SignInAfterResetPasswordCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(scopes)
            .claimsRequestJson(claimsRequestJson)
            .continuationState(state)
            .build()

    // -----------------------------------------------------------------------------------------
    // ReadyToComplete -> SignInAfterResetPasswordRequired rerouting
    // -----------------------------------------------------------------------------------------

    @Test
    fun testResetPasswordStartRetainsClaimsForTokenRequest() {
        val claimsRequestJson = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""

        every {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = correlationId,
                entryRelation = any(),
                scenario = any(),
                scopes = any(),
                claimsRequestJson = claimsRequestJson
            )
        } returns AuthorizeChallengeApiResult.UnknownError(
            correlationId = correlationId,
            error = "expected_test_stop",
            errorDescription = "Expected test stop after authorize challenge."
        )

        controller.resetPasswordStart(
            resetPasswordStartParameters(claimsRequestJson = claimsRequestJson)
        )

        verify(exactly = 1) {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = correlationId,
                entryRelation = any(),
                scenario = any(),
                scopes = any(),
                claimsRequestJson = claimsRequestJson
            )
        }
    }

    @Test
    fun testResetPasswordStartReturnsSignInAfterResetPasswordRequiredWhenChallengeReadyToComplete() {
        val afterStartState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = resetPasswordStartParameters()

        every {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = any(),
                entryRelation = any(),
                scenario = any(),
                scopes = any(),
                claimsRequestJson = null
            )
        } returns AuthorizeChallengeApiResult.ContinuationRequired(
            correlationId = correlationId,
            continuationState = afterStartState
        )
        every {
            mockStrategy.performResetPasswordStart(username = any(), state = any())
        } returns NativeAuthV2InteractionApiResult.ChallengeRequired(
            correlationId = correlationId,
            continuationState = afterStartState,
            hint = null
        )
        every {
            mockStrategy.performChallenge(state = any())
        } returns NativeAuthV2InteractionApiResult.ReadyToComplete(
            correlationId = correlationId,
            continuationState = readyState
        )

        val result = controller.resetPasswordStart(parameters)

        assertTrue(result is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired)
        result as NativeAuthV2CommandResult.SignInAfterResetPasswordRequired
        assertEquals(correlationId, result.correlationId)
        assertEquals(readyState, result.continuationState)
    }

    @Test
    fun testResetPasswordStartReturnsSignInAfterResetPasswordRequiredWhenStartReadyToComplete() {
        val afterAuthorizeState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = resetPasswordStartParameters()

        every {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = any(),
                entryRelation = any(),
                scenario = any(),
                scopes = any(),
                claimsRequestJson = null
            )
        } returns AuthorizeChallengeApiResult.ContinuationRequired(
            correlationId = correlationId,
            continuationState = afterAuthorizeState
        )
        every {
            mockStrategy.performResetPasswordStart(username = any(), state = any())
        } returns NativeAuthV2InteractionApiResult.ReadyToComplete(
            correlationId = correlationId,
            continuationState = readyState
        )

        val result = controller.resetPasswordStart(parameters)

        assertTrue(result is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired)
        result as NativeAuthV2CommandResult.SignInAfterResetPasswordRequired
        assertEquals(correlationId, result.correlationId)
        assertEquals(readyState, result.continuationState)
    }

    @Test
    fun testSubmitCodeReturnsSignInAfterResetPasswordRequiredWhenVerifyReadyToComplete() {
        val inputState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = submitCodeParameters(inputState)

        every {
            mockStrategy.performVerify(state = any(), otp = any())
        } returns NativeAuthV2InteractionApiResult.ReadyToComplete(
            correlationId = correlationId,
            continuationState = readyState
        )

        val result = controller.submitCode(parameters)

        assertTrue(result is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired)
        result as NativeAuthV2CommandResult.SignInAfterResetPasswordRequired
        assertEquals(correlationId, result.correlationId)
        assertEquals(readyState, result.continuationState)
    }

    @Test
    fun testSubmitCodePreservesInvalidCodeSubError() {
        val state = mockContinuationState()
        val errorCodes = listOf(50001)

        every { mockStrategy.performVerify(state = state, otp = any()) } returns
            NativeAuthV2InteractionApiResult.InvalidCode(
                correlationId = correlationId,
                error = "invalid_grant",
                errorDescription = "Code is invalid.",
                subError = "invalid_oob_value",
                errorCodes = errorCodes,
                retryState = state
            )

        val result = controller.submitCode(submitCodeParameters(state)) as
            NativeAuthV2CommandResult.IncorrectCode

        assertEquals("invalid_oob_value", result.subError)
        assertEquals(errorCodes, result.errorCodes)
    }

    @Test
    fun testSubmitNewPasswordReturnsSignInAfterResetPasswordRequiredWhenUpdateReadyToComplete() {
        val inputState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = submitNewPasswordParameters(inputState)

        every {
            mockStrategy.performUpdatePassword(state = any(), newPassword = any())
        } returns NativeAuthV2InteractionApiResult.ReadyToComplete(
            correlationId = correlationId,
            continuationState = readyState
        )

        val result = controller.submitNewPassword(parameters)

        assertTrue(result is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired)
        result as NativeAuthV2CommandResult.SignInAfterResetPasswordRequired
        assertEquals(correlationId, result.correlationId)
        assertEquals(readyState, result.continuationState)
    }

    @Test
    fun testSubmitNewPasswordPreservesInvalidPasswordSubError() {
        val state = mockContinuationState()
        val errorCodes = listOf(120002)

        every { mockStrategy.performUpdatePassword(state = state, newPassword = any()) } returns
            NativeAuthV2InteractionApiResult.InvalidPassword(
                correlationId = correlationId,
                error = "invalid_request",
                errorDescription = "Password is too weak.",
                subError = "password_too_weak",
                errorCodes = errorCodes,
                retryState = state
            )

        val result = controller.submitNewPassword(submitNewPasswordParameters(state)) as
            NativeAuthV2CommandResult.PasswordNotAccepted

        assertEquals("password_too_weak", result.subError)
        assertEquals(errorCodes, result.errorCodes)
    }

    @Test
    fun testResetPasswordStartPreservesUserNotFoundErrorCodes() {
        val state = mockContinuationState()
        val errorCodes = listOf(50034)

        every {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = any(),
                entryRelation = any(),
                scenario = any(),
                scopes = any(),
                claimsRequestJson = null
            )
        } returns AuthorizeChallengeApiResult.ContinuationRequired(
            correlationId = correlationId,
            continuationState = state
        )
        every {
            mockStrategy.performResetPasswordStart(username = any(), state = state)
        } returns NativeAuthV2InteractionApiResult.UserNotFound(
            correlationId = correlationId,
            error = "invalidRequest",
            errorDescription = "AADSTS50034: User not found.",
            errorCodes = errorCodes
        )

        val result = controller.resetPasswordStart(resetPasswordStartParameters()) as
            NativeAuthV2CommandResult.UserNotFound

        assertEquals(errorCodes, result.errorCodes)
    }

    @Test
    fun testSubmitNewPasswordReturnsSignInAfterResetPasswordRequiredWhenPollReachesCompletion() {
        val inputState = mockContinuationState()
        val pollState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = submitNewPasswordParameters(inputState)

        every {
            mockStrategy.performUpdatePassword(state = any(), newPassword = any())
        } returns NativeAuthV2InteractionApiResult.PollInProgress(
            correlationId = correlationId,
            continuationState = pollState,
            retryAfterMillis = 0L
        )
        every {
            mockStrategy.performPoll(state = any())
        } returns NativeAuthV2InteractionApiResult.ReadyToComplete(
            correlationId = correlationId,
            continuationState = readyState
        )

        val result = controller.submitNewPassword(parameters)

        assertTrue(result is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired)
        result as NativeAuthV2CommandResult.SignInAfterResetPasswordRequired
        assertEquals(correlationId, result.correlationId)
        assertEquals(readyState, result.continuationState)
    }

    @Test
    fun testSubmitNewPasswordReturnsAPIErrorWhenPollingIsInterrupted() {
        val inputState = mockContinuationState()
        val pollState = mockContinuationState()
        val parameters = submitNewPasswordParameters(inputState)

        every {
            mockStrategy.performUpdatePassword(state = any(), newPassword = any())
        } returns NativeAuthV2InteractionApiResult.PollInProgress(
            correlationId = correlationId,
            continuationState = pollState,
            retryAfterMillis = 1L
        )

        val result = try {
            Thread.currentThread().interrupt()
            controller.submitNewPassword(parameters)
        } finally {
            // Clear the flag so it cannot leak into any subsequent test on this thread.
            Thread.interrupted()
        }

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals(correlationId, result.correlationId)
        assertEquals("poll_interrupted", result.error)
        verify(exactly = 0) { mockStrategy.performPoll(state = any()) }
    }

    @Test
    fun testSubmitNewPasswordReturnsPasswordResetFailedAfterMaximumPollAttempts() {
        val inputState = mockContinuationState()
        val pollState = mockContinuationState()
        val parameters = submitNewPasswordParameters(inputState)

        every {
            mockStrategy.performUpdatePassword(state = any(), newPassword = any())
        } returns NativeAuthV2InteractionApiResult.PollInProgress(
            correlationId = correlationId,
            continuationState = pollState,
            retryAfterMillis = 0L
        )
        every {
            mockStrategy.performPoll(state = any())
        } returns NativeAuthV2InteractionApiResult.PollInProgress(
            correlationId = correlationId,
            continuationState = pollState,
            retryAfterMillis = 0L
        )

        val result = controller.submitNewPassword(parameters)

        assertTrue(result is NativeAuthV2CommandResult.PasswordResetFailed)
        result as NativeAuthV2CommandResult.PasswordResetFailed
        assertEquals(correlationId, result.correlationId)
        assertEquals("poll_timeout", result.error)
        verify(exactly = NativeAuthV2FlowController.MAX_POLL_ATTEMPTS) {
            mockStrategy.performPoll(state = any())
        }
    }

    // -----------------------------------------------------------------------------------------
    // signInAfterResetPassword — the new explicit token-exchange entry point
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSignInAfterResetPasswordReturnsRedirectWhenAuthorizeChallengeContinueRedirects() {
        val state = mockContinuationState()
        val parameters = signInAfterResetPasswordParameters(state)

        every {
            mockStrategy.performAuthorizeChallengeContinue(state = any())
        } returns AuthorizeChallengeApiResult.Redirect(
            correlationId = correlationId,
            redirectReason = "browser_required"
        )

        val result = controller.signInAfterResetPassword(parameters)

        assertTrue(result is INativeAuthCommandResult.Redirect)
        assertEquals(correlationId, result.correlationId)
    }

    @Test
    fun testSignInAfterResetPasswordReturnsAPIErrorWhenAuthorizeChallengeContinueFails() {
        val state = mockContinuationState()
        val parameters = signInAfterResetPasswordParameters(state)

        every {
            mockStrategy.performAuthorizeChallengeContinue(state = any())
        } returns AuthorizeChallengeApiResult.UnknownError(
            correlationId = correlationId,
            error = "invalid_state",
            errorDescription = "The continuation state is no longer valid."
        )

        val result = controller.signInAfterResetPassword(parameters)

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals(correlationId, result.correlationId)
        assertEquals("invalid_state", result.error)
    }

    @Test
    fun testSignInAfterResetPasswordReturnsAPIErrorWhenAuthorizeChallengeContinueRequiresContinuation() {
        val inputState = mockContinuationState()
        val unexpectedState = mockContinuationState()
        val parameters = signInAfterResetPasswordParameters(inputState)

        every {
            mockStrategy.performAuthorizeChallengeContinue(state = any())
        } returns AuthorizeChallengeApiResult.ContinuationRequired(
            correlationId = correlationId,
            continuationState = unexpectedState
        )

        val result = controller.signInAfterResetPassword(parameters)

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals(correlationId, result.correlationId)
        assertEquals("unexpected_api_result", result.error)
        assertTrue(result.errorDescription.orEmpty().contains("ContinuationRequired"))
        assertTrue(result.errorDescription.orEmpty().contains("authorize-challenge continue"))
    }

    @Test
    fun testSignInAfterResetPasswordReturnsAPIErrorWhenTokenRequestFails() {
        val state = mockContinuationState()
        val parameters = signInAfterResetPasswordParameters(state)

        every {
            mockStrategy.performAuthorizeChallengeContinue(state = any())
        } returns AuthorizeChallengeApiResult.AuthorizationCode(
            correlationId = correlationId,
            code = "auth-code"
        )
        every {
            mockStrategy.performTokenRequest(
                code = any(),
                scopes = any(),
                correlationId = any(),
                claimsRequestJson = null
            )
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "invalid_grant",
            errorDescription = "The authorization code is no longer valid.",
            errorCodes = emptyList()
        )

        val result = controller.signInAfterResetPassword(parameters)

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals(correlationId, result.correlationId)
        assertEquals("invalid_grant", result.error)
    }

    @Test
    fun testSignInAfterResetPasswordUsesContinuationStateScopesForTokenRequest() {
        val state = mockContinuationState()
        val flowScopes = listOf("openid", "offline_access", "User.Read")

        every { state.scopesForTokenRequest() } returns flowScopes
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), any(), any(), null)
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "invalid_grant",
            errorDescription = "Expected test stop before cache save.",
            errorCodes = emptyList()
        )

        controller.signInAfterResetPassword(
            signInAfterResetPasswordParameters(state, listOf("Different.Scope"))
        )

        verify(exactly = 1) {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                scopes = flowScopes,
                correlationId = correlationId,
                claimsRequestJson = null
            )
        }
    }

    @Test
    fun testSignInAfterResetPasswordUsesCallerClaimsForTokenRequest() {
        val state = mockContinuationState()
        val flowScopes = listOf("openid", "offline_access")
        val claimsRequestJson = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""

        every { state.scopesForTokenRequest() } returns flowScopes
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), any(), any(), any())
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "invalid_grant",
            errorDescription = "Expected test stop before cache save.",
            errorCodes = emptyList()
        )

        controller.signInAfterResetPassword(
            signInAfterResetPasswordParameters(
                state = state,
                claimsRequestJson = claimsRequestJson
            )
        )

        verify(exactly = 1) {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                scopes = flowScopes,
                correlationId = correlationId,
                claimsRequestJson = claimsRequestJson
            )
        }
    }

    @Test
    fun testSignInAfterResetPasswordUsesFlowClaimsWhenCallerClaimsAreAbsent() {
        val state = mockContinuationState()
        val flowScopes = listOf("openid", "offline_access")
        val flowClaims = """{"id_token":{"auth_time":{"essential":true}}}"""

        every { state.scopesForTokenRequest() } returns flowScopes
        every { state.claimsRequestJsonForTokenRequest() } returns flowClaims
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), any(), any(), flowClaims)
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "invalid_grant",
            errorDescription = "Expected test stop before cache save.",
            errorCodes = emptyList()
        )

        controller.signInAfterResetPassword(signInAfterResetPasswordParameters(state))

        verify(exactly = 1) {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                scopes = flowScopes,
                correlationId = correlationId,
                claimsRequestJson = flowClaims
            )
        }
    }

    @Test
    fun testSignInAfterResetPasswordUsesContinuationStateScopesForCachePersistenceRequest() {
        val state = mockContinuationState()
        val flowScopes = listOf("openid", "offline_access", "User.Read")
        val cacheRequestSlot = slot<MicrosoftStsAuthorizationRequest>()
        val stopAfterCapture = RuntimeException("Stop after cache request capture.")
        val mockTokenCache =
            mockk<OAuth2TokenCache<MicrosoftStsOAuth2Strategy, MicrosoftStsAuthorizationRequest, MicrosoftStsTokenResponse>>()
        val mockTokenResponse = mockk<MicrosoftStsTokenResponse>(relaxed = true)

        every { state.scopesForTokenRequest() } returns flowScopes
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(
                code = any(),
                scopes = any(),
                correlationId = any(),
                claimsRequestJson = null
            )
        } returns SignInTokenApiResult.Success(
            correlationId = correlationId,
            tokenResponse = mockTokenResponse
        )
        every { mockStrategy.getAuthority() } returns "https://login.contoso.com/common"
        every {
            mockTokenCache.saveAndLoadAggregatedAccountData(any(), capture(cacheRequestSlot), any())
        } throws stopAfterCapture

        val parameters = signInAfterResetPasswordParameters(state, listOf("Different.Scope"))
            .toBuilder()
            .oAuth2TokenCache(mockTokenCache)
            .clientId("client-id")
            .callerPackageName("com.contoso.app")
            .callerSignature("signature")
            .build()

        val thrown = assertThrows(RuntimeException::class.java) {
            controller.signInAfterResetPassword(parameters)
        }

        assertEquals("Stop after cache request capture.", thrown.message)
        assertEquals(flowScopes.joinToString(" "), cacheRequestSlot.captured.scope)
    }
}
