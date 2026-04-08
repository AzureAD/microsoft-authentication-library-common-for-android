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

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * Interface describing a device registration entry, where each record has a unique tenant ID.
 * Note: A device registration record is shared by all the accounts in that tenant.
 */
public interface IDeviceRegistrationRecord {

    /**
     * Gets the tenant Id to which the device is registered.
     */
    @NonNull
    String getTenantId();

    /**
     * Gets the UPN associated to this device registration entry.
     * Could be null if the device is registered without user.
     */
    @Nullable
    String getUpn();

    /**
     * Gets Device Id associated to this device registration entry.
     */
    @NonNull
    String getDeviceId();

    /**
     * Returns true if the device registration entry is on shared device mode, otherwise false.
     */
    boolean isSharedDevice();

    /**
     * True if WPJ is registered with strong key.
     * (Meaning that installCert would not work for this entry).
     */
    boolean isRegisteredWithStrongKeys();
}
