package com.microsoft.identity.client.ui.automation.device.settings;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

import java.util.Calendar;

import static com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils.obtainUiObjectWithExactText;

public class SamsungSettings extends BaseSettings {

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

            // Click Deactivate button
            UiAutomatorUtils.handleButtonClick("com.android.settings:id/action_button");

            if ("Company Portal".equalsIgnoreCase(adminName)) {
                // Confirm deactivation - CP requires confirmation
                UiAutomatorUtils.handleButtonClick("android:id/button1");
            }
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }

    @Override
    public void removeAccount(@NonNull String username) {
        launchAccountListPage();
        try {
            final UiObject account = UiAutomatorUtils.obtainChildInScrollable(
                    "android:id/list",
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
                    "android:id/list",
                    "Work account"
            );

            workAccount.click();

            broker.performJoinViaJoinActivity(username, password);

            activateAdmin();
            enrollInKnox();

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
        UiAutomatorUtils.handleButtonClick("com.android.settings:id/action_button");
    }

    public void enrollInKnox() {
        UiAutomatorUtils.handleButtonClick("com.samsung.klmsagent:id/checkBox1");
        UiAutomatorUtils.handleButtonClick("com.samsung.klmsagent:id/eula_bottom_confirm_agree");
    }
}
