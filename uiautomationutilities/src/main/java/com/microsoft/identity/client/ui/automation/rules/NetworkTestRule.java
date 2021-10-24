package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.network.NetworkStateChangeHandler;
import com.microsoft.identity.common.java.network.NetworkMarkerManager;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class NetworkTestRule implements TestRule {
    private static final NetworkMarkerManager networkMarkerManager = NetworkMarkerManager.getInstance();

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                networkMarkerManager.setEnabled(true);
                networkMarkerManager.setStateChangeHandler(new NetworkStateChangeHandler());
                try {
                    base.evaluate();
                } finally {
                    networkMarkerManager.setEnabled(false);
                }
            }
        };
    }
}
