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
 * This command is used in the Device Code Flow (DCF) protocol to fetch DCF code.
 * Takes in a parameters object containing the  desired access scopes along and returns
 * authorization result.
 */
public class DeviceCodeFlowAuthResultCommand extends BaseCommand<AuthorizationResult> {
    private static final String TAG = DeviceCodeFlowAuthResultCommand.class.getSimpleName();

    public DeviceCodeFlowAuthResultCommand(@NonNull DeviceCodeFlowCommandParameters parameters,
                                 @NonNull BaseController controller,
                                 @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                                 @NonNull String publicApiId) {
        super(parameters, controller, callback, publicApiId);
    }

    @Override
    public AuthorizationResult execute() throws Exception {
        final String methodTag = TAG + ":execute";
        Logger.verbose(
                methodTag,
                "Device Code Flow command initiating..."
        );

        final Span span = OTelUtility.createSpanFromParent(
                SpanName.AcquireTokenDcfAuthRequest.name(), getParameters().getSpanContext()
        );
        span.setAttribute(AttributeName.application_name.name(), getParameters().getApplicationName());
        span.setAttribute(AttributeName.public_api_id.name(), getPublicApiId());

        try (final Scope scope = span.makeCurrent()) {
            // Get the controller used to execute the command
            final BaseController controller = getDefaultController();

            span.setAttribute(AttributeName.controller_name.name(), controller.getClass().getSimpleName());

            // Fetch the parameters
            final DeviceCodeFlowCommandParameters commandParameters = (DeviceCodeFlowCommandParameters) getParameters();

            // Call deviceCodeFlowAuthRequest to get authorization result (Part 1 of DCF)
            @SuppressWarnings(WarningType.rawtype_warning) final AuthorizationResult authorizationResult = controller.deviceCodeFlowAuthRequest(commandParameters);

            if (authorizationResult == null) {
                span.setStatus(StatusCode.ERROR, "empty result");
            } else if (authorizationResult.getSuccess()) {
                span.setStatus(StatusCode.OK);
            } else {
                AcquireTokenResult acquireTokenResult = new AcquireTokenResult();
                acquireTokenResult.setAuthorizationResult(authorizationResult);
                final BaseException exception = ExceptionAdapter.exceptionFromAcquireTokenResult(acquireTokenResult, getParameters());
                if (exception != null) {
                    span.recordException(exception);
                    span.setStatus(StatusCode.ERROR);
                } else {
                    span.setStatus(StatusCode.ERROR, "empty exception");
                }
            }

            return authorizationResult;
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
