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
import com.microsoft.identity.common.internal.request.generated.CommandContext;
import com.microsoft.identity.common.internal.request.generated.CommandParameters;

import java.util.ArrayList;
import java.util.List;

public abstract class BaseCommand<T,
        GenericCommandContext extends CommandContext,
        GenericCommandParameters extends CommandParameters,
        GenericCommandCallback extends CommandCallback> implements Command<T> {
    private GenericCommandContext mCommandContext;
    private GenericCommandParameters mCommandParameters;
    private List<BaseController> mControllers;
    private GenericCommandCallback mCallback;
    private String mPublicApiId;

    public BaseCommand(@NonNull final GenericCommandContext GenericCommandContext,
                       @NonNull final GenericCommandParameters commandParameters,
                       @NonNull final List<BaseController> controllers,
                       @NonNull final GenericCommandCallback callback) {
        mCommandContext = GenericCommandContext;
        mCommandParameters = commandParameters;
        mControllers = controllers;
        mCallback = callback;
    }

    public BaseCommand(@NonNull final GenericCommandContext commandContext,
                       @NonNull final GenericCommandParameters commandParameters,
                       @NonNull final BaseController controller,
                       @NonNull final GenericCommandCallback callback) {
        mCommandContext = commandContext;
        mCommandParameters = commandParameters;
        mControllers = new ArrayList<>();
        mControllers.add(controller);
        mCallback = callback;
    }

    public GenericCommandParameters getParameters() {
        return mCommandParameters;
    }
    public GenericCommandContext getContext() {
        return mCommandContext;
    }
    public List<BaseController> getControllers() {
        return mControllers;
    }
    public GenericCommandCallback getCallback() {
        return mCallback;
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

        BaseCommand<?, ?, ?, ?> that = (BaseCommand<?, ?, ?, ?>) o;

        return mCommandParameters.equals(that.mCommandParameters);
    }
    //CHECKSTYLE:ON

    //CHECKSTYLE:OFF
    // This method is generated. Checkstyle and/or PMD has been disabled.
    // This method *must* be regenerated if the class' structural definition changes through the
    // addition/subtraction of fields.
    @SuppressWarnings("PMD")
    @Override
    public int hashCode() {
        return  31 * getCommandNameHashCode() + mCommandParameters.hashCode();
    }
    //CHECKSTYLE:ON
}
