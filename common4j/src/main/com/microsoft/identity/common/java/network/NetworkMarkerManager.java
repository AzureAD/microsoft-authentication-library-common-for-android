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
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

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
    private final Map<String, NetworkState> mNetworkMarkerOverride = Collections.synchronizedMap(new HashMap<String, NetworkState>());

    private boolean mEnabled = false;
    private NetworkMarker mCurrentMarker = null;
    private INetworkStateChangeHandler mStateChangeHandler = null;

    public NetworkMarkerManager() {

    }

    private boolean checkEnabled(final String methodName) {
        if (mStateChangeHandler == null) {
            Logger.warn(TAG + methodName, "No network state change handler has been registered.");
            mEnabled = false;
        }

        if (!mEnabled) {
            Logger.warn(TAG + methodName, "The NetworkMarkerManager  is not enabled.");
        }

        return mEnabled;
    }

    public void clearCurrentMarker() {
        final String methodName = ":clear";

        if (checkEnabled(methodName)) {
            if (mCurrentMarker == null) {
                Logger.info(TAG + methodName, "Nothing to clear since there is no current marker");
                return;
            }
            mStateChangeHandler.onNetworkStateCleared(mCurrentMarker);
            mCurrentMarker.setTimeCompleted(System.currentTimeMillis());
            mCurrentMarker = null;
        }
    }

    public void applyMarker(@NonNull final String marker, @Nullable final NetworkInterface networkInterface, final long delay, final long duration) {
        final String methodName = ":setNetworkMarker";

        if (checkEnabled(methodName)) {
            Logger.info(TAG + methodName, "Marking for network state change with " + marker);

            // Check whether there is an override for this marker
            final NetworkState networkState = mNetworkMarkerOverride.getOrDefault(marker,
                    new NetworkState.NetworkStateBuilder()
                            .delay(delay)
                            .duration(duration)
                            .networkInterface(networkInterface)
                            .build()
            );

            final NetworkMarker networkMarker = new NetworkMarker(
                    marker,
                    networkState.getNetworkInterface(),
                    Thread.currentThread().getId(),
                    networkState.getDuration()
            );

            mNetworkMarkers.add(networkMarker);

            if (!mStateChangeHandler.onNetworkStateApplied(networkMarker)) {
                Logger.warn(TAG + methodName, "The current marker " + marker + " was not applied. ");
                return;
            }
            mCurrentMarker = networkMarker;
        }
    }

    public void applyMarkerWithDuration(@NonNull final String marker, @NonNull final NetworkInterface networkInterface, final long duration) {
        applyMarker(marker, networkInterface, 0, duration);
    }

    public void applyMarkerWithDelay(@NonNull final String marker, @NonNull final NetworkInterface networkInterface, final long delay) {
        applyMarker(marker, networkInterface, delay, 0);
    }

    public void applyMarker(@NonNull final String marker, @NonNull final NetworkInterface networkInterface) {
        applyMarker(marker, networkInterface, 0, 0);
    }

    public void applyMarkerWithDelay(@NonNull final String marker, final long delay) {
        applyMarkerWithDelay(marker, null, delay);
    }

    public void applyMarkerWithDuration(@NonNull final String marker, final long duration) {
        applyMarkerWithDuration(marker, null, duration);
    }

    public void applyMarker(@NonNull final String marker) {
        applyMarker(marker, null, 0, 0);
    }

    public void setMarkerOverride(@NonNull final String marker, @NonNull final NetworkState networkState) {
        mNetworkMarkerOverride.put(marker, networkState);
    }

    public void removeMarkerOverride(@NonNull final String marker) {
        mNetworkMarkerOverride.remove(marker);
    }

    public void clearOverrides() {
        mNetworkMarkerOverride.clear();
    }

    public void setStateChangeHandler(INetworkStateChangeHandler stateChangeHandler) {
        mStateChangeHandler = stateChangeHandler;
    }

    public void setEnabled(boolean enabled) {
        mEnabled = enabled;
    }

    public boolean isEnabled() {
        return mEnabled;
    }

    public long getStartTime() {
        if (mNetworkMarkers.isEmpty()) {
            return 0;
        }
        return mNetworkMarkers.get(0).getTimeApplied();
    }
}
