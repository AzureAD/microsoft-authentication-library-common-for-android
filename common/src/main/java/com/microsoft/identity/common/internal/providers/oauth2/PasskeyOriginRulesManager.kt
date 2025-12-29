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
package com.microsoft.identity.common.internal.providers.oauth2

import com.microsoft.identity.common.BuildConfig
import com.microsoft.identity.common.logging.Logger
import java.net.URI

/**
 * Manages allowed origin rules for Passkey/WebAuthN APIs.
 * Ensures only trusted Microsoft origins can access sensitive credential operations.
 *
 * **Origin Categories:**
 * 1. **Production origins** - Allow all paths (any path is safe)
 * 2. **Sovereign cloud origins** - Require "/fido" as a path segment
 *
 * **Security Features:**
 * - Validates scheme must be HTTPS
 * - Prevents subdomain spoofing (e.g., attacker.com.microsoft.com)
 * - Prevents prefix attacks (e.g., evillogin.microsoft.com)
 * - Requires exact domain matching
 * - Case-insensitive FIDO path matching for sovereign clouds
 */
object PasskeyOriginRulesManager {
    private const val TAG = "PasskeyOriginRulesManager"
    private const val HTTPS_SCHEME = "https"
    private const val FIDO_SEGMENT = "fido"

    // Production origins - allow any path
    private val PRODUCTION_ORIGINS = setOf(
        "https://login.microsoft.com",
        "https://account.live.com",
        "https://mysignins.microsoft.com",
        "https://mysignins.azure.us",
        "https://mysignins.microsoft.scloud",
        "https://mysignins.eaglex.ic.gov"
    )

    // Sovereign cloud origins - require FIDO path
    private val SOVEREIGN_CLOUD_ORIGINS = setOf(
        "https://login.microsoftonline.us",
        "https://login.microsoftonline.microsoft.scloud",
        "https://login.microsoftonline.eaglex.ic.gov",
        "https://login.sovcloud-identity.fr",
        "https://login.sovcloud-identity.de",
        "https://login.sovcloud-identity.sg"
    )

    // PPE origins
    private val ALLOWED_ORIGIN_PPE= setOf(
        "https://account.live-int.com",
        "https://login.windows-ppe.net",
        "https://mysignins-ppe.microsoft.com"
    )

    /**
     * Checks if the provided URL is allowed to access Passkey/WebAuthN APIs.
     *
     * **Validation Rules:**
     * - URL must have HTTPS scheme
     * - Host must exactly match an allowed origin
     * - For sovereign cloud origins, the path must contain "fido" as a complete segment
     *   (e.g., "/fido" or "/fido/endpoint" are valid, but "/fidoauth" is not)
     * - For production origins, any path is allowed
     *
     * @param url The URL to validate
     * @return `true` if the URL is from an allowed origin and meets all validation rules, `false` otherwise
     */
    @JvmStatic
    fun isAllowedOrigin(url: String): Boolean {
        return try {
            val uri = URI(url)

            // Validate scheme
            if (uri.scheme.lowercase() != HTTPS_SCHEME) {
                return false
            }

            // Validate host and build origin
            val host = uri.host ?: return false
            val origin = "https://$host".lowercase()

            // Check if it's a production origin (any path allowed)
            if (PRODUCTION_ORIGINS.contains(origin)){
                return true
            }

            // Check if it's a sovereign cloud origin (requires FIDO path)
            if (SOVEREIGN_CLOUD_ORIGINS.contains(origin)) {
                return hasFidoPathSegment(uri.path)
            }

            false
        } catch (throwable: Throwable) {
            Logger.error(TAG, "Error validating origin for URL.", throwable)
            false
        }
    }

    /**
     * Checks if the path contains "fido" as a complete path segment.
     *
     * Valid examples:
     * - "/fido" ✓
     * - "/fido/" ✓
     * - "/fido/endpoint" ✓
     * - "/some/path/fido" ✓
     * - "/some/path/fido/endpoint" ✓
     *
     * Invalid examples:
     * - "" ✗
     * - "/fidoauth" ✗ (fido is not a complete segment)
     * - "/authenticate" ✗ (no fido)
     *
     * @param path The path to validate (e.g., "/fido" or "/some/fido/endpoint")
     * @return `true` if path contains "fido" as a complete segment, `false` otherwise
     */
    private fun hasFidoPathSegment(path: String?): Boolean {
        if (path.isNullOrEmpty()) {
            return false
        }

        // Split by '/' and check for "fido" (case-insensitive)
        val segments = path.split("/")
        return segments.any { it.equals(FIDO_SEGMENT, ignoreCase = true) }
    }

    /**
     * Returns the set of all allowed origin rules.
     *
     * @return Set containing all production and sovereign cloud origin URLs
     */
    fun getAllowedOriginRules(): Set<String> {
        return if (BuildConfig.DEBUG) {
            PRODUCTION_ORIGINS + SOVEREIGN_CLOUD_ORIGINS + ALLOWED_ORIGIN_PPE
        } else {
            PRODUCTION_ORIGINS + SOVEREIGN_CLOUD_ORIGINS
        }
    }
}
