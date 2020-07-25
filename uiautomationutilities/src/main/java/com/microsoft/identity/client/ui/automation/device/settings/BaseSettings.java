package com.microsoft.identity.client.ui.automation.device.settings;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.uiautomator.UiObject;

import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;

public abstract class BaseSettings implements ISettings {

    final static String SETTINGS_PKG = "com.android.settings";

    private static void launchIntent(@NonNull final String action) {
        final Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = new Intent(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    @Override
    public void launchDeviceAdminSettingsPage() {
        final Intent intent = new Intent();
        intent.setComponent(new ComponentName(
                SETTINGS_PKG,
                SETTINGS_PKG + ".DeviceAdminSettings")
        );
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        final Context context = ApplicationProvider.getApplicationContext();
        context.startActivity(intent);

        final UiObject deviceAdminPage = UiAutomatorUtils.obtainUiObjectWithText("device admin");
        Assert.assertTrue(deviceAdminPage.exists());
    }

    @Override
    public void launchAddAccountPage() {
        launchIntent(Settings.ACTION_ADD_ACCOUNT);
    }

    @Override
    public void launchAccountListPage() {
        launchIntent(Settings.ACTION_SYNC_SETTINGS);
    }

    @Override
    public void launchDateTimeSettingsPage() {
        launchIntent(Settings.ACTION_DATE_SETTINGS);
    }
}
