package com.microsoft.identity.client.ui.automation.testrules;

import com.microsoft.identity.client.ui.automation.broker.DeviceLimitReachedException;
import com.microsoft.identity.client.ui.automation.utils.UiAutomatorUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

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
