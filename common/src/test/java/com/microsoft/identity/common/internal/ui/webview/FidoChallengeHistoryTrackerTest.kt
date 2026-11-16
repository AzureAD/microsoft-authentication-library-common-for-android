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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

/**
 * Unit tests for [FidoChallengeHistoryTracker] covering superseded FIDO challenge history skipping,
 * multi-step back navigation, auth-cancel fallback, and forward-branch replacement pruning.
 */
@RunWith(RobolectricTestRunner::class)
class FidoChallengeHistoryTrackerTest {

    private data class FakeHistorySpec(
        val url: String?,
        val originalUrl: String? = url
    )

    private fun buildHistory(
        entries: List<FakeHistorySpec>,
        currentIndex: Int
    ): WebBackForwardList {
        val list = mock(WebBackForwardList::class.java)
        whenever(list.size).thenReturn(entries.size)
        whenever(list.currentIndex).thenReturn(currentIndex)
        entries.forEachIndexed { index, spec ->
            val item = mock(WebHistoryItem::class.java)
            whenever(item.url).thenReturn(spec.url)
            whenever(item.originalUrl).thenReturn(spec.originalUrl)
            whenever(list.getItemAtIndex(index)).thenReturn(item)
        }
        return list
    }

    private fun stubBackForwardList(
        webView: WebView,
        entries: List<FakeHistorySpec>,
        currentIndex: Int
    ) {
        val history = buildHistory(entries = entries, currentIndex = currentIndex)
        whenever(webView.copyBackForwardList()).thenReturn(history)
    }

    @Test
    fun navigateBackSkippingSupersededEntries_nullWebView_returnsFalse() {
        val tracker = FidoChallengeHistoryTracker()

        assertFalse(tracker.navigateBackSkippingSupersededEntries(null))
    }

    @Test
    fun navigateBackSkippingSupersededEntries_noSupersededEntriesAndCanGoBack_callsGoBackWithoutCopyingHistory() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        whenever(webView.canGoBack()).thenReturn(true)

        assertTrue(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView).goBack()
        verify(webView, never()).copyBackForwardList()
    }

    @Test
    fun navigateBackSkippingSupersededEntries_noSupersededEntriesAndCannotGoBack_returnsFalse() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        whenever(webView.canGoBack()).thenReturn(false)

        assertFalse(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView, never()).goBack()
        verify(webView, never()).copyBackForwardList()
    }

    @Test
    fun navigateBackSkippingSupersededEntries_initialPasskeyChallengeAtIndexZero_cancelsWithoutGoingBack() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        val challengeUrl = "https://login.live.com/oauth20_authorize.srf?login_hint=user%40outlook.com"
        val submitUrl = "https://login.microsoftonline.com/common/fido/get?uiflavor=Web"
        val fallbackUrl = "https://login.live.com/ppsecure/post.srf?contextid=123"

        // Step 1: Passkey challenge fires while WebView is at History #0.
        val historyAtChallenge = buildHistory(
            entries = listOf(FakeHistorySpec(url = challengeUrl)),
            currentIndex = 0
        )
        whenever(webView.copyBackForwardList()).thenReturn(historyAtChallenge)
        tracker.recordSupersededCurrentEntry(webView)

        // Step 2: loadUrl(submitUrl, header) redirects to "Sign in another way" at History #1.
        val historyAtFallback = buildHistory(
            entries = listOf(
                FakeHistorySpec(url = challengeUrl),
                FakeHistorySpec(url = fallbackUrl, originalUrl = submitUrl)
            ),
            currentIndex = 1
        )
        whenever(webView.copyBackForwardList()).thenReturn(historyAtFallback)
        whenever(webView.canGoBack()).thenReturn(true)

        // Step 3: Pressing Back on "Sign in another way" skips #0 and signals auth cancellation.
        assertFalse(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView, never()).goBack()
        verify(webView, never()).goBackOrForward(any())
    }

    @Test
    fun navigateBackSkippingSupersededEntries_emailEntryBeforePasskeyChallenge_jumpsTwoStepsBack() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        val emailEntryUrl = "https://login.live.com/oauth20_authorize.srf"
        val passkeyChallengeUrl = "https://login.live.com/ppsecure/post.srf?opid=111"
        val submitUrl = "https://login.microsoftonline.com/common/fido/get?uiflavor=Web"
        val fallbackUrl = "https://login.live.com/ppsecure/post.srf?opid=222"

        // Passkey challenge fires at History #1 (after user typed email on #0).
        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailEntryUrl),
                FakeHistorySpec(url = passkeyChallengeUrl)
            ),
            currentIndex = 1
        )
        tracker.recordSupersededCurrentEntry(webView)

        // Submitting passkey error lands on "Sign in another way" at History #2.
        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailEntryUrl),
                FakeHistorySpec(url = passkeyChallengeUrl),
                FakeHistorySpec(url = fallbackUrl, originalUrl = submitUrl)
            ),
            currentIndex = 2
        )
        whenever(webView.canGoBackOrForward(-2)).thenReturn(true)

        assertTrue(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView).goBackOrForward(-2)
        verify(webView, never()).goBack()
    }

    @Test
    fun navigateBackSkippingSupersededEntries_cannotGoBackOrForwardTargetSteps_returnsFalse() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        val emailEntryUrl = "https://login.live.com/oauth20_authorize.srf"
        val passkeyChallengeUrl = "https://login.live.com/ppsecure/post.srf?opid=111"
        val fallbackUrl = "https://login.live.com/ppsecure/post.srf?opid=222"

        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailEntryUrl),
                FakeHistorySpec(url = passkeyChallengeUrl)
            ),
            currentIndex = 1
        )
        tracker.recordSupersededCurrentEntry(webView)

        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailEntryUrl),
                FakeHistorySpec(url = passkeyChallengeUrl),
                FakeHistorySpec(url = fallbackUrl)
            ),
            currentIndex = 2
        )
        whenever(webView.canGoBackOrForward(-2)).thenReturn(false)

        assertFalse(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView, never()).goBackOrForward(any())
    }

    @Test
    fun navigateBackSkippingSupersededEntries_postFallbackPasswordPage_navigatesToSignInAnotherWayThenCancels() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        val challengeUrl = "https://login.microsoftonline.com/common/oauth2/v2.0/authorize?login_hint=user%40contoso.com"
        val submitUrl = "https://login.microsoftonline.com/common/fido/get"
        val passwordUrl = "https://login.microsoftonline.com/common/login"

        // Record #0 as superseded passkey challenge.
        stubBackForwardList(
            webView = webView,
            entries = listOf(FakeHistorySpec(url = challengeUrl)),
            currentIndex = 0
        )
        tracker.recordSupersededCurrentEntry(webView)

        // User is on #2 (Password page) after #1 ("Sign in another way" rendered directly at submitUrl).
        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = challengeUrl),
                FakeHistorySpec(url = submitUrl),
                FakeHistorySpec(url = passwordUrl)
            ),
            currentIndex = 2
        )
        whenever(webView.canGoBack()).thenReturn(true)

        // First Back from #2 -> goes 1 step back to #1 ("Sign in another way").
        assertTrue(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView).goBack()

        // Second Back from #1 -> skips #0 (superseded challenge) and cancels auth.
        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = challengeUrl),
                FakeHistorySpec(url = submitUrl),
                FakeHistorySpec(url = passwordUrl)
            ),
            currentIndex = 1
        )
        assertFalse(tracker.navigateBackSkippingSupersededEntries(webView))
    }

    @Test
    fun navigateBackSkippingSupersededEntries_forwardBranchReplacedWithSamePostUrl_doesNotSkipNewBranch() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        val emailEntryUrl = "https://login.live.com/oauth20_authorize.srf"
        val sharedPostUrl = "https://login.live.com/ppsecure/post.srf"
        val fallbackUrl = "https://login.live.com/ppsecure/post.srf?fallback=1"
        val forgotPasswordUrl = "https://account.live.com/ResetPassword.aspx"

        // Record #1 (sharedPostUrl) as superseded passkey challenge.
        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailEntryUrl),
                FakeHistorySpec(url = sharedPostUrl)
            ),
            currentIndex = 1
        )
        tracker.recordSupersededCurrentEntry(webView)

        // Jump back -2 from #2 to #0 (pruning #1).
        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailEntryUrl),
                FakeHistorySpec(url = sharedPostUrl),
                FakeHistorySpec(url = fallbackUrl)
            ),
            currentIndex = 2
        )
        whenever(webView.canGoBackOrForward(-2)).thenReturn(true)
        assertTrue(tracker.navigateBackSkippingSupersededEntries(webView))

        // User enters a different password-only account: new #1 has the EXACT same sharedPostUrl,
        // and user clicks "Forgot password" (#2). Pressing Back from #2 must go to #1, not skip it!
        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailEntryUrl),
                FakeHistorySpec(url = sharedPostUrl),
                FakeHistorySpec(url = forgotPasswordUrl)
            ),
            currentIndex = 2
        )
        whenever(webView.canGoBack()).thenReturn(true)

        assertTrue(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView).goBack()
    }

    @Test
    fun navigateBackSkippingSupersededEntries_multipleConsecutiveSupersededEntries_skipsAllInSingleStep() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        val emailUrl = "https://login.live.com/oauth20_authorize.srf"
        val challenge1Url = "https://login.live.com/ppsecure/post.srf?step=1"
        val challenge2Url = "https://login.live.com/ppsecure/post.srf?step=2"
        val fallbackUrl = "https://login.live.com/ppsecure/post.srf?step=3"

        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailUrl),
                FakeHistorySpec(url = challenge1Url)
            ),
            currentIndex = 1
        )
        tracker.recordSupersededCurrentEntry(webView)

        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailUrl),
                FakeHistorySpec(url = challenge1Url),
                FakeHistorySpec(url = challenge2Url)
            ),
            currentIndex = 2
        )
        tracker.recordSupersededCurrentEntry(webView)

        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailUrl),
                FakeHistorySpec(url = challenge1Url),
                FakeHistorySpec(url = challenge2Url),
                FakeHistorySpec(url = fallbackUrl)
            ),
            currentIndex = 3
        )
        whenever(webView.canGoBackOrForward(-3)).thenReturn(true)

        assertTrue(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView).goBackOrForward(-3)
    }

    @Test
    fun navigateBackSkippingSupersededEntries_urlChangedAtRecordedIndex_treatsEntryAsValidAndNavigatesToIt() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)
        val emailUrl = "https://login.live.com/oauth20_authorize.srf"
        val challengeUrl = "https://login.live.com/ppsecure/post.srf?challenge=1"
        val replacedUrl = "https://login.live.com/ppsecure/post.srf?password=1"
        val currentUrl = "https://account.live.com/ResetPassword.aspx"

        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailUrl),
                FakeHistorySpec(url = challengeUrl)
            ),
            currentIndex = 1
        )
        tracker.recordSupersededCurrentEntry(webView)

        // Suppose page JS replaced #1 with a different URL and navigated to #2.
        stubBackForwardList(
            webView = webView,
            entries = listOf(
                FakeHistorySpec(url = emailUrl),
                FakeHistorySpec(url = replacedUrl),
                FakeHistorySpec(url = currentUrl)
            ),
            currentIndex = 2
        )
        whenever(webView.canGoBack()).thenReturn(true)

        assertTrue(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView).goBack()
        verify(webView, never()).goBackOrForward(any())
    }

    @Test
    fun recordSupersededCurrentEntry_handlesNullOrEmptyHistoryGracefully() {
        val tracker = FidoChallengeHistoryTracker()
        val webView = mock(WebView::class.java)

        // Null BackForwardList
        whenever(webView.copyBackForwardList()).thenReturn(null)
        tracker.recordSupersededCurrentEntry(webView)

        // Empty BackForwardList (currentIndex = -1)
        stubBackForwardList(webView = webView, entries = emptyList(), currentIndex = -1)
        tracker.recordSupersededCurrentEntry(webView)

        // Null URLs in current item
        stubBackForwardList(
            webView = webView,
            entries = listOf(FakeHistorySpec(url = null, originalUrl = null)),
            currentIndex = 0
        )
        tracker.recordSupersededCurrentEntry(webView)

        whenever(webView.canGoBack()).thenReturn(true)
        assertTrue(tracker.navigateBackSkippingSupersededEntries(webView))
        verify(webView).goBack()
    }
}
