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
import android.provider.Settings;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;
import com.microsoft.identity.common.internal.logging.Logger;

import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.TimeZone;
import java.util.concurrent.ConcurrentHashMap;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.App;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.Device;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.Key;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.Os;

/**
 * TelemetryContext is a dictionary of information about the state of the device.
 * It is attached to every outgoing telemetry calls.
 */
public class TelemetryContext extends Properties {
    private static final String TAG = TelemetryContext.class.getSimpleName();

    TelemetryContext(final ConcurrentHashMap<String, String> delegate) {
        super(delegate);
    }

    /**
     * Create a new {@link TelemetryContext} instance filled in with information from the given {@link
     * Context}. The {@link Telemetry} client can be called from anywhere, so the returned instances
     * is thread safe.
     */
    static synchronized TelemetryContext create(final Context context) {
        final TelemetryContext telemetryContext = new TelemetryContext(new ConcurrentHashMap<String, String>());
        telemetryContext.addApplicationInfo(context);
        telemetryContext.addDeviceInfo(context);
        telemetryContext.addOsInfo();
        telemetryContext.put(Device.TIMEZONE, TimeZone.getDefault().getID());
        return telemetryContext;
    }

    void addApplicationInfo(@NonNull final Context context) {
        try {
            final PackageManager packageManager = context.getPackageManager();
            final PackageInfo packageInfo = packageManager.getPackageInfo(context.getPackageName(), 0);
            put(App.NAME, packageInfo.applicationInfo.packageName);
            put(App.VERSION, packageInfo.versionName);
            put(App.BUILD, String.valueOf(packageInfo.versionCode));
        } catch (final PackageManager.NameNotFoundException e) {
            //Not throw the exception to break the auth request when getting the app's telemetry
            Logger.warn(TAG, "Unable to find the app's package name from PackageManager.");
        }
    }

    void addDeviceInfo(@NonNull final Context context) {
        put(Device.MANUFACTURER, Build.MANUFACTURER);
        put(Device.MODEL, Build.MODEL);
        put(Device.NAME, Build.DEVICE);
        try {
            put(
                    Device.ID,
                    StringExtensions.createHash(
                            Settings.Secure.getString(
                                    context.getContentResolver(),
                                    Settings.Secure.ANDROID_ID
                            )
                    )
            );
        } catch (final NoSuchAlgorithmException | UnsupportedEncodingException exception) {
            Logger.warn(TAG, "Unable to get the device id.");
        }
    }

    void addOsInfo() {
        put(Os.NAME, Os.OS_NAME);
        put(Os.VERSION, Build.VERSION.RELEASE);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            put(Os.SECURITY_PATCH, Build.VERSION.SECURITY_PATCH);
        }
    }

    public void isNetworkDisabledFromOptimizations(final boolean isDozed) {
        put(Key.POWER_OPTIMIZATION, String.valueOf(isDozed));
    }

    public void isNetworkConnected(final boolean isConnected) {
        put(Key.NETWORK_CONNECTION, String.valueOf(isConnected));
    }
}