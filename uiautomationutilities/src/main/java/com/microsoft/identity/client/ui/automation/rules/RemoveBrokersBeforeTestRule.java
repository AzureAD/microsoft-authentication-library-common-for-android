package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.TestContext;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to remove all brokers from the device prior to executing the test case
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
                    TestContext.getTestContext().getDevice().getSettings().disableAdmin("Authenticator");
                    authenticator.uninstall();
                }

                final BrokerCompanyPortal companyPortal = new BrokerCompanyPortal();
                companyPortal.uninstall();

                // CP may still be installed if device admin
                if (companyPortal.isInstalled()) {
                    TestContext.getTestContext().getDevice().getSettings().disableAdmin("Company Portal");
                    companyPortal.uninstall();
                }

                base.evaluate();
            }
        };
    }
}
