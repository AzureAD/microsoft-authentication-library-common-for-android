// Copyright (c) Microsoft Corporation. All rights reserved.
// Licensed under the MIT License.
package com.microsoft.identity.common.java.nativeauth.providers.responses.v2

import com.microsoft.identity.common.java.nativeauth.providers.NativeAuthOAuth2Configuration
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2RequestProvider
import com.microsoft.identity.common.java.util.ObjectMapper
import org.junit.Assert.*
import org.junit.Test
import java.net.URL

class NativeAuthV2EnrollmentTest {
    private val parser = NativeAuthV2ResponseParser()

    private fun response(json: String) =
        NativeAuthV2HalApiResponse.from(HalResource.from(json), 200, "correlation")

    private fun initial() = NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
        response("""{"continuationToken":"old"}"""), "old", listOf("User.Read"),
        """{"id_token":{}}""", NativeAuthV2LinkRelation.SIGN_UP, NativeAuthV2FlowScenario.SIGN_UP
    ).withAdditionalSubmittedAttributes(listOf("email"))

    @Test
    fun enrollmentUsesSyntheticIdsAndExactLinksWithoutServerIds() {
        val result = parser.parseInteraction(response("""
            {"action":"enroll","continuationToken":"ct","_embedded":{"methods":[
              {"type":"totp"},
              {"type":"email","_links":{"enroll":{"href":"/email/enroll?dc=1"}}},
              {"type":"phone","channels":["sms"],"_links":{"enroll":{"href":"/phone/enroll?dc=2"}}}
            ]}}
        """), initial())
        assertTrue(result is NativeAuthV2InteractionApiResult.RegistrationRequired)
        result as NativeAuthV2InteractionApiResult.RegistrationRequired
        assertEquals(listOf("email:email:1", "phone:sms:2"), result.methods.map { it.id })
        assertEquals(listOf("email", "sms"), result.methods.map { it.type })
        assertEquals("/phone/enroll?dc=2", result.continuationState.withSelectedMethod("phone:sms:2")
            ?.href(NativeAuthV2LinkRelation.ENROLL))
        assertEquals("ct", result.continuationState.continuationToken)
        assertEquals(listOf("User.Read"), result.continuationState.scopesForTokenRequest())
        assertEquals("""{"id_token":{}}""", result.continuationState.claimsRequestJsonForTokenRequest())
        assertTrue(result.continuationState.hasSubmittedAttribute("email"))
    }

    @Test
    fun malformedSupportedMethodsFailAndUnsupportedMethodsAreSkipped() {
        listOf(
            """{"channels":["sms"]}""",
            """{"type":"email"}""",
            """{"type":"phone","channels":"sms"}""",
            """{"type":"phone","channels":["sms"],"_links":{"enroll":{"href":" "}}}""",
            """{"type":"phone","channels":["voice"],"_links":{"enroll":{"href":"/voice"}}}""",
            """{"type":"totp"}"""
        ).forEach { method ->
            assertTrue(parser.parseInteraction(response("""
                {"action":"enroll","continuationToken":"ct","_embedded":{"methods":[$method]}}
            """), initial()) is NativeAuthV2InteractionApiResult.UnknownError)
        }
    }

    @Test
    fun enrollmentRoundTripRetainsSelectionAndDoesNotReclassifyAuthenticationFactor() {
        val firstFactor = NativeAuthV2ContinuationState.next(initial(), response("""
            {"continuationToken":"first","challengeContext":{"authenticationFactor":"singleFactor"}}
        """))!!
        val result = parser.parseInteraction(response("""
            {"action":"enroll","continuationToken":"enroll","_embedded":{"methods":[
            {"type":"EMAIL","_links":{"enroll":{"href":"/tenant/api/v2.0/auth/email?dc=test"}}},
            {"type":"phone","channels":["voice"]}]}}
        """), firstFactor) as NativeAuthV2InteractionApiResult.RegistrationRequired
        assertEquals(listOf("email:email:0"), result.methods.map { it.id })
        val bytes = java.io.ByteArrayOutputStream().apply {
            java.io.ObjectOutputStream(this).use { it.writeObject(result.continuationState) }
        }.toByteArray()
        val restored = java.io.ObjectInputStream(bytes.inputStream()).use {
            it.readObject() as NativeAuthV2ContinuationState
        }
        assertTrue(restored.isFirstFactor)
        assertEquals("email", restored.enrollmentChannel("email:email:0"))
        assertNull(restored.withSelectedMethod("phone:sms:1"))
        assertEquals("/tenant/api/v2.0/auth/email?dc=test",
            restored.withSelectedMethod("email:email:0")?.href(NativeAuthV2LinkRelation.ENROLL))
        assertTrue(restored.hasSubmittedAttribute("email"))
    }

    @Test
    fun activationRequiresExplicitLinkAndCodeMetadata() {
        val valid = """{"action":"activate","continuationToken":"new","hint":"masked","codeLength":6,
            "_links":{"activate":{"href":"/activate?dc=1"}}}"""
        val result = parser.parseInteraction(response(valid), initial())
        assertTrue(result is NativeAuthV2InteractionApiResult.ActivationRequired)
        result as NativeAuthV2InteractionApiResult.ActivationRequired
        assertEquals("/activate?dc=1", result.continuationState.href(NativeAuthV2LinkRelation.ACTIVATE))
        listOf(valid.replace("activate\":{\"href", "verify\":{\"href"),
            valid.replace("\"codeLength\":6", "\"codeLength\":0"),
            valid.replace("\"hint\":\"masked\",", "")
        ).forEach {
            assertTrue(parser.parseInteraction(response(it), initial()) is NativeAuthV2InteractionApiResult.UnknownError)
        }
    }

    @Test
    fun payloadsUseLatestTokenAndOnlyChannelSpecificContactFields() {
        val provider = NativeAuthV2RequestProvider(NativeAuthOAuth2Configuration(
            authorityUrl = URL("https://login.contoso.com/tenant"), clientId = "client",
            challengeType = "oob", capabilities = null
        ))
        val registration = parser.parseInteraction(response("""
            {"action":"enroll","continuationToken":"newest","_embedded":{"methods":[
              {"type":"email","_links":{"enroll":{"href":"/tenant/api/v2.0/auth/email/enroll"}}},
              {"type":"phone","channels":["sms"],"_links":{"enroll":{"href":"/tenant/api/v2.0/auth/phone/enroll"}}}
            ]}}
        """), initial()) as NativeAuthV2InteractionApiResult.RegistrationRequired
        val email = provider.createEnrollRequest(registration.continuationState, "email:email:0", "contact")
        val sms = provider.createEnrollRequest(registration.continuationState, "phone:sms:1", "contact")
        assertEquals("""{"continuationToken":"newest","email":"contact"}""",
            ObjectMapper.serializeObjectToJsonString(email.parameters))
        assertEquals("""{"continuationToken":"newest","phoneNumber":"contact","channel":"sms"}""",
            ObjectMapper.serializeObjectToJsonString(sms.parameters))
        assertFalse(email.toString().contains("contact"))
        assertFalse(email.toUnsanitizedString().contains("contact"))
        val activation = parser.parseInteraction(response("""
            {"action":"activate","continuationToken":"activation","hint":"masked","codeLength":6,
            "_links":{"activate":{"href":"/tenant/api/v2.0/auth/activate"}}}
        """), registration.continuationState) as NativeAuthV2InteractionApiResult.ActivationRequired
        val request = provider.createActivateRequest(activation.continuationState, "not-locally-validated")
        assertEquals("""{"continuationToken":"activation","otp":"not-locally-validated"}""",
            ObjectMapper.serializeObjectToJsonString(request.parameters))
        assertEquals("https://login.contoso.com/tenant/api/v2.0/auth/activate", request.requestUrl.toString())
    }
}
