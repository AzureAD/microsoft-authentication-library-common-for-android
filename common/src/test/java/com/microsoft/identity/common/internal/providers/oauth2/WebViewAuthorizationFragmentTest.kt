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

import com.microsoft.identity.common.internal.ui.webview.challengehandlers.SwitchBrowserRequestHandler
import com.microsoft.identity.common.internal.ui.webview.switchbrowser.SwitchBrowserProtocolCoordinator
import com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.IS_SWITCH_BROWSER_FLOW
import com.microsoft.identity.common.java.providers.RawAuthorizationResult
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewAuthorizationFragmentTest {

    private fun createFragment(): WebViewAuthorizationFragment {
        return WebViewAuthorizationFragment()
    }

    @Test
    fun testPropertyBagContainsFlagWhenFlowWasInitiated() {
        val fragment = createFragment()
        val mockHandler = mock(SwitchBrowserRequestHandler::class.java)
        whenever(mockHandler.wasSwitchBrowserFlowInitiated).thenReturn(true)
        val coordinator = SwitchBrowserProtocolCoordinator(mockHandler)
        fragment.setSwitchBrowserProtocolCoordinator(coordinator)

        val result = RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
        val propertyBag = fragment.propertyBagFromAuthorizationResult(result)

        assertTrue(propertyBag.get<Boolean>(IS_SWITCH_BROWSER_FLOW) == true)
    }

    @Test
    fun testPropertyBagDoesNotContainFlagWhenFlowWasNotInitiated() {
        val fragment = createFragment()
        val mockHandler = mock(SwitchBrowserRequestHandler::class.java)
        whenever(mockHandler.wasSwitchBrowserFlowInitiated).thenReturn(false)
        val coordinator = SwitchBrowserProtocolCoordinator(mockHandler)
        fragment.setSwitchBrowserProtocolCoordinator(coordinator)

        val result = RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
        val propertyBag = fragment.propertyBagFromAuthorizationResult(result)

        assertNull(propertyBag.get<Boolean>(IS_SWITCH_BROWSER_FLOW))
    }

    @Test
    fun testPropertyBagDoesNotContainFlagWhenCoordinatorIsNull() {
        val fragment = createFragment()
        // coordinator is null by default

        val result = RawAuthorizationResult.fromResultCode(RawAuthorizationResult.ResultCode.CANCELLED)
        val propertyBag = fragment.propertyBagFromAuthorizationResult(result)

        assertNull(propertyBag.get<Boolean>(IS_SWITCH_BROWSER_FLOW))
    }
}
