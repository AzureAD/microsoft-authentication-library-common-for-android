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

import android.annotation.TargetApi;
import android.content.Context;
import android.os.Build;
import android.os.PowerManager;

import androidx.annotation.NonNull;

/**
 * Wrapper class for PowerManager.
 */

public class PowerManagerWrapper {

    private static PowerManagerWrapper sInstance;

    private static final String UNKNOWN_STATUS = "Unknown";
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
    @TargetApi(Build.VERSION_CODES.M)
    public boolean isDeviceIdleMode(final Context connectionContext) {
        return ((PowerManager) connectionContext.getSystemService(Context.POWER_SERVICE)).isDeviceIdleMode();
    }

    /**
     * Gets a string representing Device Idle status.
     * Will return an empty string if the device is not in any idle mode.
     * (Possible Values: "Idle", "LightIdle", "Unknown" , "")
     */
    @NonNull
    public String getDeviceIdleMode(@NonNull final Context context){
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return UNKNOWN_STATUS;
            }

            final PowerManager powerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
            if (powerManager.isDeviceIdleMode()) {
                return "Idle";
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                    powerManager.isDeviceLightIdleMode()) {
                return "LightIdle";
            }
        } catch (final Exception e){
            // Swallow all exception!
            return UNKNOWN_STATUS;
        }

        return "";
    }

    /**
     * Gets a string representing Power Optimization settings of the calling app
     * Will return an empty string if the app isn't opting out.
     * (Possible Values: "OptOut", "Unknown" , "")
     */
    @NonNull
    public String getPowerOptimizationSettings(@NonNull final Context context){
        try {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                return UNKNOWN_STATUS;
            }

            final PowerManager powerManager = ((PowerManager) context.getSystemService(Context.POWER_SERVICE));
            if (powerManager.isIgnoringBatteryOptimizations(context.getPackageName())){
                return "OptOut";
            } else {
                return "";
            }

        } catch (final Exception e){
            // Swallow all exception!
            return UNKNOWN_STATUS;
        }
    }

    /**
     * Wrap the calling to method isIgnoringBatteryOptimizations() of final class PowerManager.
     *
     * @param connectionContext Context used to query if app is ignoring battery optimizations.
     * @return true if the given application package name is on the device's power whitelist.
     */
    @TargetApi(Build.VERSION_CODES.M)
    public boolean isIgnoringBatteryOptimizations(final Context connectionContext) {
        return ((PowerManager) connectionContext.getSystemService(Context.POWER_SERVICE)).isIgnoringBatteryOptimizations(connectionContext.getPackageName());
    }
}
