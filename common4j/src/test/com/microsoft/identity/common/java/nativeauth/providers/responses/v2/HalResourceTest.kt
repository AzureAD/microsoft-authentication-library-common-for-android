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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class HalResourceTest {

    @Test
    fun from_whenBodyIsBlank_throwsClientException() {
        val exception = assertThrows(ClientException::class.java) {
            HalResource.from("   ")
        }

        assertEquals(ClientException.JSON_PARSE_FAILURE, exception.errorCode)
    }

    @Test
    fun from_whenBodyIsMalformedJson_throwsClientException() {
        val exception = assertThrows(ClientException::class.java) {
            HalResource.from("{not-json")
        }

        assertEquals(ClientException.JSON_PARSE_FAILURE, exception.errorCode)
    }

    @Test
    fun from_whenRootIsNotJsonObject_throwsClientException() {
        val exception = assertThrows(ClientException::class.java) {
            HalResource.from("""["not","an","object"]""")
        }

        assertEquals(ClientException.JSON_PARSE_FAILURE, exception.errorCode)
    }

    @Test
    fun from_normalizesObjectAndArrayLinkAndEmbeddedShapes_andIgnoresCuries() {
        val resource = HalResource.from(
            """
            {
              "wholeInt": 6,
              "wholeDecimal": 7.0,
              "fractional": 7.5,
              "tooLarge": 2147483648,
              "_links": {
                "self": {
                  "href": "/self"
                },
                "challenge": [
                  {
                    "templated": true,
                    "href": "/challenge{?dc}"
                  },
                  {
                    "href": "/challenge/first"
                  },
                  {
                    "href": "/challenge/second"
                  }
                ],
                "curies": {
                  "href": "https://docs.contoso.com/rels/{rel}",
                  "templated": true
                }
              },
              "_embedded": {
                "methods": {
                  "id": "email",
                  "_links": {
                    "verify": [
                      {
                        "templated": true,
                        "href": "/verify{?dc}"
                      },
                      {
                        "href": "/verify/email"
                      }
                    ]
                  }
                },
                "options": [
                  {
                    "id": "first"
                  },
                  {
                    "id": "second"
                  }
                ]
              }
            }
            """.trimIndent()
        )

        assertEquals("/self", resource.href("self"))
        assertEquals("/challenge{?dc}", resource.href("challenge"))
        assertEquals(
            listOf("/challenge{?dc}", "/challenge/first", "/challenge/second"),
            resource.links["challenge"]?.map { it.href }
        )
        assertTrue(resource.links.getValue("challenge").first().templated)
        assertFalse(resource.links.containsKey("curies"))

        val methods = resource.embeddedResources("methods")
        assertEquals(1, methods.size)
        assertEquals("email", methods.single().string("id"))
        assertEquals("/verify{?dc}", methods.single().href("verify"))
        assertTrue(methods.single().links.getValue("verify").first().templated)

        val options = resource.embeddedResources("options")
        assertEquals(2, options.size)
        assertEquals(listOf("first", "second"), options.map { it.string("id") })

        assertEquals(6, resource.int("wholeInt"))
        assertEquals(7, resource.int("wholeDecimal"))
        assertNull(resource.int("fractional"))
        assertNull(resource.int("tooLarge"))
    }

    @Test
    fun from_whenLinksAreTemplated_retainsLinkMetadataAtEveryLevel() {
        val resource = HalResource.from(
            """
            {
              "_links": {
                "challenge": {
                  "href": "/challenge{?dc}",
                  "templated": true
                }
              },
              "_embedded": {
                "methods": {
                  "_links": {
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

        assertEquals("/challenge{?dc}", resource.href("challenge"))
        assertTrue(resource.links.getValue("challenge").single().templated)

        val method = resource.embeddedResources("methods").single()
        assertEquals("/verify{?dc}", method.href("verify"))
        assertTrue(method.links.getValue("verify").single().templated)
    }

    @Test
    fun from_whenTemplatedLinkOnlyExpandsLeadingTenant_retainsThatRelation() {
        val resource = HalResource.from(
            """
            {
              "_links": {
                "challenge": {
                  "href": "/{tenant}/api/v0.1/auth/challenge",
                  "templated": true
                }
              }
            }
            """.trimIndent()
        )

        assertEquals(
            "/{tenant}/api/v0.1/auth/challenge",
            resource.href("challenge")
        )
    }
}
