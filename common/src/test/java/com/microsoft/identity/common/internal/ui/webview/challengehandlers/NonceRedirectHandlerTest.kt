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
package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.webkit.WebView
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectoryCloud
import io.opentelemetry.api.trace.Span
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import java.net.URL

/**
 * Tests for [NonceRedirectHandler], focused on the CWE-918 fix that prevents the PRT credential
 * header ([AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]) from being forwarded to an
 * untrusted or cleartext redirect target.
 *
 * The trust gate is exercised for real: a genuinely-validated cloud host is seeded via
 * [AzureActiveDirectory.putCloud] (which yields an [AzureActiveDirectoryCloud] with
 * `isValidated == true`), while untrusted hosts are simply never seeded, so
 * [AzureActiveDirectory.isValidCloudHost] genuinely returns `false` for them.
 */
@RunWith(RobolectricTestRunner::class)
class NonceRedirectHandlerTest {

    private lateinit var webView: WebView
    private lateinit var headers: HashMap<String, String>
    private lateinit var span: Span
    private lateinit var handler: NonceRedirectHandler

    private val prtHeaderValue = "original-prt-credential"

    @Before
    fun setUp() {
        webView = mock(WebView::class.java)
        headers = HashMap()
        headers[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER] = prtHeaderValue
        span = mock(Span::class.java)
        handler = NonceRedirectHandler(webView, headers, span)

        // Seed a real, validated cloud host so isValidCloudHost() executes for real rather than
        // being mocked. The public (network, cache, aliases) constructor sets isValidated == true.
        // Untrusted-host tests use hosts that are never seeded.
        AzureActiveDirectory.putCloud(
            TRUSTED_HOST,
            AzureActiveDirectoryCloud(TRUSTED_HOST, TRUSTED_HOST, listOf(TRUSTED_HOST))
        )
    }

    @Test
    fun `https trusted host forwards the PRT credential header`() {
        val url = "https://$TRUSTED_HOST/common/oauth2/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertEquals(
            "PRT credential header must be forwarded to a trusted HTTPS AAD host.",
            prtHeaderValue,
            forwarded[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
    }

    @Test
    fun `cleartext http trusted host strips the PRT credential header`() {
        val url = "http://$TRUSTED_HOST/common/oauth2/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertFalse(
            "PRT credential header must not be forwarded over cleartext http.",
            forwarded.containsKey(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER)
        )
        assertSharedHeadersUnmutated()
    }

    @Test
    fun `https untrusted host strips the PRT credential header`() {
        val url = "https://$UNTRUSTED_HOST/common/oauth2/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertFalse(
            "PRT credential header must not be forwarded to an untrusted host.",
            forwarded.containsKey(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER)
        )
        assertSharedHeadersUnmutated()
    }

    /**
     * The core exploit shape: omitting `login_hint` means the PRT header is never rewritten, so the
     * ORIGINAL, valid PRT would previously be forwarded verbatim. With the fix, an untrusted target
     * receives no credential header at all.
     */
    @Test
    fun `login_hint omitted on untrusted host does not leak the original PRT`() {
        val url = "https://$UNTRUSTED_HOST/authorize?sso_nonce=abc"

        handler.processChallenge(URL(url))

        val forwarded = captureLoadedHeaders(url)
        assertNull(
            "The original PRT must never reach an untrusted host, even without login_hint.",
            forwarded[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
        assertSharedHeadersUnmutated()
    }

    @Test
    fun `isRedirectTrustedForHeaderForwarding contract`() {
        assertTrue(
            NonceRedirectHandler.isRedirectTrustedForHeaderForwarding(
                "https://$TRUSTED_HOST/authorize?sso_nonce=abc"
            )
        )
        assertFalse(
            "cleartext http must not be trusted",
            NonceRedirectHandler.isRedirectTrustedForHeaderForwarding(
                "http://$TRUSTED_HOST/authorize?sso_nonce=abc"
            )
        )
        assertFalse(
            "unseeded/uninitialized host must not be trusted",
            NonceRedirectHandler.isRedirectTrustedForHeaderForwarding(
                "https://$UNTRUSTED_HOST/authorize?sso_nonce=abc"
            )
        )
        assertFalse(
            "malformed url must not be trusted",
            NonceRedirectHandler.isRedirectTrustedForHeaderForwarding("not a url")
        )
    }

    private fun captureLoadedHeaders(expectedUrl: String): Map<String, String> {
        @Suppress("UNCHECKED_CAST")
        val headersCaptor = ArgumentCaptor.forClass(Map::class.java)
            as ArgumentCaptor<Map<String, String>>
        verify(webView).loadUrl(
            eq(expectedUrl),
            headersCaptor.capture()
        )
        return headersCaptor.value
    }

    private fun assertSharedHeadersUnmutated() {
        assertEquals(
            "The shared request-header map must not be mutated by the untrusted path.",
            prtHeaderValue,
            headers[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
        )
    }

    companion object {
        private const val TRUSTED_HOST = "login.microsoftonline.com"
        private const val UNTRUSTED_HOST = "malicious.contoso.example"
    }
}
