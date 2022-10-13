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
package com.microsoft.identity.client.ui.automation.device.settings;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;

/**
 * An interface describing the Settings app on an Android device during UI Automation.
 */
public interface ISettings {

    /**
     * Launch the device admin page in Settings app.
     */
    void launchDeviceAdminSettingsPage();

    /**
     * Disable the supplied admin app on the device.
     *
     * @param deviceAdmin the admin app to be disabled
     */
    void disableAdmin(@NonNull final DeviceAdmin deviceAdmin);

    /**
     * Launch the add account page in Settings app.
     */
    void launchAddAccountPage();

    /**
     * Launch the account list page in Settings app.
     */
    void launchAccountListPage();

    /**
     * Remove the supplied account from the Account Manager.
     *
     * @param username the username of the account to be removed
     */
    void removeAccount(@NonNull final String username);

    /**
     * Add the supplied account to the device via Account Manager.
     *
     * @param expectedBroker the broker expected to be used for the account type
     * @param username       the username of the account to add
     * @param password       the password of the account to add
     */
    void addWorkAccount(final ITestBroker expectedBroker,
                        final String username,
                        final String password);

    /**
     * Add the supplied account to the device via Account Manager.
     *
     * @param expectedBroker  the broker expected to be used for the account type
     * @param username        the username of the account to add
     * @param password        the password of the account to add
     * @param isFederatedUser whether the user is federated user or not
     */
    void addWorkAccount(final ITestBroker expectedBroker,
                        final String username,
                        final String password,
                        final boolean isFederatedUser);

    /**
     * Launch the date & time page in Settings app.
     */
    void launchDateTimeSettingsPage();

    /**
     * Change the time on the device by advancing the clock by 24 hours.
     */
    void forwardDeviceTimeForOneDay();

    /**
     * Change the time on the device by seconds.
     *
     * @param seconds time to advance device time by
     * @param resetTimeZone whether or not to enable automatic time zone after changing time
     */
    void forwardDeviceTime(long seconds, boolean resetTimeZone);

    /**
     * Activate this admin app. This method is supposed to be called when the Activate Device Admin
     * UI appears on the device. It will activate the admin for whichever admin requested the activation.
     */
    void activateAdmin();

    /**
     * Adds screen lock to the device.
     */
    void setPinOnDevice(final String pin);

    /**
     * Remove screen lock from the device.
     */
    void removePinFromDevice(final String pin);

    /**
     * Launches the security page in Settings app.
     */
    void launchScreenLockPage();

    /**
     * Launch the app info page for the supplied package name.
     *
     * @param packageName the package for which to open app info page
     */
    void launchAppInfoPage(String packageName);

}
