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
import com.microsoft.identity.common.adal.internal.AuthenticationConstants.BrokerContentProvider.API;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.logging.Logger;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.experimental.Accessors;

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

    @Getter
    @Accessors(prefix = "m")
    public enum Operation {
        MSAL_HELLO(API.MSAL_HELLO, BrokerAccountManagerOperation.HELLO),
        MSAL_GET_INTENT_FOR_INTERACTIVE_REQUEST(API.ACQUIRE_TOKEN_INTERACTIVE, BrokerAccountManagerOperation.GET_INTENT_FOR_INTERACTIVE_REQUEST),
        MSAL_ACQUIRE_TOKEN_SILENT(API.ACQUIRE_TOKEN_SILENT, BrokerAccountManagerOperation.ACQUIRE_TOKEN_SILENT),
        MSAL_GET_ACCOUNTS(API.GET_ACCOUNTS, BrokerAccountManagerOperation.GET_ACCOUNTS),
        MSAL_REMOVE_ACCOUNT(API.REMOVE_ACCOUNT, BrokerAccountManagerOperation.REMOVE_ACCOUNT),
        MSAL_GET_DEVICE_MODE(API.GET_DEVICE_MODE, BrokerAccountManagerOperation.GET_DEVICE_MODE),
        MSAL_GET_CURRENT_ACCOUNT_IN_SHARED_DEVICE(API.GET_CURRENT_ACCOUNT_SHARED_DEVICE, BrokerAccountManagerOperation.GET_CURRENT_ACCOUNT),
        MSAL_SIGN_OUT_FROM_SHARED_DEVICE(API.SIGN_OUT_FROM_SHARED_DEVICE, BrokerAccountManagerOperation.REMOVE_ACCOUNT_FROM_SHARED_DEVICE),
        MSAL_GENERATE_SHR(API.GENERATE_SHR, BrokerAccountManagerOperation.GENERATE_SHR),
        BROKER_GET_KEY_FROM_INACTIVE_BROKER(null, null),
        BROKER_API_HELLO(API.BROKER_HELLO, null),
        BROKER_API_GET_BROKER_ACCOUNTS(API.BROKER_GET_ACCOUNTS, null),
        BROKER_API_REMOVE_BROKER_ACCOUNT(API.BROKER_REMOVE_ACCOUNT, null),
        BROKER_API_UPDATE_BRT(API.BROKER_UPDATE_BRT, null),
        BROKER_GET_FLIGHTS(API.BROKER_GET_FLIGHTS, null),
        BROKER_ADD_FLIGHTS(API.BROKER_SET_FLIGHTS, null),
        MSAL_SSO_TOKEN(API.GET_SSO_TOKEN, null),
        DEVICE_REGISTRATION_OPERATIONS(API.DEVICE_REGISTRATION_PROTOCOLS, null);
        final API mContentApi;
        final String mAccountManagerOperation;
        Operation(API contentApi, String accountManagerOperation) {
            this.mContentApi = contentApi;
            this.mAccountManagerOperation = accountManagerOperation;
        }
    }

    @Getter
    @NonNull final private Operation operation;

    @Getter
    @NonNull final private String targetBrokerAppPackageName;

    @Getter
    @Nullable final private Bundle bundle;

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

    private String getAccountManagerAddAccountOperationKey() throws BrokerCommunicationException{
        final String methodTag = TAG + ":getAccountManagerAddAccountOperationKey";

        String accountManagerKey = operation.getAccountManagerOperation();
        if (accountManagerKey == null) {
            final String errorMessage = "Operation " + operation.name() + " is not supported by AccountManager addAccount().";
            Logger.warn(methodTag, errorMessage);
            throw new BrokerCommunicationException(
                    OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                    ACCOUNT_MANAGER_ADD_ACCOUNT,
                    errorMessage,
                    null);
        }
        return accountManagerKey;
    }

    public String getContentProviderPath() throws BrokerCommunicationException {
        final String methodTag = TAG + ":getContentProviderUriPath";

        final API contentApi = operation.getContentApi();
        if (contentApi == null) {
            final String errorMessage = "Operation " + operation.name() + " is not supported by ContentProvider.";
            Logger.warn(methodTag, errorMessage);
            throw new BrokerCommunicationException(
                    OPERATION_NOT_SUPPORTED_ON_CLIENT_SIDE,
                    CONTENT_PROVIDER,
                    errorMessage,
                    null);
        }
        return contentApi.getPath();
    }
}
