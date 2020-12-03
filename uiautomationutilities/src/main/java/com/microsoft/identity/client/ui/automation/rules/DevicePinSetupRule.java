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

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.provider.Settings;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;
import androidx.test.uiautomator.UiObject;
import androidx.test.uiautomator.UiObjectNotFoundException;

import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.Assert;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import static com.microsoft.identity.client.ui.automation.utils.CommonUtils.launchIntent;

/**
 * A test rule that allows you to setup PIN for the device if the pin is not setup and
 * if it is already setup then it will do nothing.
 */
public class DevicePinSetupRule implements TestRule {

    final String password = "1234";
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (!isDeviceSecured()) {
                    setLock();
                }
                base.evaluate();
            }
        };
    }

    private boolean isDeviceSecured() {
        Context context = ApplicationProvider.getApplicationContext();
        KeyguardManager keyguardManager =
                (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            return keyguardManager.isDeviceSecure();
        }
        return keyguardManager.isKeyguardSecure();
    }

    private void setLock() throws UiObjectNotFoundException {

        UiDevice device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
        launchIntent(Settings.ACTION_SECURITY_SETTINGS);

        final UiObject screenLock = UiAutomatorUtils.obtainUiObjectWithText("Screen lock");
        Assert.assertTrue(screenLock.exists());
        screenLock.click();
        final UiObject pinButton = UiAutomatorUtils.obtainUiObjectWithExactText("PIN");
        Assert.assertTrue(pinButton.exists());
        pinButton.click();
        final UiObject noButton = UiAutomatorUtils.obtainUiObjectWithExactText("NO");
        Assert.assertTrue(noButton.exists());
        noButton.click();

        UiObject passwordField = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.settings:id/password_entry");
        Assert.assertTrue(passwordField.exists());
        passwordField.setText(password);
        device.pressEnter();
        passwordField = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.settings:id/password_entry");
        passwordField.setText(password);
        device.pressEnter();

        final UiObject doneButton = UiAutomatorUtils.obtainUiObjectWithResourceId("com.android.settings:id/redaction_done_button");
        Assert.assertTrue(doneButton.exists());
        doneButton.click();
    }
}
