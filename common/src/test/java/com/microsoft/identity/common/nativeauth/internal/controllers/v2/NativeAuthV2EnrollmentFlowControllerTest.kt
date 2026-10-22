// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.common.nativeauth.internal.controllers.v2

import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.nativeauth.commands.parameters.*
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.*
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test

class NativeAuthV2EnrollmentFlowControllerTest {
    private val strategy = mockk<NativeAuthV2OAuth2Strategy>(relaxed = true)
    private val authority = mockk<NativeAuthCIAMAuthority>(relaxed = true).apply {
        every { createOAuth2StrategyV2(any()) } returns strategy
    }
    private val controller = NativeAuthV2FlowController()
    private fun state(scenario: NativeAuthV2FlowScenario = NativeAuthV2FlowScenario.SIGN_IN) =
        NativeAuthV2ContinuationStateTestFactory.create("cid", scenario = scenario)
    private fun registrationState() = NativeAuthV2ContinuationStateTestFactory.registration(state())
    private fun builder(state: NativeAuthV2ContinuationState) =
        NativeAuthV2SignInAfterSignUpCommandParameters.builder()
            .continuationState(state).authority(authority)
            .platformComponents(mockk<IPlatformComponents>(relaxed = true))
            .clientId("client").correlationId("cid").challengeType(listOf("oob"))
            .build()

    @Test
    fun enrollFollowsSmsRiskOnlyOnceAndUsesLatestState() {
        val registration = registrationState()
        val risk = state()
        val activation = state()
        every { strategy.performEnroll(registration, "phone:sms:0", "contact") } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired("cid", risk)
        every { strategy.performRiskVerification(risk) } returns
            NativeAuthV2InteractionApiResult.ActivationRequired("cid", activation, "masked", 6)
        val result = controller.enroll(NativeAuthV2EnrollCommandParameters(
            builder(registration), registration, "phone:sms:0", "contact"
        ))
        assertTrue(result is NativeAuthV2CommandResult.StrongAuthVerificationRequired)
        assertSame(activation, (result as NativeAuthV2CommandResult.StrongAuthVerificationRequired).continuationState)
        verify(exactly = 1) { strategy.performRiskVerification(risk) }
    }

    @Test
    fun repeatedRiskIsRejectedWithoutAnotherHop() {
        val registration = registrationState()
        val risk = state()
        every { strategy.performEnroll(any(), any(), any()) } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired("cid", risk)
        every { strategy.performRiskVerification(risk) } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired("cid", state())
        assertTrue(controller.enroll(NativeAuthV2EnrollCommandParameters(
            builder(registration), registration, "phone:sms:0", "contact"
        )) is INativeAuthCommandResult.APIError)
        verify(exactly = 1) { strategy.performRiskVerification(any()) }
    }

    @Test
    fun emailEnrollmentRiskIsRejectedWithoutRiskRequest() {
        val registration = registrationState()
        every { strategy.performEnroll(registration, "email:email:1", "contact") } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired("cid", state())
        every { strategy.performRiskVerification(any()) } returns
            NativeAuthV2InteractionApiResult.ActivationRequired("cid", state(), "masked", 6)

        assertTrue(controller.enroll(NativeAuthV2EnrollCommandParameters(
            builder(registration), registration, "email:email:1", "contact"
        )) is INativeAuthCommandResult.APIError)
        verify(exactly = 0) { strategy.performRiskVerification(any()) }
    }

    @Test
    fun emailEnrollmentReturnsActivationWithoutRiskRequest() {
        val registration = registrationState()
        val activation = state()
        every { strategy.performEnroll(registration, "email:email:1", "contact") } returns
            NativeAuthV2InteractionApiResult.ActivationRequired("cid", activation, "masked", 6)

        val result = controller.enroll(NativeAuthV2EnrollCommandParameters(
            builder(registration), registration, "email:email:1", "contact"
        ))
        assertTrue(result is NativeAuthV2CommandResult.StrongAuthVerificationRequired)
        assertSame(activation, (result as NativeAuthV2CommandResult.StrongAuthVerificationRequired).continuationState)
        verify(exactly = 0) { strategy.performRiskVerification(any()) }
    }

    @Test
    fun unofferedSmsMethodCannotTriggerRiskRequest() {
        val registration = registrationState()
        every { strategy.performEnroll(registration, "phone:sms:99", "contact") } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired("cid", state())
        every { strategy.performRiskVerification(any()) } returns
            NativeAuthV2InteractionApiResult.ActivationRequired("cid", state(), "masked", 6)

        assertTrue(controller.enroll(NativeAuthV2EnrollCommandParameters(
            builder(registration), registration, "phone:sms:99", "contact"
        )) is INativeAuthCommandResult.APIError)
        verify(exactly = 0) { strategy.performRiskVerification(any()) }
    }

    @Test
    fun activationReturnsAnotherRegistrationInEveryOriginalScenario() {
        NativeAuthV2FlowScenario.values().forEach { scenario ->
            val prior = state(scenario)
            val next = state(scenario)
            every { strategy.performActivate(prior, "arbitrary-code") } returns
                NativeAuthV2InteractionApiResult.RegistrationRequired("cid", next, emptyList())
            val result = controller.activate(NativeAuthV2ActivateCommandParameters(builder(prior), prior, "arbitrary-code"))
            assertTrue(result is NativeAuthV2CommandResult.StrongAuthRegistrationRequired)
            assertSame(next, (result as NativeAuthV2CommandResult.StrongAuthRegistrationRequired).continuationState)
        }
    }

    @Test
    fun activationBeforePasswordUpdateCannotCompleteReset() {
        val prior = state(NativeAuthV2FlowScenario.RESET_PASSWORD)
        every { strategy.performActivate(prior, any()) } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete("cid", prior)
        assertTrue(controller.activate(NativeAuthV2ActivateCommandParameters(builder(prior), prior, "otp"))
            is INativeAuthCommandResult.APIError)
        verify(exactly = 0) { strategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun activationAfterPasswordUpdateReturnsResetCompletionWithoutAutomaticSignIn() {
        val prior = NativeAuthV2ContinuationStateTestFactory.afterPasswordUpdate(
            state(NativeAuthV2FlowScenario.RESET_PASSWORD))
        every { strategy.performActivate(prior, any()) } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete("cid", prior)
        val result = controller.activate(NativeAuthV2ActivateCommandParameters(builder(prior), prior, "otp"))
        assertTrue(result is NativeAuthV2CommandResult.SignInAfterResetPasswordRequired)
        verify(exactly = 0) { strategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun activationResumesResetPasswordCollectionAndSignUpCompletion() {
        val reset = state(NativeAuthV2FlowScenario.RESET_PASSWORD)
        every { strategy.performActivate(reset, any()) } returns
            NativeAuthV2InteractionApiResult.UpdateRequired("cid", reset)
        assertTrue(controller.activate(NativeAuthV2ActivateCommandParameters(builder(reset), reset, "otp"))
            is NativeAuthV2CommandResult.NewPasswordRequired)
        val signup = state(NativeAuthV2FlowScenario.SIGN_UP)
        every { strategy.performActivate(signup, any()) } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete("cid", signup)
        assertTrue(controller.activate(NativeAuthV2ActivateCommandParameters(builder(signup), signup, "otp"))
            is NativeAuthV2CommandResult.SignInAfterSignUpRequired)
        verify(exactly = 0) { strategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun originalInteractionCommandsSurfaceRegistrationInsteadOfUnexpectedResult() {
        val prior = state()
        val registration = NativeAuthV2InteractionApiResult.RegistrationRequired("cid", prior,
            listOf(NativeAuthV2AuthMethod("email:email:0", "email", null)))
        every { strategy.performAuthorizeChallengeStart(any(), any(), any(), any(), any()) } returns
            AuthorizeChallengeApiResult.ContinuationRequired("cid", prior)
        every { strategy.performSignInStart(any(), any()) } returns registration
        every { strategy.performResetPasswordStart(any(), any()) } returns registration
        every { strategy.performSignUpStart(any()) } returns registration.copy(
            continuationState = state(NativeAuthV2FlowScenario.SIGN_UP))
        every { strategy.performVerify(any(), any()) } returns registration
        every { strategy.performPasswordVerify(any(), any()) } returns registration
        every { strategy.performSubmitAttributes(any(), any(), any()) } returns registration
        every { strategy.performUpdatePassword(any(), any()) } returns registration
        every { strategy.performMethodChallenge(any(), any()) } returns registration
        every { strategy.performResend(any()) } returns registration
        val code = configured(NativeAuthV2SubmitCodeCommandParameters.builder()
            .continuationState(prior).code("otp")).build()
        val results = listOf(
            controller.signInStart(configured(SignInV2StartCommandParameters.builder().username("user")).build()),
            controller.resetPasswordStart(configured(ResetPasswordV2StartCommandParameters.builder().username("user")).build()),
            controller.signUpStart(configured(SignUpV2StartCommandParameters.builder()
                .username("user").attributes(emptyMap())).build()),
            controller.submitSignInCode(code),
            controller.submitSignUpCode(code),
            controller.submitResetPasswordCode(code),
            controller.submitPassword(configured(NativeAuthV2SubmitPasswordCommandParameters.builder()
                .continuationState(prior).password("password".toCharArray())).build()),
            controller.submitAttributes(configured(NativeAuthV2SubmitAttributesCommandParameters.builder()
                .continuationState(prior).attributes(emptyMap())).build()),
            controller.submitNewPassword(configured(NativeAuthV2SubmitNewPasswordCommandParameters.builder()
                .continuationState(prior).newPassword("password".toCharArray())).build()),
            controller.selectMFAMethod(configured(NativeAuthV2SelectMFAMethodCommandParameters.builder()
                .continuationState(prior).methodId("email")).build()),
            controller.selectResetPasswordMethod(configured(NativeAuthV2SelectResetPasswordMethodCommandParameters.builder()
                .continuationState(prior).methodId("email")).build()),
            controller.submitMFAChallenge(configured(NativeAuthV2SubmitMFAChallengeCommandParameters.builder()
                .continuationState(prior).code("otp")).build()),
            controller.resendCode(configured(NativeAuthV2ResendCodeCommandParameters.builder()
                .continuationState(prior)).build())
        )
        results.forEachIndexed { index, it ->
            assertTrue(it.toString(), it is NativeAuthV2CommandResult.StrongAuthRegistrationRequired)
            val returnedState = (it as NativeAuthV2CommandResult.StrongAuthRegistrationRequired).continuationState
            if (index == 2) {
                assertEquals(NativeAuthV2FlowScenario.SIGN_UP, returnedState.flowScenario())
                assertEquals("user", returnedState.pendingSignUpAttributesForSubmission()?.get("email"))
            } else {
                assertSame(prior, returnedState)
            }
        }
    }

    @Test
    fun activationCompletesSignInUsingRetainedScopesAndClaims() {
        val claims = """{"id_token":{"auth_time":null}}"""
        val prior = NativeAuthV2ContinuationStateTestFactory.create(
            "cid", scenario = NativeAuthV2FlowScenario.SIGN_IN, claimsRequestJson = claims)
        every { strategy.performActivate(prior, "otp") } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete("cid", prior)
        every { strategy.performAuthorizeChallengeContinue(prior) } returns
            AuthorizeChallengeApiResult.AuthorizationCode("cid", "authorization-code")
        every { strategy.performTokenRequest(any(), any(), any(), any()) } returns
            com.microsoft.identity.common.java.nativeauth.providers.responses.signin.SignInTokenApiResult.Redirect(
                "cid", "stop-before-cache")
        val result = controller.activate(NativeAuthV2ActivateCommandParameters(builder(prior), prior, "otp"))
        assertTrue(result is INativeAuthCommandResult.Redirect)
        verify(exactly = 1) {
            strategy.performTokenRequest("authorization-code", match { "scope" in it && "openid" in it },
                "cid", claims)
        }
    }

    @Test
    fun registrationAfterFirstFactorChallengeAndResetPollingIsNotDropped() {
        val prior = state()
        val registration = NativeAuthV2InteractionApiResult.RegistrationRequired("cid", prior, emptyList())
        every { strategy.performAuthorizeChallengeStart(any(), any(), any(), any(), any()) } returns
            AuthorizeChallengeApiResult.ContinuationRequired("cid", prior)
        every { strategy.performSignInStart(any(), any()) } returns
            NativeAuthV2InteractionApiResult.ChallengeRequired("cid", prior, null,
                listOf(NativeAuthV2AuthMethod("email", "email", null)))
        every { strategy.performMethodChallenge(prior, "email") } returns registration
        assertTrue(controller.signInStart(configured(SignInV2StartCommandParameters.builder()
            .username("user")).build()) is NativeAuthV2CommandResult.StrongAuthRegistrationRequired)
        every { strategy.performUpdatePassword(any(), any()) } returns
            NativeAuthV2InteractionApiResult.PollInProgress("cid", prior, 0)
        every { strategy.performPoll(prior) } returns registration
        assertTrue(controller.submitNewPassword(configured(NativeAuthV2SubmitNewPasswordCommandParameters.builder()
            .continuationState(prior).newPassword("password".toCharArray())).build())
            is NativeAuthV2CommandResult.StrongAuthRegistrationRequired)
    }

    @Test
    fun resetPasswordPollingCanRequireMfaBeforeExplicitSignIn() {
        val updated = NativeAuthV2ContinuationStateTestFactory.afterPasswordUpdate(
            state(NativeAuthV2FlowScenario.RESET_PASSWORD))
        val methods = listOf(NativeAuthV2AuthMethod("sms-id", "sms", "***00"))
        val mfa = NativeAuthV2InteractionApiResult.MFARequired("cid", updated, methods)
        every { strategy.performUpdatePassword(any(), any()) } returns
            NativeAuthV2InteractionApiResult.PollInProgress("cid", updated, 0)
        every { strategy.performPoll(updated) } returns mfa
        val result = controller.submitNewPassword(configured(
            NativeAuthV2SubmitNewPasswordCommandParameters.builder()
                .continuationState(updated).newPassword("password".toCharArray())).build())
        assertEquals(NativeAuthV2CommandResult.MFARequired("cid", updated, methods), result)
        every { strategy.performMethodChallenge(updated, "sms-id") } returns
            NativeAuthV2InteractionApiResult.CodeRequired("cid", updated, "***00", "sms", 6)
        assertTrue(controller.selectMFAMethod(configured(
            NativeAuthV2SelectMFAMethodCommandParameters.builder()
                .continuationState(updated).methodId("sms-id")).build())
            is NativeAuthV2CommandResult.MFAVerificationRequired)
        every { strategy.performVerify(updated, "123456") } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete("cid", updated)
        assertEquals(NativeAuthV2CommandResult.SignInAfterResetPasswordRequired("cid", updated),
            controller.submitMFAChallenge(configured(
                NativeAuthV2SubmitMFAChallengeCommandParameters.builder()
                    .continuationState(updated).code("123456")).build()))
        verify(exactly = 0) { strategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun resetPasswordUpdateAndActivationCanRequireMfa() {
        val updated = NativeAuthV2ContinuationStateTestFactory.afterPasswordUpdate(
            state(NativeAuthV2FlowScenario.RESET_PASSWORD))
        val methods = listOf(NativeAuthV2AuthMethod("sms-id", "sms", "***00"))
        val mfa = NativeAuthV2InteractionApiResult.MFARequired("cid", updated, methods)
        every { strategy.performUpdatePassword(any(), any()) } returns mfa
        every { strategy.performActivate(updated, "123456") } returns mfa
        val expected = NativeAuthV2CommandResult.MFARequired("cid", updated, methods)
        assertEquals(expected, controller.submitNewPassword(configured(
            NativeAuthV2SubmitNewPasswordCommandParameters.builder()
                .continuationState(updated).newPassword("password".toCharArray())).build()))
        assertEquals(expected, controller.activate(
            NativeAuthV2ActivateCommandParameters(builder(updated), updated, "123456")))
        verify(exactly = 0) { strategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun resetMfaBeforePasswordUpdateReturnsPasswordCollectionButCannotComplete() {
        val prior = state(NativeAuthV2FlowScenario.RESET_PASSWORD)
        val parameters = configured(NativeAuthV2SubmitMFAChallengeCommandParameters.builder()
            .continuationState(prior).code("123456")).build()
        every { strategy.performVerify(prior, "123456") } returns
            NativeAuthV2InteractionApiResult.UpdateRequired("cid", prior)
        assertEquals(NativeAuthV2CommandResult.NewPasswordRequired("cid", prior),
            controller.submitMFAChallenge(parameters))
        every { strategy.performVerify(prior, "123456") } returns
            NativeAuthV2InteractionApiResult.ReadyToComplete("cid", prior)
        assertTrue(controller.submitMFAChallenge(parameters) is INativeAuthCommandResult.APIError)
        verify(exactly = 0) { strategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun resetPasswordPollingCanEnrollSmsOrEmailAndResumeAfterActivation() {
        val updated = NativeAuthV2ContinuationStateTestFactory.afterPasswordUpdate(
            state(NativeAuthV2FlowScenario.RESET_PASSWORD))
        listOf("sms", "email").forEach { channel ->
            val method = NativeAuthV2AuthMethod("method-$channel", channel, null)
            every { strategy.performUpdatePassword(any(), any()) } returns
                NativeAuthV2InteractionApiResult.PollInProgress("cid", updated, 0)
            every { strategy.performPoll(updated) } returns
                NativeAuthV2InteractionApiResult.RegistrationRequired("cid", updated, listOf(method))
            val registration = controller.submitNewPassword(configured(
                NativeAuthV2SubmitNewPasswordCommandParameters.builder()
                    .continuationState(updated).newPassword("password".toCharArray())).build())
                as NativeAuthV2CommandResult.StrongAuthRegistrationRequired
            assertEquals(listOf(method), registration.authMethods)
            assertSame(updated, registration.continuationState)
            every { strategy.performEnroll(updated, method.id, "contact") } returns
                NativeAuthV2InteractionApiResult.ActivationRequired("cid", updated, "masked", 6)
            val verification = controller.enroll(NativeAuthV2EnrollCommandParameters(
                builder(updated), registration.continuationState, method.id, "contact"))
                as NativeAuthV2CommandResult.StrongAuthVerificationRequired
            every { strategy.performActivate(updated, "123456") } returns
                NativeAuthV2InteractionApiResult.ReadyToComplete("cid", updated)
            assertEquals(NativeAuthV2CommandResult.SignInAfterResetPasswordRequired("cid", updated),
                controller.activate(NativeAuthV2ActivateCommandParameters(
                    builder(updated), verification.continuationState, "123456")))
        }
        verify(exactly = 2) { strategy.performPoll(updated) }
        verify(exactly = 0) { strategy.performAuthorizeChallengeContinue(any()) }
    }

    @Test
    fun activationPreservesServiceOtpErrorsAndRejectsUnsupportedActions() {
        val prior = state()
        every { strategy.performActivate(prior, "") } returns
            NativeAuthV2InteractionApiResult.InvalidCode(
                "cid", "invalid_code", "Retry.", "invalid_oob_value",
                traceId = "activation-trace", timestamp = "activation-time"
            )
        val invalidCode = controller.activate(NativeAuthV2ActivateCommandParameters(builder(prior), prior, ""))
        assertTrue(invalidCode is NativeAuthV2CommandResult.IncorrectCode)
        assertEquals("activation-trace", invalidCode.traceId)
        assertEquals("activation-time", invalidCode.timestamp)
        every { strategy.performActivate(prior, "otp") } returns
            NativeAuthV2InteractionApiResult.RiskVerificationRequired("cid", prior)
        assertTrue(controller.activate(NativeAuthV2ActivateCommandParameters(builder(prior), prior, "otp"))
            is INativeAuthCommandResult.APIError)
        verify(exactly = 0) { strategy.performRiskVerification(any()) }
    }

    @Test
    fun enrollmentAndActivationPreserveBrowserRedirectDiagnostics() {
        val prior = registrationState()
        val redirect = NativeAuthV2InteractionApiResult.Redirect(
            "cid", "Use browser", errorCodes = listOf(42),
            traceId = "redirect-trace", timestamp = "redirect-time"
        )
        every { strategy.performEnroll(prior, "phone:sms:0", "contact") } returns redirect
        every { strategy.performActivate(prior, "otp") } returns redirect
        val results = listOf(
            controller.enroll(NativeAuthV2EnrollCommandParameters(builder(prior), prior, "phone:sms:0", "contact")),
            controller.activate(NativeAuthV2ActivateCommandParameters(builder(prior), prior, "otp"))
        )
        results.forEach { result ->
            assertTrue(result is INativeAuthCommandResult.Redirect)
            assertEquals("redirect-trace", result.traceId)
            assertEquals("redirect-time", result.timestamp)
            assertEquals(listOf(42), (result as INativeAuthCommandResult.Redirect).errorCodes)
        }
    }

    private fun <B : BaseSignInTokenCommandParameters.BaseSignInTokenCommandParametersBuilder<*, *>> configured(builder: B): B {
        builder.authority(authority).platformComponents(mockk<IPlatformComponents>(relaxed = true)).correlationId("cid")
        return builder
    }
}
