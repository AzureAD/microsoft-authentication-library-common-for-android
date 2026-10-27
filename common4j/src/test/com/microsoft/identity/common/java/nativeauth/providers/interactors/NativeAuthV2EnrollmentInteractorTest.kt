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
package com.microsoft.identity.common.java.nativeauth.providers.interactors

import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.*
import com.microsoft.identity.common.java.nativeauth.providers.v2.*
import com.microsoft.identity.common.java.net.HttpClient
import com.microsoft.identity.common.java.net.HttpResponse
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient
import io.mockk.*
import org.junit.Assert.*
import org.junit.Test
import java.net.URL

class NativeAuthV2EnrollmentInteractorTest {
    private val client = mockk<UrlConnectionHttpClient>()
    private val parser = NativeAuthV2ResponseParser()
    private val interactor = NativeAuthV2Interactor(
        client, NativeAuthV2RequestProvider(NativeAuthOAuth2Configuration(
            URL("https://login.contoso.com/tenant"), "client", "oob", null
        )), NativeAuthV2ResponseHandler(), parser
    )
    private fun response(json: String) = NativeAuthV2HalApiResponse.from(HalResource.from(json), 200, "cid")
    private fun initial() = NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
        response("""{"continuationToken":"old","_links":{"update":{"href":"/tenant/api/v2.0/auth/update"}}}"""),
        "old", listOf("User.Read"), """{"id_token":{}}""",
        NativeAuthV2LinkRelation.RESET_PASSWORD, NativeAuthV2FlowScenario.RESET_PASSWORD
    )
    private fun registration() = parser.parseInteraction(response("""
        {"action":"enroll","continuationToken":"registration","_embedded":{"methods":[
        {"type":"phone","channels":["sms"],"_links":{"enroll":{"href":"/tenant/api/v2.0/auth/methods/phone?dc=test"}}}]}}
    """), initial()) as NativeAuthV2InteractionApiResult.RegistrationRequired

    @Test
    fun blankContactAndFabricatedMethodNeverReachNetwork() {
        val state = registration().continuationState
        listOf(" " to "phone:sms:0", "contact" to "forged", "contact" to "email:email:0").forEach { (contact, id) ->
            val result = interactor.performEnroll(state, id, contact)
            assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
            assertEquals("invalid_input", (result as NativeAuthV2InteractionApiResult.UnknownError).error)
        }
        verify { client wasNot Called }
    }

    @Test
    fun smsEnrollmentRiskAndActivationUseLatestTokenAndExactServerLinks() {
        val urls = mutableListOf<URL>()
        val bodies = mutableListOf<ByteArray>()
        every { client.method(HttpClient.HttpMethod.POST, capture(urls), any(), capture(bodies)) } returnsMany listOf(
            HttpResponse(200, """{"action":"riskverify","continuationToken":"risk",
                "_links":{"riskverify":{"href":"/tenant/api/v2.0/auth/risk?dc=test"}}}""", emptyMap()),
            HttpResponse(200, """{"action":"activate","continuationToken":"activate","hint":"masked","codeLength":6,
                "_links":{"activate":{"href":"/tenant/api/v2.0/auth/methods/phone/activate?dc=test"}}}""", emptyMap()),
            HttpResponse(200, """{"action":"update","continuationToken":"update",
                "_links":{"update":{"href":"/tenant/api/v2.0/auth/update?dc=test"}}}""", emptyMap())
        )
        val risk = interactor.performEnroll(registration().continuationState, "phone:sms:0", "+123456789")
            as NativeAuthV2InteractionApiResult.RiskVerificationRequired
        val activate = interactor.performRiskVerification(risk.continuationState)
            as NativeAuthV2InteractionApiResult.ActivationRequired
        val next = interactor.performActivate(activate.continuationState, "any-service-validated-code")
            as NativeAuthV2InteractionApiResult.UpdateRequired
        assertEquals(listOf(
            """{"continuationToken":"registration","phoneNumber":"+123456789","channel":"sms"}""",
            """{"continuationToken":"risk"}""",
            """{"continuationToken":"activate","otp":"any-service-validated-code"}"""
        ), bodies.map { String(it, Charsets.UTF_8) })
        assertEquals(listOf(
            "https://login.contoso.com/tenant/api/v2.0/auth/methods/phone?dc=test",
            "https://login.contoso.com/tenant/api/v2.0/auth/risk?dc=test",
            "https://login.contoso.com/tenant/api/v2.0/auth/methods/phone/activate?dc=test"
        ), urls.map { it.toString() })
        assertEquals(NativeAuthV2FlowScenario.RESET_PASSWORD, next.continuationState.flowScenario())
        assertEquals(listOf("User.Read"), next.continuationState.scopesForTokenRequest())
        assertFalse(next.continuationState.isResetPasswordUpdateSubmitted())
    }

    @Test
    fun actualPasswordUpdateBeforeEnrollmentSurvivesActivationAndSerialization() {
        every { client.method(HttpClient.HttpMethod.PUT, any(), any(), any()) } returns HttpResponse(200,
            """{"action":"enroll","continuationToken":"after-update","_embedded":{"methods":[
            {"type":"email","_links":{"enroll":{"href":"/tenant/api/v2.0/auth/email/enroll"}}}]}}""", emptyMap())
        val registration = interactor.performUpdatePassword(initial(), "new-password".toCharArray())
            as NativeAuthV2InteractionApiResult.RegistrationRequired
        assertTrue(registration.continuationState.isResetPasswordUpdateSubmitted())
        val activation = parser.parseInteraction(response("""
            {"action":"activate","continuationToken":"activation","hint":"masked","codeLength":6,
            "_links":{"activate":{"href":"/tenant/api/v2.0/auth/email/activate"}}}
        """), registration.continuationState) as NativeAuthV2InteractionApiResult.ActivationRequired
        val bytes = java.io.ByteArrayOutputStream().apply {
            java.io.ObjectOutputStream(this).use { it.writeObject(activation.continuationState) }
        }.toByteArray()
        val restored = java.io.ObjectInputStream(bytes.inputStream()).use {
            it.readObject() as NativeAuthV2ContinuationState
        }
        assertTrue(restored.isResetPasswordUpdateSubmitted())
        assertEquals("""{"id_token":{}}""", restored.claimsRequestJsonForTokenRequest())
        assertEquals("/tenant/api/v2.0/auth/email/activate", restored.href(NativeAuthV2LinkRelation.ACTIVATE))
    }
}
