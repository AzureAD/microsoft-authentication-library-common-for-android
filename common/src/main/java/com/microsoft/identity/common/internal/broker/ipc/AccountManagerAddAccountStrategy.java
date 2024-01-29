// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.broker.ipc;

import android.accounts.AccountManager;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.internal.util.ProcessUtil;
import com.microsoft.identity.common.logging.Logger;

import java.io.IOException;

import static com.microsoft.identity.common.exception.BrokerCommunicationException.Category.CONNECTION_ERROR;
import static com.microsoft.identity.common.internal.broker.ipc.IIpcStrategy.Type.ACCOUNT_MANAGER_ADD_ACCOUNT;
import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.BROKER_ACCOUNT_TYPE;

/**
 * A strategy for communicating with the targeted broker via AccountManager's addAccount().
 * This will only communicate to the owner of com.microsoft.workaccount account type.
 * <p>
 * NOTE: SuppressLint is added because this API requires MANAGE_ACCOUNTS for API<= 22.
 * AccountManagerUtil.canUseAccountManagerOperation() will validate that.
 */

@SuppressLint("MissingPermission")
public class AccountManagerAddAccountStrategy implements IIpcStrategy {
    private static final String TAG = AccountManagerAddAccountStrategy.class.getSimpleName();

    private final Context mContext;

    public AccountManagerAddAccountStrategy(final Context context) {
        mContext = context;
    }

    @Override
    @Nullable
    public Bundle communicateToBroker(final @NonNull BrokerOperationBundle brokerOperationBundle)
            throws BrokerCommunicationException {
        final String methodTag = TAG + ":communicateToBroker";
        final String operationName = brokerOperationBundle.getOperation().name();
        Logger.info(methodTag, "Broker operation: " + operationName+" brokerPackage: "+brokerOperationBundle.getTargetBrokerAppPackageName());
        try {
            final AccountManagerFuture<Bundle> resultBundle =
                    AccountManager.get(mContext)
                            .addAccount(
                                    BROKER_ACCOUNT_TYPE,
                                    AuthenticationConstants.Broker.AUTHTOKEN_TYPE,
                                    null,
                                    brokerOperationBundle.getAccountManagerBundle(),
                                    null,
                                    null,
                                    ProcessUtil.getPreferredHandler());

            Logger.verbose(methodTag, "Received result from broker");
            return resultBundle.getResult();
        } catch (final AuthenticatorException | IOException | OperationCanceledException e) {
            Logger.error(methodTag, e.getMessage(), e);
            throw new BrokerCommunicationException(CONNECTION_ERROR, getType(), "Failed to connect to AccountManager", e);
        }
    }

    @Override
    public Type getType() {
        return ACCOUNT_MANAGER_ADD_ACCOUNT;
    }
}
