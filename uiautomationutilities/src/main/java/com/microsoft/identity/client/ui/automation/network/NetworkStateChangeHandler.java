package com.microsoft.identity.client.ui.automation.network;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.Network;
import android.os.Build;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.common.java.network.INetworkStateChangeHandler;
import com.microsoft.identity.common.java.network.NetworkInterface;
import com.microsoft.identity.common.java.network.NetworkMarker;
import com.microsoft.identity.common.java.network.NetworkState;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;

public class NetworkStateChangeHandler implements INetworkStateChangeHandler {
    private static final String TAG = NetworkStateChangeHandler.class.getSimpleName();
    private static final long NETWORK_AVAILABILITY_WAIT_SECONDS = 20;

    private static final Context applicationContext = ApplicationProvider.getApplicationContext();
    private final ConnectivityManager connectivityManager = (ConnectivityManager) applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE);
    private static boolean mNetworkAvailable = false;

    private static final ConnectivityManager.NetworkCallback networkCallback = new ConnectivityManager.NetworkCallback() {
        @Override
        public void onAvailable(@androidx.annotation.NonNull Network network) {
            mNetworkAvailable = true;
        }

        @Override
        public void onLost(@androidx.annotation.NonNull Network network) {
            mNetworkAvailable = false;
        }
    };

    @RequiresApi(api = Build.VERSION_CODES.N)
    public NetworkStateChangeHandler() {
        mNetworkAvailable = connectivityManager.getActiveNetworkInfo().isConnectedOrConnecting();
        connectivityManager.registerDefaultNetworkCallback(networkCallback);
    }

    private boolean waitForNetworkAvailability(boolean networkAvailable) {
        final String methodName = ":waitForNetworkAvailability";

        Logger.i(TAG + methodName, "Waiting for network availability: " + networkAvailable);
        if (networkAvailable != mNetworkAvailable) {
            final CountDownLatch latch = new CountDownLatch(1);
            try {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            while (networkAvailable != mNetworkAvailable) {
                                Thread.sleep(1000);
                            }
                            latch.countDown();
                        } catch (InterruptedException exception) {
                            latch.countDown();
                        }
                    }
                }).start();

                latch.await(NETWORK_AVAILABILITY_WAIT_SECONDS, TimeUnit.SECONDS);
            } catch (InterruptedException exception) {
                Logger.e(TAG + methodName, "Error while waiting for network availability", exception);
            }
        }

        Logger.i(TAG + methodName, "Network availability confirmed: " + (networkAvailable == mNetworkAvailable));
        return networkAvailable == mNetworkAvailable;
    }

    private boolean changeNetworkState(NetworkInterface interfaceType) {
        final String methodName = ":changeNetworkState";
        if (interfaceType == null) return false;

        Logger.i(TAG + methodName, "Changing network state to: " + interfaceType.getKey());

        AdbShellUtils.executeShellCommand("svc wifi " + (interfaceType.wifiActive() ? "enable" : "disable"));
        AdbShellUtils.executeShellCommand("svc data " + (interfaceType.cellularActive() ? "enable" : "disable"));

        return waitForNetworkAvailability((interfaceType.cellularActive() || interfaceType.wifiActive()));

    }

    @Override
    public boolean onNetworkStateApplied(@NonNull NetworkMarker marker, @NonNull NetworkState networkState) {
        final NetworkInterface networkInterface = networkState.getNetworkInterface();

        return changeNetworkState(networkInterface);
    }

    @Override
    public boolean onRestoreNetworkState(@Nullable NetworkMarker marker, @Nullable NetworkState networkState) {
        return changeNetworkState(NetworkInterface.WIFI_AND_CELLULAR);
    }

    @Override
    public void handleNetworkStateNotApplied(@NonNull NetworkMarker marker, @NonNull NetworkState networkState) {

    }


    public void unregisterNetworkAvailabilityListener() {
        connectivityManager.unregisterNetworkCallback(networkCallback);
    }

}
