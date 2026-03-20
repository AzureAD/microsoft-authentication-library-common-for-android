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
import androidx.activity.OnBackPressedDispatcher
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.whenever
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
        // Real dispatcher to track callback registration.
        val dispatcher = OnBackPressedDispatcher()

        // Mock activity that returns our dispatcher.
        val mockActivity = mock<FragmentActivity> {
            on { onBackPressedDispatcher } doReturn dispatcher
        }

        // Mock lifecycle owner in RESUMED state for lifecycleScope.launch.
        val lifecycleOwner = mock<LifecycleOwner>()
        val lifecycleRegistry = LifecycleRegistry.createUnsafe(lifecycleOwner)
        lifecycleRegistry.currentState = Lifecycle.State.RESUMED
        whenever(lifecycleOwner.lifecycle).thenReturn(lifecycleRegistry)

        // Spy the fragment and stub only what onViewCreated needs.
        val fragment = spy(SilentWebViewAuthorizationFragment())
        doReturn(mockActivity).whenever(fragment).requireActivity()
        doReturn(lifecycleOwner).whenever(fragment).viewLifecycleOwner

        // Call onViewCreated directly with a mock view.
        fragment.onViewCreated(mock<View>(), null)

        // No back-pressed callback should have been registered by the silent fragment.
        assertFalse(
            "SilentWebViewAuthorizationFragment must not register an OnBackPressedCallback",
            dispatcher.hasEnabledCallbacks()
        )
    }
}
