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

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.deviceregistration.java.protocol.DeviceRegistrationProtocolConstants;
import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;

import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Signed preprovisioning blob from Broker to forward to the provisioning service.
 * This response does not represent completed registration.
 */
@Accessors(prefix = "m")
public class PreprovisioningBlobV1Response extends AbstractDeviceRegistrationProtocolResponse {

    private static final IDeviceRegistrationProtocolSerializer<PreprovisioningBlobV1Response> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(PreprovisioningBlobV1Response.class);

    /**
     * Deserializes a protocol response.
     *
     * @param serializedData serialized protocol data
     */
    public static PreprovisioningBlobV1Response create(final byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    /**
     * @param correlationId correlation ID of the request
      * @param jws signed provisioning blob; do not log this value
     */
    public PreprovisioningBlobV1Response(@NonNull final UUID correlationId, @NonNull final String jws) {
        super(correlationId);
        mJws = jws;
    }

    @Getter
    @NonNull
    private final String mJws;

    @Override
    @NonNull
    public String getProtocolName() {
        return DeviceRegistrationProtocolConstants.GET_PRE_PROVISIONED_BLOB_V1;
    }

    @Override
    public byte[] serialize() {
        return serializer.serialize(this);
    }
}