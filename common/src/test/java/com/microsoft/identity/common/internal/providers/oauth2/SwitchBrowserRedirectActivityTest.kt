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
import android.net.Uri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf

/**
 * Tests for [SwitchBrowserRedirectActivity].
 */
@RunWith(RobolectricTestRunner::class)
class SwitchBrowserRedirectActivityTest {

    companion object {
        private const val TEST_ACTION_URI = "https://login.microsoftonline.com/process"
        private const val TEST_CODE = "test-code-123"
        private const val TEST_STATE = "test-state-456"
        private const val REDIRECT_URI = "msauth://com.test.app/signature"
    }

    @Test
    fun onCreate_startsActivityWithCorrectExtras_whenValidSwitchBrowserResumeUri() {
        val intent = buildRedirectIntent(TEST_CODE, TEST_ACTION_URI, TEST_STATE)

        val controller = Robolectric.buildActivity(
            SwitchBrowserRedirectActivity::class.java, intent
        ).create()
        val activity = controller.get()

        val shadow = shadowOf(activity)
        val startedIntent = shadow.nextStartedActivity

        assertNotNull("Should start SwitchBrowserActivity", startedIntent)
        assertEquals(TEST_ACTION_URI, startedIntent.getStringExtra(SWITCH_BROWSER.ACTION_URI))
        assertEquals(TEST_CODE, startedIntent.getStringExtra(SWITCH_BROWSER.CODE))
        assertEquals(TEST_STATE, startedIntent.getStringExtra(SWITCH_BROWSER.STATE))
        assertTrue(activity.isFinishing)
    }

    @Test
    fun onCreate_finishes_whenIntentHasNoData() {
        val intent = Intent()

        val controller = Robolectric.buildActivity(
            SwitchBrowserRedirectActivity::class.java, intent
        ).create()
        val activity = controller.get()

        val shadow = shadowOf(activity)
        val startedIntent = shadow.nextStartedActivity

        // No activity should be started when there's no data
        assertEquals(null, startedIntent)
        assertTrue(activity.isFinishing)
    }

    @Test
    fun onNewIntent_startsActivityWithNewData() {
        val initialIntent = buildRedirectIntent(TEST_CODE, TEST_ACTION_URI, TEST_STATE)

        val controller = Robolectric.buildActivity(
            SwitchBrowserRedirectActivity::class.java, initialIntent
        ).create()
        val activity = controller.get()

        // Clear the first started activity
        val shadow = shadowOf(activity)
        shadow.nextStartedActivity

        // Deliver a new intent with different params
        val newCode = "new-code-789"
        val newState = "new-state-012"
        val newIntent = buildRedirectIntent(newCode, TEST_ACTION_URI, newState)

        controller.newIntent(newIntent)

        val startedIntent = shadow.nextStartedActivity

        assertNotNull("Should start SwitchBrowserActivity with new data", startedIntent)
        assertEquals(TEST_ACTION_URI, startedIntent.getStringExtra(SWITCH_BROWSER.ACTION_URI))
        assertEquals(newCode, startedIntent.getStringExtra(SWITCH_BROWSER.CODE))
        assertEquals(newState, startedIntent.getStringExtra(SWITCH_BROWSER.STATE))
    }

    @Test
    fun onCreate_doesNotStartActivity_onConfigurationChange() {
        val intent = buildRedirectIntent(TEST_CODE, TEST_ACTION_URI, TEST_STATE)

        val controller = Robolectric.buildActivity(
            SwitchBrowserRedirectActivity::class.java, intent
        ).create()
        val activity = controller.get()

        // Clear first started activity
        val shadow = shadowOf(activity)
        shadow.nextStartedActivity

        // Simulate recreation with savedInstanceState (e.g. configuration change)
        controller.recreate()

        // After recreation with savedInstanceState, it should NOT re-start the activity
        val startedIntent = shadow.nextStartedActivity
        assertEquals(null, startedIntent)
    }

    @Test
    fun onCreate_parsesEncodedUriParameters() {
        val encodedActionUri = "https%3A%2F%2Flogin.microsoftonline.com%2Fprocess%3Fparam%3Dvalue"
        val decodedActionUri = "https://login.microsoftonline.com/process?param=value"
        val intent = Intent().apply {
            data = Uri.parse(
                "$REDIRECT_URI/${SWITCH_BROWSER.RESUME_PATH}?" +
                        "${SWITCH_BROWSER.ACTION_URI}=$encodedActionUri&" +
                        "${SWITCH_BROWSER.CODE}=$TEST_CODE&" +
                        "${SWITCH_BROWSER.STATE}=$TEST_STATE"
            )
        }

        val controller = Robolectric.buildActivity(
            SwitchBrowserRedirectActivity::class.java, intent
        ).create()
        val activity = controller.get()

        val shadow = shadowOf(activity)
        val startedIntent = shadow.nextStartedActivity

        assertNotNull("Should handle encoded URI parameters", startedIntent)
        assertEquals(decodedActionUri, startedIntent.getStringExtra(SWITCH_BROWSER.ACTION_URI))
        assertEquals(TEST_CODE, startedIntent.getStringExtra(SWITCH_BROWSER.CODE))
        assertEquals(TEST_STATE, startedIntent.getStringExtra(SWITCH_BROWSER.STATE))
    }

    private fun buildRedirectIntent(code: String, actionUri: String, state: String): Intent {
        val uriString = "$REDIRECT_URI/${SWITCH_BROWSER.RESUME_PATH}?" +
                "${SWITCH_BROWSER.ACTION_URI}=$actionUri&" +
                "${SWITCH_BROWSER.CODE}=$code&" +
                "${SWITCH_BROWSER.STATE}=$state"
        return Intent().apply {
            data = Uri.parse(uriString)
        }
    }
}
