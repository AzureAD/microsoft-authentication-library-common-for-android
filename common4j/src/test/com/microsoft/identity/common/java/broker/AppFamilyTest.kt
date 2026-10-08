// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
package com.microsoft.identity.common.java.broker

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests explicit application-family membership independently of authorization policy.
 */
class AppFamilyTest {
    private val releaseApp = App("example.browser", "release-signer")
    private val debugApp = App("example.browser.dev", "debug-signer")
    private val family = AppFamily(setOf(releaseApp), setOf(debugApp))

    @Test
    fun containsPackage_matchesExplicitReleaseAndDebugVariants() {
        assertTrue(family.containsPackage(releaseApp.packageName))
        assertTrue(family.containsPackage(debugApp.packageName))
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
    fun debugOnlyFamily_recognizesPackageWithoutGrantingReleaseIdentity() {
        val debugOnlyFamily = AppFamily(emptySet(), setOf(debugApp))
        assertTrue(debugOnlyFamily.containsPackage(debugApp.packageName))
        assertTrue(debugOnlyFamily.releaseApps.isEmpty())
    }

    @Test
    fun releaseAndDebugSignersRemainDistinctForTheSamePackage() {
        val debugIdentity = App(releaseApp.packageName, "debug-signer")
        val samePackageFamily = AppFamily(setOf(releaseApp), setOf(debugIdentity))
        assertTrue(samePackageFamily.containsPackage(releaseApp.packageName))
        assertFalse(samePackageFamily.releaseApps.contains(debugIdentity))
        assertFalse(samePackageFamily.debugApps.contains(releaseApp))
    }
}
