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

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Requests device registration and initial resource-account provisioning with prepared keys.
 * The tenant, resource account, and environment must match preprovisioning.
 */
@Accessors(prefix = "m")
public class RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters extends AbstractDeviceRegistrationProtocolParameters {

    private static final IDeviceRegistrationProtocolSerializer<RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters.class);

    /**
     * Deserializes protocol parameters.
     *
     * @param serializedData serialized protocol data
     */
    public static RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters create(final byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    /**
     * @param correlationId correlation ID for request tracing
     * @param tenantId tenant used during preprovisioning
      * @param resourceAccountId required tenant-local Entra object ID used during preprovisioning
      * @param encryptedPreAuthorizedToken STK-encrypted preauthorized challenge returned by the service
     * @param registerAsSharedDevice whether the device should be registered as shared
     * @param sdkType calling SDK type name
     * @param sdkVersion calling SDK version
     * @param discoveryEndpointName DRS environment used during preprovisioning
     */
    public RegisterDeviceAndResourceAccountWithPreauthorizedChallengeV0Parameters(@NonNull final UUID correlationId,
                                                       @NonNull final String tenantId,
                                                       @NonNull final String resourceAccountId,
                                                       @NonNull final String encryptedPreAuthorizedToken,
                                                       final boolean registerAsSharedDevice,
                                                       @NonNull final String sdkType,
                                                       @NonNull final String sdkVersion,
                                                       @NonNull final String discoveryEndpointName) {
        super(correlationId);
        mTenantId = tenantId;
        mResourceAccountId = resourceAccountId;
        mEncryptedPreAuthorizedToken = encryptedPreAuthorizedToken;
        mRegisterAsSharedDevice = registerAsSharedDevice;
        mSdkType = sdkType;
        mSdkVersion = sdkVersion;
        mDiscoveryEndpointName = discoveryEndpointName;
    }

    @Getter
    @NonNull
    private final String mTenantId;

    @Getter
    @NonNull
    private final String mResourceAccountId;

    @Getter
    @NonNull
    private final String mEncryptedPreAuthorizedToken;

    @Getter
    private final boolean mRegisterAsSharedDevice;

    @Getter
    @NonNull
    private final String mSdkType;

    @Getter
    @NonNull
    private final String mSdkVersion;

    @Getter
    @NonNull
    private final String mDiscoveryEndpointName;

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