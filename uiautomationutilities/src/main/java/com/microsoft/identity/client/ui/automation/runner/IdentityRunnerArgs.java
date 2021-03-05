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
package com.microsoft.identity.client.ui.automation.runner;

import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.BuildConfig;
import com.microsoft.identity.client.ui.automation.installer.AppInstallSource;

/**
 * Test runner args to be used with {@link IdentityTestRunner}.
 */
public class IdentityRunnerArgs {

    private final static String TAG = IdentityRunnerArgs.class.getSimpleName();

    private static boolean sPreferPreInstalledApks = BuildConfig.PREFER_PRE_INSTALLED_APKS;
    private static String sBrokerSource = BuildConfig.BROKER_INSTALL_SOURCE;

    /**
     * Set whether the test runner should prefer pre installed apks when installing broker apps etc.
     *
     * @param preferPreInstalledApks a boolean representing whether to prefer pre installed apks
     */
    public static void setPreferPreInstalledApks(final boolean preferPreInstalledApks) {
        Log.i(TAG, "Setting value for prefer pre-installed apks. " +
                "Old Value: " + sPreferPreInstalledApks + " , New Value: " + preferPreInstalledApks);
        sPreferPreInstalledApks = preferPreInstalledApks;
    }

    /**
     * Set the broker installation source to be used by the test runner when installing broker apps
     * such as Microsoft Authenticator or Company Portal
     *
     * @param brokerSource a value for Broker Install source
     */
    public static void setBrokerSource(@NonNull final String brokerSource) {
        Log.i(TAG, "Setting value for broker source. " +
                "Old Value: " + sBrokerSource + " , New Value: " + brokerSource);
        sBrokerSource = brokerSource;
    }

    /**
     * Get whether to prefer pre installed apks during automation.
     *
     * @return a boolean representing whether to prefer pre installed apks
     */
    public static boolean shouldPreferPreInstalledApks() {
        return sPreferPreInstalledApks;
    }

    /**
     * Get the broker install source to be used when installing broker apps during automation.
     *
     * @return a String representing the broker installation source
     */
    public static String getBrokerSource() {
        return sBrokerSource;
    }

}
