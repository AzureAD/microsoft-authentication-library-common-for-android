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
package com.microsoft.identity.common.internal.controllers;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.util.Pair;

import androidx.annotation.NonNull;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.UserCancelException;
import com.microsoft.identity.common.internal.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.providers.oauth2.AuthorizationActivity;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.Telemetry;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class CommandDispatcher {

    private static final String TAG = CommandDispatcher.class.getSimpleName();

    private static final ExecutorService sInteractiveExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService sSilentExecutor = Executors.newCachedThreadPool();
    private static final Object sLock = new Object();
    private static InteractiveTokenCommand sCommand = null;

    /**
     * submitSilent - Run a command using the silent thread pool
     *
     * @param command
     */
    public static void submitSilent(@NonNull final BaseCommand command) {
        final String methodName = ":submitSilent";
        Logger.verbose(
                TAG + methodName,
                "Beginning execution of silent command."
        );
        sSilentExecutor.execute(new Runnable() {
            @Override
            public void run() {
                final String correlationId = initializeDiagnosticContext();
                EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                Object result = null;
                BaseException baseException = null;
                Handler handler = new Handler(Looper.getMainLooper());

                if (command.getParameters() instanceof AcquireTokenSilentOperationParameters) {
                    logSilentRequestParams(methodName, (AcquireTokenSilentOperationParameters) command.getParameters());
                    EstsTelemetry.getInstance().emitForceRefresh(command.getParameters().getForceRefresh());
                }

                try {
                    //Try executing request
                    result = command.execute();
                } catch (final Exception e) {
                    //Capture any resulting exception and map to MsalException type
                    Logger.errorPII(
                            TAG + methodName,
                            "Silent request failed with Exception",
                            e
                    );
                    if (e instanceof BaseException) {
                        baseException = (BaseException) e;
                    } else {
                        baseException = ExceptionAdapter.baseExceptionFromException(e);
                    }
                }

                if (baseException != null) {
                    //Post On Error
                    final BaseException finalException = baseException;
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            EstsTelemetry.getInstance().flush(correlationId, finalException);
                            command.getCallback().onError(finalException);
                        }
                    });
                } else {
                    if (result != null && result instanceof AcquireTokenResult) {
                        //Handler handler, final BaseCommand command, BaseException baseException, AcquireTokenResult result
                        processTokenResult(handler, command, baseException, (AcquireTokenResult) result);
                    } else {
                        //For commands that don't return an AcquireTokenResult
                        final Object returnResult = result;

                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                EstsTelemetry.getInstance().flush(correlationId);
                                command.getCallback().onTaskCompleted(returnResult);
                            }
                        });
                    }
                }

                Telemetry.getInstance().flush(correlationId);
            }
        });
    }

    /**
     * We need to inspect the AcquireTokenResult type to determine whether the request was successful, cancelled or encountered an exception
     *
     * @param handler
     * @param command
     * @param baseException
     * @param result
     */
    private static void processTokenResult(Handler handler, final BaseCommand command, BaseException baseException, AcquireTokenResult result) {
        //Token Commands
        if (result.getSucceeded()) {
            final ILocalAuthenticationResult authenticationResult = result.getLocalAuthenticationResult();
            handler.post(new Runnable() {
                @Override
                public void run() {
                    EstsTelemetry.getInstance().flush();
                    command.getCallback().onTaskCompleted(authenticationResult);
                }
            });
        } else {
            //Get MsalException from Authorization and/or Token Error Response
            baseException = ExceptionAdapter.exceptionFromAcquireTokenResult(result);
            final BaseException finalException = baseException;

            if (finalException instanceof UserCancelException) {
                //Post Cancel
                EstsTelemetry.getInstance().flush(finalException);
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        command.getCallback().onCancel();
                    }
                });
            } else {
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        EstsTelemetry.getInstance().flush(finalException);
                        command.getCallback().onError(finalException);
                    }
                });
            }
        }
    }

    public static void beginInteractive(final InteractiveTokenCommand command) {
        final String methodName = ":beginInteractive";
        Logger.info(
                TAG + methodName,
                "Beginning interactive request"
        );
        synchronized (sLock) {
            // Send a broadcast to cancel if any active auth request is present.
            command.getParameters().getAppContext().sendBroadcast(
                    new Intent(AuthorizationActivity.CANCEL_INTERACTIVE_REQUEST_ACTION)
            );

            sInteractiveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final String correlationId = initializeDiagnosticContext();
                    EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                    if (command.getParameters() instanceof AcquireTokenOperationParameters) {
                        logInteractiveRequestParameters(methodName, (AcquireTokenOperationParameters) command.getParameters());
                    }

                    AcquireTokenResult result = null;
                    BaseException baseException = null;

                    try {
                        sCommand = command;

                        //Try executing request
                        result = command.execute();
                    } catch (Exception e) {
                        //Capture any resulting exception and map to MsalException type
                        Logger.errorPII(
                                TAG + methodName,
                                "Interactive request failed with Exception",
                                e
                        );
                        if (e instanceof BaseException) {
                            baseException = (BaseException) e;
                        } else {
                            baseException = ExceptionAdapter.baseExceptionFromException(e);
                        }
                    } finally {
                        sCommand = null;
                    }

                    Handler handler = new Handler(Looper.getMainLooper());

                    if (baseException != null) {
                        //Post On Error
                        final BaseException finalException = baseException;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                EstsTelemetry.getInstance().flush(correlationId, finalException);
                                command.getCallback().onError(finalException);
                            }
                        });
                    } else {
                        if (null != result && result.getSucceeded()) {
                            //Post Success
                            final ILocalAuthenticationResult authenticationResult = result.getLocalAuthenticationResult();
                            handler.post(new Runnable() {
                                @Override
                                public void run() {
                                    EstsTelemetry.getInstance().flush(correlationId);
                                    command.getCallback().onTaskCompleted(authenticationResult);
                                }
                            });
                        } else {
                            //Get MsalException from Authorization and/or Token Error Response
                            baseException = ExceptionAdapter.exceptionFromAcquireTokenResult(result);
                            final BaseException finalException = baseException;
                            if (finalException instanceof UserCancelException) {
                                //Post Cancel
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        EstsTelemetry.getInstance().flush(correlationId, finalException);
                                        command.getCallback().onCancel();
                                    }
                                });
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        EstsTelemetry.getInstance().flush(correlationId, finalException);
                                        command.getCallback().onError(finalException);
                                    }
                                });
                            }
                        }
                    }

                    Telemetry.getInstance().flush(correlationId);
                }
            });
        }
    }

    private static void logInteractiveRequestParameters(final String methodName,
                                                        final AcquireTokenOperationParameters params) {
        Logger.info(
                TAG + methodName,
                "Requested "
                        + params.getScopes().size()
                        + " scopes"
        );

        Logger.infoPII(
                TAG + methodName,
                "----\nRequested scopes:"
        );
        for (final String scope : params.getScopes()) {
            Logger.infoPII(
                    TAG + methodName,
                    "\t" + scope
            );
        }
        Logger.infoPII(
                TAG + methodName,
                "----"
        );
        Logger.infoPII(
                TAG + methodName,
                "ClientId: [" + params.getClientId() + "]"
        );
        Logger.infoPII(
                TAG + methodName,
                "RedirectUri: [" + params.getRedirectUri() + "]"
        );
        Logger.infoPII(
                TAG + methodName,
                "Login hint: [" + params.getLoginHint() + "]"
        );

        if (null != params.getExtraQueryStringParameters()) {
            Logger.infoPII(
                    TAG + methodName,
                    "Extra query params:"
            );
            for (final Pair<String, String> qp : params.getExtraQueryStringParameters()) {
                Logger.infoPII(
                        TAG + methodName,
                        "\t\"" + qp.first + "\":\"" + qp.second + "\""
                );
            }
        }

        if (null != params.getExtraScopesToConsent()) {
            Logger.infoPII(
                    TAG + methodName,
                    "Extra scopes to consent:"
            );
            for (final String extraScope : params.getExtraScopesToConsent()) {
                Logger.infoPII(
                        TAG + methodName,
                        "\t" + extraScope
                );
            }
        }

        Logger.info(
                TAG + methodName,
                "Using authorization agent: " + params.getAuthorizationAgent().toString()
        );

        if (null != params.getAccount()) {
            Logger.infoPII(
                    TAG + methodName,
                    "Using account: " + params.getAccount().getHomeAccountId()
            );
        }
    }

    private static void logSilentRequestParams(final String methodName,
                                               final AcquireTokenSilentOperationParameters parameters) {
        Logger.infoPII(
                TAG + methodName,
                "ClientId: [" + parameters.getClientId() + "]"
        );
        Logger.infoPII(
                TAG + methodName,
                "----\nRequested scopes:"
        );

        for (final String scope : parameters.getScopes()) {
            Logger.infoPII(
                    TAG + methodName,
                    "\t" + scope
            );
        }
        Logger.infoPII(
                TAG + methodName,
                "----"
        );

        if (null != parameters.getAccount()) {
            Logger.infoPII(
                    TAG + methodName,
                    "Using account: " + parameters.getAccount().getHomeAccountId()
            );
        }

        Logger.info(
                TAG + methodName,
                "Force refresh? [" + parameters.getForceRefresh() + "]"
        );
    }

    public static void completeInteractive(int requestCode, int resultCode, final Intent data) {
        final String methodName = ":completeInteractive";
        if (sCommand != null) {
            sCommand.notify(requestCode, resultCode, data);
        } else {
            Logger.warn(TAG + methodName, "sCommand is null, No interactive call in progress to complete.");
        }
    }

    public static String initializeDiagnosticContext() {
        final String methodName = ":initializeDiagnosticContext";
        final String correlationId = UUID.randomUUID().toString();
        final com.microsoft.identity.common.internal.logging.RequestContext rc =
                new com.microsoft.identity.common.internal.logging.RequestContext();
        rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
        DiagnosticContext.setRequestContext(rc);
        Logger.verbose(
                TAG + methodName,
                "Initialized new DiagnosticContext"
        );

        return correlationId;
    }
}
