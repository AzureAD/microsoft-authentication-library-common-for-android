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
package com.microsoft.identity.deviceregistration.java.packer;

import com.microsoft.identity.deviceregistration.testprotocols.TestHappyPathV0Parameters;
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecordWithAccount;
import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import lombok.SneakyThrows;

@RunWith(Parameterized.class)
public class DeviceRegistrationProtocolSerializerParameterizedTest {

    private static final String TEST_STRING_1 = "TEST1";
    private static final String TEST_STRING_2 = "TEST2";
    private static final float TEST_FLOAT_1 = 123.456f;
    private static final float TEST_FLOAT_2 = 9f;
    private static final String TEST_TENANT = "test.tenant.id";
    private static final String TEST_UPN = "test.upn";
    private static final String TEST_DEVICE_ID = "test.device.id";
    private static final String TEST_ACCOUNT_NAME = "test.account.name";

    private static final IDeviceRegistrationRecord DEVICE_REGISTRATION_RECORD
            = new DeviceRegistrationRecord(TEST_TENANT, TEST_UPN, TEST_DEVICE_ID, false, false);

    private static final IDeviceRegistrationRecord DEVICE_REGISTRATION_RECORD_WITH_ACCOUNT
            = new DeviceRegistrationRecordWithAccount(TEST_ACCOUNT_NAME, TEST_TENANT, TEST_UPN, TEST_DEVICE_ID, false, false);


    private static final TestHappyPathV0Parameters TEST_PROTOCOL = new TestHappyPathV0Parameters(
            TEST_STRING_1,
            TEST_STRING_2,
            true,
            false,
            TEST_FLOAT_1,
            TEST_FLOAT_2,
            DEVICE_REGISTRATION_RECORD,
            DEVICE_REGISTRATION_RECORD_WITH_ACCOUNT
    );

    private final IDeviceRegistrationProtocolSerializer<TestHappyPathV0Parameters> serializer;

    public DeviceRegistrationProtocolSerializerParameterizedTest(IDeviceRegistrationProtocolSerializer<TestHappyPathV0Parameters> serializer) {
        this.serializer = serializer;
    }

    @Parameterized.Parameters(name = "{0}")
    public static List<IDeviceRegistrationProtocolSerializer<TestHappyPathV0Parameters>> testData() {
        return new ArrayList<>(Collections.singletonList(
                new DeviceRegistrationProtocolMoshiSerializer<>(TestHappyPathV0Parameters.class)
        ));
    }


    @SneakyThrows
    @Test
    public void testSerializeProtocol() {
        // Serialize/deserialize protocol
        final byte[] serializedData = serializer.serialize(TEST_PROTOCOL);
        final TestHappyPathV0Parameters protocol = serializer.deserialize(serializedData);
        // Validate
        Assert.assertEquals(TEST_PROTOCOL.getProtocolName(), protocol.getProtocolName());
        Assert.assertEquals(TEST_PROTOCOL.getString1(), protocol.getString1());
        Assert.assertEquals(TEST_PROTOCOL.getString2(), protocol.getString2());
        Assert.assertEquals(TEST_PROTOCOL.getFloat1(), protocol.getFloat1(), 0);
        Assert.assertEquals(TEST_PROTOCOL.getFloat2(), protocol.getFloat2(), 0);
        Assert.assertTrue(protocol.isBoolean1());
        Assert.assertFalse(protocol.isBoolean2());
        Assert.assertEquals(TEST_TENANT, protocol.getRecord().getTenantId());
        Assert.assertEquals(TEST_UPN, protocol.getRecord().getUpn());
        Assert.assertEquals(TEST_DEVICE_ID, protocol.getRecord().getDeviceId());
        final DeviceRegistrationRecordWithAccount recordWithAccount =
                (DeviceRegistrationRecordWithAccount) protocol.getRecordWithAccount();
        Assert.assertEquals(TEST_ACCOUNT_NAME, recordWithAccount.getAccountName());
        Assert.assertEquals(TEST_TENANT, recordWithAccount.getTenantId());
        Assert.assertEquals(TEST_UPN, recordWithAccount.getUpn());
        Assert.assertEquals(TEST_DEVICE_ID, recordWithAccount.getDeviceId());
        Assert.assertEquals(TEST_PROTOCOL.getCorrelationId(), protocol.getCorrelationId());
    }
}
