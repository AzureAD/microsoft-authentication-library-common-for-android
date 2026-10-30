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
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.nativeauth.authorities.NativeAuthCIAMAuthority
import com.microsoft.identity.common.java.nativeauth.commands.parameters.*
import com.microsoft.identity.common.java.nativeauth.controllers.results.INativeAuthCommandResult
import com.microsoft.identity.common.java.nativeauth.controllers.results.NativeAuthV2CommandResult
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthV2OAuth2Strategy
import com.microsoft.identity.common.java.nativeauth.providers.interactors.NativeAuthV2Interactor
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ContinuationState
import com.microsoft.identity.common.java.nativeauth.providers.responses.v2.NativeAuthV2ResponseParser
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

/** Chained controller/strategy/protocol regression; only HTTP and platform configuration are mocked. */
class NativeAuthV2SignUpEnrollmentResumeTest {
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
    private val requests = mutableListOf<ByteArray>()

    @Test
    fun verificationAndResendRetainUpfrontValuesUntilCollection() {
        stubSignUpResponses(
            verificationResponse("code"),
            verificationResponse("resent"),
            """{"action":"collectAttributes","continuationToken":"attributes",
                "attributes":[{"attributeId":"password","inputType":"password","required":true}],
                "_links":{"submitAttributes":{"href":"/tenant/api/v2.0/auth/signup/attributes"}}}""",
            """{"state":"continue","continuationToken":"done"}"""
        )
        val password = "original-password".toCharArray()
        val attributes = linkedMapOf("displayName" to "Original")
        val start = controller.signUpStart(configured(SignUpV2StartCommandParameters.builder())
            .username("original@contoso.com").password(password).attributes(attributes).build())
            as NativeAuthV2CommandResult.CodeRequired
        assertTrue(password.all { it == '\u0000' })
        password.fill('x')
        attributes["displayName"] = "Changed"
        assertEquals(1, requests.size)
        val entry = JsonParser.parseString(String(requests.single(), Charsets.UTF_8)).asJsonObject
        assertEquals(setOf("username", "continuationToken"), entry.keySet())
        assertEquals("original@contoso.com", entry["username"].asString)

        val resent = controller.resendCode(configured(NativeAuthV2ResendCodeCommandParameters.builder())
            .continuationState(start.continuationState).build()) as NativeAuthV2CommandResult.CodeRequired
        assertEquals(2, requests.size)
        val completed = controller.submitSignUpCode(configured(NativeAuthV2SubmitCodeCommandParameters.builder())
            .continuationState(resent.continuationState).code("123456").build())
            as NativeAuthV2CommandResult.SignInAfterSignUpRequired

        assertEquals(4, requests.size)
        val body = JsonParser.parseString(String(requests.last(), Charsets.UTF_8)).asJsonObject
        assertEquals("attributes", body["continuationToken"].asString)
        assertEquals("original-password", body["attributes"].asJsonObject["password"].asString)
        assertEquals("Original", body["attributes"].asJsonObject["displayName"].asString)
        assertFalse(body["attributes"].asJsonObject.has("email"))
        assertNull(completed.continuationState.pendingSignUpAttributesForSubmission())
        assertNull(completed.continuationState.pendingSignUpPasswordForSubmission())
        assertFalse(completed.continuationState.requiresSignUpPasswordRecollection())
        assertTrue(completed.continuationState.hasSubmittedAttribute("password"))
        assertFalse(completed.continuationState.hasSubmittedAttribute("email"))
        assertNull(start.continuationState.pendingSignUpPasswordForSubmission())
        assertNull(resent.continuationState.pendingSignUpPasswordForSubmission())
    }

    @Test
    fun noUpfrontValuesSurfaceRequiredInputWithoutEmptySubmission() {
        for (attribute in listOf("password", "displayName")) {
            for (verifyFirst in listOf(false, true)) {
                requests.clear()
                val collection = """{"action":"collectAttributes","continuationToken":"attributes",
                    "attributes":[{"attributeId":"$attribute","inputType":"text","required":true}],
                    "_links":{"submitAttributes":{"href":"/tenant/api/v2.0/auth/signup/attributes"}}}"""
                stubSignUpResponses(*(if (verifyFirst) {
                    arrayOf(verificationResponse("code"), collection)
                } else arrayOf(collection)))
                val start = controller.signUpStart(configured(SignUpV2StartCommandParameters.builder())
                    .username("original@contoso.com").build())
                val result: INativeAuthCommandResult = if (verifyFirst) {
                    controller.submitSignUpCode(configured(NativeAuthV2SubmitCodeCommandParameters.builder())
                        .continuationState((start as NativeAuthV2CommandResult.CodeRequired).continuationState)
                        .code("123456").build())
                } else start
                if (attribute == "password") {
                    assertTrue(result is NativeAuthV2CommandResult.PasswordRequired)
                } else {
                    assertTrue(result is NativeAuthV2CommandResult.AttributesRequired)
                }
                assertEquals(if (verifyFirst) 2 else 1, requests.size)
            }
        }
    }

    @Test
    fun emailCollectionIsRejectedBeforeAnyPendingValuesAreSubmitted() {
        for (verifyFirst in listOf(false, true)) {
            requests.clear()
            val collection = """{"action":"collectAttributes","continuationToken":"attributes",
                "attributes":[{"attributeId":"password","inputType":"password","required":true},
                    {"attributeId":"EMAIL","inputType":"text","required":true}],
                "_links":{"submitAttributes":{"href":"/tenant/api/v2.0/auth/signup/attributes"}}}"""
            stubSignUpResponses(*(if (verifyFirst) {
                arrayOf(verificationResponse("code"), collection)
            } else arrayOf(collection)))
            val start = controller.signUpStart(configured(SignUpV2StartCommandParameters.builder())
                .username("original@contoso.com").password("password".toCharArray())
                .attributes(mapOf("displayName" to "Original")).build())
            val result: INativeAuthCommandResult = if (verifyFirst) {
                controller.submitSignUpCode(configured(NativeAuthV2SubmitCodeCommandParameters.builder())
                    .continuationState((start as NativeAuthV2CommandResult.CodeRequired).continuationState)
                    .code("123456").build())
            } else start
            assertTrue(result is INativeAuthCommandResult.APIError)
            assertEquals("attribute_already_submitted", (result as INativeAuthCommandResult.APIError).error)
            assertEquals(if (verifyFirst) 2 else 1, requests.size)
        }
    }

    @Test
    fun unexpectedCompletionAtStartRemainsAnApiError() {
        stubSignUpResponses("""{"state":"continue","continuationToken":"unexpected"}""")
        val result = controller.signUpStart(configured(SignUpV2StartCommandParameters.builder())
            .username("original@contoso.com").build())

        assertTrue(result is INativeAuthCommandResult.APIError)
        assertEquals("unexpected_api_result", (result as INativeAuthCommandResult.APIError).error)
        assertEquals(1, requests.size)
    }

    @Test
    fun completionAfterVerificationWithoutCollectionDiscardsPendingValues() {
        stubSignUpResponses(
            verificationResponse("code"),
            """{"state":"continue","continuationToken":"done"}"""
        )
        val start = controller.signUpStart(configured(SignUpV2StartCommandParameters.builder())
            .username("original@contoso.com").password("password".toCharArray())
            .attributes(mapOf("displayName" to "Original")).build()) as NativeAuthV2CommandResult.CodeRequired
        val result = controller.submitSignUpCode(configured(NativeAuthV2SubmitCodeCommandParameters.builder())
            .continuationState(start.continuationState).code("123456").build())
        val state = (result as NativeAuthV2CommandResult.SignInAfterSignUpRequired).continuationState
        assertNull(state.pendingSignUpAttributesForSubmission())
        assertNull(state.pendingSignUpPasswordForSubmission())
        assertFalse(state.requiresSignUpPasswordRecollection())
        assertFalse(state.hasSubmittedAttribute("password"))
        assertNull(start.continuationState.pendingSignUpPasswordForSubmission())
        assertEquals(2, requests.size)
    }

    @Test
    fun usernameSentAtStartAndUpfrontAttributesResumeAfterEnrollmentWithDistinctContact() {
        stubFlow(requestsPassword = false)
        val activation = startAndEnroll(password = null)

        val result = controller.activate(NativeAuthV2ActivateCommandParameters(
            parameters(activation), activation, "otp"))

        assertTrue(result.toString(), result is NativeAuthV2CommandResult.SignInAfterSignUpRequired)
        val attributes = JsonParser().parse(String(requests.last(), Charsets.UTF_8))
            .asJsonObject.getAsJsonObject("attributes")
        assertFalse(attributes.has("email"))
        val start = JsonParser.parseString(String(requests.first(), Charsets.UTF_8)).asJsonObject
        assertEquals("original@contoso.com", start["username"].asString)
        assertFalse(start.has("attributes"))
        assertEquals("Original Name", attributes["displayName"].asString)
        assertFalse(attributes.has("password"))
        assertTrue(String(requests[1], Charsets.UTF_8).contains("enrollment@contoso.com"))
        val completedState = (result as NativeAuthV2CommandResult.SignInAfterSignUpRequired).continuationState
        assertFalse(completedState.hasSubmittedAttribute("email"))
        assertNull(completedState.pendingSignUpAttributesForSubmission())
    }

    @Test
    fun retainedAttributesRejectedAfterEmailVerificationKeepDiagnosticsAndRetryState() {
        val details = listOf(mapOf(
            "code" to "attributeValidationFailed", "attributeIds" to listOf("displayName"),
            "message" to "Choose a different display name"
        ))
        stubFlow(
            requestsPassword = false,
            verifyEmailAfterActivation = true,
            submitResponse = """{"error":{"code":"invalidRequest","message":"Attribute rejected",
                "traceId":"attribute-trace","timestamp":"attribute-time","error_codes":[1002027],
                "innerError":{"code":"attributeValidationError","details":[{
                    "code":"attributeValidationFailed","attributeIds":["displayName"],
                    "message":"Choose a different display name"}]}}}"""
        )
        val activation = startAndEnroll(password = null)
        val verification = controller.activate(NativeAuthV2ActivateCommandParameters(
            parameters(activation), activation, "activation-otp")) as NativeAuthV2CommandResult.CodeRequired
        val result: INativeAuthCommandResult = controller.submitSignUpCode(configured(NativeAuthV2SubmitCodeCommandParameters.builder())
            .continuationState(roundTrip(verification.continuationState)).code("email-otp").build())
        assertTrue(result.toString(), result is NativeAuthV2CommandResult.AttributesInvalid)
        result as NativeAuthV2CommandResult.AttributesInvalid
        assertEquals(listOf("displayName"), result.invalidAttributes)
        assertEquals("invalidRequest", result.error)
        assertEquals("Attribute rejected", result.errorDescription)
        assertEquals("attributeValidationError", result.subError)
        assertEquals("attribute-trace", result.traceId)
        assertEquals("attribute-time", result.timestamp)
        assertEquals(listOf(1002027), result.errorCodes)
        assertEquals(details, result.attributeValidationDetails)
        val automatic = JsonParser.parseString(String(requests.last(), Charsets.UTF_8)).asJsonObject
        assertEquals("attributes", automatic["continuationToken"].asString)
        assertFalse(automatic["attributes"].asJsonObject.has("email"))
        assertEquals("Original Name", automatic["attributes"].asJsonObject["displayName"].asString)
        val retryState = roundTrip(result.continuationState)
        assertFalse(retryState.hasSubmittedAttribute("email"))
        assertNull(retryState.pendingSignUpAttributesForSubmission())
        val retryUrl = slot<URL>()
        every { http.method(HttpClient.HttpMethod.POST, capture(retryUrl), any(), capture(requests)) } returns
            response("""{"state":"continue","continuationToken":"done"}""")
        val completed = controller.submitAttributes(configured(NativeAuthV2SubmitAttributesCommandParameters.builder())
            .continuationState(retryState).attributes(mapOf("displayName" to "Corrected")).build())
        assertTrue(completed is NativeAuthV2CommandResult.SignInAfterSignUpRequired)
        assertEquals("https://login.contoso.com/tenant/api/v2.0/auth/signup/attributes", retryUrl.captured.toString())
        val retry = JsonParser.parseString(String(requests.last(), Charsets.UTF_8)).asJsonObject
        assertEquals("attributes", retry["continuationToken"].asString)
        assertEquals("Corrected", retry["attributes"].asJsonObject["displayName"].asString)
        assertFalse(retry["attributes"].asJsonObject.has("email"))
    }

    @Test
    fun existingUserAfterEmailVerificationKeepsTypedErrorAndDiagnostics() {
        stubFlow(
            requestsPassword = false,
            verifyEmailAfterActivation = true,
            submitResponse = """{"error":{"code":"invalidRequest","message":"Account exists",
                "traceId":"exists-trace","timestamp":"exists-time","error_codes":[1002029],
                "innerError":{"code":"accountConflict","details":[{
                    "code":"userAlreadyExists","attributeIds":["email"]}]}}}"""
        )
        val activation = startAndEnroll(password = null)
        val verification = controller.activate(NativeAuthV2ActivateCommandParameters(
            parameters(activation), activation, "activation-otp")) as NativeAuthV2CommandResult.CodeRequired
        val result: INativeAuthCommandResult = controller.submitSignUpCode(configured(NativeAuthV2SubmitCodeCommandParameters.builder())
            .continuationState(verification.continuationState).code("email-otp").build())
        assertTrue(result.toString(), result is NativeAuthV2CommandResult.UserAlreadyExists)
        result as NativeAuthV2CommandResult.UserAlreadyExists
        assertEquals("invalidRequest", result.error)
        assertEquals("Account exists", result.errorDescription)
        assertEquals("accountConflict", result.subError)
        assertEquals("exists-trace", result.traceId)
        assertEquals("exists-time", result.timestamp)
        assertEquals(listOf(1002029), result.errorCodes)
        assertEquals(listOf("email"), result.attributeValidationDetails?.single()?.get("attributeIds"))
        assertEquals("userAlreadyExists", result.attributeValidationDetails?.single()?.get("code"))
        assertEquals(5, requests.size)
    }

    @Test
    fun serializedUpfrontPasswordIsRecollectedWithoutLosingProfileAttributes() {
        stubFlow(requestsPassword = true)
        val original = "original-secret-never-retained".toCharArray()
        val activation = startAndEnroll(original)
        assertTrue(original.all { it == '\u0000' })

        val result = controller.activate(NativeAuthV2ActivateCommandParameters(
            parameters(activation), activation, "otp"))

        assertTrue(result.toString(), result is NativeAuthV2CommandResult.PasswordRequired)
        val passwordState = roundTrip((result as NativeAuthV2CommandResult.PasswordRequired).continuationState)
        val invalid = controller.submitAttributes(configured(NativeAuthV2SubmitAttributesCommandParameters.builder())
            .continuationState(passwordState).attributes(mapOf("email" to "override@contoso.com")).build())
        assertTrue(invalid is com.microsoft.identity.common.java.nativeauth.controllers.results.SignUpCommandResult.InvalidAttributes)
        assertEquals(3, requests.size)
        val replacement = "recollected-password".toCharArray()
        val completed = controller.submitAttributes(configured(NativeAuthV2SubmitAttributesCommandParameters.builder())
            .continuationState(passwordState).attributes(emptyMap()).password(replacement).build())
        assertTrue(completed.toString(), completed is NativeAuthV2CommandResult.SignInAfterSignUpRequired)
        val body = JsonParser().parse(String(requests.last(), Charsets.UTF_8)).asJsonObject
        val attributes = body.getAsJsonObject("attributes")
        assertFalse(attributes.has("email"))
        assertEquals("Original Name", attributes["displayName"].asString)
        assertEquals("recollected-password", attributes["password"].asString)
        assertTrue(replacement.all { it == '\u0000' })
        assertFalse(requests.any { String(it, Charsets.UTF_8).contains("original-secret-never-retained") })
        assertNull((completed as NativeAuthV2CommandResult.SignInAfterSignUpRequired)
            .continuationState.pendingSignUpAttributesForSubmission())
    }

    @Test
    fun serializedUpfrontPasswordIsRecollectedEvenWhenFirstCollectionRequestsOnlyProfileAttributes() {
        stubFlow(requestsPassword = false)
        val activation = startAndEnroll("original-secret-never-retained".toCharArray())
        val result = controller.activate(NativeAuthV2ActivateCommandParameters(
            parameters(activation), activation, "otp"))
        assertTrue(result.toString(), result is NativeAuthV2CommandResult.PasswordRequired)
        assertEquals(3, requests.size)
    }

    private fun startAndEnroll(password: CharArray?): NativeAuthV2ContinuationState {
        val start = controller.signUpStart(configured(SignUpV2StartCommandParameters.builder())
            .username("original@contoso.com").attributes(mapOf("displayName" to "Original Name"))
            .password(password).build()) as NativeAuthV2CommandResult.StrongAuthRegistrationRequired
        val registration = roundTrip(start.continuationState)
        val enrolled = controller.enroll(NativeAuthV2EnrollCommandParameters(
            parameters(registration), registration, start.authMethods.single().id, "enrollment@contoso.com"
        )) as NativeAuthV2CommandResult.StrongAuthVerificationRequired
        return roundTrip(enrolled.continuationState)
    }

    private fun stubFlow(
        requestsPassword: Boolean,
        verifyEmailAfterActivation: Boolean = false,
        submitResponse: String = """{"state":"continue","continuationToken":"done"}"""
    ) {
        every { http.post(any(), any(), any()) } returns response("""
            {"continuationToken":"entry","_links":{"signUp":{"href":"/tenant/api/v2.0/auth/signup/start"}}}
        """)
        val passwordAttribute = if (requestsPassword) """,{"attributeId":"password","inputType":"password","required":true}""" else ""
        val emailVerification = if (verifyEmailAfterActivation) listOf(response("""
            {"action":"verify","continuationToken":"email-verification","hint":"original-mask",
             "type":"email","codeLength":6,"_links":{
               "verify":{"href":"/tenant/api/v2.0/auth/email/verify"},
               "resend":{"href":"/tenant/api/v2.0/auth/email/resend"}}}
        """)) else emptyList()
        every { http.method(HttpClient.HttpMethod.POST, any(), any(), capture(requests)) } returnsMany (listOf(
            response("""{"action":"enroll","continuationToken":"enroll","_embedded":{"methods":[
                {"type":"email","_links":{"enroll":{"href":"/tenant/api/v2.0/auth/email/enroll"}}}]}}"""),
            response("""{"action":"activate","continuationToken":"activate","hint":"masked","codeLength":6,
                "_links":{"activate":{"href":"/tenant/api/v2.0/auth/email/activate"}}}""")
        ) + emailVerification + listOf(
            response("""{"action":"collectAttributes","continuationToken":"attributes",
                "attributes":[{"attributeId":"displayName","inputType":"text","required":true}$passwordAttribute],
                "_links":{"submitAttributes":{"href":"/tenant/api/v2.0/auth/signup/attributes"}}}"""),
            response(submitResponse)
        ))
    }

    private fun response(body: String) = HttpResponse(200, body, emptyMap())

    private fun stubSignUpResponses(vararg bodies: String) {
        every { http.post(any(), any(), any()) } returns response("""
            {"continuationToken":"entry","_links":{"signUp":{"href":"/tenant/api/v2.0/auth/signup/start"}}}
        """)
        every { http.method(HttpClient.HttpMethod.POST, any(), any(), capture(requests)) } returnsMany
            bodies.map(::response)
    }

    private fun verificationResponse(token: String) = """
        {"action":"verify","continuationToken":"$token","hint":"original-mask",
         "type":"email","codeLength":6,"_links":{
           "verify":{"href":"/tenant/api/v2.0/auth/email/verify"},
           "resend":{"href":"/tenant/api/v2.0/auth/email/resend"}}}
    """

    private fun parameters(state: NativeAuthV2ContinuationState) =
        configured(NativeAuthV2SignInAfterSignUpCommandParameters.builder()).continuationState(state).build()
    private fun <B : BaseSignInTokenCommandParameters.BaseSignInTokenCommandParametersBuilder<*, *>> configured(builder: B): B {
        builder.authority(authority).clientId("client").correlationId("cid")
            .platformComponents(mockk<IPlatformComponents>(relaxed = true))
        return builder
    }
    private fun roundTrip(state: NativeAuthV2ContinuationState): NativeAuthV2ContinuationState {
        val bytes = ByteArrayOutputStream().apply {
            ObjectOutputStream(this).use { it.writeObject(state) }
        }.toByteArray()
        assertFalse(String(bytes, Charsets.ISO_8859_1).contains("original-secret-never-retained"))
        assertFalse(state.toString().contains("original@contoso.com"))
        assertFalse(state.toUnsanitizedString().contains("Original Name"))
        return ObjectInputStream(bytes.inputStream()).use { it.readObject() as NativeAuthV2ContinuationState }
    }
}
