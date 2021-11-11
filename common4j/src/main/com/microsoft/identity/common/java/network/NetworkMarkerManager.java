//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.java.network;


import com.microsoft.identity.common.java.logging.Logger;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import lombok.NonNull;

public class NetworkMarkerManager {
    private static class Holder {
        static final NetworkMarkerManager INSTANCE = new NetworkMarkerManager();
    }

    public static NetworkMarkerManager getInstance() {
        return Holder.INSTANCE;
    }

    private static final String TAG = NetworkMarkerManager.class.getSimpleName();

    private final List<NetworkMarker> mNetworkMarkers = Collections.synchronizedList(new ArrayList<NetworkMarker>());
    private final Map<String, NetworkState[]> mNetworkMarkerOverride = Collections.synchronizedMap(new HashMap<String, NetworkState[]>());
    private final NetworkStatesHandler networkStatesHandler = new NetworkStatesHandler();

    private boolean mEnabled = false;
    private NetworkMarker mCurrentMarker = null;
    private INetworkStateChangeHandler mStateChangeHandler = null;
    private long mStartTime;

    public NetworkMarkerManager() {

    }


    private NetworkState[] parseNetworkStates(String networkStatesString) {
        final String[] statesString = networkStatesString.split("\\s*,\\s*");

        final NetworkState[] networkStates = new NetworkState[statesString.length];

        for (int i = 0; i < networkStates.length; i++) {
            networkStates[i] = NetworkState.newInstance(statesString[i]);
        }

        return networkStates;
    }


    private boolean checkEnabled(final String methodName) {
        if (mStateChangeHandler == null) {
            Logger.warn(TAG + methodName, "No network state change handler has been registered.");
            setEnabled(false);
        }

        if (!mEnabled) {
            Logger.warn(TAG + methodName, "The NetworkMarkerManager  is not enabled.");
        }

        return mEnabled;
    }


    public void stopMarker() {
        final String methodName = ":clear";

        if (checkEnabled(methodName)) {
            if (mCurrentMarker == null) {
                Logger.info(TAG + methodName, "Nothing to clear since there is no current marker");
                return;
            }
            networkStatesHandler.clear();
            mCurrentMarker = null;
        }
    }

    public void startMarker(@NonNull final String marker, @NonNull final String networkStatesString) {
        startMarker(marker, parseNetworkStates(networkStatesString));
    }


    public void startMarker(@NonNull final String marker, @NonNull NetworkState... networkStates) {
        final String methodName = ":setNetworkMarker";

        if (checkEnabled(methodName)) {
            if (mNetworkMarkers.isEmpty()) {
                mStartTime = System.currentTimeMillis();
            }

            Logger.info(TAG + methodName, "Marking for network state change with " + marker);

            // Check whether there is an override for this marker
            networkStates = mNetworkMarkerOverride.getOrDefault(marker, networkStates);

            if (mNetworkMarkerOverride.containsKey(marker)) {
                Logger.info(TAG + methodName, "Applying override on marker " + marker + " with " + Arrays.toString(networkStates));
            }

            final NetworkMarker networkMarker = new NetworkMarker(
                    marker,
                    Thread.currentThread().getId(),
                    networkStates
            );

            mNetworkMarkers.add(networkMarker);
            mCurrentMarker = networkMarker;
            networkStatesHandler.apply(networkMarker);
        }
    }

    public void applyNetworkStates(@NonNull final String marker, @NonNull final String networkStatesString) {
        applyNetworkStates(marker, parseNetworkStates(networkStatesString));
    }

    public void applyNetworkStates(@NonNull final String marker, @NonNull final NetworkState... networkStates) {
        mNetworkMarkerOverride.put(marker, networkStates);
    }

    public void removeNetworkStates(@NonNull final String marker) {
        mNetworkMarkerOverride.remove(marker);
    }

    public void removeAllNetworkStates() {
        mNetworkMarkerOverride.clear();
    }

    public void setStateChangeHandler(@NonNull INetworkStateChangeHandler stateChangeHandler) {
        mStateChangeHandler = stateChangeHandler;
        networkStatesHandler.setStateChangeHandler(mStateChangeHandler);
    }

    public void removeStateChangeHandler() {
        mStateChangeHandler = null;
        networkStatesHandler.setStateChangeHandler(null);
        setEnabled(false);

        Logger.warn(TAG + ":removeStateChangeHandler", "Network state change handler has been removed.");
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
        Logger.info(TAG + ":setEnabled", (enabled ? "Enabling" : "Disabling") + " the network marker manager.");
        if (!enabled) {
            networkStatesHandler.clear();
        }
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public long getStartTime() {
        return mStartTime;
    }

    public void clear() {
        setEnabled(false);
        mNetworkMarkers.clear();
        mNetworkMarkerOverride.clear();
        mStartTime = 0;
    }
}
