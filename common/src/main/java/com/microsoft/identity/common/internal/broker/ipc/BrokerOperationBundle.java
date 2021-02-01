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

package com.microsoft.identity.common.internal.broker.ipc;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerAccountManagerOperation;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.logging.Logger;

import lombok.AllArgsConstructor;
import lombok.Getter;

import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.CONTENT_PROVIDER;

/**
 * An object that acts as a bridge between business logic and communication layer.
 * - Business logic will provide a request bundle, and specify which operation it wants to perform.
 * - Communication layer will determine how to communicate to the targeted service via the provided operation,
 * and pass the request bundle to the service accordingly.
 * <p>
 * Generally, the targeted service is the active broker.
 */
@AllArgsConstructor
public class BrokerOperationBundle {
    private static final String TAG = BrokerOperationBundle.class.getName();

    public enum Operation {
        MSAL_HELLO,
        MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST,
        MSAL_ACQUIRE_TOKEN_SILENT,
        MSAL_GET_ACCOUNTS,
        MSAL_REMOVE_ACCOUNT,
        MSAL_GET_DEVICE_MODE,
        MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE,
        MSAL_SIGN_OUT_FROM_SHARED_DEVICE,
        MSAL_GENERATE_SHR,
        BROKER_GET_KEY_FROM_INACTIVE_BROKER,
        BROKER_API_HELLO,
        BROKER_API_GET_BROKER_ACCOUNTS,
        BROKER_API_REMOVE_BROKER_ACCOUNT,
        BROKER_API_UPDATE_BRT,
        CALCULATION
    }

    @Getter
    @NonNull
    final private Operation operation;

    @Getter
    @NonNull
    final private String targetBrokerAppPackageName;

    @Getter
    @Nullable
    final private Bundle bundle;

    /**
     * Packs the response bundle with the account manager key.
     */
    public Bundle getAccountManagerBundle()
            throws BrokerCommunicationException {
        Bundle requestBundle = bundle;
        if (requestBundle == null) {
            requestBundle = new Bundle();
        }

        requestBundle.putString(
                AuthenticationConstants.Broker.BROKER_ACCOUNT_MANAGER_OPERATION_KEY,
                getAccountManagerAddAccountOperationKey());

        return requestBundle;
    }

    private String getAccountManagerAddAccountOperationKey() throws BrokerCommunicationException {
        final String methodName = ":getAccountManagerAddAccountOperationKey";

        switch (operation) {
            case MSAL_HELLO:
                return BrokerAccountManagerOperation.HELLO;

            case MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST:
                return BrokerAccountManagerOperation.GET_INTENT_FOR_INTERACTIVE_REQUEST;

            case MSAL_ACQUIRE_TOKEN_SILENT:
                return BrokerAccountManagerOperation.ACQUIRE_TOKEN_SILENT;

            case MSAL_GET_ACCOUNTS:
                return BrokerAccountManagerOperation.GET_ACCOUNTS;

            case MSAL_REMOVE_ACCOUNT:
                return BrokerAccountManagerOperation.REMOVE_ACCOUNT;

            case MSAL_GET_DEVICE_MODE:
                return BrokerAccountManagerOperation.GET_DEVICE_MODE;

            case MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE:
                return BrokerAccountManagerOperation.GET_CURRENT_ACCOUNT;

            case MSAL_SIGN_OUT_FROM_SHARED_DEVICE:
                return BrokerAccountManagerOperation.REMOVE_ACCOUNT_FROM_SHARED_DEVICE;

            case MSAL_GENERATE_SHR:
                return BrokerAccountManagerOperation.GENERATE_SHR;

            case CALCULATION:
                return BrokerAccountManagerOperation.CALCULATION;

            default:
                final String errorMessage = "Operation " + operation.name() + " is not supported by AccountManager addAccount().";
                Logger.warn(TAG + methodName, errorMessage);
                throw new BrokerCommunicationException(
                        OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                        ACCOUNT_MANAGER_ADD_ACCOUNT,
                        errorMessage,
                        null);
        }
    }

    public String getContentProviderPath() throws BrokerCommunicationException {
        final String methodName = ":getContentProviderUriPath";

        switch (operation) {
            case MSAL_HELLO:
                return BrokerContentProvider.MSAL_HELLO_PATH;

            case MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST:
                return BrokerContentProvider.MSAL_ACQUIRE_TOKEN_INTERACTIVE_PATH;

            case MSAL_ACQUIRE_TOKEN_SILENT:
                return BrokerContentProvider.MSAL_ACQUIRE_TOKEN_SILENT_PATH;

            case MSAL_GET_ACCOUNTS:
                return BrokerContentProvider.MSAL_GET_ACCOUNTS_PATH;

            case MSAL_REMOVE_ACCOUNT:
                return BrokerContentProvider.MSAL_REMOVE_ACCOUNTS_PATH;

            case MSAL_GET_DEVICE_MODE:
                return BrokerContentProvider.MSAL_GET_DEVICE_MODE_PATH;

            case MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE:
                return BrokerContentProvider.MSAL_GET_CURRENT_ACCOUNT_SHARED_DEVICE_PATH;

            case MSAL_SIGN_OUT_FROM_SHARED_DEVICE:
                return BrokerContentProvider.MSAL_SIGN_OUT_FROM_SHARED_DEVICE_PATH;

            case BROKER_API_HELLO:
                return BrokerContentProvider.BROKER_API_HELLO_PATH;

            case BROKER_API_GET_BROKER_ACCOUNTS:
                return BrokerContentProvider.BROKER_API_GET_BROKER_ACCOUNTS_PATH;

            case BROKER_API_REMOVE_BROKER_ACCOUNT:
                return BrokerContentProvider.BROKER_API_REMOVE_BROKER_ACCOUNT_PATH;

            case BROKER_API_UPDATE_BRT:
                return BrokerContentProvider.BROKER_API_UPDATE_BRT_PATH;

            case MSAL_GENERATE_SHR:
                return BrokerContentProvider.GENERATE_SHR_PATH;

            case CALCULATION:
                return BrokerContentProvider.CALCULATION_PATH;

            default:
                final String errorMessage = "Operation " + operation.name() + " is not supported by ContentProvider.";
                Logger.warn(TAG + methodName, errorMessage);
                throw new BrokerCommunicationException(
                        OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                        CONTENT_PROVIDER,
                        errorMessage,
                        null);
        }
    }
}
