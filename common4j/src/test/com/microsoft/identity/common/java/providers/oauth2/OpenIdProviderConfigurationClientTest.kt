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
package com.microsoft.identity.common.java.providers.oauth2

import com.google.gson.Gson
import io.opentelemetry.api.common.AttributeKey
import org.junit.Assert
import org.junit.Test

/**
 * Tests for {@link OpenIdProviderConfigurationClient}.
 */
class OpenIdProviderConfigurationClientTest {

    private val gson = Gson()

    private fun configWithIssuer(issuer: String?): OpenIdProviderConfiguration {
        val sb = StringBuilder("{")
        if (issuer != null) {
            sb.append("\"issuer\":\"").append(issuer).append("\"")
        }
        sb.append("}")
        return gson.fromJson(sb.toString(), OpenIdProviderConfiguration::class.java)
    }

    @Test
    fun validateIssuer_success_exactMatch() {
        val requestAuthority = "https://login.example.com/tenant/v2.0"
        val config = configWithIssuer(requestAuthority)
        val client = OpenIdProviderConfigurationClient()
        val result = client.validateIssuer(config, requestAuthority)
        Assert.assertNull("Expected null attributes on successful validation", result)
    }

    @Test
    fun validateIssuer_missingIssuer() {
        val requestAuthority = "https://login.example.com/tenant/v2.0"
        val config = configWithIssuer(null)
        val client = OpenIdProviderConfigurationClient()
        val result = client.validateIssuer(config, requestAuthority)
        Assert.assertNotNull(result)
        val key = AttributeKey.stringKey("openid_issuer_invalid_reason")
        Assert.assertEquals("issuer_missing", result!!.get(key))
    }

    @Test
    fun validateIssuer_malformedIssuer() {
        val requestAuthority = "https://login.example.com/tenant/v2.0"
        val config = configWithIssuer("not-a-url")
        val client = OpenIdProviderConfigurationClient()
        val result = client.validateIssuer(config, requestAuthority)
        Assert.assertNotNull(result)
        val key = AttributeKey.stringKey("openid_issuer_invalid_reason")
        Assert.assertEquals("issuer_malformed", result!!.get(key))
    }

    @Test
    fun validateIssuer_skippedValidation_forUnknownAuthority() {
        val requestAuthority = "https://login.example.com/tenant/v2.0"
        val config = configWithIssuer("https://other.example.com/tenant/v2.0")
        val client = OpenIdProviderConfigurationClient()
        val result = client.validateIssuer(config, requestAuthority)
        Assert.assertNotNull(result)
        val key = AttributeKey.stringKey("openid_issuer_invalid_reason")
        Assert.assertEquals("issuer_validation_skipped", result!!.get(key))
    }
}
