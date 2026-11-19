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

    // ===== Tests for resolveTenantId =====

    @Test
    fun `resolveTenantId returns GUID as-is for valid GUID`() {
        val tenantId = "12345678-1234-1234-1234-123456789012"
        val result = TenantUtil.resolveTenantId(tenantId)
        assertEquals(tenantId, result)
    }

    @Test
    fun `resolveTenantId returns GUID as-is for valid GUID with uppercase letters`() {
        val tenantId = "12345678-ABCD-EFAB-CDEF-123456789012"
        val result = TenantUtil.resolveTenantId(tenantId)
        assertEquals(tenantId, result)
    }

    @Test
    fun `resolveTenantId returns GUID as-is for valid GUID with mixed case`() {
        val tenantId = "12345678-AbCd-EfAb-CdEf-123456789012"
        val result = TenantUtil.resolveTenantId(tenantId)
        assertEquals(tenantId, result)
    }

    @Test
    fun `resolveTenantId does not call OpenID configuration for GUID input`() {
        val tenantId = "12345678-1234-1234-1234-123456789012"

        mockkStatic(AzureActiveDirectory::class)

        val result = TenantUtil.resolveTenantId(tenantId)

        assertEquals(tenantId, result)
        verify(exactly = 0) { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(any()) }
    }

    @Test
    fun `resolveTenantId resolves tenant ID from domain via OpenID configuration`() {
        val domain = "contoso.com"
        val expectedTenantId = "12345678-1234-1234-1234-123456789012"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(domain) } returns mockConfiguration
        every { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) } returns expectedTenantId

        val result = TenantUtil.resolveTenantId(domain)

        assertEquals(expectedTenantId, result)
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(domain) }
        verify { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) }
    }

    @Test
    fun `resolveTenantId resolves tenant ID from subdomain via OpenID configuration`() {
        val domain = "sub.contoso.com"
        val expectedTenantId = "12345678-1234-1234-1234-123456789012"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(domain) } returns mockConfiguration
        every { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) } returns expectedTenantId

        val result = TenantUtil.resolveTenantId(domain)

        assertEquals(expectedTenantId, result)
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(domain) }
    }

    @Test
    fun `resolveTenantId returns null when configuration loading fails`() {
        val domain = "contoso.com"
        val exception = RuntimeException("Failed to load configuration")

        mockkStatic(AzureActiveDirectory::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(domain) } throws exception

        val result = TenantUtil.resolveTenantId(domain)

        assertNull(result)
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(domain) }
    }

    @Test
    fun `resolveTenantId returns null when tenant ID extraction fails`() {
        val domain = "contoso.com"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()
        val exception = RuntimeException("Failed to extract tenant ID")

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(domain) } returns mockConfiguration
        every { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) } throws exception

        val result = TenantUtil.resolveTenantId(domain)

        assertNull(result)
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(domain) }
        verify { AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration) }
    }

    @Test
    fun `resolveTenantId returns null for invalid GUID that fails resolution`() {
        val invalidGuid = "12345678-1234-1234-1234-12345678901"  // Too short - not a valid GUID
        val exception = RuntimeException("Failed to load configuration")

        mockkStatic(AzureActiveDirectory::class)

        every { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(invalidGuid) } throws exception

        val result = TenantUtil.resolveTenantId(invalidGuid)

        assertNull(result)
        // Since it's not a valid GUID, it should try to resolve via OpenID configuration
        verify { AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(invalidGuid) }
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

    // ===== Hardened R1-R4 parsing and compatibility tests =====

    @Test
    fun `getTenantFromIdentifier returns null for 36-char non-UUID strings`() {
        val nonUuid36CharInputs = listOf(
            "------------------------------------",
            "1234567890abcdef1234567890abcdef1234",
            "1234567-81234-1234-1234-123456789012",
            "12345678-1234-1234-1234-1234-5678901"
        )
        for (input in nonUuid36CharInputs) {
            assertEquals(36, input.length)
            assertNull(
                "Expected getTenantFromIdentifier to reject 36-char non-UUID: $input",
                TenantUtil.getTenantFromIdentifier(input)
            )
        }

        // Shortened non-canonical UUID segments accepted by UUID.fromString must also be rejected
        assertNull(TenantUtil.getTenantFromIdentifier("0-0-0-0-0"))
        assertNull(TenantUtil.getTenantFromIdentifier("1-2-3-4-5"))

        // Canonical all-zero and uppercase all-F GUIDs preserve original hex casing
        val zeroGuid = "00000000-0000-0000-0000-000000000000"
        val upperMaxGuid = "FFFFFFFF-FFFF-FFFF-FFFF-FFFFFFFFFFFF"
        assertEquals(zeroGuid, TenantUtil.getTenantFromIdentifier(zeroGuid))
        assertEquals(upperMaxGuid, TenantUtil.getTenantFromIdentifier(upperMaxGuid))
    }

    @Test
    fun `resolveTenantId does not treat 36-char non-UUID strings as GUIDs`() {
        val nonUuid36CharInputs = listOf(
            "------------------------------------",
            "1234567890abcdef1234567890abcdef1234",
            "1234567-81234-1234-1234-123456789012",
            "12345678-1234-1234-1234-1234-5678901"
        )
        val exception = RuntimeException("Failed to load configuration")

        mockkStatic(AzureActiveDirectory::class)

        for (input in nonUuid36CharInputs) {
            every {
                AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(input)
            } throws exception

            val result = TenantUtil.resolveTenantId(input)

            assertNull("Expected null when 36-char non-UUID fails OpenID lookup: $input", result)
            verify(exactly = 1) {
                AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(input)
            }
        }
    }

    @Test
    fun `getTenantFromIdentifier trims leading and trailing whitespace for GUID and UPN`() {
        val paddedMixedCaseGuid = "  12345678-AbCd-EfAb-CdEf-123456789012\t\n"
        assertEquals(
            "12345678-AbCd-EfAb-CdEf-123456789012",
            TenantUtil.getTenantFromIdentifier(paddedMixedCaseGuid)
        )

        val paddedUpn = "\t user@contoso.com \n"
        assertEquals("contoso.com", TenantUtil.getTenantFromIdentifier(paddedUpn))

        assertNull(TenantUtil.getTenantFromIdentifier("\t\n\r "))
    }

    @Test
    fun `resolveTenantId trims outer whitespace for GUID without calling OpenID configuration`() {
        val expectedGuid = "12345678-AbCd-EfAb-CdEf-123456789012"

        mockkStatic(AzureActiveDirectory::class)

        assertEquals(
            expectedGuid,
            TenantUtil.resolveTenantId("  12345678-AbCd-EfAb-CdEf-123456789012  ")
        )
        assertEquals(
            expectedGuid,
            TenantUtil.resolveTenantId("\t12345678-AbCd-EfAb-CdEf-123456789012\r\n")
        )
        verify(exactly = 0) {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(any())
        }
    }

    @Test
    fun `resolveTenantId trims outer whitespace before calling OpenID configuration for domain`() {
        val paddedDomain = "  contoso.com\t\n"
        val expectedTenantId = "12345678-1234-1234-1234-123456789012"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com")
        } returns mockConfiguration
        every {
            AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration)
        } returns expectedTenantId

        val result = TenantUtil.resolveTenantId(paddedDomain)

        assertEquals(expectedTenantId, result)
        verify(exactly = 1) {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com")
        }
    }

    @Test
    fun `resolveTenantId returns null without calling OpenID configuration for blank internal whitespace or URI delimiters`() {
        val invalidTenantInputs = listOf(
            "",
            "   ",
            "\t\n",
            "contoso .com",
            "con\ttoso.com",
            "con\ntoso.com",
            "con\rtoso.com",
            "12345678-1234-1234-1234- 123456789012",
            "contoso.com/v2.0",
            "contoso.com\\path",
            "contoso.com?a=b",
            "contoso.com#frag",
            " /contoso.com "
        )

        mockkStatic(AzureActiveDirectory::class)

        for (input in invalidTenantInputs) {
            assertNull(
                "Expected resolveTenantId to short-circuit to null for: $input",
                TenantUtil.resolveTenantId(input)
            )
        }
        verify(exactly = 0) {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(any())
        }
    }

    @Test
    fun `getTenantFromIdentifier extracts domain for Entra B2B EXT and valid multi-label UPNs`() {
        assertEquals(
            "contoso.onmicrosoft.com",
            TenantUtil.getTenantFromIdentifier("alice_fabrikam.com#EXT#@contoso.onmicrosoft.com")
        )
        assertEquals(
            "sub.contoso-dev.com",
            TenantUtil.getTenantFromIdentifier("  guest_user#EXT#@sub.contoso-dev.com  ")
        )
        assertEquals(
            "xn--bcher-kva.ch",
            TenantUtil.getTenantFromIdentifier("user+tag@xn--bcher-kva.ch")
        )
        assertEquals(
            "Sub.Contoso.COM",
            TenantUtil.getTenantFromIdentifier("user@Sub.Contoso.COM")
        )
        assertEquals(
            "a.b",
            TenantUtil.getTenantFromIdentifier("user@a.b")
        )

        val label63 = "a".repeat(63)
        assertEquals(
            "$label63.com",
            TenantUtil.getTenantFromIdentifier("user@$label63.com")
        )

        val label57 = "d".repeat(57)
        val maxHostname = "$label63.$label63.$label63.$label57.com"
        assertEquals(253, maxHostname.length)
        assertEquals(
            maxHostname,
            TenantUtil.getTenantFromIdentifier("user@$maxHostname")
        )
    }

    @Test
    fun `getTenantIdFromLoginHint resolves Entra B2B EXT and padded UPNs via OpenID configuration`() {
        val expectedTenantId = "12345678-1234-1234-1234-123456789012"
        val mockConfiguration = mockk<OpenIdProviderConfiguration>()

        mockkStatic(AzureActiveDirectory::class)
        mockkStatic(AzureActiveDirectoryAudience::class)

        every {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.onmicrosoft.com")
        } returns mockConfiguration
        every {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("sub.contoso-dev.com")
        } returns mockConfiguration
        every {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com")
        } returns mockConfiguration
        every {
            AzureActiveDirectoryAudience.getTenantIdFromOpenIdProviderConfiguration(mockConfiguration)
        } returns expectedTenantId

        assertEquals(
            expectedTenantId,
            TenantUtil.getTenantIdFromLoginHint(
                "alice_fabrikam.com#EXT#@contoso.onmicrosoft.com",
                "corr-ext-1"
            )
        )
        assertEquals(
            expectedTenantId,
            TenantUtil.getTenantIdFromLoginHint(
                "  guest_user#EXT#@sub.contoso-dev.com  ",
                "corr-ext-2"
            )
        )
        assertEquals(
            expectedTenantId,
            TenantUtil.getTenantIdFromLoginHint("\t user@contoso.com \n", "corr-padded")
        )

        verify(exactly = 1) {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.onmicrosoft.com")
        }
        verify(exactly = 1) {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("sub.contoso-dev.com")
        }
        verify(exactly = 1) {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant("contoso.com")
        }
    }

    @Test
    fun `getTenantIdFromLoginHint resolves padded GUID login hint without calling OpenID configuration`() {
        val paddedGuidHint = "  12345678-AbCd-EfAb-CdEf-123456789012\t\n"

        mockkStatic(AzureActiveDirectory::class)

        val result = TenantUtil.getTenantIdFromLoginHint(paddedGuidHint, "corr-guid")

        assertEquals("12345678-AbCd-EfAb-CdEf-123456789012", result)
        verify(exactly = 0) {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(any())
        }
    }

    @Test
    fun `getTenantFromIdentifier and getTenantIdFromLoginHint reject malformed UPNs and 36-char non-UUIDs without OpenID calls`() {
        val label64 = "a".repeat(64)
        val label63 = "a".repeat(63)
        val label58 = "d".repeat(58)
        val hostname254 = "$label63.$label63.$label63.$label58.com"
        assertEquals(254, hostname254.length)

        val malformedIdentifiers = listOf(
            "@contoso.com",
            "a@b@contoso.com",
            "user@@contoso.com",
            "user name@contoso.com",
            "user\tname@contoso.com",
            "user\nname@contoso.com",
            "user@.contoso.com",
            "user@contoso.com.",
            "user@contoso..com",
            "user@...",
            "user@-contoso.com",
            "user@contoso-.com",
            "user@contoso.-com",
            "user@contoso.com-",
            "user@con toso.com",
            "user@contoso.com/path",
            "user@contoso.com\\path",
            "user@contoso.com?q=1",
            "user@contoso.com#frag",
            "user@contoso.com:8443",
            "user@$label64.com",
            "user@$hostname254",
            "------------------------------------",
            "1234567890abcdef1234567890abcdef1234",
            "1234567-81234-1234-1234-123456789012",
            "12345678-1234-1234-1234-1234-5678901"
        )

        mockkStatic(AzureActiveDirectory::class)

        for (invalidInput in malformedIdentifiers) {
            assertNull(
                "Expected getTenantFromIdentifier to return null for: $invalidInput",
                TenantUtil.getTenantFromIdentifier(invalidInput)
            )
            assertNull(
                "Expected getTenantIdFromLoginHint to return null for: $invalidInput",
                TenantUtil.getTenantIdFromLoginHint(invalidInput, "corr-invalid")
            )
        }
        verify(exactly = 0) {
            AzureActiveDirectory.loadOpenIdProviderConfigurationMetadataForTenant(any())
        }
    }

    @Test
    fun `TenantUtil preserves Java INSTANCE singleton field and public method descriptors`() {
        val clazz = TenantUtil::class.java
        assertTrue(java.lang.reflect.Modifier.isPublic(clazz.modifiers))
        assertTrue(java.lang.reflect.Modifier.isFinal(clazz.modifiers))

        val instanceField = clazz.getDeclaredField("INSTANCE")
        assertTrue(java.lang.reflect.Modifier.isPublic(instanceField.modifiers))
        assertTrue(java.lang.reflect.Modifier.isStatic(instanceField.modifiers))
        assertTrue(java.lang.reflect.Modifier.isFinal(instanceField.modifiers))
        assertEquals(clazz, instanceField.type)

        val instance = instanceField.get(null)
        assertSame(TenantUtil, instance)

        val getTenantMethod = clazz.getMethod("getTenantFromIdentifier", String::class.java)
        assertEquals(String::class.java, getTenantMethod.returnType)
        assertEquals("contoso.com", getTenantMethod.invoke(instance, "  user@contoso.com  "))

        val resolveTenantMethod = clazz.getMethod("resolveTenantId", String::class.java)
        assertEquals(String::class.java, resolveTenantMethod.returnType)
        assertEquals(
            "12345678-AbCd-EfAb-CdEf-123456789012",
            resolveTenantMethod.invoke(instance, "  12345678-AbCd-EfAb-CdEf-123456789012  ")
        )

        val loginHintMethod = clazz.getMethod(
            "getTenantIdFromLoginHint",
            String::class.java,
            String::class.java
        )
        assertEquals(String::class.java, loginHintMethod.returnType)
        assertEquals(
            "12345678-1234-1234-1234-123456789012",
            loginHintMethod.invoke(instance, "12345678-1234-1234-1234-123456789012", "corr-id")
        )
    }
}
