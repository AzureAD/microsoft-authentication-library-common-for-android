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
import android.net.Uri
import android.os.Bundle
import androidx.activity.result.ActivityResultCaller
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.ui.browser.CustomTabsManager
import com.microsoft.identity.common.internal.ui.browser.authtab.AuthTabSupport
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.logging.Logger
import androidx.core.net.toUri
import androidx.core.os.bundleOf


/**
 * Activity responsible for handling browser switching flows.
 *
 * This activity serves as an intermediary between the WebView-based authentication and external browser
 * authentication. When a Switch Browser challenge is received in [WebViewAuthorizationFragment], this activity
 * is launched to handle the browser switch operation.
 *
 * **Auth Tab Support (Chrome 137+):**
 * When Auth Tab is available and enabled via flight, this activity will use the simplified Auth Tab API
 * instead of Custom Tabs. Auth Tab provides:
 * - Simplified callback mechanism (no intent filters needed)
 * - Enhanced security with direct data transfer
 * - Streamlined browser UI focused on authentication
 *
 * **Flow Overview:**
 * 1. WebViewAuthorizationFragment receives a SwitchBrowser challenge
 * 2. This activity is launched with browser configuration parameters
 * 3. Activity launches the specified browser (Auth Tab, Custom Tabs, or standard browser)
 * 4. User completes authentication in the external browser
 * 5. For Auth Tab: Result comes via ActivityResultCallback
 *    For Custom Tabs: BrokerBrowserRedirectActivity redirects back via onNewIntent()
 * 6. Activity passes the result back to WebViewAuthorizationFragment
 * 7. Activity finishes and removes itself from the task stack
 *
 * **Security Note:** This activity is not exported and can only be launched within the app
 * to prevent external apps from triggering unwanted browser switches.
 *
 * @see WebViewAuthorizationFragment
 */
class SwitchBrowserActivity : FragmentActivity() {

    // Flag to track if a Custom Chrome Tab (CCT) has been launched
    private var cctLaunched = false
    private var customTabsManager = CustomTabsManager(this)
    
    // Auth Tab support
    private var authTabLauncher: ActivityResultLauncher<Intent>? = null
    private var usingAuthTab = false

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
        
        /** Intent extra key for the redirect scheme used with Auth Tab */
        const val REDIRECT_SCHEME = "redirect_scheme"
    }

    /**
     * Initializes the activity and launches the appropriate browser for DUNA authentication.
     *
     * This method extracts the browser configuration from intent extras and launches either
     * Auth Tab (if supported), Custom Tabs, or a standard browser intent based on browser capabilities.
     *
     * @param savedInstanceState Saved instance state bundle (unused in this implementation)
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        val methodTag = "$TAG:onCreate"
        super.onCreate(savedInstanceState)
        Logger.info(methodTag, "SwitchBrowserActivity created")
        
        // Register Auth Tab launcher if supported
        registerAuthTabLauncherIfSupported()
        
        Logger.info(methodTag, "Launching browser")
        launchBrowser()
    }
    
    /**
     * Register the Auth Tab activity result launcher if Auth Tab is available.
     * This must be called before the activity reaches STARTED state.
     */
    private fun registerAuthTabLauncherIfSupported() {
        val methodTag = "$TAG:registerAuthTabLauncherIfSupported"
        
        if (!AuthTabSupport.isAuthTabAvailable(this)) {
            Logger.info(methodTag, "Auth Tab not available - will use Custom Tabs")
            return
        }
        
        val browserPackage = intent.extras?.getString(BROWSER_PACKAGE_NAME)
        if (browserPackage != null && !AuthTabSupport.isAuthTabSupportedByBrowser(this, browserPackage)) {
            Logger.info(methodTag, "Auth Tab not supported by browser: $browserPackage - will use Custom Tabs")
            return
        }
        
        try {
            authTabLauncher = AuthTabIntent.registerActivityResultLauncher(
                this as ActivityResultCaller
            ) { result ->
                handleAuthTabResult(result)
            }
            Logger.info(methodTag, "Auth Tab launcher registered successfully")
        } catch (e: Exception) {
            Logger.warn(methodTag, "Failed to register Auth Tab launcher: ${e.message}")
            authTabLauncher = null
        }
    }


    /**
     * Launches the specified browser for DUNA authentication based on intent extras.
     *
     * This method reads the target browser package name, Custom Tabs support flag,
     * and the process URI from the intent extras. It then constructs and launches
     * Auth Tab (if available), Custom Tabs, or a standard browser intent accordingly.
     *
     * If required parameters are missing, it logs an error and finishes the activity.
     */
    private fun launchBrowser() {
        val methodTag = "$TAG:launchBrowser"
        cctLaunched = false
        usingAuthTab = false
        
        // Extract configuration parameters from intent extras
        val extras = this.intent.extras ?: Bundle()
        val browserPackageName = extras.getString(BROWSER_PACKAGE_NAME)
        val browserSupportsCustomTabs = extras.getBoolean(BROWSER_SUPPORTS_CUSTOM_TABS, false)
        val processUri = extras.getString(PROCESS_URI)
        val redirectScheme = extras.getString(REDIRECT_SCHEME)

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

        // Try Auth Tab first if available and launcher is registered
        if (authTabLauncher != null && !redirectScheme.isNullOrBlank()) {
            Logger.info(methodTag, "Using Auth Tab for switch browser flow")
            launchWithAuthTab(processUri, redirectScheme)
            return
        }

        // Fall back to Custom Tabs or standard browser
        launchWithCustomTabsOrBrowser(browserPackageName, browserSupportsCustomTabs, processUri)
    }
    
    /**
     * Launch the browser using Auth Tab API.
     */
    private fun launchWithAuthTab(processUri: String, redirectScheme: String) {
        val methodTag = "$TAG:launchWithAuthTab"
        try {
            usingAuthTab = true
            val authTabIntent = AuthTabIntent.Builder().build()
            authTabIntent.launch(authTabLauncher!!, Uri.parse(processUri), redirectScheme)
            Logger.info(methodTag, "Auth Tab launched successfully")
        } catch (e: Exception) {
            Logger.error(methodTag, "Failed to launch Auth Tab, falling back to Custom Tabs", e)
            usingAuthTab = false
            val extras = intent.extras ?: Bundle()
            val browserPackageName = extras.getString(BROWSER_PACKAGE_NAME) ?: return
            val browserSupportsCustomTabs = extras.getBoolean(BROWSER_SUPPORTS_CUSTOM_TABS, false)
            launchWithCustomTabsOrBrowser(browserPackageName, browserSupportsCustomTabs, processUri)
        }
    }
    
    /**
     * Launch the browser using Custom Tabs or standard browser intent.
     */
    private fun launchWithCustomTabsOrBrowser(
        browserPackageName: String,
        browserSupportsCustomTabs: Boolean,
        processUri: String
    ) {
        val methodTag = "$TAG:launchWithCustomTabsOrBrowser"
        
        // Create an intent to launch the browser
        val browserIntent: Intent
        if (browserSupportsCustomTabs) {
            Logger.info(methodTag, "CustomTabsService is supported.")
            //create customTabsIntent
            if (!customTabsManager.bind(this, browserPackageName)) {
                Logger.warn(methodTag, "Failed to bind CustomTabsService.")
                browserIntent = Intent(Intent.ACTION_VIEW)
            } else {
                browserIntent = customTabsManager.customTabsIntent.intent
            }
        } else {
            Logger.warn(methodTag, "CustomTabsService is NOT supported")
            browserIntent = Intent(Intent.ACTION_VIEW)
            browserIntent.setPackage(browserPackageName)
        }
        browserIntent.setData(processUri.toUri())
        startActivity(browserIntent)
    }
    
    /**
     * Handle the result from Auth Tab.
     * Converts the result to the expected format and passes it to WebViewAuthorizationFragment.
     */
    private fun handleAuthTabResult(result: AuthTabIntent.AuthResult) {
        val methodTag = "$TAG:handleAuthTabResult"
        Logger.info(methodTag, "Auth Tab result received with code: ${result.resultCode}")
        
        when (result.resultCode) {
            AuthTabIntent.RESULT_OK -> {
                val resultUri = result.resultUri
                if (resultUri != null) {
                    Logger.info(methodTag, "Auth Tab completed successfully")
                    // Extract the switch browser resume parameters from the result URI
                    val actionUri = resultUri.getQueryParameter(SWITCH_BROWSER.ACTION_URI)
                    val code = resultUri.getQueryParameter(SWITCH_BROWSER.CODE)
                    val state = resultUri.getQueryParameter(SWITCH_BROWSER.STATE)
                    
                    val bundle = bundleOf(
                        SWITCH_BROWSER.ACTION_URI to actionUri,
                        SWITCH_BROWSER.CODE to code,
                        SWITCH_BROWSER.STATE to state,
                        RESUME_REQUEST to true
                    )
                    WebViewAuthorizationFragment.setSwitchBrowserBundle(bundle)
                } else {
                    Logger.warn(methodTag, "Auth Tab returned OK but with null URI")
                }
            }
            AuthTabIntent.RESULT_CANCELED -> {
                Logger.info(methodTag, "Auth Tab was cancelled by user")
                // User cancelled - no bundle to set
            }
            AuthTabIntent.RESULT_VERIFICATION_FAILED -> {
                Logger.warn(methodTag, "Auth Tab verification failed")
            }
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> {
                Logger.warn(methodTag, "Auth Tab verification timed out")
            }
            else -> {
                Logger.warn(methodTag, "Auth Tab returned unknown result code: ${result.resultCode}")
            }
        }
        
        // Clean up: finish this activity and remove it from task stack
        Logger.info(methodTag, "Finishing activity after Auth Tab result")
        finishAndRemoveTask()
    }

    /**
     * Handles the redirect back from the browser after DUNA authentication completion.
     *
     * This method is called when the browser redirects back to the app with the authentication
     * result (for Custom Tabs flow - Auth Tab uses callback instead).
     *
     * @param intent The intent containing the authentication result from the browser redirect
     */
    override fun onNewIntent(intent: Intent) {
        val methodTag = "$TAG:onNewIntent"
        super.onNewIntent(intent)
        // Update the activity's intent with the new intent containing the auth result
        Logger.info(methodTag, "On new intent received.")
        setIntent(intent)

        if (intent.hasExtra(PROCESS_URI)) {
            // Handle scenario where a new browser switch request is received while one is already in progress
            Logger.warn(
                methodTag,
                "Received new switch browser request while one is already in progress" +
                    " - Restarting browser switch flow"
            )
            // Launch the new browser request, which will reset cctLaunched and start fresh
            launchBrowser()
            return
        }
        if (intent.hasExtra(RESUME_REQUEST)) {
            WebViewAuthorizationFragment.setSwitchBrowserBundle(intent.extras)
            // Clean up: finish this activity and remove it from task stack
            Logger.info(methodTag, "Finishing activity and removing from task stack")
            finishAndRemoveTask()
            return
        }
        // Clean up: finish this activity and remove it from task stack
        Logger.info(methodTag, "Unexpected intent - Finishing activity and removing from task stack")
        finishAndRemoveTask()
    }

    /**
     * Handles the activity resume lifecycle event and manages browser launch state.
     *
     * For Auth Tab: Results come via callback, so we don't need to handle resume specially.
     * For Custom Tabs: Track whether CCT was launched and handle user cancellation.
     */
    override fun onResume() {
        super.onResume()
        val methodTag = "$TAG:onResume"
        Logger.info(methodTag, "onResume called")
        
        // If using Auth Tab, results come via callback - don't finish on resume
        if (usingAuthTab) {
            Logger.info(methodTag, "Using Auth Tab - waiting for callback result")
            return
        }

        // Custom Tabs flow: track launch state for cancellation detection
        Logger.info(methodTag, "Managing CCT launch state")
        if (cctLaunched) {
            // User has returned to this activity after CCT was launched, likely due to backing out
            Logger.info(methodTag, "CCT was launched previously and user returned - Assuming cancellation, finishing activity")
            finishAndRemoveTask()
        } else {
            // First resume after onCreate - mark CCT as launched for future reference
            Logger.info(methodTag, "First resume after onCreate - Marking CCT as launched")
        }

        cctLaunched = true
    }

    override fun onDestroy() {
        super.onDestroy()
        customTabsManager.unbind()
    }
}
