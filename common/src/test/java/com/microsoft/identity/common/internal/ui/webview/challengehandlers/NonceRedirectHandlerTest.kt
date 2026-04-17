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
import com.microsoft.identity.common.java.broker.CommonRefreshTokenCredentialProvider
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.opentelemetry.api.trace.Span
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.robolectric.RobolectricTestRunner
import java.net.URL

@RunWith(RobolectricTestRunner::class)
class NonceRedirectHandlerTest {

    private lateinit var webView: WebView
    private lateinit var headers: HashMap<String, String>
    private lateinit var span: Span

    companion object {
        private const val ESTS_URL_WITH_NONCE_AND_HINT =
            "https://login.microsoftonline.com/common/oauth2/authorize?sso_nonce=testnonce&login_hint=user@example.com"
        private const val ESTS_URL_WITH_NONCE_NO_HINT =
            "https://login.microsoftonline.com/common/oauth2/authorize?sso_nonce=testnonce"
        private const val ESTS_URL_NO_NONCE =
            "https://login.microsoftonline.com/common/oauth2/authorize"
        private const val FRESH_CREDENTIAL = "freshPrtCredential"
    }

    @Before
    fun setUp() {
        webView = mock(WebView::class.java)
        headers = hashMapOf(
            AuthenticationConstants.Broker.PRT_RESPONSE_HEADER to "existingPrtCredential"
        )
        span = mock(Span::class.java)
    }

    @After
    fun tearDown() {
        unmockkAll()
    }

    @Test
    fun `processChallenge attaches PRT credential when login_hint is in URL`() {
        mockkObject(CommonRefreshTokenCredentialProvider)
        every {
            CommonRefreshTokenCredentialProvider.getRefreshTokenCredentialUsingNewNonce(
                ESTS_URL_WITH_NONCE_AND_HINT, "user@example.com", "testnonce"
            )
        } returns FRESH_CREDENTIAL

        val handler = NonceRedirectHandler(webView, headers, span)
        handler.processChallenge(URL(ESTS_URL_WITH_NONCE_AND_HINT))

        verify(span).setAttribute(AttributeName.is_new_refresh_token_cred_header_attached.name, true)
        assertEquals(FRESH_CREDENTIAL, headers[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER])
        verify(webView).loadUrl(ESTS_URL_WITH_NONCE_AND_HINT, headers)
    }

    @Test
    fun `processChallenge skips PRT attachment when login_hint absent`() {
        val handler = NonceRedirectHandler(webView, headers, span)
        handler.processChallenge(URL(ESTS_URL_WITH_NONCE_NO_HINT))

        verify(span, never()).setAttribute(AttributeName.is_new_refresh_token_cred_header_attached.name, true)
        verify(webView).loadUrl(ESTS_URL_WITH_NONCE_NO_HINT, headers)
    }

    @Test
    fun `processChallenge skips PRT attachment when no sso_nonce in URL`() {
        val handler = NonceRedirectHandler(webView, headers, span)
        handler.processChallenge(URL(ESTS_URL_NO_NONCE))

        verify(span, never()).setAttribute(AttributeName.is_new_refresh_token_cred_header_attached.name, true)
        verify(webView).loadUrl(ESTS_URL_NO_NONCE, headers)
    }

    @Test
    fun `processChallenge skips PRT attachment when no PRT header in original headers`() {
        val headersWithoutPrt = HashMap<String, String>()
        mockkObject(CommonRefreshTokenCredentialProvider)

        val handler = NonceRedirectHandler(webView, headersWithoutPrt, span)
        handler.processChallenge(URL(ESTS_URL_WITH_NONCE_NO_HINT))

        verify(span, never()).setAttribute(AttributeName.is_new_refresh_token_cred_header_attached.name, true)
        verify(webView).loadUrl(ESTS_URL_WITH_NONCE_NO_HINT, headersWithoutPrt)
    }
}
