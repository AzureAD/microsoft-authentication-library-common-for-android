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

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI
import com.microsoft.identity.common.internal.ui.browser.BrowserAuthorizationStrategy
import com.microsoft.identity.common.internal.ui.webview.ProcessUtil
import com.microsoft.identity.common.java.browser.Browser
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightConfig
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy
import io.mockk.every
import io.mockk.mockkStatic
import io.mockk.unmockkAll
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf

/**
 * Tests that [BrowserAuthorizationStrategy.launchIntent] performs URL scheme conflict validation
 * for browser flows before delegating to the base class.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidAuthorizationStrategyValidationTest {

    companion object {
        private const val COMPETING_PACKAGE = "com.example.otherapp"
        private const val COMPETING_ACTIVITY = "com.example.otherapp.SomeActivity"
        private const val REDIRECT_URI_VALUE = "msauth://org.robolectric.default/redirect"
    }

    private lateinit var activity: Activity
    private lateinit var context: Context

    @Before
    fun setUp() {
        activity = Robolectric.buildActivity(Activity::class.java).create().get()
        context = RuntimeEnvironment.getApplication()
    }

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
        unmockkAll()
    }

    /**
     * Minimal concrete subclass of [BrowserAuthorizationStrategy] that exposes [launchIntent]
     * publicly for testing.
     */
    @Suppress("UNCHECKED_CAST")
    private inner class TestBrowserAuthorizationStrategy(
        appContext: Context,
        act: Activity
    ) : BrowserAuthorizationStrategy<
            OAuth2Strategy<*, *, *, *, *, *, *, *, *, *, *, *, *>,
            AuthorizationRequest<*>>(
        appContext, act, null,
        Browser("com.android.chrome", emptySet(), "1.0", false)
    ) {
        override fun setIntentFlag(intent: Intent) { /* no-op */ }

        override fun requestAuthorization(
            authorizationRequest: AuthorizationRequest<*>,
            oAuth2Strategy: OAuth2Strategy<*, *, *, *, *, *, *, *, *, *, *, *, *>
        ) = throw UnsupportedOperationException("not used in these tests")

        override fun completeAuthorization(requestCode: Int, data: RawAuthorizationResult) = Unit

        // Expose the protected method publicly for tests.
        @Throws(ClientException::class)
        fun testLaunchIntent(intent: Intent) = launchIntent(intent)
    }

    // ──────────────────────────────────────────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────────────────────────────────────────

    private fun registerCompetingApp() {
        val resolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = COMPETING_PACKAGE
                name = COMPETING_ACTIVITY
            }
        }
        val competingRedirectIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(REDIRECT_URI_VALUE)
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        shadowOf(context.packageManager).addResolveInfoForIntent(
            competingRedirectIntent, resolveInfo
        )
    }

    private fun buildIntent(redirectUri: String? = REDIRECT_URI_VALUE): Intent {
        val intent = Intent()
        if (redirectUri != null) intent.putExtra(REDIRECT_URI, redirectUri)
        return intent
    }

    /** Stubs [ProcessUtil.isRunningOnAuthService] to return [value] for any context. */
    private fun stubIsRunningOnAuthService(value: Boolean) {
        mockkStatic(ProcessUtil::class)
        every { ProcessUtil.isRunningOnAuthService(any()) } returns value
    }

    /**
     * A [IFlightsManager] that reports every flight as disabled (returns the logical false /
     * default-off value). Used to verify that disabling
     * [CommonFlight.SKIP_MULTIPLE_APP_VALIDATION_IN_AUTH_SERVICE] forces validation to run.
     */
    private object AllOffFlightsManager : IFlightsManager {
        private val provider = object : IFlightsProvider {
            override fun isFlightEnabled(flightConfig: IFlightConfig): Boolean = false
            override fun getBooleanValue(flightConfig: IFlightConfig): Boolean = false
            override fun getIntValue(flightConfig: IFlightConfig): Int = flightConfig.defaultValue as Int
            override fun getDoubleValue(flightConfig: IFlightConfig): Double = flightConfig.defaultValue as Double
            override fun getStringValue(flightConfig: IFlightConfig): String = flightConfig.defaultValue as String
            override fun getJsonValue(flightConfig: IFlightConfig): JSONObject = JSONObject()
        }
        override fun getFlightsProvider(waitForConfigsWithTimeoutInMs: Long): IFlightsProvider = provider
        override fun getFlightsProviderForTenant(tenantId: String, waitForConfigsWithTimeoutInMs: Long): IFlightsProvider = provider
    }

    // ──────────────────────────────────────────────────────────────────────────────────
    // Tests
    // ──────────────────────────────────────────────────────────────────────────────────

    /**
     * Browser flow with a competing app registered → [ClientException] must be thrown from
     * [launchIntent] so it propagates through the command pipeline.
     */
    @Test
    fun `launchIntent browser flow throws ClientException when competing app is registered`() {
        registerCompetingApp()
        val strategy = TestBrowserAuthorizationStrategy(context, activity)

        try {
            strategy.testLaunchIntent(buildIntent())
            fail("Expected ClientException")
        } catch (e: ClientException) {
            assertEquals(ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME, e.errorCode)
        }
    }

    /**
     * Browser flow with no competing apps → no exception.
     */
    @Test
    fun `launchIntent browser flow passes when no competing app is registered`() {
        val strategy = TestBrowserAuthorizationStrategy(context, activity)

        // No competing app registered — should not throw (startActivity completes normally).
        strategy.testLaunchIntent(buildIntent())
    }

    /**
     * Brokered flow running in the auth service process with a competing app registered and a
     * valid msauth redirect URI → multiple-app URL scheme validation is skipped, so no
     * [ClientException] is thrown. This validates the skip exemption for brokered flows
     * (e.g. COBO/COPE/AM API) where another Microsoft app legitimately listens for the same
     * redirect and would otherwise trigger a false conflict.
     */
    @Test
    fun `launchIntent skips competing app validation when in auth service with msauth redirect`() {
        registerCompetingApp()
        // Simulate running in the broker auth service process.
        stubIsRunningOnAuthService(true)

        val strategy = TestBrowserAuthorizationStrategy(context, activity)

        // Competing app is registered, but because we're in the auth service with a valid
        // msauth redirect, validation is skipped and launchIntent completes without throwing.
        strategy.testLaunchIntent(buildIntent())
    }

    /**
     * Same brokered-flow conditions as above, but with the
     * [CommonFlight.SKIP_MULTIPLE_APP_VALIDATION_IN_AUTH_SERVICE] flight disabled → validation
     * must still run and throw [ClientException], ensuring the exemption can be turned off via ECS.
     */
    @Test
    fun `launchIntent validates competing app when skip flight is disabled`() {
        registerCompetingApp()
        stubIsRunningOnAuthService(true)
        CommonFlightsManager.initializeCommonFlightsManager(AllOffFlightsManager)

        val strategy = TestBrowserAuthorizationStrategy(context, activity)

        try {
            strategy.testLaunchIntent(buildIntent())
            fail("Expected ClientException")
        } catch (e: ClientException) {
            assertEquals(ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME, e.errorCode)
        }
    }
}
