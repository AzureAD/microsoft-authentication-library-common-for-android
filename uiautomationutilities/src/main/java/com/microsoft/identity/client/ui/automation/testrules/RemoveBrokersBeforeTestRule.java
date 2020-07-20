package com.microsoft.identity.client.ui.automation.testrules;

import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;
import com.microsoft.identity.client.ui.automation.utils.SettingsUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RemoveBrokersBeforeTestRule implements TestRule {

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                final BrokerMicrosoftAuthenticator authenticator = new BrokerMicrosoftAuthenticator();
                authenticator.uninstall();

                final BrokerCompanyPortal companyPortal = new BrokerCompanyPortal();
                companyPortal.uninstall();

                // CP may still be installed if device admin
                if (companyPortal.isInstalled()) {
                    SettingsUtils.disableAdmin("Company Portal");
                    companyPortal.uninstall();
                }

                base.evaluate();
            }
        };
    }
}
