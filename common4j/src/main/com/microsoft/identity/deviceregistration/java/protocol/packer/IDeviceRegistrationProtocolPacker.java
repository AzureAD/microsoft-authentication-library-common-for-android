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
package com.microsoft.identity.deviceregistration.java.protocol.packer;

import com.microsoft.identity.deviceregistration.java.exception.DeviceRegistrationException;
import com.microsoft.identity.deviceregistration.java.protocol.IDeviceRegistrationProtocol;
import com.microsoft.identity.common.java.exception.BaseException;

import lombok.NonNull;

/**
 * A generic interface for packing and unpacking {@link IDeviceRegistrationProtocol protocols}.
 */
public interface IDeviceRegistrationProtocolPacker<PackageType> {

    /**
     * Returns the protocol name contained in the provided package.
     *
     * @param packedData Generic protocol package.
     * @return name of the protocol.
     * @throws DeviceRegistrationException if the protocol bundle contains a packed exception.
     */
    String unpackName(@NonNull final PackageType packedData) throws DeviceRegistrationException;

    /**
     * Returns the correlation id contained in the provided package.
     * If the correlation id is not present, generates a new one.
     *
     * @param packedData Generic protocol package.
     * @return correlationId.
     */
    String unpackCorrelationId(@NonNull final PackageType packedData);

    /**
     * Unpacks a packed protocol into a serialized protocol.
     *
     * @param packedData Generic protocol package to unpack.
     * @return a byte array with the protocol data.
     * @throws BaseException if the protocol bundle contains a packed exception.
     */
    byte[] unpackData(@NonNull final PackageType packedData) throws BaseException;

    /**
     * Packs a {@link IDeviceRegistrationProtocol protocol} into a generic object.
     *
     * @param protocol {@link IDeviceRegistrationProtocol protocol} to pack.
     * @return a packed protocol.
     */
    PackageType pack(@NonNull final IDeviceRegistrationProtocol protocol);

    /**
     * Packs a Throwable into a package.
     *
     * @param throwable to pack.
     * @return package with all the exception data.
     */
    PackageType packThrowable(@NonNull final Throwable throwable);
}
