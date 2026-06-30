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
package com.microsoft.identity.common.java.providers

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BrokerInstallReferrerBuilderTest {

    @Test
    fun withResumePointer_roundTrips() {
        val url = BrokerInstallReferrerBuilder.withResumePointer(
            "https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal",
            correlationId = "abc-123",
            originPackage = "com.contoso.app"
        )
        val parsed = BrokerInstallReferrerBuilder.parseResumePointer(
            url.substringAfter("referrer=").substringBefore("&").let { decode(it) }
        )
        assertEquals("abc-123" to "com.contoso.app", parsed)
    }

    @Test
    fun withResumePointer_keepsDestination() {
        val url = BrokerInstallReferrerBuilder.withResumePointer(
            "https://play.google.com/store/apps/details?id=com.microsoft.windowsintune.companyportal",
            "cid", "pkg"
        )
        assertTrue(url.contains("id=com.microsoft.windowsintune.companyportal"))
    }

    @Test
    fun withResumePointer_carriesNoLoginHint() {
        val url = BrokerInstallReferrerBuilder.withResumePointer(
            "https://play.google.com/store/apps/details?id=com.azure.authenticator",
            "cid", "pkg"
        )
        assertFalse(url.contains("@"))
    }

    @Test
    fun parseResumePointer_rejectsNonResumeReferrer() {
        assertNull(BrokerInstallReferrerBuilder.parseResumePointer("com.contoso.app"))
        assertNull(BrokerInstallReferrerBuilder.parseResumePointer(null))
    }

    private fun decode(s: String) = java.net.URLDecoder.decode(s, "UTF-8")
}
