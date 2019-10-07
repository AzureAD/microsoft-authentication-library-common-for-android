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

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.request.OperationParameters;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseAccountCommand<T> implements Command<T> {
    private OperationParameters mParameters;
    private List<BaseController> mControllers;
    private TaskCompletedCallbackWithError mCallback;

    public BaseAccountCommand(@NonNull final OperationParameters parameters,
                              @NonNull final BaseController controller,
                              @NonNull final TaskCompletedCallbackWithError callback) {
        mParameters = parameters;
        mControllers = new ArrayList<>();
        mCallback = callback;

        mControllers.add(controller);
    }

    public BaseAccountCommand(@NonNull final OperationParameters parameters,
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

    public abstract T execute() throws Exception;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseAccountCommand)) return false;

        BaseAccountCommand<?> that = (BaseAccountCommand<?>) o;

        return mParameters.equals(that.mParameters);
    }

    @Override
    public int hashCode() {
        return mParameters.hashCode();
    }
}
