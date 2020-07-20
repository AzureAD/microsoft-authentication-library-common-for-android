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
package com.microsoft.identity.client.ui.automation.app;

import android.text.TextUtils;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.installer.PlayStore;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;

import lombok.Getter;
import lombok.Setter;

/**
 * This class represents an app during a UI Test. We can interact with this app during the test by
 * performing specific operation on/with it.
 */
@Getter
public abstract class App implements IApp {

    @Setter
    private IAppInstaller appInstaller;

    private String packageName;

    @Setter
    private String appName;

    protected String localApkFileName = null;

    public App(@NonNull final String packageName) {
        this.packageName = packageName;
        this.appInstaller = new PlayStore();
    }

    public App(@NonNull final String packageName, @NonNull final String appName) {
        this(packageName);
        this.appName = appName;
    }

    public App(@NonNull final String packageName, @NonNull final IAppInstaller appInstaller) {
        this.appInstaller = appInstaller;
        this.packageName = packageName;
    }

    public App(@NonNull final String packageName,
               @NonNull final String appName,
               @NonNull final IAppInstaller appInstaller) {
        this.appInstaller = appInstaller;
        this.packageName = packageName;
        this.appName = appName;
    }

    @Override
    public void install() {
        //TODO: make it build time configurable to specify the installer that should be used.
        // Ideally we can specify different installers on app basis
        if (appInstaller instanceof LocalApkInstaller && !TextUtils.isEmpty(localApkFileName)) {
            appInstaller.installApp(localApkFileName);
        } else {
            appInstaller.installApp(appName != null ? appName : packageName);
        }
    }

    @Override
    public void launch() {
        CommonUtils.launchApp(packageName);
    }

    @Override
    public void clear() {
        AdbShellUtils.clearPackage(packageName);
    }

    @Override
    public void uninstall() {
        AdbShellUtils.removePackage(packageName);
    }

    @Override
    public boolean hasPermission(@NonNull final String permission) {
        return CommonUtils.hasPermission(packageName, permission);
    }

    @Override
    public void grantPermission(@NonNull final String permission) {
        if (!hasPermission(permission)) {
            CommonUtils.grantPackagePermission();
        }
    }
}
