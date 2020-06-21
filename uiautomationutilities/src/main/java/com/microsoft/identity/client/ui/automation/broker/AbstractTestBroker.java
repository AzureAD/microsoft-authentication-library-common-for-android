package com.microsoft.identity.client.ui.automation.broker;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;
import androidx.test.uiautomator.UiSelector;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.installer.IAppInstaller;

import org.junit.Assert;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.FIND_UI_ELEMENT_TIMEOUT;
import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.getResourceId;

public abstract class AbstractTestBroker extends App implements ITestBroker {

    public AbstractTestBroker(@NonNull String packageName,
                              @NonNull String appName,
                              @NonNull IAppInstaller appInstaller) {
        super(packageName, appName, appInstaller);
    }

    @Override
    public void handleAccountPicker(@Nullable final String username) {
        final UiDevice device = UiDevice.getInstance(getInstrumentation());

        // find the object associated to this username in account picker
        final UiObject accountSelected = device.findObject(new UiSelector().resourceId(
                getResourceId(getPackageName(), "account_chooser_listView")
        ).childSelector(new UiSelector().textContains(
                TextUtils.isEmpty(username) ? "Use another account" : username
        )));

        try {
            accountSelected.waitForExists(FIND_UI_ELEMENT_TIMEOUT);
            accountSelected.click();
        } catch (UiObjectNotFoundException e) {
            Assert.fail(e.getMessage());
        }
    }


}
