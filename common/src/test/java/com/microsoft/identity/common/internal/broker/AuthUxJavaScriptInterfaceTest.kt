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

    private val mockSessionId = "12345678"
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

    private val mockErrorCode = "530003"

    // Matches the Auth UX design-doc wire format: dispatched by action_name = "log_telemetry"
    // (no params.operation), errorCode sent as a JSON number, plus additional params context fields.
    private val logTelemetryTestPayload = """
        {
            correlationID: "corr-1",
            action_name: "log_telemetry",
            action_component: "host",
            params: {
                v: 1,
                sessionID: "sess-1",
                errorCode: $mockErrorCode,
                pageId: "ConvergedTFA",
                trackingId: "track-1"
            }
        }
    """.trimIndent()

    private val logTelemetryMissingErrorCodePayload = """
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

    // A future Auth UX version may add new key/value pairs at the top level and inside params;
    // dispatch must tolerate them and still forward errorCode.
    private val logTelemetryWithFutureFieldsPayload = """
        {
            correlationID: "corr-1",
            action_name: "log_telemetry",
            action_component: "host",
            futureTopLevel: "ignored",
            params: {
                v: 2,
                sessionID: "sess-1",
                errorCode: $mockErrorCode,
                pageId: "ConvergedTFA",
                trackingId: "track-1",
                futureField: "ignored",
                futureNumber: 42
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
    fun `test receiveAuthUxMessage with log_telemetry forwards errorCode to sink exactly once`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryTestPayload)

        // The opaque error code is forwarded to the sink exactly once...
        Assert.assertEquals(listOf(mockErrorCode), sink.received)
        // ...and H3 holds: the telemetry path never touches the number-match device store.
        Assert.assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test receiveAuthUxMessage with log_telemetry and missing errorCode is a no-op`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryMissingErrorCodePayload)

        // Absent errorCode must not throw and must not emit telemetry.
        Assert.assertTrue(sink.received.isEmpty())
    }

    @Test
    fun `test receiveAuthUxMessage with log_telemetry tolerates unknown future fields`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(logTelemetryWithFutureFieldsPayload)

        // New top-level and params key/value pairs must not break dispatch; errorCode still forwarded.
        Assert.assertEquals(listOf(mockErrorCode), sink.received)
        Assert.assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test receiveAuthUxMessage with log_telemetry does not throw when no sink attached`() {
        // Default no-arg construction (no sink): parses and validates without side effects or throwing.
        authUxJavaScriptInterface.receiveAuthUxMessage(logTelemetryTestPayload)

        Assert.assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test receiveAuthUxMessage with number_matching does not invoke telemetry sink`() {
        val sink = RecordingTelemetrySink()
        val interfaceWithSink = AuthUxJavaScriptInterface(sink)

        interfaceWithSink.receiveAuthUxMessage(numberMatchTestPayload)

        // Existing number-match behavior is unchanged (regression-safe)...
        Assert.assertEquals(mockNumberMatchValue, NumberMatchHelper.numberMatchMap[mockSessionId])
        // ...and the telemetry sink is never invoked for a non-telemetry action.
        Assert.assertTrue(sink.received.isEmpty())
    }

    /** Test double that records every error code routed to the telemetry sink. */
    private class RecordingTelemetrySink : AuthUxTelemetrySink {
        val received = mutableListOf<String>()
        override fun onAuthUxServerError(errorCode: String) {
            received.add(errorCode)
        }
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
    fun `test isValidUrlForInterface with scheme-only null-host URI`() {
        // A scheme-only URI (e.g. an openid-vc:// redirect) parses with a null host. It must be
        // rejected rather than throwing on the null-host AAD suffix check.
        val schemeOnlyUrl = "openid-vc://?request_uri=https://verifiedid.did.msidentity.com/x"
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(schemeOnlyUrl))
    }

    @Test
    fun `test isValidUrlForInterface with empty URL`() {
        val emptyUrl = ""
        Assert.assertFalse(AuthUxJavaScriptInterface.isValidUriForInterface(emptyUrl))
    }
}
