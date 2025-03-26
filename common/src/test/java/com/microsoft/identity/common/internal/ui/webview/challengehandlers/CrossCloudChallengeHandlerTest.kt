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

import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.webkit.WebView
import com.microsoft.identity.common.java.broker.CommonRefreshTokenCredentialProvider
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import io.mockk.every
import io.mockk.mockkObject
import io.mockk.unmockkAll
import io.opentelemetry.api.trace.Span
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.*

@RunWith(RobolectricTestRunner::class)
class CrossCloudChallengeHandlerTest {

    private lateinit var webView: WebView
    private lateinit var headers: HashMap<String, String>
    private lateinit var span: Span
    private lateinit var crossCloudChallengeHandler: CrossCloudChallengeHandler

    @Before
    fun setUp() {
        webView = mock(WebView::class.java)
        headers = HashMap()
        span = mock(Span::class.java)
        crossCloudChallengeHandler = CrossCloudChallengeHandler(webView, headers, span)
    }

    @Test
    fun `testProcessChallenge success`() {
        val testUrl = "https://example.com?login_hint=testuser"
        crossCloudChallengeHandler.processChallenge(testUrl)
        verify(webView).loadUrl(eq(testUrl), eq(headers))
    }

    @Test
    fun `testProcessChallenge when exception is thrown`() {
        val testUrl = "https://example.com?login_hint=testuser"

        mockkObject(CommonRefreshTokenCredentialProvider)
        every {
            CommonRefreshTokenCredentialProvider.getRefreshTokenCredential(
                testUrl,
                "testuser"
            )
        } throws Exception()

        try {
            crossCloudChallengeHandler.processChallenge(testUrl)
        } catch (e: Exception) {
            verify(webView, never()).loadUrl(eq(testUrl), eq(headers))
        }
    }

    @Test
    fun `modifyHeadersWithRefreshTokenCredential should update headers when prt is available`() {
        val url = "https://login.microsoftonline.com?login_hint=testuser"
        val username = "testuser"
        val refreshTokenCredential = "refreshTokenCredential"

        mockkObject(CommonRefreshTokenCredentialProvider)
        every {
            CommonRefreshTokenCredentialProvider.getRefreshTokenCredential(
                url,
                username
            )
        } returns refreshTokenCredential

        // Call the method
        crossCloudChallengeHandler.modifyHeadersWithRefreshTokenCredential(url)
        verify(span).setAttribute(
            AttributeName.is_new_refresh_token_cred_header_attached.name,
            true
        )
        unmockkAll()
    }

    @Test
    fun `modifyHeadersWithRefreshTokenCredential should not update headers when login_hint is missing`() {
        val url = "https://login.microsoftonline.com"
        crossCloudChallengeHandler.modifyHeadersWithRefreshTokenCredential(url)
        verify(span, never()).setAttribute(
            AttributeName.is_new_refresh_token_cred_header_attached.name,
            true
        )
    }

    @Test
    fun `modifyHeadersWithRefreshTokenCredential null refresh token credential`() {
        val url = "https://login.microsoftonline.com?login_hint=testuser"
        crossCloudChallengeHandler.modifyHeadersWithRefreshTokenCredential(url)
        verify(span, never()).setAttribute(
            AttributeName.is_new_refresh_token_cred_header_attached.name,
            true
        )
    }
}
