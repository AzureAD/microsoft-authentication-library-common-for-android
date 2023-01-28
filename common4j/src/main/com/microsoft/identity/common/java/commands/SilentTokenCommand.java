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
import com.microsoft.identity.common.java.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.java.constants.OAuth2ErrorCode;
import com.microsoft.identity.common.java.controllers.BaseController;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.UiRequiredException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import java.util.List;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@EqualsAndHashCode(callSuper = true)
public class SilentTokenCommand extends TokenCommand {

    public static final int ACQUIRE_TOKEN_SILENT_DEFAULT_TIMEOUT_MILLISECONDS = 30000;

    private static final String TAG = SilentTokenCommand.class.getSimpleName();

    public SilentTokenCommand(@NonNull SilentTokenCommandParameters parameters,
                              @NonNull BaseController controller,
                              @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                              @NonNull String publicApiId) {
        super(parameters, controller, callback, publicApiId);
    }

    public SilentTokenCommand(@NonNull SilentTokenCommandParameters parameters,
                              @NonNull List<BaseController> controllers,
                              @SuppressWarnings(WarningType.rawtype_warning) @NonNull CommandCallback callback,
                              @NonNull String publicApiId) {
        super(parameters, controllers, callback, publicApiId);
    }

    @Override
    public AcquireTokenResult execute() throws Exception {
        AcquireTokenResult result = null;
        final String methodName = ":execute";

        final Span span = OTelUtility.createSpanFromParent(
                SpanName.AcquireTokenSilent.name(), getParameters().getSpanContext()
        );
        span.setAttribute(AttributeName.application_name.name(), getParameters().getApplicationName());
        span.setAttribute(AttributeName.public_api_id.name(), getPublicApiId());

        try (final Scope scope = span.makeCurrent()) {
            for (int ii = 0; ii < this.getControllers().size(); ii++) {
                final BaseController controller = this.getControllers().get(ii);

                span.setAttribute(AttributeName.controller_name.name(), controller.getClass().getSimpleName());

                try {
                    Logger.verbose(
                            TAG + methodName,
                            "Executing with controller: "
                                    + controller.getClass().getSimpleName()
                    );

                    result = controller.acquireTokenSilent(
                            (SilentTokenCommandParameters) getParameters()
                    );

                    if (result.getSucceeded()) {
                        Logger.verbose(
                                TAG + methodName,
                                "Executing with controller: "
                                        + controller.getClass().getSimpleName()
                                        + ": Succeeded"
                        );

                        span.setAttribute(
                                AttributeName.is_serviced_from_cache.name(),
                                result.getLocalAuthenticationResult().isServicedFromCache()
                        );

                        return result;
                    }
                } catch (UiRequiredException | ClientException e) {
                    if (e.getErrorCode().equals(OAuth2ErrorCode.INVALID_GRANT) // was invalid_grant
                            && this.getControllers().size() > ii + 1) { // isn't the last controller we can try
                        continue;
                    } else if ((e.getErrorCode().equals(ErrorStrings.NO_TOKENS_FOUND)
                            || e.getErrorCode().equals(ErrorStrings.NO_ACCOUNT_FOUND))
                            && this.getControllers().size() > ii + 1) {
                        //if no token or account for this silent call, we should continue to the next silent call.
                        continue;
                    } else {
                        throw e;
                    }
                }
            }

            return result;
        } catch (final Throwable throwable) {
            span.setStatus(StatusCode.ERROR);
            span.recordException(throwable);
            throw throwable;
        } finally {
            span.end();
        }
    }

    @Override
    public boolean isEligibleForCaching() {
        return true;
    }

    @Override
    public boolean isEligibleForEstsTelemetry() {
        return true;
    }

}
