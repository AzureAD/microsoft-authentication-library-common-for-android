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

import com.microsoft.identity.common.java.commands.BaseCommand;
import com.microsoft.identity.common.java.commands.CommandCallback;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.result.GenerateShrResult;

import java.util.List;

import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;
import lombok.experimental.SuperBuilder;

import static com.microsoft.identity.common.java.exception.ErrorStrings.NO_ACCOUNT_FOUND;

/**
 * Command class to perform generation of AT-less SHRs on behalf of a user.
 */
@SuperBuilder()
@Accessors(prefix = "m")
@EqualsAndHashCode(callSuper = true)
public class GenerateShrCommand extends BaseCommand<GenerateShrResult> {

    private static final String TAG = GenerateShrCommand.class.getSimpleName();

    /**
     * Constructs a new GenerateShrCommand.
     *
     * @param parameters  The command's input parameters.
     * @param controllers The controllers on which to run this command.
     * @param callback    The command to notify once execution has completed.
     * @param publicApiId The public API ID of this command.
     */
    public GenerateShrCommand(@NonNull final GenerateShrCommandParameters parameters,
                              @NonNull final List<BaseController> controllers,
                              @NonNull final CommandCallback<GenerateShrResult, BaseException> callback,
                              @NonNull final String publicApiId) {
        super(parameters, controllers, callback, publicApiId);
    }

    @Override
    public GenerateShrResult execute() throws Exception {
        final String methodTag = TAG + ":execute";

        GenerateShrResult result = null;
        final GenerateShrCommandParameters parameters = (GenerateShrCommandParameters) getParameters();

        // Iterate over our controllers, to service the request either locally or via the broker...
        // if the local (embedded) cache contains tokens for the supplied user, we will sign using
        // the embedded PoP keys. If no local user-state exists, the broker will be delegated to
        // where the same check is performed.
        BaseController controller;
        for (int ii = 0; ii < getControllers().size(); ii++) {
            controller = getControllers().get(ii);

            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    methodTag,
                    "Executing with controller: "
                            + controller.getClass().getSimpleName()
            );

            result = controller.generateSignedHttpRequest(parameters);

            if (null != result.getErrorCode()) {
                final String errorCode = result.getErrorCode();
                final String errorMessage = result.getErrorMessage();

                // To support a consistent communication model between the local flow and the
                // broker flow, errors will be returned as properties of the result, instead
                // of as thrown Exceptions
                if (NO_ACCOUNT_FOUND.equalsIgnoreCase(errorCode)) {
                    if (getControllers().size() > ii + 1) {
                        // Try our next controller
                        continue;
                    } else {
                        throw new UiRequiredException(errorCode, errorMessage);
                    }
                } else {
                    throw new ClientException(errorCode, errorMessage);
                }
            } else {
                Logger.verbose(
                        methodTag,
                        "Executing with controller: "
                                + controller.getClass().getSimpleName()
                                + ": Succeeded"
                );

                return result;
            }
        }

        return result;
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        // There is no web service interaction in this flow.
        return false;
    }
}
