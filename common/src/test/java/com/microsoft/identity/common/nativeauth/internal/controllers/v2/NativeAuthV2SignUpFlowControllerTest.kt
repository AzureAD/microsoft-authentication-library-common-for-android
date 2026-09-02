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
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterSignUpCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitAttributesCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2RequiredAttribute
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for the V2 sign-up orchestration added to [NativeAuthV2FlowController]: the upfront
 * attribute submission that follows sign-up start, the deferred submit-attributes step, and the
 * explicit sign-in-after-sign-up completion. The scenarios mirror the server traces captured in
 * `AI Docs/signup.txt` (email one-time-code verification, deferred password/attributes,
 * user-already-exists, attribute-validation errors) and match the equivalent iOS sign-up tests.
 *
 * [NativeAuthV2OAuth2Strategy] and [NativeAuthCIAMAuthority] are mocked so these tests exercise the
 * controller's branching only, never the HTTP or cache layers.
 */
class NativeAuthV2SignUpFlowControllerTest {

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
    // signUpStart
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSignUpStartSubmitsUpfrontAndSurfacesCodeRequired() {
        val entryState = mockContinuationState()
        val codeState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart(entryState) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = entryState,
                requiredAttributes = listOf(requiredAttribute("email"))
            )
        every { mockStrategy.performSubmitAttributes(entryState, any()) } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = codeState,
                challengeTargetLabel = "u***@contoso.com",
                challengeChannel = "email",
                codeLength = 6
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.CodeRequired)
        result as NativeAuthV2CommandResult.CodeRequired
        assertEquals(codeState, result.continuationState)
        assertEquals(6, result.codeLength)
        // The upfront submit must post email (and password), so the app never re-supplies them.
        verify(exactly = 1) { mockStrategy.performSubmitAttributes(entryState, any()) }
    }

    @Test
    fun testSignUpStartSurfacesUserAlreadyExists() {
        val entryState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart(entryState) } returns
            NativeAuthV2InteractionApiResult.UserAlreadyExists(
                correlationId = correlationId,
                error = "user_already_exists",
                errorDescription = "AADSTS1003037: account already exists.",
                errorCodes = listOf(1003037)
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.UserAlreadyExists)
        assertEquals(
            listOf(1003037),
            (result as NativeAuthV2CommandResult.UserAlreadyExists).errorCodes
        )
    }

    @Test
    fun testSignUpStartMapsInvalidAttributesToRetryableAttributesInvalid() {
        val entryState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart(entryState) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = entryState,
                requiredAttributes = listOf(requiredAttribute("email"))
            )
        every { mockStrategy.performSubmitAttributes(entryState, any()) } returns
            NativeAuthV2InteractionApiResult.InvalidAttributes(
                correlationId = correlationId,
                invalidAttributes = listOf("city"),
                error = "attribute_validation_failed",
                errorDescription = "AADSTS1002027: attribute validation failed.",
                errorCodes = listOf(1002027)
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.AttributesInvalid)
        result as NativeAuthV2CommandResult.AttributesInvalid
        assertEquals(listOf("city"), result.invalidAttributes)
        // The validation-error body carries no fresh continuation token, so the just-submitted
        // state must be reused for the retry.
        assertEquals(entryState, result.continuationState)
    }

    @Test
    fun testSignUpStartWithoutPasswordDefersToPasswordRequired() {
        val entryState = mockContinuationState()
        val passwordState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart(entryState) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = entryState,
                requiredAttributes = listOf(requiredAttribute("email"))
            )
        // The upfront submit (email only, no password) makes the server request the password next.
        every { mockStrategy.performSubmitAttributes(entryState, any()) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = passwordState,
                requiredAttributes = listOf(requiredAttribute("password"))
            )

        val result = controller.signUpStart(signUpStartParameters(password = null))

        assertTrue(result is NativeAuthV2CommandResult.PasswordRequired)
        assertEquals(
            passwordState,
            (result as NativeAuthV2CommandResult.PasswordRequired).continuationState
        )
    }

    @Test
    fun testSignUpStartMapsRedirect() {
        val entryState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart(entryState) } returns
            NativeAuthV2InteractionApiResult.Redirect(
                correlationId = correlationId,
                redirectReason = "fallback_required"
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is INativeAuthCommandResult.Redirect)
    }

    @Test
    fun testSignUpStartMapsAuthorizeChallengeRedirect() {
        every {
            mockStrategy.performAuthorizeChallengeStart(
                correlationId = any(),
                entryRelation = any(),
                scenario = any(),
                scopes = any(),
                claimsRequestJson = any()
            )
        } returns AuthorizeChallengeApiResult.Redirect(correlationId, "fallback_required")

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is INativeAuthCommandResult.Redirect)
        verify(exactly = 0) { mockStrategy.performSignUpStart(any()) }
    }

    // -----------------------------------------------------------------------------------------
    // submitAttributes
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSubmitAttributesCompletesWithSignInAfterSignUpRequired() {
        val state = mockContinuationState()
        val readyState = mockContinuationState()
        every { mockStrategy.performSubmitAttributes(state, any()) } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)

        val result = controller.submitAttributes(submitAttributesParameters(state))

        assertTrue(result is NativeAuthV2CommandResult.SignInAfterSignUpRequired)
        assertEquals(
            readyState,
            (result as NativeAuthV2CommandResult.SignInAfterSignUpRequired).continuationState
        )
    }

    @Test
    fun testSubmitAttributesMapsInvalidAttributesToRetryableStateForSameState() {
        val state = mockContinuationState()
        every { mockStrategy.performSubmitAttributes(state, any()) } returns
            NativeAuthV2InteractionApiResult.InvalidAttributes(
                correlationId = correlationId,
                invalidAttributes = listOf("city"),
                error = "attribute_validation_failed",
                errorDescription = "AADSTS1002027: attribute validation failed.",
                errorCodes = listOf(1002027)
            )

        val result = controller.submitAttributes(submitAttributesParameters(state))

        assertTrue(result is NativeAuthV2CommandResult.AttributesInvalid)
        result as NativeAuthV2CommandResult.AttributesInvalid
        assertEquals(listOf("city"), result.invalidAttributes)
        assertEquals(state, result.continuationState)
    }

    @Test
    fun testSubmitAttributesSurfacesFurtherAttributesRequired() {
        val state = mockContinuationState()
        val nextState = mockContinuationState()
        every { mockStrategy.performSubmitAttributes(state, any()) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = nextState,
                requiredAttributes = listOf(requiredAttribute("city"))
            )

        val result = controller.submitAttributes(submitAttributesParameters(state))

        assertTrue(result is NativeAuthV2CommandResult.AttributesRequired)
        assertEquals(
            nextState,
            (result as NativeAuthV2CommandResult.AttributesRequired).continuationState
        )
    }

    @Test
    fun testSubmitAttributesRejectsReRequestOfAlreadySubmittedAttribute() {
        val state = mockContinuationState()
        val nextState = mockContinuationState()
        every { nextState.hasSubmittedAttribute("city") } returns true
        every { mockStrategy.performSubmitAttributes(state, any()) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = nextState,
                requiredAttributes = listOf(requiredAttribute("city"))
            )

        val result = controller.submitAttributes(submitAttributesParameters(state))

        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    // -----------------------------------------------------------------------------------------
    // signInAfterSignUp
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSignInAfterSignUpMapsAuthorizeChallengeContinueError() {
        val state = mockContinuationState()
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.UnknownError(
                correlationId = correlationId,
                error = "expected_test_error",
                errorDescription = "Expected test error."
            )

        val result = controller.signInAfterSignUp(signInAfterSignUpParameters(state))

        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals(
            "expected_test_error",
            (result as INativeAuthCommandResult.APIError).error
        )
    }

    @Test
    fun testSignInAfterSignUpMapsRedirect() {
        val state = mockContinuationState()
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.Redirect(correlationId, "fallback_required")

        val result = controller.signInAfterSignUp(signInAfterSignUpParameters(state))

        assertTrue(result is INativeAuthCommandResult.Redirect)
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

    private fun requiredAttribute(name: String): NativeAuthV2RequiredAttribute =
        NativeAuthV2RequiredAttribute(name = name, type = "string", required = true)

    private fun mockContinuationState(id: String = correlationId): NativeAuthV2ContinuationState {
        val state = mockk<NativeAuthV2ContinuationState>(relaxed = true)
        every { state.correlationId } returns id
        every { state.scopesForTokenRequest() } returns emptyList()
        every { state.hasSubmittedAttribute(any()) } returns false
        return state
    }

    private fun signUpStartParameters(
        username: String = "user@contoso.com",
        password: CharArray? = "Password123!".toCharArray(),
        attributes: Map<String, String>? = null
    ): SignUpV2StartCommandParameters =
        SignUpV2StartCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .username(username)
            .password(password)
            .attributes(attributes)
            .build()

    private fun submitAttributesParameters(
        state: NativeAuthV2ContinuationState,
        attributes: Map<String, String> = mapOf("city" to "Redmond")
    ): NativeAuthV2SubmitAttributesCommandParameters =
        NativeAuthV2SubmitAttributesCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .continuationState(state)
            .attributes(attributes)
            .build()

    private fun signInAfterSignUpParameters(
        state: NativeAuthV2ContinuationState
    ): NativeAuthV2SignInAfterSignUpCommandParameters =
        NativeAuthV2SignInAfterSignUpCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .continuationState(state)
            .build()
}
