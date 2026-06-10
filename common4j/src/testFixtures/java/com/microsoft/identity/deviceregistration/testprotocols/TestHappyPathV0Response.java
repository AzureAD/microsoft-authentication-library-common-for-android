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

import com.microsoft.identity.deviceregistration.java.api.DeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.response.AbstractDeviceRegistrationProtocolResponse;
import com.microsoft.identity.common.java.exception.ClientException;

import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Implements a protocol result, for testing.
 */
@Accessors(prefix = "m")
public class TestHappyPathV0Response extends AbstractDeviceRegistrationProtocolResponse {

    public static final IDeviceRegistrationProtocolSerializer<TestHappyPathV0Response> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(TestHappyPathV0Response.class);

    public TestHappyPathV0Response(@NonNull final UUID correlationId,
                                   @NonNull final String concatenatedStrings,
                                   @NonNull final float multiplicationResult,
                                   final boolean logicalOr,
                                   @NonNull final DeviceRegistrationRecord deviceRegistrationRecord) {
        super(correlationId);
        mConcatenatedStrings = concatenatedStrings;
        mMultiplicationResult = multiplicationResult;
        mLogicalOr = logicalOr;
        mSameRecordWithConcatenatedX = deviceRegistrationRecord;
    }

    /**
     * Creates a protocol object from a bytes array (serialized protocol).
     */
    static public TestHappyPathV0Response create(byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    @Getter
    @NonNull
    private final String mConcatenatedStrings;

    @Getter
    private final float mMultiplicationResult;

    @Getter
    private final boolean mLogicalOr;

    @Getter
    @NonNull
    private final DeviceRegistrationRecord mSameRecordWithConcatenatedX;

    @Override
    public final String getProtocolName() {
        return DeviceRegistrationTestProtocolConstants.TEST_HAPPY_PATH_V0_PROTOCOL;
    }

    @Override
    public byte[] serialize() {
        return serializer.serialize(this);
    }
}
