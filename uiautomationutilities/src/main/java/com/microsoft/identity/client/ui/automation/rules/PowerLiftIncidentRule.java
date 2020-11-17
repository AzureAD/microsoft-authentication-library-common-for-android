package com.microsoft.identity.client.ui.automation.rules;

import android.util.Log;

import com.microsoft.identity.client.ui.automation.broker.ITestBroker;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to create a PowerLift Incident via a broker if a test fails.
 */
public class PowerLiftIncidentRule implements TestRule {

    private final static String TAG = PowerLiftIncidentRule.class.getSimpleName();

    private ITestBroker broker;

    public PowerLiftIncidentRule(final ITestBroker broker) {
        this.broker = broker;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Log.i(TAG, "Applying rule....");
                try {
                    base.evaluate();
                } catch (final Throwable throwable) {
                    Log.w(TAG, "Encountered error during execution....creating powerlift incident.");
                    broker.createPowerLiftIncident();
                    throw throwable;
                }
            }
        };
    }
}
