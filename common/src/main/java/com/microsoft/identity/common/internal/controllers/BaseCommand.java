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

public abstract class BaseCommand<T> implements Command<T> {
    private OperationParameters mParameters;
    private List<BaseController> mControllers;
    private CommandCallback mCallback;
    private String mPublicApiId;

    public BaseCommand(@NonNull final OperationParameters parameters,
                       @NonNull final BaseController controller,
                       @NonNull final CommandCallback callback) {
        mParameters = parameters;
        mControllers = new ArrayList<>();
        mCallback = callback;

        mControllers.add(controller);
    }

    public BaseCommand(@NonNull final OperationParameters parameters,
                       @NonNull final List<BaseController> controllers,
                       @NonNull final CommandCallback callback) {
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

    public CommandCallback getCallback() {
        return mCallback;
    }

    public void setCallback(CommandCallback callback) {
        this.mCallback = callback;
    }

    public void setPublicApiId(String publicApiId) {
        this.mPublicApiId = publicApiId;
    }

    public String getPublicApiId() {
        return mPublicApiId;
    }

    public abstract T execute() throws Exception;

    public BaseController getDefaultController() {
        return mControllers.get(0);
    }

    public abstract int getCommandNameHashCode();

    public boolean isEligibleForCaching(){
        return false;
    }

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BaseCommand)) return false;

        BaseCommand<?> that = (BaseCommand<?>) o;

        return mParameters.equals(that.mParameters);
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        return  31 * getCommandNameHashCode() + mParameters.hashCode();
    }
    //CHECKSTYLE:ON
}
