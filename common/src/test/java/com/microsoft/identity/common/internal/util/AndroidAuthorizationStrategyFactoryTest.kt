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
import androidx.fragment.app.Fragment
import com.microsoft.identity.common.internal.ui.AndroidAuthorizationStrategyFactory
import com.microsoft.identity.common.internal.ui.browser.DefaultBrowserAuthorizationStrategy
import com.microsoft.identity.common.internal.ui.webview.EmbeddedWebViewAuthorizationStrategy
import com.microsoft.identity.common.java.browser.Browser
import com.microsoft.identity.common.java.providers.oauth2.IAuthorizationStrategy
import com.microsoft.identity.common.java.ui.AuthorizationAgent
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AndroidAuthorizationStrategyFactoryTest {

    companion object {
        private val browser = mock(Browser::class.java)
    }

    @Test
    fun `test getAuthorizationStrategy with authorization agent WEBVIEW`() {
        val strategy = getAuthorizationStrategy(
            authorizationAgent = AuthorizationAgent.WEBVIEW,
            browser = browser,
        )
        assert(strategy is EmbeddedWebViewAuthorizationStrategy)
    }

    @Test
    fun `test getAuthorizationStrategy with authorization agent WEBVIEW, browser null`() {
        val strategy = getAuthorizationStrategy(
            authorizationAgent = AuthorizationAgent.WEBVIEW,
            browser = null,
        )
        assert(strategy is EmbeddedWebViewAuthorizationStrategy)
    }

    @Test
    fun `test getAuthorizationStrategy with authorization agent BROWSER`() {
        val strategy = getAuthorizationStrategy(
            authorizationAgent = AuthorizationAgent.BROWSER,
            browser = browser,
        )
        assert(strategy is DefaultBrowserAuthorizationStrategy)
    }


    @Test
    fun `test getAuthorizationStrategy with authorization agent BROWSER, browser null`() {
        val strategy = getAuthorizationStrategy(
            authorizationAgent = AuthorizationAgent.BROWSER,
            browser = null,
        )
        assert(strategy is EmbeddedWebViewAuthorizationStrategy)
    }

    private fun getAuthorizationStrategy(
        browser: Browser?,
        authorizationAgent: AuthorizationAgent,
    ): IAuthorizationStrategy<*, *> {
        // Construct the factory
        val strategyFactory = AndroidAuthorizationStrategyFactory.builder()
            .context(org.robolectric.RuntimeEnvironment.getApplication())
            .activity(mock(Activity::class.java))
            .fragment(mock(Fragment::class.java))
            .browserSelector { _, _ -> browser }
            .build()

        return strategyFactory.getAuthorizationStrategy(
            authorizationAgent,
            emptyList(),
            null,
            true
        )
    }
}
