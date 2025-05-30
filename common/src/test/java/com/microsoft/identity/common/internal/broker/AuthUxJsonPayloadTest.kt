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
                correlationID: 12345,
                action_name: "write_data",
                action_component: "broker",
                params: {
                    operation: "number_matching",
                    sessionID: 67890,
                    code_match: 123456
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
        assertEquals("number_matching", params?.operation)
        assertEquals("67890", params?.sessionId)
        assertEquals("123456", params?.codeMatch)
    }

    @Test
    fun `test deserialization of JSON with missing optional fields`() {
        val json = """
            {
                correlationID: 12345,
                action_name: "write_data",
                action_component: "broker"
            }
        """.trimIndent()

        val payload = gson.fromJson(json, AuthUxJsonPayload::class.java)

        assertNotNull(payload)
        assertEquals("12345", payload.correlationId)
        assertEquals("write_data", payload.actionName)
        assertEquals("broker", payload.actionComponent)
        assertNull(payload.params)
    }

    @Test(expected = JsonParseException::class)
    fun `test deserialization of JSON with missing mandatory fields, exception expected`() {
        val json = """
            {
                correlationID: 12345,
                action_name: "write_data"
            }
        """.trimIndent()

        // This should throw an exception because "action_component" and "params" is missing
        gson.fromJson(json, AuthUxJsonPayload::class.java)

    }

    @Test(expected = JsonParseException::class)
    fun `test deserialization of empty JSON, exception expected`() {
        val json = "{}"

        // This should throw an exception because the JSON is empty and does not contain required fields
        gson.fromJson(json, AuthUxJsonPayload::class.java)
    }

    @Test
    fun `test deserialization of JSON with unexpected fields`() {
        val json = """
            {
                correlationID: 12345,
                action_name: "write_data",
                action_component: "broker",
                "unexpected_field": "unexpected_value",
                params: {
                    operation: "number_matching",
                    sessionID: 67890,
                    code_match: 123456
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
        assertEquals("number_matching", params?.operation)
        assertEquals("67890", params?.sessionId)
        assertEquals("123456", params?.codeMatch)
    }

    @Test(expected = JsonParseException::class)
    fun `test deserialization of invalid JSON`() {
        val json = """
            {
                "correlationID": "12345",
                "action_name": "write_data",
                "action_component": "broker",
                "params": {
                    "operation": "NUMBER_MATCH",
                    "sessionID": "67890",
                    "code_match": "123456"
                }
        """.trimIndent() // Missing closing brace

        gson.fromJson(json, AuthUxJsonPayload::class.java)
    }
}
