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
package com.microsoft.identity.common.internal.platform;

import android.os.Build;

import com.microsoft.identity.common.exception.ClientException;

import java.io.IOException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Helper class to add additional platform specific query parameters or headers for the request sent to sts.
 */
public final class Device {

    private static IDevicePopManager sDevicePoPManager;

    /**
     * Private constructor to prevent a help class from being initiated.
     */
    private Device() {
    }

    public static Map<String, String> getPlatformIdParameters() {
        final Map<String, String> platformParameters = new HashMap<>();

        platformParameters.put(PlatformIdParameters.PRODUCT, PlatformIdParameters.PRODUCT_NAME);
        platformParameters.put(PlatformIdParameters.VERSION, PlatformIdParameters.PRODUCT_VERSION);

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            platformParameters.put(PlatformIdParameters.CPU_PLATFORM, Build.CPU_ABI);
        } else {
            final String[] supportedABIs = Build.SUPPORTED_ABIS;

            if (supportedABIs != null && supportedABIs.length > 0) {
                platformParameters.put(PlatformIdParameters.CPU_PLATFORM, supportedABIs[0]);
            }
        }

        platformParameters.put(PlatformIdParameters.OS, String.valueOf(Build.VERSION.SDK_INT));
        platformParameters.put(PlatformIdParameters.DEVICE_MODEL, Build.MODEL);

        return Collections.unmodifiableMap(platformParameters);
    }

    public static final class PlatformIdParameters {
        /**
         * The String representing the sdk platform.
         */
        public static final String PRODUCT = "x-client-SKU";

        /**
         * The String representing the sdk platform name.
         */
        public static final String PRODUCT_NAME = "MSAL.Android";

        /**
         * The String representing the sdk platform version.
         */
        public static final String PRODUCT_VERSION = "1.4.0";

        /**
         * The String representing the sdk version.
         */
        public static final String VERSION = "x-client-Ver";

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

    /**
     * Gets the API level of the current runtime.
     *
     * @return The API level.
     */
    public static int getApiLevel() {
        return Build.VERSION.SDK_INT;
    }

    /**
     * Gets the manufacturer of the current device.
     *
     * @return The name of the device manufacturer.
     */
    public static String getManufacturer() {
        return Build.MANUFACTURER;
    }

    /**
     * Gets the model name/code of the current device.
     *
     * @return The device model name.
     */
    public static String getModel() {
        return Build.MODEL;
    }

    public static synchronized IDevicePopManager getDevicePoPManagerInstance() throws ClientException {
        try {
            if (null == sDevicePoPManager) {
                sDevicePoPManager = new DevicePopManager();
            }

            return sDevicePoPManager;
        } catch (CertificateException
                | NoSuchAlgorithmException
                | KeyStoreException
                | IOException e) {
            throw new ClientException(
                    ClientException.KEYSTORE_NOT_INITIALIZED,
                    "Failed to initialize DevicePoPManager = " + e.getMessage(),
                    e
            );
        }
    }

}
