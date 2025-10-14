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
import androidx.activity.result.ActivityResultLauncher
import androidx.browser.auth.AuthTabIntent
import androidx.browser.customtabs.CustomTabsClient
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.internal.ui.browser.CustomTabsManager
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator
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
class SwitchBrowserActivity : FragmentActivity() {

    // Flag to track if a Custom Chrome Tab (CCT) has been launched
    private var cctLaunched = false
    private var customTabsManager = CustomTabsManager(this)
    private lateinit var mLauncher: ActivityResultLauncher<Intent>

    companion object {
        private val TAG: String = SwitchBrowserActivity::class.java.simpleName

        /** Intent extra key for the target browser package name */
        const val BROWSER_PACKAGE_NAME = "browser_package_name"

        /** Intent extra key indicating if the browser supports Custom Tabs */
        const val BROWSER_SUPPORTS_CUSTOM_TABS = "browser_supports_custom_tabs"

        /** Intent extra key for the URI to process in the browser */
        const val PROCESS_URI = "process_uri"

        /** Intent extra key for the redirect URI */
        const val REDIRECT_URI = "redirect_uri"

        /** Intent extra key indicating a resume request from the browser redirect */
        const val RESUME_REQUEST = "resume_request"
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
        Logger.info(methodTag, "SwitchBrowserActivity created - Launching browser")
        mLauncher = AuthTabIntent.registerActivityResultLauncher(this) { handleAuthResult(it) }
        launchBrowser()
    }


    /**
     * Launches the specified browser for DUNA authentication based on intent extras.
     *
     * This method reads the target browser package name, Custom Tabs support flag,
     * and the process URI from the intent extras. It then constructs and launches
     * either a Custom Tabs intent or a standard browser intent accordingly.
     *
     * If required parameters are missing, it logs an error and finishes the activity.
     */
    private fun launchBrowser() {
        val methodTag = "$TAG:launchBrowser"
        cctLaunched = false
        // Extract configuration parameters from intent extras
        val extras = this.intent.extras ?: Bundle()
        val browserPackageName = extras.getString(BROWSER_PACKAGE_NAME)
        val browserSupportsCustomTabs = extras.getBoolean(BROWSER_SUPPORTS_CUSTOM_TABS, false)
        val processUri = extras.getString(PROCESS_URI)
        val redirectUri = extras.getString(REDIRECT_URI)

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

        if (redirectUri.isNullOrBlank()) {
            Logger.error(methodTag, "No redirect URI found in extras - Cannot proceed with browser switch", null)
            finish()
            return
        }

        Logger.info(
            methodTag,
            "Launching switch browser request on browser: $browserPackageName, Custom Tabs supported: $browserSupportsCustomTabs"
        )

        // Create an intent to launch the browser
        val browserIntent: Intent
        if (CustomTabsClient.isAuthTabSupported(applicationContext, browserPackageName)) {
            Logger.info(methodTag, "AuthTab is supported.")
            launchAuthTab(mLauncher, processUri.toUri(), redirectUri)
            return
        }

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
    override fun onNewIntent(intent: Intent) {
        val methodTag = "$TAG:onNewIntent"
        super.onNewIntent(intent)
        // Update the activity's intent with the new intent containing the auth result
        Logger.info(methodTag, "On new intent received.")
        setIntent(intent)

        if (intent != null) {
            if (intent.hasExtra(PROCESS_URI)) {
                // Handle scenario where a new browser switch request is received while one is already in progress
                // This can occur when the user initiates another auth request before completing the first one.
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
        }
        // Clean up: finish this activity and remove it from task stack
        Logger.info(methodTag, "Unexpected intent - Finishing activity and removing from task stack")
        finishAndRemoveTask()
    }

    /**
     * Handles the activity resume lifecycle event and manages Custom Chrome Tab (CCT) launch state.
     *
     * This method implements a critical part of the browser switch flow by tracking whether a Custom Chrome Tab
     * has been launched and handling the case where the user returns to this activity without completing
     * the authentication flow in the browser.
     *
     * **Behavior Logic:**
     * - On first resume (after onCreate): Sets cctLaunched flag to true and continues normally
     * - On subsequent resumes: If CCT was already launched, assumes user backed out of browser and finishes activity
     *
     * **Why This Logic is Needed:**
     * When a Custom Chrome Tab is launched, this activity goes into the background. If the user presses the back
     * button in the CCT or otherwise returns to this activity without completing authentication, we need to
     * clean up and finish this activity to prevent it from remaining in the back stack.
     *
     * **Flow Scenarios:**
     * 1. **Normal Flow**: onCreate → onResume (1st time) → CCT launched → user completes auth → onNewIntent → finish
     * 2. **User Cancellation**: onCreate → onResume (1st time) → CCT launched → user backs out → onResume (2nd time) → finish
     *
     * **Important Notes:**
     * - This prevents the activity from staying alive indefinitely if authentication is cancelled
     * - Uses finishAndRemoveTask() to clean up the entire task stack, not just this activity
     * - The cctLaunched flag is essential for distinguishing between the initial resume and subsequent resumes
     */
    override fun onResume() {
        super.onResume()
        val methodTag = "$TAG:onResume"
        Logger.info(methodTag, "onResume called - Managing CCT launch state")

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

    private fun handleAuthResult(result: AuthTabIntent.AuthResult) {
        val methodTag = "$TAG:handleAuthResult"

        val message = when (result.resultCode) {
            AuthTabIntent.RESULT_OK -> "Received auth result from AuthTab."
            AuthTabIntent.RESULT_CANCELED -> "AuthTab canceled."
            AuthTabIntent.RESULT_VERIFICATION_FAILED -> "App Link Verification failed."
            AuthTabIntent.RESULT_VERIFICATION_TIMED_OUT -> "App Link Verification timed out."
            else -> "Unknown result code: ${result.resultCode}"
        }

        Logger.info(methodTag, message)

        if (result.resultCode == AuthTabIntent.RESULT_OK) {
            result.resultUri?.let { completeAuth(it) }
        }

        // Clean up: finish this activity and remove it from task stack
        Logger.info(methodTag, "Finishing activity and removing from task stack")
        finishAndRemoveTask()
    }

    private fun completeAuth(uri: Uri) {
        val intent = SwitchBrowserProtocolCoordinator.getIntentToResumeWebViewAuth(applicationContext, uri.toString())
        WebViewAuthorizationFragment.setSwitchBrowserBundle(intent.extras)
    }


    /**
     * Invokes the appropriate AuthTabIntent.launch overload based on redirect URI type.
     */
    private fun launchAuthTab(
        launcher: ActivityResultLauncher<Intent>,
        processUri: Uri,
        redirectUriStr: String?
    ): Boolean {
        val methodTag = "$TAG:launchAuthTab"
        val info = parseRedirectForAuthTab(redirectUriStr)
        if (info == null) {
            Logger.error(methodTag, "Cannot parse redirect URI: $redirectUriStr", null)
            return false
        }
        val authTabIntent = AuthTabIntent.Builder().build()
        return when {
            info.isAppLink && info.host != null && info.path != null -> {
                Logger.info(methodTag, "AuthTab AppLink host=${info.host} path=${info.path}")
                authTabIntent.launch(launcher, processUri, info.host, info.path)
                true
            }
            !info.isAppLink && info.scheme != null -> {
                Logger.info(methodTag, "AuthTab CustomScheme scheme=${info.scheme}")
                authTabIntent.launch(launcher, processUri, info.scheme)
                true
            }
            else -> {
                Logger.warn(methodTag, "Incomplete redirect info: $info")
                false
            }
        }
    }

    // Helper data holder
    data class RedirectHandlingInfo(
        val isAppLink: Boolean,
        val scheme: String?,
        val host: String?,
        val path: String?    // For https: MUST include leading slash exactly as in final redirect
    )

    /**
     * Parses a redirect URI string and returns info needed for AuthTab launch.
     * Custom scheme example: myapp://auth/callback  -> scheme = myapp
     * App Link example: https://example.com/auth/callback?code=123
     *   -> host = example.com, path = /auth/callback
     */
    private fun parseRedirectForAuthTab(redirectUriStr: String?): RedirectHandlingInfo? {
        if (redirectUriStr.isNullOrBlank()) return null
        return try {
            val uri = redirectUriStr.toUri()
            val schemeLower = uri.scheme?.lowercase()
            if (schemeLower == "https") {
                val host = uri.host ?: return null
                val path = uri.path ?: "/"          // uri.path keeps leading slash and is decoded
                if (path.isBlank()) return null
                RedirectHandlingInfo(
                    isAppLink = true,
                    scheme = null,
                    host = host,
                    path = path                      // e.g. "/androidbroker/…/switch_browser_resume"
                )
            } else {
                RedirectHandlingInfo(
                    isAppLink = false,
                    scheme = uri.scheme,
                    host = null,
                    path = null
                )
            }
        } catch (ex: Exception) {
            Logger.warn("$TAG:parseRedirectForAuthTab", "Parse error: ${ex.message}")
            null
        }
    }
}
