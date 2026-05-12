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
package com.microsoft.identity.common.java.telemetry

import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenResponse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OnboardingBlockingErrorParserTest {

    // --- MicrosoftTokenResponse overload ---

    @Test
    fun nullResponseReturnsNull() {
        assertNull(OnboardingBlockingErrorParser.extractBlockingError(null as MicrosoftTokenResponse?))
    }

    @Test
    fun emptyResponseReturnsNull() {
        val response = MicrosoftTokenResponse()
        assertNull(OnboardingBlockingErrorParser.extractBlockingError(response))
    }

    @Test
    fun zeroErrorCodeReturnsNull() {
        val response = MicrosoftTokenResponse().apply {
            setCliTelemErrorCode("0")
            setCliTelemSubErrorCode("0")
        }
        assertNull(OnboardingBlockingErrorParser.extractBlockingError(response))
    }

    @Test
    fun subErrorPreferredOverError() {
        val response = MicrosoftTokenResponse().apply {
            setCliTelemErrorCode("65001")
            setCliTelemSubErrorCode("interaction_required")
        }
        assertEquals("interaction_required", OnboardingBlockingErrorParser.extractBlockingError(response))
    }

    @Test
    fun errorReturnedWhenNoSubError() {
        val response = MicrosoftTokenResponse().apply {
            setCliTelemErrorCode("53000")
        }
        assertEquals("53000", OnboardingBlockingErrorParser.extractBlockingError(response))
    }

    @Test
    fun errorReturnedWhenSubErrorIsZero() {
        val response = MicrosoftTokenResponse().apply {
            setCliTelemErrorCode("53000")
            setCliTelemSubErrorCode("0")
        }
        assertEquals("53000", OnboardingBlockingErrorParser.extractBlockingError(response))
    }

    // --- Header string overload ---

    @Test
    fun nullHeaderReturnsNull() {
        assertNull(OnboardingBlockingErrorParser.extractBlockingError(null as String?))
    }

    @Test
    fun blankHeaderReturnsNull() {
        assertNull(OnboardingBlockingErrorParser.extractBlockingError(""))
    }

    @Test
    fun headerWithErrorReturnsErrorCode() {
        // Format: version,errorCode,subErrorCode,timeSinceTokenIssuance,tokenRoutingHint
        val header = "1,53000,0,1234,routinghint"
        assertEquals("53000", OnboardingBlockingErrorParser.extractBlockingError(header))
    }

    @Test
    fun headerWithZeroErrorReturnsNull() {
        val header = "1,0,0,1234,routinghint"
        assertNull(OnboardingBlockingErrorParser.extractBlockingError(header))
    }
}
