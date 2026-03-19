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

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.VisibleForTesting
import androidx.lifecycle.lifecycleScope
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.logging.Logger
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fragment for handling silent web view authorization flows. It hides all UI and makes it transparent.
 * This is to support scenario where the app needs to perform authorization without expecting user interaction,
 * and notifying user. It is equivalent to the silent flow. Such flow is non-ideal but may be required by protocol.
 * Current requirement is for SWAG (Sign-in-with Apple/Google) flow for Copilot, to allow them to silently migrate
 * users from their stack to MSA.
 * Similar change for activity in [SilentAuthorizationActivity].
 */
class SilentWebViewAuthorizationFragment : WebViewAuthorizationFragment() {

    companion object {
        private const val TAG = "SilentWebViewAuthorizationFragment"
        private const val DEFAULT_WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIME_OUT = 5000L
        private const val MIN_WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT = 1000L
        private const val MAX_WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT = 20000L
    }

    private var webViewSilentAuthorizationFlowTimeOut: Long = DEFAULT_WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIME_OUT
        set(value) {
            field = value.coerceIn(MIN_WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT, MAX_WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT) // Validate range
        }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? = super.onCreateView(inflater, container, savedInstanceState)?.apply {
        visibility = View.GONE
        setBackgroundColor(android.graphics.Color.TRANSPARENT) // Ensure the view is transparent
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        // Do not call super.onViewCreated() here. The parent AuthorizationFragment.onViewCreated()
        // registers an OnBackPressedCallback that would intercept device back button presses.
        // SilentWebViewAuthorizationFragment is designed for silent, invisible flows and must not
        // intercept back button events so that host app navigation is unaffected.
        cancelAuthorizationOnTimeOut(webViewSilentAuthorizationFlowTimeOut)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putLong(WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT, webViewSilentAuthorizationFlowTimeOut)
    }

    override fun extractState(state: Bundle) {
        super.extractState(state)
        webViewSilentAuthorizationFlowTimeOut = state.getLong(WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT, DEFAULT_WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIME_OUT)
    }

    /**
     * Cancels the authorization flow after a specified timeout.
     * This is useful for silent flows where we want to avoid waiting indefinitely.
     * Tied to fragment lifecycle to ensure it is cancelled if the fragment is no longer visible.
     *
     * @param timeOutInMs The timeout duration in milliseconds.
     */
    @VisibleForTesting
    internal fun cancelAuthorizationOnTimeOut(timeOutInMs : Long) = viewLifecycleOwner.lifecycleScope.launch{
        val methodTag = "$TAG:cancelAuthorizationOnTimeOut"
        delay(timeOutInMs)
        Logger.info(methodTag, "Received Authorization flow cancel request from SDK")
        sendResult(RawAuthorizationResult.ResultCode.TIMED_OUT)
        finish()
    }
}