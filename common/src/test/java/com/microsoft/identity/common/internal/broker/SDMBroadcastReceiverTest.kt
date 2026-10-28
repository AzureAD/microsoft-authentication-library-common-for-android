// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files (the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions:
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
package com.microsoft.identity.common.internal.broker

import android.content.Context
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.internal.activebrokerdiscovery.BrokerDiscoveryClientFactory
import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClient
import com.microsoft.identity.common.java.constants.SharedDeviceModeConstants
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkObject
import io.mockk.unmockkObject
import io.mockk.verify
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows
import org.robolectric.annotation.LooperMode
import org.robolectric.shadows.ShadowSystemClock
import java.time.Duration

@RunWith(RobolectricTestRunner::class)
@LooperMode(LooperMode.Mode.PAUSED)
class SDMBroadcastReceiverTest {
    private lateinit var context: Context
    private lateinit var callback: CountingCallback

    @Before
    fun setUp() {
        SDMBroadcastReceiver.resetThrottleStateForTest()
        context = ApplicationProvider.getApplicationContext()
        callback = CountingCallback()
    }

    @After
    fun tearDown() {
        SDMBroadcastReceiver.resetThrottleStateForTest()
    }

    @Test
    fun handleBroadcast_firstEventIsImmediate_duplicateIsDeliveredAtWindowBoundary() {
        handleBroadcast(SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT)
        handleBroadcast(SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT)

        assertEquals(1, callback.globalSignOutCount)

        advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS - 1)
        assertEquals(1, callback.globalSignOutCount)

        advanceMainLooperBy(1)
        assertEquals(2, callback.globalSignOutCount)
    }

    @Test
    fun runWithGlobalSignOutThrottle_manyDuplicates_deliversOnlyOneTrailingCallback() {
        var deliveryCount = 0

        repeat(10) {
            runThrottled {
                deliveryCount++
            }
        }

        assertEquals(1, deliveryCount)

        advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)
        assertEquals(2, deliveryCount)

        advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)
        assertEquals(2, deliveryCount)
    }

    @Test
    fun runWithGlobalSignOutThrottle_continuousDuplicates_useFixedWindows() {
        var deliveryCount = 0
        val delivery: () -> Unit = { deliveryCount++ }

        runThrottled(delivery)
        advanceMainLooperBy(4_000)
        repeat(5) {
            runThrottled(delivery)
        }

        advanceMainLooperBy(1_000)
        assertEquals(2, deliveryCount)

        advanceMainLooperBy(4_000)
        repeat(5) {
            runThrottled(delivery)
        }

        advanceMainLooperBy(1_000)
        assertEquals(3, deliveryCount)
    }

    @Test
    fun runWithGlobalSignOutThrottle_duplicates_deliversLatestOnMainLooper() {
        val deliveries = mutableListOf<String>()

        runThrottled { deliveries.add("first") }
        runThrottled { deliveries.add("superseded") }
        runThrottled {
            assertSame(Looper.getMainLooper(), Looper.myLooper())
            deliveries.add("latest")
        }

        assertEquals(listOf("first"), deliveries)
        advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)
        assertEquals(listOf("first", "latest"), deliveries)
    }

    @Test
    fun handleBroadcast_registrationStarted_isNotThrottledBySignOut() {
        repeat(2) {
            handleBroadcast(SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT)
            handleBroadcast(SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTRATION_START)
        }

        assertEquals(1, callback.globalSignOutCount)
        assertEquals(2, callback.registrationStartedCount)

        advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)
        assertEquals(2, callback.globalSignOutCount)
        assertEquals(2, callback.registrationStartedCount)
    }

    @Test
    fun handleBroadcast_registered_isNotThrottledAndStillRequiresBrokerState() {
        val brokerDiscoveryClient = mockk<IBrokerDiscoveryClient>()
        mockkObject(BrokerDiscoveryClientFactory.Companion)
        try {
            every {
                BrokerDiscoveryClientFactory.getInstanceForBrokerSdk(context, any())
            } returns brokerDiscoveryClient
            every { brokerDiscoveryClient.getActiveBroker(false) } returns null

            handleBroadcast(SharedDeviceModeConstants.BROADCAST_TYPE_GLOBAL_SIGN_OUT)
            repeat(2) {
                handleBroadcast(SharedDeviceModeConstants.BROADCAST_TYPE_SDM_REGISTERED)
            }

            verify(exactly = 2) { brokerDiscoveryClient.getActiveBroker(false) }
            assertEquals(0, callback.registeredCount)

            advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)
            verify(exactly = 2) { brokerDiscoveryClient.getActiveBroker(false) }
            assertEquals(0, callback.registeredCount)
        } finally {
            unmockkObject(BrokerDiscoveryClientFactory.Companion)
        }
    }

    @Test
    fun runWithGlobalSignOutThrottle_quietWindowClosesWithoutTrailingCallback() {
        var deliveryCount = 0
        val delivery: () -> Unit = { deliveryCount++ }

        runThrottled(delivery)
        advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)

        assertEquals(1, deliveryCount)

        runThrottled(delivery)
        assertEquals(2, deliveryCount)
    }

    @Test
    fun runWithGlobalSignOutThrottle_expiredWindowWithQueuedTimer_replacesStaleDelivery() {
        val deliveries = mutableListOf<String>()
        runThrottled { deliveries.add("first") }
        runThrottled { deliveries.add("stale") }

        ShadowSystemClock.advanceBy(
            Duration.ofMillis(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)
        )
        runThrottled { deliveries.add("new") }
        assertEquals(listOf("first", "new"), deliveries)

        advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)
        assertEquals(listOf("first", "new"), deliveries)
    }

    @Test
    fun handleBroadcast_nullAndUnknownTypesRemainIgnored() {
        handleBroadcast(null)
        handleBroadcast("unknown")

        assertEquals(0, callback.registrationStartedCount)
        assertEquals(0, callback.registeredCount)
        assertEquals(0, callback.globalSignOutCount)
    }

    @Test
    fun resetThrottleStateForTest_cancelsPendingDelivery() {
        var deliveryCount = 0
        val delivery: () -> Unit = { deliveryCount++ }

        runThrottled(delivery)
        runThrottled(delivery)
        SDMBroadcastReceiver.resetThrottleStateForTest()

        advanceMainLooperBy(SDMBroadcastReceiver.GLOBAL_SIGN_OUT_THROTTLE_WINDOW_MILLIS)
        assertEquals(1, deliveryCount)

        runThrottled(delivery)
        assertEquals(2, deliveryCount)
    }

    private fun handleBroadcast(broadcastType: String?) {
        val intent = Intent()
        broadcastType?.let {
            intent.putExtra(SharedDeviceModeConstants.BROADCAST_TYPE_KEY, it)
        }
        SDMBroadcastReceiver.handleSharedDeviceModeBroadCast(context, intent, callback)
    }

    private fun runThrottled(delivery: () -> Unit) {
        SDMBroadcastReceiver.runWithGlobalSignOutThrottle(Runnable(delivery))
    }

    private fun advanceMainLooperBy(durationMillis: Long) {
        Shadows.shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(durationMillis))
    }

    private class CountingCallback : SDMBroadcastReceiver.SharedDeviceModeCallback {
        var registrationStartedCount = 0
        var registeredCount = 0
        var globalSignOutCount = 0

        override fun onSharedDeviceModeRegistrationStarted() {
            registrationStartedCount++
        }

        override fun onSharedDeviceModeRegistered() {
            registeredCount++
        }

        override fun onGlobalSignOut() {
            globalSignOutCount++
        }
    }
}
