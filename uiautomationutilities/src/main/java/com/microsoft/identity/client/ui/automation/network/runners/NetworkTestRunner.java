package com.microsoft.identity.client.ui.automation.network.runners;

import android.content.Context;

import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.network.statements.NetworkTestExecutor;
import com.microsoft.identity.client.ui.automation.rules.NetworkTestRule;

import org.junit.Before;
import org.junit.rules.TestRule;
import org.junit.runners.BlockJUnit4ClassRunner;
import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.InitializationError;
import org.junit.runners.model.Statement;

import java.util.List;

public class NetworkTestRunner extends BlockJUnit4ClassRunner {

    private final Context mContext = ApplicationProvider.getApplicationContext();

    public NetworkTestRunner(Class<?> testClass) throws InitializationError {
        super(testClass);
    }

    @Override
    protected Statement withBefores(FrameworkMethod method, Object target, Statement statement) {
        final List<FrameworkMethod> befores = getTestClass().getAnnotatedMethods(Before.class);
        final NetworkTestRule<?> networkTestRule = getNetworkTestRule(target);

        return befores.isEmpty() || networkTestRule == null ?
                statement : new NetworkTestExecutor<>(method, mContext, befores, target, statement, networkTestRule);
    }

    private NetworkTestRule<?> getNetworkTestRule(final Object target) {
        final List<TestRule> rules = getTestRules(target);

        NetworkTestRule<?> networkTestRule = null;
        for (TestRule testRule : rules) {
            if (testRule instanceof NetworkTestRule) {
                networkTestRule = (NetworkTestRule<?>) testRule;
            }
        }
        return networkTestRule;
    }
}
