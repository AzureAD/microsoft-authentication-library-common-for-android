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
import android.content.pm.ActivityInfo
import android.content.pm.ResolveInfo
import android.net.Uri
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.exception.ErrorStrings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.fail
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.shadows.ShadowPackageManager

/**
 * Unit tests for [BrowserRedirectValidator].
 */
@RunWith(RobolectricTestRunner::class)
class BrowserRedirectValidatorTest {

    companion object {
        private const val COMPETING_PACKAGE = "com.example.otherapp"
        private const val COMPETING_ACTIVITY = "com.example.otherapp.SomeActivity"
    }

    private val context: Context = RuntimeEnvironment.getApplication()
    private val redirectUri = "msauth://com.example.myapp/redirect"
    private val appPackageName = context.packageName

    // These must stay in sync with the private constants in BrowserRedirectValidator.
    private val browserTabActivityClass = "com.microsoft.identity.client.BrowserTabActivity"
    private val currentTaskBrowserTabActivityClass =
        "com.microsoft.identity.client.CurrentTaskBrowserTabActivity"

    // ===================== Helper Functions =====================

    /**
     * Builds a ResolveInfo whose activityInfo describes [packageName]/[activityClassName].
     */
    private fun buildResolveInfo(packageName: String, activityClassName: String): ResolveInfo {
        val resolveInfo = ResolveInfo()
        resolveInfo.activityInfo = ActivityInfo().apply {
            this.packageName = packageName
            this.name = activityClassName
        }
        return resolveInfo
    }

    /**
     * Returns the ShadowPackageManager for [context].
     */
    private fun getShadowPackageManager(): ShadowPackageManager {
        return shadowOf(context.packageManager)
    }

    /**
     * Adds a resolve info entry so that [queryIntentActivities] returns [resolveInfo] for
     * ACTION_VIEW intents targeting [redirectUri].
     */
    private fun registerResolveInfoForRedirectUri(resolveInfo: ResolveInfo) {
        val matchIntent = Intent(Intent.ACTION_VIEW).apply {
            data = Uri.parse(redirectUri)
            addCategory(Intent.CATEGORY_DEFAULT)
            addCategory(Intent.CATEGORY_BROWSABLE)
        }
        getShadowPackageManager().addResolveInfoForIntent(matchIntent, resolveInfo)
    }

    // ===================== Tests =====================

    @Test
    fun `validateNoMultipleAppsListening passes when no activities are registered`() {
        // Default Robolectric PackageManager returns an empty list — no exception expected.
        BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, false)
    }

    @Test
    fun `validateNoMultipleAppsListening passes when only our BrowserTabActivity is registered`() {
        registerResolveInfoForRedirectUri(
            buildResolveInfo(appPackageName, browserTabActivityClass)
        )

        // Should not throw.
        BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, false)
    }

    @Test
    fun `validateNoMultipleAppsListening passes when only our CurrentTaskBrowserTabActivity is registered with useCurrentTask`() {
        registerResolveInfoForRedirectUri(
            buildResolveInfo(appPackageName, currentTaskBrowserTabActivityClass)
        )

        // Should not throw.
        BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, true)
    }

    @Test
    fun `validateNoMultipleAppsListening throws when another app is registered`() {
        registerResolveInfoForRedirectUri(buildResolveInfo(COMPETING_PACKAGE, COMPETING_ACTIVITY))

        try {
            BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, false)
            fail("Expected ClientException to be thrown")
        } catch (e: ClientException) {
            assertEquals(
                ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME,
                e.errorCode
            )
            assertNotNull(e.message)
        }
    }

    @Test
    fun `validateNoMultipleAppsListening throws with correct error code containing other package name`() {
        registerResolveInfoForRedirectUri(buildResolveInfo(COMPETING_PACKAGE, COMPETING_ACTIVITY))

        try {
            BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, false)
            fail("Expected ClientException to be thrown")
        } catch (e: ClientException) {
            assertNotNull(e.message)
            assert(e.message!!.contains(COMPETING_PACKAGE)) {
                "Error message should contain the other app's package name"
            }
        }
    }

    @Test
    fun `validateNoMultipleAppsListening throws when our BrowserTabActivity and another app are both registered`() {
        // Our expected activity is registered first, then another app.
        registerResolveInfoForRedirectUri(
            buildResolveInfo(appPackageName, browserTabActivityClass)
        )
        registerResolveInfoForRedirectUri(
            buildResolveInfo(COMPETING_PACKAGE, COMPETING_ACTIVITY)
        )

        // Should throw because the other app is also registered.
        try {
            BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, false)
            fail("Expected ClientException to be thrown")
        } catch (e: ClientException) {
            assertEquals(
                ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME,
                e.errorCode
            )
        }
    }

    @Test
    fun `validateNoMultipleAppsListening useCurrentTask true uses CurrentTaskBrowserTabActivity class`() {
        // Register CurrentTaskBrowserTabActivity for our package — this is the expected activity for useCurrentTask=true.
        registerResolveInfoForRedirectUri(
            buildResolveInfo(appPackageName, currentTaskBrowserTabActivityClass)
        )

        // Should not throw since only our expected activity is registered.
        BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, useCurrentTask = true)
    }

    @Test
    fun `validateNoMultipleAppsListening useCurrentTask false uses BrowserTabActivity class`() {
        // Register BrowserTabActivity for our package — expected for useCurrentTask=false.
        registerResolveInfoForRedirectUri(
            buildResolveInfo(appPackageName, browserTabActivityClass)
        )

        // Should not throw since only our expected activity is registered.
        BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, useCurrentTask = false)
    }

    @Test
    fun `validateNoMultipleAppsListening throws when our CurrentTaskBrowserTabActivity registered but useCurrentTask is false`() {
        // Registered: CurrentTaskBrowserTabActivity — but validator looks for BrowserTabActivity (useCurrentTask=false).
        // So this entry is treated as "other" → should throw.
        registerResolveInfoForRedirectUri(
            buildResolveInfo(appPackageName, currentTaskBrowserTabActivityClass)
        )

        try {
            BrowserRedirectValidator.validateNoMultipleAppsListening(context, redirectUri, useCurrentTask = false)
            fail("Expected ClientException to be thrown")
        } catch (e: ClientException) {
            assertEquals(
                ErrorStrings.MULTIPLE_APPS_LISTENING_CUSTOM_URL_SCHEME,
                e.errorCode
            )
        }
    }

    @Test
    fun `validateNoMultipleAppsListening handles null packageManager gracefully`() {
        val mockContext = mock(Context::class.java)
        `when`(mockContext.packageManager).thenReturn(null)
        `when`(mockContext.packageName).thenReturn(appPackageName)

        // Should return without throwing when packageManager is null.
        BrowserRedirectValidator.validateNoMultipleAppsListening(mockContext, redirectUri, false)
    }
}
