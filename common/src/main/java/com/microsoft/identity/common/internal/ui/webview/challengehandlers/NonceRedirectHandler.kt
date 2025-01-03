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
import com.microsoft.identity.common.java.broker.CommonRefreshTokenCredentialProvider
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.SSO_NONCE_PARAMETER
import com.microsoft.identity.common.adal.internal.util.StringExtensions
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import java.net.URL

/**
 * Handler for processing nonce from redirect and attaching new prt credential header on web view.
 */
class NonceRedirectHandler(
    private val webView: WebView,
    private val headers: HashMap<String, String>,
    private val span : Span
) : IChallengeHandler<URL, Void> {
    private val TAG = NonceRedirectHandler::class.java.simpleName

    override fun processChallenge(input: URL) : Void? {
        val nonce = getNonceFromRedirectUrl(input)
        if (nonce != null) {
            modifyHeadersWithNewRefreshTokenCredential(nonce, input.toString())
        }
        webView.loadUrl(input.toString(), headers)
        return null
    }

    private fun getNonceFromRedirectUrl(url: URL): String? {
        val parameters = StringExtensions.getUrlParameters(url.toString())
        return parameters[SSO_NONCE_PARAMETER]
    }

    private fun getPrtHeader(requestHeaders: HashMap<String, String>): String? {
        return requestHeaders[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]
    }

    // Updates the headers by attaching a new refresh token credential header (Generated using the new nonce).
    private fun modifyHeadersWithNewRefreshTokenCredential(
        nonce: String,
        url: String
    ) {
        val methodTag = "$TAG:getHeadersWithNewRefreshTokenCredential"
        val prtHeader = getPrtHeader(headers)
        if (!prtHeader.isNullOrEmpty()) {
            Logger.info(methodTag, "PRT credential header found in headers!")
            val username = getUserNameFromWebViewUrl(url)
            if (username != null) {
                val updatedRefreshTokenCredentialHeader =
                    CommonRefreshTokenCredentialProvider.getRefreshTokenCredentialUsingNewNonce(
                        url, username,
                        nonce
                    )
                if (updatedRefreshTokenCredentialHeader != null) {
                    headers[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER] =
                        updatedRefreshTokenCredentialHeader
                    span.setAttribute(AttributeName.is_new_refresh_token_cred_header_attached.name, true)
                }
            }
        }
    }

    private fun getUserNameFromWebViewUrl(url: String): String? {
        val parameters: Map<String, String> = StringExtensions.getUrlParameters(url)
        return parameters["login_hint"]
    }
}
