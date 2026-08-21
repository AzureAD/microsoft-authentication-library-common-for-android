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

import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.ObjectInputStream
import java.io.ObjectStreamClass
import java.io.ObjectOutputStream
import java.io.Serializable

class NativeAuthV2ContinuationStateTest {

    @Test
    fun javaSerializationRoundTrip_preservesStateAndRedactsStringRepresentations() {
        val original = createState()

        assertTrue(Serializable::class.java.isInstance(original))
        assertEquals(
            1L,
            ObjectStreamClass.lookup(NativeAuthV2ContinuationState::class.java).serialVersionUID
        )

        val serialized = ByteArrayOutputStream().use { bytes ->
            ObjectOutputStream(bytes).use { it.writeObject(original) }
            bytes.toByteArray()
        }
        val restored = ObjectInputStream(ByteArrayInputStream(serialized)).use {
            it.readObject() as NativeAuthV2ContinuationState
        }

        assertEquals(CONTINUATION_TOKEN, restored.continuationToken)
        assertEquals(RESET_PASSWORD_HREF, restored.href(NativeAuthV2LinkRelation.RESET_PASSWORD))
        assertEquals(SIGN_IN_HREF, restored.href(NativeAuthV2LinkRelation.SIGN_IN))
        assertEquals(SCOPES, restored.scopes)
        assertEquals(CORRELATION_ID, restored.correlationId)
        assertEquals(NativeAuthV2LinkRelation.RESET_PASSWORD, restored.entryRelation)
        assertEquals(REDACTED_STRING, restored.toString())
        assertEquals(REDACTED_STRING, restored.toUnsanitizedString())
    }

    @Test
    fun next_whenContinuationTokenIsBlank_returnsNull() {
        val previous = createState()

        listOf("", "   ").forEach { token ->
            val response = responseFrom(
                """
                {
                  "continuation_token": "$token",
                  "reset_password": "$RESET_PASSWORD_HREF"
                }
                """.trimIndent()
            )

            assertNull(NativeAuthV2ContinuationState.next(previous, response))
        }
    }

    @Test
    fun next_whenContinuationTokenIsMissing_returnsNull() {
        assertNull(NativeAuthV2ContinuationState.next(createState(), responseFrom("""{}""")))
    }

    @Test
    fun fromAuthorizeChallengeResponse_filtersUnsupportedRelationsAndDefensivelyCopiesScopes() {
        val originalScopes = mutableListOf("openid", "User.Read")
        val response = responseFrom(
            """
            {
              "continuation_token": "$CONTINUATION_TOKEN",
              "_links": {
                "resetPassword": {
                  "href": "$RESET_PASSWORD_HREF"
                },
                "unsupported": {
                  "href": "/tenant/unsupported"
                }
              }
            }
            """.trimIndent()
        )

        val state = NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = response,
            continuationToken = CONTINUATION_TOKEN,
            scopes = originalScopes,
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD
        )

        originalScopes += "Mail.Read"
        val returnedScopes = state.scopesForTokenRequest() as MutableList<String>
        returnedScopes += "Calendars.Read"

        assertEquals(listOf("openid", "User.Read"), state.scopesForTokenRequest())
        assertEquals(RESET_PASSWORD_HREF, state.href(NativeAuthV2LinkRelation.RESET_PASSWORD))
        assertNull(state.href(NativeAuthV2LinkRelation("unsupported")))
    }

    @Test
    fun next_carriesClaimsForwardForTokenRequest() {
        val previous = NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
            response = responseFrom(
                """
                {
                  "continuation_token": "$CONTINUATION_TOKEN",
                  "sign_in": "$SIGN_IN_HREF"
                }
                """.trimIndent()
            ),
            continuationToken = CONTINUATION_TOKEN,
            scopes = SCOPES,
            claimsRequestJson = CLAIMS_REQUEST_JSON,
            entryRelation = NativeAuthV2LinkRelation.SIGN_IN
        )

        val next = NativeAuthV2ContinuationState.next(
            previous = previous,
            response = responseFrom(
                """
                {
                  "continuation_token": "next-token",
                  "sign_in": "$SIGN_IN_HREF"
                }
                """.trimIndent()
            )
        )

        assertEquals(CLAIMS_REQUEST_JSON, next?.claimsRequestJsonForTokenRequest())
    }

    @Test
    fun next_whenSelectedMethodContainsRelation_prefersSelectedMethodLinkOverTopLevel() {
        val next = NativeAuthV2ContinuationState.next(
            previous = createState(),
            response = responseFrom(
                """
                {
                  "continuation_token": "next-token",
                  "_links": {
                    "challenge": {
                      "href": "/tenant/top-level-challenge"
                    },
                    "verify": {
                      "href": "/tenant/top-level-verify"
                    }
                  },
                  "_embedded": {
                    "methods": [
                      {
                        "id": "email",
                        "_links": {
                          "challenge": {
                            "href": "/tenant/embedded-challenge"
                          },
                          "verify": {
                            "href": "/tenant/embedded-verify"
                          },
                          "unsupported": {
                            "href": "/tenant/unsupported"
                          }
                        }
                      }
                    ]
                  }
                }
                """.trimIndent()
            ),
            selectedMethod = responseFrom(
                """
                {
                  "_embedded": {
                    "methods": [
                      {
                        "id": "email",
                        "_links": {
                          "challenge": {
                            "href": "/tenant/embedded-challenge"
                          },
                          "verify": {
                            "href": "/tenant/embedded-verify"
                          },
                          "unsupported": {
                            "href": "/tenant/unsupported"
                          }
                        }
                      }
                    ]
                  }
                }
                """.trimIndent()
            ).methods.single()
        )

        requireNotNull(next)
        assertEquals("/tenant/embedded-challenge", next.href(NativeAuthV2LinkRelation.CHALLENGE))
        assertEquals("/tenant/embedded-verify", next.href(NativeAuthV2LinkRelation.VERIFY))
        assertNull(next.href(NativeAuthV2LinkRelation("unsupported")))
    }

    @Test
    fun next_whenSelectedMethodLacksRelation_fallsBackToTopLevelLink() {
        val response = responseFrom(
            """
            {
              "continuation_token": "next-token",
              "_links": {
                "resend": {
                  "href": "/tenant/top-level-resend"
                }
              },
              "_embedded": {
                "methods": {
                  "id": "email",
                  "_links": {
                    "challenge": {
                      "href": "/tenant/embedded-challenge"
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val next = NativeAuthV2ContinuationState.next(
            previous = createState(),
            response = response,
            selectedMethod = response.methods.single()
        )

        requireNotNull(next)
        assertEquals("/tenant/top-level-resend", next.href(NativeAuthV2LinkRelation.RESEND))
        assertEquals("/tenant/embedded-challenge", next.href(NativeAuthV2LinkRelation.CHALLENGE))
    }

    @Test
    fun fromAuthorizeChallengeResponse_whenContinuationTokenIsBlank_throwsClientException() {
        val response = responseFrom(
            """
            {
              "continuation_token": "$CONTINUATION_TOKEN",
              "reset_password": "$RESET_PASSWORD_HREF"
            }
            """.trimIndent()
        )

        listOf("", "   ").forEach { token ->
            assertThrows(ClientException::class.java) {
                NativeAuthV2ContinuationState.fromAuthorizeChallengeResponse(
                    response = response,
                    continuationToken = token,
                    scopes = SCOPES,
                    entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD
                )
            }
        }
    }

    private fun createState(): NativeAuthV2ContinuationState {
        val response = responseFrom(
            """
            {
              "continuation_token": "$CONTINUATION_TOKEN",
              "reset_password": "$RESET_PASSWORD_HREF",
              "sign_in": "$SIGN_IN_HREF"
            }
            """.trimIndent()
        )
        val result = NativeAuthV2ResponseParser().parseAuthorizeChallenge(
            response = response,
            entryRelation = NativeAuthV2LinkRelation.RESET_PASSWORD,
            scopes = SCOPES
        )

        return (result as AuthorizeChallengeApiResult.ContinuationRequired).continuationState
    }

    private fun responseFrom(json: String): NativeAuthV2HalApiResponse =
        NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(json),
            statusCode = 200,
            correlationId = CORRELATION_ID
        )

    private companion object {
        private const val CLAIMS_REQUEST_JSON = """{"access_token":{"xms_cc":{"values":["cp1"]}}}"""
        private const val CONTINUATION_TOKEN = "flow-token"
        private const val RESET_PASSWORD_HREF = "/tenant/reset-password"
        private const val SIGN_IN_HREF = "/tenant/sign-in"
        private const val CORRELATION_ID = "corr-123"
        private const val REDACTED_STRING = "NativeAuthV2ContinuationState(<redacted>)"
        private val SCOPES = listOf("openid", "User.Read")
    }
}
