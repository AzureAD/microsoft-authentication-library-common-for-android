package com.microsoft.identity.client.ui.automation.network.statements;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;

import com.microsoft.identity.client.ui.automation.annotations.NetworkTestTimeout;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.network.NetworkTestStateManager;
import com.microsoft.identity.client.ui.automation.rules.NetworkTestRule;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkTestExecutor<T> extends Statement {

    private static final String TAG = NetworkTestExecutor.class.getSimpleName();

    private final List<FrameworkMethod> befores;
    private final Object target;
    private final Statement statement;
    private final Context mContext;
    private final NetworkTestRule<T> networkTestRule;
    private final FrameworkMethod method;
    private long testStartTime, testEndTime;

    public NetworkTestExecutor(FrameworkMethod method, Context mContext, List<FrameworkMethod> befores, Object target, Statement statement, NetworkTestRule<T> networkTestRule) {
        this.method = method;
        this.mContext = mContext;
        this.befores = befores;
        this.target = target;
        this.statement = statement;
        this.networkTestRule = networkTestRule;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void evaluate() throws Throwable {
        final NetworkTestStateManager stateManager = networkTestRule.getCurrentStateManager();
        Log.d(TAG, "Running test [" + stateManager.getId() + "]. Waiting for network....");
        NetworkTestStateManager.resetNetworkState(mContext); // Reset the network state before running tests

        Log.d(TAG, "Network available. ");

        final Thread networkStateThread = stateManager.execute();
        final ExecutorService executorService = Executors.newFixedThreadPool(2);
        NetworkTestTimeout testTimeout = method.getAnnotation(NetworkTestTimeout.class);

        for (FrameworkMethod before : befores) {
            before.invokeExplosively(target);
        }

        executorService.execute(networkStateThread);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    testStartTime = System.currentTimeMillis();
                    statement.evaluate();
                } catch (Throwable throwable) {
                    networkTestRule.setException(new Exception(throwable));
                }
            }
        });
        executorService.shutdown();

        long timeoutSeconds = testTimeout.seconds();
        Log.d(TAG, "Test timeout: " + timeoutSeconds);
        try {
            T result;
            if (timeoutSeconds == 0) result = networkTestRule.getResult();
            else result = networkTestRule.getResult(timeoutSeconds, TimeUnit.SECONDS);

            testEndTime = System.currentTimeMillis();

            Log.d(TAG, "Test [" + stateManager.getId() + "] result: " + result);
        } catch (Throwable throwable) {
            testEndTime = System.currentTimeMillis();
            Log.e(TAG, "Test [" + stateManager.getId() + "] threw an exception", throwable);

            Logger.e(TAG, String.format("Test [%s] threw a %s exception", stateManager.getId(), throwable.getClass().getSimpleName()), throwable);
        }

        Log.d(TAG, "Network test: " + stateManager.getId() + " completed after: " + (testEndTime - testStartTime) + " ms");

        executorService.shutdownNow();
    }
}
