package com.microsoft.identity.client.ui.automation.installer;

import androidx.test.uiautomator.UiDevice;

import org.junit.Assert;

import java.io.IOException;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class LocalApkInstaller implements IAppInstaller {

    private static final String LOCAL_APK_PATH_PREFIX = "/data/local/tmp/";

    private String mApkFolderPath;

    public LocalApkInstaller() {
        mApkFolderPath = LOCAL_APK_PATH_PREFIX;
    }

    public LocalApkInstaller(String apkFolderPath) {
        this.mApkFolderPath = apkFolderPath;
    }

    @Override
    public void installApp(final String apkFileName) {
        final String fullPath = LOCAL_APK_PATH_PREFIX + apkFileName;
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.executeShellCommand("pm install " + fullPath);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }
}
