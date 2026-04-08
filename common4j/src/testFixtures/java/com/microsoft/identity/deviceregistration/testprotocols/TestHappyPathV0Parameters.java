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
package com.microsoft.identity.deviceregistration.testprotocols;

import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.parameters.AbstractDeviceRegistrationProtocolParameters;
import com.microsoft.identity.common.java.exception.ClientException;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Implements a protocol parameters, for testing.
 */
@AllArgsConstructor
@Accessors(prefix = "m")
public class TestHappyPathV0Parameters extends AbstractDeviceRegistrationProtocolParameters {

    public final static String TEST_HAPPY_PATH_V0_PROTOCOL = "protocol.happy.path.v0";

    private static final IDeviceRegistrationProtocolSerializer<TestHappyPathV0Parameters> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(TestHappyPathV0Parameters.class);

    /**
     * Creates a protocol object from a bytes array (serialized protocol).
     */
    static public TestHappyPathV0Parameters create(byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    @Getter
    @NonNull
    private final String mString1;

    @Getter
    @NonNull
    private final String mString2;

    @Getter
    private final boolean mBoolean1;

    @Getter
    private final boolean mBoolean2;
    @Getter
    private final float mFloat1;

    @Getter
    private final float mFloat2;

    @Getter
    @NonNull
    private final IDeviceRegistrationRecord mRecord;

    @Getter
    @NonNull
    private final IDeviceRegistrationRecord mRecordWithAccount;

    @Override
    public final String getProtocolName() {
        return TEST_HAPPY_PATH_V0_PROTOCOL;
    }

    @Override
    public byte[] serialize() {
        return serializer.serialize(this);
    }
}
