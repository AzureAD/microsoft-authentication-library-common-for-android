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

    @Test
    fun `test deserialization of log_telemetry payload matches design-doc shape`() {
        // Exact Auth UX design-doc wire format for the log_telemetry action.
        val json = """
            {
                correlationID: "corr-1",
                action_name: "log_telemetry",
                action_component: "host",
                params: {
                    v: 1,
                    sessionID: "sess-1",
                    errorCode: 530003,
                    pageId: "ConvergedTFA",
                    trackingId: "track-1"
                }
            }
        """.trimIndent()

        val payload = gson.fromJson(json, AuthUxJsonPayload::class.java)

        assertEquals("log_telemetry", payload.actionName)
        assertEquals("host", payload.actionComponent)

        val params = payload.params
        assertNotNull(params)
        // errorCode is sent as a JSON number but captured as an opaque string.
        assertEquals("530003", params?.errorCode)
        assertEquals(1, params?.version)
        assertEquals("sess-1", params?.sessionId)
        assertEquals("ConvergedTFA", params?.pageId)
        assertEquals("track-1", params?.trackingId)
        // The log_telemetry action carries no params.operation.
        assertNull(params?.operation)
    }

    @Test
    fun `test deserialization of log_telemetry payload without errorCode yields null`() {
        val json = """
            {
                correlationID: "corr-1",
                action_name: "log_telemetry",
                action_component: "host",
                params: {
                    v: 1,
                    sessionID: "sess-1",
                    pageId: "ConvergedTFA",
                    trackingId: "track-1"
                }
            }
        """.trimIndent()

        val payload = gson.fromJson(json, AuthUxJsonPayload::class.java)

        val params = payload.params
        assertNotNull(params)
        assertNull(params?.errorCode)
    }

    @Test
    fun `test deserialization of log_telemetry payload tolerates unknown top-level and params fields`() {
        // Forward-compat: Auth UX may add new key/value pairs at the top level and inside params;
        // unknown fields must not break parsing of the known errorCode field.
        val json = """
            {
                correlationID: "corr-1",
                action_name: "log_telemetry",
                action_component: "host",
                futureTopLevel: "ignored",
                params: {
                    v: 2,
                    sessionID: "sess-1",
                    errorCode: 530003,
                    pageId: "ConvergedTFA",
                    trackingId: "track-1",
                    futureField: "ignored",
                    futureNumber: 42
                }
            }
        """.trimIndent()

        val payload = gson.fromJson(json, AuthUxJsonPayload::class.java)

        val params = payload.params
        assertNotNull(params)
        assertEquals("530003", params?.errorCode)
        assertEquals("ConvergedTFA", params?.pageId)
        assertEquals("track-1", params?.trackingId)
    }
}
