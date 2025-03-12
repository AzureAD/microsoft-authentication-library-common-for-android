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
import com.microsoft.identity.common.adal.internal.util.StringExtensions
import com.microsoft.identity.common.java.broker.CommonRefreshTokenCredentialProvider
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.internal.StringUtils
import io.opentelemetry.api.trace.Span
import java.net.URL

/**
 * Handler for attaching prt credential header on web view in cross cloud redirect scenarios.
 */
class CrossCloudChallengeHandler(
    private val webView: WebView,
    private val headers: HashMap<String, String>,
    private val span : Span
) : IChallengeHandler<String, Void> {
    private val TAG = CrossCloudChallengeHandler::class.java.simpleName

    override fun processChallenge(inputUrl: String): Void? {
        Logger.info(TAG, "Processing challenge of a cross cloud request.")
        modifyHeadersWithRefreshTokenCredential(inputUrl)
        webView.loadUrl(inputUrl, headers)
        return null
    }

    // Updates the headers by attaching a refresh token credential header.
    private fun modifyHeadersWithRefreshTokenCredential(
        url: String,
    ) {
        val methodTag = "$TAG:modifyHeadersWithRefreshTokenCredential"
        val parameters: Map<String, String> = StringExtensions.getUrlParameters(url)
        val username = parameters["login_hint"]
        if (!username.isNullOrEmpty()) {
            val updatedRefreshTokenCredentialHeader =
                CommonRefreshTokenCredentialProvider.getRefreshTokenCredential(
                    url, username
                )
            if (!updatedRefreshTokenCredentialHeader.isNullOrEmpty()) {
                Logger.info(methodTag, "Attaching refresh token credential in headers.")
                span.setAttribute(AttributeName.is_new_refresh_token_cred_header_attached.name, true)
                headers[AuthenticationConstants.Broker.PRT_RESPONSE_HEADER] =
                    updatedRefreshTokenCredentialHeader
            }
        }
    }
}
