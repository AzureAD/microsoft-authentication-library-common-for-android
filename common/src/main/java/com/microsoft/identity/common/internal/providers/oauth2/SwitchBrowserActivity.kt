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

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.core.net.toUri
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserUriHelper
import com.microsoft.identity.common.java.exception.BaseException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

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
 * 5. SwitchBrowserRedirectActivity (or subclass) is launched when the redirect URI is triggered.
 * 5. SwitchBrowserRedirectActivity redirects back to this activity via onNewIntent()
 * 6. Activity passes the result back to WebViewAuthorizationFragment
 * 7. Activity finishes and removes itself from the task stack
 *
 * Activity back stack behavior:
 * 1 AuthorizationActivity hosting WebViewAuthorizationFragment --launches--> SwitchBrowserActivity in a new task.
 * 2 SwitchBrowserActivity --launches--> 3rd Party Browser (Custom Tabs or standard browser) in current task.
 * 3 3rd Party Browser --redirects to--> SwitchBrowserRedirectActivity (or subclass) in a new task.
 * 4 SwitchBrowserRedirectActivity -- launches--> SwitchBrowserActivity in the existing task, and finishes current task.
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
    private var launchStrategy: BrowserLaunchStrategy? = null
    private var span: Span? = null

    // Scope returned by SpanExtension.makeCurrentSpan(...). Must be closed to avoid
    // leaking the span into thread-local storage beyond this activity's lifetime,
    // which would contaminate later traces on the same thread.
    private var spanScope: io.opentelemetry.context.Scope? = null

    companion object {
        private val TAG: String = SwitchBrowserActivity::class.java.simpleName

        /** Intent extra key for the target browser package name */
        const val BROWSER_PACKAGE_NAME = "browser_package_name"

        /** Intent extra key for the redirect URI to use for the switch-browser callback */
        const val REDIRECT_URI = "redirect_uri"

        /** Intent extra key indicating if the browser supports Custom Tabs */
        const val BROWSER_SUPPORTS_CUSTOM_TABS = "browser_supports_custom_tabs"

        /** Intent extra key for the URI to process in the browser */
        const val PROCESS_URI = "process_uri"

        /**
         * Builds an [Intent] targeting [SwitchBrowserActivity] to resume the switch-browser flow
         * in the WebView.
         *
         * Parses [intentDataString] as a resume redirect URI and extracts the authentication response
         * parameters ([SWITCH_BROWSER.ACTION_URI], [SWITCH_BROWSER.CODE], [SWITCH_BROWSER.STATE]).
         * Also sets [SWITCH_BROWSER.RESUME_REQUEST] to `true` so that [onNewIntent] can identify this as a resume delivery.
         *
         * @param context           Application or activity context used to build the intent.
         * @param intentDataString  The full resume redirect URI string received from the browser,
         *                          e.g. `msauth://com.microsoft.identity.client/switch_browser_resume?code=…&action_uri=…&state=…`
         * @return A configured [Intent] ready to be delivered to [SwitchBrowserActivity].
         */
        @JvmStatic
        fun buildSwitchBrowserResumeIntent(context: Context, intentDataString: String): Intent {
            val uri = intentDataString.toUri()
            val bundle = SwitchBrowserUriHelper.extractSwitchBrowserResumeParamsAsBundle(uri)
            val intent = Intent(context, SwitchBrowserActivity::class.java)
            intent.putExtras(bundle)
            return intent
        }

        /**
         * Builds an [Intent] to start [SwitchBrowserActivity] for launching a browser-based
         * switch-browser authentication flow.
         *
         * The intent includes all parameters needed to identify the target browser, the URI to
         * process, and the span context for distributed tracing continuity across the activity
         * boundary.
         *
         * @param context                   Application or activity context used to build the intent.
         * @param redirectUri               The redirect URI used for the switch-browser callback.
         * @param browserPackageName         The package name of the browser to launch.
         * @param browserSupportsCustomTabs  Whether the target browser supports Custom Tabs.
         * @param processUri                 The URI to open in the browser for authentication.
         * @param spanContext                The serializable span context for trace propagation, or `null`.
         * @return A configured [Intent] with [Intent.FLAG_ACTIVITY_NEW_TASK] set.
         */
        @JvmStatic
        fun buildSwitchBrowserLaunchIntent(
            context: Context,
            redirectUri: String,
            browserPackageName: String,
            browserSupportsCustomTabs: Boolean,
            processUri: String,
            spanContext: SerializableSpanContext?
        ): Intent {
            return Intent(context, SwitchBrowserActivity::class.java).apply {
                putExtra(REDIRECT_URI, redirectUri)
                putExtra(BROWSER_PACKAGE_NAME, browserPackageName)
                putExtra(BROWSER_SUPPORTS_CUSTOM_TABS, browserSupportsCustomTabs)
                putExtra(PROCESS_URI, processUri)
                putExtra(SerializableSpanContext.SERIALIZABLE_SPAN_CONTEXT, spanContext)
                setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
        }

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
        super.onCreate(savedInstanceState)
        val methodTag = "$TAG:onCreate"
        Logger.info(methodTag, "SwitchBrowserActivity created - Launching browser")
        // Only initialize the span on the first creation. On activity recreation
        // (config change / process restore) the intent still carries the upstream
        // SerializableSpanContext, and creating another child span would produce
        // a duplicate sibling in the trace for one logical flow.
        if (savedInstanceState == null) {
            initSpanFromIntent()
        }
        launchStrategy = getLaunchStrategy()
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
        if (launchStrategy == null) {
            Logger.error(methodTag, "No browser launch strategy available", null)
            span?.setStatus(StatusCode.ERROR)
            span?.setAttribute(AttributeName.error_code.name, ErrorStrings.SWITCH_BROWSER_NO_LAUNCH_STRATEGY)
            setResultAndFinish(
                SwitchBrowserProtocolCoordinator.createErrorBundle(
                    ErrorStrings.SWITCH_BROWSER_NO_LAUNCH_STRATEGY,
                    "No browser launch strategy available"
                )
            )
            return
        }
        try {
            launchStrategy?.launch()
        } catch (throwable: Throwable) {
            span?.setStatus(StatusCode.ERROR)
            span?.recordException(throwable)
            Logger.error(methodTag, "Failed to launch browser for switch browser flow", throwable)
            setResultAndFinish(
                SwitchBrowserProtocolCoordinator.createErrorBundle(
                    if (throwable is BaseException) throwable.errorCode else ErrorStrings.UNKNOWN_ERROR,
                    throwable.message ?: "Unknown error occurred while launching browser for switch browser flow"
                )
            )
        }
    }

    private fun getLaunchStrategy(): BrowserLaunchStrategy {
        val methodTag = "$TAG:getLaunchStrategy"
        val isAuthTabFlightEnabled = CommonFlightsManager.getFlightsProvider(0)
            .isFlightEnabled(CommonFlight.ENABLE_AUTH_TAB_FOR_SWITCH_BROWSER)
        val extras = this.intent.extras ?: Bundle()
        val browserPackageName = extras.getString(BROWSER_PACKAGE_NAME).orEmpty()
        val isAuthTabSupported = AuthTabStrategyProvider.isAuthTabSupported(this, browserPackageName)
        Logger.info(
            methodTag,
            "Auth Tab decision inputs: flightEnabled=$isAuthTabFlightEnabled, " +
                "browserPackagePresent=${browserPackageName.isNotBlank()}, " +
                "isAuthTabSupported=$isAuthTabSupported, " +
                "browserPackage=$browserPackageName"
        )
        span?.setAttribute(
            AttributeName.browser_package_name.name,
            browserPackageName
        )
        span?.setAttribute(
            AttributeName.is_auth_tab_supported.name,
            isAuthTabSupported
        )

        val authTabStrategy = if (isAuthTabFlightEnabled && browserPackageName.isNotBlank() && isAuthTabSupported) {
            AuthTabStrategyProvider.createStrategy(this, ::onAuthTabResult)
        } else {
            null
        }
        span?.setAttribute(
            AttributeName.auth_tab_used.name,
            authTabStrategy != null
        )

        if (authTabStrategy != null) {
            Logger.info(methodTag, "Using Auth Tab strategy")
            return authTabStrategy
        }

        Logger.info(methodTag, "Using Custom Tabs strategy")
        return CustomTabsLaunchStrategy(this)
    }

    private fun onAuthTabResult(resultBundle: Bundle) {
        val methodTag = "$TAG:onAuthTabResult"
        Logger.info(methodTag, "Received Auth Tab result callback")
        span?.setStatus(StatusCode.OK)
        setResultAndFinish(resultBundle)
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
        super.onNewIntent(intent)
        val methodTag = "$TAG:onNewIntent"
        // Update the activity's intent with the new intent containing the auth result
        Logger.info(methodTag, "On new intent received.")
        setIntent(intent)

        if (intent.hasExtra(PROCESS_URI)) {
            // Don't set terminal ERROR status here — the flow is being restarted, not failed.
            // Record it as an attribute/event so the span outcome reflects the final result.
            span?.setAttribute(AttributeName.error_code.name, ErrorStrings.SWITCH_BROWSER_ALREADY_IN_PROGRESS)
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
        if (intent.hasExtra(SWITCH_BROWSER.RESUME_REQUEST)) {
            span?.setStatus(StatusCode.OK)
            setResultAndFinish(intent.extras)
            return
        }
        span?.setStatus(StatusCode.ERROR)
        span?.setAttribute(AttributeName.error_code.name, ErrorStrings.SWITCH_BROWSER_UNEXPECTED_INTENT)
        // Clean up: finish this activity and remove it from task stack
        Logger.info(methodTag, "Unexpected intent - Finishing activity and removing from task stack")
        setResultAndFinish(
            SwitchBrowserProtocolCoordinator.createErrorBundle(
                ErrorStrings.SWITCH_BROWSER_UNEXPECTED_INTENT,
                "Received an intent that does not match expected patterns for switch browser flow"
            )
        )
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

        if (cctLaunched && launchStrategy?.handlesCancellationOnResume() == true) {
            // User has returned to this activity after CCT was launched, likely due to backing out
            val cancellation = ClientException(
                ErrorStrings.USER_CANCELLED,
                ErrorStrings.AUTH_TAB_CANCELED_MESSAGE
            )
            span?.setStatus(StatusCode.ERROR)
            span?.recordException(cancellation)
            Logger.info(methodTag, "CCT was launched previously and user returned - Assuming cancellation, finishing activity")
            setResultAndFinish(
                SwitchBrowserProtocolCoordinator.createErrorBundle(
                    ErrorStrings.USER_CANCELLED,
                    ErrorStrings.AUTH_TAB_CANCELED_MESSAGE
                )
            )

        } else {
            // First resume after onCreate - mark CCT as launched for future reference
            Logger.info(methodTag, "First resume after onCreate - Marking CCT as launched")
        }

        cctLaunched = true
    }

    override fun onDestroy() {
        // Close the Scope before ending the span so the thread-local context is restored
        // to whatever it was prior to initSpanFromIntent().
        spanScope?.close()
        spanScope = null
        span?.end()
        launchStrategy?.cleanup()
        super.onDestroy()
    }

    /**
     * Restores the distributed tracing context propagated.
     * via the intent extras.
     *
     * Extracts [SerializableSpanContext] from the launching intent and creates a child span
     * ([SpanName.SwitchBrowserFlow]) parented to it. The span is stored as a field and made
     * current so that any code in this activity (or its strategies) that calls
     * [SpanExtension.current] will record attributes on the correct trace.
     */
    private fun initSpanFromIntent() {
        val extras = intent.extras
        val spanContext: SerializableSpanContext? = extras?.let {
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                it.getSerializable(
                    SerializableSpanContext.SERIALIZABLE_SPAN_CONTEXT,
                    SerializableSpanContext::class.java
                )
            } else {
                @Suppress("DEPRECATION")
                it.getSerializable(SerializableSpanContext.SERIALIZABLE_SPAN_CONTEXT)
                    as? SerializableSpanContext
            }
        }
        // Always create a span — fall back to an unparented span if no context was propagated —
        // so later code paths never operate on an unset/invalid span.
        OTelUtility.createSpanFromParent(SpanName.SwitchBrowserFlow.name, spanContext).let {
            span = it
            // Hold onto the Scope so we can close it in onDestroy(); otherwise the span
            // would remain attached to this thread's context and leak into subsequent work.
            spanScope = SpanExtension.makeCurrentSpan(it)
        }
    }

    /**
     * Returns the authentication result to [WebViewAuthorizationFragment] and finishes this activity.
     *
     * @param resultBundle The authentication result bundle (success or error), or `null`.
     */
    private fun setResultAndFinish(resultBundle: Bundle?) {
        WebViewAuthorizationFragment.setSwitchBrowserBundle(resultBundle)
        finishAndRemoveTask()
    }
}
