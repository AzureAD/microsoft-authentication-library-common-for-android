package com.microsoft.identity.client.ui.automation.rules;

import com.microsoft.identity.client.ui.automation.app.OutlookApp;
import com.microsoft.identity.client.ui.automation.app.TeamsApp;
import com.microsoft.identity.client.ui.automation.app.WordApp;
import com.microsoft.identity.client.ui.automation.browser.BrowserEdge;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

/**
 * A Test Rule to remove all first party apps from the device prior to executing the test case
 */
public class RemoveFirstPartyAppsTestRule implements TestRule {

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                new OutlookApp().uninstall();
                new TeamsApp().uninstall();
                new WordApp().uninstall();
                new BrowserEdge().uninstall();

                base.evaluate();
            }
        };
    }
}
