package com.microsoft.identity.client.ui.automation.network.statements;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import com.microsoft.identity.client.ui.automation.annotations.NetworkTest;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.network.NetworkTestConstants;
import com.microsoft.identity.client.ui.automation.network.NetworkTestStateManager;
import com.microsoft.identity.client.ui.automation.reporting.Timeline;
import com.microsoft.identity.client.ui.automation.rules.NetworkTestRule;

import org.junit.runners.model.FrameworkMethod;
import org.junit.runners.model.Statement;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NetworkTestExecutor extends Statement {

    private static final String TAG = NetworkTestExecutor.class.getSimpleName();

    private final List<FrameworkMethod> befores;
    private final Object target;
    private final Statement statement;
    private final Context mContext;
    private final NetworkTestRule networkTestRule;
    private final FrameworkMethod method;
    private final ConnectivityManager connectivityManager;

    private ConnectivityManager.NetworkCallback networkCallback;

    public NetworkTestExecutor(
            FrameworkMethod method,
            Context mContext,
            List<FrameworkMethod> befores,
            Object target,
            Statement statement,
            NetworkTestRule networkTestRule
    ) {
        this.method = method;
        this.mContext = mContext;
        this.befores = befores;
        this.target = target;
        this.statement = statement;
        this.networkTestRule = networkTestRule;
        this.connectivityManager = (ConnectivityManager) mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    private void registerNetworkStateListener() {
        connectivityManager.registerDefaultNetworkCallback(networkCallback = new ConnectivityManager.NetworkCallback() {
            @Override
            public void onAvailable(@NonNull Network network) {
                final NetworkInfo networkInfo = connectivityManager.getNetworkInfo(network);
                Timeline.start(
                        NetworkTestConstants.TimelineEntities.NETWORK_AVAILABILITY_STATE,
                        "Network connection available.",
                        "Device connected to " + networkInfo.getTypeName()
                );
            }

            @Override
            public void onLost(@NonNull Network network) {
                Timeline.start(
                        NetworkTestConstants.TimelineEntities.NETWORK_AVAILABILITY_STATE,
                        "Network connection lost"
                );
            }
        });
    }

    private void unregisterNetworkStateListener() {
        Timeline.finish(NetworkTestConstants.TimelineEntities.NETWORK_AVAILABILITY_STATE);
        connectivityManager.unregisterNetworkCallback(networkCallback);
        networkCallback = null;
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @Override
    public void evaluate() throws Throwable {
        this.registerNetworkStateListener();

        Timeline.start(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, "Waiting for network availability");
        final NetworkTestStateManager stateManager = networkTestRule.getCurrentStateManager();
        NetworkTestStateManager.resetNetworkState(mContext); // Reset the network state before running tests

        Timeline.finish(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, "Network available");

        final Thread networkStateThread = stateManager.execute();
        final ExecutorService executorService = Executors.newFixedThreadPool(3);
        NetworkTest testAnnotation = method.getAnnotation(NetworkTest.class);

        Timeline.start(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, "Setting up test");
        for (FrameworkMethod before : befores) {
            before.invokeExplosively(target);
        }
        Timeline.finish(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, "Done setting up test");


        Timeline.start(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, "Running test", method.getName());
        executorService.execute(networkStateThread);
        executorService.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    statement.evaluate();
                } catch (Throwable throwable) {
                    networkTestRule.setException(new Exception(throwable));
                }
            }
        });
        executorService.shutdown();

        long timeoutSeconds = testAnnotation.testTimeout();
        try {
            String result;
            if (timeoutSeconds == 0) result = networkTestRule.getResult();
            else result = networkTestRule.getResult(timeoutSeconds, TimeUnit.SECONDS);


            Timeline.finish(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, String.valueOf(result));
        } catch (Throwable throwable) {
            Timeline.finish(NetworkTestConstants.TimelineEntities.TEST_EXECUTION_STAGE, throwable);
        }


        executorService.shutdownNow();
        this.unregisterNetworkStateListener();
    }
}
