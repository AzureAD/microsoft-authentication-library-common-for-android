package com.microsoft.identity.common.java.network;

import lombok.NonNull;

public interface INetworkStateChangeHandler {
    boolean onNetworkStateApplied(@NonNull final NetworkMarker networkMarker);

    boolean onNetworkStateCleared(@NonNull final NetworkMarker networkMarker);
}
