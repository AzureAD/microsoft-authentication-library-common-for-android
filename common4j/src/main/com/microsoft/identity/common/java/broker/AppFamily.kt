// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
package com.microsoft.identity.common.java.broker

/**
 * Groups related application variants without granting them authorization.
 * Each [App] retains its own package and signing certificate identity.
 *
 * This is application grouping metadata, not an OAuth family of client IDs.
 *
 * @property releaseApps Application identities signed with release certificates.
 * @property debugApps Application identities requiring the consumer's debug-trust policy.
 */
data class AppFamily(
    val releaseApps: Set<App>,
    val debugApps: Set<App> = emptySet()
) {
    /**
     * Checks explicit package membership, including debug-only variants.
     * A match does not verify the caller's signing certificate.
     *
     * @param packageName Package resolved by the platform, or null for an unresolved caller.
     * @return Whether the package belongs to this family, ignoring case as caller validation does.
     */
    fun containsPackage(packageName: String?): Boolean =
        releaseApps.any { it.packageName.equals(packageName, ignoreCase = true) } ||
                debugApps.any { it.packageName.equals(packageName, ignoreCase = true) }
}
