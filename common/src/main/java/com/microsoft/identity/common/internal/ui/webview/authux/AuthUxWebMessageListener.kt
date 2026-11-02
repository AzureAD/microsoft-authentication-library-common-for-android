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

package com.microsoft.identity.common.internal.ui.webview.authux

import android.net.Uri
import android.webkit.WebView
import androidx.annotation.UiThread
import androidx.webkit.JavaScriptReplyProxy
import androidx.webkit.WebMessageCompat
import androidx.webkit.WebViewCompat
import com.microsoft.identity.common.internal.broker.AuthUxMessageProcessor
import com.microsoft.identity.common.internal.broker.TelemetryAwareAuthUxMessageProcessor
import com.microsoft.identity.common.internal.ui.webview.authux.AuthUxMeasurementObserver.Event
import com.microsoft.identity.common.logging.Logger

/**
 * Validates and forwards main-frame Auth UX WebMessages to the request processor.
 */
class AuthUxWebMessageListener @JvmOverloads constructor(
    private val processor: AuthUxMessageProcessor,
    private val originPolicy: AuthUxOriginPolicy,
    private val bridgeTelemetry: AuthUxBridgeTelemetry = AuthUxBridgeTelemetry.NONE
) : WebViewCompat.WebMessageListener {

    @UiThread
    override fun onPostMessage(
        view: WebView,
        message: WebMessageCompat,
        sourceOrigin: Uri,
        isMainFrame: Boolean,
        replyProxy: JavaScriptReplyProxy
    ) {
        val callbackTelemetry = bridgeTelemetry.admitCallback()
        var rejection: Event? = Event.REJECTED_PROCESSING
        var forwarded = false
        try {
            if (!originPolicy.isAllowedCallback(sourceOrigin, isMainFrame)) {
                rejection = Event.REJECTED_ORIGIN
                Logger.warn(TAG, "Rejected Auth UX WebMessage callback.")
                return
            }

            if (message.type != WebMessageCompat.TYPE_STRING) {
                rejection = Event.REJECTED_TYPE
                Logger.warn(TAG, "Rejected Auth UX WebMessage callback with unsupported data type.")
                return
            }

            val data = message.data
            if (data == null) {
                rejection = Event.REJECTED_MALFORMED
                Logger.info(TAG, "Ignoring Auth UX WebMessage callback with no data.")
                return
            }

            if (processor is TelemetryAwareAuthUxMessageProcessor) {
                forwarded = true
                processor.processWithTelemetry(data, callbackTelemetry)
            } else {
                processor.process(data)
                rejection = null
            }
        } finally {
            if (!forwarded) rejection?.let { callbackTelemetry.count(it) }
            callbackTelemetry.completeCallback()
        }
    }

    companion object {
        private val TAG = AuthUxWebMessageListener::class.java.simpleName
    }
}
