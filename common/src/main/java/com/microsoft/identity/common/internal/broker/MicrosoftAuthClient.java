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
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.client.IMicrosoftAuthService;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.broker.ipc.BrokerOperationBundle;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_ACTIVITY_NAME;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.Broker.BROKER_PACKAGE_NAME;
import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.BOUND_SERVICE;

/**
 * Client that wraps the code necessary to bind to the a service that implements IMicrosoftAuthService.aidl
 */
public class MicrosoftAuthClient extends BoundServiceClient<IMicrosoftAuthService> {
    private static final String MICROSOFT_AUTH_SERVICE_INTENT_FILTER = "com.microsoft.identity.client.MicrosoftAuth";
    private static final String MICROSOFT_AUTH_SERVICE_CLASS_NAME = "com.microsoft.identity.client.MicrosoftAuthService";

    /**
     * MicrosoftAuthClient's constructor.
     *
     * @param context Application context.
     */
    public MicrosoftAuthClient(@NonNull final Context context) {
        super(context,
                MICROSOFT_AUTH_SERVICE_CLASS_NAME,
                MICROSOFT_AUTH_SERVICE_INTENT_FILTER);
    }

    /**
     * MicrosoftAuthClient's constructor.
     *
     * @param context          Application context.
     * @param timeOutInSeconds The client will terminates its connection if it can't connect to the service by this time out.
     */
    public MicrosoftAuthClient(@NonNull final Context context,
                               final int timeOutInSeconds) {
        super(context,
                MICROSOFT_AUTH_SERVICE_INTENT_FILTER,
                MICROSOFT_AUTH_SERVICE_CLASS_NAME,
                timeOutInSeconds);
    }

    @Override
    @NonNull
    Bundle performOperationInternal(@NonNull final BrokerOperationBundle brokerOperationBundle,
                                    @NonNull final IMicrosoftAuthService microsoftAuthService)
            throws RemoteException, BrokerCommunicationException {

        final Bundle inputBundle = brokerOperationBundle.getBundle();
        switch (brokerOperationBundle.getOperation()) {
            case MSAL_HELLO:
                return microsoftAuthService.hello(inputBundle);

            case MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST:
                final Intent intent = microsoftAuthService.getIntentForInteractiveRequest();
                final Bundle bundle = intent.getExtras();

                //older brokers (pre-ContentProvider) are ONLY sending these values in the intent itself.
                if (intent.getComponent() != null &&
                        !TextUtils.isEmpty(intent.getPackage()) &&
                        !TextUtils.isEmpty(intent.getComponent().getClassName())){
                    bundle.putString(BROKER_PACKAGE_NAME, intent.getPackage());
                    bundle.putString(BROKER_ACTIVITY_NAME, intent.getComponent().getClassName());
                }

                return bundle;

            case MSAL_ACQUIRE_TOKEN_SILENT:
                return microsoftAuthService.acquireTokenSilently(inputBundle);

            case MSAL_GET_ACCOUNTS:
                return microsoftAuthService.getAccounts(inputBundle);

            case MSAL_REMOVE_ACCOUNT:
                return microsoftAuthService.removeAccount(inputBundle);

            case MSAL_GET_DEVICE_MODE:
                return microsoftAuthService.getDeviceMode();

            case MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE:
                return microsoftAuthService.getCurrentAccount(inputBundle);

            case MSAL_SIGN_OUT_FROM_SHARED_DEVICE:
                return microsoftAuthService.removeAccountFromSharedDevice(inputBundle);

            case MSAL_GENERATE_SHR:
                return microsoftAuthService.generateSignedHttpRequest(inputBundle);

            default:
                final String errorMessage = "Operation " + brokerOperationBundle.getOperation().name() + " is not supported by MicrosoftAuthClient.";
                throw new BrokerCommunicationException(
                        OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                        BOUND_SERVICE,
                        errorMessage, null);
        }
    }

    @Override
    @NonNull IMicrosoftAuthService getInterfaceFromIBinder(@NonNull IBinder binder) {
        final IMicrosoftAuthService service = IMicrosoftAuthService.Stub.asInterface(binder);
        if (service == null) {
            throw new IllegalStateException("Failed to extract IMicrosoftAuthService from IBinder.", null);
        }
        return service;
    }
}
