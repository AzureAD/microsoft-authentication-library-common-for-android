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
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.aad.adal.IBrokerAccountService;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;

import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.BOUND_SERVICE;

/**
 * Client that wraps the code necessary to bind to the a service that implements IBrokerAccountService.aidl
 */
public class BrokerAccountServiceClient extends BoundServiceClient<IBrokerAccountService> {

    private static final String BROKER_ACCOUNT_SERVICE_INTENT_FILTER = "com.microsoft.workaccount.BrokerAccount";
    private static final String BROKER_ACCOUNT_SERVICE_CLASS_NAME = "com.microsoft.aad.adal.BrokerAccountService";

    /**
     * BrokerAccountServiceClient's constructor.
     *
     * @param context Application context.
     */
    public BrokerAccountServiceClient(@NonNull final Context context) {
        super(context,
                BROKER_ACCOUNT_SERVICE_CLASS_NAME,
                BROKER_ACCOUNT_SERVICE_INTENT_FILTER
        );
    }

    /**
     * BrokerAccountServiceClient's constructor.
     *
     * @param context          Application context.
     * @param timeOutInSeconds The client will terminates its connection if it can't connect to the service by this time out.
     */
    public BrokerAccountServiceClient(@NonNull final Context context,
                                      final int timeOutInSeconds) {
        super(context,
                BROKER_ACCOUNT_SERVICE_CLASS_NAME,
                BROKER_ACCOUNT_SERVICE_INTENT_FILTER,
                timeOutInSeconds
        );
    }

    @Override
    @NonNull IBrokerAccountService getInterfaceFromIBinder(@NonNull IBinder binder) {
        final IBrokerAccountService service = IBrokerAccountService.Stub.asInterface(binder);
        if (service == null) {
            throw new IllegalStateException("Failed to extract IBrokerAccountService from IBinder.", null);
        }
        return service;
    }

    @Override
    @NonNull
    public Bundle performOperationInternal(@NonNull BrokerOperationBundle brokerOperationBundle,
                                           @NonNull IBrokerAccountService brokerAccountService)
            throws RemoteException, BrokerCommunicationException {
        final Bundle inputBundle = brokerOperationBundle.getBundle();
        if (brokerOperationBundle.getOperation() == BrokerOperationBundle.Operation.BROKER_GET_KEY_FROM_INACTIVE_BROKER) {
            return brokerAccountService.getInactiveBrokerKey(inputBundle);
        }
        throw new BrokerCommunicationException(
                OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                BOUND_SERVICE,
                "Operation not supported. Wrong BoundServiceClient used.",
                null);
    }
}

