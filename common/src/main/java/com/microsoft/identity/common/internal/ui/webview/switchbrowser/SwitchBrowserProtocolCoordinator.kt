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
package com.microsoft.identity.common.internal.ui.webview.switchbrowser

import android.app.Activity
import android.net.Uri
import android.os.Bundle
import androidx.annotation.VisibleForTesting
import androidx.core.net.toUri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.providers.oauth2.SwitchBrowserActivity
import com.microsoft.identity.common.internal.ui.browser.AndroidBrowserSelector
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl
import com.microsoft.identity.common.java.AuthenticationConstants.AAD.AUTHORIZATION
import com.microsoft.identity.common.java.browser.IBrowserSelector
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SerializableSpanContext
import com.microsoft.identity.common.java.opentelemetry.SpanExtension
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.java.ui.BrowserDescriptor
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.SpanContext
import io.opentelemetry.api.trace.StatusCode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Coordinates the switch-browser protocol: the outbound "challenge" step (driven by
 * [AzureActiveDirectoryWebViewClient]) and the inbound "resume" step (driven by
 * [com.microsoft.identity.common.internal.providers.oauth2.WebViewAuthorizationFragment]).
 *
 * Both phases call [SwitchBrowserUriHelper.validateActionUri], which triggers AAD cloud
 * discovery — a synchronous HTTPS call on a cold cache. The async entry points hop off the
 * main thread to avoid [android.os.NetworkOnMainThreadException]. The coordinator does not
 * touch UI directly; it reports flow status via [SwitchBrowserStatusCallback] and the host
 * reacts.
 */
class SwitchBrowserProtocolCoordinator(
    private val activity: Activity,
    private val browserSelector: IBrowserSelector,
    private val statusCallback: SwitchBrowserStatusCallback,
    private val spanContext: SpanContext?
) {

    /** Convenience constructor that uses [AndroidBrowserSelector] by default. */
    constructor(
        activity: Activity,
        statusCallback: SwitchBrowserStatusCallback,
        spanContext: SpanContext?
    ) : this(
        activity,
        AndroidBrowserSelector(activity.applicationContext),
        statusCallback,
        spanContext
    )

    /**
     * True between a successful outbound challenge launch and the resume being consumed (or reset).
     * Read cross-module via [isExpectingSwitchBrowserResume]; the setter is restricted so external
     * holders of the coordinator cannot flip the resume state — only this class (and tests) may.
     */
    @Volatile
    var isSwitchBrowserChallengeActive: Boolean = false
        @VisibleForTesting internal set

    /**
     * Set when the outbound challenge successfully launches the browser activity. Unlike
     * [isSwitchBrowserChallengeActive], this is never reset. Volatile for cross-thread reads.
     */
    @Volatile
    var wasSwitchBrowserFlowInitiated: Boolean = false
        private set

    /**
     * Test-only override for [asyncScope]. Set via [forTesting]; production code never
     * touches this and the [asyncScope] lazy falls through to the real Main-immediate scope.
     */
    @VisibleForTesting
    internal var asyncScopeOverride: CoroutineScope? = null

    /**
     * Lazy so we don't touch [Dispatchers.Main] at construction time (avoids
     * MissingMainCoroutineDispatcherException in unit tests). Tests inject via [forTesting].
     */
    private val asyncScope: CoroutineScope by lazy {
        asyncScopeOverride ?: CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    }

    companion object {
        private const val TAG = "SwitchBrowserProtocolCoordinator"

        private const val ERROR_CODE_KEY = "error_code"
        private const val ERROR_MESSAGE_KEY = "error_message"

        /**
         * Checks if the given [url] is used to resume the switch browser flow.
         * This is determined by validating whether the URL starts with `[redirectUrl]/switch_browser_resume`.
         *
         * Returns `true` if the URL matches the expected pattern, `false` otherwise.
         */
        fun isSwitchBrowserResume(url: String?, redirectUrl: String): Boolean {
            return SwitchBrowserUriHelper.isSwitchBrowserRedirectUrl(url, redirectUrl, SWITCH_BROWSER.RESUME_PATH)
        }

        fun createErrorBundle(errorCode: String, errorMessage: String): Bundle {
            return Bundle().apply {
                putString(ERROR_CODE_KEY, errorCode)
                putString(ERROR_MESSAGE_KEY, errorMessage)
            }
        }

        /**
         * Factory for tests that need to drive coroutines on a controlled scope (e.g. a
         * [Dispatchers.Unconfined]-backed scope). Production code uses the regular
         * constructors, which default to a lazy Main-immediate scope.
         */
        @VisibleForTesting
        internal fun forTesting(
            activity: Activity,
            browserSelector: IBrowserSelector,
            statusCallback: SwitchBrowserStatusCallback,
            spanContext: SpanContext?,
            asyncScope: CoroutineScope
        ): SwitchBrowserProtocolCoordinator = SwitchBrowserProtocolCoordinator(
            activity,
            browserSelector,
            statusCallback,
            spanContext
        ).apply { asyncScopeOverride = asyncScope }
    }

    /**
     * Process an inbound switch_browser redirect: build the action URI (which transitively
     * triggers AAD cloud-discovery on a cold cache — a synchronous HTTPS call), validate the
     * OAuth `state`, pick a browser, and launch [SwitchBrowserActivity].
     *
     * Returns immediately. [SwitchBrowserUriHelper.buildProcessUri] runs on [Dispatchers.IO];
     * state validation, browser selection, and [Activity.startActivity] run on Main.
     *
     * Status is reported to [SwitchBrowserStatusCallback] (started / failed). Success is observed
     * via the side effect of [Activity.startActivity].
     *
     * The handler does **not** serialize concurrent invocations. Re-entry is suppressed because
     * [SwitchBrowserStatusCallback.onSwitchBrowserStarted] disables the WebView, and it runs
     * synchronously before this method returns — which holds only because [asyncScope] uses
     * [Dispatchers.Main.immediate] (plain [Dispatchers.Main] would post the disable and lose the guard).
     *
     * @param switchBrowserRedirectUrl `<redirect>/switch_browser?code=...&action_uri=...`
     *   URL captured by the WebView in `shouldOverrideUrlLoading`.
     * @param authorizationUrl Original authorization-request URL — used to validate `state`.
     * @param baseRedirectUri The app's registered redirect URI; routed back via
     *   [SwitchBrowserActivity].
     */
    fun processSwitchBrowserRedirectAsync(
        switchBrowserRedirectUrl: String,
        authorizationUrl: String,
        baseRedirectUri: String
    ) {
        val methodTag = "$TAG:processSwitchBrowserRedirectAsync"
        asyncScope.launch {
            statusCallback.onSwitchBrowserStarted()
            val span = OTelUtility.createSpanFromParent(SpanName.SwitchBrowserProcess.name, spanContext)
            try {
                // buildProcessUri triggers cloud discovery (sync HTTPS on cold cache).
                val processUri = withContext(Dispatchers.IO) {
                    SwitchBrowserUriHelper.buildProcessUri(switchBrowserRedirectUrl.toUri())
                }
                launchSwitchBrowserActivity(authorizationUrl, baseRedirectUri, processUri, span)
                span.setStatus(StatusCode.OK)
            } catch (ce: CancellationException) {
                // Cancelled (e.g. fragment destroyed). Host is tearing down — don't report.
                span.setStatus(StatusCode.ERROR, "Switch browser redirect cancelled")
                span.recordException(ce)
                throw ce
            } catch (t: Throwable) {
                Logger.error(methodTag, "Error processing switch_browser redirect: ${t.message}", t)
                span.setStatus(StatusCode.ERROR)
                span.recordException(t)
                statusCallback.onSwitchBrowserFailed(t)
            } finally {
                span.end()
            }
        }
    }

    /**
     * Main-thread portion of [processSwitchBrowserRedirectAsync]: validate the OAuth state, pick
     * a browser, and launch the broker activity.
     */
    @Throws(ClientException::class)
    private fun launchSwitchBrowserActivity(
        authorizationUrl: String,
        baseRedirectUri: String,
        processUri: Uri,
        span: Span
    ) {
        SpanExtension.makeCurrentSpan(span).use {
            val state = processUri.getQueryParameter(SWITCH_BROWSER.STATE)
            SwitchBrowserUriHelper.statesMatch(authorizationUrl, state)

            // Select a browser to handle the switch_browser flow.
            val browser = browserSelector.selectBrowser(
                BrowserDescriptor.getBrowserSafeListForSwitchBrowser(),
                null
            ) ?: throw ClientException(
                ClientException.NO_BROWSERS_AVAILABLE,
                "No browser found to handle the switch_browser flow."
            )

            span.setAttribute(
                AttributeName.browser_package_name.name,
                browser.packageName
            )
            span.setAttribute(
                AttributeName.is_custom_tabs_supported.name,
                browser.isCustomTabsServiceSupported
            )
            val switchBrowserIntent = SwitchBrowserActivity.buildSwitchBrowserLaunchIntent(
                context = activity,
                redirectUri = baseRedirectUri,
                browserPackageName = browser.packageName,
                browserSupportsCustomTabs = browser.isCustomTabsServiceSupported,
                processUri = processUri.toString(),
                spanContext = span.spanContext.toSerializable()
            )
            activity.startActivity(switchBrowserIntent)
            isSwitchBrowserChallengeActive = true
            wasSwitchBrowserFlowInitiated = true
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
    @VisibleForTesting
    internal fun resetChallengeState() {
        isSwitchBrowserChallengeActive = false
    }

    // region resume

    /**
     * Check if the handler is expecting a switch browser resume. True if a previous
     * [processSwitchBrowserRedirectAsync] invocation successfully launched the browser
     * activity (i.e. we are partway through the switch_browser flow and waiting for control
     * to come back).
     */
    fun isExpectingSwitchBrowserResume(): Boolean {
        val methodTag = "$TAG:isExpectingSwitchBrowserResume"
        Logger.verbose(methodTag, "ExpectingRequest: $isSwitchBrowserChallengeActive")
        return isSwitchBrowserChallengeActive
    }

    /**
     * Inspects the given [bundle] for error entries populated by [createErrorBundle].
     * If either [ERROR_CODE_KEY] or [ERROR_MESSAGE_KEY] is present, a [ClientException] is
     * constructed with those values and thrown immediately.
     *
     * @param bundle The bundle to inspect.
     * @throws ClientException if the bundle contains an error code or error message.
     */
    @Throws(ClientException::class)
    private fun throwIfBundleContainsError(bundle: Bundle) {
        val errorCode = bundle.getString(ERROR_CODE_KEY)
        val errorMessage = bundle.getString(ERROR_MESSAGE_KEY)
        if (!errorCode.isNullOrEmpty() || !errorMessage.isNullOrEmpty()) {
            throw ClientException(
                errorCode ?: ClientException.UNKNOWN_ERROR,
                errorMessage ?: "An unknown error occurred in the switch browser flow."
            )
        }
    }

    /**
     * Processes the switch browser resume action.
     *
     * @param authorizationRequest Original authorization request URL (for state validation).
     * @param extras The bundle containing the switch browser action URI and authorization code.
     * @return A pair of (resume URI, headers) the caller should use to launch the WebView.
     * @throws ClientException if validation fails or the bundle carries an error.
     */
    @VisibleForTesting
    @Throws(ClientException::class)
    internal fun processSwitchBrowserResume(
        authorizationRequest: String,
        extras: Bundle
    ): Pair<Uri, HashMap<String, String>> {
        val methodTag = "$TAG:processSwitchBrowserResume"
        val resumeSpan = OTelUtility.createSpanFromParent(SpanName.SwitchBrowserResume.name, spanContext)
        return SpanExtension.makeCurrentSpan(resumeSpan).use {
            try {
                throwIfBundleContainsError(extras)
                val actionUri = extras.getString(SWITCH_BROWSER.ACTION_URI)
                val code = extras.getString(SWITCH_BROWSER.CODE)
                val state = extras.getString(SWITCH_BROWSER.STATE)
                if (actionUri.isNullOrEmpty() || code.isNullOrEmpty()) {
                    throw ClientException(
                        ClientException.MISSING_PARAMETER,
                        "Action URI is null/empty: ${actionUri.isNullOrEmpty()}," +
                                " code is null/empty: ${code.isNullOrEmpty()}."
                    )
                }
                // Validate the state from auth request and redirect URL is the same.
                SwitchBrowserUriHelper.statesMatch(authorizationRequest, state)
                val resumeUri = SwitchBrowserUriHelper.buildResumeUri(actionUri, state)
                val headers = hashMapOf(AUTHORIZATION to "Bearer $code")
                Logger.info(methodTag, "Switch browser resume action processed successfully.")
                resumeSpan.setAttribute(AttributeName.is_switch_browser_resume_handled.name, true)
                resumeSpan.setStatus(StatusCode.OK)
                resumeUri to headers
            } catch (t: Throwable) {
                resumeSpan.setStatus(StatusCode.ERROR)
                resumeSpan.recordException(t)
                throw t
            } finally {
                // Always clear the challenge state — this resume is one-shot. Leaving it set
                // on the error path would cause subsequent onResume() calls to re-enter the
                // resume flow with an already-consumed bundle and fail again.
                resetChallengeState()
                resumeSpan.end()
            }
        }
    }

    /**
     * Async wrapper around [processSwitchBrowserResume].
     *
     * [SwitchBrowserUriHelper.buildResumeUri] runs [AzureActiveDirectory.ensureCloudDiscoveryForAuthority],
     * which is a synchronous HTTPS call on a cold cache. Invoking the synchronous variant
     * from a UI thread (e.g. `Fragment.onResume`) would crash with
     * [android.os.NetworkOnMainThreadException]. This method dispatches the body to
     * [Dispatchers.IO] and reports success/failure on Main. Callers own any UI state
     * (spinner, WebView enabled flag) around this call.
     *
     * @param authorizationRequest Original authorization request URL (for state validation).
     * @param extras               Bundle delivered by the system browser via the resume intent.
     */
    fun processSwitchBrowserResumeAsync(
        authorizationRequest: String,
        extras: Bundle
    ) {
        val methodTag = "$TAG:processSwitchBrowserResumeAsync"
        // Close the re-entrancy window synchronously: if onResume() fires again before the IO
        // work completes, isExpectingSwitchBrowserResume() must already read false so the flow
        // is not re-dispatched against the one-shot bundle (which would yield a spurious error +
        // finish()). The sync resume's finally also calls resetChallengeState() — harmless repeat.
        resetChallengeState()
        asyncScope.launch {
            statusCallback.onSwitchBrowserResumed()
            try {
                // buildResumeUri can hit the network (cold cache), so build off the main thread.
                val (uri, headers) = withContext(Dispatchers.IO) {
                    processSwitchBrowserResume(authorizationRequest, extras)
                }
                statusCallback.onSwitchBrowserCompleted(uri, headers)
            } catch (ce: CancellationException) {
                // Coroutine cancellation (e.g. fragment destroyed) — rethrow so structured
                // concurrency can unwind the scope. Not a failure to report to the callback.
                throw ce
            } catch (t: Throwable) {
                Logger.error(methodTag, "Async switch browser resume failed: ${t.message}", t)
                statusCallback.onSwitchBrowserFailed(t)
            }
        }
    }

    /**
     * Cancels any in-flight async work so coroutine continuations do not resume against a
     * destroyed [Activity]/Fragment (which would touch dead UI — `startActivity`, `mWebView`,
     * `sendResult`, `finish()`). Call from the owning component's teardown
     * (e.g. `WebViewAuthorizationFragment.onDestroy`). No-op for test-injected scopes.
     */
    fun cancel() {
        // Only cancel the production scope; a test-injected scope is owned by the test.
        if (asyncScopeOverride == null) {
            asyncScope.cancel()
        }
    }

    // endregion
}

/**
 * Converts a [SpanContext] to a [SerializableSpanContext] so it can be passed between activities
 * via [android.content.Intent] extras under the [SerializableSpanContext.SERIALIZABLE_SPAN_CONTEXT] key.
 */
private fun SpanContext.toSerializable(): SerializableSpanContext =
    SerializableSpanContext.builder()
        .traceId(traceId)
        .spanId(spanId)
        .traceFlags(traceFlags.asByte())
        .build()

