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
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;

import com.microsoft.identity.common.BuildConfig;
import com.microsoft.identity.common.internal.platform.Device;

import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

/**
 * TelemetryContext is a dictionary of information about the state of the device.
 * It is attached to every outgoing telemetry calls.
 *
 */
public class TelemetryContext extends ValueMap {
    // App
    private static final String APP_NAME_KEY = "app_name";
    private static final String APP_VERSION_KEY = "app_version";
    private static final String APP_PACKAGE_NAME_KEY = "app_package_name";
    private static final String APP_BUILD_KEY = "app_build";
    // Device
    private static final String DEVICE_MANUFACTURER_KEY = "device_manufacturer";
    private static final String DEVICE_MODEL_KEY = "device_model";
    private static final String DEVICE_NAME_KEY = "device_name";
    // Library
    private static final String LIBRARY_NAME_KEY = "library_name";
    private static final String LIBRARY_VERSION_KEY = "library_version";
    // Network
    private static final String NETWORK_CONNECTION_KEY = "network_connection";
    private static final String NETWORK_POWER_OPTIMIZATION_KEY = "network_carrier";
    // OS
    private static final String OS_NAME_KEY = "os_name";
    private static final String OS_VERSION_KEY = "os_version";
    private static final String TIMEZONE_KEY = "timezone";

    TelemetryContext(Map<String, Object> delegate) {
        super(delegate);
    }

    /**
     * Create a new {@link TelemetryContext} instance filled in with information from the given {@link
     * Context}. The {@link Telemetry} client can be called from anywhere, so the returned instances
     * is thread safe.
     */
    static synchronized TelemetryContext create(final Context context) {
        TelemetryContext telemetryContext = new TelemetryContext(new ConcurrentHashMap<String, Object>());
        telemetryContext.putApp(context);
        telemetryContext.putDevice();
        telemetryContext.putLibrary();
        telemetryContext.putOs();
        telemetryContext.put(TIMEZONE_KEY, TimeZone.getDefault().getID());
        return telemetryContext;
    }

    void putApp(Context context) {
        try {
            PackageManager packageManager = context.getPackageManager();
            PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            Map<String, Object> app = new ConcurrentHashMap<>();
            put(APP_NAME_KEY, packageInfo.applicationInfo.loadLabel(packageManager));
            put(APP_VERSION_KEY, packageInfo.versionName);
            put(APP_PACKAGE_NAME_KEY, packageInfo.packageName);
            put(APP_BUILD_KEY, String.valueOf(packageInfo.versionCode));
        } catch (PackageManager.NameNotFoundException e) {
            // ignore
        }
    }

    void putDevice() {
        put(DEVICE_MANUFACTURER_KEY, Build.MANUFACTURER);
        put(DEVICE_MODEL_KEY, Build.MODEL);
        put(DEVICE_NAME_KEY, Build.DEVICE);
    }

    void putLibrary() {
        put(LIBRARY_NAME_KEY, "common-android");
        put(LIBRARY_VERSION_KEY, BuildConfig.VERSION_NAME);
    }

    void putOs() {
        put(OS_NAME_KEY, "Android");
        put(OS_VERSION_KEY, Build.VERSION.RELEASE);
    }
}
