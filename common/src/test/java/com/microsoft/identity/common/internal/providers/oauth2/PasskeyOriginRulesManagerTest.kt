/*
 * Copyright (c) Microsoft Corporation.
 * All rights reserved.
 *
 * This code is licensed under the MIT License.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files(the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions :
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.microsoft.identity.common.internal.providers.oauth2

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [PasskeyOriginRulesManager].
 * Tests origin validation logic to ensure only allowed origins can access Passkey/WebAuthN APIs.
 */
@RunWith(RobolectricTestRunner::class)
class PasskeyOriginRulesManagerTest {

    // ==================== Production Origins Tests ====================

    @Test
    fun `isAllowedOrigin returns true for production login microsoft com`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoft.com"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoft.com/"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoft.com/common/oauth2/authorize"))
    }

    @Test
    fun `isAllowedOrigin returns true for account live com`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://account.live.com"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://account.live.com/"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://account.live.com/page/webauthn"))
    }

    @Test
    fun `isAllowedOrigin returns true for mysignins microsoft com`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.microsoft.com"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.microsoft.com/"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.microsoft.com/auth/passkey"))
    }

    @Test
    fun `isAllowedOrigin returns true for mysignins azure us`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.azure.us"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.azure.us/"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.azure.us/path"))
    }

    @Test
    fun `isAllowedOrigin returns true for mysignins microsoft scloud`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.microsoft.scloud"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.microsoft.scloud/"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.microsoft.scloud/auth"))
    }

    @Test
    fun `isAllowedOrigin returns true for mysignins eaglex ic gov`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.eaglex.ic.gov"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.eaglex.ic.gov/"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.eaglex.ic.gov/credential"))
    }

    // ==================== Sovereign Cloud Origins Tests (Requires FIDO Path) ====================

    @Test
    fun `isAllowedOrigin returns true for sovereign cloud microsoftonline us with fido in path`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/fido"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/fido/endpoint"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/some/path/fido"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/fido/"))
    }

    @Test
    fun `isAllowedOrigin returns false for sovereign cloud microsoftonline us without fido in path`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/other/endpoint"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/fidoauth"))
    }

    @Test
    fun `isAllowedOrigin returns true for sovereign cloud microsoft scloud with fido in path`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.microsoft.scloud/fido"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.microsoft.scloud/fido/endpoint"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.microsoft.scloud/path/fido/auth"))
    }

    @Test
    fun `isAllowedOrigin returns false for sovereign cloud microsoft scloud without fido in path`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.microsoft.scloud"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.microsoft.scloud/"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.microsoft.scloud/other"))
    }

    @Test
    fun `isAllowedOrigin returns true for sovereign cloud eaglex ic gov with fido in path`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.eaglex.ic.gov/fido"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.eaglex.ic.gov/fido/endpoint"))
    }

    @Test
    fun `isAllowedOrigin returns false for sovereign cloud eaglex ic gov without fido in path`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.eaglex.ic.gov"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.eaglex.ic.gov/"))
    }

    @Test
    fun `isAllowedOrigin returns true for french sovereign cloud with fido in path`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.fr/fido"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.fr/fido/endpoint"))
    }

    @Test
    fun `isAllowedOrigin returns false for french sovereign cloud without fido in path`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.fr"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.fr/"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.fr/other"))
    }

    @Test
    fun `isAllowedOrigin returns true for german sovereign cloud with fido in path`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.de/fido"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.de/fido/endpoint"))
    }

    @Test
    fun `isAllowedOrigin returns false for german sovereign cloud without fido in path`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.de"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.de/"))
    }

    @Test
    fun `isAllowedOrigin returns true for singapore sovereign cloud with fido in path`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.sg/fido"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.sg/fido/endpoint"))
    }

    @Test
    fun `isAllowedOrigin returns false for singapore sovereign cloud without fido in path`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.sg"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.sovcloud-identity.sg/"))
    }

    // ==================== Case Insensitivity Tests ====================

    @Test
    fun `isAllowedOrigin is case insensitive for fido path in sovereign cloud`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/FIDO"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/FiDo/endpoint"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us/path/fIdO"))
    }

    // ==================== Subdomain Spoofing Prevention Tests ====================

    @Test
    fun `isAllowedOrigin returns false for subdomain spoofing on microsoftonline us`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us.someDomain.com/fido"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us.evil.com/fido"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftonline.us.attacker.io/fido"))
    }

    @Test
    fun `isAllowedOrigin returns false for subdomain spoofing on production origins`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoft.com.evil.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://account.live.com.attacker.io"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.microsoft.com.fake.net"))
    }

    @Test
    fun `isAllowedOrigin returns false for subdomain prefix attack`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://evillogin.microsoft.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://fake.account.live.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://fake.mysignins.microsoft.com"))
    }

    // ==================== Scheme Validation Tests ====================

    @Test
    fun `isAllowedOrigin returns false for http scheme instead of https`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("http://login.microsoft.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("http://account.live.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("http://mysignins.microsoft.com"))
    }

    @Test
    fun `isAllowedOrigin returns false for non-standard scheme`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("ftp://login.microsoft.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("file://login.microsoft.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("custom://login.microsoft.com"))
    }

    // ==================== Non-Matching Origins Tests ====================

    @Test
    fun `isAllowedOrigin returns false for completely unrelated hosts`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://evil.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.example.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://account.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://mysignins.com"))
    }

    @Test
    fun `isAllowedOrigin returns false for similar but different hosts`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoftware.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://accounts.live.com"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("https://signin.microsoft.com"))
    }

    // ==================== Edge Cases ====================

    @Test
    fun `isAllowedOrigin returns false for empty url`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin(""))
    }

    @Test
    fun `isAllowedOrigin returns false for malformed url`() {
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("not a url"))
        assertFalse(PasskeyOriginRulesManager.isAllowedOrigin("://login.microsoft.com"))
    }

    @Test
    fun `isAllowedOrigin handles trailing slashes consistently`() {
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoft.com"))
        assertTrue(PasskeyOriginRulesManager.isAllowedOrigin("https://login.microsoft.com/"))
    }

    // ==================== getAllowedOriginRules Tests ====================

    @Test
    fun `getAllowedOriginRules contains all required production origins`() {
        val rules = PasskeyOriginRulesManager.getAllowedOriginRules()
        assertTrue(rules.contains("https://login.microsoft.com"))
        assertTrue(rules.contains("https://account.live.com"))
        assertTrue(rules.contains("https://mysignins.microsoft.com"))
        assertTrue(rules.contains("https://mysignins.azure.us"))
        assertTrue(rules.contains("https://mysignins.microsoft.scloud"))
        assertTrue(rules.contains("https://mysignins.eaglex.ic.gov"))
    }

    @Test
    fun `getAllowedOriginRules contains all required sovereign cloud origins`() {
        val rules = PasskeyOriginRulesManager.getAllowedOriginRules()
        assertTrue(rules.contains("https://login.microsoftonline.us"))
        assertTrue(rules.contains("https://login.microsoftonline.microsoft.scloud"))
        assertTrue(rules.contains("https://login.microsoftonline.eaglex.ic.gov"))
        assertTrue(rules.contains("https://login.sovcloud-identity.fr"))
        assertTrue(rules.contains("https://login.sovcloud-identity.de"))
        assertTrue(rules.contains("https://login.sovcloud-identity.sg"))
    }

    @Test
    fun `getAllowedOriginRules returns non-empty set`() {
        val rules = PasskeyOriginRulesManager.getAllowedOriginRules()
        assertTrue(rules.isNotEmpty())
        assertTrue(rules.size >= 12) // At least production + sovereign cloud origins
    }
}

