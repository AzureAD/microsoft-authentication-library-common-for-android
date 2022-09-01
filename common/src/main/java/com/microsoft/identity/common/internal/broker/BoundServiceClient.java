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

package com.microsoft.identity.common.internal.broker;

import android.content.Context;
import android.content.Intent;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;
import com.microsoft.identity.common.java.util.ResultFuture;
import com.microsoft.identity.common.logging.Logger;

import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.BOUND_SERVICE;

/**
 * Interface for a Bound Service client.
 * A separate implementation is required for each AIDL interface (android.os.IInterface)
 */
public abstract class BoundServiceClient<T extends IInterface> {
    private static final String TAG = BoundServiceClient.class.getSimpleName();

    private static final int DEFAULT_BIND_TIMEOUT_IN_SECONDS = 30;

    protected final Context mContext;
    private final int mTimeOutInSeconds;
    private final String mTargetServiceClassName;
    private final String mTargetServiceIntentFilter;

    private BoundServiceConnection mConnection;
    private boolean mHasStartedBinding;

    /**
     * Perform the given operation with the given .aidl {@link IInterface}
     */
    abstract @Nullable Bundle performOperationInternal(@NonNull final BrokerOperationBundle inputBundle,
                                                       @NonNull final T aidlInterface) throws RemoteException, BrokerCommunicationException;

    /**
     * Extracts {@link IInterface} from a given {@link IBinder}
     * i.e. T.Stub.asInterface(binder), where T is an .aidl {@link IInterface}.
     */
    abstract @NonNull T getInterfaceFromIBinder(@NonNull final IBinder binder);

    /**
     * BoundServiceClient's constructor.
     *
     * @param context                   application context.
     * @param targetServiceClassName    Full class name of the service that implements the AIDL interface.
     * @param targetServiceIntentFilter Intent filter of the service that implements the AIDL interface.
     */
    public BoundServiceClient(@NonNull final Context context,
                              @NonNull final String targetServiceClassName,
                              @NonNull final String targetServiceIntentFilter) {
        this(context, targetServiceClassName, targetServiceIntentFilter, DEFAULT_BIND_TIMEOUT_IN_SECONDS);
    }

    /**
     * BoundServiceClient's default constructor.
     *
     * @param context                   application context.
     * @param targetServiceClassName    Full class name of the service that implements the AIDL interface.
     * @param targetServiceIntentFilter Intent filter of the service that implements the AIDL interface.
     * @param timeOutInSeconds          the client will terminates its connection if it can't connect to the service by this time out.
     */
    public BoundServiceClient(@NonNull final Context context,
                              @NonNull final String targetServiceClassName,
                              @NonNull final String targetServiceIntentFilter,
                              final int timeOutInSeconds) {
        mContext = context;
        mTimeOutInSeconds = timeOutInSeconds;
        mTargetServiceClassName = targetServiceClassName;
        mTargetServiceIntentFilter = targetServiceIntentFilter;
    }

    /**
     * Connects this client to the AIDL service and passes the request bundle to perform the operation.
     * The inherited class will use the connect() function to connects to the targeted service,
     * and use the data inside {@link BrokerOperationBundle} to pick the AIDL method to invoke accordingly.
     *
     * @param inputBundle a {@link BrokerOperationBundle} containing a request bundle.
     * @return a bundle that contains a response from the AIDL service.
     */
    public @Nullable Bundle performOperation(@NonNull final BrokerOperationBundle inputBundle)
            throws RemoteException, BrokerCommunicationException, InterruptedException, ExecutionException, TimeoutException {
        final T aidlInterface = connect(inputBundle.getTargetBrokerAppPackageName());
        return performOperationInternal(inputBundle, aidlInterface);
    }

    /**
     * Binds to the service.
     *
     * @param targetServicePackageName Package name of the app this client will talk to.
     */
    protected @NonNull T connect(@NonNull final String targetServicePackageName)
            throws BrokerCommunicationException, InterruptedException, TimeoutException, ExecutionException {
        final String methodTag = TAG + ":connect";

        if (!isBoundServiceSupported(targetServicePackageName)) {
            final String errorMessage = "Bound service is not supported.";
            Logger.info(methodTag, errorMessage);
            throw new BrokerCommunicationException(
                    OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
                    BOUND_SERVICE,
                    errorMessage,
                    null);
        }

        final ResultFuture<IBinder> future = new ResultFuture<>();
        mConnection = new BoundServiceConnection(future);
        mHasStartedBinding = mContext.bindService(getIntentForBoundService(targetServicePackageName), mConnection, Context.BIND_AUTO_CREATE);

        if (!mHasStartedBinding) {
            final String errorMessage = "failed to bind. The service is not available.";
            Logger.info(methodTag, errorMessage);
            throw new BrokerCommunicationException(
                    OPERATION_NOT_SUPPORTED_ON_SERVER_SIDE,
                    BOUND_SERVICE,
                    errorMessage,
                    null);
        }

        Logger.info(methodTag, "Android is establishing the bound service connection.");
        final IBinder binder = future.get(mTimeOutInSeconds, TimeUnit.SECONDS);
        return getInterfaceFromIBinder(binder);
    }

    /**
     * Disconnects (unbinds) from the service.
     */
    public void disconnect() {
        final String methodTag = TAG + ":disconnect";
        if (mHasStartedBinding) {
            try {
                mContext.unbindService(mConnection);
            } catch (final IllegalArgumentException e) {
                // This is coming from LoadedApk framework code when there is some error unbinding the service,
                // possibly due to it not having been registered correctly in the first place, or already unregistered.
                // Since this is the cleanup path, just handle log this and move on.
                final String errorDescription = "Error occurred while unbinding bound Service with " + getClass().getSimpleName();
                Logger.error(methodTag, errorDescription, e);
            }
            mHasStartedBinding = false;
        }
    }

    /**
     * returns true if the target package supports this bound service
     *
     * @param targetServicePackageName Package name of the app this client will talk to.
     */
    public boolean isBoundServiceSupported(@NonNull final String targetServicePackageName) {
        final List<ResolveInfo> info = mContext.getPackageManager().queryIntentServices(getIntentForBoundService(targetServicePackageName), 0);
        return info != null && info.size() > 0;
    }

    /**
     * returns an intent object for bind service.
     *
     * @param targetServicePackageName Package name of the app this client will talk to.
     */
    private @NonNull Intent getIntentForBoundService(@NonNull final String targetServicePackageName) {
        final Intent boundServiceIntent = new Intent(mTargetServiceIntentFilter);
        boundServiceIntent.setPackage(targetServicePackageName);
        boundServiceIntent.setClassName(targetServicePackageName, mTargetServiceClassName);
        return boundServiceIntent;
    }
}
