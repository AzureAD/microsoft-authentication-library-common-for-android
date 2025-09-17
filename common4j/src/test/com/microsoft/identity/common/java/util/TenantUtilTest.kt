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
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.java.providers.oauth2.OpenIdProviderConfiguration
import io.mockk.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for [TenantUtil]
 */
class TenantUtilTest {

    @Before
    fun setUp() {
        // Clear all mocks before each test
        clearAllMocks()
    }

    @After
    fun tearDown() {
        // Clear all mocks after each test
        clearAllMocks()
    }

    // ===== Tests for getTenantFromIdentifier =====

    @Test
    fun `getTenantFromIdentifier returns null for null identifier`() {
        val result = TenantUtil.getTenantFromIdentifier(null)
        assertNull(result)
    }

    @Test
    fun `getTenantFromIdentifier returns null for blank identifier`() {
        val result = TenantUtil.getTenantFromIdentifier("")
        assertNull(result)
    }

    @Test
    fun `getTenantFromIdentifier returns null for whitespace only identifier`() {
        val result = TenantUtil.getTenantFromIdentifier("   ")
        assertNull(result)
    }

    @Test
    fun `getTenantFromIdentifier returns tenant ID for valid GUID`() {
        val tenantId = "12345678-1234-1234-1234-123456789012"
        val result = TenantUtil.getTenantFromIdentifier(tenantId)
        assertEquals(tenantId, result)
    }

    @Test
    fun `getTenantFromIdentifier returns tenant ID for valid GUID with uppercase letters`() {
        val tenantId = "12345678-ABCD-EFAB-CDEF-123456789012"
        val result = TenantUtil.getTenantFromIdentifier(tenantId)
        assertEquals(tenantId, result)
    }

    @Test
    fun `getTenantFromIdentifier returns tenant ID for valid GUID with mixed case`() {
        val tenantId = "12345678-AbCd-EfAb-CdEf-123456789012"
        val result = TenantUtil.getTenantFromIdentifier(tenantId)
        assertEquals(tenantId, result)
    }

    @Test
    fun `getTenantFromIdentifier returns domain for valid email address`() {
        val email = "user@contoso.com"
        val expectedDomain = "contoso.com"
        val result = TenantUtil.getTenantFromIdentifier(email)
        assertEquals(expectedDomain, result)
    }

    @Test
    fun `getTenantFromIdentifier returns domain for valid UPN with subdomain`() {
        val upn = "john.doe@sub.contoso.com"
        val expectedDomain = "sub.contoso.com"
        val result = TenantUtil.getTenantFromIdentifier(upn)
        assertEquals(expectedDomain, result)
    }

    @Test
    fun `getTenantFromIdentifier trims whitespace from extracted domain`() {
        val upn = "user@contoso.com   "
        val expectedDomain = "contoso.com"
        val result = TenantUtil.getTenantFromIdentifier(upn)
        assertEquals(expectedDomain, result)
    }

    @Test
    fun `getTenantFromIdentifier returns domain for email with multiple dots`() {
        val email = "user.name@mail.contoso.com"
        val expectedDomain = "mail.contoso.com"
        val result = TenantUtil.getTenantFromIdentifier(email)
        assertEquals(expectedDomain, result)
    }

    @Test
    fun `getTenantFromIdentifier returns null for invalid GUID format`() {
        val invalidGuid = "12345678-1234-1234-1234-12345678901"  // Too short
        val result = TenantUtil.getTenantFromIdentifier(invalidGuid)
        assertNull(result)
    }

    @Test
    fun `getTenantFromIdentifier returns null for GUID with invalid characters`() {
        val invalidGuid = "12345678-1234-1234-1234-12345678901G"  // Contains 'G'
        val result = TenantUtil.getTenantFromIdentifier(invalidGuid)
        assertNull(result)
    }

    @Test
    fun `getTenantFromIdentifier returns null for invalid email format missing domain`() {
        val invalidEmail = "user@"
        val result = TenantUtil.getTenantFromIdentifier(invalidEmail)
        assertNull(result)
    }

    @Test
    fun `getTenantFromIdentifier returns null for invalid email format missing at symbol`() {
        val invalidEmail = "usercontoso.com"
        val result = TenantUtil.getTenantFromIdentifier(invalidEmail)
        assertNull(result)
    }

    @Test
    fun `getTenantFromIdentifier returns null for invalid email format missing TLD`() {
        val invalidEmail = "user@contoso"
        val result = TenantUtil.getTenantFromIdentifier(invalidEmail)
        assertNull(result)
    }

    @Test
    fun `getTenantFromIdentifier returns null for malformed identifier`() {
        val malformedIdentifier = "not-an-email-or-guid"
        val result = TenantUtil.getTenantFromIdentifier(malformedIdentifier)
        assertNull(result)
    }

    // ===== Tests for getTenantIdFromLoginHint =====

    @Test
    fun `getTenantIdFromLoginHint returns null for null login hint`() {
        val result = TenantUtil.getTenantIdFromLoginHint(null, "correlation-id")
        assertNull(result)
    }

    @Test
    fun `getTenantIdFromLoginHint returns null for blank login hint`() {
        val result = TenantUtil.getTenantIdFromLoginHint("", "correlation-id")
        assertNull(result)
    }

    @Test
    fun `getTenantIdFromLoginHint returns null for invalid login hint`() {
        val result = TenantUtil.getTenantIdFromLoginHint("invalid-hint", "correlation-id")
        assertNull(result)
    }

    @Test
    fun `getTenantIdFromLoginHint successfully resolves tenant ID from email`() {
        val loginHint = "user@contoso.com"
        val correlationId = "correlation-id"
        val expectedTenantId = "12345678-1234-1234-1234-123456789012"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com") } returns mockConfiguration
        every { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) } returns expectedTenantId

        val result = TenantUtil.getTenantIdFromLoginHint(loginHint, correlationId)

        assertEquals(expectedTenantId, result)
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com") }
        verify { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) }
    }

    @Test
    fun `getTenantIdFromLoginHint returns null when configuration loading fails`() {
        val loginHint = "user@contoso.com"
        val correlationId = "correlation-id"
        val exception = RuntimeException("Failed to load configuration")

        mockkStatic(AzureActiveDirectory::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com") } throws exception

        val result = TenantUtil.getTenantIdFromLoginHint(loginHint, correlationId)

        assertNull(result)
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com") }
    }

    @Test
    fun `getTenantIdFromLoginHint returns null when tenant ID extraction fails`() {
        val loginHint = "user@contoso.com"
        val correlationId = "correlation-id"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()
        val exception = RuntimeException("Failed to extract tenant ID")

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com") } returns mockConfiguration
        every { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) } throws exception

        val result = TenantUtil.getTenantIdFromLoginHint(loginHint, correlationId)

        assertNull(result)
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com") }
        verify { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) }
    }

    @Test
    fun `getTenantIdFromLoginHint works with null correlation ID`() {
        val loginHint = "user@contoso.com"
        val expectedTenantId = "12345678-1234-1234-1234-123456789012"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com") } returns mockConfiguration
        every { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) } returns expectedTenantId

        val result = TenantUtil.getTenantIdFromLoginHint(loginHint, null)

        assertEquals(expectedTenantId, result)
    }

    @Test
    fun `getTenantIdFromLoginHint handles complex email domains correctly`() {
        val loginHint = "user.name@sub.domain.contoso.com"
        val correlationId = "correlation-id"
        val expectedTenantId = "12345678-1234-1234-1234-123456789012"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("sub.domain.contoso.com") } returns mockConfiguration
        every { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) } returns expectedTenantId

        val result = TenantUtil.getTenantIdFromLoginHint(loginHint, correlationId)

        assertEquals(expectedTenantId, result)
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("sub.domain.contoso.com") }
    }
}
