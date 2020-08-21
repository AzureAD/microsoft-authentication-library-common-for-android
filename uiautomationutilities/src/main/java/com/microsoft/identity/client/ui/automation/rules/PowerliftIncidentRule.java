package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to create a powerlift incident via a broker if a test fails.
 */
public class PowerliftIncidentRule implements TestRule {

    private ITestBroker broker;

    public PowerliftIncidentRule(final ITestBroker broker) {
        this.broker = broker;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } catch (final Throwable throwable) {
                    broker.createPowerliftIncident();
                    throw throwable;
                }
            }
        };
    }
}
