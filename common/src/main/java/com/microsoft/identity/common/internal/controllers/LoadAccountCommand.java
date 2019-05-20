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

import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.os.RemoteException;
import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.internal.dto.AccountRecord;
import com.microsoft.identity.common.internal.request.OperationParameters;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;

/**
 * Command class to call controllers to load accounts and return the account list to
 * {@see com.microsoft.identity.common.internal.controllers.ApiDispatcher}.
 */
public class LoadAccountCommand implements Command<List<AccountRecord>> {
    private static final String TAG = LoadAccountCommand.class.getSimpleName();

    protected OperationParameters mParameters;
    protected List<BaseController> mControllers;
    protected TaskCompletedCallbackWithError mCallback;

    public LoadAccountCommand(@NonNull final OperationParameters parameters,
                              @NonNull final BaseController controller,
                              @NonNull final TaskCompletedCallbackWithError callback) {
        mParameters = parameters;
        mControllers = new ArrayList<>();
        mCallback = callback;

        mControllers.add(controller);
    }

    public LoadAccountCommand(@NonNull final OperationParameters parameters,
                              @NonNull final List<BaseController> controllers,
                              @NonNull final TaskCompletedCallbackWithError callback) {
        mParameters = parameters;
        mControllers = controllers;
        mCallback = callback;
    }

    public OperationParameters getParameters() {
        return mParameters;
    }

    public void setParameters(OperationParameters parameters) {
        mParameters = parameters;
    }

    public List<BaseController> getControllers() {
        return mControllers;
    }

    public void setControllers(List<BaseController> controllers) {
        mControllers = controllers;
    }

    public TaskCompletedCallbackWithError getCallback() {
        return mCallback;
    }

    public void setCallback(TaskCompletedCallbackWithError callback) {
        this.mCallback = callback;
    }

    public List<AccountRecord> execute()
            throws ClientException, InterruptedException, ExecutionException, RemoteException, OperationCanceledException, IOException, AuthenticatorException {
        final String methodName = ":execute";

        List<AccountRecord> result = new ArrayList<>();

        for (int ii = 0; ii < mControllers.size(); ii++) {
            final BaseController controller = mControllers.get(ii);
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Executing with controller: "
                            + controller.getClass().getSimpleName()
            );

            result.addAll(controller.getAccounts(getParameters()));
        }

        return result;
    }
}
