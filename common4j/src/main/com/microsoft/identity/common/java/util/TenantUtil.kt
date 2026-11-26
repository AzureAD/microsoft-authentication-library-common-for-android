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
    private const val MAX_DNS_HOSTNAME_LENGTH: Int = 253
    private val UUID_REGEX = Regex(
        """^[0-9A-Fa-f]{8}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{4}-[0-9A-Fa-f]{12}$"""
    )
    private val DOMAIN_REGEX = Regex(
        """^[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?(?:\.[A-Za-z0-9](?:[A-Za-z0-9-]{0,61}[A-Za-z0-9])?)+$"""
    )

    /**
     * Extracts tenant information from an identifier.
     *
     * Leading and trailing whitespace is trimmed prior to classification:
     * - Email addresses/UPNs: Returns the multi-label DNS hostname after the single "@" symbol
     * - Tenant GUIDs: Returns the canonical 8-4-4-4-12 GUID with original hex casing preserved
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
        val trimmedIdentifier = identifier?.trim()
        if (trimmedIdentifier.isNullOrEmpty()) {
            return null
        }

        if (UUID_REGEX.matches(trimmedIdentifier)) {
            return trimmedIdentifier
        }

        val domain = extractDomainFromEmailOrUpn(trimmedIdentifier)
        if (domain != null) {
            return domain
        }

        Logger.warn(methodTag, "Identifier is neither a valid email/UPN nor a GUID.")
        return null
    }

    /**
     * Resolves a tenant identifier to a tenant ID (GUID).
     *
     * Leading and trailing whitespace is trimmed prior to classification.
     * If the trimmed input is already a valid canonical GUID, it is returned as-is.
     * Blank inputs and non-GUID inputs containing internal whitespace or URI delimiters
     * ('/', '\', '?', '#') return null immediately without making OpenID metadata requests.
     * Otherwise, the method attempts to resolve the tenant ID by loading the OpenID provider
     * configuration metadata for the trimmed tenant identifier.
     *
     * @param tenant The tenant identifier, which can be either:
     *               - A tenant ID (GUID) (e.g., "12345678-1234-1234-1234-123456789012")
     *               - A hostname/domain (e.g., "contoso.com")
     * @return The tenant ID (GUID) if successful, null if resolution fails
     */
    fun resolveTenantId(tenant: String): String? {
        val methodTag = "$TAG:resolveTenantId"
        val trimmedTenant = tenant.trim()
        if (trimmedTenant.isEmpty()) {
            Logger.warn(methodTag, "Tenant identifier is blank.")
            return null
        }

        // If already a GUID, return as-is
        if (UUID_REGEX.matches(trimmedTenant)) {
            Logger.info(methodTag, "Tenant is already a GUID, returning as-is.")
            return trimmedTenant
        }

        if (containsWhitespaceOrUriDelimiters(trimmedTenant)) {
            Logger.warn(
                methodTag,
                "Tenant identifier contains invalid whitespace or URI delimiter characters."
            )
            return null
        }

        // Otherwise, resolve via OpenID configuration
        try {
            val configuration =
                AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(trimmedTenant)
            val tenantId =
                AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(configuration)
            Logger.info(methodTag, "Successfully got tenant ID from identifier.")
            return tenantId
        } catch (e: Exception) {
            Logger.error(methodTag, "Failed to get tenant ID from identifier.", e)
            return null
        }
    }

    /**
     * Extracts tenant ID from a login hint by resolving the tenant information.
     *
     * This method first extracts the tenant name from the login hint, then attempts to
     * resolve it to a tenant ID by loading the OpenID provider configuration metadata
     * for the specified tenant.
     *
     * @param loginHint The login hint string (e.g., "user@contoso.com")
     * @param correlationId Correlation ID for the request
     * @return The resolved tenant ID if successful, null if the login hint is invalid,
     *         the tenant cannot be resolved, or if an error occurs during resolution
     */
    fun getTenantIdFromLoginHint(loginHint: String?, correlationId: String?): String? {
        val methodTag = "$TAG:getTenantIdFromLoginHint"
        val tenantName = getTenantFromIdentifier(loginHint) ?: run {
            Logger.warn(methodTag, correlationId, "Login hint is invalid or empty.")
            return null
        }

        return resolveTenantId(tenantName)
    }

    private fun extractDomainFromEmailOrUpn(trimmedIdentifier: String): String? {
        val atIndex = trimmedIdentifier.indexOf('@')
        if (atIndex <= 0 || atIndex != trimmedIdentifier.lastIndexOf('@')) {
            return null
        }

        val localPart = trimmedIdentifier.substring(0, atIndex)
        if (localPart.any { it.isWhitespace() }) {
            return null
        }

        val domainPart = trimmedIdentifier.substring(atIndex + 1)
        if (domainPart.length > MAX_DNS_HOSTNAME_LENGTH || !DOMAIN_REGEX.matches(domainPart)) {
            return null
        }

        return domainPart
    }

    private fun containsWhitespaceOrUriDelimiters(value: String): Boolean {
        return value.any { ch ->
            ch.isWhitespace() || ch == '/' || ch == '\\' || ch == '?' || ch == '#'
        }
    }
}
