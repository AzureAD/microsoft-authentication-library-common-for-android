package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.device.settings.ISettings;
import com.microsoft.identity.client.ui.automation.logging.Logger;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * Test Rule to be used with CBA Test cases to allow removal of user certificates before and after test.
 */
public class UserCertificateRemovalRule implements TestRule {

    private final static String TAG = UserCertificateRemovalRule.class.getSimpleName();

    private final ISettings mSettings;

    public UserCertificateRemovalRule(final ISettings settings) {
        this.mSettings = settings;
    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule....");
                // We need to remove user credentials before the test case to have a clean state
                mSettings.clearUserCredentials();
                base.evaluate();
                // We need to remove user credentials after the test so that non-CBA test cases don't need to run this check
                mSettings.clearUserCredentials();
            }
        };
    }
}
