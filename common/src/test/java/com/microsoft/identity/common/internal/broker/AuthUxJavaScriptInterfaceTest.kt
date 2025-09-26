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
package com.microsoft.identity.common.internal.broker

import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test

class AuthUxJavaScriptInterfaceTest {

    private lateinit var authUxJavaScriptInterface: AuthUxJavaScriptInterface

    private val mockSessionId = "1234"
    private val mockNumberMatchValue = "00"

    private val numberMatchTestPayload = """
        {
            correlationID: w.ServerData.correlationId,
            action_name: "write_data",
            action_component: "broker",
            params: {
                operation: "number_matching",
                sessionID: $mockSessionId,
                code_match: $mockNumberMatchValue
            }
        }
    """.trimIndent()

    @Before
    fun setUp() {
        authUxJavaScriptInterface = AuthUxJavaScriptInterface()
    }

    @After
    fun tearDown() {
        // Clear the static map after each test
        NumberMatchHelper.numberMatchMap.clear()
    }

    @Test
    fun `test receiveAuthUxMessage with NUMBER_MATCH function`() {
        // Call the method
        authUxJavaScriptInterface.receiveAuthUxMessage(numberMatchTestPayload)

        // Verify that the data was stored in NumberMatchHelper
        val storedValue = NumberMatchHelper.numberMatchMap[mockSessionId]
        Assert.assertTrue(storedValue == mockNumberMatchValue)
    }

    @Test
    fun `test receiveAuthUxMessage with empty json`() {
        // Call the method
        authUxJavaScriptInterface.receiveAuthUxMessage("{}")

        // Should not get an exception
    }

    @Test
    fun `test receiveAuthUxMessage with non-json string`() {
        // Call the method
        authUxJavaScriptInterface.receiveAuthUxMessage("NotAJson")

        // Should not get an exception
    }

    @Test
    fun `test isValidUrlForInterface with valid AAD Global URL`() {
        val validUrl = "https://login.microsoftonline.com/common/oauth2/authorize"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUriForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with valid AAD US URL`() {
        val validUrl = "https://login.microsoftonline.us/common/oauth2/authorize"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUriForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with valid AAD China URL`() {
        val validUrl = "https://login.microsoftonline.cn/common/oauth2/authorize"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUriForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with valid MSA URL`() {
        val validUrl = "https://login.live.com/oauth20_authorize.srf"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUriForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with null URL`() {
        val nullUrl: String? = null
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(nullUrl))
    }

    @Test
    fun `test isValidUrlForInterface with invalid URL`() {
        val invalidUrl = "https://example.com"
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(invalidUrl))
    }

    @Test
    fun `test isValidUrlForInterface with empty URL`() {
        val emptyUrl = ""
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(emptyUrl))
    }
}
