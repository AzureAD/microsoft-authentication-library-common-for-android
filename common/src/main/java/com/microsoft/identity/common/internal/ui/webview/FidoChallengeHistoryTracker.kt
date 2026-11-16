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
package com.microsoft.identity.common.internal.ui.webview

import android.webkit.WebBackForwardList
import android.webkit.WebHistoryItem
import android.webkit.WebView
import androidx.annotation.UiThread
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.metrics.LongCounter

/**
 * Tracks [WebBackForwardList] entries whose FIDO/passkey challenge (`urn:http-auth:PassKey`)
 * has already been answered via `WebView.loadUrl(submitUrl, headers)` so interactive Back
 * navigation does not revisit an auto-triggering passkey challenge page in a loop (AB#3599772).
 *
 * All methods must be invoked on the Android UI thread owning the [WebView].
 */
class FidoChallengeHistoryTracker {

    private data class SupersededHistoryEntry(
        val url: String?,
        val originalUrl: String?
    ) {
        fun matches(item: WebHistoryItem): Boolean {
            if (url == null && originalUrl == null) {
                return false
            }
            return url == item.url && originalUrl == item.originalUrl
        }
    }

    private val supersededEntries = mutableMapOf<Int, SupersededHistoryEntry>()

    /**
     * Marks the [WebView]'s current [WebBackForwardList] entry as superseded immediately before
     * `AuthFidoChallengeHandler` submits the passkey challenge response to `submitUrl`.
     *
     * @param webView The authentication [WebView] about to load the FIDO `submitUrl`.
     */
    @UiThread
    fun recordSupersededCurrentEntry(webView: WebView) {
        val methodTag = "$TAG:recordSupersededCurrentEntry"
        val history = webView.copyBackForwardList() ?: return
        val currentIndex = history.currentIndex
        if (currentIndex < 0 || currentIndex >= history.size) {
            return
        }
        val currentItem = history.getItemAtIndex(currentIndex) ?: return
        if (currentItem.url == null && currentItem.originalUrl == null) {
            return
        }
        // Submitting a new load from currentIndex truncates any forward history past currentIndex.
        supersededEntries.keys.removeAll { it > currentIndex }
        supersededEntries[currentIndex] = SupersededHistoryEntry(
            url = currentItem.url,
            originalUrl = currentItem.originalUrl
        )
        Logger.verbose(
            methodTag,
            "Recorded superseded FIDO challenge history entry at index $currentIndex."
        )
    }

    /**
     * Navigates backward in [webView] while skipping any preceding [WebBackForwardList] entries
     * recorded as superseded FIDO challenge pages.
     *
     * @param webView The authentication [WebView], or `null` if unavailable.
     * @return `true` if backward navigation was performed in [webView]; `false` if no valid
     * non-superseded history entry precedes the current page and authorization should be cancelled.
     */
    @UiThread
    fun navigateBackSkippingSupersededEntries(webView: WebView?): Boolean {
        val methodTag = "$TAG:navigateBackSkippingSupersededEntries"
        if (webView == null) {
            return false
        }
        if (supersededEntries.isEmpty()) {
            return navigateSingleStepBack(webView)
        }

        val history = webView.copyBackForwardList() ?: return navigateSingleStepBack(webView)
        supersededEntries.keys.removeAll { it >= history.size }

        val currentIndex = history.currentIndex
        if (currentIndex <= 0) {
            return false
        }

        var skippedCount = 0
        var targetIndex = -1
        for (index in (currentIndex - 1) downTo 0) {
            val item = history.getItemAtIndex(index)
            if (item != null && isEntrySuperseded(index, item)) {
                skippedCount++
            } else {
                targetIndex = index
                break
            }
        }

        if (skippedCount == 0) {
            supersededEntries.keys.removeAll { it > targetIndex }
            return navigateSingleStepBack(webView)
        }

        if (targetIndex >= 0) {
            val steps = targetIndex - currentIndex
            supersededEntries.keys.removeAll { it > targetIndex }
            Logger.info(
                methodTag,
                "Skipping $skippedCount superseded FIDO challenge history entries; navigating $steps steps."
            )
            recordSkipOutcome(OUTCOME_SKIPPED_GO_BACK)
            return if (webView.canGoBackOrForward(steps)) {
                webView.goBackOrForward(steps)
                true
            } else {
                Logger.warn(
                    methodTag,
                    "WebView cannot navigate $steps steps; falling back to authorization cancel."
                )
                false
            }
        }

        supersededEntries.clear()
        Logger.info(
            methodTag,
            "All $skippedCount preceding history entries are superseded FIDO challenges; cancelling authorization."
        )
        recordSkipOutcome(OUTCOME_SKIPPED_CANCEL_AUTH)
        return false
    }

    private fun isEntrySuperseded(index: Int, item: WebHistoryItem): Boolean {
        val recorded = supersededEntries[index] ?: return false
        if (!recorded.matches(item)) {
            supersededEntries.remove(index)
            return false
        }
        return true
    }

    private fun navigateSingleStepBack(webView: WebView): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        } else {
            false
        }
    }

    private fun recordSkipOutcome(outcome: String) {
        val attributes = Attributes.builder()
            .put(AttributeName.operation_outcome.name, outcome)
            .build()
        sFidoBackNavigationSkipCount.add(1, attributes)
    }

    companion object {
        private val TAG = FidoChallengeHistoryTracker::class.java.simpleName

        internal const val OUTCOME_SKIPPED_GO_BACK = "fido_challenge_page_skipped_go_back"
        internal const val OUTCOME_SKIPPED_CANCEL_AUTH = "fido_challenge_page_skipped_cancel_auth"

        private val sFidoBackNavigationSkipCount: LongCounter = OTelUtility.createLongCounter(
            "fido_webview_back_navigation_skip_count",
            "Number of WebView back presses that skipped superseded FIDO challenge history entries"
        )
    }
}
