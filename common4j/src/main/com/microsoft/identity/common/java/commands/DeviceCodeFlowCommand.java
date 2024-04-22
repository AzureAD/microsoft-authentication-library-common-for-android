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
import com.microsoft.identity.common.java.controllers.IControllerFactory;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OTelUtility;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanName;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsAuthorizationResponse;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.result.AcquireTokenResult;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

/**
 * This command is used to execute the device code flow protocol.
 * Takes in a parameters object containing the  desired access scopes along and returns
 * a token result.
 * Class also includes some pre-defined error codes and messages to be used in
 * exception handling.
 */
public class DeviceCodeFlowCommand extends TokenCommand {
    private static final String TAG = DeviceCodeFlowCommand.class.getSimpleName();

    public DeviceCodeFlowCommand(@NonNull final DeviceCodeFlowCommandParameters parameters,
                                 @NonNull final IControllerFactory controllerFactory,
                                 @SuppressWarnings(WarningType.rawtype_warning) @NonNull final DeviceCodeFlowCommandCallback callback,
                                 @NonNull final String publicApiId) {
        super(parameters, controllerFactory, callback, publicApiId);
    }

    @Override
    public AcquireTokenResult execute() throws Exception {
        final String methodTag = TAG + ":execute";
        Logger.verbose(
                methodTag,
                "Device Code Flow command initiating..."
        );

        final Span span = OTelUtility.createSpanFromParent(
                SpanName.AcquireTokenDcf.name(), getParameters().getSpanContext()
        );
        span.setAttribute(AttributeName.correlation_id.name(), getParameters().getCorrelationId());
        span.setAttribute(AttributeName.application_name.name(), getParameters().getApplicationName());
        span.setAttribute(AttributeName.public_api_id.name(), getPublicApiId());

        try (final Scope scope = SpanExtension.makeCurrentSpan(span)) {
            // Get the controller used to execute the command
            final BaseController controller = getControllerFactory().getDefaultController();

            span.setAttribute(AttributeName.controller_name.name(), controller.getClass().getSimpleName());

            // Fetch the parameters
            final DeviceCodeFlowCommandParameters commandParameters = (DeviceCodeFlowCommandParameters) getParameters();

            // Call deviceCodeFlowAuthRequest to get authorization result (Part 1 of DCF)
            @SuppressWarnings(WarningType.rawtype_warning) final AuthorizationResult authorizationResult = controller.deviceCodeFlowAuthRequest(commandParameters);

            // Fetch the authorization response
            final MicrosoftStsAuthorizationResponse authorizationResponse =
                    (MicrosoftStsAuthorizationResponse) authorizationResult.getAuthorizationResponse();

            final Date expiredDate = new Date();
            try {
                long expiredInInMilliseconds = TimeUnit.SECONDS.toMillis(Long.parseLong(authorizationResponse.getExpiresIn()));
                expiredDate.setTime(expiredDate.getTime() + expiredInInMilliseconds);
            } catch (final NumberFormatException e) {
                // Shouldn't happen, but if it does, we don't want to fail the request because of this.
                Logger.error(methodTag, "Failed to parse authorizationResponse.getExpiresIn()", e);
            }

            // Communicate with user app and provide authentication information
            @SuppressWarnings(WarningType.rawtype_warning) final DeviceCodeFlowCommandCallback deviceCodeFlowCommandCallback = (DeviceCodeFlowCommandCallback) getCallback();
            deviceCodeFlowCommandCallback.onUserCodeReceived(
                    authorizationResponse.getVerificationUri(),
                    authorizationResponse.getUserCode(),
                    authorizationResponse.getMessage(),
                    expiredDate
            );

            // Call acquireDeviceCodeFlowToken to get token result (Part 2 of DCF)
            final AcquireTokenResult tokenResult = controller.acquireDeviceCodeFlowToken(authorizationResult, commandParameters);

            Logger.verbose(
                    methodTag,
                    "Device Code Flow command exiting with token..."
            );

            if (tokenResult == null) {
                span.setStatus(StatusCode.ERROR, "empty result");
            } else if (tokenResult.getSucceeded()) {
                span.setStatus(StatusCode.OK);
            } else {
                final BaseException exception = ExceptionAdapter.exceptionFromAcquireTokenResult(tokenResult, getParameters());
                if (!(exception.getErrorCode().equals(ErrorStrings.DEVICE_CODE_FLOW_AUTHORIZATION_PENDING_ERROR_CODE))) {
                    if (exception != null) {
                        span.recordException(exception);
                        span.setStatus(StatusCode.ERROR);
                    } else {
                        span.setStatus(StatusCode.ERROR, "empty exception");
                    }
                }
            }

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
