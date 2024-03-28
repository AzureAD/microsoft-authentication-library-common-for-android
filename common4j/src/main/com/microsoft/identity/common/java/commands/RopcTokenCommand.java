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

import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.IControllerFactory;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import java.util.List;

import lombok.NonNull;

/**
 * ROPC command for Resource Owner Password Credentials.
 * see: https://docs.microsoft.com/en-us/azure/active-directory/develop/v2-oauth-ropc
 */
public class RopcTokenCommand extends TokenCommand {

    private static final String TAG = RopcTokenCommand.class.getSimpleName();

    public RopcTokenCommand(@NonNull final RopcTokenCommandParameters parameters,
                            @NonNull final IControllerFactory controllerFactory,
                            @SuppressWarnings(WarningType.rawtype_warning) @NonNull final CommandCallback callback,
                            @NonNull final String publicApiId) {
        super(parameters, controllerFactory, callback, publicApiId);
    }

    @Override
    public AcquireTokenResult execute() throws Exception {
        final String methodName = ":execute";
        if (getParameters() instanceof RopcTokenCommandParameters) {
            Logger.info(
                    TAG + methodName,
                    "Executing ROPC token command..."
            );

            return getControllerFactory().getDefaultController()
                    .acquireTokenWithPassword(
                            (RopcTokenCommandParameters) getParameters()
                    );
        } else {
            throw new IllegalArgumentException("Invalid operation parameters");
        }
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return false;
    }

    @Override
    public boolean isEligibleForCaching() {
        return true;
    }
}
