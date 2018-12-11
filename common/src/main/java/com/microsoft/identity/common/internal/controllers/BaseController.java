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
package com.microsoft.identity.common.internal.controllers;

import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public abstract class BaseController {

    private static final String TAG = BaseController.class.getSimpleName();

    public abstract AcquireTokenResult acquireToken(AcquireTokenOperationParameters request) throws ExecutionException, InterruptedException, ClientException, IOException, ArgumentException;

    public abstract void completeAcquireToken(int requestCode, int resultCode, final Intent data);

    public abstract AcquireTokenResult acquireTokenSilent(AcquireTokenSilentOperationParameters request) throws IOException, ClientException, UiRequiredException, ArgumentException;


    protected void throwIfNetworkNotAvailable(final Context context) throws ClientException {
        final String methodName = ":throwIfNetworkNotAvailable";
        final ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        final NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();

        if (networkInfo == null || !networkInfo.isConnected()) {
            throw new ClientException(
                    ClientException.DEVICE_NETWORK_NOT_AVAILABLE,
                    "Device network connection is not available."
            );
        }

        Logger.info(
                TAG + methodName,
                "Network status: connected"
        );
    }

}
