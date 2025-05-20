package com.microsoft.identity.common.internal.broker

import com.google.gson.Gson
import org.junit.Assert.*
import org.junit.Test

class AuthUxJsonPayloadObjectTest {

    private val gson = Gson()

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

        val payload = gson.fromJson(json, AuthUxJsonPayloadObject::class.java)

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
    fun `test deserialization of JSON with missing fields`() {
        val json = """
            {
                "correlationID": "12345",
                "action_name": "write_data"
            }
        """.trimIndent()

        val payload = gson.fromJson(json, AuthUxJsonPayloadObject::class.java)

        assertNotNull(payload)
        assertEquals("12345", payload.correlationId)
        assertEquals("write_data", payload.actionName)
        assertNull(payload.actionComponent)
        assertNull(payload.params)
    }

    @Test
    fun `test deserialization of empty JSON`() {
        val json = "{}"

        val payload = gson.fromJson(json, AuthUxJsonPayloadObject::class.java)

        assertNotNull(payload)
        assertNull(payload.correlationId)
        assertNull(payload.actionName)
        assertNull(payload.actionComponent)
        assertNull(payload.params)
    }
}