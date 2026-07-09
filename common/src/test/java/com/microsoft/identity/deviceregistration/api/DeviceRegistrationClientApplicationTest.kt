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
package com.microsoft.identity.deviceregistration.api

import android.content.Context
import android.os.Bundle
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.internal.activebrokerdiscovery.IBrokerDiscoveryClient
import com.microsoft.identity.common.internal.broker.BrokerData
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle
import com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy
import com.microsoft.identity.common.java.interfaces.IPlatformComponents
import com.microsoft.identity.common.java.interfaces.IStorageSupplier
import com.microsoft.identity.deviceregistration.AndroidDeviceRegistrationProtocolPacker
import com.microsoft.identity.deviceregistration.DeviceRegistrationIpcStrategiesProvider
import com.microsoft.identity.deviceregistration.java.DeviceState
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecord
import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationRecord
import com.microsoft.identity.deviceregistration.java.protocol.parameters.PreProvisionedBlobV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.response.GetDeviceRegistrationRecordV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.GetDeviceRegistrationRecordsV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.GetRegistrationStateV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.PreProvisionedBlobV0Response
import com.microsoft.identity.deviceregistration.java.protocol.parameters.ProvisionResourceAccountCredentialsV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.response.ProvisionResourceAccountCredentialsV0Response
import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.common.java.request.SdkType
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import java.util.UUID

/**
 * Unit tests for [DeviceRegistrationClientApplication].
 * Uses mock IPC strategy to verify DRCA methods call the controller
 * with correct V0 params and return correctly parsed responses.
 */
@RunWith(RobolectricTestRunner::class)
class DeviceRegistrationClientApplicationTest {

    private lateinit var context: Context
    private val packer = AndroidDeviceRegistrationProtocolPacker()
    private val testBrokerPkg = "com.microsoft.test.broker"

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
    }

    private fun createDrca(strategy: IIpcStrategy): DeviceRegistrationClientApplication {
        val storageSupplier: IStorageSupplier = mock(defaultAnswer = Mockito.RETURNS_DEEP_STUBS)
        val components: IPlatformComponents = mock {
            whenever(it.storageSupplier).thenReturn(storageSupplier)
        }
        val discoveryClient: IBrokerDiscoveryClient = mock {
            whenever(it.getActiveBroker(false)).thenReturn(BrokerData(testBrokerPkg, "sig"))
        }
        val provider = object : DeviceRegistrationIpcStrategiesProvider(false) {
            override fun getStrategies(
                context: Context,
                components: IPlatformComponents,
                activeBrokerPackageName: String
            ): MutableList<IIpcStrategy> = mutableListOf(strategy)
        }
        return DeviceRegistrationClientApplication(context, components, discoveryClient, provider)
    }

    private fun successStrategy(responseBundle: Bundle): IIpcStrategy {
        val strategy: IIpcStrategy = mock()
        whenever(strategy.getType()).thenReturn(IIpcStrategy.Type.CONTENT_PROVIDER)
        whenever(strategy.communicateToBroker(any())).thenReturn(responseBundle)
        return strategy
    }

    @Test
    fun getPreProvisionedBlob_returnsJws() {
        val expectedJws = "test.jws.blob"
        val response = PreProvisionedBlobV0Response(UUID.randomUUID(), expectedJws)
        val drca = createDrca(successStrategy(packer.pack(response)))

        val result = drca.getPreProvisionedBlob("test-tenant", UUID.randomUUID())


        Assert.assertEquals(expectedJws, result)
    }

    @Test
    fun getRegistrationState_returnsDeviceState() {
        val response = GetRegistrationStateV0Response(UUID.randomUUID(), "DEVICE_VALID")
        val drca = createDrca(successStrategy(packer.pack(response)))

        val result = drca.getRegistrationState(
            DeviceRegistrationRecord("tenant", "upn", "device", false, false),
            UUID.randomUUID()
        )

        Assert.assertEquals(DeviceState.DEVICE_VALID, result)
    }

    @Test
    fun getRegistrationState_unknownState_returnsUnknown() {
        val response = GetRegistrationStateV0Response(UUID.randomUUID(), "SOME_NEW_STATE")
        val drca = createDrca(successStrategy(packer.pack(response)))

        val result = drca.getRegistrationState(
            DeviceRegistrationRecord("tenant", "upn", "device", false, false),
            UUID.randomUUID()
        )

        Assert.assertEquals(DeviceState.UNKNOWN, result)
    }

    @Test
    fun getDeviceRegistrationRecord_returnsRecord() {
        val record =
            DeviceRegistrationRecord("test-tenant", "user@test.com", "device-123", false, true)
        val response = GetDeviceRegistrationRecordV0Response(UUID.randomUUID(), record)
        val drca = createDrca(successStrategy(packer.pack(response)))

        val result = drca.getDeviceRegistrationRecord("test-tenant", UUID.randomUUID())


        Assert.assertNotNull(result)
        Assert.assertEquals("test-tenant", result!!.tenantId)
        Assert.assertEquals("user@test.com", result.upn)
        Assert.assertEquals("device-123", result.deviceId)
    }

    @Test
    fun getDeviceRegistrationRecord_notFound_returnsNull() {
        val response = GetDeviceRegistrationRecordV0Response(UUID.randomUUID(), null)
        val drca = createDrca(successStrategy(packer.pack(response)))

        val result = drca.getDeviceRegistrationRecord("unknown-tenant", UUID.randomUUID())


        Assert.assertNull(result)
    }

    @Test
    fun getAllEntries_returnsList() {
        val records = listOf<IDeviceRegistrationRecord>(
            DeviceRegistrationRecord("tenant1", "upn1", "dev1", false, false),
            DeviceRegistrationRecord("tenant2", "upn2", "dev2", true, false)
        )
        val response = GetDeviceRegistrationRecordsV0Response(UUID.randomUUID(), records)
        val drca = createDrca(successStrategy(packer.pack(response)))

        val result = drca.getAllEntries(UUID.randomUUID())


        Assert.assertEquals(2, result.size)
        Assert.assertEquals("tenant1", result[0].tenantId)
        Assert.assertEquals("tenant2", result[1].tenantId)
    }

    @Test
    fun correlationId_isPassedToParameters() {
        val correlationId = UUID.randomUUID()
        val response = PreProvisionedBlobV0Response(UUID.randomUUID(), "jws")
        val strategy: IIpcStrategy = mock()
        whenever(strategy.getType()).thenReturn(IIpcStrategy.Type.CONTENT_PROVIDER)
        whenever(strategy.communicateToBroker(any())).thenAnswer { invocation ->
            val bundle = (invocation.arguments[0] as BrokerOperationBundle).bundle
            val protocolData = bundle?.getByteArray("protocol.data")
            Assert.assertNotNull(protocolData)
            val parameters = PreProvisionedBlobV0Parameters.create(protocolData)
            Assert.assertEquals(correlationId, parameters.correlationId)
            // Return the packed response
            packer.pack(response)
        }

        val drca = createDrca(strategy)

        drca.getPreProvisionedBlob("test-tenant", correlationId)
        // If we get here without exception, the correlationId was accepted and the flow completed
    }

    @Test
    fun provisionResourceAccountCredentials_returnsAccountRecord() {
        val accountRecord = AccountRecord()
        accountRecord.homeAccountId = "uid.utid"
        accountRecord.localAccountId = "uid"
        accountRecord.username = "ra@test.com"
        accountRecord.environment = "login.microsoftonline.com"
        accountRecord.realm = "utid"
        val response = ProvisionResourceAccountCredentialsV0Response(UUID.randomUUID(), accountRecord)
        val drca = createDrca(successStrategy(packer.pack(response)))

        val result = drca.provisionResourceAccountCredentials("utid", "uid", UUID.randomUUID(), SdkType.MSAL, "1.0.0")

        Assert.assertNotNull(result)
        Assert.assertEquals("uid.utid", result.homeAccountId)
        Assert.assertEquals("uid", result.localAccountId)
        Assert.assertEquals("ra@test.com", result.username)
        Assert.assertEquals("login.microsoftonline.com", result.environment)
        Assert.assertEquals("utid", result.realm)
    }

    @Test
    fun provisionResourceAccountCredentials_passesParamsToIpc() {
        val correlationId = UUID.randomUUID()
        val accountRecord = AccountRecord()
        val response = ProvisionResourceAccountCredentialsV0Response(UUID.randomUUID(), accountRecord)
        val strategy: IIpcStrategy = mock()
        whenever(strategy.getType()).thenReturn(IIpcStrategy.Type.CONTENT_PROVIDER)
        whenever(strategy.communicateToBroker(any())).thenAnswer { invocation ->
            val bundle = (invocation.arguments[0] as BrokerOperationBundle).bundle
            val protocolData = bundle?.getByteArray("protocol.data")
            Assert.assertNotNull(protocolData)
            val parameters = ProvisionResourceAccountCredentialsV0Parameters.create(protocolData)
            Assert.assertEquals(correlationId, parameters.correlationId)
            Assert.assertEquals("test-tenant", parameters.tenantId)
            Assert.assertEquals("test-ra-oid", parameters.raObjectId)
            Assert.assertEquals("MSAL", parameters.sdkType)
            Assert.assertEquals("1.0.0", parameters.sdkVersion)
            Assert.assertEquals("PROD", parameters.drsDiscoveryEndpoint)
            packer.pack(response)
        }

        val drca = createDrca(strategy)
        drca.provisionResourceAccountCredentials("test-tenant", "test-ra-oid", correlationId, SdkType.MSAL, "1.0.0")
    }
}