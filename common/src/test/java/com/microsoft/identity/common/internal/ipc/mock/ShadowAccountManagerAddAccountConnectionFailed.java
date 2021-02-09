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
package com.microsoft.identity.common.internal.ipc.mock;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;

import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Implements(AccountManager.class)
public class ShadowAccountManagerAddAccountConnectionFailed {

    public AccountManagerFuture<Bundle> addAccount(final String accountType,
                                                   final String authTokenType, final String[] requiredFeatures,
                                                   final Bundle addAccountOptions,
                                                   final Activity activity, AccountManagerCallback<Bundle> callback, Handler handler) {
        return new AccountManagerFuture<Bundle>() {
            @Override public boolean cancel(boolean mayInterruptIfRunning) {
                return false;
            }

            @Override public boolean isCancelled() {
                return true;
            }

            @Override public boolean isDone() {
                return false;
            }

            @Override public Bundle getResult() throws AuthenticatorException, IOException, OperationCanceledException {
                throw new AuthenticatorException("Failed to bind");
            }

            @Override public Bundle getResult(long timeout, TimeUnit unit) throws AuthenticatorException, IOException, OperationCanceledException {
                throw new AuthenticatorException("Failed to bind");
            }
        };
    }
}
