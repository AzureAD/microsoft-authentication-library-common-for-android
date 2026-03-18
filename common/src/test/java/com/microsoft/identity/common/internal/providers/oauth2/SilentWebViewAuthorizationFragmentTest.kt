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
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_URL
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT
import com.microsoft.identity.common.java.ui.AuthorizationAgent
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [SilentWebViewAuthorizationFragment].
 */
@RunWith(RobolectricTestRunner::class)
class SilentWebViewAuthorizationFragmentTest {

    /**
     * Verifies that [SilentWebViewAuthorizationFragment.onViewCreated] does NOT register an
     * [androidx.activity.OnBackPressedCallback] on the host activity's
     * [androidx.activity.OnBackPressedDispatcher]. Silent flows are invisible to the user and
     * must never intercept the device back button, so the callback registered by
     * [AuthorizationFragment.onViewCreated] must be skipped.
     */
    @Test
    fun onViewCreated_doesNotRegisterOnBackPressedCallback() {
        val intent = Intent().apply {
            putExtra(AUTHORIZATION_AGENT, AuthorizationAgent.WEBVIEW)
            putExtra(WEB_VIEW_SILENT_AUTHORIZATION_FLOW_TIMEOUT, 5000L)
            putExtra(REDIRECT_URI, "msauth://com.test.package/redirect")
            putExtra(REQUEST_URL, "https://login.microsoftonline.com/common/oauth2/v2.0/authorize")
        }

        val activity = Robolectric.buildActivity(SilentAuthorizationActivity::class.java, intent)
            .create()
            .start()
            .resume()
            .get()

        // Confirm the SilentWebViewAuthorizationFragment was actually added to the activity and
        // that onViewCreated() was called, so the assertion below is meaningful.
        val fragment = activity.supportFragmentManager.fragments
            .filterIsInstance<SilentWebViewAuthorizationFragment>()
            .firstOrNull()
        assert(fragment != null) {
            "SilentWebViewAuthorizationFragment was not added to the activity; cannot verify back-press behavior."
        }
        assert(fragment?.view != null) {
            "SilentWebViewAuthorizationFragment view is null; onViewCreated() may not have been called."
        }

        // No back-pressed callback should have been registered by the silent fragment.
        assertFalse(
            "SilentWebViewAuthorizationFragment must not register an OnBackPressedCallback",
            activity.onBackPressedDispatcher.hasEnabledCallbacks()
        )
    }
}
