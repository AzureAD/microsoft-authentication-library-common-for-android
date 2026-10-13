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

package com.microsoft.identity.deviceregistration.java.packer

import com.microsoft.identity.common.java.dto.AccountRecord
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecord
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecordWithAccount
import com.microsoft.identity.deviceregistration.java.protocol.DeviceRegistrationProtocolConstants
import com.microsoft.identity.deviceregistration.java.protocol.IDeviceRegistrationProtocol
import com.microsoft.identity.deviceregistration.java.protocol.parameters.DeviceRegistrationPreAuthorizedV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.PreProvisionedBlobV0Parameters
import com.microsoft.identity.deviceregistration.java.protocol.parameters.PreprovisioningBlobV1Parameters
import com.microsoft.identity.deviceregistration.java.protocol.response.DeviceRegistrationPreAuthorizedV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.PreProvisionedBlobV0Response
import com.microsoft.identity.deviceregistration.java.protocol.response.PreprovisioningBlobV1Response
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class DeviceRegistrationV1ProtocolTest {
    private val correlationId = UUID.fromString("5f3a7734-7554-4a99-aed3-309093ce80cf")

    @Test
    fun protocolIdentifiersMatchOperationsAndPreserveV0() {
        assertEquals(
            "protocol.get.pre.provisioned.blob.v1",
            DeviceRegistrationProtocolConstants.GET_PRE_PROVISIONED_BLOB_V1
        )
        assertEquals(
            "protocol.register.device.and.resource.account.with.preauthorized.challenge.v0",
            DeviceRegistrationProtocolConstants.REGISTER_DEVICE_AND_RESOURCE_ACCOUNT_WITH_PREAUTHORIZED_CHALLENGE_V0
        )
        assertEquals(
            "protocol.get.pre.provisioned.blob.v0",
            DeviceRegistrationProtocolConstants.GET_PRE_PROVISIONED_BLOB_V0
        )
        assertEquals(
            "protocol.device.registration.preauthorized.v0",
            DeviceRegistrationProtocolConstants.DEVICE_REGISTRATION_PREAUTHORIZED_V0
        )
    }

    @Test
    fun protocolTypeVersionsMatchWireVersions() {
        val protocols = listOf<IDeviceRegistrationProtocol>(
            PreprovisioningBlobV1Parameters(correlationId, "tenant", null, "PROD"),
            PreprovisioningBlobV1Response(correlationId, "signed-blob"),
            RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters(
                correlationId, "tenant", "resource", "challenge", false, "MSAL", "1.0", "PROD"
            ),
            RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response(
                correlationId, DeviceRegistrationRecord("tenant", "", "device", false, true), AccountRecord()
            )
        )
        val versionSuffix = Regex("V(\\d+)(Parameters|Response)$")

        for (protocol in protocols) {
            val typeName = protocol.javaClass.simpleName
            val version = requireNotNull(versionSuffix.find(typeName)).groupValues[1]
            assertEquals(typeName, "v$version", protocol.protocolName.substringAfterLast('.'))
        }
    }

    @Test
    fun protocolConstantVersionsMatchWireVersions() {
        val versionSuffix = Regex("_V(\\d+)$")
        for (field in DeviceRegistrationProtocolConstants::class.java.fields) {
            val version = versionSuffix.find(field.name)?.groupValues?.get(1) ?: continue
            val protocolName = field.get(null) as String
            assertEquals(field.name, "v$version", protocolName.substringAfterLast('.'))
        }
    }

    @Test
    fun blobParametersRoundTrip() {
        val request = PreprovisioningBlobV1Parameters(correlationId, "tenant", "resource", "PPE")
        val restored = PreprovisioningBlobV1Parameters.create(request.serialize())

        assertEquals(correlationId, restored.correlationId)
        assertEquals("tenant", restored.tenantId)
        assertEquals("resource", restored.resourceAccountId)
        assertEquals("PPE", restored.discoveryEndpointName)
        assertEquals(DeviceRegistrationProtocolConstants.GET_PRE_PROVISIONED_BLOB_V1, restored.protocolName)
        assertTrue(String(request.serialize(), Charsets.UTF_8).contains("\"mResourceAccountId\":\"resource\""))
    }

    @Test
    fun blobParametersRoundTripWithoutResourceAccount() {
        val request = PreprovisioningBlobV1Parameters(correlationId, "tenant", null, "PPE")
        val restored = PreprovisioningBlobV1Parameters.create(request.serialize())

        assertEquals(correlationId, restored.correlationId)
        assertEquals("tenant", restored.tenantId)
        assertNull(restored.resourceAccountId)
        assertEquals("PPE", restored.discoveryEndpointName)
        assertEquals(DeviceRegistrationProtocolConstants.GET_PRE_PROVISIONED_BLOB_V1, restored.protocolName)
        assertFalse(String(request.serialize(), Charsets.UTF_8).contains("mResourceAccountId"))
    }

    @Test
    fun registrationParametersRoundTrip() {
        for (sharedDevice in listOf(true, false)) {
            val request = RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters(
                correlationId, "tenant", "resource", "encrypted-challenge", sharedDevice, "ONEAUTH", "1.0", "PROD"
            )
            val restored = RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters.create(request.serialize())

            assertEquals(correlationId, restored.correlationId)
            assertEquals("tenant", restored.tenantId)
            assertEquals("resource", restored.resourceAccountId)
            assertEquals("encrypted-challenge", restored.encryptedPreAuthorizedToken)
            assertEquals(sharedDevice, restored.isRegisterAsSharedDevice)
            assertEquals("ONEAUTH", restored.sdkType)
            assertEquals("1.0", restored.sdkVersion)
            assertEquals("PROD", restored.discoveryEndpointName)
            assertEquals(DeviceRegistrationProtocolConstants.REGISTER_DEVICE_AND_RESOURCE_ACCOUNT_WITH_PREAUTHORIZED_CHALLENGE_V0, restored.protocolName)
            assertFalse(request.toString().contains("encrypted-challenge"))
        }
    }

    @Test
    fun blobResponseRoundTrip() {
        val response = PreprovisioningBlobV1Response(correlationId, "signed-blob")
        val restored = PreprovisioningBlobV1Response.create(response.serialize())

        assertEquals(correlationId, restored.correlationId)
        assertEquals("signed-blob", restored.jws)
        assertEquals(DeviceRegistrationProtocolConstants.GET_PRE_PROVISIONED_BLOB_V1, restored.protocolName)
        assertFalse(response.toString().contains("signed-blob"))
    }

    @Test
    fun registrationResponseRoundTripsBothRecordTypes() {
        val account = AccountRecord().apply {
            homeAccountId = "resource.tenant"
            localAccountId = "resource"
            realm = "tenant"
            username = "resource@example.com"
            environment = "login.microsoftonline.com"
        }
        val records = listOf(
            DeviceRegistrationRecord("tenant", "resource@example.com", "device", true, true),
            DeviceRegistrationRecordWithAccount("account", "tenant", "resource@example.com", "device", true, true)
        )
        for (record in records) {
            val response = RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response(correlationId, record, account)
            val restored = RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response.create(response.serialize())

            assertEquals(correlationId, restored.correlationId)
            assertEquals(record.javaClass, restored.deviceRegistrationRecord.javaClass)
            assertEquals("device", restored.deviceRegistrationRecord.deviceId)
            assertEquals(account, restored.resourceAccountRecord)
            assertEquals(DeviceRegistrationProtocolConstants.REGISTER_DEVICE_AND_RESOURCE_ACCOUNT_WITH_PREAUTHORIZED_CHALLENGE_V0, restored.protocolName)
        }
    }

    @Test
    fun responseDeserializationMatchesExistingV0Defaults() {
        val serialized = "{}".toByteArray(Charsets.UTF_8)
        val oldBlob = PreProvisionedBlobV0Response.create(serialized)
        val newBlob = PreprovisioningBlobV1Response.create(serialized)
        assertEquals(oldBlob.correlationId, newBlob.correlationId)
        assertEquals(oldBlob.jws, newBlob.jws)

        val oldJoin = DeviceRegistrationPreAuthorizedV0Response.create(serialized)
        val newJoin = RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response.create(serialized)
        assertEquals(oldJoin.correlationId, newJoin.correlationId)
        assertEquals(oldJoin.deviceRegistrationRecord, newJoin.deviceRegistrationRecord)
        assertNull(newJoin.resourceAccountRecord)
    }

    @Test
    fun blankBlobDeserializationMatchesExistingV0() {
        val payload = """{"mCorrelationId":"$correlationId","mJws":"   "}"""
        val serialized = payload.toByteArray(Charsets.UTF_8)
        val oldBlob = PreProvisionedBlobV0Response.create(serialized)
        val newBlob = PreprovisioningBlobV1Response.create(serialized)
        assertEquals(oldBlob.correlationId, newBlob.correlationId)
        assertEquals(oldBlob.jws, newBlob.jws)
    }

    @Test
    fun javaBooleanGetterMatchesExistingProtocols() {
        val request = RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters(
            correlationId, "tenant", "resource", "challenge", true, "ONEAUTH", "1.0", "PROD"
        )
        assertEquals(
            true,
            RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters::class.java
                .getMethod("isRegisterAsSharedDevice").invoke(request)
        )
    }

    @Test
    fun parameterDeserializationMatchesExistingV0Defaults() {
        val serialized = "{}".toByteArray(Charsets.UTF_8)
        val oldBlob = PreProvisionedBlobV0Parameters.create(serialized)
        val newBlob = PreprovisioningBlobV1Parameters.create(serialized)
        assertEquals(oldBlob.correlationId, newBlob.correlationId)
        assertEquals(oldBlob.tenantId, newBlob.tenantId)
        assertNull(newBlob.resourceAccountId)
        assertNull(newBlob.discoveryEndpointName)

        val oldJoin = DeviceRegistrationPreAuthorizedV0Parameters.create(serialized)
        val newJoin = RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters.create(serialized)
        assertEquals(oldJoin.correlationId, newJoin.correlationId)
        assertEquals(oldJoin.tenantId, newJoin.tenantId)
        assertEquals(oldJoin.isRegisterAsSharedDevice, newJoin.isRegisterAsSharedDevice)
        assertFalse(newJoin.isRegisterAsSharedDevice)
        assertNull(newJoin.resourceAccountId)
        assertNull(newJoin.encryptedPreAuthorizedToken)
        assertNull(newJoin.sdkType)
        assertNull(newJoin.sdkVersion)
        assertNull(newJoin.discoveryEndpointName)
    }
}