package com.microsoft.identity.common.java.network;

import javax.annotation.Nullable;

import lombok.NonNull;

public interface INetworkStateChangeHandler {
    boolean onNetworkStateApplied(@NonNull final NetworkState networkState);

    boolean onRestoreNetworkState(@Nullable final NetworkState networkState);

    void handleNetworkStateNotApplied(@NonNull final NetworkMarker marker, @NonNull final NetworkState networkState);
}
