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
package com.microsoft.identity.client.ui.automation;

import android.content.Context;
import android.os.Build;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.device.TestDevice;

import lombok.Getter;
import lombok.NonNull;

/**
 * A class describing the context under which the test is being executed. This includes things like
 * details about the device on which the test is being run.
 */
@Getter
public class TestContext {

    private final TestDevice testDevice;
    private final Context applicationContext;

    private static TestContext sTestContext;

    private TestContext(@NonNull final TestDevice testDevice, @NonNull final Context context) {
        this.testDevice = testDevice;
        this.applicationContext = context;
    }

    public static TestContext getTestContext() {
        if (sTestContext == null) {
            sTestContext = createTestContext();
        }

        return sTestContext;
    }

    private static TestContext createTestContext() {
        final TestDevice testDevice = createDeviceUnderTest();
        final Context context = ApplicationProvider.getApplicationContext();
        return new TestContext(testDevice, context);
    }

    private static TestDevice createDeviceUnderTest() {
        final String manufacturer = Build.MANUFACTURER;
        final String model = Build.MODEL;
        final int apiLevel = Build.VERSION.SDK_INT;

        return new TestDevice(manufacturer, model, apiLevel);
    }
}
