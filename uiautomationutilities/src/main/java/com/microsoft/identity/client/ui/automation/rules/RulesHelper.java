package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerHost;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import org.junit.rules.RuleChain;

public class RulesHelper {

    private final static String TAG = RulesHelper.class.getSimpleName();

    public static RuleChain getPrimaryRules(final ITestBroker broker) {
        RuleChain ruleChain = RuleChain.outerRule(new RetryTestRule());

        System.out.println(TAG + ": Adding UiAutomatorTestRule");
        ruleChain = ruleChain.around(new UiAutomatorTestRule());

        System.out.println(TAG + ": Adding ResetAutomaticTimeZoneTestRule");
        ruleChain = ruleChain.around(new ResetAutomaticTimeZoneTestRule());

        if (com.microsoft.identity.client.ui.automation.BuildConfig.PREFER_PRE_INSTALLED_APKS) {
            System.out.println(TAG + ": Adding CopyPreInstalledApkRule");
            ruleChain = ruleChain.around(new CopyPreInstalledApkRule(
                    new BrokerMicrosoftAuthenticator(), new BrokerCompanyPortal(),
                    new BrokerHost(), new AzureSampleApp()
            ));
        }

        System.out.println(TAG + ": Adding RemoveBrokersBeforeTestRule");
        ruleChain = ruleChain.around(new RemoveBrokersBeforeTestRule());

        if (broker != null) {
            System.out.println(TAG + ": Adding BrokerSupportRule");
            ruleChain = ruleChain.around(new BrokerSupportRule(broker));

            System.out.println(TAG + ": Adding InstallBrokerTestRule");
            ruleChain = ruleChain.around(new InstallBrokerTestRule(broker));

            System.out.println(TAG + ": Adding PowerLiftIncidentRule");
            ruleChain =  ruleChain.around(new PowerLiftIncidentRule(broker));

            System.out.println(TAG + ": Adding DeviceEnrollmentFailureRecoveryRule");
            ruleChain = ruleChain.around(new DeviceEnrollmentFailureRecoveryRule());
        }

        return ruleChain;
    }
}
