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

import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.logging.Logger

/**
 * Activity responsible for handling browser switching flows.
 *
 * This activity serves as an intermediary between the WebView-based authentication and external browser
 * authentication. When a Switch Browser challenge is received in [WebViewAuthorizationFragment], this activity
 * is launched to handle the browser switch operation.
 *
 * **Flow Overview:**
 * 1. WebViewAuthorizationFragment receives a SwitchBrowser challenge
 * 2. This activity is launched with browser configuration parameters
 * 3. Activity selects a [BrowserLaunchStrategy] and delegates launch to it
 * 4. User completes authentication in the external browser
 * 5. BrokerBrowserRedirectActivity is launched when the redirect URI is triggered (Custom Tabs path).
 * 6. Activity passes the result back to WebViewAuthorizationFragment
 * 7. Activity finishes and removes itself from the task stack
 *
 * Activity back stack behavior (Custom Tabs path):
 * 1 BrokerAuthorizationActivity hosting WebViewAuthorizationFragment --launches--> SwitchBrowserActivity in a new task.
 * 2 SwitchBrowserActivity --launches--> 3rd Party Browser (Custom Tabs or standard browser) in current task.
 * 3 3rd Party Browser --redirects to--> BrokerBrowserRedirectActivity in a new task.
 * 4 BrokerBrowserRedirectActivity -- launches--> SwitchBrowserActivity in the existing task, and finishes current task.
 * 5 SwitchBrowserActivity --passes result to--> WebViewAuthorizationFragment, and finishes current activity stack.
 *
 * **Security Note:** This activity is not exported and can only be launched within the app
 * to prevent external apps from triggering unwanted browser switches.
 *
 * @see WebViewAuthorizationFragment
 */
class SwitchBrowserActivity : FragmentActivity() {

    private lateinit var launchStrategy: BrowserLaunchStrategy

    /**
     * Flag used by the Custom Tabs cancellation heuristic.
     */
    private var cctLaunched = false

    companion object {
        private val TAG: String = SwitchBrowserActivity::class.java.simpleName

        /** Intent extra key for the target browser package name */
        const val BROWSER_PACKAGE_NAME = "browser_package_name"

        /** Intent extra key indicating if the browser supports Custom Tabs */
        const val BROWSER_SUPPORTS_CUSTOM_TABS = "browser_supports_custom_tabs"

        /** Intent extra key for the URI to process in the browser */
        const val PROCESS_URI = "process_uri"

        /** Intent extra key indicating a resume request from the browser redirect */
        const val RESUME_REQUEST = "resume_request"

        /**
         * Optional intent extra used by broker-owned Auth Tab strategies.
         */
        const val REDIRECT_SCHEME = "redirect_scheme"
    }

    /**
     * Initializes the activity, selects the appropriate [BrowserLaunchStrategy], and launches
     * the browser.
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val methodTag = "$TAG:onCreate"
        Logger.info(methodTag, "SwitchBrowserActivity created - Selecting launch strategy")

        if (::launchStrategy.isInitialized) {
            launchStrategy.cleanup()
        }

        val browserPackageName = intent?.extras?.getString(BROWSER_PACKAGE_NAME) ?: ""
        val shouldUseAuthTab = isAuthTabFlightEnabled() &&
            AuthTabStrategyProvider.isAvailable() &&
            AuthTabStrategyProvider.isAuthTabSupported(this, browserPackageName)

        launchStrategy = if (shouldUseAuthTab) {
            Logger.info(methodTag, "Auth Tab is supported and flight is enabled — requesting strategy from provider")
            AuthTabStrategyProvider.createStrategy(this, ::handleSwitchBrowserResume)
                ?: run {
                    Logger.warn(methodTag, "Auth Tab strategy provider returned null, falling back to Custom Tabs")
                    CustomTabsLaunchStrategy(this)
                }
        } else {
            Logger.info(methodTag, "Using CustomTabsLaunchStrategy")
            CustomTabsLaunchStrategy(this)
        }

        launchBrowser()
    }

    /**
     * Launches the browser switch flow using the selected strategy.
     */
    private fun launchBrowser() {
        val methodTag = "$TAG:launchBrowser"
        cctLaunched = false
        val extras = this.intent?.extras ?: Bundle()
        val browserPackageName = extras.getString(BROWSER_PACKAGE_NAME)
        val processUri = extras.getString(PROCESS_URI)

        if (browserPackageName.isNullOrBlank()) {
            Logger.error(methodTag, "No browser package name found in extras - Cannot proceed with browser switch", null)
            finish()
            return
        }
        if (processUri.isNullOrBlank()) {
            Logger.error(methodTag, "No process URI found in extras - Cannot proceed with browser switch", null)
            finish()
            return
        }

        Logger.info(methodTag, "Launching switch browser request on browser: $browserPackageName")
        launchStrategy.launch(processUri.toUri(), browserPackageName)
    }

    /**
     * Handles a browser resume result and forwards it to [WebViewAuthorizationFragment].
     */
    private fun handleSwitchBrowserResume(bundle: Bundle?) {
        val methodTag = "$TAG:handleSwitchBrowserResume"
        Logger.info(methodTag, "Handling switch browser resume, bundle present: ${bundle != null}")
        WebViewAuthorizationFragment.setSwitchBrowserBundle(bundle)
        finishAndRemoveTask()
    }

    /**
     * Handles new intents received while this activity is alive.
     */
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        val methodTag = "$TAG:onNewIntent"
        Logger.info(methodTag, "On new intent received.")

        if (!::launchStrategy.isInitialized) {
            Logger.warn(methodTag, "launchStrategy not initialized - ignoring new intent")
            return
        }

        if (!launchStrategy.handlesCancellationOnResume()) {
            return
        }

        if (intent == null) {
            Logger.warn(methodTag, "Received null intent - Finishing activity")
            finishAndRemoveTask()
            return
        }

        setIntent(intent)

        if (intent.hasExtra(PROCESS_URI)) {
            Logger.warn(
                methodTag,
                "Received new switch browser request while one is already in progress - Restarting browser switch flow"
            )
            launchBrowser()
            return
        }

        if (intent.hasExtra(RESUME_REQUEST)) {
            handleSwitchBrowserResume(intent.extras)
            return
        }

        Logger.info(methodTag, "Unexpected intent - Finishing activity and removing from task stack")
        finishAndRemoveTask()
    }

    /**
     * Handles cancellation heuristic for strategies that rely on [onResume].
     */
    override fun onResume() {
        super.onResume()
        val methodTag = "$TAG:onResume"
        Logger.info(methodTag, "onResume called - Managing CCT launch state")

        if (!::launchStrategy.isInitialized) {
            return
        }

        if (!launchStrategy.handlesCancellationOnResume()) {
            return
        }

        if (cctLaunched) {
            Logger.info(methodTag, "CCT was launched previously and user returned - Assuming cancellation, finishing activity")
            finishAndRemoveTask()
        } else {
            Logger.info(methodTag, "First resume after onCreate - Marking CCT as launched")
        }

        cctLaunched = true
    }

    override fun onDestroy() {
        super.onDestroy()
        if (::launchStrategy.isInitialized) {
            launchStrategy.cleanup()
        }
    }

    /**
     * Returns `true` when the Auth Tab flight is enabled, or `false` on any check failure.
     */
    private fun isAuthTabFlightEnabled(): Boolean {
        return try {
            CommonFlightsManager.getFlightsProvider()
                .isFlightEnabled(CommonFlight.ENABLE_AUTH_TAB_FOR_SWITCH_BROWSER)
        } catch (e: Exception) {
            Logger.warn("$TAG:isAuthTabFlightEnabled", "Exception checking Auth Tab flight: ${e.message}")
            false
        }
    }

    /**
     * Visible for unit tests only.
     */
    internal fun getLaunchStrategyForTest(): BrowserLaunchStrategy = launchStrategy
}
