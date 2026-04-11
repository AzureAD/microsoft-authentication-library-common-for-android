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

import android.net.Uri
import android.os.Bundle
import androidx.fragment.app.FragmentActivity
import com.microsoft.identity.common.internal.ui.browser.AuthTabManager
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator
import com.microsoft.identity.common.java.opentelemetry.AttributeName
import com.microsoft.identity.common.java.opentelemetry.OTelUtility
import com.microsoft.identity.common.java.opentelemetry.SpanName
import com.microsoft.identity.common.logging.Logger
import io.opentelemetry.api.trace.Span
import io.opentelemetry.api.trace.StatusCode

/**
 * [BrowserLaunchStrategy] that uses the AndroidX Browser 1.9.0 [AuthTabIntent] API to launch
 * authentication in a stripped-down, secure browser tab.
 *
 * Auth Tab delivers an explicit result callback for every outcome (success, cancellation, or
 * verification failure), so the on-resume heuristic used by Custom Tabs is not needed here:
 * [handlesCancellationOnResume] returns `false`.
 *
 * Telemetry attributes ([AttributeName.auth_tab_used], [AttributeName.auth_tab_result_code],
 * [AttributeName.is_auth_tab_supported]) are recorded on a [SpanName.SwitchBrowserProcess] span.
 *
 * @param activity   The hosting [FragmentActivity]; used to register the Auth Tab launcher and to
 *                   retrieve the [SwitchBrowserActivity.REDIRECT_SCHEME] intent extra.
 * @param onComplete Callback invoked when the Auth Tab flow ends.  Receives the resume [Bundle]
 *                   (non-null on success) or `null` on cancellation/failure, and then finishes
 *                   the activity.
 */
class AuthTabLaunchStrategy(
    private val activity: FragmentActivity,
    private val onComplete: (Bundle?) -> Unit
) : BrowserLaunchStrategy {

    companion object {
        private val TAG: String = AuthTabLaunchStrategy::class.java.simpleName
    }

    private val authTabManager = AuthTabManager()

    /**
     * Span created eagerly so that it captures the full Auth Tab flow from construction through
     * result delivery.  The span is ended either in [handleSuccess]/[handleCanceledOrFailed] or
     * in [cleanup] if the flow is interrupted before a result arrives.
     */
    private val span: Span = OTelUtility.createSpan(SpanName.SwitchBrowserProcess.name)

    init {
        authTabManager.registerLauncher(activity) { result ->
            onAuthTabResult(result)
        }
    }

    /**
     * Launches the Auth Tab for the given [processUri].
     *
     * The redirect scheme is read from the [SwitchBrowserActivity.REDIRECT_SCHEME] intent extra.
     * If the extra is absent or blank a warning is logged but the launch is still attempted.
     */
    override fun launch(processUri: Uri, browserPackageName: String) {
        val methodTag = "$TAG:launch"
        val redirectScheme = activity.intent?.extras
            ?.getString(SwitchBrowserActivity.REDIRECT_SCHEME) ?: ""
        if (redirectScheme.isBlank()) {
            Logger.warn(methodTag, "REDIRECT_SCHEME extra is missing; Auth Tab may not intercept the redirect correctly.")
        }
        Logger.info(methodTag, "Launching Auth Tab for URI: $processUri")
        span.setAttribute(AttributeName.auth_tab_used.name, true)
        span.setAttribute(AttributeName.is_auth_tab_supported.name, true)
        authTabManager.launch(processUri, redirectScheme)
    }

    /**
     * Auth Tab delivers an explicit cancellation callback, so the on-resume heuristic is
     * not required.
     */
    override fun handlesCancellationOnResume(): Boolean = false

    /**
     * Unregisters the [AuthTabManager] launcher.  If the Auth Tab flow was interrupted before
     * a result arrived (e.g. the activity was destroyed), the telemetry span is ended here to
     * prevent resource leaks.
     */
    override fun cleanup() {
        authTabManager.unregister()
        if (span.isRecording) {
            span.setStatus(StatusCode.ERROR)
            span.end()
        }
    }

    // region private helpers

    private fun onAuthTabResult(result: AuthTabManager.AuthTabResult) {
        val methodTag = "$TAG:onAuthTabResult"
        Logger.info(methodTag, "Auth Tab result received: $result")
        when (result) {
            is AuthTabManager.AuthTabResult.Success -> handleSuccess(result.resultUri)
            is AuthTabManager.AuthTabResult.Canceled -> handleCanceledOrFailed("CANCELED")
            is AuthTabManager.AuthTabResult.VerificationFailed -> handleCanceledOrFailed("VERIFICATION_FAILED")
            is AuthTabManager.AuthTabResult.VerificationTimedOut -> handleCanceledOrFailed("VERIFICATION_TIMED_OUT")
        }
    }

    private fun handleSuccess(resultUri: Uri) {
        val methodTag = "$TAG:handleSuccess"
        Logger.info(methodTag, "Auth Tab succeeded")
        span.setAttribute(AttributeName.auth_tab_result_code.name, "OK")
        span.setStatus(StatusCode.OK)
        span.end()
        val resumeIntent = SwitchBrowserProtocolCoordinator.getIntentToResumeWebViewAuth(
            activity,
            resultUri.toString()
        )
        onComplete(resumeIntent.extras)
    }

    private fun handleCanceledOrFailed(resultCode: String) {
        val methodTag = "$TAG:handleCanceledOrFailed"
        Logger.info(methodTag, "Auth Tab ended with result code: $resultCode")
        span.setAttribute(AttributeName.auth_tab_result_code.name, resultCode)
        span.setStatus(StatusCode.OK)
        span.end()
        onComplete(null)
    }

    // endregion
}
