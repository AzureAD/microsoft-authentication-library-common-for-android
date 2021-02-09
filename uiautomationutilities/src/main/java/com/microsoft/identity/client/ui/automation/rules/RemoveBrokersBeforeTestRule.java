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
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;

/**
 * A Test Rule to remove all brokers from the device prior to executing the test case.
 */
public class RemoveBrokersBeforeTestRule implements TestRule {

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final List<ITestBroker> testBrokers = CommonUtils.getAllPossibleTestBrokers();

                for (final ITestBroker broker : testBrokers) {
                    broker.uninstall();

                    if (broker.isInstalled()) {
                        // The broker app will still be installed on the device if it is enabled
                        // as a device admin. In this case, we need to disable the admin and then
                        // uninstall.
                        TestContext.getTestContext().getTestDevice()
                                .getSettings().disableAdmin(broker.getAdminName());
                        broker.uninstall();
                    }
                }

                base.evaluate();
            }
        };
    }
}
