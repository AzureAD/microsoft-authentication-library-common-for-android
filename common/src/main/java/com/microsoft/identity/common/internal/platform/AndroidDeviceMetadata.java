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
package com.microsoft.identity.common.internal.platform;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.platform.AbstractDeviceMetadata;

import lombok.NonNull;

/**
 * Provides device metadata in Android.
 **/
public class AndroidDeviceMetadata extends AbstractDeviceMetadata {

    private static final String ANDROID_DEVICE_TYPE = "Android";
    private static final String DEVICE_TYPE = "DeviceType";
    private static final String TAG = AndroidDeviceMetadata.class.getSimpleName();

    @Override
    @NonNull
    public String getDeviceType() {
        return ANDROID_DEVICE_TYPE;
    }

    @Override
    @NonNull
    public String getCpu() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            //CPU_ABI has been deprecated
            return Build.CPU_ABI;
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;
            if (supportedABIs != null && supportedABIs.length > 0) {
                return supportedABIs[0];
            }
        }
        return "UNKNOWN";
    }

    // Returns a SDK version (i.e. 24) instead of a user-friendly android version (i.e. 7.0)
    @Override
    public @NonNull String getOsForEsts() {
        return String.valueOf(Build.VERSION.SDK_INT);
    }

    @Override
    public @NonNull String getOsForMats() {
        return android.os.Build.VERSION.RELEASE;
    }

    @Override
    public @NonNull String getOsForDrs() {
        return android.os.Build.VERSION.RELEASE;
    }

    @Override
    @NonNull
    public String getDeviceModel() {
        return Build.MODEL;
    }

    @Override
    @NonNull
    public String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * Get the android device type, i.e; if it is an nGMS teams device or a mobile device
     *
     * @param context {@link Context}
     * @return device type
     */
    public static String getAndroidDeviceTypeFromMetadata(@NonNull final Context context) {
        final String methodTag = TAG + " :getDeviceType";
        String defaultDeviceType = "mobileDevice";
        try {
            final PackageManager packageManager = context.getPackageManager();
            final ApplicationInfo appInfo = packageManager.getApplicationInfo(context.getPackageName(), PackageManager.GET_META_DATA);
            final Bundle metaDataBundle = appInfo.metaData;
            final String deviceType = metaDataBundle.getString(DEVICE_TYPE, defaultDeviceType);
            Logger.verbose(methodTag, "Setting the deviceType as " + deviceType);
            return deviceType;
        } catch (final PackageManager.NameNotFoundException e) {
            // Do not throw the exception to break the auth request when getting the app's telemetry
            Logger.warn(methodTag, "Unable to find the app's package name from PackageManager.");
        }
        return defaultDeviceType;
    }
}

