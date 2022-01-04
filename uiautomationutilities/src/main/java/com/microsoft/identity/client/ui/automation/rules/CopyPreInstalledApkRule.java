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
package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.app.IApp;
import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;
import com.microsoft.identity.client.ui.automation.logging.Logger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * A JUnit rule to copy specified pre-installed APKs to /data/local/tmp directory on the device.
 * These APKs can then just be uninstalled and re-installed on the device by grabbing the apk from
 * the tmp directory.
 */
public class CopyPreInstalledApkRule implements TestRule {

    private final String TAG = CopyPreInstalledApkRule.class.getSimpleName();

    private List<IApp> mPreInstalledAppsToCopy;

    public CopyPreInstalledApkRule(final List<IApp> preInstalledAppsToCopy) {
        mPreInstalledAppsToCopy = preInstalledAppsToCopy;
    }

    public CopyPreInstalledApkRule(final IApp... preInstalledAppsToCopy) {
        mPreInstalledAppsToCopy = new ArrayList<>();
        mPreInstalledAppsToCopy.addAll(Arrays.asList(preInstalledAppsToCopy));
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
                for (final IApp app : mPreInstalledAppsToCopy) {
                    Logger.i(TAG, "Attempting to copy APK for " + ((App) app).getAppName());
                    if (app.isInstalled()) {
                        Logger.i(TAG, "Detected pre-installed app: " + ((App) app).getAppName());
                        Logger.i(
                                TAG,
                                "Proceeding with copying apk for: " + ((App) app).getAppName());
                        app.copyApk(getDestApkFileName(app));
                    } else {
                        Logger.i(
                                TAG,
                                "Can't copy APK for: "
                                        + ((App) app).getAppName()
                                        + " as it is not pre-installed");
                    }
                }

                base.evaluate();
            }
        };
    }

    private String getDestApkFileName(final IApp app) {
        if (((App) app).getLocalApkFileName() != null) {
            return LocalApkInstaller.LOCAL_APK_PATH_PREFIX + ((App) app).getLocalApkFileName();
        } else {
            return LocalApkInstaller.LOCAL_APK_PATH_PREFIX + app.getClass().getSimpleName();
        }
    }
}
