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
package com.microsoft.identity.deviceregistration.java.protocol.parameters;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.deviceregistration.java.protocol.DeviceRegistrationProtocolConstants;
import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Requests Broker key preparation and a signed preprovisioning blob.
 * A resource-account ID also requests an RA key; null requests device-only preparation.
 */
@Accessors(prefix = "m")
public class PreprovisioningBlobV1Parameters extends AbstractDeviceRegistrationProtocolParameters {

    private static final IDeviceRegistrationProtocolSerializer<PreprovisioningBlobV1Parameters> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(PreprovisioningBlobV1Parameters.class);

    /**
     * Deserializes protocol parameters.
     *
     * @param serializedData serialized protocol data
     */
    public static PreprovisioningBlobV1Parameters create(final byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    /**
     * @param correlationId correlation ID for request tracing
     * @param tenantId tenant in which the device will be registered
      * @param resourceAccountId optional tenant-local Entra object ID; null omits RA-key preparation
     * @param discoveryEndpointName DRS environment used for preparation and registration
     */
    public PreprovisioningBlobV1Parameters(@NonNull final UUID correlationId,
                                         @NonNull final String tenantId,
                                         @Nullable final String resourceAccountId,
                                         @NonNull final String discoveryEndpointName) {
        super(correlationId);
        mTenantId = tenantId;
        mResourceAccountId = resourceAccountId;
        mDiscoveryEndpointName = discoveryEndpointName;
    }

    @Getter
    @NonNull
    private final String mTenantId;

    @Getter
    @Nullable
    private final String mResourceAccountId;

    @Getter
    @NonNull
    private final String mDiscoveryEndpointName;

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