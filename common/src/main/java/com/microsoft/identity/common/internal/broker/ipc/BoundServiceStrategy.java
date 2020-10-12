package com.microsoft.identity.common.internal.broker.ipc;

import android.os.Bundle;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.BoundServiceClient;
import com.microsoft.identity.common.internal.logging.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

/**
 * A strategy for communicating with the active broker (:auth process) via Bound Service.
 */
public class BoundServiceStrategy<T extends IInterface> implements IIpcStrategy {
    private static final String TAG = BoundServiceStrategy.class.getSimpleName();

    private final BoundServiceClient<T> mClient;

    public BoundServiceStrategy(@NonNull final BoundServiceClient<T> boundServiceClient) {
        mClient = boundServiceClient;
    }

    @Nullable
    @Override
    public Bundle communicateToBroker(@NonNull BrokerOperationBundle brokerOperationBundle)
            throws BaseException {
        final String methodName = brokerOperationBundle.getOperation().name();

        try {
            return mClient.performOperation(brokerOperationBundle);
        } catch (final RemoteException | InterruptedException | ExecutionException | TimeoutException | RuntimeException e) {
            // We know for a fact that in some OEM, bind service might throw a runtime exception.
            final String errorDescription = e.getClass().getSimpleName() + " occurred while awaiting (get) return of bound Service";
            Logger.error(TAG + methodName, errorDescription, e);
            throw new BrokerCommunicationException(errorDescription, e);
        } finally {
            mClient.disconnect();
        }
    }
}
