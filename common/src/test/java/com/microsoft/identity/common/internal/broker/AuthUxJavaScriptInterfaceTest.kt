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
import org.junit.Before
import org.junit.Test

class AuthUxJavaScriptInterfaceTest {

    private lateinit var authUxJavaScriptInterface: AuthUxJavaScriptInterface

    private val mockSessionId = "1234"
    private val mockNumberMatchValue = "00"

    private val numberMatchTestPayload = """
        { 
            "correlationID": "SOME_CORRELATION_ID" ,
            "action_name":"write_data", 
            "action_component":"broker", 
            "params": 
            { 
                "function": "NUMBER_MATCH", 
                "data": 
                { 
                    "sessionID": "$mockSessionId", 
                    "numberMatch": "$mockNumberMatchValue" 
                } 
            }
        }
    """.trimIndent()

    @Before
    fun setUp() {
        authUxJavaScriptInterface = AuthUxJavaScriptInterface()
    }

    @Test
    fun `test postMessageToBroker with NUMBER_MATCH function`() {
        // Call the method
        authUxJavaScriptInterface.postMessageToBroker(numberMatchTestPayload)

        // Verify that the data was stored in NumberMatchHelper
        val storedValue = NumberMatchHelper.numberMatchMap[mockSessionId]
        assert(storedValue == mockNumberMatchValue)
    }
}
