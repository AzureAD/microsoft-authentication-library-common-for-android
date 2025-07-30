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

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.fragment.app.Fragment

/**
 * Activity for handling silent web view authorization flows. It hides all UI and makes it transparent.
 * This is to support scenario where the app needs to perform authorization without expecting user interaction,
 * and notifying user. It is equivalent to the silent flow. Such flow is non-ideal but may be required by protocol.
 * Current requirement is for SWAG (Sign-in-with Apple/Google) flow for Copilot, to allow them to silently migrate
 * users from their stack to MSA. Similar change for fragment [SilentWebViewAuthorizationFragment].
 */
class SilentAuthorizationActivity : AuthorizationActivity() {
    override fun onCreate(savedInstanceState: android.os.Bundle?) {
        // Set the content view to a transparent layout. This along with
        // theme will ensure that the activity is transparent.
        window.decorView.alpha = 0f
        super.onCreate(savedInstanceState)
    }

    override fun setFragment(fragment: Fragment) {
        // Create a container for the fragment, but do not display it.
        val container = FrameLayout(this)
        container.id = View.generateViewId()
        container.layoutParams = ViewGroup.LayoutParams(0, 0)
        container.visibility = View.GONE

        setContentView(container)

        supportFragmentManager
            .beginTransaction()
            .replace(container.id, fragment)
            .commitNow()
    }
}