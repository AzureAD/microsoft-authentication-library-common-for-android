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
 * Implements protocol parameters for provisioning resource account credentials.
 */
@Accessors(prefix = "m")
public class ProvisionResourceAccountCredentialsV0Parameters extends AbstractDeviceRegistrationProtocolParameters {

    private static final IDeviceRegistrationProtocolSerializer<ProvisionResourceAccountCredentialsV0Parameters> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(ProvisionResourceAccountCredentialsV0Parameters.class);

    /**
     * Creates a protocol object from a byte array (serialized protocol).
     *
     * @param serializedData protocol data serialized
     */
    public static ProvisionResourceAccountCredentialsV0Parameters create(final byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    public ProvisionResourceAccountCredentialsV0Parameters(@NonNull final UUID correlationId,
                                                           @NonNull final String tenantId,
                                                           @NonNull final String raObjectId,
                                                           @Nullable final String sdkType,
                                                           @Nullable final String sdkVersion) {
        super(correlationId);
        mTenantId = tenantId;
        mRaObjectId = raObjectId;
        mSdkType = sdkType;
        mSdkVersion = sdkVersion;
    }

    @Getter
    @NonNull
    private final String mTenantId;

    @Getter
    @NonNull
    private final String mRaObjectId;

    @Getter
    @Nullable
    private final String mSdkType;

    @Getter
    @Nullable
    private final String mSdkVersion;

    @Override
    @NonNull
    public String getProtocolName() {
        return DeviceRegistrationProtocolConstants.PROVISION_RESOURCE_ACCOUNT_CREDENTIALS_V0;
    }

    /**
     * Serialization is handled by the packer layer.
     */
    @Override
    public byte[] serialize() {
        return serializer.serialize(this);
    }
}
