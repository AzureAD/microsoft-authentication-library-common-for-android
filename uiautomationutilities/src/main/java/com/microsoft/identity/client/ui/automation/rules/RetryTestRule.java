package com.microsoft.identity.client.ui.automation.rules;

import android.util.Log;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

public class RetryTestRule implements TestRule {
    @Override
    public Statement apply(final Statement base, final Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                Throwable caughtThrowable = null;

                for (int i = 0; i < 2; i++) {
                    try {
                        base.evaluate();
                        return;
                    } catch (Throwable throwable) {
                        caughtThrowable = throwable;
                        Log.v("TESTLOG", description.getDisplayName() + ": run " + (i + 1) +
                                " failed with " + throwable.getMessage());
                    }
                }

                Log.v("TESTLOG", "Test " + description.getMethodName() +
                        " - Giving up after " + 2 + " attempts");
                caughtThrowable.printStackTrace();
                throw caughtThrowable;
            }
        };
    }
}
