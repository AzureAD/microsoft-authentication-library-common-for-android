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

import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.UiRequiredException;
import com.microsoft.identity.common.internal.authscheme.IPoPAuthenticationSchemeParams;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.GenerateShrCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.result.GenerateShrResult;

import java.util.List;

/**
 * Command class to perform generation of AT-less SHRs on behalf of a user.
 */
public class GenerateShrCommand extends BaseCommand<GenerateShrResult> {

    private static final String TAG = GenerateShrCommand.class.getSimpleName();

    public GenerateShrCommand(@NonNull final GenerateShrCommandParameters parameters,
                              @NonNull final List<BaseController> controllers,
                              @NonNull final CommandCallback<GenerateShrResult, ClientException> callback,
                              @NonNull final String publicApiId) {
        super(parameters, controllers, callback, publicApiId);
    }

    @Override
    public GenerateShrResult execute() throws Exception {
        final String methodName = ":execute";

        GenerateShrResult result = null;
        final GenerateShrCommandParameters parameters = (GenerateShrCommandParameters) getParameters();

        // Iterate over our controllers, to service the request either locally or via the broker...
        BaseController controller;
        for (int ii = 0; ii < getControllers().size(); ii++) {
            controller = getControllers().get(ii);

            com.microsoft.identity.common.internal.logging.Logger.verbose(
                    TAG + methodName,
                    "Executing with controller: "
                            + controller.getClass().getSimpleName()
            );

            // TODO invoke the call on the controller...
            try {
                result = controller.generateSignedHttpRequest(parameters);
            } catch (final UiRequiredException e) {
                // TODO Move on to the next controller
            } catch (final Exception e) {

            }
        }

        return result;
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return false;
    }
}
