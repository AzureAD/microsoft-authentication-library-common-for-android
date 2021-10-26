package com.microsoft.identity.client.ui.automation.network;

import androidx.annotation.Nullable;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.common.java.network.INetworkStateChangeHandler;
import com.microsoft.identity.common.java.network.NetworkInterface;
import com.microsoft.identity.common.java.network.NetworkMarker;
import com.microsoft.identity.common.java.network.NetworkState;

import lombok.NonNull;

public class NetworkStateChangeHandler implements INetworkStateChangeHandler {
    private static final String TAG = NetworkStateChangeHandler.class.getSimpleName();

    private void changeNetworkState(NetworkInterface interfaceType) {
        final String methodName = ":changeNetworkState";
        if (interfaceType != null) {
            Logger.i(TAG + methodName, "Changing network state to: " + interfaceType.getKey());

            AdbShellUtils.executeShellCommand("svc wifi " + (interfaceType.wifiActive() ? "enable" : "disable"));
            AdbShellUtils.executeShellCommand("svc data " + (interfaceType.cellularActive() ? "enable" : "disable"));
        }
    }

    @Override
    public boolean onNetworkStateApplied(@NonNull NetworkState networkState) {
        changeNetworkState(networkState.getNetworkInterface());
        return false;
    }

    @Override
    public boolean onRestoreNetworkState(@Nullable NetworkState networkState) {
        changeNetworkState(NetworkInterface.WIFI_AND_CELLULAR);
        return false;
    }

    @Override
    public void handleNetworkStateNotApplied(@NonNull NetworkMarker marker, @NonNull NetworkState networkState) {

    }
}
