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
package com.microsoft.identity.common.java.commands.webapps

import com.google.gson.JsonParser
import com.microsoft.identity.common.java.util.ObjectMapper
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Verifies the serialized JSON schema (field names and nesting) of the WebApps API
 * error response produced by [WebAppError], including its nested [WebAppErrorDetails]
 * and [MatsProperties].
 */
class WebAppErrorTest {

    @Test
    fun serialize_producesExpectedSchema() {
        val webAppError = WebAppError(
            errorCode = "OSError",
            description = "Something went wrong",
            extra = WebAppErrorDetails(
                error = 42,
                protocolError = "invalid_grant",
                status = "PERSISTENT_ERROR",
                properties = MatsProperties(matsData = "mats-blob")
            )
        )

        val json = JsonParser.parseString(
            ObjectMapper.serializeObjectToJsonString(webAppError)
        ).asJsonObject

        // Top-level WebAppError schema.
        assertEquals(
            setOf("code", "description", "ext"),
            json.keySet()
        )
        assertEquals("OSError", json.get("code").asString)
        assertEquals("Something went wrong", json.get("description").asString)

        // Nested WebAppErrorDetails schema (under "ext").
        val ext = json.getAsJsonObject("ext")
        assertEquals(
            setOf("error", "protocol_error", "status", "properties"),
            ext.keySet()
        )
        assertEquals(42, ext.get("error").asInt)
        assertEquals("invalid_grant", ext.get("protocol_error").asString)
        assertEquals("PERSISTENT_ERROR", ext.get("status").asString)

        // Nested MatsProperties schema (under "properties").
        val properties = ext.getAsJsonObject("properties")
        assertEquals(setOf("MATS"), properties.keySet())
        assertEquals("mats-blob", properties.get("MATS").asString)
    }
}
