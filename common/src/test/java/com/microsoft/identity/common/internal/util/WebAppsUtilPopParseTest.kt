// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.

package com.microsoft.identity.common.internal.util

import com.microsoft.identity.common.java.authscheme.WebAppsPopAuthenticationSchemeInternal
import com.microsoft.identity.common.java.commands.webapps.WebAppsGetTokenSubOperationRequest
import com.microsoft.identity.common.java.exception.ClientException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebAppsUtilPopParseTest {

    private fun buildRequest(
        tokenType: String? = null,
        reqCnf: String? = null,
        extraParameters: Map<String, String>? = null
    ): WebAppsGetTokenSubOperationRequest {
        return WebAppsGetTokenSubOperationRequest(
            homeAccountId = null,
            clientId = "clientId",
            authority = "https://login.microsoftonline.com/common",
            scopes = "User.Read",
            redirectUri = "https://app.example.com/",
            tokenType = tokenType,
            reqCnf = reqCnf,
            extraParameters = extraParameters
        )
    }

    @Test
    fun testParsePopAuthScheme_noPopParams_returnsNull() {
        val request = buildRequest()
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNull(result)
    }

    @Test
    fun testParsePopAuthScheme_bearerTokenType_returnsNull() {
        val request = buildRequest(tokenType = "bearer")
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNull(result)
    }

    @Test
    fun testParsePopAuthScheme_bearerTokenTypeCaseInsensitive_returnsNull() {
        val request = buildRequest(tokenType = "BEARER")
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNull(result)
    }

    @Test
    fun testParsePopAuthScheme_popWithReqCnf_returnsScheme() {
        val reqCnfValue = "test-req-cnf-value"
        val request = buildRequest(tokenType = "pop", reqCnf = reqCnfValue)
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNotNull(result)
        assertEquals(reqCnfValue, result!!.requestConfirmation)
        assertEquals(WebAppsPopAuthenticationSchemeInternal.SCHEME_POP_PREGENERATED, result.name)
    }

    @Test
    fun testParsePopAuthScheme_popTokenTypeCaseInsensitive_returnsScheme() {
        val reqCnfValue = "test-req-cnf-value"
        val request = buildRequest(tokenType = "POP", reqCnf = reqCnfValue)
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNotNull(result)
        assertEquals(reqCnfValue, result!!.requestConfirmation)
    }

    @Test
    fun testParsePopAuthScheme_popTokenTypeMixedCase_returnsScheme() {
        val reqCnfValue = "test-req-cnf-value"
        val request = buildRequest(tokenType = "Pop", reqCnf = reqCnfValue)
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNotNull(result)
        assertEquals(reqCnfValue, result!!.requestConfirmation)
    }

    @Test(expected = ClientException::class)
    fun testParsePopAuthScheme_popWithoutReqCnf_throwsException() {
        val request = buildRequest(tokenType = "pop")
        WebAppsUtil.parsePopAuthSchemeFromRequest(request)
    }

    @Test(expected = ClientException::class)
    fun testParsePopAuthScheme_reqCnfWithoutTokenType_throwsException() {
        val request = buildRequest(reqCnf = "some-cnf-value")
        WebAppsUtil.parsePopAuthSchemeFromRequest(request)
    }

    @Test
    fun testParsePopAuthScheme_popInExtraParameters_returnsScheme() {
        val reqCnfValue = "extra-param-cnf"
        val request = buildRequest(
            extraParameters = mapOf(
                WebAppsGetTokenSubOperationRequest.FIELD_TOKEN_TYPE to "pop",
                WebAppsGetTokenSubOperationRequest.FIELD_REQ_CNF to reqCnfValue
            )
        )
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNotNull(result)
        assertEquals(reqCnfValue, result!!.requestConfirmation)
    }

    @Test
    fun testParsePopAuthScheme_topLevelTakesPriorityOverExtraParams() {
        val topLevelReqCnf = "top-level-cnf"
        val extraParamReqCnf = "extra-param-cnf"
        val request = buildRequest(
            tokenType = "pop",
            reqCnf = topLevelReqCnf,
            extraParameters = mapOf(
                WebAppsGetTokenSubOperationRequest.FIELD_TOKEN_TYPE to "pop",
                WebAppsGetTokenSubOperationRequest.FIELD_REQ_CNF to extraParamReqCnf
            )
        )
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNotNull(result)
        // Top-level reqCnf should take priority
        assertEquals(topLevelReqCnf, result!!.requestConfirmation)
    }

    @Test(expected = ClientException::class)
    fun testParsePopAuthScheme_popInExtraParamsWithoutReqCnf_throwsException() {
        val request = buildRequest(
            extraParameters = mapOf(
                WebAppsGetTokenSubOperationRequest.FIELD_TOKEN_TYPE to "pop"
            )
        )
        WebAppsUtil.parsePopAuthSchemeFromRequest(request)
    }

    @Test(expected = ClientException::class)
    fun testParsePopAuthScheme_reqCnfInExtraParamsWithoutTokenType_throwsException() {
        val request = buildRequest(
            extraParameters = mapOf(
                WebAppsGetTokenSubOperationRequest.FIELD_REQ_CNF to "some-cnf"
            )
        )
        WebAppsUtil.parsePopAuthSchemeFromRequest(request)
    }

    @Test
    fun testParsePopAuthScheme_tokenTypeTopLevel_reqCnfInExtraParams_returnsScheme() {
        val reqCnfValue = "extra-param-cnf"
        val request = buildRequest(
            tokenType = "pop",
            extraParameters = mapOf(
                WebAppsGetTokenSubOperationRequest.FIELD_REQ_CNF to reqCnfValue
            )
        )
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNotNull(result)
        assertEquals(reqCnfValue, result!!.requestConfirmation)
    }

    @Test
    fun testParsePopAuthScheme_emptyTokenType_returnsNull() {
        val request = buildRequest(tokenType = "")
        val result = WebAppsUtil.parsePopAuthSchemeFromRequest(request)
        assertNull(result)
    }
}
