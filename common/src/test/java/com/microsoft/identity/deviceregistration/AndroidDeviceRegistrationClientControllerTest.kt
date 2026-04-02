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
package com.microsoft.identity.deviceregistration

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.deviceregistration.testprotocols.TestHappyPathV0Parameters
import com.microsoft.identity.deviceregistration.testprotocols.TestHappyPathV0Response
import com.microsoft.identity.common.exception.BrokerCommunicationException
import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClient
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.internal.cache.ActiveBrokerCacheUpdater
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecord
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecordWithAccount
import com.microsoft.identity.deviceregistration.java.exception.DeviceRegistrationException
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.UUID
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [AndroidDeviceRegistrationClientController] in the common module.
 * Uses mock IPC strategies to verify controller behavior without broker dependencies.
 */
@RunWith(RobolectricTestRunner::class)
class AndroidDeviceRegistrationClientControllerTest {

    private lateinit var context: Context
    private val backgroundExecutor = Executors.newSingleThreadExecutor()
    private val testBrokerPackageName = "com.microsoft.test.broker"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createController(
        strategies: List<IIpcStrategy>,
        supportsBoundService: Boolean = false
    ): AndroidDeviceRegistrationClientController {
        val provider = object : DeviceRegistrationIpcStrategiesProvider(supportsBoundService) {
            override fun getStrategies(
                context: Context,
                components: IPlatformComponents,
                activeBrokerPackageName: String
            ): MutableList<IIpcStrategy> = strategies.toMutableList()
        }
        val components: IPlatformComponents = mock()
        val discoveryClient: IBrokerDiscoveryClient = mock {
            whenever(it.getActiveBroker(false)).thenReturn(
                BrokerData(testBrokerPackageName, "test_sig")
            )
        }
        val cacheUpdater: ActiveBrokerCacheUpdater = mock()
        return AndroidDeviceRegistrationClientController(
            context,
            components,
            discoveryClient,
            provider,
            cacheUpdater
        )
    }

    private val packer = AndroidDeviceRegistrationProtocolPacker()

    private val testRecord = DeviceRegistrationRecord("tenant", "upn", "deviceId", false, false)
    private val testRecordWithAccount = DeviceRegistrationRecordWithAccount("account", "tenant", "upn", "deviceId", false, false)

    private fun testParams() = TestHappyPathV0Parameters(
        "hello ", "world", true, false, 1.4f, 3.7f, testRecord, testRecordWithAccount
    )

    private fun testResponse() = TestHappyPathV0Response(
        UUID.randomUUID(), "hello world", 1.4f * 3.7f, true,
        DeviceRegistrationRecord("tenantX", "upnX", "deviceIdX", false, false)
    )

    private fun packTestResponse(): Bundle = packer.pack(testResponse())

    private fun <T> runOnBackground(operation: () -> T): T {
        val future = backgroundExecutor.submit(operation)
        return future.get(5, TimeUnit.SECONDS)
    }

    private fun successStrategy(responseBundle: Bundle): IIpcStrategy = mock {
        whenever(it.getType()).thenReturn(IIpcStrategy.Type.CONTENT_PROVIDER)
        whenever(it.communicateToBroker(any())).thenReturn(responseBundle)
    }

    private fun failingStrategy(): IIpcStrategy = mock {
        whenever(it.getType()).thenReturn(IIpcStrategy.Type.CONTENT_PROVIDER)
        whenever(it.communicateToBroker(any())).thenThrow(
            BrokerCommunicationException(
                BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
                IIpcStrategy.Type.CONTENT_PROVIDER,
                null,
                null
            )
        )
    }

    @Test
    fun testExecuteSucceedsWithFirstStrategy() {
        val controller = createController(listOf(successStrategy(packTestResponse())))

        val result = runOnBackground { controller.execute(testParams()) }

        val response = TestHappyPathV0Response.create(result)
        assertEquals("hello world", response.concatenatedStrings)
        assertEquals(1.4f * 3.7f, response.multiplicationResult, 0.001f)
        assertTrue(response.isLogicalOr)
        assertEquals("tenantX", response.sameRecordWithConcatenatedX.tenantId)
    }

    @Test
    fun testExecuteFallsBackToSecondStrategy() {
        val controller = createController(
            listOf(failingStrategy(), successStrategy(packTestResponse()))
        )

        val result = runOnBackground { controller.execute(testParams()) }

        val response = TestHappyPathV0Response.create(result)
        assertEquals("hello world", response.concatenatedStrings)
    }

    @Test
    fun testExecuteThrowsWhenAllStrategiesFail() {
        val controller = createController(listOf(failingStrategy(), failingStrategy()))

        val exception = assertThrows(DeviceRegistrationException::class.java) {
            runOnBackground { controller.execute(testParams()) }
        }

        assertEquals(DeviceRegistrationException.FAILED_TO_COMMUNICATE_WITH_BROKER_ERROR_CODE, exception.errorCode)
    }

    @Test
    fun testExecuteThrowsWhenNoStrategies() {
        val controller = createController(emptyList())

        val exception = assertThrows(DeviceRegistrationException::class.java) {
            runOnBackground { controller.execute(testParams()) }
        }

        assertEquals(DeviceRegistrationException.FAILED_TO_COMMUNICATE_WITH_BROKER_ERROR_CODE, exception.errorCode)
    }

    @Test
    fun testExecuteThrowsOnMainThreadWhenBoundServiceSupported() {
        val controller = createController(
            listOf(successStrategy(packTestResponse())),
            supportsBoundService = true
        )

        val exception = assertThrows(ClientException::class.java) {
            controller.execute(testParams())
        }

        assertEquals(ClientException.CALLED_ON_MAIN_THREAD, exception.errorCode)
    }

    @Test
    fun testExecuteAllowsMainThreadWhenBoundServiceNotSupported() {
        val controller = createController(
            listOf(successStrategy(packTestResponse())),
            supportsBoundService = false
        )

        val result = controller.execute(testParams())

        val response = TestHappyPathV0Response.create(result)
        assertEquals("hello world", response.concatenatedStrings)
    }

    @Test
    fun testExecuteWithRecordsSerialization() {
        val controller = createController(listOf(successStrategy(packTestResponse())))

        val result = runOnBackground { controller.execute(testParams()) }

        val response = TestHappyPathV0Response.create(result)
        assertEquals("tenantX", response.sameRecordWithConcatenatedX.tenantId)
        assertEquals("upnX", response.sameRecordWithConcatenatedX.upn)
        assertEquals("deviceIdX", response.sameRecordWithConcatenatedX.deviceId)
    }

    @Test
    fun testConstructorThrowsWhenNoBrokerFound() {
        val discoveryClient: IBrokerDiscoveryClient = mock {
            whenever(it.getActiveBroker(false)).thenReturn(null)
        }
        val components: IPlatformComponents = mock()
        val provider = DeviceRegistrationIpcStrategiesProvider()
        val cacheUpdater: ActiveBrokerCacheUpdater = mock()

        val exception = assertThrows(ClientException::class.java) {
            AndroidDeviceRegistrationClientController(
                context, components, discoveryClient, provider, cacheUpdater
            )
        }

        assertEquals(ClientException.NOT_VALID_BROKER_FOUND, exception.errorCode)
    }
}
