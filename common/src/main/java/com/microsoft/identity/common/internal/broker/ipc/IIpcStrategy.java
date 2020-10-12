package com.microsoft.identity.common.internal.broker.ipc;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BaseException;

/**
 * an interface for inter-process communication strategies.
 */
public interface IIpcStrategy {
    /**
     * Communicates with the target broker.
     *
     * @param bundle a {@link BrokerOperationBundle} object.
     * @return a response bundle (returned from the active broker).
     */
    @Nullable Bundle communicateToBroker(@NonNull final BrokerOperationBundle bundle) throws BaseException;
}
