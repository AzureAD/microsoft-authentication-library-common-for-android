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

import android.os.Build;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.Calendar;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils.obtainUiObjectWithExactText;

/**
 * A model representing the Settings app on a Google device. Please note that this class is
 * currently optimized for a Google Pixel 2 device.
 */
public class GoogleSettings extends BaseSettings {

    @Override
    public void disableAdmin(@NonNull final DeviceAdmin deviceAdmin) {
        launchDeviceAdminSettingsPage();

        try {
            // scroll down the recycler view to find the list item for this admin
            final UiObject adminAppListItem = obtainDisableAdminButton(deviceAdmin);

            // select this admin by clicking it
            assert adminAppListItem != null;
            adminAppListItem.click();

            // scroll down the recycler view to find btn to deactivate admin
            final UiObject deactivateBtn = UiAutomatorUtils.obtainChildInScrollable(
                    android.widget.ScrollView.class,
                    "Deactivate this device admin app"
            );

            // click the deactivate admin btn
            deactivateBtn.click();

            // Click confirmation
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void removeAccount(@NonNull final String username) {
        launchAccountListPage();

        try {
            // find the list item associated to this account
            final UiObject account = obtainButtonInScrollable(username);

            // Click this account
            account.click();

            final UiObject removeAccountBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndText(
                    "com.android.settings:id/button",
                    "Remove account"
            );

            // Click the removeAccountBtn
            removeAccountBtn.click();

            final UiObject removeAccountConfirmationDialogBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndText(
                    "android:id/button1",
                    "Remove account"
            );

            // Click confirm in confirmation dialog
            removeAccountConfirmationDialogBtn.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void addWorkAccount(@NonNull final ITestBroker broker,
                               @NonNull final String username,
                               @NonNull final String password) {
        launchAddAccountPage();

        try {
            // scroll down the recycler view to find the list item for this account type
            final UiObject workAccount = obtainButtonInScrollable("Work account");

            // Click into this account type
            workAccount.click();

            // perform Join using the supplied broker
            broker.performJoinViaJoinActivity(username, password);

            final UiDevice device =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            // Find the cert installer and make sure it exists
            UiObject certInstaller = device.findObject(new UiSelector().packageName("com.android.certinstaller"));
            certInstaller.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            Assert.assertTrue(
                    "Cert Installer appears while adding work account",
                    certInstaller.exists()
            );

            // Confirm install cert
            UiAutomatorUtils.handleButtonClick("android:id/button1");

            // Make sure account appears in Join Activity afterwards
            broker.confirmJoinInJoinActivity(username);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void forwardDeviceTimeForOneDay() {
        // Disable Automatic TimeZone
        AdbShellUtils.disableAutomaticTimeZone();
        // Launch the date time settings page
        launchDateTimeSettingsPage();

        try {
            // Click the set date button
            final UiObject setDateBtn = obtainDateButton();
            setDateBtn.click();

            // Make sure we see the calendar
            final UiObject datePicker = UiAutomatorUtils.obtainUiObjectWithResourceId("android:id/date_picker_header_date");
            Assert.assertTrue("Date Picker appears", datePicker.exists());

            final Calendar cal = Calendar.getInstance();

            // add one to move to next day
            cal.add(Calendar.DATE, 1);

            // this is the new date
            final int dateToSet = cal.get(Calendar.DATE);

            if (dateToSet == 1) {
                // looks we are into the next month, so let's update month here too
                UiAutomatorUtils.handleButtonClick("android:id/next");
            }

            // Click on this new date in this calendar
            UiObject specifiedDateIcon = obtainUiObjectWithExactText(
                    String.valueOf(dateToSet)
            );
            specifiedDateIcon.click();

            // Confirm setting date
            final UiObject okBtn = UiAutomatorUtils.obtainUiObjectWithText("OK");
            okBtn.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void activateAdmin() {
        try {
            // scroll down the recycler view to find activate device admin btn
            final UiObject activeDeviceAdminBtn = UiAutomatorUtils.obtainChildInScrollable(
                    "Activate this device admin app"
            );

            assert activeDeviceAdminBtn != null;

            // click on activate device admin btn
            activeDeviceAdminBtn.click();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private UiObject obtainDateButton() {
        if (android.os.Build.VERSION.SDK_INT == 28) {
            return UiAutomatorUtils.obtainUiObjectWithText("Set date");
        }

        return UiAutomatorUtils.obtainEnabledUiObjectWithExactText("Date");
    }

    private UiObject obtainButtonInScrollable(final String Text) {
        if (android.os.Build.VERSION.SDK_INT == 28) {
            return UiAutomatorUtils.obtainChildInScrollable(
                    "com.android.settings:id/list",
                    Text
            );
        }

        return UiAutomatorUtils.obtainChildInScrollable(
                "com.android.settings:id/recycler_view",
                Text
        );
    }


    private UiObject obtainDisableAdminButton(final DeviceAdmin deviceAdmin) {
        if (android.os.Build.VERSION.SDK_INT == 28) {
            return UiAutomatorUtils.obtainChildInScrollable(
                    "android:id/list",
                    deviceAdmin.getAdminName()
            );
        }

        return UiAutomatorUtils.obtainChildInScrollable(
                "com.android.settings:id/recycler_view",
                deviceAdmin.getAdminName()
        );
    }

    @Override
    public void setPinOnDevice(final String pin) {
        try {
            final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            launchScreenLockPage();
            final UiObject screenLock = UiAutomatorUtils.obtainUiObjectWithText("Screen lock");
            Assert.assertTrue(screenLock.exists());
            screenLock.click();
            UiAutomatorUtils.handleButtonClick("com.android.settings:id/lock_pin");
            UiAutomatorUtils.handleInput("com.android.settings:id/password_entry", pin);
            device.pressEnter();
            UiAutomatorUtils.handleInput("com.android.settings:id/password_entry", pin);
            device.pressEnter();
            handleDoneButton();
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void removePinFromDevice(final String pin) {
        try {
            final UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
            launchScreenLockPage();
            final UiObject screenLock = UiAutomatorUtils.obtainUiObjectWithText("Screen lock");
            Assert.assertTrue(screenLock.exists());
            screenLock.click();
            UiAutomatorUtils.handleInput("com.android.settings:id/password_entry", pin);
            device.pressEnter();
            // Click Lock None
            UiAutomatorUtils.handleButtonClick("com.android.settings:id/lock_none");
            // confirm removal of screen lock
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void setDefaultBrowser(String browserName) throws UiObjectNotFoundException {
        launchDefaultAppsPage();
        handleBrowserApp();
        handleSetBrowserDefault(browserName);
    }

    private void handleDoneButton() throws UiObjectNotFoundException {
        if (android.os.Build.VERSION.SDK_INT == 28) {
            UiAutomatorUtils.handleButtonClick("com.android.settings:id/redaction_done_button");
        } else {
            final UiObject doneButton = UiAutomatorUtils.obtainUiObjectWithExactText("Done");
            doneButton.click();
        }
    }

    private void handleBrowserApp() throws UiObjectNotFoundException {
        if (android.os.Build.VERSION.SDK_INT == 28) {
            final UiObject defaultBrowser = UiAutomatorUtils.obtainChildInScrollable("com.android.settings:id/list", "Browser app");
            defaultBrowser.click();
        } else {
            final UiObject defaultBrowser = UiAutomatorUtils.obtainChildInScrollable("com.android.permissioncontroller:id/recycler_view", "Browser app");
            defaultBrowser.click();
        }
    }

    private void handleSetBrowserDefault(final String browserName) throws UiObjectNotFoundException {
        if (android.os.Build.VERSION.SDK_INT == 28) {
            final UiObject selectBrowser = UiAutomatorUtils.obtainChildInScrollable("com.android.settings:id/list", browserName);
            selectBrowser.click();
        } else {
            UiObject selectBrowser = UiAutomatorUtils.obtainChildInScrollable("com.android.permissioncontroller:id/recycler_view", browserName);
            selectBrowser.click();
        }
    }

}

