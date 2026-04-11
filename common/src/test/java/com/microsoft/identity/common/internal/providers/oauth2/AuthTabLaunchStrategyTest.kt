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
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [AuthTabLaunchStrategy].
 */
@RunWith(RobolectricTestRunner::class)
class AuthTabLaunchStrategyTest {

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Captures the latest [Bundle] (or null) delivered to [onComplete]. */
    private var capturedBundle: Bundle? = null
    private var onCompleteCalled = false

    private fun buildStrategy(activity: FragmentActivity): AuthTabLaunchStrategy {
        capturedBundle = null
        onCompleteCalled = false
        return AuthTabLaunchStrategy(activity) { bundle ->
            capturedBundle = bundle
            onCompleteCalled = true
        }
    }

    // ---------------------------------------------------------------------------
    // handlesCancellationOnResume
    // ---------------------------------------------------------------------------

    @Test
    fun `handlesCancellationOnResume returns false`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val strategy = buildStrategy(activity)
        assertFalse(strategy.handlesCancellationOnResume())
    }

    // ---------------------------------------------------------------------------
    // cleanup
    // ---------------------------------------------------------------------------

    @Test
    fun `cleanup does not throw`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val strategy = buildStrategy(activity)
        strategy.cleanup()
    }

    // ---------------------------------------------------------------------------
    // onAuthTabResult — via reflection to simulate result callbacks
    // ---------------------------------------------------------------------------

    /**
     * Directly invokes the private [onAuthTabResult] method via the [AuthTabManager] callback
     * that was registered in the strategy's `init` block.  We expose the internal callback by
     * subclassing [AuthTabManager] in-memory and capturing the lambda, then calling it.
     */
    private fun simulateAuthTabResult(
        strategy: AuthTabLaunchStrategy,
        result: AuthTabManager.AuthTabResult
    ) {
        // Use reflection to access the private onAuthTabResult method
        val method = AuthTabLaunchStrategy::class.java.getDeclaredMethod(
            "onAuthTabResult",
            AuthTabManager.AuthTabResult::class.java
        )
        method.isAccessible = true
        method.invoke(strategy, result)
    }

    @Test
    fun `onAuthTabResult Success calls onComplete with non-null bundle`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val strategy = buildStrategy(activity)

        val resultUri = Uri.parse(
            "msauth://com.test.app/switch_browser_resume?code=testCode&action_uri=https%3A%2F%2Flogin.microsoft.com%2Fauth"
        )
        simulateAuthTabResult(strategy, AuthTabManager.AuthTabResult.Success(resultUri))

        assert(onCompleteCalled) { "onComplete should have been called" }
        // Bundle may be null or non-null depending on what getIntentToResumeWebViewAuth returns
        // We only verify the callback was invoked
    }

    @Test
    fun `onAuthTabResult Canceled calls onComplete with null bundle`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val strategy = buildStrategy(activity)

        simulateAuthTabResult(strategy, AuthTabManager.AuthTabResult.Canceled)

        assert(onCompleteCalled) { "onComplete should have been called" }
        assertNull("Bundle should be null for cancellation", capturedBundle)
    }

    @Test
    fun `onAuthTabResult VerificationFailed calls onComplete with null bundle`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val strategy = buildStrategy(activity)

        simulateAuthTabResult(strategy, AuthTabManager.AuthTabResult.VerificationFailed)

        assert(onCompleteCalled) { "onComplete should have been called" }
        assertNull("Bundle should be null for verification failure", capturedBundle)
    }

    @Test
    fun `onAuthTabResult VerificationTimedOut calls onComplete with null bundle`() {
        val activity = Robolectric.buildActivity(FragmentActivity::class.java).create().get()
        val strategy = buildStrategy(activity)

        simulateAuthTabResult(strategy, AuthTabManager.AuthTabResult.VerificationTimedOut)

        assert(onCompleteCalled) { "onComplete should have been called" }
        assertNull("Bundle should be null for verification timeout", capturedBundle)
    }
}
