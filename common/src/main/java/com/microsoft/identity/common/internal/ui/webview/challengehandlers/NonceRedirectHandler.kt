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
import com.microsoft.identity.common.java.providers.microsoft.azureactivedirectory.AzureActiveDirectory
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import java.net.MalformedURLException
import java.net.URL

/**
 * Handler for processing nonce from redirect and attaching new prt credential header on web view.
 */
class NonceRedirectHandler(
    private val webView: WebView,
    private val headers: HashMap<String, String>,
    private val span : Span
) : IChallengeHandler<URL, Void> {

    override fun processChallenge(input: URL) : Void? {
        val methodTag = "$TAG:processChallenge"
        // SECURITY (CWE-918): the caller reaches this handler for any URL whose string merely
        // contains the "sso_nonce" substring, and that branch is evaluated before the SSL gate and
        // before every host-validated branch in AzureActiveDirectoryWebViewClient.handleUrl. So the
        // redirect target here is untrusted by default. The request headers carry the PRT credential
        // header (x-ms-RefreshTokenCredential); forwarding it to an arbitrary/cleartext host leaks a
        // valid, AAD-audience-bound PRT. Only forward credential headers to an HTTPS, trusted AAD
        // cloud host. Otherwise strip the credential and still load the page, so an untrusted hop
        // simply loses SSO instead of dead-ending sign-in.
        if (!isRedirectTrustedForHeaderForwarding(input.toString())) {
            Logger.warn(
                methodTag,
                "Nonce redirect target is not a trusted HTTPS AAD host; " +
                    "loading without the PRT credential header."
            )
            webView.loadUrl(input.toString(), withoutCredentialHeaders(headers))
            return null
        }
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

    companion object {
        private val TAG = NonceRedirectHandler::class.java.simpleName

        /**
         * Determines whether the PRT credential header ([AuthenticationConstants.Broker.PRT_RESPONSE_HEADER])
         * may be forwarded to the given redirect target.
         *
         * The header is only forwarded when the target is both served over HTTPS and hosted by a
         * validated Azure Active Directory cloud host. This mirrors the trust checks already applied
         * by the sibling redirect branches in `AzureActiveDirectoryWebViewClient`
         * (`isCrossCloudRedirect`, `isWebCpAuthorizeUrl`, `isHeaderForwardingRequiredUri`).
         *
         * A cleartext (`http://`) target, a malformed URL, or a host that is not a validated AAD
         * cloud host all return `false`.
         *
         * @param url the redirect target URL, including scheme and host.
         * @return `true` if the target is trusted to receive credential headers, `false` otherwise.
         */
        @JvmStatic
        fun isRedirectTrustedForHeaderForwarding(url: String): Boolean {
            return try {
                val parsed = URL(url)
                parsed.protocol.equals("https", ignoreCase = true) &&
                    AzureActiveDirectory.isValidCloudHost(parsed)
            } catch (e: MalformedURLException) {
                false
            }
        }

        /**
         * Returns a copy of [headers] with the PRT credential header
         * ([AuthenticationConstants.Broker.PRT_RESPONSE_HEADER]) removed.
         *
         * A copy is returned so the caller's shared header map is left untouched; the original map
         * still carries the credential for subsequent, trusted navigations.
         *
         * @param headers the request headers to sanitize.
         * @return a new map without the PRT credential header.
         */
        @JvmStatic
        fun withoutCredentialHeaders(headers: HashMap<String, String>): HashMap<String, String> {
            val sanitized = HashMap(headers)
            sanitized.remove(AuthenticationConstants.Broker.PRT_RESPONSE_HEADER)
            return sanitized
        }
    }
}
