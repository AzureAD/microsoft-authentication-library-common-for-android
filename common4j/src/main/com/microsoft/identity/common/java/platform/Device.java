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
package com.microsoft.identity.common.java.platform;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.util.StringUtil;

import net.jcip.annotations.GuardedBy;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import lombok.NonNull;

/**
 * Helper class to add additional platform specific query parameters or headers for the request sent to eSTS.
 */
public class Device {
    private static final String TAG = Device.class.getSimpleName();

    /**
     * The String to be returned if the value is not set.
     */
    protected static final String NOT_SET = "NOT_SET";

    private static IDeviceMetadata sDeviceMetadata;

    private static final ReentrantReadWriteLock sLock = new ReentrantReadWriteLock();

    @GuardedBy("sLock")
    public static void setDeviceMetadata(@NonNull final IDeviceMetadata deviceMetadata) {
        sLock.writeLock().lock();
        try {
            sDeviceMetadata = deviceMetadata;
        } finally {
            sLock.writeLock().unlock();
        }
    }

    // Visible for testing only.
    @GuardedBy("sLock")
    public static void clearDeviceMetadata(){
        sLock.writeLock().lock();
        try {
            sDeviceMetadata = null;
        } finally {
            sLock.writeLock().unlock();
        }
    }

    @NonNull
    @GuardedBy("sLock")
    public static Map<String, String> getPlatformIdParameters() {
        sLock.readLock().lock();
        try {
            final Map<String, String> platformParameters = new HashMap<>();

            if (sDeviceMetadata != null) {
                platformParameters.put(PlatformIdParameters.CPU_PLATFORM, sDeviceMetadata.getCpu());
                platformParameters.put(PlatformIdParameters.OS, sDeviceMetadata.getOsForEsts());
                platformParameters.put(PlatformIdParameters.DEVICE_MODEL, sDeviceMetadata.getDeviceModel());
            } else {
                platformParameters.put(PlatformIdParameters.CPU_PLATFORM, NOT_SET);
                platformParameters.put(PlatformIdParameters.OS, NOT_SET);
                platformParameters.put(PlatformIdParameters.DEVICE_MODEL, NOT_SET);
            }

            return Collections.unmodifiableMap(platformParameters);
        } finally {
            sLock.readLock().unlock();
        }
    }

    @NonNull
    public static String getProductVersion() {
        final String methodName = ":getProductVersion";

        final String version = DiagnosticContext.INSTANCE.getRequestContext().get(AuthenticationConstants.SdkPlatformFields.VERSION);
        if (StringUtil.isNullOrEmpty(version)) {
            Logger.warn(TAG + methodName, "Product version is not set.", null);
            return "1.5.9-default";
        } else {
            return version;
        }
    }

    @NonNull
    @GuardedBy("sLock")
    public static String getDeviceType() {
        sLock.readLock().lock();
        try {
            if (sDeviceMetadata != null) {
                return sDeviceMetadata.getDeviceType();
            }
            return NOT_SET;
        } finally {
            sLock.readLock().unlock();
        }
    }

    /**
     * Gets the CPU of the current device.
     *
     * @return The name of the device CPU.
     */
    @NonNull
    @GuardedBy("sLock")
    public static String getCpu() {
        sLock.readLock().lock();
        try {
            if (sDeviceMetadata != null) {
                return sDeviceMetadata.getCpu();
            }
            return NOT_SET;
        } finally {
            sLock.readLock().unlock();
        }
    }

    /**
     * Get the OS of this device to be sent to eSTS.
     *
     * We have this because in Android, we're sending [SDK VERSION] (i.e. 24) to eSTS,
     * not the [OS VERSION] (i.e. 7.0), and eSTS are using these [SDK VERSION] to gate features.
     *
     * @return The name/version of the device OS.
     */
    @NonNull
    @GuardedBy("sLock")
    public static String getOsForEsts() {
        sLock.readLock().lock();
        try {
            if (sDeviceMetadata != null) {
                return sDeviceMetadata.getOsForEsts();
            }
            return NOT_SET;
        } finally {
            sLock.readLock().unlock();
        }
    }

    /**
     * Get the OS of this device to be sent to DRS.
     *
     * @return a String representing the OS information
     */
    @NonNull
    @GuardedBy("sLock")
    public static String getOsForDrs() {
        sLock.readLock().lock();
        try {
            if (sDeviceMetadata != null) {
                return sDeviceMetadata.getOsForDrs();
            }
            return NOT_SET;
        } finally {
            sLock.readLock().unlock();
        }
    }

    /**
     * Gets the manufacturer of the current device.
     *
     * @return The name of the device manufacturer.
     */
    @NonNull
    @GuardedBy("sLock")
    public static String getManufacturer() {
        sLock.readLock().lock();
        try {
            if (sDeviceMetadata != null) {
                return sDeviceMetadata.getManufacturer();
            }
            return NOT_SET;
        } finally {
            sLock.readLock().unlock();
        }
    }

    /**
     * Gets the model name of the current device.
     *
     * @return The device model name.
     */
    @NonNull
    @GuardedBy("sLock")
    public static String getModel() {
        sLock.readLock().lock();
        try {
            if (sDeviceMetadata != null) {
                return sDeviceMetadata.getDeviceModel();
            }
            return NOT_SET;
        } finally {
            sLock.readLock().unlock();
        }
    }

    /**
     * Get the device display name.
     *
     * @return a String representing this device's display name.
     */
    @NonNull
    public static String getDeviceDisplayName() {
        return getManufacturer() + getModel();
    }

    public static final class PlatformIdParameters {
        /**
         * The String representing the CPU for the device.
         */
        public static final String CPU_PLATFORM = "x-client-CPU";

        /**
         * The String representing the device OS.
         */
        public static final String OS = "x-client-OS";

        /**
         * The String representing the device model.
         */
        public static final String DEVICE_MODEL = "x-client-DM";

        /**
         * String for the broker version.
         */
        public static final String BROKER_VERSION = "x-client-brkrver";
    }

}
