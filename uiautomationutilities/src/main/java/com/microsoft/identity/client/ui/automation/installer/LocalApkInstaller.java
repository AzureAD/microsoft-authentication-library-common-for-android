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
package com.microsoft.identity.client.ui.automation.installer;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiDevice;

import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import org.junit.Assert;

import java.io.IOException;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class LocalApkInstaller implements IAppInstaller {

    // Files would be picked up from here (by Default) so they should be pushed to this folder on the device
    public static final String LOCAL_APK_PATH_PREFIX = "/sdcard/dir1/";

    private String mApkFolderPath;

    public LocalApkInstaller() {
        mApkFolderPath = LOCAL_APK_PATH_PREFIX;
    }

    public LocalApkInstaller(final String apkFolderPath) {
        this.mApkFolderPath = apkFolderPath;
    }

    @Override
    public void installApp(@NonNull final String apkFileName) {
        final String fullPath = LOCAL_APK_PATH_PREFIX + apkFileName;
        // using -t flag to also allow installation of test only packages
        AdbShellUtils.installPackage(fullPath, "-t");
    }

    @Override
    public void updateApp(@NonNull final String apkFileName) {
        final String fullPath = LOCAL_APK_PATH_PREFIX + apkFileName;
        // adding -r flag will reinstall the apk
        // -d flag will allow version downgrade as well
        AdbShellUtils.installPackage(fullPath, "-t", "-r", "-d");
    }
}
