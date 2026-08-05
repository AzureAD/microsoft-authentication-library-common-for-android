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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterResetPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitNewPasswordCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.ResetPasswordV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
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
 * [NativeAuthOAuth2Strategy] and [NativeAuthCIAMAuthority] are mocked so these tests exercise only
 * the controller's own branching/mapping logic, not the (unchanged) HTTP or cache layers
 * underneath `completeFlow`'s success path.
 */
class NativeAuthV2FlowControllerTest {

    private val correlationId = "test-correlation-id"

    private lateinit var mockAuthority: NativeAuthCIAMAuthority
    private lateinit var mockStrategy: NativeAuthOAuth2Strategy
    private lateinit var controller: NativeAuthV2FlowController

    @Before
    fun setup() {
        mockStrategy = mockk(relaxed = true)
        mockAuthority = mockk(relaxed = true)
        every { mockAuthority.createOAuth2Strategy(any()) } returns mockStrategy

        // No-op sleeper so the poll-completion test does not incur a real delay.
        controller = NativeAuthV2FlowController(sleeper = NativeAuthV2PollingSleeper { true })
    }

    private fun mockContinuationState(id: String = correlationId): NativeAuthV2ContinuationState {
        val state = mockk<NativeAuthV2ContinuationState>(relaxed = true)
        every { state.correlationId } returns id
        return state
    }

    // -----------------------------------------------------------------------------------------
    // ReadyToComplete -> SignInAfterResetPasswordRequired rerouting
    // -----------------------------------------------------------------------------------------

    @Test
    fun testResetPasswordStartReturnsSignInAfterResetPasswordRequiredWhenChallengeReadyToComplete() {
        val afterStartState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = mockk<ResetPasswordV2StartCommandParameters>(relaxed = true)
        every { parameters.getCorrelationId() } returns correlationId
        every { parameters.authority } returns mockAuthority
        every { parameters.username } returns "user@contoso.com"

        every {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = any(),
                entryRelation = any(),
                scenario = any(),
                scopes = any()
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
    fun testSubmitCodeReturnsSignInAfterResetPasswordRequiredWhenVerifyReadyToComplete() {
        val inputState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = mockk<NativeAuthV2SubmitCodeCommandParameters>(relaxed = true)
        every { parameters.getCorrelationId() } returns correlationId
        every { parameters.authority } returns mockAuthority
        every { parameters.continuationState } returns inputState
        every { parameters.code } returns "123456"

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
    fun testSubmitNewPasswordReturnsSignInAfterResetPasswordRequiredWhenUpdateReadyToComplete() {
        val inputState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = mockk<NativeAuthV2SubmitNewPasswordCommandParameters>(relaxed = true)
        every { parameters.getCorrelationId() } returns correlationId
        every { parameters.authority } returns mockAuthority
        every { parameters.continuationState } returns inputState
        every { parameters.newPassword } returns "newPassword1".toCharArray()

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
    fun testSubmitNewPasswordReturnsSignInAfterResetPasswordRequiredWhenPollReachesCompletion() {
        val inputState = mockContinuationState()
        val pollState = mockContinuationState()
        val readyState = mockContinuationState()
        val parameters = mockk<NativeAuthV2SubmitNewPasswordCommandParameters>(relaxed = true)
        every { parameters.getCorrelationId() } returns correlationId
        every { parameters.authority } returns mockAuthority
        every { parameters.continuationState } returns inputState
        every { parameters.newPassword } returns "newPassword1".toCharArray()

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

    // -----------------------------------------------------------------------------------------
    // signInAfterResetPassword — the new explicit token-exchange entry point
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSignInAfterResetPasswordReturnsRedirectWhenAuthorizeChallengeContinueRedirects() {
        val state = mockContinuationState()
        val parameters = mockk<NativeAuthV2SignInAfterResetPasswordCommandParameters>(relaxed = true)
        every { parameters.getCorrelationId() } returns correlationId
        every { parameters.authority } returns mockAuthority
        every { parameters.continuationState } returns state

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
        val parameters = mockk<NativeAuthV2SignInAfterResetPasswordCommandParameters>(relaxed = true)
        every { parameters.getCorrelationId() } returns correlationId
        every { parameters.authority } returns mockAuthority
        every { parameters.continuationState } returns state

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
    fun testSignInAfterResetPasswordReturnsAPIErrorWhenTokenRequestFails() {
        val state = mockContinuationState()
        val parameters = mockk<NativeAuthV2SignInAfterResetPasswordCommandParameters>(relaxed = true)
        every { parameters.getCorrelationId() } returns correlationId
        every { parameters.authority } returns mockAuthority
        every { parameters.continuationState } returns state

        every {
            mockStrategy.performAuthorizeChallengeContinue(state = any())
        } returns AuthorizeChallengeApiResult.AuthorizationCode(
            correlationId = correlationId,
            code = "auth-code"
        )
        every {
            mockStrategy.performTokenRequest(code = any(), scopes = any(), correlationId = any())
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
}
