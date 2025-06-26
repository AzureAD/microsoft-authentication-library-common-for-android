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

import android.os.Bundle
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.WEB_VIEW_PRESERVE_FLOW_ON_SSL_ERROR
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class WebViewAuthorizationFragmentTest {
    @Test
    fun testExtractState_NewSslErrorHandlerEnabled() {
        val fragment = WebViewAuthorizationFragment()

        val bundle = Bundle().apply {
            putBoolean(WEB_VIEW_PRESERVE_FLOW_ON_SSL_ERROR, true)
        }

        fragment.extractState(bundle)
        assertTrue(fragment.shouldPreserveWebViewFlowOnSslError())
    }

    @Test
    fun testExtractState_NewSslErrorHandlerDisabled() {
        val fragment = WebViewAuthorizationFragment()

        val bundle = Bundle().apply {
            putBoolean(WEB_VIEW_PRESERVE_FLOW_ON_SSL_ERROR, false)
        }

        fragment.extractState(bundle)
        assertFalse(fragment.shouldPreserveWebViewFlowOnSslError())
    }

    @Test
    fun testExtractState_NewSslErrorHandlerNotSet() {
        val fragment = WebViewAuthorizationFragment()

        val bundle = Bundle()

        fragment.extractState(bundle)

        // default value from flight
        assertFalse(fragment.shouldPreserveWebViewFlowOnSslError())
    }

    @Test
    fun testExtractState_NewSslErrorHandlerNotSet_Flight_Enabled() {
        val fragment = WebViewAuthorizationFragment()
        val flightsManger: IFlightsManager = mock()
        val flightsProvider: IFlightsProvider = mock()
        whenever(flightsProvider.isFlightEnabled(eq(CommonFlight.SHOULD_PRESERVE_WEBVIEW_FLOW_ON_SSL_ERROR))).thenReturn(
            true
        )
        whenever(flightsManger.getFlightsProvider()).thenReturn(flightsProvider)
        CommonFlightsManager.initializeCommonFlightsManager(
            flightsManger
        )

        val bundle = Bundle()

        fragment.extractState(bundle)

        assertTrue(fragment.shouldPreserveWebViewFlowOnSslError())
        CommonFlightsManager.resetFlightsManager()
    }

    @Test
    fun testExtractState_NewSslErrorHandlerNotSet_Flight_Disabled() {
        val fragment = WebViewAuthorizationFragment()
        val flightsManger: IFlightsManager = mock()
        val flightsProvider: IFlightsProvider = mock()
        whenever(flightsProvider.isFlightEnabled(eq(CommonFlight.SHOULD_PRESERVE_WEBVIEW_FLOW_ON_SSL_ERROR))).thenReturn(
            false
        )
        whenever(flightsManger.getFlightsProvider()).thenReturn(flightsProvider)
        CommonFlightsManager.initializeCommonFlightsManager(
            flightsManger
        )
        fragment.extractState(Bundle())

        assertFalse(fragment.shouldPreserveWebViewFlowOnSslError())
        CommonFlightsManager.resetFlightsManager()
    }
}