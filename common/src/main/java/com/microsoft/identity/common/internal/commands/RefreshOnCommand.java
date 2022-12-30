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

import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.result.VoidResult;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.logging.Logger;
import com.microsoft.identity.common.java.commands.BaseCommand;

import java.io.IOException;

public class RefreshOnCommand extends BaseCommand<VoidResult>{

    private static final String TAG = RefreshOnCommand.class.getSimpleName();

    public RefreshOnCommand(@NonNull CommandParameters parameters, @NonNull BaseController controller, @NonNull String publicApiId) {
        super(parameters, controller, new RefreshOnCallback(), publicApiId);
    }

    @Override
    public VoidResult execute() throws IOException, ClientException, ServiceException {
        final String methodTag = TAG + ":execute";

        final BaseController controller = getDefaultController();
        Logger.verbose(
                methodTag,
                "Executing with controller: "
                        + controller.getClass().getSimpleName()
        );
        final SilentTokenCommandParameters commandParameters = (SilentTokenCommandParameters) getParameters();
        final TokenResult result = controller.renewAccessToken(commandParameters);

        if(!result.getSuccess()) {
            Log.e(TAG, result.getErrorResponse().getError());
        }

        return new VoidResult();
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return false;
    }

    @Override
    public boolean isEligibleForCaching() {
        return false;
    }
}
