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
import com.microsoft.identity.common.java.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import lombok.NonNull;

/**
 * This command is used in the Device Code Flow (DCF) protocol to acquire token.
 * Takes in a parameters object containing the  desired access scopes along and returns
 * a token result.
 */
public class DeviceCodeFlowTokenResultCommand extends TokenCommand{
    private static final String TAG = DeviceCodeFlowTokenResultCommand.class.getSimpleName();

    private final AuthorizationResult mAuthorizationResult;

    public DeviceCodeFlowTokenResultCommand(@NonNull DeviceCodeFlowCommandParameters parameters,
                                @NonNull AuthorizationResult authorizationResult,
                                @NonNull BaseController controller,
                                @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                                @NonNull String publicApiId) {
        super(parameters, controller, callback, publicApiId);

        mAuthorizationResult = authorizationResult;
    }

    @Override
    public AcquireTokenResult execute() throws Exception {
        final String methodTag = TAG + ":execute";
        Logger.verbose(
                methodTag,
                "DCFTokenFetchCommand initiating..."
        );

        // Get the controller used to execute the command
        final BaseController controller = getDefaultController();

        // Fetch the parameters
        final DeviceCodeFlowCommandParameters commandParameters = (DeviceCodeFlowCommandParameters) getParameters();

        // Call acquireDeviceCodeFlowToken to get token result (Part 2 of DCF)
        final AcquireTokenResult tokenResult = controller.acquireDeviceCodeFlowToken(mAuthorizationResult, commandParameters);

        Logger.verbose(
                methodTag,
                "DCFTokenFetchCommand exiting with token..."
        );

        return tokenResult;
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return true;
    }
}
