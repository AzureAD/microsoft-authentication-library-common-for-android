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
package com.microsoft.identity.common.java.commands;

import lombok.NonNull;

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.IControllerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.SuperBuilder;

// Suppressing rawtype warnings due to the generic types CommandCallback, BaseController
@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_OF_NONNULL_VALUE")
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
    @EqualsAndHashCode.Exclude
    private final String publicApiId;

    @NonNull
    @EqualsAndHashCode.Exclude
    private final IControllerFactory controllerFactory;

    public BaseCommand(@NonNull final CommandParameters parameters,
                       @NonNull final IControllerFactory controllerFactory,
                       @NonNull final CommandCallback callback,
                       @NonNull final String publicApiId) {
        this.parameters = parameters;
        this.callback = callback;
        this.controllerFactory = controllerFactory;
        this.publicApiId = publicApiId;
    }

    public abstract T execute() throws Exception;

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
