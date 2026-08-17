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

import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.util.ObjectMapper
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthV2TokenRequestTest {

    @Test
    fun create_whenScopesAreEmpty_throwsClientException() {
        assertThrows(ClientException::class.java) {
            createRequest(emptyList())
        }
    }

    @Test
    fun create_whenAnyScopeIsBlank_throwsClientException() {
        listOf(
            listOf(""),
            listOf("   "),
            listOf("openid", " ")
        ).forEach { scopes ->
            assertThrows(ClientException::class.java) {
                createRequest(scopes)
            }
        }
    }

    @Test
    fun create_whenScopesAreValid_serializesSpaceDelimitedScopeInFinalFormBody() {
        val request = createRequest(listOf("openid", "offline_access", "User.Read"))

        val formBody = ObjectMapper.serializeObjectToFormUrlEncoded(request.parameters)

        assertTrue(formBody.contains("scope=openid+offline_access+User.Read"))
    }

    private fun createRequest(scopes: List<String>): NativeAuthV2TokenRequest =
        NativeAuthV2TokenRequest.create(
            clientId = "client-id",
            code = "authorization-code",
            scopes = scopes,
            requestUrl = "https://login.microsoftonline.com/tenant/oauth2/v2.0/token",
            headers = mapOf("Content-Type" to "application/x-www-form-urlencoded")
        )
}
