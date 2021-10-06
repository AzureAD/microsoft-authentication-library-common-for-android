package com.microsoft.identity.client.ui.automation.rules;

import android.os.Build;

import androidx.annotation.RequiresApi;

import com.microsoft.identity.client.ui.automation.annotations.NetworkStatesFile;
import com.microsoft.identity.client.ui.automation.annotations.NetworkTestTimeout;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.network.NetworkTestStateManager;
import com.microsoft.identity.client.ui.automation.sdk.ResultFuture;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;

import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class NetworkTestRule<T> implements TestRule {

    private static final String TAG = NetworkTestRule.class.getSimpleName();
    private static final long FIND_UI_ELEMENT_TIMEOUT = CommonUtils.FIND_UI_ELEMENT_TIMEOUT;

    private ResultFuture<T, Exception> testResult = new ResultFuture<>();
    private NetworkTestStateManager currentStateManager = null;

    @Override
    public Statement apply(Statement base, Description description) {
        return new Statement() {
            @RequiresApi(api = Build.VERSION_CODES.N)
            @Override
            public void evaluate() throws Throwable {
                Logger.i(TAG, "Applying rule...");
                NetworkStatesFile statesFile = description.getAnnotation(NetworkStatesFile.class);
                NetworkTestTimeout testTimeout = description.getAnnotation(NetworkTestTimeout.class);

                // Update the timeout for waiting of a UI element
                CommonUtils.FIND_UI_ELEMENT_TIMEOUT =
                        testTimeout == null ? FIND_UI_ELEMENT_TIMEOUT : TimeUnit.SECONDS.toMillis(testTimeout.seconds());

                if (statesFile == null) {
                    Logger.i(TAG, "Method[" + description.getMethodName() + "] does not have any network states file annotation...");

                    statesFile = description.getClass().getAnnotation(NetworkStatesFile.class);
                }

                if (statesFile != null) {
                    final List<NetworkTestStateManager> stateManagers = NetworkTestStateManager
                            .readCSVFile(description.getTestClass(), statesFile.value());

                    for (NetworkTestStateManager stateManager : stateManagers) {
                        currentStateManager = stateManager;
                        if (stateManager.isIgnored()) {
                            Logger.i(TAG, "Skipping network test [" + stateManager.getId() + "] since it was marked as ignored.");
                        } else {
                            base.evaluate();

                            cleanUp();
                        }
                    }
                } else {
                    Logger.e(TAG, "No tests to be run. Network states input file is not defined.");
                }
            }
        };
    }

    private void cleanUp() {
        testResult = new ResultFuture<>();
    }

    public void setResult(T result) {
        testResult.setResult(result);
    }

    public void setException(Exception exception) {
        testResult.setException(exception);
    }

    public NetworkTestStateManager getCurrentStateManager() {
        return currentStateManager;
    }

    public T getResult(long timeoutSeconds, TimeUnit timeUnit) throws Throwable {
        return testResult.get(timeoutSeconds, timeUnit);
    }

    public T getResult() throws Exception {
        return testResult.get();
    }
}
