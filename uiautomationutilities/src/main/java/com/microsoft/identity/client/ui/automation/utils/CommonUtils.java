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
package com.microsoft.identity.client.ui.automation.utils;

import android.content.Context;
import android.content.Intent;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.uiautomator.UiDevice;

import org.junit.Assert;

import java.io.IOException;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class CommonUtils {

    public final static long TIMEOUT = 1000 * 60;

    /**
     * Launch (open) the supplied package on the device
     *
     * @param packageName the package name to launch
     */
    public static void launchApp(final String packageName) {
        final Context context = ApplicationProvider.getApplicationContext();
        Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);  //sets the intent to start your app
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);  //clear out any previous task, i.e., make sure it starts on the initial screen
        context.startActivity(intent);
    }

    /**
     * Remove the supplied package name from the device
     *
     * @param packageName the packahe name to remove
     */
    public static void removeApp(final String packageName) {
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.executeShellCommand("pm uninstall " + packageName);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Clear the contents of the storage associated to the given package name
     *
     * @param packageName the package name to clear
     */
    public static void clearApp(final String packageName) {
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.executeShellCommand("pm clear " + packageName);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Get the complete resource id by combining the package name and the actual resource id
     *
     * @param appPackageName     the package name for the app
     * @param internalResourceId the resource id for the element
     * @return
     */
    public static String getResourceId(final String appPackageName, final String internalResourceId) {
        return appPackageName + ":id/" + internalResourceId;
    }

    static boolean isStringPackageName(final String hint) {
        return hint.contains("."); // best guess
    }
}
