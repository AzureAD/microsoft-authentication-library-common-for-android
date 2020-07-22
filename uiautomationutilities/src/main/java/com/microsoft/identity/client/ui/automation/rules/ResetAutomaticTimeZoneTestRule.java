package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class ResetAutomaticTimeZoneTestRule implements TestRule {

    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                AdbShellUtils.enableAutomaticTimeZone();
                base.evaluate();
            }
        };
    }

}
