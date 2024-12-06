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

package com.microsoft.identity.common.internal.util

import android.app.Activity
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ActivityInfo
import android.content.pm.ApplicationInfo
import android.content.pm.PackageInfo
import android.content.pm.ResolveInfo
import android.content.pm.Signature
import android.net.Uri
import androidx.fragment.app.Fragment
import com.microsoft.identity.common.internal.ui.AndroidAuthorizationStrategyFactory
import com.microsoft.identity.common.internal.ui.browser.DefaultBrowserAuthorizationStrategy
import com.microsoft.identity.common.internal.ui.webview.EmbeddedWebViewAuthorizationStrategy
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy
import com.microsoft.identity.common.java.ui.AuthorizationAgent
import com.microsoft.identity.common.java.ui.BrowserDescriptor
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
// We need this to add the PackageManager.MATCH_DEFAULT_ONLY flag on BrowserSelector.getBrowsers
// and to avoid a NPE on PackageHelper.getSignatures.
@Config(sdk = [27])
class AndroidAuthorizationStrategyFactoryTest {

    companion object {
        private const val PACKAGE_NAME = "com.android.chrome"
        private const val BROWSER_NAME = "Chrome"
        private const val SIGNATURE_HASH_HEX = "3082010A0282010100C3B3A700D1E020302020034A7B8888"
        private const val SIGNATURE_HASH_BASE_64 = "Lu2NRuBdl7odm7sAKREJMShwDFWO7piPO_K69PxPWghQSaboLhOI2fvJt-Q17dW9NTgyPhOopWS6Cxgi9wrTew=="
        private const val VERSION_LOWER_BOUND = "0"
        private const val VERSION_UPPER_BOUND = "1"
        private val fakeBrowser = BrowserDescriptor(
            PACKAGE_NAME,
            SIGNATURE_HASH_BASE_64,
            VERSION_LOWER_BOUND,
            VERSION_UPPER_BOUND
        )
    }

    @Test
    fun `test getAuthorizationStrategy with authorization agent WEBVIEW, empty browser safe list`() {
        val strategy = getAuthorizationStrategy(
            authorizationAgent = AuthorizationAgent.WEBVIEW,
            browserSafeList = emptyList()
        )
        assert(strategy is EmbeddedWebViewAuthorizationStrategy)
    }

    @Test
    fun `test getAuthorizationStrategy with authorization agent BROWSER, empty browser safe list`() {
        val strategy = getAuthorizationStrategy(
            authorizationAgent = AuthorizationAgent.BROWSER,
            browserSafeList = emptyList()
        )
        assert(strategy is EmbeddedWebViewAuthorizationStrategy)
    }

    @Test
    fun `test getAuthorizationStrategy with authorization agent WEBVIEW, fakeBrowser in safe list`() {
        val strategy = getAuthorizationStrategy(
            authorizationAgent = AuthorizationAgent.WEBVIEW,
            browserSafeList = listOf(fakeBrowser)
        )
        assert(strategy is EmbeddedWebViewAuthorizationStrategy)
    }

    @Test
    fun `test getAuthorizationStrategy with authorization agent BROWSER, fakeBrowser in safe list`() {
        val strategy = getAuthorizationStrategy(
            authorizationAgent = AuthorizationAgent.BROWSER,
            browserSafeList= listOf(fakeBrowser)
        )
        assert(strategy is DefaultBrowserAuthorizationStrategy)
    }

    private fun getAuthorizationStrategy(
        browserSafeList: List<BrowserDescriptor>,
        authorizationAgent: AuthorizationAgent
    ): IAuthorizationStrategy<*, *> {

        val context = org.robolectric.RuntimeEnvironment.getApplication()
        val shadowPackageManager = Shadows.shadowOf(context.packageManager)

        // Define a mock browser package
        val browserPackageInfo = PackageInfo().apply {
            packageName = PACKAGE_NAME
            versionName = "1.0"
            // Add signatures
            signatures = arrayOf(
                Signature(SIGNATURE_HASH_HEX)
            )
            applicationInfo = ApplicationInfo().apply {
                this.packageName = PACKAGE_NAME
                this.name = BROWSER_NAME
            }
        }

        // Add the browser package to the shadow PackageManager
        shadowPackageManager.installPackage(browserPackageInfo)

        // Create a mock ResolveInfo for browser activities
        val browserResolveInfo = ResolveInfo().apply {
            activityInfo = ActivityInfo().apply {
                packageName = PACKAGE_NAME
                name = BROWSER_NAME
            }
            isDefault = true

            // Add a filter for handling specific intents (e.g., http/https URLs)
            filter = IntentFilter().apply {
                addAction(Intent.ACTION_VIEW) // Action to view content
                addCategory(Intent.CATEGORY_BROWSABLE) // Default category
                addDataScheme("http") // Filter for http URLs
                addDataScheme("https") // Filter for https URLs
            }
        }

        // Add the browser to handle VIEW intents with http/https schemes
        shadowPackageManager.addResolveInfoForIntent(
            Intent(Intent.ACTION_VIEW, Uri.parse("http://www.example.com")),
            browserResolveInfo
        )

        // Construct the factory
        val strategyFactory = AndroidAuthorizationStrategyFactory.builder()
            .context(context)
            .activity(mock(Activity::class.java))
            .fragment(mock(Fragment::class.java))
            .build()

        // Mock the parameters
        val params: InteractiveTokenCommandParameters =
            mock(InteractiveTokenCommandParameters::class.java)
        `when`(params.browserSafeList).thenReturn(browserSafeList)
        `when`(params.preferredBrowser).thenReturn(null)
        `when`(params.authorizationAgent).thenReturn(authorizationAgent)

        return strategyFactory.getAuthorizationStrategy(params)
    }
}