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
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

class NativeAuthV2TokenRequestTest {

    @Test
    fun tokenRequestLoggingNeverIncludesAuthorizationCode() {
        val code = "test-secret-authorization-code"
        val request = NativeAuthV2TokenRequest.create(
            clientId = "client-id",
            code = code,
            requestUrl = "https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
        )

        for (description in listOf(
            request.toString(), request.toUnsanitizedString(),
            request.parameters.toString(), request.parameters.toUnsanitizedString()
        )) {
            assertFalse("Token request logging must not include the authorization code", description.contains(code))
        }
    }

    @Test
    fun create_serializesTokenExchangeFieldsWithoutScopesOrClaims() {
        val request = NativeAuthV2TokenRequest.create(
            clientId = "client-id",
            code = "authorization-code",
            requestUrl = "https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
        )

        val formBody = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)

        assertEquals(
            mapOf(
                "client_id" to "client-id",
                "grant_type" to "authorization_code",
                "code" to "authorization-code",
                "client_info" to "true"
            ),
            parseFormBody(formBody)
        )
    }

    private fun parseFormBody(body: String): Map<String, String> =
        body.split("&").associate { parameter ->
            val (name, value) = parameter.split("=", limit = 2)
            URLDecoder.decode(name, StandardCharsets.UTF_8.name()) to
                URLDecoder.decode(value, StandardCharsets.UTF_8.name())
        }
}
