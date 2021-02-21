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

import android.os.Bundle;

import androidx.test.runner.AndroidJUnitRunner;

import com.microsoft.identity.client.ui.automation.installer.AppInstallSource;

public class IdentityTestRunner extends AndroidJUnitRunner {

    public static final String PREFER_PRE_INSTALLED_APKS = "prefer_pre_installed_apks";
    public static final String BROKER_SOURCE = "broker_source";

    @Override
    public void onCreate(Bundle arguments) {
        IdentityRunnerArgs.brokerSource = arguments.getString(
                BROKER_SOURCE, AppInstallSource.PlayStore.getName()
        );
        IdentityRunnerArgs.preferPreInstalledApks = Boolean.parseBoolean(
                arguments.getString(PREFER_PRE_INSTALLED_APKS, "false")
        );
        super.onCreate(arguments);
    }
}
