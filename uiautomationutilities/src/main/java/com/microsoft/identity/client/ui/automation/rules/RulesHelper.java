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

import androidx.annotation.Nullable;

import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.powerlift.IPowerLiftIntegratedApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import org.junit.rules.RuleChain;
import org.junit.rules.Timeout;

import java.util.concurrent.TimeUnit;

/**
 * A helper class to instantiate and return a {@link RuleChain} comprised of the rules required for
 * a given scenario.
 */
public class RulesHelper {

    private final static String TAG = RulesHelper.class.getSimpleName();

    /**
     * Get a RuleChain object containing the necessary rules
     *
     * @param broker the broker that may be used during this test
     * @return a {@link RuleChain} object
     */
    public static RuleChain getPrimaryRules(@Nullable final ITestBroker broker) {
        return RulesHelper.getPrimaryRules(broker, new Timeout(10, TimeUnit.MINUTES));
    }

    /**
     * Get a RuleChain object containing the necessary rules.
     *
     * @param broker  the broker that may be used during this test
     * @param timeout the timeout of the test
     * @return a {@link RuleChain} object
     */
    public static RuleChain getPrimaryRules(@Nullable final ITestBroker broker, Timeout timeout) {
        Log.i(TAG, "Adding UncaughtExceptionHandlerRule");
        RuleChain ruleChain = RuleChain.outerRule(new UncaughtExceptionHandlerRule());

        Log.i(TAG, "Adding AutomationLoggingRule");
        ruleChain = ruleChain.around(new AutomationLoggingRule());

        Log.i(TAG, "Adding RetryTestRule");
        ruleChain = ruleChain.around(new RetryTestRule());

        Log.i(TAG, "Adding Timeout Rule");
        ruleChain = ruleChain.around(timeout);

        Log.i(TAG, "Adding UiAutomatorTestRule");
        ruleChain = ruleChain.around(new UiAutomatorTestRule());

        Log.i(TAG, "Adding ResetAutomaticTimeZoneTestRule");
        ruleChain = ruleChain.around(new ResetAutomaticTimeZoneTestRule());

        Log.i(TAG, "Adding DeviceLockSetRule");
        ruleChain = ruleChain.around(new DevicePinSetupRule(broker));

        if (com.microsoft.identity.client.ui.automation.BuildConfig.PREFER_PRE_INSTALLED_APKS) {
            Log.i(TAG, "Adding CopyPreInstalledApkRule");
            ruleChain = ruleChain.around(new CopyPreInstalledApkRule(
                    new BrokerMicrosoftAuthenticator(), new BrokerCompanyPortal(),
                    new BrokerHost(), new AzureSampleApp()
            ));
        }

        Log.i(TAG, "Adding RemoveBrokersBeforeTestRule");
        ruleChain = ruleChain.around(new RemoveBrokersBeforeTestRule());

        if (broker != null) {
            Log.i(TAG, "Adding BrokerSupportRule");
            ruleChain = ruleChain.around(new BrokerSupportRule(broker));

            Log.i(TAG, "Adding InstallBrokerTestRule");
            ruleChain = ruleChain.around(new InstallBrokerTestRule(broker));

            if (broker instanceof IPowerLiftIntegratedApp) {
                Log.i(TAG, "Adding PowerLiftIncidentRule");
                ruleChain = ruleChain.around(new PowerLiftIncidentRule((IPowerLiftIntegratedApp) broker));
            }

            Log.i(TAG, "Adding DeviceEnrollmentFailureRecoveryRule");
            ruleChain = ruleChain.around(new DeviceEnrollmentFailureRecoveryRule());
        }

        return ruleChain;
    }
}
