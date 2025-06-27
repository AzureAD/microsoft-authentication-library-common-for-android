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

import android.content.ContentResolver
import android.content.Context
import android.database.Cursor
import android.net.Uri
import com.microsoft.identity.common.internal.numberMatch.NumberMatchHelper
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.mockito.MockitoAnnotations
import org.robolectric.RobolectricTestRunner
import org.junit.runner.RunWith

@RunWith(RobolectricTestRunner::class)
class AuthUxJavaScriptInterfaceTest {

    private lateinit var authUxJavaScriptInterface: AuthUxJavaScriptInterface

    @Mock
    private lateinit var context: Context
    @Mock
    private lateinit var contentResolver: ContentResolver
    @Mock
    private lateinit var cursor: Cursor

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
        MockitoAnnotations.openMocks(this)
        Mockito.reset(context, contentResolver, cursor)
        // Mock contentResolver on context
        `when`(context.contentResolver).thenReturn(contentResolver)
        // Mock insert, delete, and query for contentResolver
        `when`(contentResolver.insert(Mockito.any(Uri::class.java), Mockito.any())).thenReturn(null)
        `when`(contentResolver.delete(Mockito.any(Uri::class.java), Mockito.any(), Mockito.any())).thenReturn(1)
        // Mock query for getNumberMatch
        `when`(contentResolver.query(Mockito.any(Uri::class.java), Mockito.any(), Mockito.anyString(), Mockito.any(), Mockito.isNull())).thenReturn(cursor)
        // Mock cursor for getNumberMatch
        `when`(cursor.moveToFirst()).thenReturn(true)
        `when`(cursor.getColumnIndexOrThrow(Mockito.anyString())).thenReturn(0)
        `when`(cursor.getString(0)).thenReturn(mockNumberMatchValue)
        authUxJavaScriptInterface = AuthUxJavaScriptInterface(context)
    }

    @After
    fun tearDown() {
        NumberMatchHelper.clearNumberMatch(context)
    }

    @Test
    fun `test receiveAuthUxMessage with NUMBER_MATCH function`() {
        // Call the method
        authUxJavaScriptInterface.receiveAuthUxMessage(numberMatchTestPayload)

        // Verify that the data was stored in NumberMatchHelper
        val storedValue = NumberMatchHelper.getNumberMatch(context, mockSessionId)
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
    fun `test isValidUrlForInterface with valid AAD URL`() {
        val validUrl = "https://login.microsoftonline.com/common/oauth2/authorize"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUrlForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with valid MSA URL`() {
        val validUrl = "https://login.live.com/oauth20_authorize.srf"
        Assert.assertTrue(AuthUxJavaScriptInterface.isValidUrlForInterface(validUrl))
    }

    @Test
    fun `test isValidUrlForInterface with null URL`() {
        val nullUrl: String? = null
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUrlForInterface(nullUrl))
    }

    @Test
    fun `test isValidUrlForInterface with invalid URL`() {
        val invalidUrl = "https://example.com"
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUrlForInterface(invalidUrl))
    }

    @Test
    fun `test isValidUrlForInterface with empty URL`() {
        val emptyUrl = ""
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUrlForInterface(emptyUrl))
    }
}
