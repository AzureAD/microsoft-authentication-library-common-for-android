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

import android.app.Activity
import android.content.Context
import android.content.Intent
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.ui.browser.AndroidBrowserSelector
import com.microsoft.identity.common.internal.ui.browser.CustomTabsManager
import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl
import com.microsoft.identity.common.java.browser.IBrowserSelector
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.ui.BrowserDescriptor
import com.microsoft.identity.common.logging.Logger

/**
 * SwitchBrowserRequestHandler is a challenge handler for SwitchBrowserRequestChallenge.
 * It handles the challenge by selecting a valid browser to launch the Switch browser URI.
 */
class SwitchBrowserRequestHandler(
    private val activity: Activity,
    private val context: Context,
    private val customTabsManager: CustomTabsManager,
    private val browserSelector: IBrowserSelector
) : IChallengeHandler<SwitchBrowserRequestChallenge, Unit> {


    var isChallengeHandled: Boolean = false
        private set

    companion object {
        private val TAG = SwitchBrowserRequestHandler::class.simpleName
    }

    constructor(activity: Activity) : this(
        activity,
        activity.applicationContext,
        CustomTabsManager(activity.applicationContext),
        AndroidBrowserSelector(activity.applicationContext)
    )

    /**
     * Process the SwitchBrowserRequestChallenge, which is a request to switch the browser.
     * This method will select a valid browser to launch the challenge URI.
     *
     * @param switchBrowserRequestChallenge challenge request
     * @return true if the challenge is handled successfully, false otherwise.
     */
    @Throws(ClientException::class)
    override fun processChallenge(switchBrowserRequestChallenge: SwitchBrowserRequestChallenge) {
        val methodTag = "$TAG:processChallenge"

        // Select a browser to handle the switch browser challenge
        val browser = browserSelector.selectBrowser(
            BrowserDescriptor.getBrowserSafeListForSwitchBrowser(),
            null
        )
        if (browser == null) {
            val exception = ClientException(
                ClientException.NO_BROWSERS_AVAILABLE,
                "No browser found for SwitchBrowserRequestChallenge."
            )
            Logger.error(
                methodTag,
                "No browser found for SwitchBrowserRequestChallenge.",
                exception
            )
            throw exception
        }

        // Create an intent to launch the browser
        val browserIntent: Intent
        if (browser.isCustomTabsServiceSupported) {
            Logger.info(methodTag, "CustomTabsService is supported.")
            //create customTabsIntent
            if (!customTabsManager.bind(context, browser.packageName)) {
                Logger.warn(methodTag, "Failed to bind CustomTabsService.")
                browserIntent = Intent(Intent.ACTION_VIEW)
            } else {
                browserIntent = customTabsManager.customTabsIntent.intent
            }
        } else {
            Logger.warn(methodTag, "CustomTabsService is NOT supported")
            browserIntent = Intent(Intent.ACTION_VIEW)
        }
        Logger.info(
            methodTag,
            "Launching switch browser request on browser: ${browser.packageName}"
        )
        browserIntent.setPackage(browser.packageName)
        browserIntent.setData(switchBrowserRequestChallenge.uri)
        activity.startActivity(browserIntent)
        isChallengeHandled = true
    }

    /**
     * Check if the request is to start the switch the browser flow.
     *
     * The request is considered "switch_browser" if the URL
     * starts with the following pattern: {redirectUrl}/switch_browser
     *
     *
     * @param url The URL to be checked.
     * @param redirectUrl The redirect URL to be checked against.
     * @return True if the request matches the pattern, false otherwise.
     */
    fun isSwitchBrowserRequest(url: String?, redirectUrl: String): Boolean {
        return isSwitchBrowserRedirectUrl(url, redirectUrl, SWITCH_BROWSER.REQUEST_PATH)
    }

    /**
     * Handles the necessary cleanup after a challenge is completed.
     * This includes unbinding the custom tabs manager and resetting the challenge handled flag.
     */
    fun resetChallengeState() {
        // Unbind the custom tabs manager
        customTabsManager.unbind()
        // Reset the challenge handled flag
        isChallengeHandled = false

    }

}
