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
package com.microsoft.identity.deviceregistration.java.protocol.parameters

import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecord
import com.microsoft.identity.deviceregistration.java.protocol.DeviceRegistrationProtocolConstants
import org.junit.Assert
import org.junit.Test
import java.util.UUID

/**
 * Unit tests for [GetDeviceTokenV1Parameters] serialization and field access.
 */
class GetDeviceTokenV1ParametersTest {

    companion object {
        private val CORRELATION_ID = UUID.randomUUID()
        private const val TEST_TENANT = "test-tenant-id"
        private const val TEST_UPN = "user@test.com"
        private const val TEST_DEVICE_ID = "device-123"
        private const val TEST_RESOURCES = "https://resource.example.com"
        private const val TEST_CLIENT_ID = "test-client-id-12345"
        private const val TEST_SCOPE = "openid profile"
        private val DEVICE_REGISTRATION_RECORD =
            DeviceRegistrationRecord(TEST_TENANT, TEST_UPN, TEST_DEVICE_ID, false, false)
    }

    @Test
    fun testSerializationRoundTrip_withScope() {
        val original = GetDeviceTokenV1Parameters(
            CORRELATION_ID, DEVICE_REGISTRATION_RECORD, TEST_RESOURCES, TEST_CLIENT_ID, TEST_SCOPE
        )

        val serialized = original.serialize()
        val deserialized = GetDeviceTokenV1Parameters.create(serialized)

        Assert.assertEquals(CORRELATION_ID, deserialized.correlationId)
        Assert.assertEquals(TEST_TENANT, deserialized.deviceRegistrationRecord.tenantId)
        Assert.assertEquals(TEST_UPN, deserialized.deviceRegistrationRecord.upn)
        Assert.assertEquals(TEST_DEVICE_ID, deserialized.deviceRegistrationRecord.deviceId)
        Assert.assertEquals(TEST_RESOURCES, deserialized.resources)
        Assert.assertEquals(TEST_CLIENT_ID, deserialized.clientId)
        Assert.assertEquals(TEST_SCOPE, deserialized.scope)
    }

    @Test
    fun testSerializationRoundTrip_withoutScope() {
        val original = GetDeviceTokenV1Parameters(
            CORRELATION_ID, DEVICE_REGISTRATION_RECORD, TEST_RESOURCES, TEST_CLIENT_ID, null
        )

        val serialized = original.serialize()
        val deserialized = GetDeviceTokenV1Parameters.create(serialized)

        Assert.assertEquals(CORRELATION_ID, deserialized.correlationId)
        Assert.assertEquals(TEST_RESOURCES, deserialized.resources)
        Assert.assertEquals(TEST_CLIENT_ID, deserialized.clientId)
        Assert.assertNull(deserialized.scope)
    }

    @Test
    fun testProtocolName() {
        val parameters = GetDeviceTokenV1Parameters(
            CORRELATION_ID, DEVICE_REGISTRATION_RECORD, TEST_RESOURCES, TEST_CLIENT_ID, null
        )

        Assert.assertEquals(
            DeviceRegistrationProtocolConstants.GET_DEVICE_TOKEN_V1,
            parameters.protocolName
        )
    }

    @Test
    fun testGetters() {
        val parameters = GetDeviceTokenV1Parameters(
            CORRELATION_ID, DEVICE_REGISTRATION_RECORD, TEST_RESOURCES, TEST_CLIENT_ID, TEST_SCOPE
        )

        Assert.assertEquals(CORRELATION_ID, parameters.correlationId)
        Assert.assertSame(DEVICE_REGISTRATION_RECORD, parameters.deviceRegistrationRecord)
        Assert.assertEquals(TEST_RESOURCES, parameters.resources)
        Assert.assertEquals(TEST_CLIENT_ID, parameters.clientId)
        Assert.assertEquals(TEST_SCOPE, parameters.scope)
    }
}
