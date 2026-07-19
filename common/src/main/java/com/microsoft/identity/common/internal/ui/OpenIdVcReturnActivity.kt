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
import android.content.Intent
import android.os.Bundle
import com.microsoft.identity.common.logging.Logger

/**
 * Transparent, no-history return trampoline that Authenticator invokes (via a
 * [android.app.PendingIntent]) to bring the original calling app's task back to the foreground
 * after the OpenID Verifiable Credentials (VID) hand-off. When Authenticator determines that
 * automatic return is appropriate, it invokes the supplied `PendingIntent` to surface the caller.
 * Otherwise, Authenticator displays a completion screen instructing the user to navigate back
 * manually.
 *
 * This activity is manifest-merged into any app that depends on this library, so a consuming
 * (MSAL) app does not need to declare it. The consuming app creates an explicit, immutable
 * `PendingIntent` targeting this class and passes it to Authenticator as part of the openid-vc
 * launch intent.
 *
 * **This activity is purely a return / navigation signal.** It does NOT mark VID or auth as
 * successful just because it was invoked. VID/auth success must be validated through the normal
 * trusted continuation state / broker / server validation. This activity only reveals the previous
 * activity in the caller's task and finishes immediately.
 */
class OpenIdVcReturnActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleReturn()
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handleReturn()
    }

    private fun handleReturn() {
        // Return/navigation signal only. Do NOT treat invocation as proof of VID/auth success.
        Logger.info(TAG, "OpenIdVcReturnActivity invoked; revealing caller task.")
        finish()
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
