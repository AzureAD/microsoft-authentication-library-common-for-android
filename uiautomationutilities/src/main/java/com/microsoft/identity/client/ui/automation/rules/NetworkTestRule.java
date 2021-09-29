package com.microsoft.identity.client.ui.automation.rules;

import android.content.Context;
import android.os.Build;
import android.util.Log;

import androidx.annotation.RequiresApi;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.annotations.NetworkStatesFile;
import com.microsoft.identity.client.ui.automation.annotations.NetworkTestTimeout;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.network.NetworkTestStateManager;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkTestRule<T> implements TestRule {

    private long testStartTime, testEndTime;
    private static final String TAG = NetworkTestRule.class.getSimpleName();
    private ResultFuture<T, Exception> testResult = new ResultFuture<>();
    private final Context mContext = ApplicationProvider.getApplicationContext();

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule...");
                NetworkStatesFile statesFile = description.getAnnotation(NetworkStatesFile.class);
                NetworkTestTimeout testTimeout = description.getAnnotation(NetworkTestTimeout.class);

                if (statesFile == null) {
                    Logger.i(TAG, "Method[" + description.getMethodName() + "] does not have any network states file annotation...");

                    statesFile = description.getClass().getAnnotation(NetworkStatesFile.class);
                }

                if (statesFile != null) {
                    final List<NetworkTestStateManager> stateManagers = NetworkTestStateManager
                            .readCSVFile(description.getTestClass(), statesFile.value());

                    for (NetworkTestStateManager stateManager : stateManagers) {
                        executeTest(stateManager, base, testTimeout == null ? 0 : testTimeout.seconds());
                    }
                }
            }
        };
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void executeTest(final NetworkTestStateManager stateManager, final Statement base, final int timeoutSeconds) throws InterruptedException {
        Log.d(TAG, "Running network test: " + stateManager.getId());

        NetworkTestStateManager.resetNetworkState(mContext);

        final Thread networkStateThread = stateManager.execute();
        final ExecutorService executorService = Executors.newFixedThreadPool(2);

        executorService.execute(networkStateThread);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(2000);
                    testStartTime = System.currentTimeMillis();
                    base.evaluate();
                } catch (Throwable throwable) {
                    testResult.setException(new Exception(throwable));
                } finally {
                    testEndTime = System.currentTimeMillis();
                }
            }
        });
        executorService.shutdown();

        try {
            T result;
            if (timeoutSeconds == 0) result = testResult.get();
            else result = testResult.get(timeoutSeconds, TimeUnit.SECONDS);

            Log.d(TAG, "Test [" + stateManager.getId() + "] result: " + result);
        } catch (Throwable throwable) {
            Log.e(TAG, "Test [" + stateManager.getId() + "] threw an exception", throwable);
        }

        Log.d(TAG, "Network test: " + stateManager.getId() + " completed after: " + (testEndTime - testStartTime) + " ms");

        executorService.shutdownNow();
        testResult = new ResultFuture<>();
    }

    public void setResult(T result) {
        this.testResult.setResult(result);
    }

    public void setException(Exception exception) {
        this.testResult.setException(exception);
    }
}
