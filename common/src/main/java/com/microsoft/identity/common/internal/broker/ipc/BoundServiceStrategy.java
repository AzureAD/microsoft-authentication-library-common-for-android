package com.microsoft.identity.common.internal.broker.ipc;

import android.os.Bundle;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.BoundServiceClient;
import com.microsoft.identity.common.logging.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.CONNECTION_ERROR;

/**
 * A strategy for communicating with the targeted broker via Bound Service.
 */
public class BoundServiceStrategy<T extends IInterface> implements IIpcStrategy {
    private static final String TAG = BoundServiceStrategy.class.getSimpleName();

    private final BoundServiceClient<T> mClient;

    public BoundServiceStrategy(final @NonNull BoundServiceClient<T> boundServiceClient) {
        mClient = boundServiceClient;
    }

    @Override
    public @Nullable
    Bundle communicateToBroker(final @NonNull BrokerOperationBundle brokerOperationBundle)
            throws BrokerCommunicationException {
        final String methodTag = TAG + ":communicateToBroker";
        final String operationName = brokerOperationBundle.getOperation().name();

        Logger.info(methodTag, "Broker operation: " + operationName+ " brokerPackage: " + brokerOperationBundle.getTargetBrokerAppPackageName());

        try {
            return mClient.performOperation(brokerOperationBundle);
        } catch (final RemoteException | InterruptedException | ExecutionException | TimeoutException | RuntimeException e) {
            // We know for a fact that in some OEM, bind service might throw a runtime exception.
            final String errorDescription = "Error occurred while awaiting (get) return of bound Service with " + mClient.getClass().getSimpleName();
            Logger.error(methodTag, errorDescription, e);
            throw new BrokerCommunicationException(CONNECTION_ERROR, getType(), errorDescription, e);
        } finally {
            mClient.disconnect();
        }
    }

    @Override
    public Type getType() {
        return Type.BOUND_SERVICE;
    }
}
