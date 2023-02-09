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
import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

/**
 * This command is used in the Device Code Flow (DCF) protocol to acquire token.
 * Takes in a parameters object containing the  desired access scopes along with authorizationResult {@link AuthorizationResult}
 * and returns a token result.
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
                "DeviceCodeFlowTokenResultCommand initiating..."
        );

        final Span span = OTelUtility.createSpanFromParent(
                SpanName.AcquireTokenDcf.name(), getParameters().getSpanContext()
        );
        span.setAttribute(AttributeName.application_name.name(), getParameters().getApplicationName());
        span.setAttribute(AttributeName.public_api_id.name(), getPublicApiId());

        try (final Scope scope = span.makeCurrent()) {
            // Get the controller used to execute the command
            final BaseController controller = getDefaultController();

            // Fetch the parameters
            final DeviceCodeFlowCommandParameters commandParameters = (DeviceCodeFlowCommandParameters) getParameters();

            // Call acquireDeviceCodeFlowToken to get token result (Part 2 of DCF)
            final AcquireTokenResult tokenResult = controller.acquireDeviceCodeFlowToken(mAuthorizationResult, commandParameters);

            if (tokenResult == null) {
                span.setStatus(StatusCode.ERROR, "empty result");
            } else if (tokenResult.getSucceeded()) {
                span.setStatus(StatusCode.OK);
            } else {
                final BaseException exception = ExceptionAdapter.exceptionFromAcquireTokenResult(tokenResult, getParameters());
                if (exception != null && !(exception.getErrorCode().equals(ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_ERROR_CODE))) {
                    span.recordException(exception);
                    span.setStatus(StatusCode.ERROR);
                } else {
                    span.setStatus(StatusCode.ERROR, "empty exception");
                }
            }

            Logger.verbose(
                    methodTag,
                    "DeviceCodeFlowTokenResultCommand exiting with token..."
            );

            return tokenResult;
        } catch (final Throwable throwable) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(throwable);
            throw throwable;
        } finally {
            span.end();
        }
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return true;
    }
}
