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
package com.microsoft.identity.common.internal.providers.oauth2

import android.app.Activity
import android.webkit.WebBackForwardList
import android.webkit.WebHistoryItem
import android.webkit.WebView
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.testing.TestLifecycleOwner
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.internal.fido.FidoManagerFactory
import com.microsoft.identity.common.internal.fido.IFidoManager
import com.microsoft.identity.common.internal.fido.IFidoManagerProvider
import com.microsoft.identity.common.internal.fido.TestFidoManager
import com.microsoft.identity.common.internal.ui.webview.AzureActiveDirectoryWebViewClient
import com.microsoft.identity.common.internal.ui.webview.OnPageLoadedCallback
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator
import com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.IS_SWITCH_BROWSER_FLOW
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightConfig
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.java.ui.webview.authorization.IAuthorizationCompletionCallback
import org.junit.After
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyBoolean
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.Mockito.doAnswer
import org.mockito.Mockito.doNothing
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.spy
import org.mockito.Mockito.verify
import org.mockito.kotlin.whenever
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewAuthorizationFragmentTest {

    companion object {
        private const val TEST_REDIRECT_URI = "msauth://com.microsoft.test/callback"
        private const val TEST_PASSKEY_REDIRECT_URL =
            "urn:http-auth:PassKey?challenge=T1xCsnxM2DNL2KdK5CLa6fMhD7OBqho6syzInk_n-Uo" +
                "&version=1.0" +
                "&submitUrl=https%3A%2F%2Flogin.microsoftonline.com%2Fcommon%2Ffido%2Fget" +
                "&context=ctx%20flowToken" +
                "&relyingPartyIdentifier=login.microsoft.com" +
                "&allowedCredentials=user"
    }

    private lateinit var testLifecycleOwner: LifecycleOwner

    @Before
    fun setUp() {
        testLifecycleOwner = TestLifecycleOwner()
        CommonFlightsManager.resetFlightsManager()
        FidoManagerFactory.setProvider(ApplicationProvider.getApplicationContext(), null)
        FidoManagerFactory.isBrokerHosted = { true }
    }

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
        FidoManagerFactory.setProvider(ApplicationProvider.getApplicationContext(), null)
    }

    private fun configureSkipPasskeyFlight(enabled: Boolean) {
        val mockFlightsProvider = mock(IFlightsProvider::class.java)
        whenever(mockFlightsProvider.isFlightEnabled(any(IFlightConfig::class.java))).thenAnswer { invocation ->
            val config = invocation.getArgument<IFlightConfig>(0)
            if (config == CommonFlight.SKIP_PASSKEY_CHALLENGE_ON_WEBVIEW_BACK) {
                enabled
            } else {
                (config.defaultValue as? Boolean) ?: false
            }
        }
        val mockFlightsManager = object : IFlightsManager {
            override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long): IFlightsProvider =
                mockFlightsProvider

            override fun getFlightsProviderForTenant(
                tenantId: String,
                waitForConfigsWithTimeoutInMs: Long
            ): IFlightsProvider = mockFlightsProvider
        }
        CommonFlightsManager.initializeCommonFlightsManager(mockFlightsManager)
    }

    private fun registerTestFidoManager(challengeOverrideManager: IFidoManager = TestFidoManager()) {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        FidoManagerFactory.setProvider(
            context,
            object : IFidoManagerProvider {
                override fun getFidoManager(activity: Activity): IFidoManager = challengeOverrideManager
            }
        )
    }

    private fun buildHistory(urls: List<String>, currentIndex: Int): WebBackForwardList {
        val list = mock(WebBackForwardList::class.java)
        whenever(list.size).thenReturn(urls.size)
        whenever(list.currentIndex).thenReturn(currentIndex)
        urls.forEachIndexed { index, url ->
            val item = mock(WebHistoryItem::class.java)
            whenever(item.url).thenReturn(url)
            whenever(item.originalUrl).thenReturn(url)
            whenever(list.getItemAtIndex(index)).thenReturn(item)
        }
        return list
    }

    private fun createClient(activity: Activity): AzureActiveDirectoryWebViewClient {
        return AzureActiveDirectoryWebViewClient(
            activity,
            object : IAuthorizationCompletionCallback {
                override fun onChallengeResponseReceived(response: RawAuthorizationResult) = Unit
                override fun setPKeyAuthStatus(status: Boolean) = Unit
            },
            OnPageLoadedCallback { },
            TEST_REDIRECT_URI,
            mock(SwitchBrowserProtocolCoordinator::class.java),
            "utid",
            false
        )
    }

    private fun stubBackForwardList(webView: WebView, urls: List<String>, currentIndex: Int) {
        val history = buildHistory(urls = urls, currentIndex = currentIndex)
        whenever(webView.copyBackForwardList()).thenReturn(history)
    }

    private fun createMockWebView(activity: Activity): WebView {
        val webView = mock(WebView::class.java)
        whenever(webView.context).thenReturn(activity)
        val tags = mutableMapOf<Int, Any?>()
        doAnswer { invocation ->
            tags[invocation.getArgument(0)] = invocation.getArgument(1)
            null
        }.`when`(webView).setTag(anyInt(), any())
        whenever(webView.getTag(anyInt())).thenAnswer { invocation ->
            tags[invocation.getArgument(0)]
        }
        whenever(webView.post(any())).thenAnswer { invocation ->
            invocation.getArgument<Runnable?>(0)?.run()
            true
        }
        webView.setViewTreeLifecycleOwner(testLifecycleOwner)
        return webView
    }

    private fun createFragment(): WebViewAuthorizationFragment {
        return WebViewAuthorizationFragment()
    }

    private fun attachWebView(
        fragment: WebViewAuthorizationFragment,
        webView: WebView,
        client: AzureActiveDirectoryWebViewClient
    ) {
        WebViewAuthorizationFragment::class.java.getDeclaredField("mWebView").apply {
            isAccessible = true
            set(fragment, webView)
        }
        WebViewAuthorizationFragment::class.java.getDeclaredField("mAADWebViewClient").apply {
            isAccessible = true
            set(fragment, client)
        }
    }

    @Test
    fun testPropertyBagContainsFlagWhenFlowWasInitiated() {
        val fragment = createFragment()
        val mockHandler = mock(SwitchBrowserProtocolCoordinator::class.java)
        whenever(mockHandler.wasSwitchBrowserFlowInitiated).thenReturn(true)
        fragment.setSwitchBrowserProtocolCoordinator(mockHandler)

        val result = RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
        val propertyBag = fragment.propertyBagFromAuthorizationResult(result)

        assertTrue(propertyBag.get<Boolean>(IS_SWITCH_BROWSER_FLOW) == true)
    }

    @Test
    fun testPropertyBagDoesNotContainFlagWhenFlowWasNotInitiated() {
        val fragment = createFragment()
        val mockHandler = mock(SwitchBrowserProtocolCoordinator::class.java)
        whenever(mockHandler.wasSwitchBrowserFlowInitiated).thenReturn(false)
        fragment.setSwitchBrowserProtocolCoordinator(mockHandler)

        val result = RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
        val propertyBag = fragment.propertyBagFromAuthorizationResult(result)

        assertNull(propertyBag.get<Boolean>(IS_SWITCH_BROWSER_FLOW))
    }

    @Test
    fun testPropertyBagDoesNotContainFlagWhenHandlerIsNull() {
        val fragment = createFragment()
        // handler is null by default

        val result = RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
        val propertyBag = fragment.propertyBagFromAuthorizationResult(result)

        assertNull(propertyBag.get<Boolean>(IS_SWITCH_BROWSER_FLOW))
    }

    @Test
    fun handleBackButtonPressed_flightDisabled_preservesLegacyBackNavigation() {
        configureSkipPasskeyFlight(enabled = false)
        registerTestFidoManager()

        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val client = createClient(activity)
        val webView = createMockWebView(activity)
        val fragment = spy(createFragment())
        doNothing().`when`(fragment).cancelAuthorization(anyBoolean())
        attachWebView(fragment, webView, client)

        assertTrue(client.shouldOverrideUrlLoading(webView, TEST_PASSKEY_REDIRECT_URL))

        whenever(webView.canGoBack()).thenReturn(true)
        fragment.handleBackButtonPressed()
        verify(webView).goBack()
        verify(webView, never()).copyBackForwardList()
        verify(fragment, never()).cancelAuthorization(anyBoolean())

        whenever(webView.canGoBack()).thenReturn(false)
        fragment.handleBackButtonPressed()
        verify(fragment).cancelAuthorization(true)
        assertTrue(!client.navigateBackSkippingSupersededPasskeyPages(webView))
    }

    @Test
    fun handleBackButtonPressed_flightEnabledAndInitialPasskeyAtIndexZero_cancelsAuthInsteadOfLooping() {
        configureSkipPasskeyFlight(enabled = true)
        registerTestFidoManager()

        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val client = createClient(activity)
        val webView = createMockWebView(activity)
        val fragment = spy(createFragment())
        doNothing().`when`(fragment).cancelAuthorization(anyBoolean())
        attachWebView(fragment, webView, client)

        val challengeUrl = "https://login.live.com/oauth20_authorize.srf?login_hint=user%40outlook.com"
        val fallbackUrl = "https://login.live.com/ppsecure/post.srf?contextid=123"

        stubBackForwardList(webView = webView, urls = listOf(challengeUrl), currentIndex = 0)
        assertTrue(client.shouldOverrideUrlLoading(webView, TEST_PASSKEY_REDIRECT_URL))

        // Retry passkey from fallback page (#1) as well, then press Back on #2 -> skips #1 and #0 and cancels.
        stubBackForwardList(webView = webView, urls = listOf(challengeUrl, fallbackUrl), currentIndex = 1)
        assertTrue(client.shouldOverrideUrlLoading(webView, TEST_PASSKEY_REDIRECT_URL))

        stubBackForwardList(
            webView = webView,
            urls = listOf(challengeUrl, fallbackUrl, fallbackUrl),
            currentIndex = 2
        )
        whenever(webView.canGoBack()).thenReturn(true)

        fragment.handleBackButtonPressed()

        verify(webView, never()).goBack()
        verify(webView, never()).goBackOrForward(anyInt())
        verify(fragment).cancelAuthorization(true)
    }

    @Test
    fun handleBackButtonPressed_flightEnabledWithEarlierAndDeeperPages_skipsOnlyMatchingSupersededEntries() {
        configureSkipPasskeyFlight(enabled = true)
        registerTestFidoManager()

        val activity = Robolectric.buildActivity(Activity::class.java).setup().get()
        val client = createClient(activity)
        val webView = createMockWebView(activity)
        val fragment = spy(createFragment())
        doNothing().`when`(fragment).cancelAuthorization(anyBoolean())
        attachWebView(fragment, webView, client)

        val emailEntryUrl = "https://login.live.com/oauth20_authorize.srf"
        val passkeyChallengeUrl = "https://login.live.com/ppsecure/post.srf?opid=111"
        val fallbackUrl = "https://login.live.com/ppsecure/post.srf?opid=222"
        val passwordUrl = "https://login.live.com/ppsecure/post.srf?opid=333"

        stubBackForwardList(webView = webView, urls = listOf(emailEntryUrl, passkeyChallengeUrl), currentIndex = 1)
        assertTrue(client.shouldOverrideUrlLoading(webView, TEST_PASSKEY_REDIRECT_URL))

        // Deeper non-superseded page (#3 -> #2) uses normal single-step goBack().
        stubBackForwardList(
            webView = webView,
            urls = listOf(emailEntryUrl, passkeyChallengeUrl, fallbackUrl, passwordUrl),
            currentIndex = 3
        )
        whenever(webView.canGoBack()).thenReturn(true)
        fragment.handleBackButtonPressed()
        verify(webView).goBack()

        // Next Back from fallback (#2) skips superseded passkey page (#1) to email page (#0) via -2 steps.
        stubBackForwardList(
            webView = webView,
            urls = listOf(emailEntryUrl, passkeyChallengeUrl, fallbackUrl),
            currentIndex = 2
        )
        whenever(webView.canGoBackOrForward(-2)).thenReturn(true)
        fragment.handleBackButtonPressed()
        verify(webView).goBackOrForward(-2)
        verify(fragment, never()).cancelAuthorization(anyBoolean())

        // If a recorded history slot changes URL (e.g., replaceState), it is no longer skipped.
        stubBackForwardList(webView = webView, urls = listOf(emailEntryUrl, passkeyChallengeUrl), currentIndex = 1)
        assertTrue(client.shouldOverrideUrlLoading(webView, TEST_PASSKEY_REDIRECT_URL))
        stubBackForwardList(
            webView = webView,
            urls = listOf(emailEntryUrl, "https://login.live.com/ppsecure/post.srf?different=1", fallbackUrl),
            currentIndex = 2
        )
        assertTrue(client.navigateBackSkippingSupersededPasskeyPages(webView))
    }
}
