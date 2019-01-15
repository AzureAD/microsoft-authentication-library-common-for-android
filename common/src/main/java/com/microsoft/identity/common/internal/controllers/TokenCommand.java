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
import android.support.annotation.NonNull;

import com.microsoft.identity.common.exception.ArgumentException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ServiceException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.ILocalAuthenticationCallback;
import com.microsoft.identity.common.internal.request.OperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ExecutionException;

public class TokenCommand implements TokenOperation {

    private static final String TAG = TokenCommand.class.getSimpleName();

    protected OperationParameters mParameters;
    protected BaseController mController;
    protected List<BaseController> mControllers;
    protected Context mContext;
    protected ILocalAuthenticationCallback mCallback;


    public TokenCommand() {
    }

    public TokenCommand(@NonNull final Context context,
                        @NonNull final OperationParameters parameters,
                        @NonNull final BaseController controller,
                        @NonNull final ILocalAuthenticationCallback callback) {
        mContext = context;
        mParameters = parameters;
        mController = controller;
        mCallback = callback;

        if (!(mParameters instanceof AcquireTokenSilentOperationParameters)) {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    public TokenCommand(@NonNull final Context context,
                        @NonNull final OperationParameters parameters,
                        @NonNull final List<BaseController> controllers,
                        @NonNull final ILocalAuthenticationCallback callback) {
        mContext = context;
        mParameters = parameters;
        mController = null;
        mControllers = controllers;
        mCallback = callback;

        if (!(mParameters instanceof AcquireTokenSilentOperationParameters)) {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public AcquireTokenResult execute() throws InterruptedException, ExecutionException, IOException, ClientException, UiRequiredException, ArgumentException, ServiceException {
        AcquireTokenResult result = null;
        final String methodName = ":execute";

        for(BaseController controller : mControllers) {
            try {
                com.microsoft.identity.common.internal.logging.Logger.verbose(
                        TAG + methodName,
                        "Executing with controller: " + controller.getClass().getSimpleName()
                );
                result = controller.acquireTokenSilent((AcquireTokenSilentOperationParameters) getParameters());
                if(result.getSucceeded()){
                    com.microsoft.identity.common.internal.logging.Logger.verbose(
                            TAG + methodName,
                            "Executing with controller: " + controller.getClass().getSimpleName() + ": Succeeded"
                    );
                    return result;
                }
            }catch(UiRequiredException e){
                if(e.getErrorCode().equals(UiRequiredException.INVALID_GRANT)){
                    continue;
                }else{
                    throw e;
                }
            }
        }

        return result;
    }

    @Override
    public void notify(int requestCode, int resultCode, Intent data) {
        throw new UnsupportedOperationException();
    }

    public OperationParameters getParameters() {
        return mParameters;
    }

    public void setParameters(OperationParameters parameters) {
        if (!(parameters instanceof AcquireTokenSilentOperationParameters)) {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
        this.mParameters = parameters;
    }

    public BaseController getController() {
        return mController;
    }

    public List<BaseController> getControllers () { return mControllers; }

    public void setControllers(List<BaseController> controllers){ this.mControllers = controllers;}

    public void setController(BaseController controller) {
        this.mController = controller;
    }

    public Context getContext() {
        return mContext;
    }

    public void setContext(Context context) {
        this.mContext = context;
    }

    public ILocalAuthenticationCallback getCallback() {
        return mCallback;
    }

    public void setCallback(ILocalAuthenticationCallback callback) {
        this.mCallback = callback;
    }
}
