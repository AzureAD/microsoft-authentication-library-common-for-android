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

package com.microsoft.identity.common.internal.providers.oauth2

import androidx.webkit.JavaScriptReplyProxy
import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import io.mockk.slot
import org.json.JSONArray
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import java.lang.RuntimeException

class PasskeyReplyChannelTest {

    private lateinit var mockReplyProxy: JavaScriptReplyProxy
    private lateinit var passkeyReplyChannel: PasskeyReplyChannel
    private val testRequestType = "test_request_type"

    @Before
    fun setUp() {
        mockReplyProxy = mockk(relaxed = true)
        passkeyReplyChannel = PasskeyReplyChannel(mockReplyProxy, testRequestType)
    }

    @Test
    fun `postSuccess sends correct success message format`() {
        // Given
        val testJson = """{"key": "value"}"""
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postSuccess(testJson)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val capturedMessage = messageSlot.captured
        val messageArray = JSONArray(capturedMessage)

        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, messageArray.getString(0))
        assertEquals(testRequestType, messageArray.getString(2))

        // Verify the JSON object is parsed correctly
        val dataObject = messageArray.getJSONObject(1)
        assertEquals("value", dataObject.getString("key"))
    }

    @Test
    fun `postSuccess handles invalid JSON gracefully`() {
        // Given
        val invalidJson = "invalid json string"
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postSuccess(invalidJson)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val capturedMessage = messageSlot.captured
        val messageArray = JSONArray(capturedMessage)

        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, messageArray.getString(0))
        assertEquals(invalidJson, messageArray.getString(1)) // Should use raw string when JSON parsing fails
        assertEquals(testRequestType, messageArray.getString(2))
    }

    @Test
    fun `postError with string message sends correct error format`() {
        // Given
        val errorMessage = "Test error message"
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(errorMessage)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val capturedMessage = messageSlot.captured
        val messageArray = JSONArray(capturedMessage)

        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, messageArray.getString(0))
        assertEquals(errorMessage, messageArray.getString(1))
        assertEquals(testRequestType, messageArray.getString(2))
    }

    @Test
    fun `postError with throwable sends correct error format`() {
        // Given
        val exceptionMessage = "Exception occurred"
        val testException = RuntimeException(exceptionMessage)
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(testException)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val capturedMessage = messageSlot.captured
        val messageArray = JSONArray(capturedMessage)

        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, messageArray.getString(0))
        assertEquals(exceptionMessage, messageArray.getString(1))
        assertEquals(testRequestType, messageArray.getString(2))
    }

    @Test
    fun `postError with throwable handles null message`() {
        // Given
        val testException = RuntimeException(null as String?)
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(testException)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val capturedMessage = messageSlot.captured
        val messageArray = JSONArray(capturedMessage)

        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, messageArray.getString(0))
        assertEquals("Unknown error", messageArray.getString(1))
        assertEquals(testRequestType, messageArray.getString(2))
    }

    @Test
    fun `send method handles postMessage exceptions gracefully`() {
        // Given
        val testJson = """{"key": "value"}"""
        every { mockReplyProxy.postMessage(any<String>()) } throws RuntimeException("PostMessage failed")

        // When/Then - Should not throw exception
        assertDoesNotThrow {
            passkeyReplyChannel.postSuccess(testJson)
        }

        verify { mockReplyProxy.postMessage(any<String>()) }
    }

    @Test
    fun `constructor with default request type uses unknown`() {
        // Given
        val channelWithDefaultType = PasskeyReplyChannel(mockReplyProxy)
        val messageSlot = slot<String>()

        // When
        channelWithDefaultType.postSuccess("""{"test": "data"}""")

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val capturedMessage = messageSlot.captured
        val messageArray = JSONArray(capturedMessage)
        assertEquals("unknown", messageArray.getString(2))
    }

    @Test
    fun `ReplyMessage Success toString creates valid JSON array`() {
        // Given
        val testJson = """{"key": "value"}"""
        val successMessage = PasskeyReplyChannel.ReplyMessage.Success(testJson, testRequestType)

        // When
        val result = successMessage.toString()

        // Then
        val messageArray = JSONArray(result)
        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, messageArray.getString(0))
        assertEquals(testRequestType, messageArray.getString(2))

        val dataObject = messageArray.getJSONObject(1)
        assertEquals("value", dataObject.getString("key"))
    }

    @Test
    fun `ReplyMessage Error toString creates valid JSON array`() {
        // Given
        val errorMessage = "Test error"
        val errorReplyMessage = PasskeyReplyChannel.ReplyMessage.Error(errorMessage, testRequestType)

        // When
        val result = errorReplyMessage.toString()

        // Then
        val messageArray = JSONArray(result)
        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.ERROR_STATUS, messageArray.getString(0))
        assertEquals(errorMessage, messageArray.getString(1))
        assertEquals(testRequestType, messageArray.getString(2))
    }

    @Test
    fun `ReplyMessage Success handles malformed JSON in toString`() {
        // Given
        val malformedJson = "not a valid json"
        val successMessage = PasskeyReplyChannel.ReplyMessage.Success(malformedJson, testRequestType)

        // When
        val result = successMessage.toString()

        // Then
        assertDoesNotThrow {
            val messageArray = JSONArray(result)
            assertEquals(3, messageArray.length())
            assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, messageArray.getString(0))
            assertEquals(malformedJson, messageArray.getString(1)) // Should use raw string
            assertEquals(testRequestType, messageArray.getString(2))
        }
    }

    @Test
    fun `constants have correct values`() {
        assertEquals("success", PasskeyReplyChannel.SUCCESS_STATUS)
        assertEquals("error", PasskeyReplyChannel.ERROR_STATUS)
        assertEquals("PasskeyReplyChannel", PasskeyReplyChannel.TAG)
    }

    @Test
    fun `ReplyMessage Success handles empty JSON object`() {
        // Given
        val emptyJson = "{}"
        val successMessage = PasskeyReplyChannel.ReplyMessage.Success(emptyJson, testRequestType)

        // When
        val result = successMessage.toString()

        // Then
        val messageArray = JSONArray(result)
        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, messageArray.getString(0))

        val dataObject = messageArray.getJSONObject(1)
        assertEquals(0, dataObject.length()) // Empty JSON object
        assertEquals(testRequestType, messageArray.getString(2))
    }

    @Test
    fun `ReplyMessage Success handles complex JSON structures`() {
        // Given
        val complexJson = """
        {
            "nested": {
                "array": [1, 2, 3],
                "boolean": true,
                "null": null
            },
            "string": "test"
        }
        """.trimIndent()
        val successMessage = PasskeyReplyChannel.ReplyMessage.Success(complexJson, testRequestType)

        // When
        val result = successMessage.toString()

        // Then
        val messageArray = JSONArray(result)
        assertEquals(3, messageArray.length())
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, messageArray.getString(0))

        val dataObject = messageArray.getJSONObject(1)
        assertEquals("test", dataObject.getString("string"))
        assertEquals(true, dataObject.getJSONObject("nested").getBoolean("boolean"))
        assertEquals(testRequestType, messageArray.getString(2))
    }

    private fun assertDoesNotThrow(executable: () -> Unit) {
        try {
            executable()
        } catch (e: Exception) {
            fail("Expected no exception, but got: ${e.message}")
        }
    }
}
