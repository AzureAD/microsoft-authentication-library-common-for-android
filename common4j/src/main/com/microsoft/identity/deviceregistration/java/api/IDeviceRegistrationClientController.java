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
package com.microsoft.identity.deviceregistration.java.api;

import com.microsoft.identity.deviceregistration.java.protocol.parameters.IDeviceRegistrationProtocolParameters;
import com.microsoft.identity.common.java.exception.BaseException;

import lombok.NonNull;

/**
 * An interface that represents a platform device registration controller to communicate protocols to the broker.
 */
public interface IDeviceRegistrationClientController {
    /**
     * Communicates the protocol associated with the given parameters using the available strategies.
     *
     * @param protocolParameters {@link IDeviceRegistrationProtocolParameters} protocol parameters.
     * @return a serialized protocol response.
     * @throws BaseException if all strategies to execute the protocol fail.
     */
    byte[] execute(@NonNull final IDeviceRegistrationProtocolParameters protocolParameters)
            throws BaseException;
}
