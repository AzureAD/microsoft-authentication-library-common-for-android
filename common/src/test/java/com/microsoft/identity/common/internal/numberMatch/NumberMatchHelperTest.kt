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

    private val mockSessionId = "1234"
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
}
