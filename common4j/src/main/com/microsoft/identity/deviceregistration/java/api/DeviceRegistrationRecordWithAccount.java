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

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * This class extends the {@link DeviceRegistrationRecord} class adding the account name as private member.
 * This to improve the performance of LegacyExecutors that use an Account for most of the legacy
 * WorkplaceJoin operations.
 */
@Getter
@Accessors(prefix = "m")
public class DeviceRegistrationRecordWithAccount extends DeviceRegistrationRecord {

    /**
     * Account name of the device registration record.
     */
    @NonNull
    private final String mAccountName;

    public DeviceRegistrationRecordWithAccount(@NonNull final String accountName,
                                               @NonNull final String tenantId,
                                               @Nullable final String upn,
                                               @NonNull String deviceId,
                                               final boolean sharedDevice,
                                               final boolean isRegisteredWithStrongKeys) {
        super(tenantId, upn, deviceId, sharedDevice, isRegisteredWithStrongKeys);
        mAccountName = accountName;
    }
}
