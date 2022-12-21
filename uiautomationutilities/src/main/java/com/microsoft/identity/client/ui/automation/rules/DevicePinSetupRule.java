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

import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.device.settings.BaseSettings;
import com.microsoft.identity.client.ui.automation.device.settings.ISettings;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.device.TestDevice;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A test rule that allows you to either set a PIN on the device or remove an existing PIN from the
 * device based on the nature of the test case i.e. based on the broker being used in the test.
 * <p>
 * If the test is using Authenticator app then we would just remove the PIN from the device because
 * if the device has a PIN then Authenticator turns on App Lock and some dialogs pop up when you
 * launch Authenticator in such case and it is very complex to handle that UI during automation and
 * we are better off not handling that UI in the first place by ensuring that the device doesn't
 * have a lock.
 * <p>
 * If the test is using Company Portal then we would set a PIN on the device because some test cases
 * exercise conditional policies to require a PIN to be set on the device. Instead of setting it
 * within the test case when such a need arises, we would rather just set it here in the rule prior
 * to starting the test because it just keeps test code much simpler and cleaner.
 * <p>
 * If the test is using Broker Host then we really don't care as the test code is not affected by
 * whether there is a PIN on the device or not.
 */
public class DevicePinSetupRule implements TestRule {

    private final static String TAG = DevicePinSetupRule.class.getSimpleName();
    public static final String PIN = "1234";

    private final ITestBroker mBroker;

    DevicePinSetupRule(final ITestBroker broker) {
        this.mBroker = broker;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule..");
                final TestDevice device = TestContext.getTestContext().getTestDevice();
                if ((mBroker instanceof BrokerCompanyPortal || mBroker instanceof BrokerMicrosoftAuthenticator) && !device.isSecured()) {
                    device.setPin(PIN);
                } // for BrokerHost it doesn't really matter (at least not yet)

                base.evaluate();
            }
        };
    }

}
