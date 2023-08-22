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
package com.microsoft.identity.client.ui.automation.utils;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerLTW;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.logging.Logger;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class CommonUtils {

    private final static String TAG = CommonUtils.class.getSimpleName();
    public final static long FIND_UI_ELEMENT_TIMEOUT = TimeUnit.SECONDS.toMillis(15);
    public final static long FIND_UI_ELEMENT_TIMEOUT_LONG = TimeUnit.SECONDS.toMillis(30);

    private final static String SD_CARD = "/sdcard";

    /**
     * Launch (open) the supplied package on the device.
     *
     * @param packageName the package name to launch
     */
    public static void launchApp(@NonNull final String packageName) {
        Logger.i(TAG, "Launch/Open " + packageName + " App..");
        final Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = context.getPackageManager().getLaunchIntentForPackage(packageName);  //sets the intent to start your app
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);  //clear out any previous task, i.e., make sure it starts on the initial screen
        context.startActivity(intent);
    }

    /**
     * Grant (allow) the requested permission for the current package i.e the app that is currently
     * open on the device and requesting a permission.
     * <p>
     * When any app requests a permission on Android, it shows an Android system dialog on the UI,
     * this dialog looks the same regardless of the app or the permission being requested. This API
     * just responds to that by accepting that permission.
     */
    public static void grantPackagePermission() {
        Logger.i(TAG, "Granting the requested permission to the package by handling allow permission dialog..");
        if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.P) {
            UiAutomatorUtils.handleButtonClick("com.android.packageinstaller:id/permission_allow_button");
        } else {
            UiAutomatorUtils.handleButtonClick("com.android.permissioncontroller:id/permission_allow_button");
        }
    }

    /**
     * Check if the supplied permission has already been granted for given package.
     *
     * @param packageName the package for which to check if permission was granted
     * @param permission  the permission which to check for
     * @return a boolean indicating whether permission was already granted or not
     */
    public static boolean hasPermission(@NonNull final String packageName, @NonNull final String permission) {
        Logger.i(TAG, "Check if given permission:" + permission + " for " + packageName + " has been granted or not..");
        final Context context = ApplicationProvider.getApplicationContext();
        final PackageManager packageManager = context.getPackageManager();
        return PackageManager.PERMISSION_GRANTED == packageManager.checkPermission(
                permission,
                packageName
        );
    }

    /**
     * Get the complete resource id by combining the package name and the actual resource id.
     *
     * @param appPackageName     the package name for the app
     * @param internalResourceId the resource id for the element
     * @return
     */
    public static String getResourceId(@NonNull final String appPackageName, @NonNull final String internalResourceId) {
        return appPackageName + ":id/" + internalResourceId;
    }

    /**
     * Checks if the supplied String could be a valid Android package name.
     *
     * @param hint the String for which to check if it is a package name
     * @return a boolean indicating whether the supplied String is a valid Android package name
     */
    public static boolean isStringPackageName(@NonNull final String hint) {
        return hint.contains("."); // best guess
    }

    /**
     * Checks if the specified package is installed on the device.
     *
     * @param packageName the package name to check
     * @return a boolean indicating if the package is installed
     */
    public static boolean isPackageInstalled(@NonNull final String packageName) {
        Logger.i(TAG, "Checks if the " + packageName + " is installed on the device..");
        final Context context = ApplicationProvider.getApplicationContext();
        final PackageManager packageManager = context.getPackageManager();
        final List<ApplicationInfo> packages = packageManager.getInstalledApplications(0);

        for (final ApplicationInfo applicationInfo : packages) {
            if (applicationInfo.packageName.equals(packageName))
                return true;
        }

        return false;
    }

    /**
     * Get a list of all brokers supported by our MSAL/ADAL sdks. These list contains all possible
     * broker apps regardless of active build variant.
     *
     * @return a {@link List} of {@link ITestBroker} objects
     */
    public static List<ITestBroker> getAllPossibleTestBrokers() {
        Logger.i(TAG, "Get the List of all Possible Test Brokers..");
        return Arrays.asList(
                new ITestBroker[]{
                        new BrokerCompanyPortal(),
                        new BrokerMicrosoftAuthenticator(),
                        new BrokerHost(),
                        new BrokerLTW()
                }
        );
    }

    /**
     * Copy the provided file object to the sdcard directory on the device. Callers can optionally
     * supply a folder name to copy the file within that folder inside sdcard.
     *
     * @param file   the file to copy
     * @param folder the folder inside sdcard where to copy the file
     */
    public static void copyFileToFolderInSdCard(final File file, @Nullable final String folder) {
        Logger.i(TAG, "Copy the provided file object to " + folder + " inside sdcard directory on the device..");
        final String filePath = file.getAbsolutePath();
        final String destinationPath = SD_CARD + ((folder == null) ? "" : ("/" + folder));
        final File dir = new File(destinationPath);
        final File destFile = new File(dir, file.getName());
        final String destFilePath = destFile.getAbsolutePath();
        AdbShellUtils.executeShellCommand("mkdir -p " + destinationPath);
        AdbShellUtils.executeShellCommandAsCurrentPackage("cp " + filePath + " " + destFilePath);
    }

    /**
     * Launches an activity specified by the action.
     *
     * @param action action is which operation to be performed.
     */
    public static void launchIntent(@NonNull final String action) {
        Logger.i(TAG, "Launching an activity specified by the action: " + action);
        final Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = new Intent(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        context.startActivity(intent);
    }

    /**
     * Launches an activity specified by the action and data.
     *
     * @param action action is which operation to be performed
     * @param data   the data to pass to the intent
     */
    public static void launchIntent(@NonNull final String action, @NonNull final Uri data) {
        final Context context = ApplicationProvider.getApplicationContext();
        final Intent intent = new Intent(action);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        intent.setData(data);
        context.startActivity(intent);
    }
}
