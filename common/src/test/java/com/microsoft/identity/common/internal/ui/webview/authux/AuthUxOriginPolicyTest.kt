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

package com.microsoft.identity.common.internal.ui.webview.authux

import android.net.Uri
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AuthUxOriginPolicyTest {
    private val policy = AuthUxOriginPolicy()

    @Test
    fun `http and https default-port trusted subdomains are supported by registration rules`() {
        val expectedRules = setOf(
            "https://*.microsoftonline.com",
            "http://*.microsoftonline.com",
            "https://*.microsoftonline.us",
            "http://*.microsoftonline.us",
            "https://*.microsoftonline.cn",
            "http://*.microsoftonline.cn",
            "https://*.microsoft.com",
            "http://*.microsoft.com"
        )
        assertEquals(expectedRules, policy.getAllowedOriginRules())

        listOf(
            "https://login.microsoftonline.com/path",
            "https://login.microsoftonline.com:443/path",
            "HTTPS://login.microsoftonline.com/path",
            "http://login.microsoftonline.us/path",
            "http://login.microsoftonline.us:80/path",
            "https://login.microsoftonline.cn/path",
            "https://device.login.microsoft.com/path"
        ).forEach { url ->
            assertTrue(policy.isInitialUrlSupportedByRegistrationRules(url))
        }
    }

    @Test
    fun `unsupported initial origins require JavaScript interface transport`() {
        assertFalse(policy.isInitialUrlSupportedByRegistrationRules("custom://login.microsoftonline.com/path"))
        assertFalse(policy.isInitialUrlSupportedByRegistrationRules("https://login.microsoftonline.com:8443/path"))
        assertFalse(policy.isInitialUrlSupportedByRegistrationRules("http://login.microsoftonline.com:8080/path"))
        assertFalse(policy.isInitialUrlSupportedByRegistrationRules("https://microsoftonline.com/path"))
        assertFalse(
            policy.isInitialUrlSupportedByRegistrationRules(
                "https://login.microsoftonline.com.evil.example"
            )
        )
        assertFalse(policy.isInitialUrlSupportedByRegistrationRules("about:blank"))
        assertFalse(policy.isInitialUrlSupportedByRegistrationRules(null))
        assertFalse(policy.isInitialUrlSupportedByRegistrationRules(""))
    }

    @Test
    fun `initial URL validation retains full Java URI syntax checks`() {
        listOf(
            "https://login.microsoftonline.com/oauth2/authorize?claims={\"access_token\":{}}",
            "https://login.microsoftonline.com/oauth2/authorize#claims={\"access_token\":{}}",
            "https://login.microsoftonline.com/%"
        ).forEach { url ->
            assertFalse(url, policy.isInitialUrlSupportedByRegistrationRules(url))
        }
    }

    @Test
    fun `encoded claims are supported by registration policy`() {
        assertTrue(
            policy.isInitialUrlSupportedByRegistrationRules(
                "https://login.microsoftonline.com/oauth2/authorize" +
                    "?claims=%7B%22access_token%22%3A%7B%7D%7D"
            )
        )
    }

    @Test
    fun `initial and callback validation retain case-sensitive matching for every suffix`() {
        listOf(
            "microsoftonline.com" to "MicrosoftOnline.com",
            "microsoft.com" to "Microsoft.com",
            "microsoftonline.us" to "MicrosoftOnline.us",
            "microsoftonline.cn" to "MicrosoftOnline.cn"
        ).forEach { (suffix, mixedCaseSuffix) ->
            listOf(
                "https://LOGIN.${suffix.uppercase()}",
                "https://login.$mixedCaseSuffix"
            ).forEach { origin ->
                assertFalse(
                    origin,
                    policy.isInitialUrlSupportedByRegistrationRules("$origin/path")
                )
                assertFalse(origin, policy.isAllowedCallback(Uri.parse(origin), isMainFrame = true))
            }
            listOf("login", "LOGIN").forEach { subdomain ->
                val origin = "https://$subdomain.$suffix"
                assertTrue(
                    origin,
                    policy.isInitialUrlSupportedByRegistrationRules("$origin/path")
                )
                assertTrue(origin, policy.isAllowedCallback(Uri.parse(origin), isMainFrame = true))
            }
        }
    }

    @Test
    fun `initial and callback validation reject malformed authorities`() {
        listOf(
            "https://.microsoftonline.com",
            "https://login.microsoftonline.com:invalid",
            "https://evil.example\\@login.microsoftonline.com"
        ).forEach { origin ->
            assertFalse(
                origin,
                policy.isInitialUrlSupportedByRegistrationRules("$origin/path")
            )
            assertFalse(origin, policy.isAllowedCallback(Uri.parse(origin), isMainFrame = true))
        }
    }

    @Test
    fun `callback requires registration-rule origin and main frame`() {
        listOf(
            "https://login.microsoftonline.com",
            "https://login.microsoftonline.com:443",
            "http://login.microsoftonline.com",
            "http://login.microsoftonline.com:80"
        ).forEach { origin ->
            assertTrue(
                origin,
                policy.isAllowedCallback(Uri.parse(origin), isMainFrame = true)
            )
        }
        assertFalse(
            policy.isAllowedCallback(
                Uri.parse("https://login.microsoftonline.com"),
                isMainFrame = false
            )
        )
        assertFalse(
            policy.isAllowedCallback(
                Uri.parse("https://login.microsoftonline.com.evil.example"),
                isMainFrame = true
            )
        )
        assertFalse(
            policy.isAllowedCallback(
                Uri.parse("https://login.microsoftonline.com:8443"),
                isMainFrame = true
            )
        )
        assertFalse(
            policy.isAllowedCallback(
                Uri.parse("custom://login.microsoftonline.com"),
                isMainFrame = true
            )
        )
    }
}
