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

/**
 * An interface for an Android app on which we can perform specific operations during a UI test.
 */
public interface IApp {

    /**
     * Returns the package name of the app.
     */
    String getPackageName();

    /**
     * Install this app on the device.
     */
    void install();

    /**
     * Launch this app on the device.
     */
    void launch();

    /**
     * Update this app on the device.
     */
    void update();

    /**
     * Clear (storage) associated to this app on the device.
     */
    void clear();

    /**
     * Remove this app from the device.
     */
    void uninstall();

    /**
     * Enable this app.
     */
    void enable();

    /**
     * Disable this app.
     */
    void disable();

    /**
     * Handle the first run experience for this app on first time launch.
     */
    void handleFirstRun();

    /**
     * Checks if this app has already been granted the supplied Android system permission.
     *
     * @param permission the permission for which to check if it has been granted
     * @return a boolean indicating whether the permission has been granted or not
     */
    boolean hasPermission(String permission);

    /**
     * Grant (allow) the requested Android system permission for this app.
     *
     * @param permission the permission that should be granted
     */
    void grantPermission(String permission);

    /**
     * Force stops (closes, shuts down) this application.
     */
    void forceStop();

    /**
     * Determines if this application (package) is installed on the device.
     *
     * @return a boolean indicating if the package is installed on the device
     */
    boolean isInstalled();

    /**
     * Copy Installed APK to supplied destination.
     */
    void copyApk(String destApkFileName);
}
