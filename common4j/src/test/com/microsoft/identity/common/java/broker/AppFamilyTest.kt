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
package com.microsoft.identity.common.java.broker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests explicit application-family membership independently of authorization policy.
 */
class AppFamilyTest {
    private val storeApp = App("example.browser", "store-signer")
    private val devApp = App("example.browser.dev", "direct-distribution-signer")
    private val family = AppFamily(setOf(storeApp, devApp))

    @Test
    fun containsPackage_matchesExplicitVariants() {
        assertTrue(family.containsPackage(storeApp.packageName))
        assertTrue(family.containsPackage(devApp.packageName))
    }

    @Test
    fun containsPackage_usesCallerValidationCaseSemantics() {
        assertTrue(family.containsPackage("EXAMPLE.BROWSER"))
        assertTrue(family.containsPackage("EXAMPLE.BROWSER.DEV"))
    }

    @Test
    fun containsPackage_doesNotMatchPrefixesSuffixesOrUnregisteredVariants() {
        listOf(
            "example",
            "example.browser.beta",
            "example.browser.dev.extra",
            "other.example.browser",
            " example.browser",
            ""
        ).forEach { assertFalse(it, family.containsPackage(it)) }
    }

    @Test
    fun containsPackage_doesNotMatchUnresolvedCaller() {
        assertFalse(family.containsPackage(null))
    }

    @Test
    fun emptyFamily_doesNotRecognizeAnyPackage() {
        val emptyFamily = AppFamily(emptySet())
        assertFalse(emptyFamily.containsPackage(storeApp.packageName))
        assertFalse(emptyFamily.containsPackage(devApp.packageName))
    }

    @Test
    fun multipleProductionSignersRemainDistinctForTheSamePackage() {
        val directIdentity = App(storeApp.packageName, "direct-distribution-signer")
        val samePackageFamily = AppFamily(setOf(storeApp, directIdentity))
        assertTrue(samePackageFamily.containsPackage(storeApp.packageName))
        assertEquals(2, samePackageFamily.apps.size)
        assertTrue(samePackageFamily.apps.contains(storeApp))
        assertTrue(samePackageFamily.apps.contains(directIdentity))
    }
}
