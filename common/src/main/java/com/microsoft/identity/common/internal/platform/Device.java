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
