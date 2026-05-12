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

import com.microsoft.identity.deviceregistration.java.protocol.DeviceRegistrationProtocolConstants;

import java.util.Map;
import java.util.UUID;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

import com.microsoft.identity.deviceregistration.java.protocol.packer.DeviceRegistrationProtocolMoshiSerializer;
import com.microsoft.identity.deviceregistration.java.protocol.packer.IDeviceRegistrationProtocolSerializer;
import com.microsoft.identity.common.java.exception.ClientException;

/**
 * Implements a protocol response with the information necessary to launch
 * a install WPJ certificate intent and get the result or error message from the activity.
 */
@Accessors(prefix = "m")
public class GetInstallWpjCertificateIntentRequestV0Response extends AbstractDeviceRegistrationProtocolResponse {

    private static final IDeviceRegistrationProtocolSerializer<GetInstallWpjCertificateIntentRequestV0Response> serializer
            = new DeviceRegistrationProtocolMoshiSerializer<>(GetInstallWpjCertificateIntentRequestV0Response.class);

    /**
     * Creates a protocol object from a byte array (serialized protocol).
     *
     * @param serializedData protocol data serialized
     */
    public static GetInstallWpjCertificateIntentRequestV0Response create(final byte[] serializedData) throws ClientException {
        return serializer.deserialize(serializedData);
    }

    public GetInstallWpjCertificateIntentRequestV0Response(@NonNull final UUID correlationId,
                                                           @NonNull final String activityClassName,
                                                           @NonNull final String brokerPackageName,
                                                           @NonNull final String installCertActivityResultKey,
                                                           @NonNull final String installCertActivityErrorKey,
                                                           @NonNull final Map<String, String> extras) {
        super(correlationId);
        mActivityClassName = activityClassName;
        mBrokerPackageName = brokerPackageName;
        mInstallCertActivityResultKey = installCertActivityResultKey;
        mInstallCertActivityErrorKey = installCertActivityErrorKey;
        mExtras = extras;
    }

    /**
     * Class name of the Activity that will install the WPJ certificate (InstallCertActivity).
     */
    @NonNull
    @Getter
    private final String mActivityClassName;

    /**
     * The package name of the active broker.
     */
    @NonNull
    @Getter
    private final String mBrokerPackageName;

    /**
     * key used to extract the result from the InstallCertActivity.
     */
    @NonNull
    @Getter
    private final String mInstallCertActivityResultKey;

    /**
     * key used to extract the error message from the InstallCertActivity.
     */
    @NonNull
    @Getter
    private final String mInstallCertActivityErrorKey;

    /**
     * HashMap with the variables we will send to the InstallCertActivity.
     */
    @NonNull
    @Getter
    private final Map<String, String> mExtras;

    /**
     * Returns the name of the protocol.
     */
    @Override
    public String getProtocolName() {
        return DeviceRegistrationProtocolConstants.DEVICE_REGISTRATION_WITH_TOKENS_V0;
    }

    /**
     * return the serialized the protocol.
     */
    @Override
    public byte[] serialize() {
        return serializer.serialize(this);
    }
}
