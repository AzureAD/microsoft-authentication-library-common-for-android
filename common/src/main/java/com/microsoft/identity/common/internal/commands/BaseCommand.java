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

import android.util.Log;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.WarningType;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;

import java.util.ArrayList;
import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.Getter;

// Suppressing rawtype warnings due to the generic types CommandCallback, BaseController
@SuppressWarnings(WarningType.rawtype_warning)
@Getter
@EqualsAndHashCode
public abstract class BaseCommand<T> implements Command<T> {

    private CommandParameters parameters;

    @EqualsAndHashCode.Exclude
    private CommandCallback callback;

    private List<BaseController> controllers;

    @EqualsAndHashCode.Exclude
    private String publicApiId;

    public BaseCommand(@NonNull final CommandParameters parameters,
                       @NonNull final BaseController controller,
                       @NonNull final CommandCallback callback,
                       @NonNull final String publicApiId) {
        this.parameters = parameters;
        this.controllers = new ArrayList<>();
        this.callback = callback;

        controllers.add(controller);
        this.publicApiId = publicApiId;
    }

    public BaseCommand(@NonNull final CommandParameters parameters,
                       @NonNull final List<BaseController> controllers,
                       @NonNull final CommandCallback callback,
                       @NonNull final String publicApiId) {
        this.parameters = parameters;
        this.controllers = controllers;
        this.callback = callback;
        this.publicApiId = publicApiId;
    }

    public abstract T execute() throws Exception;

    public T run() throws Exception {
        try {
            return execute();
        } catch (final Exception e) {
            Log.wtf("BaseCommand:run", "Unhandled exception in command " + this.getClass().getCanonicalName() + "#execute()", e);
            throw e;
        }
    }

    public BaseController getDefaultController() {
        return controllers.get(0);
    }

    public boolean isEligibleForCaching() {
        return false;
    }
}
