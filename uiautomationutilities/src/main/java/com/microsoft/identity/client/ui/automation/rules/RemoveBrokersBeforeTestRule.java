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
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.constants.DeviceAdmin;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to remove all brokers from the device prior to executing the test case.
 */
public class RemoveBrokersBeforeTestRule implements TestRule {

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final BrokerMicrosoftAuthenticator authenticator = new BrokerMicrosoftAuthenticator();
                authenticator.uninstall();

                // Auth App may still be installed if device admin (Samsung devices for instance)
                if (authenticator.isInstalled()) {
                    TestContext.getTestContext().getTestDevice().getSettings().disableAdmin(DeviceAdmin.MICROSOFT_AUTHENTICATOR);
                    authenticator.uninstall();
                }

                final BrokerCompanyPortal companyPortal = new BrokerCompanyPortal();
                companyPortal.uninstall();

                // CP may still be installed if device admin
                if (companyPortal.isInstalled()) {
                    TestContext.getTestContext().getTestDevice().getSettings().disableAdmin(DeviceAdmin.COMPANY_PORTAL);
                    companyPortal.uninstall();
                }

                base.evaluate();
            }
        };
    }
}
