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
package com.microsoft.identity.common.internal.ui

import android.app.Activity
import android.os.Bundle
import com.microsoft.identity.common.logging.Logger

/**
 * Transparent return trampoline that Authenticator invokes (via a one-shot, immutable
 * [android.app.PendingIntent]) after the OpenID Verifiable Credentials (VID) hand-off, to bring
 * the broker host back to the foreground so the interrupted brokered auth can resume.
 *
 * Return-to-caller is wired ONLY for the brokered flow, so this activity runs inside the broker
 * app (Authenticator / Company Portal / Link to Windows) that minted the PendingIntent; the
 * brokerless/embedded case never sends it (see AzureActiveDirectoryWebViewClient). It declares
 * `android:taskAffinity=""`, so the mandatory `FLAG_ACTIVITY_NEW_TASK` launch lands in an isolated
 * throwaway task instead of an affinity-matched (and therefore forgeable) task. Simply launching
 * it foregrounds the broker host; it then removes its own throwaway task so the broker's own task
 * surfaces.
 *
 * **This activity is purely a return / navigation signal.** It does NOT mark VID or auth as
 * successful just because it was invoked. VID/auth success must be validated through the normal
 * trusted continuation state / broker / server validation.
 */
class OpenIdVcReturnActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The mere launch of this activity foregrounds the (broker) host app. Immediately tear
        // down this isolated throwaway task so the host's own task surfaces; touch no other task.
        Logger.info(TAG, "OpenID VC return trampoline invoked; finishing throwaway task.")
        finishAndRemoveTask()
    }

    companion object {
        private val TAG = OpenIdVcReturnActivity::class.java.simpleName

        /**
         * Extra (on the openid-vc launch intent sent to Authenticator) carrying the immutable
         * return [android.app.PendingIntent] that targets this activity.
         */
        const val RETURN_PENDING_INTENT_EXTRA = "return_pending_intent"

        /** Action used on the explicit return intent that this activity is the target of. */
        const val ACTION_RETURN_FROM_VID = "com.microsoft.identity.RETURN_FROM_VID"
    }
}
