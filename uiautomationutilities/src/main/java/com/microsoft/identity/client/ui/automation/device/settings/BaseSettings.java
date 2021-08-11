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

import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.Calendar;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.launchIntent;
import static com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils.obtainUiObjectWithExactText;

public abstract class BaseSettings implements ISettings {

    private final static String TAG = BaseSettings.class.getSimpleName();
    final static String SETTINGS_PKG = "com.android.settings";

    @Override
    public void launchDeviceAdminSettingsPage() {
        Logger.i(TAG, "Launching Device Admin Settings Page..");
        final Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                SETTINGS_PKG,
                SETTINGS_PKG + ".DeviceAdminSettings")
        );
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        final Context context = ApplicationProvider.getApplicationContext();
        context.startActivity(intent);

        final UiObject deviceAdminPage = UiAutomatorUtils.obtainUiObjectWithText("device admin");
        Assert.assertTrue("Device Admin Settings Page appears", deviceAdminPage.exists());
    }

    @Override
    public void launchAddAccountPage() {
        Logger.i(TAG, "Launching Add Account Page..");
        launchIntent(Settings.ACTION_ADD_ACCOUNT);
    }

    @Override
    public void launchAccountListPage() {
        Logger.i(TAG, "Launching Account List Page..");
        launchIntent(Settings.ACTION_SYNC_SETTINGS);
    }

    @Override
    public void launchDateTimeSettingsPage() {
        Logger.i(TAG, "Open the date & time settings page..");
        launchIntent(Settings.ACTION_DATE_SETTINGS);
    }

    @Override
    public void launchScreenLockPage() {
        Logger.i(TAG, "Launching Screen Lock Page..");
        launchIntent(Settings.ACTION_SECURITY_SETTINGS);
    }

    @Override
    public void launchAppInfoPage(@NonNull final String packageName) {
        try {
            //Open the specific App Info page:
            final Uri data = Uri.parse("package:" + packageName);
            launchIntent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, data);
        } catch (final ActivityNotFoundException e) {
            Logger.e(TAG, "Package: " + packageName + " probably not available on device.", e);
            // we could probably install the package from PlayStore (but for now let's fail the test
            // to see if this ever happens in the wild)
            throw new AssertionError(e);
        }
    }

    public void forwardDeviceTime(int seconds) {
        Logger.i(TAG, "Forwarding Time For One Day on Google Device..");
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

            // move by seconds
            cal.add(Calendar.SECOND, seconds);

            // this is the new date
            final int dateToSet = cal.get(Calendar.DATE);

            if (dateToSet == 1) {
                // looks if we are into the next month, so let's update month here too
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

    private UiObject obtainDateButton() {
        Logger.i(TAG, "Obtain Date Button on Google Device..");
        if (android.os.Build.VERSION.SDK_INT == 28) {
            return UiAutomatorUtils.obtainUiObjectWithText("Set date");
        }

        return UiAutomatorUtils.obtainEnabledUiObjectWithExactText("Date");
    }
}
