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
import com.microsoft.identity.common.java.cache.ICacheRecord
import com.microsoft.identity.common.java.cache.MicrosoftStsAccountCredentialAdapter
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitCodeCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SignInAfterSignUpCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SubmitAttributesCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.SignUpV2StartCommandParameters
import com.microsoft.identity.common.java.nativeauth.commands.parameters.NativeAuthV2SelectAuthMethodCommandParameters
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResponse
import com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.AuthorizeChallengeApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationStateTestFactory
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2InteractionApiResult
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2RequiredAttribute
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationRequest
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenResponse
import com.microsoft.identity.common.java.providers.oauth2.OAuth2TokenCache
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
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
 * HTTP and cache persistence are mocked. Scope-target fallback is checked with the real
 * [MicrosoftStsAccountCredentialAdapter].
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

    @Test
    fun existingAccountChallenge_handlesRiskVerificationForAutomaticAndExplicitSelection() {
        val state = NativeAuthV2ContinuationStateTestFactory.challenge(
            NativeAuthV2ContinuationStateTestFactory.create(
                correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN),
            listOf(NativeAuthV2AuthMethod("email-id", "email", null)))
        val riskState = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN)
        stubAuthorizeChallengeStart(state)
        every { mockStrategy.performSignUpStart("user@contoso.com", state) } returns
            NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired(
                correlationId, state, listOf(NativeAuthV2AuthMethod("email-id", "email", null)))
        every { mockStrategy.performMethodChallenge(state, "email-id") } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired(correlationId, riskState)
        every { mockStrategy.performRiskVerification(riskState) } returns
            NativeAuthV2InteractionApiResult.CodeRequired(correlationId, riskState, "***", "email", 6)
        val expected = NativeAuthV2CommandResult.ExistingAccountCodeRequired(
            correlationId, riskState, 6, "***", "email")
        assertEquals(expected, controller.signUpStart(signUpStartParameters()))
        val parameters = NativeAuthV2SelectAuthMethodCommandParameters.builder()
            .authority(mockAuthority).platformComponents(mockPlatformComponents)
            .correlationId(correlationId).scopes(emptyList()).continuationState(state)
            .methodId("email-id").build()
        assertEquals(expected, controller.selectAuthMethod(parameters))
        verify(exactly = 2) { mockStrategy.performRiskVerification(riskState) }
    }

    // -----------------------------------------------------------------------------------------
    // signUpStart
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSignUpStartSubmitsUpfrontAndSurfacesCodeRequired() {
        val entryState = mockContinuationState()
        val codeState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart("user@contoso.com", entryState) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = entryState,
                requiredAttributes = listOf(requiredAttribute("password"))
            )
        every { mockStrategy.performSubmitAttributes(entryState, any(), any()) } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = codeState,
                challengeTargetLabel = "u***@contoso.com",
                challengeChannel = "email",
                codeLength = 6
            )

        val result = controller.signUpStart(signUpStartParameters(attributes = mapOf("city" to "Redmond")))

        assertTrue(result is NativeAuthV2CommandResult.CodeRequired)
        result as NativeAuthV2CommandResult.CodeRequired
        assertEquals(codeState, result.continuationState)
        assertEquals(6, result.codeLength)
        verify(exactly = 1) { mockStrategy.performSubmitAttributes(entryState, mapOf("city" to "Redmond"), null) }
    }

    @Test
    fun testSignUpStartRejectsNonEmailCodeRequired() {
        val entryState = mockContinuationState()
        val codeState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart("user@contoso.com", entryState) } returns
            NativeAuthV2InteractionApiResult.CodeRequired(
                correlationId = correlationId,
                continuationState = codeState,
                challengeTargetLabel = "***1234",
                challengeChannel = "sms",
                codeLength = 6
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals("unsupported_challenge_method", result.error)
        assertEquals(
            "Sign up currently supports email one-time-code verification only.",
            result.errorDescription
        )
    }

    @Test
    fun testSignUpStartSurfacesUserAlreadyExists() {
        val entryState = mockContinuationState()
        val details = listOf(mapOf(
            "code" to "userAlreadyExists",
            "attributeIds" to listOf("email"),
            "message" to "An account already exists for this email."
        ))
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart("user@contoso.com", entryState) } returns
            NativeAuthV2InteractionApiResult.UserAlreadyExists(
                correlationId = correlationId,
                error = "user_already_exists",
                errorDescription = "AADSTS1003037: account already exists.",
                errorCodes = listOf(1003037),
                subError = "attributeValidationError",
                traceId = "service-trace",
                timestamp = "service-time",
                attributeValidationDetails = details
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.UserAlreadyExists)
        result as NativeAuthV2CommandResult.UserAlreadyExists
        assertEquals(listOf(1003037), result.errorCodes)
        assertEquals("attributeValidationError", result.subError)
        assertEquals("service-trace", result.traceId)
        assertEquals("service-time", result.timestamp)
        assertEquals(details, result.attributeValidationDetails)
    }

    @Test
    fun testSignUpStartMapsInvalidAttributesToRetryableAttributesInvalid() {
        val entryState = mockContinuationState()
        val submittedState = mockContinuationState()
        val details = listOf(mapOf(
            "code" to "attributeRequired",
            "attributeIds" to listOf("city"),
            "message" to "Enter a city."
        ))
        every {
            entryState.withAdditionalSubmittedAttributes(
                match {
                    it.size == 1 &&
                        it.any { name -> name.equals("city", ignoreCase = true) }
                }
            )
        } returns submittedState
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart("user@contoso.com", entryState) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = entryState,
                requiredAttributes = listOf(requiredAttribute("password"))
            )
        every { mockStrategy.performSubmitAttributes(entryState, any(), any()) } returns
            NativeAuthV2InteractionApiResult.InvalidAttributes(
                correlationId = correlationId,
                invalidAttributes = listOf("city"),
                error = "attribute_validation_failed",
                errorDescription = "AADSTS1002027: attribute validation failed.",
                errorCodes = listOf(1002027),
                subError = "attributeValidationError",
                traceId = "service-trace",
                timestamp = "service-time",
                attributeValidationDetails = details
            )

        val result = controller.signUpStart(signUpStartParameters(attributes = mapOf("city" to "Redmond")))

        assertTrue(result is NativeAuthV2CommandResult.AttributesInvalid)
        result as NativeAuthV2CommandResult.AttributesInvalid
        assertEquals(listOf("city"), result.invalidAttributes)
        assertEquals("service-trace", result.traceId)
        assertEquals("service-time", result.timestamp)
        assertEquals("attributeValidationError", result.subError)
        assertEquals(details, result.attributeValidationDetails)
        // The validation-error body carries no fresh continuation token.
        assertSame(submittedState, result.continuationState)
    }

    @Test
    fun testSignUpStartPreservesUnknownErrorDiagnostics() {
        val entryState = mockContinuationState()
        val details = listOf(mapOf(
            "code" to "futureValidationCode",
            "attributeIds" to listOf("city"),
            "message" to "Unrecognized attribute diagnostic."
        ))
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart("user@contoso.com", entryState) } returns
            NativeAuthV2InteractionApiResult.UnknownError(
                correlationId = correlationId,
                error = "future_error",
                errorDescription = "Unrecognized service error.",
                errorCodes = listOf(90000),
                subError = "future_suberror",
                traceId = "service-trace",
                timestamp = "service-time",
                attributeValidationDetails = details
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals("future_error", result.error)
        assertEquals("Unrecognized service error.", result.errorDescription)
        assertEquals(listOf(90000), result.errorCodes)
        assertEquals(correlationId, result.correlationId)
        assertEquals("future_suberror", result.subError)
        assertEquals("service-trace", result.traceId)
        assertEquals("service-time", result.timestamp)
        assertEquals(details, result.attributeValidationDetails)
    }

    @Test
    fun testSignUpStartWithoutPasswordDefersToPasswordRequired() {
        val entryState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart("user@contoso.com", entryState) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = entryState,
                requiredAttributes = listOf(requiredAttribute("password"))
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is NativeAuthV2CommandResult.PasswordRequired)
        assertEquals(
            entryState,
            (result as NativeAuthV2CommandResult.PasswordRequired).continuationState
        )
        verify(exactly = 0) { mockStrategy.performSubmitAttributes(any(), any(), any()) }
    }

    @Test
    fun testSignUpStartMapsRedirect() {
        val entryState = mockContinuationState()
        stubAuthorizeChallengeStart(entryState)
        every { mockStrategy.performSignUpStart("user@contoso.com", entryState) } returns
            NativeAuthV2InteractionApiResult.Redirect(
                correlationId = correlationId,
                redirectReason = "fallback_required",
                errorCodes = listOf(123),
                traceId = "redirect-trace",
                timestamp = "redirect-time"
            )

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is INativeAuthCommandResult.Redirect)
        result as INativeAuthCommandResult.Redirect
        assertEquals(listOf(123), result.errorCodes)
        assertEquals("redirect-trace", result.traceId)
        assertEquals("redirect-time", result.timestamp)
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
        verify(exactly = 0) { mockStrategy.performSignUpStart(any(), any()) }
    }

    @Test
    fun testSignUpStartRejectsReservedAttributesBeforeNetworkRequest() {
        val result = controller.signUpStart(
            signUpStartParameters(
                attributes = linkedMapOf(
                    "EMAIL" to "other@contoso.com",
                    "Password" to "not-the-password"
                )
            )
        )

        assertTrue(result is SignUpCommandResult.InvalidAttributes)
        result as SignUpCommandResult.InvalidAttributes
        assertEquals("invalid_attributes", result.error)
        assertEquals(listOf("EMAIL", "Password"), result.invalidAttributes)
        assertTrue(result.errorDescription.contains("EMAIL, Password"))
        verify(exactly = 0) { mockAuthority.createOAuth2StrategyV2(any()) }
    }

    // -----------------------------------------------------------------------------------------
    // submitCode
    // -----------------------------------------------------------------------------------------

    @Test
    fun testSubmitSignUpCodeCompletesWithSignInAfterSignUpRequired() {
        val state = mockContinuationState()
        val readyState = mockContinuationState()
        every { mockStrategy.performVerify(state, any()) } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete(correlationId, readyState)

        val result = controller.submitSignUpCode(submitCodeParameters(state))

        assertTrue(result is NativeAuthV2CommandResult.SignInAfterSignUpRequired)
        assertEquals(
            readyState,
            (result as NativeAuthV2CommandResult.SignInAfterSignUpRequired).continuationState
        )
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
    fun testSubmitPasswordMapsInvalidAttributesToStateRecordingSubmittedPassword() {
        val state = mockContinuationState()
        val submittedState = mockContinuationState()
        every {
            state.withAdditionalSubmittedAttributes(
                match {
                    it.size == 1 &&
                        it.first().equals("password", ignoreCase = true)
                }
            )
        } returns submittedState
        every { mockStrategy.performSubmitAttributes(state, emptyMap(), any()) } returns
            NativeAuthV2InteractionApiResult.InvalidAttributes(
                correlationId = correlationId,
                invalidAttributes = listOf("password"),
                error = "attribute_validation_failed",
                errorDescription = "AADSTS1002027: attribute validation failed.",
                errorCodes = listOf(1002027)
            )

        val result = controller.submitAttributes(
            submitAttributesParameters(
                state = state,
                attributes = emptyMap(),
                password = "Password123!".toCharArray()
            )
        )

        assertTrue(result is NativeAuthV2CommandResult.AttributesInvalid)
        result as NativeAuthV2CommandResult.AttributesInvalid
        assertEquals(listOf("password"), result.invalidAttributes)
        assertSame(submittedState, result.continuationState)
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
    fun testSubmitAttributesAllowsReRequestOfOrdinaryPreviouslySubmittedAttribute() {
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

        assertTrue(result is NativeAuthV2CommandResult.AttributesRequired)
        assertEquals(
            nextState,
            (result as NativeAuthV2CommandResult.AttributesRequired).continuationState
        )
    }

    @Test
    fun testSubmitAttributesRejectsReRequestOfSubmittedEmail() {
        val state = mockContinuationState()
        val nextState = mockContinuationState()
        every { nextState.hasSubmittedAttribute("EMAIL") } returns true
        every { mockStrategy.performSubmitAttributes(state, any()) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = nextState,
                requiredAttributes = listOf(requiredAttribute("EMAIL"))
            )

        val result = controller.submitAttributes(submitAttributesParameters(state))

        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testSubmitAttributesRejectsReRequestOfSubmittedPassword() {
        val state = mockContinuationState()
        val nextState = mockContinuationState()
        every { nextState.hasSubmittedAttribute("PASSWORD") } returns true
        every { mockStrategy.performSubmitAttributes(state, any()) } returns
            NativeAuthV2InteractionApiResult.AttributesRequired(
                correlationId = correlationId,
                continuationState = nextState,
                requiredAttributes = listOf(requiredAttribute("PASSWORD"))
            )

        val result = controller.submitAttributes(submitAttributesParameters(state))

        assertTrue(result is INativeAuthCommandResult.APIError)
    }

    @Test
    fun testSubmitAttributesRejectsReservedAttributesBeforeNetworkRequest() {
        val password = "Password123!".toCharArray()
        val result = controller.submitAttributes(
            submitAttributesParameters(
                state = mockContinuationState(),
                password = password,
                attributes = linkedMapOf(
                    "email" to "other@contoso.com",
                    "PASSWORD" to "not-the-password"
                )
            )
        )

        assertTrue(result is SignUpCommandResult.InvalidAttributes)
        result as SignUpCommandResult.InvalidAttributes
        assertEquals("invalid_attributes", result.error)
        assertEquals(listOf("email", "PASSWORD"), result.invalidAttributes)
        assertTrue(result.errorDescription.contains("email, PASSWORD"))
        verify(exactly = 0) { mockAuthority.createOAuth2StrategyV2(any()) }
        assertTrue(password.all { it == '\u0000' })
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
                errorDescription = "Expected test error.",
                traceId = "authorize-trace",
                timestamp = "authorize-time"
            )

        val result = controller.signInAfterSignUp(signInAfterSignUpParameters(state))

        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals(
            "expected_test_error",
            (result as INativeAuthCommandResult.APIError).error
        )
        assertEquals("authorize-trace", result.traceId)
        assertEquals("authorize-time", result.timestamp)
    }

    @Test
    fun testSignInAfterSignUpMapsRedirect() {
        val state = mockContinuationState()
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.Redirect(correlationId, "fallback_required")

        val result = controller.signInAfterSignUp(signInAfterSignUpParameters(state))

        assertTrue(result is INativeAuthCommandResult.Redirect)
    }

    @Test
    fun testSignUpRejectsAuthorizationCodeBeforeExplicitCompletionSignIn() {
        every { mockStrategy.performAuthorizeChallengeStart(any(), any(), any(), any(), any()) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "premature-code")

        val result = controller.signUpStart(signUpStartParameters())

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals("unexpected_api_result", result.error)
        assertEquals(correlationId, result.correlationId)
        verify(exactly = 0) {
            mockStrategy.performSignUpStart(any(), any())
            mockStrategy.performAuthorizeChallengeContinue(any())
            mockStrategy.performTokenRequest(any(), any())
        }
    }

    @Test
    fun testSignInAfterSignUpExchangesAuthorizationCodeSavesTokensAndCompletes() {
        val state = mockContinuationState()
        val tokenCache = mockTokenCache()
        val tokenResponse = mockk<MicrosoftStsTokenResponse>(relaxed = true)
        val cacheRecord = mockk<ICacheRecord>(relaxed = true)
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                correlationId = correlationId
            )
        } returns SignInTokenApiResult.Success(correlationId, tokenResponse)
        every { mockStrategy.getAuthority() } returns "https://login.contoso.com/common"
        every {
            tokenCache.saveAndLoadAggregatedAccountData(any(), any(), tokenResponse)
        } returns listOf(cacheRecord)

        val result = controller.signInAfterSignUp(
            signInAfterSignUpParameters(state, tokenCache = tokenCache)
        )

        assertTrue(result is NativeAuthV2CommandResult.Complete)
        result as NativeAuthV2CommandResult.Complete
        assertEquals(correlationId, result.correlationId)
        assertEquals(listOf(cacheRecord), result.authenticationResult!!.cacheRecordWithTenantProfileData)
    }

    @Test
    fun testSignInAfterSignUpMapsTokenRequestFailure() {
        val state = mockContinuationState()
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                correlationId = correlationId
            )
        } returns SignInTokenApiResponse(
            statusCode = 400,
            correlationId = correlationId,
            continuationToken = null,
            error = "invalid_grant",
            errorDescription = "The authorization code is no longer valid.",
            errorCodes = emptyList(),
            errorUri = null,
            traceId = "token-trace",
            timestamp = "token-time",
            subError = "future_suberror"
        ).toErrorResult()

        val result = controller.signInAfterSignUp(signInAfterSignUpParameters(state))

        assertTrue(result is INativeAuthCommandResult.APIError)
        result as INativeAuthCommandResult.APIError
        assertEquals("invalid_grant", result.error)
        assertEquals("token-trace", result.traceId)
        assertEquals("token-time", result.timestamp)
        assertEquals("future_suberror", result.subError)
    }

    @Test
    fun testSignInAfterSignUpIgnoresExplicitScopesWhenTokenResponseOmitsScope() {
        val state = mockContinuationState()
        val explicitScopes = listOf("Different.Scope")
        val expectedScopes = setOf("Retained.Scope", "openid", "offline_access", "profile")
        val cacheRequest = slot<MicrosoftStsAuthorizationRequest>()
        val tokenCache = mockTokenCache()
        val tokenResponse = mockk<MicrosoftStsTokenResponse>(relaxed = true)
        every { tokenResponse.scope } returns null
        every { tokenResponse.clientInfo } returns "eyJ1aWQiOiJ1aWQiLCJ1dGlkIjoidXRpZCJ9"
        every { state.scopesForTokenRequest() } returns listOf("Retained.Scope")
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                correlationId = correlationId
            )
        } returns SignInTokenApiResult.Success(correlationId, tokenResponse)
        every { mockStrategy.getAuthority() } returns "https://login.contoso.com/common"
        every {
            tokenCache.saveAndLoadAggregatedAccountData(any(), capture(cacheRequest), tokenResponse)
        } returns listOf(mockk(relaxed = true))

        controller.signInAfterSignUp(
            signInAfterSignUpParameters(
                state = state,
                scopes = explicitScopes,
                tokenCache = tokenCache
            )
        )

        assertEquals(expectedScopes, cacheRequest.captured.scope.split(" ").toSet())
        val accessToken = MicrosoftStsAccountCredentialAdapter().createAccessToken(
            mockStrategy, cacheRequest.captured, tokenResponse
        )
        assertEquals("Retained.Scope", accessToken.target)
    }

    @Test
    fun testSignInAfterSignUpRetainsEntryScopesWhenCompletionScopesAreNullOrEmpty() {
        val state = mockContinuationState()
        val retainedScopes = listOf("Retained.Scope", "OPENID", "PROFILE", "OFFLINE_ACCESS")
        val expectedScopes = setOf("Retained.Scope", "openid", "offline_access", "profile")
        val tokenCache = mockTokenCache()
        val tokenResponse = mockk<MicrosoftStsTokenResponse>(relaxed = true)
        every { tokenResponse.scope } returns null
        every { tokenResponse.clientInfo } returns "eyJ1aWQiOiJ1aWQiLCJ1dGlkIjoidXRpZCJ9"
        val cacheRequest = slot<MicrosoftStsAuthorizationRequest>()
        every { state.scopesForTokenRequest() } returns retainedScopes
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), any())
        } returns SignInTokenApiResult.Success(correlationId, tokenResponse)
        every { mockStrategy.getAuthority() } returns "https://login.contoso.com/common"
        every {
            tokenCache.saveAndLoadAggregatedAccountData(any(), capture(cacheRequest), tokenResponse)
        } returns listOf(mockk(relaxed = true))

        for (scopes in listOf<List<String>?>(null, emptyList())) {
            val result = controller.signInAfterSignUp(
                signInAfterSignUpParameters(state, scopes = scopes, tokenCache = tokenCache)
            )

            assertTrue(result is NativeAuthV2CommandResult.Complete)
            assertEquals(
                expectedScopes,
                cacheRequest.captured.scope.split(" ").toSet()
            )
            val accessToken = MicrosoftStsAccountCredentialAdapter().createAccessToken(
                mockStrategy, cacheRequest.captured, tokenResponse
            )
            assertEquals("Retained.Scope", accessToken.target)
        }
        verify(exactly = 2) {
            mockStrategy.performTokenRequest("auth-code", correlationId)
        }
    }

    @Test
    fun testSignInAfterSignUpExchangesOnlyCodeRegardlessOfCallerAndRetainedClaims() {
        val state = mockContinuationState()
        val retainedClaims = """{"id_token":{"auth_time":{"essential":true}}}"""
        every { state.claimsRequestJsonForTokenRequest() } returns retainedClaims
        stubTokenRequestCapture(state)

        for (claims in listOf(null, "   ", """{"access_token":{"xms_cc":{"values":["cp1"]}}}""")) {
            val result = controller.signInAfterSignUp(
                signInAfterSignUpParameters(state, claimsRequestJson = claims)
            )
            assertTrue(result is INativeAuthCommandResult.APIError)
        }

        verify(exactly = 3) {
            mockStrategy.performTokenRequest(
                code = "auth-code",
                correlationId = correlationId
            )
        }
    }

    @Test
    fun testSignInAfterSignUpThrowsWhenCacheReturnsNoRecords() {
        val state = mockContinuationState()
        val tokenCache = mockTokenCache()
        val tokenResponse = mockk<MicrosoftStsTokenResponse>(relaxed = true)
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), any())
        } returns SignInTokenApiResult.Success(correlationId, tokenResponse)
        every { mockStrategy.getAuthority() } returns "https://login.contoso.com/common"
        every {
            tokenCache.saveAndLoadAggregatedAccountData(any(), any(), tokenResponse)
        } returns emptyList()

        val thrown = assertThrows(ClientException::class.java) {
            controller.signInAfterSignUp(
                signInAfterSignUpParameters(state, tokenCache = tokenCache)
            )
        }

        assertEquals(ClientException.TOKEN_CACHE_SAVE_FAILED, thrown.errorCode)
    }

    @Test
    fun testSignUpEntryCanonicalizesOidcDefaultsWithoutChangingResourceScopes() {
        val requestedScopes = listOf("OPENID", "PROFILE", "OFFLINE_ACCESS", "User.Read", "user.read", "User.Read", "openid", "profile", "offline_access")
        val expectedScopes = listOf("openid", "profile", "offline_access", "User.Read", "user.read", "User.Read")
        every { mockStrategy.performAuthorizeChallengeStart(any(), any(), any(), any(), any()) } returns
            AuthorizeChallengeApiResult.UnknownError(correlationId, "test_stop", "test stop")
        controller.signUpStart(signUpStartParameters().toBuilder().scopes(requestedScopes).build())
        verify(exactly = 1) {
            mockStrategy.performAuthorizeChallengeStart(correlationId, any(), any(), expectedScopes, null)
        }
    }

    @Test
    fun testSignUpEntrySendsDefaultScopesAndClaimsForNullEmptyAndExplicitScopes() {
        every { mockStrategy.performAuthorizeChallengeStart(any(), any(), any(), any(), any()) } returns
            AuthorizeChallengeApiResult.UnknownError(correlationId, "test_stop", "test stop")
        for (scopes in listOf(null, emptyList(), listOf("User.Read"))) {
            controller.signUpStart(
                signUpStartParameters().toBuilder().scopes(scopes)
                    .claimsRequestJson("{\"id_token\":{}}").build()
            )
            verify(exactly = 1) {
                mockStrategy.performAuthorizeChallengeStart(
                    correlationId, any(), any(),
                    match { it.toSet() == (scopes.orEmpty() + listOf("openid", "profile", "offline_access")).toSet() },
                    "{\"id_token\":{}}"
                )
            }
            clearMocks(mockStrategy, answers = false)
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

    private fun requiredAttribute(name: String): NativeAuthV2RequiredAttribute =
        NativeAuthV2RequiredAttribute(name = name, type = "string", required = true)

    private fun mockContinuationState(id: String = correlationId): NativeAuthV2ContinuationState {
        val state = mockk<NativeAuthV2ContinuationState>(relaxed = true)
        every { state.matchesAuthenticationMethod(any(), any()) } answers {
            secondArg<String>().equals("email", ignoreCase = true) ||
                secondArg<String>().equals("password", ignoreCase = true)
        }
        every { state.correlationId } returns id
        every { state.scopesForTokenRequest() } returns emptyList()
        every { state.hasSubmittedAttribute(any()) } returns false
        every { state.withAdditionalSubmittedAttributes(any()) } returns state
        every { state.pendingSignUpAttributesForSubmission() } returns null
        every { state.withPendingSignUpAttributes(any()) } returns state
        every { state.withoutPendingSignUpAttributes() } returns state
        return state
    }

    private fun signUpStartParameters(
        username: String = "user@contoso.com",
        attributes: Map<String, String>? = null
    ): SignUpV2StartCommandParameters =
        SignUpV2StartCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .username(username)
            .attributes(attributes)
            .build()

    private fun submitAttributesParameters(
        state: NativeAuthV2ContinuationState,
        attributes: Map<String, String> = mapOf("city" to "Redmond"),
        password: CharArray? = null
    ): NativeAuthV2SubmitAttributesCommandParameters =
        NativeAuthV2SubmitAttributesCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(emptyList())
            .continuationState(state)
            .attributes(attributes)
            .password(password)
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

    private fun signInAfterSignUpParameters(
        state: NativeAuthV2ContinuationState,
        scopes: List<String>? = null,
        claimsRequestJson: String? = null,
        tokenCache: OAuth2TokenCache<MicrosoftStsOAuth2Strategy, MicrosoftStsAuthorizationRequest, MicrosoftStsTokenResponse>? = null
    ): NativeAuthV2SignInAfterSignUpCommandParameters =
        NativeAuthV2SignInAfterSignUpCommandParameters.builder()
            .authority(mockAuthority)
            .platformComponents(mockPlatformComponents)
            .correlationId(correlationId)
            .scopes(scopes)
            .claimsRequestJson(claimsRequestJson)
            .continuationState(state)
            .authenticationScheme(BearerAuthenticationSchemeInternal())
            .apply {
                if (tokenCache != null) {
                    oAuth2TokenCache(tokenCache)
                    clientId("client-id")
                    callerPackageName("com.contoso.app")
                    callerSignature("signature")
                }
            }
            .build()

    private fun mockTokenCache() =
        mockk<OAuth2TokenCache<MicrosoftStsOAuth2Strategy, MicrosoftStsAuthorizationRequest, MicrosoftStsTokenResponse>>()

    private fun stubTokenRequestCapture(state: NativeAuthV2ContinuationState) {
        every { mockStrategy.performAuthorizeChallengeContinue(state) } returns
            AuthorizeChallengeApiResult.AuthorizationCode(correlationId, "auth-code")
        every {
            mockStrategy.performTokenRequest(any(), any())
        } returns SignInTokenApiResult.UnknownError(
            correlationId = correlationId,
            error = "expected_test_stop",
            errorDescription = "Expected test stop before cache persistence.",
            errorCodes = emptyList()
        )
    }

    @Test
    fun existingAccountMultipleMethods_waitForSelectionAtEverySignUpStep() {
        val state = mockContinuationState()
        stubAuthorizeChallengeStart(state)
        for (methods in listOf(
            listOf(NativeAuthV2AuthMethod("email-id", "email", null), NativeAuthV2AuthMethod("password-id", "password", null)),
            listOf(NativeAuthV2AuthMethod("email-1", "email", null), NativeAuthV2AuthMethod("email-2", "email", null),
                NativeAuthV2AuthMethod("password-id", "password", null))
        )) {
            val challenge = NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired(
                correlationId, state, methods + NativeAuthV2AuthMethod("unsupported", "sms", null))
            every { mockStrategy.performSignUpStart("user@contoso.com", state) } returns challenge
            every { mockStrategy.performSubmitAttributes(state, any(), any()) } returns challenge
            every { mockStrategy.performVerify(state, any()) } returns challenge
            val expected = NativeAuthV2CommandResult.AuthMethodSelectionRequired(correlationId, state, methods)
            assertEquals(expected, controller.signUpStart(signUpStartParameters()))
            assertEquals(expected, controller.submitAttributes(submitAttributesParameters(state)))
            assertEquals(expected, controller.submitSignUpCode(submitCodeParameters(state)))
        }
        verify(exactly = 0) { mockStrategy.performMethodChallenge(any(), any()) }
        verify(exactly = 0) { mockStrategy.performPasswordVerify(any(), any()) }
        verify(exactly = 0) { mockStrategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun existingAccountSingleSupportedMethod_ignoresUnsupportedAlternatives() {
        val state = mockContinuationState()
        every { state.flowScenario() } returns NativeAuthV2FlowScenario.SIGN_IN
        stubAuthorizeChallengeStart(state)
        every { mockStrategy.performSignUpStart("user@contoso.com", state) } returns
            NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired(correlationId, state,
                listOf(NativeAuthV2AuthMethod("sms-id", "sms", null),
                    NativeAuthV2AuthMethod("password-id", "password", null)))
        every { mockStrategy.performMethodChallenge(state, "password-id") } returns
            NativeAuthV2InteractionApiResult.PasswordRequired(correlationId, state)
        assertEquals(NativeAuthV2CommandResult.ExistingAccountPasswordRequired(correlationId, state),
            controller.signUpStart(signUpStartParameters()))
        verify(exactly = 1) { mockStrategy.performMethodChallenge(state, "password-id") }
        verify(exactly = 0) { mockStrategy.performMethodChallenge(state, "sms-id") }
    }

    @Test
    fun existingAccountSelection_challengesChosenMethodAndPreservesOutcomes() {
        val state = mockContinuationState()
        every { state.flowScenario() } returns NativeAuthV2FlowScenario.SIGN_IN
        val code = NativeAuthV2InteractionApiResult.CodeRequired(correlationId, state, "***", "email", 6)
        val password = NativeAuthV2InteractionApiResult.PasswordRequired(correlationId, state)
        val enrollment = NativeAuthV2InteractionApiResult.RegistrationRequired(
            correlationId, state, listOf(NativeAuthV2AuthMethod("sms", "sms", null)))
        val redirect = NativeAuthV2InteractionApiResult.Redirect(
            correlationId = correlationId, redirectReason = "browser required")
        for ((method, response, expected) in listOf(
            Triple(NativeAuthV2AuthMethod("email-id", "email", null), code,
                NativeAuthV2CommandResult.ExistingAccountCodeRequired(correlationId, state, 6, "***", "email")),
            Triple(NativeAuthV2AuthMethod("password-id", "password", null), password,
                NativeAuthV2CommandResult.ExistingAccountPasswordRequired(correlationId, state)),
            Triple(NativeAuthV2AuthMethod("email-id", "email", null), enrollment,
                NativeAuthV2CommandResult.StrongAuthRegistrationRequired(correlationId, state, enrollment.methods)),
            Triple(NativeAuthV2AuthMethod("password-id", "password", null), redirect,
                INativeAuthCommandResult.Redirect(correlationId, "browser required"))
        )) {
            every { mockStrategy.performMethodChallenge(state, method.id) } returns response
            val parameters = NativeAuthV2SelectAuthMethodCommandParameters.builder()
                .authority(mockAuthority).platformComponents(mockPlatformComponents)
                .correlationId(correlationId).scopes(emptyList())
                .continuationState(state).methodId(method.id).build()
            assertEquals(expected, controller.selectAuthMethod(parameters))
        }
        verify(exactly = 2) { mockStrategy.performMethodChallenge(state, "email-id") }
        verify(exactly = 2) { mockStrategy.performMethodChallenge(state, "password-id") }
        verify(exactly = 0) { mockStrategy.performPasswordVerify(any(), any()) }
        verify(exactly = 0) { mockStrategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun existingAccountSelection_rejectsUnsupportedChallengeChannels() {
        val state = mockContinuationState()
        every { state.flowScenario() } returns NativeAuthV2FlowScenario.SIGN_IN
        every { mockStrategy.performMethodChallenge(state, any()) } returns
            NativeAuthV2InteractionApiResult.CodeRequired(correlationId, state, "***", "sms", 6)
        for (type in listOf("email", "password")) {
            val parameters = NativeAuthV2SelectAuthMethodCommandParameters.builder()
                .authority(mockAuthority).platformComponents(mockPlatformComponents)
                .correlationId(correlationId).scopes(emptyList()).continuationState(state)
                .methodId("$type-id").build()
            assertTrue(controller.selectAuthMethod(parameters) is INativeAuthCommandResult.APIError)
        }
        verify(exactly = 2) { mockStrategy.performMethodChallenge(state, any()) }
    }

    @Test
    fun existingAccountPassword_isDistinctAndRequiresExplicitPassword() {
        val state = mockContinuationState()
        every { state.flowScenario() } returns NativeAuthV2FlowScenario.SIGN_IN
        stubAuthorizeChallengeStart(state)
        every { mockStrategy.performSignUpStart("user@contoso.com", state) } returns
            NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired(correlationId, state,
                listOf(com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod(
                    "password-id", "password", null)))
        every { mockStrategy.performMethodChallenge(state, "password-id") } returns
            NativeAuthV2InteractionApiResult.PasswordRequired(correlationId, state)
        val result = controller.signUpStart(signUpStartParameters())
        assertEquals(NativeAuthV2CommandResult.ExistingAccountPasswordRequired(correlationId, state), result)
        verify(exactly = 0) { mockStrategy.performPasswordVerify(any(), any()) }
        verify(exactly = 0) { mockStrategy.performSubmitAttributes(any(), any(), any()) }
        verify(exactly = 0) { mockStrategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun existingAccountEmail_isDistinctAtStartAttributesAndCodeVerification() {
        val state = mockContinuationState()
        every { state.flowScenario() } returns NativeAuthV2FlowScenario.SIGN_IN
        val challenge = NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired(correlationId, state,
            listOf(com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod(
                "email-id", "email", null)))
        stubAuthorizeChallengeStart(state)
        every { mockStrategy.performSignUpStart("user@contoso.com", state) } returns challenge
        every { mockStrategy.performSubmitAttributes(state, any(), any()) } returns challenge
        every { mockStrategy.performVerify(state, any()) } returns challenge
        every { mockStrategy.performMethodChallenge(state, "email-id") } returns
            NativeAuthV2InteractionApiResult.CodeRequired(correlationId, state, "***@contoso.com", "email", 6)
        val expected = NativeAuthV2CommandResult.ExistingAccountCodeRequired(
            correlationId, state, 6, "***@contoso.com", "email")
        assertEquals(expected, controller.signUpStart(signUpStartParameters()))
        assertEquals(expected, controller.submitAttributes(submitAttributesParameters(state)))
        assertEquals(expected, controller.submitSignUpCode(submitCodeParameters(state)))
        verify(exactly = 3) { mockStrategy.performMethodChallenge(state, "email-id") }
        verify(exactly = 0) { mockStrategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun existingAccountChallenge_preservesEnrollmentInterruptionsFromEverySignUpStep() {
        val signUpState = mockContinuationState()
        val signInState = NativeAuthV2ContinuationStateTestFactory.create(
            correlationId, scenario = NativeAuthV2FlowScenario.SIGN_IN)
        val registrationState = NativeAuthV2ContinuationStateTestFactory.registration(signInState)
        val enrollmentMethods = listOf(NativeAuthV2AuthMethod("sms-id", "sms", null))
        stubAuthorizeChallengeStart(signUpState)

        for (type in listOf("email", "password")) {
            val challenge = NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired(
                correlationId, signInState, listOf(NativeAuthV2AuthMethod("$type-id", type, null)))
            every { mockStrategy.performSignUpStart("user@contoso.com", signUpState) } returns challenge
            every { mockStrategy.performSubmitAttributes(signUpState, any(), any()) } returns challenge
            every { mockStrategy.performVerify(signUpState, any()) } returns challenge
            every { mockStrategy.performMethodChallenge(signInState, "$type-id") } returns
                NativeAuthV2InteractionApiResult.RegistrationRequired(
                    correlationId, registrationState, enrollmentMethods)
            val expected = NativeAuthV2CommandResult.StrongAuthRegistrationRequired(
                correlationId, registrationState, enrollmentMethods)

            for (result in listOf(
                controller.signUpStart(signUpStartParameters()),
                controller.submitAttributes(submitAttributesParameters(signUpState)),
                controller.submitSignUpCode(submitCodeParameters(signUpState))
            )) {
                assertEquals(expected, result)
                assertEquals(NativeAuthV2FlowScenario.SIGN_IN,
                    (result as NativeAuthV2CommandResult.StrongAuthRegistrationRequired)
                        .continuationState.flowScenario())
            }
        }
        verify(exactly = 0) { mockStrategy.performPasswordVerify(any(), any()) }
        verify(exactly = 0) { mockStrategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun existingAccountEmail_acceptsMixedCaseChallengeChannels() {
        val state = mockContinuationState()
        every { state.flowScenario() } returns NativeAuthV2FlowScenario.SIGN_IN
        val challenge = NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired(
            correlationId, state, listOf(NativeAuthV2AuthMethod("email-id", "email", null)))
        stubAuthorizeChallengeStart(state)
        every { mockStrategy.performSignUpStart("user@contoso.com", state) } returns challenge

        for (channel in listOf("email", "Email", "EMAIL")) {
            every { mockStrategy.performMethodChallenge(state, "email-id") } returns
                NativeAuthV2InteractionApiResult.CodeRequired(
                    correlationId, state, "***@contoso.com", channel, 6)

            assertEquals(
                NativeAuthV2CommandResult.ExistingAccountCodeRequired(
                    correlationId, state, 6, "***@contoso.com", channel),
                controller.signUpStart(signUpStartParameters())
            )
        }
    }

    @Test
    fun existingAccountChallenge_doesNotAcceptMismatchedOrUnsupportedMethods() {
        val state = mockContinuationState()
        every { state.flowScenario() } returns NativeAuthV2FlowScenario.SIGN_IN
        stubAuthorizeChallengeStart(state)
        every { mockStrategy.performMethodChallenge(any(), any()) } returns
            NativeAuthV2InteractionApiResult.CodeRequired(correlationId, state, "***", "sms", 6)
        for (type in listOf("sms", "email", "password")) {
            every { mockStrategy.performSignUpStart("user@contoso.com", state) } returns
                NativeAuthV2InteractionApiResult.ExistingAccountChallengeRequired(correlationId, state,
                    listOf(com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2AuthMethod(
                        "$type-id", type, null)))
            assertTrue(controller.signUpStart(signUpStartParameters()) is INativeAuthCommandResult.APIError)
        }
        verify(exactly = 0) { mockStrategy.performMethodChallenge(any(), "sms-id") }
        verify(exactly = 0) { mockStrategy.performAuthorizeChallengeContinue(any()) }
    }
}
