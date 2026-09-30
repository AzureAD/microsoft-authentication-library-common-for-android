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
import org.junit.Assert
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

    @Test
    fun malformedHeaderReturnsNull() {
        assertNull(OnboardingBlockingErrorParser.extractBlockingError("1,50058"))
    }

    @Test
    fun headerWithZeroSubErrorFallsBackToServerError() {
        assertEquals(
            "50097",
            OnboardingBlockingErrorParser.extractBlockingError("1,50097,0,1234,routinghint")
        )
    }

    @Test
    fun blankSubErrorFallsBackToServerError() {
        val response = MicrosoftTokenResponse().apply {
            setCliTelemErrorCode("50126")
            setCliTelemSubErrorCode(" ")
        }
        assertEquals("50126", OnboardingBlockingErrorParser.extractBlockingError(response))
    }

    // --- AADSTS codes and shared JS sentinel policy ---

    @Test
    fun aadstsCodesReturnedFromResponseAndHeaderInEitherPosition() {
        for (code in listOf("50058", "50097", "50126")) {
            val response = MicrosoftTokenResponse().apply { setCliTelemErrorCode(code) }
            assertEquals(code, OnboardingBlockingErrorParser.extractBlockingError(response))
            response.setCliTelemErrorCode("65001")
            response.setCliTelemSubErrorCode(code)
            assertEquals(code, OnboardingBlockingErrorParser.extractBlockingError(response))

            assertEquals(code, OnboardingBlockingErrorParser.extractBlockingError("1,$code,0,1234,routinghint"))
            assertEquals(code, OnboardingBlockingErrorParser.extractBlockingError("1,65001,$code,1234,routinghint"))
        }
    }

    @Test
    fun sharedSentinelPolicyOnlyRejectsLiteralZero() {
        Assert.assertTrue(OnboardingBlockingErrorParser.isNonBlockingOnboardingErrorCode("0"))
        for (code in listOf(null, "", " ", "50058", "50097", "50126", "65001")) {
            Assert.assertFalse(OnboardingBlockingErrorParser.isNonBlockingOnboardingErrorCode(code))
        }
    }

    // --- extractBlockingErrorsFromAuthorizationErrorCodes (Path B / OAuth error_codes) ---

    @Test
    fun authzErrorCodes_NullReturnsEmpty() {
        Assert.assertTrue(
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes(null).isEmpty()
        )
    }

    @Test
    fun authzErrorCodes_BlankReturnsEmpty() {
        Assert.assertTrue(
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes("").isEmpty()
        )
        Assert.assertTrue(
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes("   ").isEmpty()
        )
    }

    @Test
    fun authzErrorCodes_SingleCodeReturned() {
        Assert.assertEquals(
            listOf("53003"),
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes("53003")
        )
    }

    @Test
    fun authzErrorCodes_MultipleCodesAllReturnedInOrder() {
        // eSTS commonly emits multiple codes when one failure has multiple causes.
        Assert.assertEquals(
            listOf("53003", "65001"),
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes("53003,65001")
        )
    }

    @Test
    fun authzErrorCodes_AadstsCodesReturnedWithOtherCodes() {
        Assert.assertEquals(
            listOf("50058", "53003", "50097", "50126"),
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes(
                "50058,53003,50097,50126,50058"
            )
        )
    }

    @Test
    fun authzErrorCodes_AadstsCodesReturnedWithoutOtherCodes() {
        Assert.assertEquals(
            listOf("50058", "50097", "50126"),
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes("50058,50097,50126")
        )
    }

    @Test
    fun authzErrorCodes_ZeroSentinelFilteredOut() {
        Assert.assertEquals(
            listOf("53003"),
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes("0,53003")
        )
    }

    @Test
    fun authzErrorCodes_EmptyEntriesFilteredOut() {
        // Trailing/leading commas → empty entries → ignored.
        Assert.assertEquals(
            listOf("53003", "65001"),
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes(",53003,,65001,")
        )
    }

    @Test
    fun authzErrorCodes_WhitespaceTrimmed() {
        Assert.assertEquals(
            listOf("53003", "65001"),
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes(" 53003 , 65001 ")
        )
    }

    @Test
    fun authzErrorCodes_DuplicatesDeduped() {
        Assert.assertEquals(
            listOf("53003", "65001"),
            OnboardingBlockingErrorParser.extractBlockingErrorsFromAuthorizationErrorCodes("53003,65001,53003")
        )
    }
}
