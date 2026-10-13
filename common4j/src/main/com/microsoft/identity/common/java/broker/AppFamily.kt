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

/**
 * Groups related application variants without granting them authorization.
 * Each [App] retains its own package and signing certificate identity.
 *
 * This is application grouping metadata, not an OAuth family of client IDs.
 *
 * @property apps Explicit package and signing-certificate identities belonging to the family.
 */
data class AppFamily(
    val apps: Set<App>
) {
    /**
     * Checks explicit package membership across all registered signing identities.
     * A match does not verify the caller's signing certificate.
     *
     * @param packageName Package resolved by the platform, or null for an unresolved caller.
     * @return Whether the package belongs to this family, ignoring case as caller validation does.
     */
    fun containsPackage(packageName: String?): Boolean =
        apps.any { it.packageName.equals(packageName, ignoreCase = true) }
}
