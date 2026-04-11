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
import androidx.browser.customtabs.CustomTabsClient
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightConfig
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkStatic
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [SwitchBrowserActivity] strategy selection logic.
 *
 * The test intent intentionally omits [SwitchBrowserActivity.PROCESS_URI] so that
 * [SwitchBrowserActivity.launchBrowser] exits early (before calling [BrowserLaunchStrategy.launch]),
 * allowing us to inspect the selected strategy without triggering an actual browser launch.
 */
@RunWith(RobolectricTestRunner::class)
class SwitchBrowserActivityTest {

    // ---------------------------------------------------------------------------
    // Simple test flights provider
    // ---------------------------------------------------------------------------

    private inner class TestFlightsProvider : IFlightsProvider {
        private val flights = mutableMapOf<String, Boolean>()

        fun setFlight(key: String, enabled: Boolean) {
            flights[key] = enabled
        }

        override fun isFlightEnabled(flightConfig: IFlightConfig): Boolean =
            flights[flightConfig.key] ?: (flightConfig.defaultValue as? Boolean ?: false)

        override fun getBooleanValue(flightConfig: IFlightConfig): Boolean =
            flights[flightConfig.key] ?: (flightConfig.defaultValue as? Boolean ?: false)

        override fun getIntValue(flightConfig: IFlightConfig): Int = 0

        override fun getDoubleValue(flightConfig: IFlightConfig): Double = 0.0

        override fun getStringValue(flightConfig: IFlightConfig): String = ""

        override fun getJsonValue(flightConfig: IFlightConfig): JSONObject = JSONObject()
    }

    private val testFlightsProvider = TestFlightsProvider()

    @Before
    fun setUp() {
        mockkStatic(CustomTabsClient::class)
        CommonFlightsManager.initializeCommonFlightsManager(object : IFlightsManager {
            override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long): IFlightsProvider =
                testFlightsProvider

            override fun getFlightsProviderForTenant(
                tenantId: String,
                waitForConfigsWithTimeoutInMs: Long
            ): IFlightsProvider = testFlightsProvider
        })
    }

    @After
    fun tearDown() {
        unmockkStatic(CustomTabsClient::class)
        CommonFlightsManager.resetFlightsManager()
    }

    // ---------------------------------------------------------------------------
    // Helper: build the activity with only BROWSER_PACKAGE_NAME.
    // PROCESS_URI is intentionally omitted so launchBrowser() exits early.
    // ---------------------------------------------------------------------------

    private fun buildAndCreateActivity(
        browserPackageName: String = "com.android.chrome"
    ): SwitchBrowserActivity {
        val intent = Intent(
            ApplicationProvider.getApplicationContext(),
            SwitchBrowserActivity::class.java
        ).apply {
            putExtra(SwitchBrowserActivity.BROWSER_PACKAGE_NAME, browserPackageName)
        }
        return Robolectric.buildActivity(SwitchBrowserActivity::class.java, intent)
            .create()
            .get()
    }

    // ---------------------------------------------------------------------------
    // Strategy selection
    // ---------------------------------------------------------------------------

    @Test
    fun `onCreate selects CustomTabsLaunchStrategy when flight is disabled`() {
        testFlightsProvider.setFlight(CommonFlight.ENABLE_AUTH_TAB_FOR_SWITCH_BROWSER.key, false)
        every { CustomTabsClient.isAuthTabSupported(any(), any()) } returns true

        val activity = buildAndCreateActivity()

        assertTrue(
            "Expected CustomTabsLaunchStrategy when flight is disabled",
            activity.getLaunchStrategy() is CustomTabsLaunchStrategy
        )
    }

    @Test
    fun `onCreate selects CustomTabsLaunchStrategy when flight is enabled but Auth Tab not supported`() {
        testFlightsProvider.setFlight(CommonFlight.ENABLE_AUTH_TAB_FOR_SWITCH_BROWSER.key, true)
        every { CustomTabsClient.isAuthTabSupported(any(), any()) } returns false

        val activity = buildAndCreateActivity()

        assertTrue(
            "Expected CustomTabsLaunchStrategy when Auth Tab is not supported",
            activity.getLaunchStrategy() is CustomTabsLaunchStrategy
        )
    }

    @Test
    fun `onCreate selects AuthTabLaunchStrategy when flight is enabled and Auth Tab is supported`() {
        testFlightsProvider.setFlight(CommonFlight.ENABLE_AUTH_TAB_FOR_SWITCH_BROWSER.key, true)
        every { CustomTabsClient.isAuthTabSupported(any(), any()) } returns true

        val activity = buildAndCreateActivity()

        assertTrue(
            "Expected AuthTabLaunchStrategy when flight is on and Auth Tab is supported",
            activity.getLaunchStrategy() is AuthTabLaunchStrategy
        )
    }

    @Test
    fun `CustomTabsLaunchStrategy handlesCancellationOnResume returns true`() {
        testFlightsProvider.setFlight(CommonFlight.ENABLE_AUTH_TAB_FOR_SWITCH_BROWSER.key, false)
        every { CustomTabsClient.isAuthTabSupported(any(), any()) } returns false

        val activity = buildAndCreateActivity()

        assertTrue(
            "CustomTabsLaunchStrategy should handle cancellation on resume",
            activity.getLaunchStrategy().handlesCancellationOnResume()
        )
    }

    @Test
    fun `AuthTabLaunchStrategy handlesCancellationOnResume returns false`() {
        testFlightsProvider.setFlight(CommonFlight.ENABLE_AUTH_TAB_FOR_SWITCH_BROWSER.key, true)
        every { CustomTabsClient.isAuthTabSupported(any(), any()) } returns true

        val activity = buildAndCreateActivity()

        assertFalse(
            "AuthTabLaunchStrategy should NOT handle cancellation on resume",
            activity.getLaunchStrategy().handlesCancellationOnResume()
        )
    }

    // ---------------------------------------------------------------------------
    // Helper: use the internal accessor instead of reflection
    // ---------------------------------------------------------------------------

    private fun SwitchBrowserActivity.getLaunchStrategy(): BrowserLaunchStrategy =
        getLaunchStrategyForTest()
}
