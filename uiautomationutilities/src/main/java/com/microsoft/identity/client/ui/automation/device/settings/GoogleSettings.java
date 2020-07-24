package com.microsoft.identity.client.ui.automation.device.settings;

import androidx.annotation.NonNull;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.Calendar;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils.obtainUiObjectWithExactText;

public class GoogleSettings extends BaseSettings {

    @Override
    public void disableAdmin(@NonNull String adminName) {
        launchDeviceAdminSettingsPage();

        try {
            // scroll down the recycler view to find the list item for this admin
            final UiObject adminAppListItem = UiAutomatorUtils.obtainChildInScrollable(
                    "android:id/list",
                    adminName
            );

            assert adminAppListItem != null;
            adminAppListItem.click();

            // scroll down the recycler view to find btn to deactivate admin
            final UiObject deactivateBtn = UiAutomatorUtils.obtainChildInScrollable(
                    android.widget.ScrollView.class,
                    "Deactivate this device admin app"
            );

            deactivateBtn.click();

            UiAutomatorUtils.handleButtonClick("android:id/button1");
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Override
    public void removeAccount(@NonNull String username) {
        launchAccountListPage();
        try {
            final UiObject account = UiAutomatorUtils.obtainChildInScrollable(
                    "com.android.settings:id/list",
                    username
            );
            account.click();

            final UiObject removeAccountBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndText(
                    "com.android.settings:id/button",
                    "Remove account"
            );

            removeAccountBtn.click();

            final UiObject removeAccountConfirmationDialogBtn = UiAutomatorUtils.obtainUiObjectWithResourceIdAndText(
                    "android:id/button1",
                    "Remove account"
            );

            removeAccountConfirmationDialogBtn.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Override
    public void addWorkAccount(ITestBroker broker, String username, String password) {
        launchAddAccountPage();

        try {
            // scroll down the recycler view to find the list item for this account type
            final UiObject workAccount = UiAutomatorUtils.obtainChildInScrollable(
                    "com.android.settings:id/list",
                    "Work account"
            );

            workAccount.click();

            broker.performJoinViaJoinActivity(username, password);

            final UiDevice device =
                    UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());

            UiObject certInstaller = device.findObject(new UiSelector().packageName("com.android.certinstaller"));
            certInstaller.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            Assert.assertTrue(certInstaller.exists());

            UiAutomatorUtils.handleButtonClick("android:id/button1");

            broker.confirmJoinInJoinActivity(username);
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Override
    public void changeDeviceTime() {
        AdbShellUtils.disableAutomaticTimeZone();
        launchDateTimeSettingsPage();

        try {
            final UiObject setTimeBtn = UiAutomatorUtils.obtainUiObjectWithText("Set date");
            setTimeBtn.click();

            final UiObject datePicker = UiAutomatorUtils.obtainUiObjectWithResourceId("android:id/date_picker_header_date");
            Assert.assertTrue(datePicker.exists());

            final Calendar cal = Calendar.getInstance();

            // add one to move to next day
            cal.add(Calendar.DATE, 1);

            // this is the new date
            final int dateToSet = cal.get(Calendar.DATE);

            if (dateToSet == 1) {
                // looks we are into the next month, so let's update month here too
                UiAutomatorUtils.handleButtonClick("android:id/next");
            }

            UiObject specifiedDateIcon = obtainUiObjectWithExactText(
                    String.valueOf(dateToSet)
            );
            specifiedDateIcon.click();

            final UiObject okBtn = UiAutomatorUtils.obtainUiObjectWithText("OK");
            okBtn.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
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
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }
}
