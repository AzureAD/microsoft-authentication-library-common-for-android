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
import com.google.gson.GsonBuilder
import com.google.gson.JsonParseException
import org.junit.Assert.*
import org.junit.Test

class AuthUxJsonPayloadTest {

    private val gson = GsonBuilder()
        .registerTypeAdapter(AuthUxJsonPayload::class.java, AuthUxJsonPayloadKTDeserializer())
        .create()

    @Test
    fun `test deserialization of valid JSON`() {
        val json = """
            {
                "correlationID": "12345",
                "action_name": "write_data",
                "action_component": "broker",
                "params": {
                    "function": "NUMBER_MATCH",
                    "data": {
                        "sessionID": "67890",
                        "numberMatch": "123456"
                    }
                }
            }
        """.trimIndent()

        val payload = gson.fromJson(json, AuthUxJsonPayload::class.java)

        assertNotNull(payload)
        assertEquals("12345", payload.correlationId)
        assertEquals("write_data", payload.actionName)
        assertEquals("broker", payload.actionComponent)

        val params = payload.params
        assertNotNull(params)
        assertEquals("NUMBER_MATCH", params?.function)

        val data = params?.data
        assertNotNull(data)
        assertEquals("67890", data?.sessionId)
        assertEquals("123456", data?.numberMatch)
    }

    @Test
    fun `test deserialization of JSON with missing optional fields`() {
        val json = """
            {
                "correlationID": "12345",
                "action_name": "write_data",
                "action_component": "broker",
                "params" : {
                    "invalidField": "invalidField"
                }
            }
        """.trimIndent()

        val payload = gson.fromJson(json, AuthUxJsonPayload::class.java)

        assertNotNull(payload)
        assertEquals("12345", payload.correlationId)
        assertEquals("write_data", payload.actionName)
        assertEquals("broker", payload.actionComponent)
        assertNotNull(payload.params)
        assertNull(payload.params?.data)
        assertNull(payload.params?.function)
    }

    @Test(expected = JsonParseException::class)
    fun `test deserialization of JSON with missing mandatory fields, exception expected`() {
        val json = """
            {
                "correlationID": "12345",
                "action_name": "write_data"
            }
        """.trimIndent()

        // This should throw an exception because "action_component" and "params" is missing
        gson.fromJson(json, AuthUxJsonPayload::class.java)

    }

    @Test(expected = JsonParseException::class)
    fun `test deserialization of empty JSON, exception expected`() {
        val json = "{}"

        // This should throw an exception because the JSON is empty
        val payload = gson.fromJson(json, AuthUxJsonPayload::class.java)
    }
}
