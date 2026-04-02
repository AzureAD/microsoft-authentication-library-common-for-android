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

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * This class represents a device registration entry, where each record has a unique tenant ID.
 * Note: A device registration record is shared by all the accounts in that tenant.
 */
@AllArgsConstructor
@Getter
@Accessors(prefix = "m")
public class DeviceRegistrationRecord implements IDeviceRegistrationRecord {
    public static final String TAG = DeviceRegistrationRecord.class.getSimpleName();

    /**
     * Tenant Id to which the device is registered.
     */
    @NonNull
    private final String mTenantId;

    /**
     * UPN associated to this device registration entry.
     * Could be null if the device is registered without user.
     */
    @Nullable
    private final String mUpn;

    /**
     * Device Id associated to this device registration entry.
     */
    @NonNull
    private final String mDeviceId;

    /**
     * True if the device is shared.
     */
    @Getter
    private final boolean mSharedDevice;

    /**
     * True if WPJ is registered with strong key.
     * (Meaning that installCert would not work for this entry).
     */
    @Getter
    private final boolean mIsRegisteredWithStrongKeys;

    @Override
    public String toString() {
        return "TenantId: " + mTenantId + "\n" + "Upn:" + mUpn + "\n" +
                "DeviceId:" + mDeviceId + "\n" + "isShared:" + mSharedDevice + "\n" +
                "isRegisteredWithStrongKeys:" + mIsRegisteredWithStrongKeys;
    }
}
