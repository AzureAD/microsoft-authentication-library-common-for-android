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
package com.microsoft.identity.common.java.nativeauth.providers.requests.v2

import com.microsoft.identity.common.java.util.ObjectMapper
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2RequestSerializationTest {

    @Test
    fun authorizeChallenge_serializesEntryScopesAndClaimsWithoutLoggingClaims() {
        listOf(null, "", """{"id_token":{"test":null}}""").forEach { claims ->
            val request = AuthorizeChallengeStartRequest.create(
                clientId = CLIENT_ID,
                scopes = listOf("openid", "profile", "offline_access", "User.Read"),
                requestUrl = "https://contoso.com/oauth2/v2.0/authorize-challenge",
                headers = mapOf("Content-Type" to "application/x-www-form-urlencoded"),
                claimsRequestJson = claims
            )
            val form = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
            assertTrue(form.contains("scope=openid+profile+offline_access+User.Read"))
            assertEquals(!claims.isNullOrEmpty(), form.contains("claims="))
            assertFalse(request.toString().contains("id_token"))
            assertFalse(request.parameters.toString().contains("id_token"))
        }
    }

    @Test
    fun verifyRequest_sendsEmptyAndShortOtpWithoutLocalRejection() {
        listOf("", "1").forEach { otp ->
            val request = NativeAuthV2VerifyRequest.create(
                CONTINUATION_TOKEN, otp, "https://contoso.com/verify",
                mapOf("Content-Type" to "application/json")
            )
            val serialized = JSONObject(ObjectMapper.serializeObjectToJsonString(request.parameters))
            assertEquals(otp, serialized.getString("otp"))
            assertFalse(serialized.has("scope"))
            assertFalse(serialized.has("claims"))
        }
    }

    @Test
    fun authorizeChallenge_emptyScopes_omitsScope() {
        val request = AuthorizeChallengeStartRequest.create(
            clientId = CLIENT_ID,
            scopes = emptyList(),
            requestUrl = "https://contoso.com/oauth2/v2.0/authorize-challenge",
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
        )
        val form = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)
        assertFalse(form.contains("scope="))
    }

    @Test
    fun jsonRequestParameters_serializeExpectedWireFieldsWithoutClientId() {
        val cases = listOf(
            NativeAuthV2EntryRequest.NativeAuthV2EntryRequestParameters(
                clientId = CLIENT_ID,
                username = USERNAME,
                continuationToken = CONTINUATION_TOKEN
            ) to JSONObject()
                .put("username", USERNAME)
                .put("continuationToken", CONTINUATION_TOKEN),
            NativeAuthV2ChallengeRequest.NativeAuthV2ChallengeRequestParameters(
                continuationToken = CONTINUATION_TOKEN
            ) to JSONObject().put("continuationToken", CONTINUATION_TOKEN),
            NativeAuthV2VerifyRequest.NativeAuthV2VerifyRequestParameters(
                continuationToken = CONTINUATION_TOKEN,
                otp = OTP
            ) to JSONObject()
                .put("continuationToken", CONTINUATION_TOKEN)
                .put("otp", OTP),
            NativeAuthV2PollRequest.NativeAuthV2PollRequestParameters(
                continuationToken = CONTINUATION_TOKEN
            ) to JSONObject().put("continuationToken", CONTINUATION_TOKEN)
        )

        cases.forEach { (parameters, expected) ->
            val serialized = JSONObject(ObjectMapper.serializeObjectToJsonString(parameters))

            assertEquals(expected.toString(), serialized.toString())
            assertFalse(serialized.has("clientId"))
            assertFalse(serialized.has("client_id"))
            assertFalse(serialized.has("scope"))
            assertFalse(serialized.has("claims"))
        }
    }

    @Test
    fun updatePasswordParameters_serializePasswordAsStringAndExcludeClientId() {
        val password = "P@ssw0rd!".toCharArray()
        try {
            val parameters =
                NativeAuthV2UpdatePasswordRequest.NativeAuthV2UpdatePasswordRequestParameters(
                    clientId = CLIENT_ID,
                    continuationToken = CONTINUATION_TOKEN,
                    newPassword = password
                )

            val serialized = JSONObject(ObjectMapper.serializeObjectToJsonString(parameters))

            assertEquals(CONTINUATION_TOKEN, serialized.getString("continuationToken"))
            assertEquals("P@ssw0rd!", serialized.getString("newPassword"))
            assertFalse(serialized.has("clientId"))
            assertFalse(serialized.has("client_id"))
        } finally {
            password.fill('\u0000')
        }
    }

    @Test
    fun oauthRequestParameters_serializeExpectedFormFieldNames() {
        val start = AuthorizeChallengeStartRequest.NativeAuthAuthorizeChallengeStartRequestParameters(
            clientId = CLIENT_ID
        )
        val continuation =
            AuthorizeChallengeContinueRequest.NativeAuthAuthorizeChallengeContinueRequestParameters(
                continuationToken = CONTINUATION_TOKEN
            )
        val token = NativeAuthV2TokenRequest.NativeAuthV2TokenRequestParameters(
            clientId = CLIENT_ID,
            grantType = "authorization_code",
            code = "code",
            scope = "User.Read offline_access",
            claimsRequestJson = null
        )

        val startForm = ObjectMapper.serializeObjectToFormUrlEncoded(start)
        val continuationForm = ObjectMapper.serializeObjectToFormUrlEncoded(continuation)
        val tokenForm = ObjectMapper.serializeObjectToFormUrlEncoded(token)

        assertTrue(startForm.contains("client_id=$CLIENT_ID"))
        assertTrue(continuationForm.contains("continuation_token=$CONTINUATION_TOKEN"))
        assertTrue(tokenForm.contains("grant_type=authorization_code"))
        assertTrue(tokenForm.contains("code=code"))
        assertTrue(tokenForm.contains("scope=User.Read+offline_access"))
        assertTrue(tokenForm.contains("client_info=true"))
    }

    private companion object {
        private const val CLIENT_ID = "client-id"
        private const val USERNAME = "ada@contoso.com"
        private const val CONTINUATION_TOKEN = "continuation-token"
        private const val OTP = "123456"
    }
}
