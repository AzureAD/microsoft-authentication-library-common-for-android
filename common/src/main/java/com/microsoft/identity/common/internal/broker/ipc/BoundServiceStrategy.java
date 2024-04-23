package com.microsoft.identity.common.internal.broker.ipc;

import android.os.Bundle;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.BoundServiceClient;
import com.microsoft.identity.common.logging.Logger;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.CONNECTION_ERROR;

/**
 * A strategy for communicating with the targeted broker via Bound Service.
 */
public class BoundServiceStrategy<T extends IInterface> extends AbstractIpcStrategyWithServiceValidation {
    private static final String TAG = BoundServiceStrategy.class.getSimpleName();

    private final BoundServiceClient<T> mClient;

    public BoundServiceStrategy(final @NonNull BoundServiceClient<T> boundServiceClient) {
        super(false);
        mClient = boundServiceClient;
    }

    @VisibleForTesting
    protected BoundServiceStrategy(final @NonNull BoundServiceClient<T> boundServiceClient,
                                   final boolean shouldBypassSupportValidation) {
        super(shouldBypassSupportValidation);
        mClient = boundServiceClient;
    }

    @Override
    @Nullable
    protected Bundle communicateToBrokerAfterValidation(final @NonNull BrokerOperationBundle brokerOperationBundle)
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
    @NonNull
    public Type getType() {
        return Type.BOUND_SERVICE;
    }

    @Override
    public boolean isSupportedByTargetedBroker(@NonNull final String targetedBrokerPackageName) {
        return mClient.isBoundServiceSupported(targetedBrokerPackageName);
    }
}
