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
package com.microsoft.identity.common.internal.commands;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.java.commands.ICommand;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

// Suppressing rawtype warnings due to the generic types CommandCallback, BaseController
@SuppressWarnings(WarningType.rawtype_warning)
@Getter
@EqualsAndHashCode
@SuperBuilder(toBuilder = true)
public abstract class BaseCommand<T> implements ICommand<T> {

    @NonNull
    private final CommandParameters parameters;

    @NonNull
    @EqualsAndHashCode.Exclude
    private final CommandCallback callback;

    @NonNull
    private final List<BaseController> controllers;

    @NonNull
    @EqualsAndHashCode.Exclude
    private final String publicApiId;

    public BaseCommand(@NonNull final CommandParameters parameters,
                       @NonNull final BaseController controller,
                       @NonNull final CommandCallback callback,
                       @NonNull final String publicApiId) {
        this.parameters = parameters;
        this.callback = callback;
        controllers = Collections.unmodifiableList(Arrays.asList(controller));
        this.publicApiId = publicApiId;
    }

    public BaseCommand(@NonNull final CommandParameters parameters,
                       @NonNull final List<BaseController> controllers,
                       @NonNull final CommandCallback callback,
                       @NonNull final String publicApiId) {
        this.parameters = parameters;
        this.controllers = Collections.unmodifiableList(new ArrayList<BaseController>(controllers));
        this.callback = callback;
        this.publicApiId = publicApiId;
    }

    public abstract T execute() throws Exception;

    public BaseController getDefaultController() {
        return controllers.get(0);
    }

    @Override
    public boolean isEligibleForCaching() {
        return false;
    }

    @Override
    public String getCorrelationId() {
        return getParameters().getCorrelationId();
    }

    @Override
    public boolean willReachTokenEndpoint() {
        return false;
    }
}
