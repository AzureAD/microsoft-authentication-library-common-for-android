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

import com.microsoft.identity.client.ui.automation.installer.LocalApkInstaller;

import lombok.NonNull;

/**
 * A model for interacting with the NativeAuth Sample App during UI automation tests.
 */
public class NativeAuthSampleApp extends App {

    public static final String NATIVE_AUTH_SAMPLE_PACKAGE_NAME = "com.azuresamples.msalnativeauthandroidkotlinsampleapp";
    public static final String NATIVE_AUTH_SAMPLE_APP_NAME = "NativeAuth Sample App";
    public static final String NATIVE_AUTH_SAMPLE_APK = "NativeAuthSampleApp.apk";

    public NativeAuthSampleApp() {
        super(NATIVE_AUTH_SAMPLE_PACKAGE_NAME, NATIVE_AUTH_SAMPLE_APP_NAME, new LocalApkInstaller());
        localApkFileName = NATIVE_AUTH_SAMPLE_APK;
        localUpdateApkFileName = NATIVE_AUTH_SAMPLE_APK;
    }

    @Override
    protected void initialiseAppImpl() {
        // No version-specific implementation needed for NativeAuthSampleApp
    }

    @Override
    public void handleFirstRun() {
        // No first-run dialog to handle for NativeAuthSampleApp
    }
}
