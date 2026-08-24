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

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2HalApiResponseTest {

    @Test
    fun error_buildsClientSideErrorWithoutFlowState() {
        val response = NativeAuthV2HalApiResponse.error(
            statusCode = 500,
            correlationId = REQUEST_CORRELATION_ID,
            errorCode = "empty_body_error",
            errorMessage = "V2 HAL response body was empty or blank."
        )

        assertEquals(500, response.statusCode)
        assertEquals(REQUEST_CORRELATION_ID, response.correlationId)
        assertEquals("empty_body_error", response.serverError?.code)
        assertEquals("V2 HAL response body was empty or blank.", response.serverError?.message)
        assertEquals(REQUEST_CORRELATION_ID, response.serverError?.correlationId)
        assertNull(response.serverError?.innerErrorCode)
        assertNull(response.continuationToken)
        assertNull(response.state)
        assertNull(response.action)
        assertNull(response.authorizationCode)
        assertEquals(emptyMap<String, String>(), response.links)
        assertTrue(response.methods.isEmpty())
        assertFalse(response.isWebFallbackRequired)
    }

    /**
     * Guards the reason [NativeAuthV2HalApiResponse.error] exists: the message is carried as data,
     * so JSON metacharacters survive verbatim instead of corrupting a synthesised JSON document.
     */
    @Test
    fun error_preservesMessagesContainingJsonMetacharacters() {
        val message = """Unexpected token "}" in body \ at offset 3"""

        val response = NativeAuthV2HalApiResponse.error(
            statusCode = 400,
            correlationId = REQUEST_CORRELATION_ID,
            errorCode = "response_parse_error",
            errorMessage = message
        )

        assertEquals(message, response.serverError?.message)
    }

    @Test
    fun from_whenOAuthErrorBodyIsFlat_preservesFlatErrorFields() {
        val response = responseFrom(
            """
            {
              "error": "invalid_grant",
              "error_description": "Request failed. AADSTS70011",
              "correlation_id": "service-correlation-id"
            }
            """.trimIndent()
        )

        assertEquals(REQUEST_CORRELATION_ID, response.correlationId)
        assertEquals("invalid_grant", response.serverError?.code)
        assertEquals("Request failed. AADSTS70011", response.serverError?.message)
        assertEquals("service-correlation-id", response.serverError?.correlationId)
        assertNull(response.serverError?.innerErrorCode)
        assertFalse(response.isWebFallbackRequired)
    }

    @Test
    fun from_whenOAuthErrorIsFlatRedirectToWeb_setsWebFallbackRequired() {
        val response = responseFrom(
            """
            {
              "error": "redirect_to_web",
              "error_description": "Browser required."
            }
            """.trimIndent()
        )

        assertEquals("redirect_to_web", response.serverError?.code)
        assertEquals("Browser required.", response.serverError?.message)
        assertTrue(response.isWebFallbackRequired)
    }

    @Test
    fun from_whenTemplatedAndConcreteLinksAreMixed_usesFirstConcreteLinkAndOmitsTemplatedOnlyRelations() {
        val response = responseFrom(
            """
            {
              "_links": {
                "signIn": [
                  {
                    "href": "/sign-in{?dc}",
                    "templated": true
                  },
                  {
                    "href": "/sign-in/real"
                  }
                ],
                "signUp": {
                  "href": "/sign-up{?dc}",
                  "templated": true
                }
              },
              "_embedded": {
                "methods": {
                  "id": "email",
                  "_links": {
                    "challenge": [
                      {
                        "href": "/challenge{?dc}",
                        "templated": true
                      },
                      {
                        "href": "/challenge/real"
                      }
                    ],
                    "verify": {
                      "href": "/verify{?dc}",
                      "templated": true
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        assertEquals("/sign-in/real", response.links[NativeAuthV2LinkRelation.SIGN_IN.value])
        assertFalse(response.links.containsKey(NativeAuthV2LinkRelation.SIGN_UP.value))
        assertEquals("/challenge/real", response.methods.single().links[NativeAuthV2LinkRelation.CHALLENGE.value])
        assertFalse(response.methods.single().links.containsKey(NativeAuthV2LinkRelation.VERIFY.value))
    }

    @Test
    fun compiledApi_doesNotExposeDataClassCopyMethod() {
        assertFalse(
            NativeAuthV2HalApiResponse::class.java.declaredMethods.any { it.name.startsWith("copy") }
        )
    }

    private fun responseFrom(json: String): NativeAuthV2HalApiResponse =
        NativeAuthV2HalApiResponse.from(
            halResource = HalResource.from(json),
            statusCode = 400,
            correlationId = REQUEST_CORRELATION_ID
        )

    private companion object {
        private const val REQUEST_CORRELATION_ID = "request-correlation-id"
    }
}
