package com.microsoft.identity.client.ui.automation.utils;

import androidx.annotation.NonNull;
import androidx.test.uiautomator.UiDevice;

import org.junit.Assert;

import java.io.IOException;

import static androidx.test.platform.app.InstrumentationRegistry.getInstrumentation;

/**
 * This class contains utility methods that can be used to interact with the ADB Shell from within
 * code during the execution of a UI Test.
 */
public class AdbShellUtils {

    private static void executeShellCommand(@NonNull final String command) {
        final UiDevice mDevice = UiDevice.getInstance(getInstrumentation());
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
    public static void removePackage(@NonNull final String packageName) {
        executeShellCommand("pm uninstall " + packageName);
    }

    /**
     * Clear the contents of the storage associated to the given package name
     *
     * @param packageName the package name to clear
     */
    public static void clearPackage(@NonNull final String packageName) {
        executeShellCommand("pm clear " + packageName);
    }

    /**
     * Force stop (shut down) the supplied the package
     *
     * @param packageName the package to force stop
     */
    public static void forceStopPackage(@NonNull final String packageName) {
        executeShellCommand("am force-stop " + packageName);
    }

    private static void putGlobalSettings(final String settingName, final String value) {
        executeShellCommand("settings put global " + settingName + " " + value);
    }

    /**
     * Enable automatic time zone on the device
     */
    public static void enableAutomaticTimeZone() {
        putGlobalSettings("auto_time", "1");
    }

    /**
     * Disable automatic time zone on the device
     */
    public static void disableAutomaticTimeZone() {
        putGlobalSettings("auto_time", "0");
    }
}
