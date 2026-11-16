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

import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.protocol.DeviceRegistrationProtocolConstants;
import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;

import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Response after device registration and initial resource-account provisioning both succeed.
 * Both records are required. {@link AccountRecord} provides the concrete type for deserialization.
 */
@Accessors(prefix = "m")
public class RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response extends AbstractDeviceRegistrationProtocolResponse {

    private static final IDeviceRegistrationProtocolSerializer<RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response.class);

    /**
     * Deserializes a protocol response.
     *
     * @param serializedData serialized protocol data
     */
    public static RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response create(final byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    /**
     * @param correlationId correlation ID of the request
     * @param deviceRegistrationRecord completed device registration
     * @param resourceAccountRecord metadata for the provisioned resource account
     */
    public RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Response(@NonNull final UUID correlationId,
                                                    @NonNull final IDeviceRegistrationRecord deviceRegistrationRecord,
                                                    @NonNull final AccountRecord resourceAccountRecord) {
        super(correlationId);
        mDeviceRegistrationRecord = deviceRegistrationRecord;
        mResourceAccountRecord = resourceAccountRecord;
    }

    @Getter
    @NonNull
    private final IDeviceRegistrationRecord mDeviceRegistrationRecord;

    @Getter
    @NonNull
    private final AccountRecord mResourceAccountRecord;

    @Override
    @NonNull
    public String getProtocolName() {
        return DeviceRegistrationProtocolConstants.REGISTER_DEVICE_AND_RESOURCE_ACCOUNT_WITH_PREAUTHORIZED_CHALLENGE_V0;
    }

    @Override
    public byte[] serialize() {
        return serializer.serialize(this);
    }
}