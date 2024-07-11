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
package com.microsoft.identity.client.ui.automation.broker;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;

/**
 * Handles app interaction with Broker-hosting app LTW
 */
public class BrokerLTW extends AbstractTestBroker {
    private static final String TAG = BrokerLTW.class.getSimpleName();
    public final static String BROKER_LTW_APP_PACKAGE_NAME = "com.microsoft.appmanager";
    public final static String BROKER_LTW_APP_NAME = "LTW App";
    public final static String BROKER_LTW_APK = "LTW.apk";
    public final static String OLD_BROKER_LTW_APK = "OldLTW.apk";

    public BrokerLTW() {
        super(BROKER_LTW_APP_PACKAGE_NAME, BROKER_LTW_APP_NAME);
        localApkFileName = BROKER_LTW_APK;
    }

    public BrokerLTW(@NonNull final IAppInstaller appInstaller) {
        super(BROKER_LTW_APP_PACKAGE_NAME, BROKER_LTW_APP_NAME, appInstaller);
        localApkFileName = BROKER_LTW_APK;
    }

    public BrokerLTW(@NonNull final IAppInstaller appInstaller, @NonNull final IAppInstaller updateAppInstaller) {
        super(BROKER_LTW_APP_PACKAGE_NAME, BROKER_LTW_APP_NAME, appInstaller, updateAppInstaller);
        localApkFileName = BROKER_LTW_APK;
    }

    public BrokerLTW(@NonNull final String ltwApkName,
                                        @NonNull final String updateLtwApkName) {
        super(BROKER_LTW_APP_PACKAGE_NAME, BROKER_LTW_APP_NAME);
        localApkFileName = ltwApkName;
        localUpdateApkFileName = updateLtwApkName;
    }

    @Override
    protected void initialiseAppImpl() {
        // Nothing
    }

    @Override
    public void handleFirstRun() {
        // Nothing
    }

    @Override
    public void performDeviceRegistration(String username, String password) {
        throw new UnsupportedOperationException("LTW doesn't support this");
    }

    @Override
    public void performDeviceRegistration(String username, String password, boolean isFederatedUser) {
        throw new UnsupportedOperationException("LTW doesn't support this");
    }

    @Override
    public void performSharedDeviceRegistration(String username, String password) {
        throw new UnsupportedOperationException("LTW doesn't support this");
    }

    @Override
    public void performSharedDeviceRegistrationDontValidate(String username, String password) {
        throw new UnsupportedOperationException("LTW doesn't support this");
    }

    @Nullable
    @Override
    public String obtainDeviceId() {
        throw new UnsupportedOperationException("LTW doesn't support this");
    }

    @Override
    public void enableBrowserAccess(@NonNull final String username) {
        throw new UnsupportedOperationException("LTW doesn't support this");
    }

    @Override
    public DeviceAdmin getAdminName() {
        throw new UnsupportedOperationException("LTW doesn't support this");
    }
}
