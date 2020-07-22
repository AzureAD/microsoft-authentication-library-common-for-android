package com.microsoft.identity.client.ui.automation.rules;

import androidx.annotation.Nullable;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class InstallBrokerTestRule implements TestRule {

    private final ITestBroker broker;

    public InstallBrokerTestRule(@Nullable ITestBroker broker) {
        this.broker = broker;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                if (broker != null) {
                    broker.install();
                }
                base.evaluate();
            }
        };
    }
}
