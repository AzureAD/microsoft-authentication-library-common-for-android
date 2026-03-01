//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.sharedwithoneauth

import android.os.Bundle
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.internal.broker.ipc.IpcStrategyWithRetry
import com.microsoft.identity.common.internal.mocks.MockCommonFlightsManager
import com.microsoft.identity.common.java.flighting.CommonFlight
import com.microsoft.identity.common.java.flighting.CommonFlightsManager
import com.microsoft.identity.common.java.flighting.IFlightsProvider
import org.junit.After
import org.junit.Assert
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.Mockito.`when`
import org.robolectric.RobolectricTestRunner

/**
 * Tests for [OneAuthSharedFunctions.maybeWrapWithRetry] wiring logic.
 */
@RunWith(RobolectricTestRunner::class)
class OneAuthSharedFunctionsTest {

    private val mockStrategy = object : IIpcStrategy {
        override fun communicateToBroker(bundle: BrokerOperationBundle): Bundle? = Bundle()
        override fun isSupportedByTargetedBroker(targetedBrokerPackageName: String) = true
        override fun getType() = IIpcStrategy.Type.CONTENT_PROVIDER
    }

    @After
    fun tearDown() {
        CommonFlightsManager.resetFlightsManager()
    }

    @Test
    fun maybeWrapWithRetry_whenRetryDisabled_returnsOriginalStrategy() {
        val flightsProvider = Mockito.mock(IFlightsProvider::class.java)
        `when`(flightsProvider.isFlightEnabled(CommonFlight.IPC_RETRY_ENABLED)).thenReturn(false)
        val manager = MockCommonFlightsManager()
        manager.setMockCommonFlightsProvider(flightsProvider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)

        val result = OneAuthSharedFunctions.maybeWrapWithRetry("testTag", mockStrategy)

        Assert.assertFalse(
            "Strategy should NOT be wrapped when IPC_RETRY_ENABLED is false",
            result is IpcStrategyWithRetry
        )
        Assert.assertSame(mockStrategy, result)
    }

    @Test
    fun maybeWrapWithRetry_whenRetryEnabled_returnsIpcStrategyWithRetry() {
        val maxRetries = 3
        val baseDelayMs = 500
        val flightsProvider = Mockito.mock(IFlightsProvider::class.java)
        `when`(flightsProvider.isFlightEnabled(CommonFlight.IPC_RETRY_ENABLED)).thenReturn(true)
        `when`(flightsProvider.getIntValue(CommonFlight.IPC_RETRY_MAX_ATTEMPTS)).thenReturn(maxRetries)
        `when`(flightsProvider.getIntValue(CommonFlight.IPC_RETRY_BASE_DELAY_MS)).thenReturn(baseDelayMs)
        val manager = MockCommonFlightsManager()
        manager.setMockCommonFlightsProvider(flightsProvider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)

        val result = OneAuthSharedFunctions.maybeWrapWithRetry("testTag", mockStrategy)

        Assert.assertTrue(
            "Strategy should be wrapped with IpcStrategyWithRetry when IPC_RETRY_ENABLED is true",
            result is IpcStrategyWithRetry
        )
    }

    @Test
    fun maybeWrapWithRetry_whenRetryEnabled_preservesStrategyType() {
        val flightsProvider = Mockito.mock(IFlightsProvider::class.java)
        `when`(flightsProvider.isFlightEnabled(CommonFlight.IPC_RETRY_ENABLED)).thenReturn(true)
        `when`(flightsProvider.getIntValue(CommonFlight.IPC_RETRY_MAX_ATTEMPTS)).thenReturn(2)
        `when`(flightsProvider.getIntValue(CommonFlight.IPC_RETRY_BASE_DELAY_MS)).thenReturn(0)
        val manager = MockCommonFlightsManager()
        manager.setMockCommonFlightsProvider(flightsProvider)
        CommonFlightsManager.initializeCommonFlightsManager(manager)

        val result = OneAuthSharedFunctions.maybeWrapWithRetry("testTag", mockStrategy)

        Assert.assertEquals(
            "Wrapped strategy should delegate getType() to the underlying strategy",
            IIpcStrategy.Type.CONTENT_PROVIDER,
            result.getType()
        )
    }
}
