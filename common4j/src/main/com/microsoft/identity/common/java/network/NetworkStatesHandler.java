package com.microsoft.identity.common.java.network;

import com.microsoft.identity.common.java.logging.Logger;

import java.util.List;

import lombok.NonNull;

public class NetworkStatesHandler implements Runnable {
    private static final String TAG = NetworkStatesHandler.class.getSimpleName();

    private final Thread networkStateThread = new Thread(this);

    private INetworkStateChangeHandler mStateChangeHandler = null;
    private NetworkMarker currentMarker = null;
    private NetworkState currentState = null;

    public void clear() {
        clearCurrentMarker();
    }

    public void apply(@NonNull final NetworkMarker networkMarker) {
        clearCurrentMarker();
        currentMarker = networkMarker;
        networkStateThread.start();
    }

    @Override
    public void run() {
        final String methodName = ":run";
        Logger.info(TAG + methodName, "Running network states for marker: " + currentMarker.getMarker());
        final List<NetworkState> networkStateList = currentMarker.getNetworkStates();

        for (NetworkState networkState : networkStateList) {
            try {
                if (currentState != null && currentState.wasApplied()) {
                    Logger.info(TAG + methodName, "Completing network state: " + currentState);
                    currentState.setTimeCompleted(System.currentTimeMillis());
                }

                networkState.setTimeApplied(System.currentTimeMillis());
                currentState = networkState;

                if (networkState.getDelay() > 0) {
                    Thread.sleep(networkState.getDelay());
                }

                final boolean stateApplied = mStateChangeHandler.onNetworkStateApplied(networkState);
                networkState.setApplied(stateApplied);

                if (!stateApplied) {
                    Logger.warn(TAG + methodName, "Could not apply network state: " + networkState);
                    mStateChangeHandler.handleNetworkStateNotApplied(currentMarker, networkState);
                }

                if (networkState.getDuration() > 0 && stateApplied) {
                    Thread.sleep(networkState.getDuration());
                }
            } catch (InterruptedException e) {
                Logger.warn(TAG + methodName, "Network marker handler interrupted while running state: " + networkState);
                networkState.setApplied(false);
                mStateChangeHandler.handleNetworkStateNotApplied(currentMarker, networkState);
            }
        }
    }

    public void setStateChangeHandler(INetworkStateChangeHandler stateChangeHandler) {
        if (stateChangeHandler == null && networkStateThread.isAlive()) {
            clearCurrentMarker();
        }

        this.mStateChangeHandler = stateChangeHandler;
    }

    private void clearCurrentMarker() {
        mStateChangeHandler.onRestoreNetworkState(currentState);
        if (currentState != null) {
            if (networkStateThread.isAlive()) {
                // there's a current marker applying network states, interrupt it.
                networkStateThread.interrupt();
            } else {
                currentState.setTimeCompleted(System.currentTimeMillis());
            }
        }
        currentState = null;
        currentMarker = null;
    }
}
