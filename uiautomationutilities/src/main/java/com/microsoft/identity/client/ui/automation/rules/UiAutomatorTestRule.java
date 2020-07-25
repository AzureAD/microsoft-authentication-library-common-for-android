package com.microsoft.identity.client.ui.automation.rules;

import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.uiautomator.UiDevice;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to warm up (initialize) UI Automator before executing the test
 */
public class UiAutomatorTestRule implements TestRule {

    private UiDevice device;

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation());
                base.evaluate();
            }
        };
    }

    public UiDevice getUiDevice() {
        return device;
    }
}
