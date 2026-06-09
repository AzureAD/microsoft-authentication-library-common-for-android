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
package com.microsoft.identity.common.adal.internal;

import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.common.internal.BatteryOptimizationStatus;
import com.microsoft.identity.common.internal.DeviceDozeModeStatus;
import com.microsoft.identity.common.logging.Logger;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wrapper class for PowerManager.
 */

public class PowerManagerWrapper {

    private static final String TAG = PowerManagerWrapper.class.getSimpleName();

    private static PowerManagerWrapper sInstance;

    // In-memory cache for battery optimization status for each apps.
    private final Map<String, BatteryOptimizationStatus> batteryOptOutCache = new ConcurrentHashMap<>();
    /**
     * Set instance of PowerManagerWrapper.
     *
     * @param wrapper PowerManagerWrapper
     */
    public static void setInstance(final PowerManagerWrapper wrapper) {
        sInstance = wrapper;
    }

    /**
     * Singleton implementation for PowerManagerWrapper.
     *
     * @return PowerManagerWrapper singleton instance.
     */
    public static synchronized PowerManagerWrapper getInstance() {
        if (sInstance == null) {
            sInstance = new PowerManagerWrapper();
        }
        return sInstance;
    }

    /**
     * Wrap the calling to method isDeviceIdleMode() of final class PowerManager.
     *
     * @param connectionContext Context used to query if app is in idle mode.
     * @return true if the device is in doze/idle mode.
     */
    @RequiresApi(Build.VERSION_CODES.M)
    public boolean isDeviceIdleMode(final Context connectionContext) {
        return ((PowerManager) connectionContext.getSystemService(Context.POWER_SERVICE)).isDeviceIdleMode();
    }

    /**
     * Gets the Device Doze Mode Status.
     * 
     * This is exposed to OneAuth.
     *
     * @param context The context to use for PowerManager.
     * @return a {@link DeviceDozeModeStatus}
     */
    @NonNull
    public DeviceDozeModeStatus getDeviceDozeModeStatus(@NonNull final Context context){
        final String methodTag = TAG + ":getDeviceDozeModeStatus";

        try {
            final PowerManager powerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
            if (powerManager == null) {
                Logger.error(methodTag, "PowerManager is null", null);
                return DeviceDozeModeStatus.CannotRetrievePowerManager;
            }
            if (powerManager.isDeviceIdleMode()) {
                return DeviceDozeModeStatus.Idle;
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    powerManager.isDeviceLightIdleMode()) {
                return DeviceDozeModeStatus.LightIdle;
            }

            return DeviceDozeModeStatus.NotInDozeMode;
        } catch (Exception e){
            Logger.error(methodTag, "Unknown Exception when checking doze mode status", e);
            return DeviceDozeModeStatus.UnknownError;
        }
    }

    /**
     * Wrap the calling to method isIgnoringBatteryOptimizations() of final class PowerManager.
     *
     * @param connectionContext Context used to query if app is ignoring battery optimizations.
     * @return true if the given application package name is on the device's power allow list.
     */
    public boolean isIgnoringBatteryOptimizations(final Context connectionContext) {
        return ((PowerManager) connectionContext.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(connectionContext.getPackageName());
    }

    /**
     * Checks if the app with the given package name is opted out from battery optimization.
     * Caches the result in memory using computeIfAbsent for thread safety.
     * Returns a string indicating the result or exception type.
     *
     * This is exposed to OneAuth.
     *
     * @param packageName The package name to check.
     * @param context The context to use for PowerManager.
     * @return a {@link BatteryOptimizationStatus}
     */
    public BatteryOptimizationStatus isAppOptedOutFromBatteryOptimization(@NonNull final String packageName, @NonNull final Context context) {
        final String methodTag = TAG + ":isAppOptedOutFromBatteryOptimization";

        return batteryOptOutCache.computeIfAbsent(packageName, key -> {
            try {
                final PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
                if (powerManager == null) {
                    Logger.error(methodTag, "PowerManager is null", null);
                    return BatteryOptimizationStatus.CannotRetrievePowerManager;
                }

                if (powerManager.isIgnoringBatteryOptimizations(key)) {
                    return BatteryOptimizationStatus.OptOut;
                } else {
                    return BatteryOptimizationStatus.NotOptOut;
                }
            } catch (Exception e) {
                Logger.error(methodTag, "Unknown Exception when checking battery optimization status for package: " + packageName, e);
                return BatteryOptimizationStatus.UnknownError;
            }
        });
    }
}
