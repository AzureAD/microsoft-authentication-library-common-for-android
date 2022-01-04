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
package com.microsoft.identity.client.ui.automation.device;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.device.settings.GoogleSettings;
import com.microsoft.identity.client.ui.automation.device.settings.ISettings;
import com.microsoft.identity.client.ui.automation.device.settings.SamsungSettings;
import com.microsoft.identity.client.ui.automation.logging.Logger;

import lombok.Getter;

/**
 * This class represents a device under test during UI Automation.
 */
@Getter
public class TestDevice {

    private static final String TAG = TestDevice.class.getSimpleName();
    private final String manufacturer;
    private final String model;
    private final int apiLevel;
    private final ISettings settings;

    public TestDevice(
            @NonNull final String manufacturer, @NonNull final String model, final int apiLevel) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.apiLevel = apiLevel;
        this.settings = getSupportedDeviceSettings(manufacturer, model);
    }

    public TestDevice(
            @NonNull final String manufacturer,
            @NonNull final String model,
            final int apiLevel,
            @NonNull final ISettings settings) {
        this.manufacturer = manufacturer;
        this.model = model;
        this.apiLevel = apiLevel;
        this.settings = settings;
    }

    private static ISettings getSupportedDeviceSettings(
            @NonNull final String manufacturer, @NonNull final String model) {
        Logger.i(TAG, "Get Supported Device Settings..");
        // each device could have its own version of settings depending on make, model & apiLevel
        // For simplicity right now, we just have two configurations depending on manufacturer
        if ("SAMSUNG".equalsIgnoreCase(manufacturer)) {
            return new SamsungSettings();
        }

        return new GoogleSettings();
    }

    public boolean isSecured() {
        final Context context = ApplicationProvider.getApplicationContext();
        final KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure();
        }
        return keyguardManager.isKeyguardSecure();
    }

    public void setPin(final String pin) {
        if (isSecured()) {
            Logger.i(
                    TAG,
                    "The device already has a PIN setup so we are not going to put "
                            + "a new one in there.");
            return;
        }
        getSettings().setPinOnDevice(pin);
    }

    public void removePin(final String pin) {
        if (!isSecured()) {
            Logger.w(
                    TAG,
                    "Unable to remove pin from device as the device doesn't actually "
                            + "have a screen lock.");
            return;
        }

        getSettings().removePinFromDevice(pin);
    }
}
