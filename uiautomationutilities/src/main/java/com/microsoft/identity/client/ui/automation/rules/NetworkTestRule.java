package com.microsoft.identity.client.ui.automation.rules;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.microsoft.identity.client.ui.automation.network.NetworkStateChangeHandler;
import com.microsoft.identity.common.java.network.NetworkMarkerManager;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

@RequiresApi(api = Build.VERSION_CODES.N)
public class NetworkTestRule implements TestRule {
    private static final NetworkStateChangeHandler networkStateChangeHandler = new NetworkStateChangeHandler();

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void evaluate() throws Throwable {
                NetworkMarkerManager.setEnabled(true);
                NetworkMarkerManager.setStateChangeHandler(networkStateChangeHandler);
                try {
                    base.evaluate();
                } finally {
                    NetworkMarkerManager.setEnabled(false);
                    NetworkMarkerManager.clear();
                }
            }
        };
    }
}
