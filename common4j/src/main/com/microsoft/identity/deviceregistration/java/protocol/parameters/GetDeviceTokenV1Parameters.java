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

import com.microsoft.identity.deviceregistration.java.api.IDeviceRegistrationRecord;
import com.microsoft.identity.deviceregistration.java.protocol.DeviceRegistrationProtocolConstants;

import java.util.UUID;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;
import com.microsoft.identity.common.java.exception.ClientException;

/**
 * Protocol parameters for GET_DEVICE_TOKEN_V1.
 * This version requires a clientId to identify the calling application.
 */
@Accessors(prefix = "m")
public class GetDeviceTokenV1Parameters extends AbstractDeviceRegistrationProtocolParameters {

    private static final IDeviceRegistrationProtocolSerializer<GetDeviceTokenV1Parameters> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(GetDeviceTokenV1Parameters.class);

    /**
     * Creates a protocol object from a byte array (serialized protocol).
     *
     * @param serializedData protocol data serialized
     */
    public static GetDeviceTokenV1Parameters create(final byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    public GetDeviceTokenV1Parameters(@NonNull final UUID correlationId,
                                      @NonNull final IDeviceRegistrationRecord deviceRegistrationRecord,
                                      @NonNull final String resources,
                                      @NonNull final String clientId,
                                      @NonNull final String redirectUri,
                                      @Nullable final String scope) {
        super(correlationId);
        mDeviceRegistrationRecord = deviceRegistrationRecord;
        mResources = resources;
        mClientId = clientId;
        mRedirectUri = redirectUri;
        mScope = scope;
    }

    @Getter
    @NonNull
    private final IDeviceRegistrationRecord mDeviceRegistrationRecord;

    @Getter
    @NonNull
    private final String mResources;

    @Getter
    @NonNull
    private final String mClientId;

    @Getter
    @NonNull
    private final String mRedirectUri;

    @Getter
    @Nullable
    private final String mScope;

    /**
     * Returns the name of the protocol.
     */
    @Override
    public String getProtocolName() {
        return DeviceRegistrationProtocolConstants.GET_DEVICE_TOKEN_V1;
    }

    /**
     * Serialization is handled by the packer layer.
     */
    @Override
    public byte[] serialize() {
        return serializer.serialize(this);
    }
}
