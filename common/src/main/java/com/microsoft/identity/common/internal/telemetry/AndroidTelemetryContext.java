// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.telemetry;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import androidx.core.content.pm.PackageInfoCompat;

import com.microsoft.identity.common.java.telemetry.AbstractTelemetryContext;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.logging.Logger;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

/**
 * TelemetryContext for Android.
 * Containing Android Metadata. It also persists data in SharedPreferences.
 */
public class AndroidTelemetryContext extends AbstractTelemetryContext {

    private static final String TAG = AndroidTelemetryContext.class.getName();

    @SuppressFBWarnings(value = "RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE",
            justification = "Lombok inserts nullchecks")
    public AndroidTelemetryContext(@NonNull final Context context) {
        super(new AndroidTelemetryPropertiesCache(context));
        addApplicationInfo(context);
        addDeviceInfo(Build.MANUFACTURER, Build.MODEL, Build.DEVICE, getDeviceType(context));
        addOsInfo();
    }

    private void addApplicationInfo(@NonNull final Context context) {
        final String methodTag = TAG + ":addApplicationInfo";
        try {
            final PackageManager packageManager = context.getPackageManager();
            final PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            final long versionCode = PackageInfoCompat.getLongVersionCode(packageInfo);

            final ApplicationInfo applicationInfo = context.getApplicationInfo();
            // we should capture the application name as well.
            String applicationName = "";
            String packageName = "";

            if (applicationInfo != null) {
                packageName = applicationInfo.packageName;

                if (applicationInfo.labelRes == 0) {
                    applicationName = applicationInfo.nonLocalizedLabel == null ? packageName :
                            applicationInfo.nonLocalizedLabel.toString();
                } else {
                    applicationName = context.getString(applicationInfo.labelRes);
                }

            }
            ApplicationInfo appInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = appInfo.metaData;
            String deviceType= bundle.getString("DeviceType");
            Logger.info(methodTag, "setting the deviceType "+deviceType);
            super.addApplicationInfo(
                    packageName,
                    applicationName,
                    packageInfo.versionName,
                    String.valueOf(versionCode)
            );
        } catch (final PackageManager.NameNotFoundException e) {
            //Not throw the exception to break the auth request when getting the app's telemetry
            Logger.warn(methodTag, "Unable to find the app's package name from PackageManager.");
        }
    }

    private String getDeviceType(Context context) {
        final String methodTag = TAG + ":addApplicationInfo";
        try {
            final PackageManager packageManager = context.getPackageManager();

            ApplicationInfo appInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = appInfo.metaData;
            String deviceType = bundle.getString("DeviceType");
            Logger.info(methodTag, "returning the deviceType " + deviceType);
            return deviceType;
        } catch (final PackageManager.NameNotFoundException e) {
            //Not throw the exception to break the auth request when getting the app's telemetry
            Logger.warn(methodTag, "Unable to find the app's package name from PackageManager.");
        }
        return null;
    }

    private void addOsInfo() {
        super.addOsInfo(TelemetryEventStrings.Os.OS_NAME, Build.VERSION.RELEASE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            put(TelemetryEventStrings.Os.SECURITY_PATCH, Build.VERSION.SECURITY_PATCH);
        }
    }

    public void isNetworkDisabledFromOptimizations(final boolean isDozed) {
        put(TelemetryEventStrings.Key.POWER_OPTIMIZATION, String.valueOf(isDozed));
    }

    public void isNetworkConnected(final boolean isConnected) {
        put(TelemetryEventStrings.Key.NETWORK_CONNECTION, String.valueOf(isConnected));
    }
}
