//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.util

import com.microsoft.identity.common.java.authorities.AzureActiveDirectoryAudience
import com.microsoft.identity.common.java.logging.Logger
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory

/**
 * Utility object for tenant-related operations.
 *
 * Provides methods to extract tenant information from various identifier formats
 * such as email addresses, UPNs (User Principal Names), and tenant GUIDs.
 */
object TenantUtil {
    private const val TAG: String = "TenantUtil"
    private val EMAIL_REGEX = Regex("""^.*@.*$""")
    private val UUID_REGEX = Regex("""^[0-9A-Fa-f\-]{36}$""")


    /**
     * Extracts tenant information from an identifier.
     *
     * This method can handle two types of identifiers:
     * - Email addresses/UPNs: Returns the domain part after the "@" symbol
     * - Tenant GUIDs: Returns the GUID as-is
     *
     * @param identifier The identifier string which could be:
     *                   - An email address or UPN (e.g., "user@contoso.com")
     *                   - A GUID representing a tenant ID (e.g., "12345678-1234-1234-1234-123456789012")
     *                   - Can be null or blank
     * @return The extracted tenant (hostname for UPNs or tenant ID for GUIDs),
     *         or null if the identifier is invalid, null, or blank
     */
    fun getTenantFromIdentifier(identifier: String?): String? {
        val methodTag = "$TAG:getTenantFromIdentifier"
        if (identifier.isNullOrBlank()) {
            return null
        }

        if (UUID_REGEX.matches(identifier)) {
            return identifier
        }

        if (EMAIL_REGEX.matches(identifier)) {
            identifier.substringAfter("@").trim()
        }

        Logger.warn(methodTag, "Identifier is neither a valid email/UPN nor a GUID.")
        return null
    }


    /**
     * Extracts tenant ID from a login hint by resolving the tenant information.
     *
     * This method first extracts the tenant name from the login hint, then attempts to
     * resolve it to a tenant ID by loading the OpenID provider configuration metadata
     * for the specified tenant.
     *
     * @param loginHint The login hint string which could be:
     *                  - An email address or UPN (e.g., "user@contoso.com")
     *                  - A GUID representing a tenant ID
     *                  - Can be null or blank
     * @param correlationId Correlation ID for the request, used for logging and debugging purposes.
     *                      Can be null.
     * @return The resolved tenant ID if successful, null if the login hint is invalid,
     *         the tenant cannot be resolved, or if an error occurs during resolution
     */
    fun getTenantIdFromLoginHint(loginHint: String?, correlationId: String?): String? {
        val methodTag = "$TAG:getTenantIdFromLoginHint"
        val tenantName = getTenantFromIdentifier(loginHint) ?: run {
            Logger.warn(methodTag, correlationId, "Login hint is invalid or empty.")
            return null
        }
        try {
            val configuration =
                AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(tenantName)
            val tenantId =
                AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(configuration)
            Logger.info(methodTag, correlationId, "Successfully got tenant ID from login hint.")
            return tenantId
        } catch (e: Exception) {
            Logger.error(methodTag, correlationId, "Failed to get tenant ID from login hint.", e)
            return null
        }
    }
}
