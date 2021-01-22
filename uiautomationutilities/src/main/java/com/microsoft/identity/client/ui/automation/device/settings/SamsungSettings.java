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

import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.Calendar;

/**
 * A model representing the Settings app on a Samsung device. Please note that this class is
 * currently optimized for a Samsung Galaxy S6 device.
 */
public class SamsungSettings extends BaseSettings {

    @Override
    public void disableAdmin(@NonNull final DeviceAdmin deviceAdmin) {
        launchDeviceAdminSettingsPage();

        try {
            // scroll down the recycler view to find the list item for this admin
            final UiObject adminAppListItem = UiAutomatorUtils.obtainChildInScrollable(
                    "android:id/list",
                    deviceAdmin.getAdminName()
            );

            // Click into this admin
            assert adminAppListItem != null;
            adminAppListItem.click();

            // Click Deactivate button
            UiAutomatorUtils.handleButtonClick("com.android.settings:id/action_button");

            if (deviceAdmin == DeviceAdmin.COMPANY_PORTAL) {
                // Confirm deactivation - CP requires confirmation
                UiAutomatorUtils.handleButtonClick("android:id/button1");
            }
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void removeAccount(@NonNull final String username) {
        launchAccountListPage();
        try {
            // scroll down the recycler view to find the list item for this account
            final UiObject account = UiAutomatorUtils.obtainChildInScrollable(
                    "android:id/list",
                    username
            );

            // Click into this account
            account.click();

            // Find the remover Account btn
            final UiObject removeAccountBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndText(
                    "com.android.settings:id/button",
                    "Remove account"
            );

            // Click the remove account btn
            removeAccountBtn.click();

            // Click confirm in the dialog to complete removal
            final UiObject removeAccountConfirmationDialogBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndText(
                    "android:id/button1",
                    "Remove account"
            );

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
            final UiObject workAccount = UiAutomatorUtils.obtainChildInScrollable(
                    "android:id/list",
                    "Work account"
            );

            // Click into the work account
            workAccount.click();

            // add work account by performing join via the broker
            broker.performJoinViaJoinActivity(username, password);

            // activate broker app as admin
            activateAdmin();

            // enroll in Knox
            enrollInKnox();

            // make sure account appears in Join activity and join successful
            broker.confirmJoinInJoinActivity(username);
        } catch (final UiObjectNotFoundException e) {
            throw new AssertionError(e);
        }
    }

    @Override
    public void forwardDeviceTimeForOneDay() {
        // Disable automatic time zone
        AdbShellUtils.disableAutomaticTimeZone();
        // Open the date & time settings page
        launchDateTimeSettingsPage();

        try {
            // Click set date btn
            final UiObject setDateBtn = UiAutomatorUtils.obtainUiObjectWithText("Set date");
            setDateBtn.click();

            // Make sure we are seeing the calendar
            final UiObject datePicker = UiAutomatorUtils.obtainUiObjectWithResourceId("android:id/sem_datepicker_calendar_header");
            Assert.assertTrue("Date Picker appears.", datePicker.exists());

            final Calendar cal = Calendar.getInstance();

            // add one to move to next day
            cal.add(Calendar.DATE, 1);

            // this is the new date
            final int dateToSet = cal.get(Calendar.DATE);

            if (dateToSet == 1) {
                // looks we are into the next month, so let's update month here too
                UiAutomatorUtils.handleButtonClick("android:id/sem_datepicker_calendar_header_next_button");
            }

            // Click the calendar item for this date (index = date - 1)
            UiObject specifiedDateIcon = UiAutomatorUtils.obtainUiObjectWithClassAndIndex(
                    View.class,
                    dateToSet - 1
            );
            specifiedDateIcon.click();

            // Click ok to set date
            UiAutomatorUtils.handleButtonClick("android:id/button1");
        } catch (final UiObjectNotFoundException e) {
           throw new AssertionError(e);
        }
    }

    @Override
    public void activateAdmin() {
        UiAutomatorUtils.handleButtonClick("com.android.settings:id/action_button");
    }

    public void enrollInKnox() {
        UiAutomatorUtils.handleButtonClick("com.samsung.klmsagent:id/checkBox1");
        UiAutomatorUtils.handleButtonClick("com.samsung.klmsagent:id/eula_bottom_confirm_agree");
    }

    @Override
    public void setPinOnDevice(final String password) {
        //TODO: implement addPinSetup for SAMSUNG device.
    }

    @Override
    public void removePinFromDevice(String pin) {
        //TODO: implement removing PIN for SAMSUNG device.
    }

    @Override
    public void setDefaultBrowser(String browserName) throws UiObjectNotFoundException {
        //TODO: implement set default browser.
    }
}
