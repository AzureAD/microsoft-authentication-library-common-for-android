package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.TestContext;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import lombok.NonNull;

public class DefaultBrowserRule implements TestRule {

    private final String browserName;

    public DefaultBrowserRule(@NonNull final String browserName){
        this.browserName = browserName;

    }

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                TestContext.getTestContext().getTestDevice().getSettings().setDefaultBrowser(browserName);
                base.evaluate();
            }
        };
    }
}
