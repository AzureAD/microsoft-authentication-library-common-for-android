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
package com.microsoft.identity.labapi.utilities.client;

import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.labapi.utilities.constants.UserType;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * An account object model that will used to represent accounts used for testing purposes.
 */
@Getter
@Accessors(prefix = "m")
@Builder
@EqualsAndHashCode
public class LabAccount implements ILabAccount {

    @NonNull
    private final String mUsername;

    @NonNull
    private final String mPassword;

    @NonNull
    private final UserType mUserType;

    @NonNull
    private final String mHomeTenantId;

    // nullable
    // dependency for Nullable annotation not currently added to LabApiUtilities
    private final ConfigInfo mConfigInfo;

    @Override
    public String getAssociatedClientId() {
        return mConfigInfo.getAppInfo().getAppId();
    }

    @Override
    public String getAuthority() {
        return mConfigInfo.getLabInfo().getAuthority();
    }
}
