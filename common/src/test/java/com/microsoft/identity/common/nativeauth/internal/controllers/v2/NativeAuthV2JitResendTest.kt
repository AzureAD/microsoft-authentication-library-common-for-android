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

import com.google.gson.JsonParser
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.nativeauth.commands.parameters.*
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.interactors.NativeAuthV2Interactor
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationStateTestFactory
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2LinkRelation
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2ResponseHandler
import com.microsoft.identity.common.java.net.HttpClient
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test
import java.io.*
import java.net.URL

/** Exercises controller, strategy, and protocol together; only HTTP and platform are mocked. */
class NativeAuthV2JitResendTest {
    private val http = mockk<UrlConnectionHttpClient>()
    private val config = NativeAuthOAuth2Configuration(
        URL("https://login.contoso.com/tenant"), "client", "oob", null)
    private val strategy = NativeAuthV2OAuth2Strategy(
        OAuth2StrategyParameters.builder().build(), config,
        NativeAuthV2Interactor(http, NativeAuthV2RequestProvider(config),
            NativeAuthV2ResponseHandler(), NativeAuthV2ResponseParser()))
    private val authority = mockk<NativeAuthCIAMAuthority>(relaxed = true).apply {
        every { createOAuth2StrategyV2(any()) } returns strategy
    }
    private val controller = NativeAuthV2FlowController()
    private val urls = mutableListOf<URL>()
    private val bodies = mutableListOf<ByteArray>()

    @Test
    fun repeatedResendsAndActivationUseRefreshedLinksAndTokensInEveryScenario() {
        NativeAuthV2FlowScenario.values().forEach { scenario ->
            urls.clear()
            bodies.clear()
            stubResponses(
                activation("first", "first"),
                activation("second", "second", codeLength = 8),
                activation("third", null, codeLength = 7, hint = ""","hint":"updated-mask""""),
                """{"error":{"code":"invalidGrant","innerError":{"code":"invalidOneTimeCode"}}}"""
            )
            val enrolled = enroll(initial(scenario))
            assertEquals("", enrolled.challengeTargetLabel)
            val first = roundTrip(enrolled.continuationState)
            val resent = controller.resendCode(resendParameters(first)) as NativeAuthV2CommandResult.StrongAuthVerificationRequired
            assertEquals(8, resent.codeLength)
            assertEquals("", resent.challengeTargetLabel)
            val second = roundTrip(resent.continuationState)
            val again = controller.resendCode(resendParameters(second)) as NativeAuthV2CommandResult.StrongAuthVerificationRequired
            assertEquals(7, again.codeLength)
            assertEquals("updated-mask", again.challengeTargetLabel)
            val third = roundTrip(again.continuationState)
            listOf(first, second, third).forEach {
                assertEquals(scenario, it.flowScenario())
                assertEquals(listOf("User.Read"), it.scopesForTokenRequest())
                assertEquals("""{"id_token":{}}""", it.claimsRequestJsonForTokenRequest())
                assertEquals(scenario == NativeAuthV2FlowScenario.RESET_PASSWORD, it.isResetPasswordUpdateSubmitted())
                if (scenario == NativeAuthV2FlowScenario.SIGN_UP) {
                    assertEquals(mapOf("email" to "original@contoso.com"), it.pendingSignUpAttributesForSubmission())
                    assertTrue(it.requiresSignUpPasswordRecollection())
                }
            }
            // The newest response removed resend: never reuse a previous response's link.
            assertMissingResend(third, ClientException.MISSING_PARAMETER)
            assertEquals(3, bodies.size)
            val invalidCode = controller.activate(NativeAuthV2ActivateCommandParameters(
                tokenParameters(third), third, " otp unchanged "))
            assertTrue(invalidCode is NativeAuthV2CommandResult.IncorrectCode)
            assertEquals("https://login.contoso.com/tenant/api/v2.0/auth/resend?step=first", urls[1].toString())
            assertEquals("https://login.contoso.com/tenant/api/v2.0/auth/resend?step=second", urls[2].toString())
            assertEquals("https://login.contoso.com/tenant/api/v2.0/auth/activate?step=third", urls[3].toString())
            assertEquals("""{"continuationToken":"first"}""", String(bodies[1], Charsets.UTF_8))
            assertEquals("""{"continuationToken":"second"}""", String(bodies[2], Charsets.UTF_8))
            val submitted = JsonParser.parseString(String(bodies[3], Charsets.UTF_8)).asJsonObject
            assertEquals("third", submitted["continuationToken"].asString)
            assertEquals(" otp unchanged ", submitted["otp"].asString)
        }
    }

    @Test
    fun absentOrBlankResendFailsBeforeHttpWithoutPreventingEnrollment() {
        listOf(null, "").forEach { resend ->
            bodies.clear()
            stubResponses(activation("first", resend))
            val state = enroll(initial(NativeAuthV2FlowScenario.SIGN_IN)).continuationState
            assertMissingResend(state, if (resend == null) ClientException.MISSING_PARAMETER else ClientException.MALFORMED_URL)
            assertEquals(1, bodies.size)
        }
    }

    @Test
    fun resendPreservesServiceErrorDiagnosticsAndBrowserRedirect() {
        stubResponses(
            activation("first", "first"),
            """{"error":{"code":"invalidRequest","message":"Try later","traceId":"trace",
                "timestamp":"time","innerError":{"code":"serviceSpecific"}}}""",
            """{"state":"webFallbackRequired","continuationToken":"redirect"}"""
        )
        val state = enroll(initial(NativeAuthV2FlowScenario.SIGN_IN)).continuationState
        val error = controller.resendCode(resendParameters(state)) as INativeAuthCommandResult.APIError
        assertEquals("invalidRequest", error.error)
        assertEquals("serviceSpecific", error.subError)
        assertEquals("trace", error.traceId)
        assertEquals("time", error.timestamp)
        assertTrue(controller.resendCode(resendParameters(state)) is INativeAuthCommandResult.Redirect)
    }

    @Test
    fun resendRejectsMalformedActivationAndDoesNotFollowUnexpectedRisk() {
        stubResponses(
            activation("first", "first"),
            activation("second", "second", codeLength = 0),
            """{"action":"riskverify","continuationToken":"risk",
                "_links":{"riskverify":{"href":"/tenant/risk"}}}"""
        )
        val state = enroll(initial(NativeAuthV2FlowScenario.SIGN_IN)).continuationState
        assertTrue(controller.resendCode(resendParameters(state)) is INativeAuthCommandResult.APIError)
        assertTrue(controller.resendCode(resendParameters(state)) is INativeAuthCommandResult.APIError)
        assertEquals(3, bodies.size)
    }

    private fun initial(scenario: NativeAuthV2FlowScenario): NativeAuthV2ContinuationState {
        val state = NativeAuthV2ContinuationStateTestFactory.create(
            "cid", listOf("User.Read"),
            entryRelation = when (scenario) {
                NativeAuthV2FlowScenario.SIGN_IN -> NativeAuthV2LinkRelation.SIGN_IN
                NativeAuthV2FlowScenario.SIGN_UP -> NativeAuthV2LinkRelation.SIGN_UP
                NativeAuthV2FlowScenario.RESET_PASSWORD -> NativeAuthV2LinkRelation.RESET_PASSWORD
            },
            scenario = scenario, claimsRequestJson = """{"id_token":{}}""")
        return when (scenario) {
            NativeAuthV2FlowScenario.SIGN_UP -> state.withPendingSignUpAttributes(
                mapOf("email" to "original@contoso.com"), true)
            NativeAuthV2FlowScenario.RESET_PASSWORD -> NativeAuthV2ContinuationStateTestFactory.afterPasswordUpdate(state)
            else -> state
        }
    }

    private fun enroll(state: NativeAuthV2ContinuationState): NativeAuthV2CommandResult.StrongAuthVerificationRequired {
        val registration = NativeAuthV2ContinuationStateTestFactory.registration(state)
        return controller.enroll(NativeAuthV2EnrollCommandParameters(
            tokenParameters(registration), registration, "phone:sms:0", "+15551234567"
        )) as NativeAuthV2CommandResult.StrongAuthVerificationRequired
    }

    private fun activation(token: String, resend: String?, codeLength: Int = 6, hint: String = ""): String {
        val resendLink = when (resend) {
            null -> ""
            "" -> ""","resend":{"href":" "}"""
            else -> ""","resend":{"href":"/tenant/api/v2.0/auth/resend?step=$resend"}"""
        }
        return """{"action":"activate","continuationToken":"$token","codeLength":$codeLength$hint,
            "_links":{"activate":{"href":"/tenant/api/v2.0/auth/activate?step=$token"}$resendLink}}"""
    }

    private fun stubResponses(vararg responses: String) {
        every { http.method(HttpClient.HttpMethod.POST, capture(urls), any(), capture(bodies)) } returnsMany
            responses.map { HttpResponse(200, it, emptyMap()) }
    }

    private fun assertMissingResend(state: NativeAuthV2ContinuationState, errorCode: String) {
        try {
            controller.resendCode(resendParameters(state))
            fail("Expected resend to fail before HTTP")
        } catch (e: ClientException) {
            assertEquals(errorCode, e.errorCode)
        }
    }

    private fun tokenParameters(state: NativeAuthV2ContinuationState) =
        configured(NativeAuthV2SignInAfterSignUpCommandParameters.builder()).continuationState(state).build()

    private fun resendParameters(state: NativeAuthV2ContinuationState) =
        configured(NativeAuthV2ResendCodeCommandParameters.builder()).continuationState(state).build()

    private fun <B : BaseSignInTokenCommandParameters.BaseSignInTokenCommandParametersBuilder<*, *>> configured(builder: B): B {
        builder.authority(authority).clientId("client").correlationId("cid")
            .platformComponents(mockk<IPlatformComponents>(relaxed = true))
        return builder
    }

    private fun roundTrip(state: NativeAuthV2ContinuationState): NativeAuthV2ContinuationState {
        val bytes = ByteArrayOutputStream().apply {
            ObjectOutputStream(this).use { it.writeObject(state) }
        }.toByteArray()
        return ObjectInputStream(bytes.inputStream()).use { it.readObject() as NativeAuthV2ContinuationState }
    }
}
