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

import android.content.Intent;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.internal.commands.parameters.DeviceCodeFlowCommandParameters;
import com.microsoft.identity.common.internal.controllers.BaseController;
import com.microsoft.identity.common.internal.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;

/**
 * This command is used to execute the device code flow protocol.
 * Takes in a parameters object containing the  desired access scopes along and returns
 * a token result.
 * Class also includes some pre-defined error codes and messages to be used in
 * exception handling.
 */
public class DeviceCodeFlowCommand extends TokenCommand {

    public DeviceCodeFlowCommand(@NonNull DeviceCodeFlowCommandParameters parameters,
                                 @NonNull BaseController controller,
                                 @NonNull DeviceCodeFlowCommandCallback callback,
                                 @NonNull String publicApiId) {
        super(parameters, controller, callback, publicApiId);
    }

    @Override
    public AcquireTokenResult execute() throws Exception {

        // Get the controller used to execute the command
        final BaseController controller = getDefaultController();

        // Fetch the parameters
        final DeviceCodeFlowCommandParameters commandParameters = (DeviceCodeFlowCommandParameters) getParameters();

        // Call deviceCodeFlowAuthRequest to get authorization result (Part 1 of DCF)
        final AuthorizationResult authorizationResult = controller.deviceCodeFlowAuthRequest(commandParameters);

        // Fetch the authorization response
        final MicrosoftStsAuthorizationResponse authorizationResponse =
                (MicrosoftStsAuthorizationResponse) authorizationResult.getAuthorizationResponse();

        // Communicate with user app and provide authentication information
        final DeviceCodeFlowCommandCallback deviceCodeFlowCommandCallback = (DeviceCodeFlowCommandCallback) getCallback();
        deviceCodeFlowCommandCallback.onUserCodeReceived(
                authorizationResponse.getVerificationUri(),
                authorizationResponse.getUserCode(),
                authorizationResponse.getMessage()
        );

        // Call acquireDeviceCodeFlowToken to get token result (Part 2 of DCF)
        return controller.acquireDeviceCodeFlowToken(authorizationResult, commandParameters);
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return true;
    }

    @Override
    void notify(int requestCode, int resultCode, Intent data) {
        getDefaultController().completeAcquireToken(requestCode, resultCode, data);
    }
}
