package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.broker.DeviceLimitReachedException;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule that allows recovery from device enrollment failures by catching the {@link DeviceLimitReachedException}
 * and removing devices from Company Portal to allow successful enrollments in the future
 */
public class DeviceEnrollmentFailureRecoveryRule implements TestRule {

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                try {
                    base.evaluate();
                } catch (Throwable throwable) {
                    if (throwable instanceof DeviceLimitReachedException) {
                        // Click REMOVE DEVICE btn in the dialog
                        UiAutomatorUtils.handleButtonClick("android:id/button1");

                        for (int i = 0; i < 7; i++) {
                            ((DeviceLimitReachedException) throwable).getCompanyPortal().removeDevice();
                        }
                    }

                    throw throwable; // the retry rule should handle retries
                }
            }
        };
    }

}
