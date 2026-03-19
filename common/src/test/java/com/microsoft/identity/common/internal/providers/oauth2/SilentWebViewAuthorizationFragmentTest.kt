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
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.spy
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

/**
 * Tests for [SilentWebViewAuthorizationFragment].
 */
class SilentWebViewAuthorizationFragmentTest {

    /**
     * Verifies that [SilentWebViewAuthorizationFragment.onViewCreated] does NOT call
     * [AuthorizationFragment.onViewCreated], which would register an enabled
     * [androidx.activity.OnBackPressedCallback] on the host activity's dispatcher.
     *
     * Silent flows are invisible to the user and must never intercept the device back button.
     * If [AuthorizationFragment.onViewCreated] were called, it would invoke [requireActivity],
     * which throws [IllegalStateException] when the fragment is not attached to an activity,
     * failing this test. The test passing confirms [super.onViewCreated] is skipped.
     */
    @Test
    fun onViewCreated_skipsSuperOnViewCreated_andTriggersCancelAuthorizationOnTimeOut() {
        val fragment = spy(SilentWebViewAuthorizationFragment())
        val mockView = mock<View>()

        // Stub out the internal timeout call so no Fragment lifecycle (viewLifecycleOwner) is needed.
        doNothing().whenever(fragment).cancelAuthorizationOnTimeOut(any())

        // If super.onViewCreated() were invoked it would call requireActivity(), throwing
        // IllegalStateException because the fragment is not attached. The call succeeding
        // proves super is skipped.
        fragment.onViewCreated(mockView, null)

        // Confirm the only side-effect — the timeout cancellation — was triggered.
        verify(fragment).cancelAuthorizationOnTimeOut(any())
    }
}
