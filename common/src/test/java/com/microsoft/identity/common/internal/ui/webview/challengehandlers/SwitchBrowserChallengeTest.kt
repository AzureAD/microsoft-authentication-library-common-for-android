package com.microsoft.identity.common.internal.ui.webview.challengehandlers

import android.net.Uri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SwitchBrowserChallengeTest {

    companion object {
        private const val CODE = "your-switch-browser-code"
        private const val ACTION_URI = "login.microsoftonline.com/switchbrowser/process"
        private const val ACTION = "action"
    }

    @Test
    fun `test constructFromRedirectUri with valid redirect uri`() {
        val redirectString = "${Broker.NEW_BROKER_REDIRECT_URI}?" +
                "${AuthenticationConstants.SWITCH_BROWSER.CODE}=$CODE&" +
                "${AuthenticationConstants.SWITCH_BROWSER.ACTION}=$ACTION&" +
                "${AuthenticationConstants.SWITCH_BROWSER.ACTION_URI}=$ACTION_URI"
        val redirectUri = Uri.parse(redirectString)

        val switchBrowserProcessUri = SwitchBrowserChallenge.constructFromRedirectUri(redirectUri)?.uri
        Assert.assertNotNull(switchBrowserProcessUri)
        Assert.assertEquals(
            CODE,
            switchBrowserProcessUri?.getQueryParameter(AuthenticationConstants.SWITCH_BROWSER.CODE)
        )
        Assert.assertEquals(
            Broker.NEW_BROKER_REDIRECT_URI,
            switchBrowserProcessUri?.getQueryParameter(AuthenticationConstants.OAuth2.REDIRECT_URI)
        )
        Assert.assertEquals(
            ACTION_URI,
            switchBrowserProcessUri?.host + switchBrowserProcessUri?.path
        )
    }

    @Test
    fun `test constructFromRedirectUri with missing code`() {
        val redirectString = "${Broker.NEW_BROKER_REDIRECT_URI}?" +
                "${AuthenticationConstants.SWITCH_BROWSER.ACTION}=$ACTION&" +
                "${AuthenticationConstants.SWITCH_BROWSER.ACTION_URI}=$ACTION_URI"
        val redirectUri = Uri.parse(redirectString)

        val switchBrowserProcessUri = SwitchBrowserChallenge.constructFromRedirectUri(redirectUri)?.uri
        Assert.assertNull(switchBrowserProcessUri)
    }

    @Test
    fun `test constructFromRedirectUri with missing action uri`() {
        val redirectString = "${Broker.NEW_BROKER_REDIRECT_URI}?" +
                "${AuthenticationConstants.SWITCH_BROWSER.CODE}=$CODE&" +
                "${AuthenticationConstants.SWITCH_BROWSER.ACTION}=$ACTION"
        val redirectUri = Uri.parse(redirectString)

        val switchBrowserProcessUri = SwitchBrowserChallenge.constructFromRedirectUri(redirectUri)?.uri
        Assert.assertNull(switchBrowserProcessUri)
    }
}
