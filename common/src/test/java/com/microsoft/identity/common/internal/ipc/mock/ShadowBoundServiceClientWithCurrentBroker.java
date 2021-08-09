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

import android.content.Intent;
import android.os.Bundle;
import android.os.IBinder;
import android.os.IInterface;
import android.os.RemoteException;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.IMicrosoftAuthService;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.internal.broker.BoundServiceClient;
import com.microsoft.identity.common.internal.ipc.IpcStrategyTests;

import org.robolectric.annotation.Implements;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeoutException;

@Implements(BoundServiceClient.class)
public class ShadowBoundServiceClientWithCurrentBroker<T extends IInterface> {
    protected @NonNull
    T connect(@NonNull final String targetServicePackageName)
            throws ClientException, InterruptedException, TimeoutException, ExecutionException {
        final IMicrosoftAuthService authService = new IMicrosoftAuthService() {
            @Override public IBinder asBinder() {
                return null;
            }

            @Override public Bundle hello(Bundle bundle) throws RemoteException {
                throw new RemoteException("Not Implemented");
            }

            @Override public Bundle getAccounts(Bundle bundle) throws RemoteException {
                throw new RemoteException("Not Implemented");
            }

            @Override public Bundle acquireTokenSilently(Bundle requestBundle) throws RemoteException {
                throw new RemoteException("Not Implemented");
            }

            @Override public Intent getIntentForInteractiveRequest() throws RemoteException {
                return IpcStrategyTests.getMockInteractiveRequestResultIntent();
            }

            @Override public Bundle removeAccount(Bundle bundle) throws RemoteException {
                throw new RemoteException("Not Implemented");
            }

            @Override public Bundle getDeviceMode() throws RemoteException {
                throw new RemoteException("Not Implemented");
            }

            @Override public Bundle getCurrentAccount(Bundle bundle) throws RemoteException {
                throw new RemoteException("Not Implemented");
            }

            @Override public Bundle removeAccountFromSharedDevice(Bundle bundle) throws RemoteException {
                throw new RemoteException("Not Implemented");
            }

            @Override
            public Bundle generateSignedHttpRequest(Bundle bundle) throws RemoteException {
                throw new RemoteException("Not Implemented");
            }
        };

        @SuppressWarnings("unchecked")
        final T returnVal = (T) authService;
        return returnVal;
    }
}