//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.nativeauth.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeAuthHeaderValidatorTest {

    @Test
    fun testValidCustomHeaders() {
        val headers = mapOf(
            "x-custom-header" to "value1",
            "x-akamai-sensor" to "sensor-data",
            "x-fraud-signal" to "signal123"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(3, result.size)
        assertEquals("value1", result["x-custom-header"])
        assertEquals("sensor-data", result["x-akamai-sensor"])
        assertEquals("signal123", result["x-fraud-signal"])
    }

    @Test
    fun testHeadersWithoutXPrefixAreRejected() {
        val headers = mapOf(
            "x-valid" to "keep",
            "Authorization" to "Bearer token",
            "Content-Type" to "application/json",
            "custom-header" to "no-x-prefix"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(1, result.size)
        assertEquals("keep", result["x-valid"])
    }

    @Test
    fun testReservedXMsPrefixIsRejected() {
        val headers = mapOf(
            "x-ms-correlation-id" to "abc",
            "x-ms-request-id" to "def",
            "x-valid-header" to "keep"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(1, result.size)
        assertEquals("keep", result["x-valid-header"])
    }

    @Test
    fun testReservedXClientPrefixIsRejected() {
        val headers = mapOf(
            "x-client-SKU" to "Android",
            "x-client-Ver" to "1.0",
            "x-custom" to "keep"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(1, result.size)
        assertEquals("keep", result["x-custom"])
    }

    @Test
    fun testReservedXBrokerPrefixIsRejected() {
        val headers = mapOf(
            "x-broker-version" to "1.0",
            "x-valid" to "keep"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(1, result.size)
        assertEquals("keep", result["x-valid"])
    }

    @Test
    fun testReservedXAppPrefixIsRejected() {
        val headers = mapOf(
            "x-app-name" to "MyApp",
            "x-valid" to "keep"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(1, result.size)
        assertEquals("keep", result["x-valid"])
    }

    @Test
    fun testCaseInsensitivePrefixCheck() {
        val headers = mapOf(
            "X-Custom-Header" to "value1",
            "X-MS-Reserved" to "rejected",
            "X-CLIENT-Info" to "rejected",
            "X-BROKER-Data" to "rejected",
            "X-APP-Version" to "rejected"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(1, result.size)
        assertEquals("value1", result["X-Custom-Header"])
    }

    @Test
    fun testEmptyMapReturnsEmpty() {
        val result = NativeAuthHeaderValidator.filterValidHeaders(emptyMap())
        assertTrue(result.isEmpty())
    }

    @Test
    fun testAllInvalidHeadersReturnsEmpty() {
        val headers = mapOf(
            "Authorization" to "Bearer token",
            "x-ms-foo" to "bar",
            "x-client-bar" to "baz"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertTrue(result.isEmpty())
    }

    @Test
    fun testOriginalHeaderCaseIsPreserved() {
        val headers = mapOf(
            "X-Akamai-Sensor-Data" to "encoded-value"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(1, result.size)
        assertEquals("encoded-value", result["X-Akamai-Sensor-Data"])
    }

    @Test
    fun testMixedValidAndInvalidHeaders() {
        val headers = mapOf(
            "x-fraud-signal" to "signal",
            "Authorization" to "secret",
            "x-ms-telemetry" to "rejected",
            "x-akamai-data" to "keep",
            "x-client-id" to "rejected",
            "Content-Type" to "text/plain",
            "x-custom-trace" to "trace123"
        )

        val result = NativeAuthHeaderValidator.filterValidHeaders(headers)

        assertEquals(3, result.size)
        assertEquals("signal", result["x-fraud-signal"])
        assertEquals("keep", result["x-akamai-data"])
        assertEquals("trace123", result["x-custom-trace"])
    }
}
