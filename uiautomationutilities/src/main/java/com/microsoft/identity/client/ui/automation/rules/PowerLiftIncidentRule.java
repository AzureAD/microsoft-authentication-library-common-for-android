package com.microsoft.identity.client.ui.automation.rules;

import android.text.TextUtils;

import com.microsoft.identity.client.ui.automation.app.App;
import com.microsoft.identity.client.ui.automation.powerlift.IPowerLiftIntegratedApp;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.powerlift.ThrowableWithPowerLiftIncident;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to create a PowerLift Incident via a broker if a test fails.
 */
public class PowerLiftIncidentRule implements TestRule {

    private final static String TAG = PowerLiftIncidentRule.class.getSimpleName();

    private IPowerLiftIntegratedApp powerLiftIntegratedApp;

    public PowerLiftIncidentRule(final IPowerLiftIntegratedApp powerLiftIntegratedApp) {
        this.powerLiftIntegratedApp = powerLiftIntegratedApp;
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
//                try {
                    base.evaluate();
//                } catch (final Throwable originalThrowable) {
//                    String powerLiftIncidentDetails = null;
//                    try {
//                        Logger.e(
//                                TAG,
//                                "Encountered error during test....creating PowerLift incident.",
//                                originalThrowable
//                        );
//                        powerLiftIncidentDetails = powerLiftIntegratedApp.createPowerLiftIncident();
//                    } catch (final Throwable powerLiftError) {
//                        Logger.e(
//                                TAG,
//                                "Oops...something went wrong...unable to create PowerLift incident.",
//                                powerLiftError
//                        );
//                    }
//                    if (TextUtils.isEmpty(powerLiftIncidentDetails)) {
//                        throw originalThrowable;
//                    } else {
//                        assert powerLiftIncidentDetails != null;
//                        throw new ThrowableWithPowerLiftIncident(
//                                powerLiftIntegratedApp,
//                                powerLiftIncidentDetails,
//                                originalThrowable
//                        );
//                    }
//                }
            }
        };
    }
}
