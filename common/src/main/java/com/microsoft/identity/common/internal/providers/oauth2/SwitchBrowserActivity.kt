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
import androidx.browser.customtabs.CustomTabsIntent
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.logging.Logger
import androidx.core.net.toUri


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
 * 3. Activity launches the specified browser (Custom Tabs or standard browser)
 * 4. User completes authentication in the external browser
 * 5. BrokerBrowserRedirectActivity is launched when the redirect URI is triggered.
 * 5. BrokerBrowserRedirectActivity redirects back to this activity via onNewIntent()
 * 6. Activity passes the result back to WebViewAuthorizationFragment
 * 7. Activity finishes and removes itself from the task stack
 *
 * Activity back stack behavior:
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
class SwitchBrowserActivity: FragmentActivity() {

    companion object {
        private val TAG: String = SwitchBrowserActivity::class.java.simpleName

        /** Intent extra key for the target browser package name */
        const val BROWSER_PACKAGE_NAME = "browser_package_name"

        /** Intent extra key indicating if the browser supports Custom Tabs */
        const val BROWSER_SUPPORTS_CUSTOM_TABS = "browser_supports_custom_tabs"

        /** Intent extra key for the URI to process in the browser */
        const val PROCESS_URI = "process_uri"
    }

    /**
     * Initializes the activity and launches the appropriate browser for DUNA authentication.
     *
     * This method extracts the browser configuration from intent extras and launches either
     * a Custom Tabs intent or a standard browser intent based on browser capabilities.
     *
     * @param savedInstanceState Saved instance state bundle (unused in this implementation)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val methodTag = "$TAG:onCreate"
        super.onCreate(savedInstanceState)

        // Extract configuration parameters from intent extras
        val extras = this.intent.extras ?: Bundle()
        val browserPackageName = extras.getString(BROWSER_PACKAGE_NAME)
        val browserSupportsCustomTabs = extras.getBoolean(BROWSER_SUPPORTS_CUSTOM_TABS, false)
        val processUri = extras.getString(PROCESS_URI)

        // Validate required parameters
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

        Logger.info(
            methodTag,
            "Launching switch browser request on browser: $browserPackageName, Custom Tabs supported: $browserSupportsCustomTabs"
        )

        // Launch browser based on Custom Tabs support
        if (browserSupportsCustomTabs) {
            // Use Custom Tabs for better user experience and security
            Logger.info(methodTag, "Launching Custom Tabs intent for DUNA authentication")
            val customTabsIntent = CustomTabsIntent.Builder().build().apply {
                intent.setPackage(browserPackageName)
            }
            customTabsIntent.launchUrl(this, processUri.toUri())
        } else {
            // Fallback to standard browser intent
            Logger.info(methodTag, "Launching standard browser intent for DUNA authentication")
            val browserIntent = Intent(Intent.ACTION_VIEW).apply {
                data = processUri.toUri()
                setPackage(browserPackageName)
            }
            startActivity(browserIntent)
        }
    }

    /**
     * Handles the redirect back from the browser after DUNA authentication completion.
     *
     * This method is called when the browser redirects back to the app with the authentication
     * result. The intent contains the authentication response which is passed back to the
     * WebViewAuthorizationFragment for processing.
     *
     * **Important:** This method also finishes the activity and removes it from the task stack
     * to prevent it from remaining in the back stack after the authentication flow completes.
     *
     * @param intent The intent containing the authentication result from the browser redirect
     */
    override fun onNewIntent(intent: Intent?) {
        val methodTag = "$TAG:onNewIntent"
        super.onNewIntent(intent)
        // Update the activity's intent with the new intent containing the auth result
        setIntent(intent)

        // Pass the authentication result back to WebViewAuthorizationFragment
        Logger.info(methodTag, "Passing authentication result back to WebViewAuthorizationFragment")
        if (intent != null) {
            WebViewAuthorizationFragment.setSwitchBrowserBundle(intent.extras)
        } else {
            Logger.error(methodTag, "Received null intent in onNewIntent - Cannot pass result back", null)
        }

        // Clean up: finish this activity and remove it from task stack
        Logger.info(methodTag, "Browser switch complete - Finishing activity and removing from task stack")
        finishAndRemoveTask()
    }
}
