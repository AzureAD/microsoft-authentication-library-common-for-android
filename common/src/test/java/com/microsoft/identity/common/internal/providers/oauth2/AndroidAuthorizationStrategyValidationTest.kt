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
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import com.microsoft.identity.common.adal.internal.AuthenticationConstants
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REDIRECT_URI
import com.microsoft.identity.common.internal.ui.browser.BrowserAuthorizationStrategy
import com.microsoft.identity.common.java.browser.Browser
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationRequest
import com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy
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
     * Browser flow with a competing app registered but Intune as the calling package →
     * multiple-app URL scheme validation is skipped, so no [ClientException] is thrown.
     * This validates the COBO / COPE exemption where Authenticator also listens for the
     * broker redirect and would otherwise trigger a false conflict.
     */
    @Test
    fun `launchIntent browser flow skips competing app validation for Intune package`() {
        registerCompetingApp()
        val intuneContext = object : ContextWrapper(context) {
            override fun getPackageName(): String = AuthenticationConstants.Broker.INTUNE_APP_PACKAGE_NAME
        }

        val strategy = TestBrowserAuthorizationStrategy(intuneContext, activity)

        // A competing app is registered, but because the calling package is Intune the
        // validation is skipped and launchIntent completes without throwing.
        strategy.testLaunchIntent(buildIntent())
    }

    /**
     * Browser flow with a competing app registered and a non-Intune calling package →
     * [ClientException] must still be thrown, ensuring the Intune exemption does not
     * relax validation for other apps.
     */
    @Test
    fun `launchIntent browser flow throws for non-Intune package when competing app is registered`() {
        registerCompetingApp()
        val nonIntuneContext = object : ContextWrapper(context) {
            override fun getPackageName(): String = COMPETING_PACKAGE
        }

        val strategy = TestBrowserAuthorizationStrategy(nonIntuneContext, activity)

        try {
            strategy.testLaunchIntent(buildIntent())
            fail("Expected ClientException")
        } catch (e: ClientException) {
            assertEquals(ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME, e.errorCode)
        }
    }
}
