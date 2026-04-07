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
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.AUTHORIZATION_AGENT
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy
import com.microsoft.identity.common.java.ui.AuthorizationAgent
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
 * Tests that [AndroidAuthorizationStrategy.launchIntent] performs URL scheme conflict validation
 * for non-WebView flows and skips it for WebView flows.
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

    /**
     * Minimal concrete subclass of [AndroidAuthorizationStrategy] that exposes [launchIntent]
     * publicly for testing (and overrides [requestAuthorization] as required by the interface).
     */
    @Suppress("UNCHECKED_CAST")
    private inner class TestAndroidAuthorizationStrategy(
        appContext: Context,
        act: Activity
    ) : AndroidAuthorizationStrategy<OAuth2Strategy, AuthorizationRequest>(appContext, act, null) {

        override fun requestAuthorization(
            authorizationRequest: AuthorizationRequest,
            oAuth2Strategy: OAuth2Strategy
        ) = throw UnsupportedOperationException("not used in these tests")

        override fun completeAuthorization(requestCode: Int, data: com.microsoft.identity.common.java.util.ported.PropertyBag) =
            Unit

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
        shadowOf(context.packageManager).addResolveInfoForIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse(REDIRECT_URI_VALUE)), resolveInfo
        )
    }

    private fun buildIntent(agent: AuthorizationAgent?, redirectUri: String? = REDIRECT_URI_VALUE): Intent {
        val intent = Intent()
        if (agent != null) intent.putExtra(AUTHORIZATION_AGENT, agent)
        if (redirectUri != null) intent.putExtra(REDIRECT_URI, redirectUri)
        return intent
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
        val strategy = TestAndroidAuthorizationStrategy(context, activity)

        try {
            strategy.testLaunchIntent(buildIntent(AuthorizationAgent.BROWSER))
            fail("Expected ClientException")
        } catch (e: ClientException) {
            assertEquals(ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME, e.errorCode)
        }
    }

    /**
     * WebView flow with a competing app registered → no exception (WebView is excluded from
     * URL scheme conflict validation).
     */
    @Test
    fun `launchIntent webview flow does not throw even when competing app is registered`() {
        registerCompetingApp()
        val strategy = TestAndroidAuthorizationStrategy(context, activity)

        // Should not throw — activity.startActivity() will simply be called.
        strategy.testLaunchIntent(buildIntent(AuthorizationAgent.WEBVIEW))
    }

    /**
     * Null authorizationAgent is treated as a browser flow → validation runs, and a conflict
     * causes [ClientException].
     */
    @Test
    fun `launchIntent null authorizationAgent treated as browser flow and throws on conflict`() {
        registerCompetingApp()
        val strategy = TestAndroidAuthorizationStrategy(context, activity)

        try {
            strategy.testLaunchIntent(buildIntent(agent = null))
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
        val strategy = TestAndroidAuthorizationStrategy(context, activity)

        // No competing app registered — should not throw (startActivity completes normally).
        strategy.testLaunchIntent(buildIntent(AuthorizationAgent.BROWSER))
    }
}
