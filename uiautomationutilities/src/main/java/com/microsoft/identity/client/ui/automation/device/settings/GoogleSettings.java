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
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT_LONG;
import static com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils.obtainUiObjectWithExactText;

/**
 * A model representing the Settings app on a Google device. Please note that this class is
 * currently optimized for a Google Pixel 2 device.
 */
public class GoogleSettings extends BaseSettings {

    private final static String TAG = GoogleSettings.class.getSimpleName();

    @Override
    public void disableAdmin(@NonNull final DeviceAdmin deviceAdmin) {
        Logger.i(TAG, "Disabling Admin on Google Device..");
        launchDeviceAdminSettingsPage();

        try {
            // scroll down the recycler view to find the list item for this admin
            final UiObject adminAppListItem = obtainDisableAdminButton(deviceAdmin);

            // select this admin by clicking it
            assert adminAppListItem != null;
            adminAppListItem.click();

            // Check if work profile is present
            final UiObject removeWorkProfileBtn = UiAutomatorUtils.obtainUiObjectWithExactText("Remove work profile");

            if (removeWorkProfileBtn.waitForExists(TimeUnit.SECONDS.toMillis(3))) {
                removeWorkProfileBtn.click();

                // Click confirmation
                UiAutomatorUtils.handleButtonClick("android:id/button1");

                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(15));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                // scroll down the recycler view to find btn to deactivate admin
                final UiObject deactivateBtn = UiAutomatorUtils.obtainChildInScrollable(
                        android.widget.ScrollView.class,
                        "Deactivate this device admin app"
                );

                // click the deactivate admin btn
                deactivateBtn.click();

                // Click confirmation
                UiAutomatorUtils.handleButtonClick("android:id/button1");
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void removeAccount(@NonNull final String username) {
        Logger.i(TAG, "Removing Account from Google Device..");
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
        addWorkAccount(broker, username, password, false);
    }

    public void addWorkAccount(@NonNull final ITestBroker broker,
                               @NonNull final String username,
                               @NonNull final String password,
                               final boolean isFederatedUser) {
        Logger.i(TAG, "Adding Work Account on Google Device..");
        launchAddAccountPage();

        try {
            // scroll down the recycler view to find the list item for this account type
            final UiObject workAccount = obtainButtonInScrollable("Work account");

            // Click into this account type
            workAccount.click();

            // perform Join using the supplied broker
            broker.performJoinViaJoinActivity(username, password, isFederatedUser);

            final UiDevice device =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            // Find the cert installer and make sure it exists
            UiObject certInstaller = device.findObject(new UiSelector().packageName("com.android.certinstaller"));
            Assert.assertTrue(
                    "Cert Installer appears while adding work account",
                    certInstaller.waitForExists(FIND_UI_ELEMENT_TIMEOUT_LONG)
            );

            // Confirm install cert
            UiAutomatorUtils.handleButtonClick("android:id/button1");

            // Confirm cert name (API 30+ only)
            if (android.os.Build.VERSION.SDK_INT >= 30) {
                UiAutomatorUtils.handleButtonClickSafely("android:id/button1");
            }

            // Make sure account appears in Join Activity afterwards
            broker.confirmJoinInJoinActivity(username);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void forwardDeviceTimeForOneDay() {
        forwardDeviceTime(TimeUnit.DAYS.toSeconds(1), false);
    }

    /**
     * Change the time on the device by advancing the clock by seconds.
     *
     * @param seconds amount to advance time by
     */
    @Override
    public void forwardDeviceTime(long seconds, final boolean enableTimeZone) {
        Logger.i(TAG, "Forwarding Time by " + seconds + " seconds on Google Device");
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

            // add the # of seconds to forward device time
            cal.add(Calendar.SECOND, (int) seconds);

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

            // Reset to Automatic Time Zone to avoid chain validation failures
            // Some cases fail if you do this, however, so setting it to a boolean parameter
            if (enableTimeZone) {
                try {
                    Thread.sleep(TimeUnit.SECONDS.toMillis(3));
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                AdbShellUtils.enableAutomaticTimeZone();
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    private UiObject obtainDateButton() {
        Logger.i(TAG, "Obtain Date Button on Google Device..");
        if (android.os.Build.VERSION.SDK_INT == 28) {
            return UiAutomatorUtils.obtainUiObjectWithText("Set date");
        }

        return UiAutomatorUtils.obtainEnabledUiObjectWithExactText("Date");
    }

    @Override
    public void activateAdmin() {
        Logger.i(TAG, "Activating Admin on Google Device..");
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

    private UiObject obtainButtonInScrollable(final String Text) {
        Logger.i(TAG, "Obtain Button In Scrollable on Google Device..");
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
        Logger.i(TAG, "Obtain Disable Admin Button on Google Device..");
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
            Logger.i(TAG, "Set Pin on Google Device..");
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
            Logger.i(TAG, "Remove Pin on Google Device..");
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

    private void handleDoneButton() throws UiObjectNotFoundException {
        Logger.i(TAG, "Handle Done Button on Google Device..");
        if (android.os.Build.VERSION.SDK_INT == 28) {
            UiAutomatorUtils.handleButtonClick("com.android.settings:id/redaction_done_button");
        } else {
            try {
                final UiObject doneButton = UiAutomatorUtils.obtainUiObjectWithExactText("Done");
                doneButton.click();
            } catch (UiObjectNotFoundException e) {
                Logger.i(TAG, "First Done button attempt failed: " + e.getMessage());
                final UiObject doneButton = UiAutomatorUtils.obtainUiObjectWithExactText("DONE");
                doneButton.click();
            }
        }
    }

}

