package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.app.Activity
import androidx.annotation.VisibleForTesting
import com.microsoft.identity.common.CommonPrtCredentialHolder
import com.microsoft.identity.common.adal.internal.util.StringExtensions
import com.microsoft.identity.common.java.util.StringUtil
import com.microsoft.identity.common.logging.Logger
import java.net.MalformedURLException
import java.net.URL

/**
 * Handler for processing nonce from redirect and attaching new prt credential header on web view.
 */
class NonceRedirectHandler {
    private val TAG = NonceRedirectHandler::class.java.simpleName

    private fun getPrtHeader(requestHeaders: HashMap<String, String>): String? {
        return requestHeaders["x-ms-RefreshTokenCredential"]
    }

    // Updates the headers by attaching a new refresh token credential header (Generated using the new nonce).
    fun getHeadersWithNewRefreshTokenCredential(
        requestHeaders: HashMap<String, String>,
        nonce: String,
        url: String,
        activity: Activity
    ): Map<String, String> {
        val methodTag = "$TAG:getHeadersWithNewRefreshTokenCredential"
        val prtHeader = getPrtHeader(requestHeaders)
        if (!StringUtil.isNullOrEmpty(prtHeader)) {
            Logger.info(methodTag, "PRT credential header found in headers! ")
            val authorityStr = getAuthorityFromWebViewUrl(url, methodTag)
            val username = getUserNameFromWebViewUrl(url)
            if (authorityStr != null && username != null) {
                val updatedRefreshTokenCredentialHeader =
                    CommonPrtCredentialHolder.getRefreshTokenCredentialUsingNewNonce(
                        authorityStr, username,
                        nonce,
                        prtHeader!!,
                        activity
                    )
                if (updatedRefreshTokenCredentialHeader != null) {
                    requestHeaders["x-ms-RefreshTokenCredential"] =
                        updatedRefreshTokenCredentialHeader
                }

            }
        } // Else it is a no-op.
        return requestHeaders
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getAuthorityFromWebViewUrl(url: String, methodTag: String): String? {
        try {
            val parsedUrl = URL(url)
            return parsedUrl.protocol + "://" + parsedUrl.host + parsedUrl.path
        } catch (e: MalformedURLException) {
            Logger.error(methodTag, "Could not parse webview url to get the authority", e)
            return null
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    internal fun getUserNameFromWebViewUrl(url: String): String? {
        val parameters: Map<String, String> = StringExtensions.getUrlParameters(url)
        val username = parameters["login_hint"]
        return username
    }
}