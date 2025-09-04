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
import android.content.Intent
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.providers.oauth2.SwitchBrowserActivity
import com.microsoft.identity.common.internal.ui.browser.AndroidBrowserSelector
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserUriHelper
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl
import com.microsoft.identity.common.java.browser.IBrowserSelector
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.java.ui.BrowserDescriptor
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode

/**
 * SwitchBrowserRequestHandler is a challenge handler for SwitchBrowserChallenge.
 * It handles the challenge by selecting a valid browser to launch the Switch browser URI.
 */
class SwitchBrowserRequestHandler(
    private val activity: Activity,
    private val browserSelector: IBrowserSelector,
    private val spanContext: SpanContext?
) : IChallengeHandler<SwitchBrowserChallenge, Unit> {

    val span: Span by lazy {
        OTelUtility.createSpanFromParent(SpanName.SwitchBrowserProcess.name, spanContext)
    }

    var isSwitchBrowserChallengeActive: Boolean = false

    companion object {
        private val TAG = SwitchBrowserRequestHandler::class.simpleName
    }

    constructor(activity: Activity, spanContext: SpanContext?) : this(
        activity,
        AndroidBrowserSelector(activity.applicationContext),
        spanContext
    )

    /**
     * Process the SwitchBrowserChallenge, which is a request to switch the browser.
     * This method will select a valid browser to launch the challenge URI.
     *
     * @param switchBrowserChallenge challenge request
     * @return true if the challenge is handled successfully, false otherwise.
     */
    @Throws(ClientException::class)
    override fun processChallenge(switchBrowserChallenge: SwitchBrowserChallenge) {
        SpanExtension.makeCurrentSpan(span).use {
            val methodTag = "$TAG:processChallenge"
            
            val state = switchBrowserChallenge.processUri.getQueryParameter(SWITCH_BROWSER.STATE)
            SwitchBrowserUriHelper.statesMatch(switchBrowserChallenge.authorizationUrl, state)

            // Select a browser to handle the switch browser challenge
            val browser = browserSelector.selectBrowser(
                BrowserDescriptor.getBrowserSafeListForSwitchBrowser(),
                null
            )
            if (browser == null) {
                val exception = ClientException(
                    ClientException.NO_BROWSERS_AVAILABLE,
                    "No browser found for SwitchBrowserChallenge."
                )
                Logger.error(
                    methodTag,
                    "No browser found for SwitchBrowserChallenge.",
                    exception
                )
                span.setStatus(StatusCode.ERROR)
                span.recordException(exception)
                span.end()
                throw exception
            }
            span.setAttribute(
                AttributeName.browser_package_name.name,
                browser.packageName
            )
            span.setAttribute(
                AttributeName.is_custom_tabs_supported.name,
                browser.isCustomTabsServiceSupported
            )
            val switchBrowserIntent = Intent(activity, SwitchBrowserActivity::class.java).apply {
                putExtra(SwitchBrowserActivity.BROWSER_PACKAGE_NAME, browser.packageName)
                putExtra(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, browser.isCustomTabsServiceSupported)
                putExtra(SwitchBrowserActivity.PROCESS_URI, switchBrowserChallenge.processUri.toString())
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            activity.startActivity(switchBrowserIntent)
            span.setStatus(StatusCode.OK)
            isSwitchBrowserChallengeActive = true
            span.end()
        }
    }

    /**
     *  Check if the request is to start the switch browser flow.
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
     * Reset the challenge state.
     * This method is called after processing the switch browser resume action.
     */
    fun resetChallengeState() {
        isSwitchBrowserChallengeActive = false
    }
}
