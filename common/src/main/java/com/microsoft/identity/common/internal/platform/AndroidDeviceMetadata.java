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

import android.os.Build;

import com.microsoft.identity.common.java.platform.AbstractDeviceMetadata;

import lombok.NonNull;

/**
 * Provides device metadata in Android.
 **/
public class AndroidDeviceMetadata extends AbstractDeviceMetadata {

    private static final String ANDROID_DEVICE_TYPE = "Android";

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
    @NonNull
    public String getOs() {
        return String.valueOf(Build.VERSION.SDK_INT);
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
}

