package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.microsoft.identity.common.internal.ui.browser.CustomTabsManager
import com.microsoft.identity.common.java.browser.Browser
import com.microsoft.identity.common.java.browser.IBrowserSelector
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.doNothing
import org.powermock.api.mockito.PowerMockito.`when`
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SwitchBrowserHandlerTest {

    @Test
    fun `test processChallenge success`() {
        // Mock parameters
        val activity = mock(Activity::class.java)
        doNothing().`when`(activity).startActivity(Intent())
        val context = mock(Context::class.java)
        val customTabsManager = mock(CustomTabsManager::class.java)
        val challenge = mock(SwitchBrowserChallenge::class.java)
        `when`(challenge.uri).thenReturn(Uri.parse("https://example.com"))
        val browserSelector = // Browser available
            IBrowserSelector { _, _ -> Browser("package", emptySet(), "browser", false) }

        val handler = SwitchBrowserHandler(activity, context, customTabsManager, browserSelector)
        val status = handler.processChallenge(challenge)
        Assert.assertTrue(status)
    }

    @Test
    fun `test processChallenge no browser available`() {
        // Mock parameters
        val activity = mock(Activity::class.java)
        doNothing().`when`(activity).startActivity(Intent())
        val context = mock(Context::class.java)
        val customTabsManager = mock(CustomTabsManager::class.java)
        val challenge = mock(SwitchBrowserChallenge::class.java)
        `when`(challenge.uri).thenReturn(Uri.parse("https://example.com"))
        val browserSelector = IBrowserSelector { _, _ -> null } // No browser available

        val handler = SwitchBrowserHandler(activity, context, customTabsManager, browserSelector)
        val status = handler.processChallenge(challenge)
        Assert.assertFalse(status)
    }

}
