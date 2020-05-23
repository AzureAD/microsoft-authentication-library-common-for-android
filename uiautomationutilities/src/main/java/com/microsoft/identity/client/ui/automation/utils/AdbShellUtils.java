package com.microsoft.identity.client.ui.automation.utils;

import androidx.test.uiautomator.UiDevice;

import org.junit.Assert;

import java.io.IOException;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

public class AdbShellUtils {

    private static void executeShellCommand(final String command) {
        UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
        try {
            mDevice.executeShellCommand(command);
        } catch (IOException e) {
            Assert.fail(e.getMessage());
        }
    }

    /**
     * Remove the supplied package name from the device
     *
     * @param packageName the package name to remove
     */
    public static void removePackage(final String packageName) {
        executeShellCommand("pm uninstall " + packageName);
    }

    /**
     * Clear the contents of the storage associated to the given package name
     *
     * @param packageName the package name to clear
     */
    public static void clearPackage(final String packageName) {
        executeShellCommand("pm clear " + packageName);
    }
}
