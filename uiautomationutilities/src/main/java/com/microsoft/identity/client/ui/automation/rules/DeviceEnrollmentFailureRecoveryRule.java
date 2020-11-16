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
package com.microsoft.identity.client.ui.automation.rules;

import android.util.Log;

import com.microsoft.identity.client.ui.automation.broker.DeviceLimitReachedException;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule that allows recovery from device enrollment failures by catching the {@link DeviceLimitReachedException}
 * and removing devices from Company Portal to allow successful enrollments in the future.
 */
public class DeviceEnrollmentFailureRecoveryRule implements TestRule {

    private final static String TAG = DeviceEnrollmentFailureRecoveryRule.class.getSimpleName();

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Log.i(TAG, "Applying rule....");
                try {
                    base.evaluate();
                } catch (final Throwable throwable) {
                    if (throwable instanceof DeviceLimitReachedException) {
                        Log.w(TAG, "Received DeviceLimitReachedException....removing devices..");
                        // Click REMOVE DEVICE btn in the dialog
                        UiAutomatorUtils.handleButtonClick("android:id/button1");

                        // we delete 10 devices as it is much more efficient
                        for (int i = 0; i < 10; i++) {
                            ((DeviceLimitReachedException) throwable).getCompanyPortal().removeDevice();
                        }
                    }

                    throw throwable; // the retry rule should handle retries
                }
            }
        };
    }

}
