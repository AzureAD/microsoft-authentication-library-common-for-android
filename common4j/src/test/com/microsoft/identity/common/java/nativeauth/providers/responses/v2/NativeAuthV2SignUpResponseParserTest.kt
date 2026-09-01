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
package com.microsoft.identity.common.java.nativeauth.providers.responses.v2

import com.microsoft.identity.common.java.nativeauth.providers.responses.ApiErrorResult
import com.microsoft.identity.common.java.nativeauth.providers.v2.NativeAuthV2FlowScenario
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the V2 sign-up additions to [NativeAuthV2ResponseParser]: the `collectAttributes` action,
 * the sign-up error table ([userAlreadyExists], [attributeValidationError]), the shared email
 * one-time-code verify contract, and ready-to-complete detection. The response bodies mirror the
 * captured server traces in `AI Docs/signup.txt`.
 */
class NativeAuthV2SignUpResponseParserTest {

    private val parser = NativeAuthV2ResponseParser()

    // region collect attributes

    @Test
    fun parseInteraction_whenSignUpStartCollectsAttributes_returnsAttributesRequiredWithNamesAndTypes() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "state": "interactionRequired",
                  "action": "collectAttributes",
                  "attributes": [
                    { "attributeId": "city", "inputType": "text", "required": true },
                    { "attributeId": "country", "inputType": "text", "required": false }
                  ],
                  "_links": { "submitAttributes": { "href": "/tenant/signup/submitattributes" } }
                }
                """.trimIndent()
            ),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.SIGN_UP_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.AttributesRequired)
        val attributesRequired = result as NativeAuthV2InteractionApiResult.AttributesRequired
        assertEquals(listOf("city", "country"), attributesRequired.requiredAttributes.map { it.name })
        assertEquals(listOf("text", "text"), attributesRequired.requiredAttributes.map { it.type })
        assertEquals(listOf(true, false), attributesRequired.requiredAttributes.map { it.required })
        // The submitAttributes href is retained so the controller can follow it.
        assertEquals(
            "/tenant/signup/submitattributes",
            attributesRequired.continuationState.href(NativeAuthV2LinkRelation.SUBMIT_ATTRIBUTES)
        )
    }

    @Test
    fun parseInteraction_whenCollectAttributesResponseLacksSubmitLink_returnsUnknownError() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "action": "collectAttributes",
                  "attributes": [ { "attributeId": "city", "inputType": "text", "required": true } ]
                }
                """.trimIndent()
            ),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.SIGN_UP_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        assertEquals(
            ApiErrorResult.INVALID_STATE,
            (result as NativeAuthV2InteractionApiResult.UnknownError).error
        )
    }

    @Test
    fun parseInteraction_whenAttributeIsMissingItsName_returnsUnknownError() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "action": "collectAttributes",
                  "attributes": [ { "inputType": "text", "required": true } ],
                  "_links": { "submitAttributes": { "href": "/tenant/signup/submitattributes" } }
                }
                """.trimIndent()
            ),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.SUBMIT_ATTRIBUTES
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
        assertEquals(
            ApiErrorResult.INVALID_STATE,
            (result as NativeAuthV2InteractionApiResult.UnknownError).error
        )
    }

    // endregion

    // region sign-up error table

    @Test
    fun parseInteraction_whenSignUpStartFindsExistingAccount_returnsUserAlreadyExists() {
        val result = parser.parseInteraction(
            response = responseFrom(USER_ALREADY_EXISTS_JSON),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.SIGN_UP_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UserAlreadyExists)
    }

    @Test
    fun parseInteraction_whenAttributeValidationFails_returnsInvalidAttributesWithRejectedNames() {
        val result = parser.parseInteraction(
            response = responseFrom(PASSWORD_POLICY_VIOLATION_JSON),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.SUBMIT_ATTRIBUTES
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.InvalidAttributes)
        assertEquals(
            listOf("password"),
            (result as NativeAuthV2InteractionApiResult.InvalidAttributes).invalidAttributes
        )
    }

    @Test
    fun parseInteraction_whenUserAlreadyExistsSurfacesOutsideSignUp_staysUnknownError() {
        // The userAlreadyExists detail is only actionable during sign-up. Any other operation must
        // not be reinterpreted as UserAlreadyExists.
        val result = parser.parseInteraction(
            response = responseFrom(USER_ALREADY_EXISTS_JSON),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.RESET_PASSWORD_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.UnknownError)
    }

    // endregion

    // region shared verify + ready to complete

    @Test
    fun parseInteraction_whenSignUpChallengeVerifiesEmail_returnsCodeRequired() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "action": "verify",
                  "codeLength": 8,
                  "type": "email",
                  "hint": "a***@contoso.com",
                  "_links": { "verify": { "href": "/tenant/signup/verify" } }
                }
                """.trimIndent()
            ),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.SIGN_UP_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.CodeRequired)
        val codeRequired = result as NativeAuthV2InteractionApiResult.CodeRequired
        assertEquals(8, codeRequired.codeLength)
        assertEquals("a***@contoso.com", codeRequired.challengeTargetLabel)
        assertEquals("email", codeRequired.challengeChannel)
    }

    @Test
    fun parseInteraction_whenSignUpReachesContinueState_returnsReadyToComplete() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-complete",
                  "state": "continue",
                  "_links": { "signIn": { "href": "/tenant/signin/start" } }
                }
                """.trimIndent()
            ),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.SUBMIT_ATTRIBUTES
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.ReadyToComplete)
    }

    @Test
    fun parseInteraction_whenSignUpResponseRequiresWebFallback_returnsRedirect() {
        val result = parser.parseInteraction(
            response = responseFrom(
                """
                {
                  "continuationToken": "ct-1",
                  "action": "collectAttributes",
                  "error": { "code": "redirect_to_web", "message": "Use the browser." }
                }
                """.trimIndent()
            ),
            previousState = signUpState(),
            operation = NativeAuthV2Operation.SIGN_UP_START
        )

        assertTrue(result is NativeAuthV2InteractionApiResult.Redirect)
        assertEquals(
            "redirect_to_web",
            (result as NativeAuthV2InteractionApiResult.Redirect).redirectReason
        )
    }

    // endregion

    private fun responseFrom(json: String): NativeAuthV2HalApiResponse =
        NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(json),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )

    private fun signUpState(): NativeAuthV2ContinuationState =
        NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = responseFrom("""{"continuationToken":"seed","signUp":"/tenant/signup/start"}"""),
            continuationToken = "seed",
            entryRelation = NativeAuthV2LinkRelation.SIGN_UP,
            scopes = listOf("openid", "offline_access"),
            scenario = NativeAuthV2FlowScenario.SIGN_UP
        )

    private companion object {
        private const val CORRELATION_ID = "corr-123"

        private val USER_ALREADY_EXISTS_JSON = """
            {
              "error": {
                "code": "invalidRequest",
                "message": "AADSTS1003037: It looks like you may already have an account with us using this email address.",
                "innerError": {
                  "details": [
                    { "attributeIds": ["email"], "code": "userAlreadyExists", "message": "An account with this identifier already exists." }
                  ]
                }
              }
            }
        """.trimIndent()

        private val PASSWORD_POLICY_VIOLATION_JSON = """
            {
              "error": {
                "code": "invalidRequest",
                "message": "AADSTS1002027: Some of the collected attributes were invalid.",
                "innerError": {
                  "code": "attributeValidationError",
                  "details": [
                    { "attributeIds": ["password"], "code": "passwordPolicyViolation", "message": "Password validation failed." }
                  ]
                }
              }
            }
        """.trimIndent()
    }
}
