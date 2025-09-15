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
import java.util.regex.Matcher
import java.util.regex.Pattern

object TenantUtil {
    private const val TAG: String = "TenantUtil"
    private const val IDENTIFIER_REGEX: String = "(.*@.*|^[0-9A-Fa-f\\-]{36}$)"
    private val PAIR_REGEX: Pattern = Pattern.compile(IDENTIFIER_REGEX)

    /**
     * Extracts tenant from an identifier.
     *
     * @param identifier {@link String} This could be an email address/UPN or a GUID (tenant ID).
     * @return a tenant (hostname or tenant ID).
     */
    fun getTenantFromIdentifier(identifier: String?): String? {
        if (identifier.isNullOrBlank()) {
            return null
        }
        val matcher: Matcher = PAIR_REGEX.matcher(identifier)
        if (!matcher.matches()) {
            return null
        }
        // If identifier is a UPN, extracts a host from it.
        if (identifier.contains("@")) {
            return identifier.substring(identifier.indexOf("@") + 1).trim()
        }
        return identifier
    }

    /**
     * Extracts tenant ID from login hint.
     *
     * @param loginHint {@link String} This could be an email address/UPN or a GUID (tenant ID).
     * @param correlationId Correlation ID for the request to be logged.
     * @return a tenant ID if found, null otherwise.
     */
    fun getTenantIdFromLoginHnt(loginHint: String?, correlationId : String?): String? {
        val methodTag = "$TAG:getTenantIdFromLoginHnt"
        if (loginHint.isNullOrBlank()) {
            Logger.info(methodTag, correlationId, "Login hint is empty")
            return null
        }
        val tenantName  = getTenantFromIdentifier(loginHint)
        if (tenantName.isNullOrBlank()) {
            Logger.warn(methodTag, correlationId, "Tenant name is empty")
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
