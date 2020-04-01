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

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.exception.UserCancelException;
import com.microsoft.identity.common.internal.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.internal.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.AcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.request.AcquireTokenSilentOperationParameters;
import com.microsoft.identity.common.internal.request.BrokerAcquireTokenOperationParameters;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.Telemetry;

import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.CANCEL_INTERACTIVE_REQUEST;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.RETURN_INTERACTIVE_REQUEST_RESULT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.RESULT_CODE;

public class CommandDispatcher {

    private static final String TAG = CommandDispatcher.class.getSimpleName();

    private static final ExecutorService sInteractiveExecutor = Executors.newSingleThreadExecutor();
    private static final ExecutorService sSilentExecutor = Executors.newCachedThreadPool();
    private static final Object sLock = new Object();
    private static InteractiveTokenCommand sCommand = null;
    private static final CommandResultCache sCommandResultCache = new CommandResultCache();

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
                final String correlationId = initializeDiagnosticContext(command.getParameters().getCorrelationId());

                // set correlation id on parameters as it may not already be set
                command.getParameters().setCorrelationId(correlationId);

                EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                CommandResult commandResult = null;
                Handler handler = new Handler(Looper.getMainLooper());

                //Log operation parameters
                if (command.getParameters() instanceof AcquireTokenSilentOperationParameters) {
                    logSilentRequestParams(methodName, (AcquireTokenSilentOperationParameters) command.getParameters());
                    EstsTelemetry.getInstance().emitForceRefresh(command.getParameters().getForceRefresh());
                }

                //Check cache to see if the same command completed in the last 30 seconds
                commandResult = sCommandResultCache.get(command);

                //If nothing in cache, execute the command and cache the result
                if (commandResult == null) {
                    commandResult = executeCommand(command);
                    cacheCommandResult(command, commandResult);
                } else {
                    Logger.info(
                            TAG + methodName,
                            "Silent command result returned from cache."
                    );
                }

                //Return the result via the callback
                returnCommandResult(command, commandResult, handler);

                Telemetry.getInstance().flush(correlationId);

                if (commandResult.getResult() instanceof BaseException) {
                    EstsTelemetry.getInstance().flush(correlationId, (BaseException) commandResult.getResult());
                } else {
                    EstsTelemetry.getInstance().flush(correlationId);
                }
            }
        });
    }

    static void clearCommandCache() {
        sCommandResultCache.clear();
    }

    /**
     * We need to inspect the AcquireTokenResult type to determine whether the request was successful, cancelled or encountered an exception
     * <p>
     * Execute the command provided to the command dispatcher
     *
     * @param command
     * @return
     */
    private static CommandResult executeCommand(BaseCommand command) {

        Object result = null;
        BaseException baseException = null;
        CommandResult commandResult;

        try {
            //Try executing request
            result = command.execute();
        } catch (final Exception e) {
            if (e instanceof BaseException) {
                baseException = (BaseException) e;
            } else {
                baseException = ExceptionAdapter.baseExceptionFromException(e);
            }
        }

        if (baseException != null) {
            //Post On Error
            commandResult = new CommandResult(CommandResult.ResultStatus.ERROR, baseException);
        } else {
            if (result != null && result instanceof AcquireTokenResult) {
                //Handler handler, final BaseCommand command, BaseException baseException, AcquireTokenResult result
                commandResult = getCommandResultFromTokenResult(baseException, (AcquireTokenResult) result);
            } else {
                //For commands that don't return an AcquireTokenResult
                commandResult = new CommandResult(CommandResult.ResultStatus.COMPLETED, result);
            }
        }

        return commandResult;

    }

    /**
     * Return the result of the command to the caller via the callback associated with the command
     *
     * @param command
     * @param result
     * @param handler
     */
    private static void returnCommandResult(final BaseCommand command, final CommandResult result, Handler handler) {
        handler.post(new Runnable() {
            @Override
            public void run() {
                switch (result.getStatus()) {
                    case ERROR:
                        command.getCallback().onError(result.getResult());
                        break;
                    case COMPLETED:
                        command.getCallback().onTaskCompleted(result.getResult());
                        break;
                    case CANCEL:
                        command.getCallback().onCancel();
                    default:

                }
            }
        });
    }

    /**
     * Cache the result of the command (if eligible to do so) in order to protect the service from clients
     * making the requests in a tight loop
     *
     * @param command
     * @param commandResult
     */
    private static void cacheCommandResult(BaseCommand command, CommandResult commandResult) {
        if (command.isEligibleForCaching() && eligibleToCache(commandResult)) {
            sCommandResultCache.put(command, commandResult);
        }
    }

    /**
     * Determine if the command result should be cached
     *
     * @param commandResult
     * @return
     */
    private static boolean eligibleToCache(CommandResult commandResult) {
        switch (commandResult.getStatus()) {
            case ERROR:
                return eligibleToCacheException((BaseException) commandResult.getResult());
            case COMPLETED:
                return true;
            default:
                return false;
        }
    }

    /**
     * Determine if the exception type is eligible to be cached
     *
     * @param exception
     * @return
     */
    private static boolean eligibleToCacheException(BaseException exception) {
        if (exception instanceof IntuneAppProtectionPolicyRequiredException) {
            return false;
        }
        return true;
    }


    /**
     * Get Commandresult from acquiretokenresult
     *
     * @param baseException
     * @param result
     */
    private static CommandResult getCommandResultFromTokenResult(BaseException baseException, AcquireTokenResult result) {
        //Token Commands
        if (result.getSucceeded()) {
            return new CommandResult(CommandResult.ResultStatus.COMPLETED, result.getLocalAuthenticationResult());
        } else {
            //Get MsalException from Authorization and/or Token Error Response
            baseException = ExceptionAdapter.exceptionFromAcquireTokenResult(result);
            if (baseException instanceof UserCancelException) {
                return new CommandResult(CommandResult.ResultStatus.CANCEL, null);
            } else {
                return new CommandResult(CommandResult.ResultStatus.ERROR, baseException);
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
            final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(command.getParameters().getAppContext());

            // only send broadcast to cancel if within broker
            if (command.getParameters() instanceof BrokerAcquireTokenOperationParameters) {
                // Send a broadcast to cancel if any active auth request is present.
                localBroadcastManager.sendBroadcast(
                        new Intent(CANCEL_INTERACTIVE_REQUEST)
                );
            }

            sInteractiveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final String correlationId = initializeDiagnosticContext(
                            command.getParameters().getCorrelationId()
                    );

                    // set correlation id on parameters as it may not already be set
                    command.getParameters().setCorrelationId(correlationId);

                    EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                    if (command.getParameters() instanceof AcquireTokenOperationParameters) {
                        logInteractiveRequestParameters(methodName, (AcquireTokenOperationParameters) command.getParameters());
                    }

                    final BroadcastReceiver resultReceiver = new BroadcastReceiver() {
                        @Override
                        public void onReceive(Context context, Intent intent) {
                            completeInteractive(intent);
                        }
                    };

                    AcquireTokenResult result = null;
                    BaseException baseException = null;

                    try {
                        localBroadcastManager.registerReceiver(
                                resultReceiver,
                                new IntentFilter(RETURN_INTERACTIVE_REQUEST_RESULT));

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
                        localBroadcastManager.unregisterReceiver(resultReceiver);
                    }

                    Handler handler = new Handler(Looper.getMainLooper());

                    if (baseException != null) {
                        //Post On Error
                        final BaseException finalException = baseException;
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
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
                                        command.getCallback().onCancel();
                                    }
                                });
                            } else {
                                handler.post(new Runnable() {
                                    @Override
                                    public void run() {
                                        command.getCallback().onError(finalException);
                                    }
                                });
                            }
                        }
                    }

                    EstsTelemetry.getInstance().flush(correlationId, baseException);
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

    private static void completeInteractive(final Intent resultIntent) {
        final String methodName = ":completeInteractive";

        int requestCode = resultIntent.getIntExtra(REQUEST_CODE, 0);
        int resultCode = resultIntent.getIntExtra(RESULT_CODE, 0);

        if (sCommand != null) {
            sCommand.notify(requestCode, resultCode, resultIntent);
        } else {
            Logger.warn(TAG + methodName, "sCommand is null, No interactive call in progress to complete.");
        }
    }

    public static String initializeDiagnosticContext(@Nullable final String requestCorrelationId) {
        final String methodName = ":initializeDiagnosticContext";

        final String correlationId = TextUtils.isEmpty(requestCorrelationId) ?
                UUID.randomUUID().toString() :
                requestCorrelationId;

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

    public static int getCachedResultCount() {
        return sCommandResultCache.getSize();
    }

}
