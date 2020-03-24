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

import java.util.List;

/**
 * Command class to call controllers to remove the account and return the result to
 * {@see com.microsoft.identity.common.internal.controllers.CommandDispatcher}.
 */
public class RemoveAccountCommand extends BaseCommand<Boolean> {
    private static final String TAG = RemoveAccountCommand.class.getSimpleName();

    public RemoveAccountCommand(@NonNull final OperationParameters parameters,
                                @NonNull final List<BaseController> controllers,
                                @NonNull final CommandCallback callback) {
        super(parameters, controllers, callback);
    }

    @Override
    public Boolean execute() throws Exception {
        final String methodName = ":execute";

        boolean result = false;

        for (int ii = 0; ii < getControllers().size(); ii++) {
            final BaseController controller = getControllers().get(ii);
            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Executing with controller: "
                            + controller.getClass().getSimpleName()
            );

            result = controller.removeAccount(getParameters());
        }

        return result;
    }

    @Override
    public int getCommandNameHashCode() {
        return TAG.hashCode();
    }
}
