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
package com.microsoft.identity.common.internal.numberMatch

import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

class NumberMatchHelperTest {

    private val mockSessionId = "12345678" // Updated to valid 8-char alphanumeric
    private val mockNumberMatchValue = "00"

    @Before
    fun setUp() {
        // Clear the map before each test to ensure a clean state
        NumberMatchHelper.clearNumberMatchMap()
    }

    @Test
    fun `test storeNumberMatch with valid inputs`() {
        // Store a valid session ID and number match
        val sessionId = mockSessionId
        val numberMatch = mockNumberMatchValue
        NumberMatchHelper.storeNumberMatch(sessionId, numberMatch)

        // Verify that the map contains the correct value
        assertEquals(mockNumberMatchValue, NumberMatchHelper.numberMatchMap[sessionId])
    }

    @Test
    fun `test storeNumberMatch with null sessionId`() {
        // Attempt to store a null session ID
        val numberMatch = mockNumberMatchValue
        NumberMatchHelper.storeNumberMatch(null, numberMatch)

        // Verify that the map is still empty
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test storeNumberMatch with null numberMatch`() {
        // Attempt to store a null number match
        val sessionId = mockSessionId
        NumberMatchHelper.storeNumberMatch(sessionId, null)

        // Verify that the map is still empty
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test clearNumberMatchMap`() {
        // Add an entry to the map
        val sessionId = mockSessionId
        val numberMatch = mockNumberMatchValue
        NumberMatchHelper.storeNumberMatch(sessionId, numberMatch)

        // Clear the map
        NumberMatchHelper.clearNumberMatchMap()

        // Verify that the map is empty
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test storeNumberMatch with invalid numberMatch values`() {
        val validSessionId = "12345678" // valid 8-char alphanumeric
        val invalidNumberMatches = listOf(null, "", "1", "123", "a2", "2a", "!2", "0 ", " 0", "0a", "a0")
        for (numberMatch in invalidNumberMatches) {
            NumberMatchHelper.clearNumberMatchMap()
            NumberMatchHelper.storeNumberMatch(validSessionId, numberMatch)
            assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
        }
    }

    @Test
    fun `test storeNumberMatch with valid numberMatch values`() {
        val validSessionId = "12345678"
        val validNumberMatches = listOf("00", "01", "99", "42")
        for (numberMatch in validNumberMatches) {
            NumberMatchHelper.clearNumberMatchMap()
            NumberMatchHelper.storeNumberMatch(validSessionId, numberMatch)
            assertEquals(numberMatch, NumberMatchHelper.numberMatchMap[validSessionId])
        }
    }

    @Test
    fun `test storeNumberMatch with invalid sessionId values`() {
        val validNumberMatch = "12"
        val invalidSessionIds = listOf(null, "", "123", "1234567", "123456789", "1234567!", "zzzzzzzz@", "1234-5678", "not-a-guid", "1234567_", " ", "-", "{12345678}")
        for (sessionId in invalidSessionIds) {
            NumberMatchHelper.clearNumberMatchMap()
            NumberMatchHelper.storeNumberMatch(sessionId, validNumberMatch)
            assertTrue("Failed for sessionId: $sessionId", NumberMatchHelper.numberMatchMap.isEmpty())
        }
    }

    @Test
    fun `test storeNumberMatch with valid sessionId GUIDs`() {
        val validNumberMatch = "55"
        val validGuids = listOf(
            "123e4567-e89b-12d3-a456-426614174000",
            "A23E4567-E89B-12D3-A456-426614174000",
            "abcdefab-1234-5678-abcd-abcdefabcdef"
        )
        for (guid in validGuids) {
            NumberMatchHelper.clearNumberMatchMap()
            NumberMatchHelper.storeNumberMatch(guid, validNumberMatch)
            assertEquals(validNumberMatch, NumberMatchHelper.numberMatchMap[guid])
        }
    }

    @Test
    fun `test storeNumberMatch with valid 8-char alphanumeric sessionIds`() {
        val validNumberMatch = "77"
        val validSessionIds = listOf("abcdefgh", "ABCDEFGH", "12345678", "A1B2C3D4", "z9Y8x7W6")
        for (sessionId in validSessionIds) {
            NumberMatchHelper.clearNumberMatchMap()
            NumberMatchHelper.storeNumberMatch(sessionId, validNumberMatch)
            assertEquals(validNumberMatch, NumberMatchHelper.numberMatchMap[sessionId])
        }
    }

    @Test
    fun `test storeNumberMatch with invalid sessionId and invalid numberMatch`() {
        val invalidSessionId = "bad!id"
        val invalidNumberMatch = "a1"
        NumberMatchHelper.storeNumberMatch(invalidSessionId, invalidNumberMatch)
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test storeNumberMatch with valid sessionId but invalid numberMatch`() {
        val validSessionId = "12345678"
        val invalidNumberMatch = "a1"
        NumberMatchHelper.storeNumberMatch(validSessionId, invalidNumberMatch)
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }

    @Test
    fun `test storeNumberMatch with invalid sessionId but valid numberMatch`() {
        val invalidSessionId = "bad!id"
        val validNumberMatch = "12"
        NumberMatchHelper.storeNumberMatch(invalidSessionId, validNumberMatch)
        assertTrue(NumberMatchHelper.numberMatchMap.isEmpty())
    }
}
