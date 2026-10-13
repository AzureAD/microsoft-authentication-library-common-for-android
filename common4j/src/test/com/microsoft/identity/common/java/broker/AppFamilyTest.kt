// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
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
