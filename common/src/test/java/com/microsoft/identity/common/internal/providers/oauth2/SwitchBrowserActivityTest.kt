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

import android.content.Context
import android.content.Intent
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.SWITCH_BROWSER
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightConfig
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.mockkConstructor
import io.mockk.mockkObject
import io.mockk.runs
import io.mockk.unmockkAll
import io.mockk.verify
import org.json.JSONObject
import org.junit.After
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [SwitchBrowserActivity].
 */
@RunWith(RobolectricTestRunner::class)
class SwitchBrowserActivityTest {

    @Before
    fun setUp() {
        // SwitchBrowserActivity.getLaunchStrategy() reads
        // CommonFlight.ENABLE_AUTH_TAB_FOR_SWITCH_BROWSER. Its default value is environment-
        // dependent (true on a developer machine where it has been initialized, false in CI
        // where the DefaultValueFlightsProvider returns the flight's default — which is false
        // for any feature still rolling out). Force it ON here via the shared
        // MockCommonFlightsManager helper so the Auth Tab branch is exercised deterministically
        // across local and pipeline runs.
        val flightsManager = MockCommonFlightsManager().apply {
            setMockCommonFlightsProvider(AllOnFlightsProvider)
        }
        CommonFlightsManager.initializeCommonFlightsManager(flightsManager)
    }

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
        unmockkAll()
    }

    // ...existing code...
    @Test
    fun onCreate_usesAuthTabStrategy_whenProviderReturnsOne() {
        mockkObject(AuthTabStrategyProvider)
        val authTabStrategy = mockk<BrowserLaunchStrategy>(relaxed = true)
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns true
        every { AuthTabStrategyProvider.createStrategy(any(), any()) } returns authTabStrategy

        Robolectric.buildActivity(SwitchBrowserActivity::class.java, getIntent()).create()

        // launchStrategy.launch() is invoked synchronously inside onCreate(); no looper idling
        // required. Idling here previously drained a queued CustomTabs service-connection
        // callback that NPE'd inside CustomTabsClient.warmup under Robolectric.
        verify(exactly = 1) { authTabStrategy.launch() }
    }

    @Test
    fun onCreate_fallsBackToCustomTabsStrategy_whenAuthTabUnsupported() {
        mockkObject(AuthTabStrategyProvider)
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns false
        mockkConstructor(CustomTabsLaunchStrategy::class)
        every { anyConstructed<CustomTabsLaunchStrategy>().launch() } just runs

        Robolectric.buildActivity(SwitchBrowserActivity::class.java, getIntent()).create()

        verify(exactly = 1) { anyConstructed<CustomTabsLaunchStrategy>().launch() }
    }

    @Test
    fun onCreate_fallsBackToCustomTabsStrategy_whenBrowserPackageMissing() {
        mockkObject(AuthTabStrategyProvider)
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns false
        mockkConstructor(CustomTabsLaunchStrategy::class)
        every { anyConstructed<CustomTabsLaunchStrategy>().launch() } just runs

        val intentWithoutPackage = Intent().apply {
            putExtra(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, true)
            putExtra(SwitchBrowserActivity.PROCESS_URI, "https://login.microsoftonline.com/switchbrowser/process")
        }

        Robolectric.buildActivity(SwitchBrowserActivity::class.java, intentWithoutPackage).create()

        // isAuthTabSupported is called but returns false (package is empty)
        verify(exactly = 1) { AuthTabStrategyProvider.isAuthTabSupported(any(), "") }
        // createStrategy should NOT be called because browserPackageName.isNotBlank() fails
        verify(exactly = 0) { AuthTabStrategyProvider.createStrategy(any(), any()) }
        // CustomTabs strategy is used as fallback
        verify(exactly = 1) { anyConstructed<CustomTabsLaunchStrategy>().launch() }
    }

    @Test
    fun onCreate_fallsBackToCustomTabsStrategy_whenCreateStrategyReturnsNull() {
        mockkObject(AuthTabStrategyProvider)
        // isAuthTabSupported returns true, but createStrategy returns null
        // This can occur if the provider is misconfigured or fails to create a strategy
        every { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) } returns true
        every { AuthTabStrategyProvider.createStrategy(any(), any()) } returns null
        mockkConstructor(CustomTabsLaunchStrategy::class)
        every { anyConstructed<CustomTabsLaunchStrategy>().launch() } just runs

        Robolectric.buildActivity(SwitchBrowserActivity::class.java, getIntent()).create()

        // Verify that isAuthTabSupported was called
        verify(exactly = 1) { AuthTabStrategyProvider.isAuthTabSupported(any(), any()) }
        // Verify that createStrategy was called
        verify(exactly = 1) { AuthTabStrategyProvider.createStrategy(any(), any()) }
        // Verify that CustomTabsLaunchStrategy.launch() was called as fallback
        verify(exactly = 1) { anyConstructed<CustomTabsLaunchStrategy>().launch() }
    }

    @Test
    fun `test buildSwitchBrowserResumeIntent`() {
        // Mock parameters
        val mockContext = mock(Context::class.java)
        val actionUri = "mock-action-uri"
        val code = "mock-code"
        val state = "mock-state"
        val intentDataString =
            "${Broker.NEW_BROKER_REDIRECT_URI}/${SWITCH_BROWSER.RESUME_PATH}?" +
                    "${SWITCH_BROWSER.ACTION_URI}=$actionUri&" +
                    "${SWITCH_BROWSER.CODE}=$code&" +
                    "${SWITCH_BROWSER.STATE}=$state"

        // Call the method to be tested
        val intent = SwitchBrowserActivity.buildSwitchBrowserResumeIntent(mockContext, intentDataString)

        // Verify the result
        Assert.assertEquals(
            0,
            intent.flags
        )
        Assert.assertEquals(actionUri, intent.getStringExtra(SWITCH_BROWSER.ACTION_URI))
        Assert.assertEquals(code, intent.getStringExtra(SWITCH_BROWSER.CODE))
        Assert.assertEquals(state, intent.getStringExtra(SWITCH_BROWSER.STATE))
    }

    private fun getIntent(): Intent {
        return Intent().apply {
            putExtra(SwitchBrowserActivity.BROWSER_PACKAGE_NAME, "com.test.browser")
            putExtra(SwitchBrowserActivity.BROWSER_SUPPORTS_CUSTOM_TABS, true)
            putExtra(SwitchBrowserActivity.PROCESS_URI, "https://login.microsoftonline.com/switchbrowser/process")
        }
    }

    /**
     * Test [IFlightsProvider] that returns `true` for any boolean flight, ensuring tests are
     * not affected by environment-specific flight defaults (CI vs local dev machine).
     * Plugged into the shared [MockCommonFlightsManager] helper.
     */
    private object AllOnFlightsProvider : IFlightsProvider {
        override fun isFlightEnabled(flightConfig: IFlightConfig): Boolean = true
        override fun getBooleanValue(flightConfig: IFlightConfig): Boolean = true
        override fun getIntValue(flightConfig: IFlightConfig): Int =
            flightConfig.defaultValue as Int
        override fun getDoubleValue(flightConfig: IFlightConfig): Double =
            flightConfig.defaultValue as Double
        override fun getStringValue(flightConfig: IFlightConfig): String =
            flightConfig.defaultValue as String
        override fun getJsonValue(flightConfig: IFlightConfig): JSONObject =
            flightConfig.defaultValue as JSONObject
    }
}
