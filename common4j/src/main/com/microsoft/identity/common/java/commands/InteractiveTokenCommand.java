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
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import java.util.List;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
public class InteractiveTokenCommand extends TokenCommand {
    private static final String TAG = InteractiveTokenCommand.class.getSimpleName();

    public InteractiveTokenCommand(@NonNull final InteractiveTokenCommandParameters parameters,
                                   @NonNull final BaseController controller,
                                   @SuppressWarnings(WarningType.rawtype_warning) @NonNull final CommandCallback callback,
                                   @NonNull final String publicApiId) {
        super(parameters, controller, callback, publicApiId);
    }

    public InteractiveTokenCommand(@NonNull InteractiveTokenCommandParameters parameters,
                                   @NonNull List<BaseController> controllers,
                                   @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                                   @NonNull final String publicApiId) {
        super(parameters, controllers, callback, publicApiId);
    }

    @Override
    public AcquireTokenResult execute() throws Exception {
        final String methodName = ":execute";

        final Span span = SpanExtension.current();

        span.setAttribute(AttributeName.application_name.name(), getParameters().getApplicationName());
        span.setAttribute(AttributeName.public_api_id.name(), getPublicApiId());

        try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
            if (getParameters() instanceof InteractiveTokenCommandParameters) {
                Logger.info(
                        TAG + methodName,
                        "Executing interactive token command..."
                );

                final BaseController controller = getDefaultController();

                span.setAttribute(AttributeName.controller_name.name(), controller.getClass().getSimpleName());

                final AcquireTokenResult result = controller.acquireToken((InteractiveTokenCommandParameters) getParameters());

                if (result == null) {
                    span.setStatus(StatusCode.ERROR, "empty result");
                } else if (result.getSucceeded()) {
                    span.setStatus(StatusCode.OK);
                } else {
                    final BaseException exception = ExceptionAdapter.exceptionFromAcquireTokenResult(result, getParameters());
                    if (exception != null) {
                        span.recordException(exception);
                        span.setStatus(StatusCode.ERROR);
                    } else {
                        span.setStatus(StatusCode.ERROR, "empty exception");
                    }
                }

                return result;
            } else {
                throw new IllegalArgumentException("Invalid operation parameters");
            }
        } catch (final Throwable throwable) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(throwable);
            throw throwable;
        }
    }

    public void onFinishAuthorizationSession(int requestCode,
                                             int resultCode,
                                             @NonNull final PropertyBag data) {
        getDefaultController().onFinishAuthorizationSession(requestCode, resultCode, data);
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return true;
    }
}
