package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.app.AzureSampleApp;
import com.microsoft.identity.client.ui.automation.broker.BrokerCompanyPortal;
import com.microsoft.identity.client.ui.automation.broker.BrokerMicrosoftAuthenticator;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class FirebaseRule implements TestRule {

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                new BrokerMicrosoftAuthenticator().copyApk();
                new BrokerCompanyPortal().copyApk();
                new AzureSampleApp().copyApk();
                base.evaluate();
            }
        };
    }

}
