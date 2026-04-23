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
package com.microsoft.identity.deviceregistration.java.protocol.response;

import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.protocol.DeviceRegistrationProtocolConstants;

import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;
import com.microsoft.identity.common.java.exception.ClientException;

@Accessors(prefix = "m")
public class DeviceRegistrationPreAuthorizedV0Response extends AbstractDeviceRegistrationProtocolResponse {

    private static final IDeviceRegistrationProtocolSerializer<DeviceRegistrationPreAuthorizedV0Response> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(DeviceRegistrationPreAuthorizedV0Response.class);

    /**
     * Creates a protocol object from a byte array (serialized protocol).
     *
     * @param serializedData protocol data serialized
     */
    public static DeviceRegistrationPreAuthorizedV0Response create(final byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    public DeviceRegistrationPreAuthorizedV0Response(@NonNull final UUID correlationId,
                                                     @NonNull final IDeviceRegistrationRecord deviceRegistrationRecord) {
        super(correlationId);
        mDeviceRegistrationRecord = deviceRegistrationRecord;
    }

    @Getter
    @NonNull
    private final IDeviceRegistrationRecord mDeviceRegistrationRecord;

    /**
     * Returns the name of the protocol.
     */
    @Override
    public String getProtocolName() {
        return DeviceRegistrationProtocolConstants.DEVICE_REGISTRATION_PREAUTHORIZED_V0;
    }

    /**
     * return the serialized the protocol.
     */
    @Override
    public byte[] serialize() {
        return serializer.serialize(this);
    }
}
