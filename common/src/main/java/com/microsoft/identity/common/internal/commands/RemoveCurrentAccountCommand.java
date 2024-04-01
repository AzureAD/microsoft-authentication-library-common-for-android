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
import com.microsoft.identity.common.java.commands.BaseCommand;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.commands.parameters.RemoveAccountCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.IControllerFactory;
import com.microsoft.identity.common.logging.Logger;

import java.util.List;

import lombok.EqualsAndHashCode;

/**
 * Command class to call controllers to remove the account and return the result to
 * {@see com.microsoft.identity.common.java.controllers.CommandDispatcher}.
 */
@EqualsAndHashCode(callSuper = true)
public class RemoveCurrentAccountCommand extends BaseCommand<Boolean> {

    private static final String TAG = RemoveCurrentAccountCommand.class.getSimpleName();

    public RemoveCurrentAccountCommand(@NonNull RemoveAccountCommandParameters parameters,
                                       @NonNull IControllerFactory controllerFactory,
                                       @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                                       @NonNull String publicApiId) {
        super(parameters, controllerFactory, callback, publicApiId);
    }

    @Override
    public Boolean execute() throws Exception {
        final String methodTag = TAG + ":execute";

        for (final BaseController controller : getControllerFactory().getAllControllers()) {
            Logger.verbose(
                    methodTag,
                    "Executing with controller: "
                            + controller.getClass().getSimpleName()
            );

            final boolean removed = controller.removeCurrentAccount((RemoveAccountCommandParameters) getParameters());

            if (removed) {
                return true;
            }
        }

        return false;
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return true;
    }
}
