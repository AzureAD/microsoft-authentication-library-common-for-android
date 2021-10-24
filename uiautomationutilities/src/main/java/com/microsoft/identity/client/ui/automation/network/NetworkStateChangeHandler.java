package com.microsoft.identity.client.ui.automation.network;

import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;
import com.microsoft.identity.common.java.network.INetworkStateChangeHandler;
import com.microsoft.identity.common.java.network.NetworkInterface;
import com.microsoft.identity.common.java.network.NetworkMarker;

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
    public boolean onNetworkStateApplied(@NonNull NetworkMarker networkMarker) {
        changeNetworkState(networkMarker.getNetworkState().getNetworkInterface());
        return true;
    }

    @Override
    public boolean onNetworkStateCleared(@NonNull NetworkMarker networkMarker) {
        return true;
    }
}
