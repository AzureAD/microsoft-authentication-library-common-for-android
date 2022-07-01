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
package com.microsoft.identity.common.java.telemetry;

import lombok.NonNull;

import java.util.TimeZone;

import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.App;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Device;
import static com.microsoft.identity.common.java.telemetry.TelemetryEventStrings.Os;

/**
 * TelemetryContext is a dictionary of information about the state of the device.
 * It is attached to every outgoing telemetry calls.
 */
public abstract class AbstractTelemetryContext extends Properties {
    private TelemetryPropertiesCache mTelemetryPropsCache;

    protected AbstractTelemetryContext(@NonNull final TelemetryPropertiesCache telemetryPropertiesCache) {
        super();
        mTelemetryPropsCache = telemetryPropertiesCache;
        put(Device.ID, mTelemetryPropsCache.getOrCreateRandomStableDeviceId());
        put(Device.TIMEZONE, TimeZone.getDefault().getID());
    }

    protected void addApplicationInfo(final String appPackage,
                                      final String appName,
                                      final String appVersion,
                                      final String appBuildCode){
        put(App.NAME, appName);
        put(App.PACKAGE, appPackage);
        put(App.VERSION, appVersion);
        put(App.BUILD, appBuildCode);
    }

    protected void addDeviceInfo(final String manufacturer,
                                 final String model,
                                 final String device) {
        put(Device.MANUFACTURER, manufacturer);
        put(Device.MODEL, model);
        put(Device.NAME, device);
    }

    protected void addOsInfo(final String osName,
                             final String osVersion) {
        put(Os.NAME, osName);
        put(Os.VERSION, osVersion);
    }
}
