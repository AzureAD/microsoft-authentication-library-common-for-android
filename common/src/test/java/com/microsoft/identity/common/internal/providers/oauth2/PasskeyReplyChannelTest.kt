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

import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialInterruptedException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnknownException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialInterruptedException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnknownException
import androidx.credentials.exceptions.NoCredentialException
import androidx.webkit.JavaScriptReplyProxy
import io.mockk.mockk
import io.mockk.verify
import io.mockk.every
import io.mockk.slot
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

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
    fun `postSuccess sends correct message format`() {
        // Given
        val testJson = """{"key": "value"}"""
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postSuccess(testJson)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val messageObject = JSONObject(messageSlot.captured)
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, messageObject.getString(PasskeyReplyChannel.STATUS_KEY))
        assertEquals(testRequestType, messageObject.getString(PasskeyReplyChannel.TYPE_KEY))

        val dataObject = messageObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
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

        val messageObject = JSONObject(messageSlot.captured)
        val dataObject = messageObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(0, dataObject.length())
    }

    @Test
    fun `postError with string sends correct error format`() {
        // Given
        val errorMessage = "Test error message"
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(RuntimeException(errorMessage))

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val messageObject = JSONObject(messageSlot.captured)
        val dataObject = messageObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)

        assertEquals(errorMessage, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_MESSAGE_KEY))
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_NOT_ALLOWED_ERROR,
            dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with CreateCredentialCancellationException returns NotAllowedError`() {
        // Given
        val exception = CreateCredentialCancellationException("User cancelled")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_NOT_ALLOWED_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with GetCredentialCancellationException returns NotAllowedError`() {
        // Given
        val exception = GetCredentialCancellationException("User cancelled")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_NOT_ALLOWED_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with CreateCredentialInterruptedException returns AbortError`() {
        // Given
        val exception = CreateCredentialInterruptedException("Interrupted")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_ABORT_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with GetCredentialInterruptedException returns AbortError`() {
        // Given
        val exception = GetCredentialInterruptedException("Interrupted")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_ABORT_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with CreateCredentialProviderConfigurationException returns NotSupportedError`() {
        // Given
        val exception = CreateCredentialProviderConfigurationException("Config missing")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_NOT_SUPPORTED_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with GetCredentialProviderConfigurationException returns NotSupportedError`() {
        // Given
        val exception = GetCredentialProviderConfigurationException("Config missing")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_NOT_SUPPORTED_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with CreateCredentialUnknownException returns UnknownError`() {
        // Given
        val exception = CreateCredentialUnknownException("Unknown error")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_UNKNOWN_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with GetCredentialUnknownException returns UnknownError`() {
        // Given
        val exception = GetCredentialUnknownException("Unknown error")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_UNKNOWN_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with NoCredentialException returns NotAllowedError`() {
        // Given
        val exception = NoCredentialException("No credentials")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_NOT_ALLOWED_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError with generic exception returns NotAllowedError`() {
        // Given
        val exception = RuntimeException("Generic error")
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals(PasskeyReplyChannel.DOM_EXCEPTION_NOT_ALLOWED_ERROR, dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_NAME_KEY))
    }

    @Test
    fun `postError handles null exception message`() {
        // Given
        val exception = RuntimeException(null as Throwable?)
        val messageSlot = slot<String>()

        // When
        passkeyReplyChannel.postError(exception)

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val dataObject = JSONObject(messageSlot.captured).getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals("Unknown error (empty message)", dataObject.getString(PasskeyReplyChannel.DOM_EXCEPTION_MESSAGE_KEY))
    }

    @Test
    fun `send throws exception when postMessage fails`() {
        // Given
        val testJson = """{"key": "value"}"""
        val expectedException = RuntimeException("PostMessage failed")
        every { mockReplyProxy.postMessage(any<String>()) } throws expectedException

        // When/Then - Should throw the exception
        val thrownException = assertThrows(RuntimeException::class.java) {
            passkeyReplyChannel.postSuccess(testJson)
        }

        assertEquals("PostMessage failed", thrownException.message)
        verify { mockReplyProxy.postMessage(any<String>()) }
    }

    @Test
    fun `constructor uses unknown as default request type`() {
        // Given
        val channelWithDefaultType = PasskeyReplyChannel(mockReplyProxy)
        val messageSlot = slot<String>()

        // When
        channelWithDefaultType.postSuccess("""{"test": "data"}""")

        // Then
        verify { mockReplyProxy.postMessage(capture(messageSlot)) }

        val messageObject = JSONObject(messageSlot.captured)
        assertEquals("unknown", messageObject.getString(PasskeyReplyChannel.TYPE_KEY))
    }

    @Test
    fun `ReplyMessage Success handles complex JSON structures`() {
        // Given
        val complexJson = """{"nested": {"array": [1, 2, 3], "boolean": true}, "string": "test"}"""
        val successMessage = PasskeyReplyChannel.ReplyMessage.Success(complexJson, testRequestType)

        // When
        val result = successMessage.toString()

        // Then
        val messageObject = JSONObject(result)
        assertEquals(PasskeyReplyChannel.SUCCESS_STATUS, messageObject.getString(PasskeyReplyChannel.STATUS_KEY))

        val dataObject = messageObject.getJSONObject(PasskeyReplyChannel.DATA_KEY)
        assertEquals("test", dataObject.getString("string"))
        assertEquals(true, dataObject.getJSONObject("nested").getBoolean("boolean"))
    }

    @Test
    fun `constants have correct values`() {
        assertEquals("success", PasskeyReplyChannel.SUCCESS_STATUS)
        assertEquals("error", PasskeyReplyChannel.ERROR_STATUS)
        assertEquals("PasskeyReplyChannel", PasskeyReplyChannel.TAG)

        assertEquals("NotAllowedError", PasskeyReplyChannel.DOM_EXCEPTION_NOT_ALLOWED_ERROR)
        assertEquals("AbortError", PasskeyReplyChannel.DOM_EXCEPTION_ABORT_ERROR)
        assertEquals("NotSupportedError", PasskeyReplyChannel.DOM_EXCEPTION_NOT_SUPPORTED_ERROR)
        assertEquals("UnknownError", PasskeyReplyChannel.DOM_EXCEPTION_UNKNOWN_ERROR)
    }

}
