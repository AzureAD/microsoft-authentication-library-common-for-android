/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.internal.testutils.labutils;

import androidx.annotation.NonNull;

import com.microsoft.identity.internal.test.labapi.model.ConfigInfo;
import com.microsoft.identity.internal.test.labapi.model.TempUser;

public class LabConfig {

    private static LabConfig sCurrentLabConfig;

    public static LabConfig getCurrentLabConfig() {
        return sCurrentLabConfig;
    }

    public static void setCurrentLabConfig(LabConfig currentLabConfig) {
        LabConfig.sCurrentLabConfig = currentLabConfig;
    }

    private ConfigInfo mConfigInfo;
    private TempUser mTempUser;
    private String mLabUserPassword;

    public LabConfig(@NonNull ConfigInfo configInfo, String labUserPassword) {
        this.mConfigInfo = configInfo;
        this.mLabUserPassword = labUserPassword;
    }

    public LabConfig(@NonNull ConfigInfo configInfo) {
        this(configInfo, null);
    }

    public LabConfig(@NonNull TempUser tempUser, String labUserPassword) {
        this.mTempUser = tempUser;
        this.mLabUserPassword = labUserPassword;
    }

    public LabConfig(@NonNull TempUser tempUser) {
        this(tempUser, null);
    }

    public ConfigInfo getConfigInfo() {
        return mConfigInfo;
    }

    public TempUser getTempUser() {
        return mTempUser;
    }

    public String getLabUserPassword() {
        return mLabUserPassword;
    }

    public void setLabUserPassword(String labUserPassword) {
        this.mLabUserPassword = labUserPassword;
    }

    public String getAuthority() {
        if (mConfigInfo != null) {
            return mConfigInfo.getLabInfo().getAuthority();
        } else if (mTempUser != null) {
            return mTempUser.getAuthority();
        } else {
            return null;
        }
    }

    public String getAppId() {
        if (mConfigInfo == null) {
            return null;
        }

        return mConfigInfo.getAppInfo().getAppId();
    }
}
