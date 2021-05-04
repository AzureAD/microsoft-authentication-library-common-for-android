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

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import com.microsoft.identity.common.WarningType;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.exception.BaseException;
import com.microsoft.identity.common.exception.BrokerCommunicationException;
import com.microsoft.identity.common.exception.ClientException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.IntuneAppProtectionPolicyRequiredException;
import com.microsoft.identity.common.exception.UserCancelException;
import com.microsoft.identity.common.internal.commands.BaseCommand;
import com.microsoft.identity.common.internal.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.internal.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.CommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.InteractiveTokenCommandParameters;
import com.microsoft.identity.common.internal.commands.parameters.SilentTokenCommandParameters;
import com.microsoft.identity.common.internal.eststelemetry.EstsTelemetry;
import com.microsoft.identity.common.java.util.ObjectMapper;
import com.microsoft.identity.common.logging.DiagnosticContext;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.request.SdkType;
import com.microsoft.identity.common.internal.result.AcquireTokenResult;
import com.microsoft.identity.common.internal.result.FinalizableResultFuture;
import com.microsoft.identity.common.internal.result.LocalAuthenticationResult;
import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.util.BiConsumer;
import com.microsoft.identity.common.internal.util.StringUtil;
import com.microsoft.identity.common.internal.util.ThreadUtils;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.Map;
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.CANCEL_INTERACTIVE_REQUEST;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentAction.RETURN_INTERACTIVE_REQUEST_RESULT;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.REQUEST_CODE;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AuthorizationIntentKey.RESULT_CODE;

public class CommandDispatcher {

    private static final String TAG = CommandDispatcher.class.getSimpleName();

    private static final int SILENT_REQUEST_THREAD_POOL_SIZE = 5;
    private static final int INTERACTIVE_REQUEST_THREAD_POOL_SIZE = 1;
    //TODO:1315931 - Refactor the threadpools to not be unbounded for both silent and interactive requests.
    private static final ExecutorService sInteractiveExecutor = ThreadUtils.getNamedThreadPoolExecutor(
            1, INTERACTIVE_REQUEST_THREAD_POOL_SIZE, -1, 0, TimeUnit.MINUTES, "interactive"
    );
    private static final ExecutorService sSilentExecutor = ThreadUtils.getNamedThreadPoolExecutor(
            1, SILENT_REQUEST_THREAD_POOL_SIZE, -1, 1, TimeUnit.MINUTES, "silent"
    );
    private static final Object sLock = new Object();
    private static InteractiveTokenCommand sCommand = null;
    private static final CommandResultCache sCommandResultCache = new CommandResultCache();

    private static final TreeSet<String> nonCacheableErrorCodes = new TreeSet(
            Arrays.asList(
                    ErrorStrings.DEVICE_NETWORK_NOT_AVAILABLE,
                    BrokerCommunicationException.Category.CONNECTION_ERROR.toString(),
                    ClientException.INTERRUPTED_OPERATION,
                    ClientException.IO_ERROR));

    private static final Object mapAccessLock = new Object();
    @GuardedBy("mapAccessLock")
    // Suppressing rawtype warnings due to the generic type BaseCommand
    @SuppressWarnings(WarningType.rawtype_warning)
    private static ConcurrentMap<BaseCommand, FinalizableResultFuture<CommandResult>> sExecutingCommandMap = new ConcurrentHashMap<>();

    /**
     * Remove all keys that are the command reference from the executing command map.  Since if they key has
     * been changed, remove will not work, construct a new map and add all keys that are not identically
     * that key into the new map.  <strong>MUST</strong> only be used under the mapAccessLock.
     *
     * @param command the command whose identity to use to cleanse the map.
     */
    // Suppressing rawtype warnings due to the generic type BaseCommand
    @SuppressWarnings(WarningType.rawtype_warning)
    private static void cleanMap(BaseCommand command) {
        ConcurrentMap<BaseCommand, FinalizableResultFuture<CommandResult>> newMap = new ConcurrentHashMap<>();
        for (Map.Entry<BaseCommand, FinalizableResultFuture<CommandResult>> e : sExecutingCommandMap.entrySet()) {
            if (! (command == e.getKey())) {
                newMap.put(e.getKey(), e.getValue());
            }
        }
        sExecutingCommandMap = newMap;
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static int outstandingCommands() {
        synchronized (mapAccessLock) {
            return sExecutingCommandMap.size();
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static boolean isCommandOutstanding(BaseCommand c) {
        synchronized (mapAccessLock) {
            for (Map.Entry<BaseCommand, ?> e : sExecutingCommandMap.entrySet()) {
                if (e.getKey() == c) {
                    System.out.println("Command out there " + c);
                    return true;
                }
            }
            return false;
        }
    }

    @VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void clearState() throws Exception {
        synchronized (mapAccessLock) {
            sExecutingCommandMap.clear();
        }
        sSilentExecutor.shutdownNow();
        sInteractiveExecutor.shutdownNow();
        Field f = CommandDispatcher.class.getDeclaredField("sSilentExecutor");
        f.setAccessible(true);
        f.set(null, Executors.newFixedThreadPool(SILENT_REQUEST_THREAD_POOL_SIZE));
        f.setAccessible(false);

        f = CommandDispatcher.class.getDeclaredField("sInteractiveExecutor");
        f.setAccessible(true);
        f.set(null, Executors.newSingleThreadExecutor());
        f.setAccessible(false);
    }


    /**
     * submitSilent - Run a command using the silent thread pool.
     *
     * @param command
     */
    public static void submitSilent(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command) {
        submitSilentReturningFuture(command);
    }

    /**
     * submitSilent - Run a command using the silent thread pool, and return the future governing it.
     *
     * @param command
     */
    @VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static FinalizableResultFuture<CommandResult> submitSilentReturningFuture(@SuppressWarnings(WarningType.rawtype_warning)
                                                                                         @NonNull final BaseCommand command) {

        final String methodName = ":submitSilent";

        final CommandParameters commandParameters = command.getParameters();
        final String correlationId = initializeDiagnosticContext(commandParameters.getCorrelationId(),
                commandParameters.getSdkType() == null ? SdkType.UNKNOWN.getProductName() :
                        commandParameters.getSdkType().getProductName(), commandParameters.getSdkVersion());

        // set correlation id on parameters as it may not already be set
        commandParameters.setCorrelationId(correlationId);

        logParameters(TAG + methodName, correlationId, commandParameters, command.getPublicApiId());

        final Handler handler = new Handler(Looper.getMainLooper());
        synchronized (mapAccessLock) {
            final FinalizableResultFuture<CommandResult> finalFuture;
            if (command.isEligibleForCaching()) {
                FinalizableResultFuture<CommandResult> future = sExecutingCommandMap.get(command);

                if (null == future) {
                    future = new FinalizableResultFuture<>();
                    final FinalizableResultFuture<CommandResult> putValue = sExecutingCommandMap.putIfAbsent(command, future);

                    if (null == putValue) {
                        // our value was inserted.
                        future.whenComplete(getCommandResultConsumer(command, handler));
                    } else {
                        // Our value was not inserted, grab the one that was and hang a new listener off it
                        putValue.whenComplete(getCommandResultConsumer(command, handler));
                        return putValue;
                    }
                } else {
                    future.whenComplete(getCommandResultConsumer(command, handler));
                    return future;
                }

                finalFuture = future;
            } else {
                finalFuture = new FinalizableResultFuture<>();
                finalFuture.whenComplete(getCommandResultConsumer(command, handler));
            }

            sSilentExecutor.execute(new Runnable() {
                @Override
                public void run() {

                    try {
                        //initializing again since the request is transferred to a different thread pool
                        initializeDiagnosticContext(correlationId, commandParameters.getSdkType() == null ?
                                SdkType.UNKNOWN.getProductName() : commandParameters.getSdkType().getProductName(),
                                commandParameters.getSdkVersion());

                        EstsTelemetry.getInstance().initTelemetryForCommand(command);

                        EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                        CommandResult commandResult = null;

                        //Log operation parameters
                        if (command.getParameters() instanceof SilentTokenCommandParameters) {
                            EstsTelemetry.getInstance().emitForceRefresh(((SilentTokenCommandParameters) command.getParameters()).isForceRefresh());
                        }

                        //Check cache to see if the same command completed in the last 30 seconds
                        commandResult = sCommandResultCache.get(command);
                        //If nothing in cache, execute the command and cache the result
                        if (commandResult == null) {
                            commandResult = executeCommand(command);
                            cacheCommandResult(command, commandResult);
                            Logger.info(TAG + methodName, "Completed silent request as owner for correlation id : **"
                                    + correlationId + ", with the status : " + commandResult.getStatus().getLogStatus()
                                    + " is cacheable : " + command.isEligibleForCaching());
                        } else {
                            Logger.info(
                                    TAG + methodName,
                                    "Silent command result returned from cache for correlation id : "
                                            + correlationId + " having status : " + commandResult.getStatus().getLogStatus()
                            );
                            // Added to keep the original correlation id intact, and to not let it mutate with the cascading requests hitting the cache.
                            commandResult = new CommandResult(commandResult.getStatus(),
                                    commandResult.getResult(), commandResult.getCorrelationId());
                        }
                        // TODO 1309671 : change required to stop the LocalAuthenticationResult object from mutating in cases of cached command.
                        // set correlation id on Local Authentication Result
                        setCorrelationIdOnResult(commandResult, correlationId);
                        Telemetry.getInstance().flush(correlationId);
                        EstsTelemetry.getInstance().flush(command, commandResult);
                        finalFuture.setResult(commandResult);
                    } catch (final Throwable t) {
                        Logger.info(TAG + methodName, "Request encountered an exception with correlation id : **" + correlationId);
                        finalFuture.setException(new ExecutionException(t));
                    } finally {
                        synchronized (mapAccessLock) {
                            if (command.isEligibleForCaching()) {
                                final FinalizableResultFuture mapFuture = sExecutingCommandMap.remove(command);
                                if (mapFuture == null) {
                                    // If this has happened, the command that we started with has mutated.  We will
                                    // examine every entry in the map, find the one with the same object identity
                                    // and remove it.
                                    // ADO:TODO:1153495 - Rekey this map with stable string keys.
                                    Logger.error(TAG, "The command in the map has mutated " + command.getClass().getCanonicalName()
                                            + " the calling application was " + command.getParameters().getApplicationName(), null);
                                    cleanMap(command);
                                }
                            }
                            finalFuture.setCleanedUp();
                        }
                        DiagnosticContext.clear();
                    }
                }
            });
            return finalFuture;
        }
    }

    private static void logParameters(@NonNull String tag, @NonNull String correlationId,
                                      @NonNull Object parameters, @Nullable String publicApiId) {
        final String TAG = tag + ":" + parameters.getClass().getSimpleName();

        //TODO:1315871 - conversion of PublicApiId in readable form.
        Logger.info(TAG, DiagnosticContext.getRequestContext().toJsonString(),
                "Starting request for correlation id : ##" + correlationId
                        + ", with PublicApiId : " + publicApiId);

        if (Logger.getAllowPii()) {
            Logger.infoPII(TAG, ObjectMapper.serializeObjectToJsonString(parameters));
        } else {
            Logger.info(TAG, ObjectMapper.serializeExposedFieldsOfObjectToJsonString(parameters));
        }
    }

    private static BiConsumer<CommandResult, Throwable> getCommandResultConsumer(
            @SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command,
            @NonNull final Handler handler) {

        final String methodName = ":getCommandResultConsumer";

        return new BiConsumer<CommandResult, Throwable>() {
            @Override
            public void accept(CommandResult result, final Throwable throwable) {
                if (null != throwable) {
                    Logger.info(TAG + methodName, "Request encountered an exception " +
                            "(this maybe a duplicate request which caries the exception encountered by the original request)");
                    handler.post(new Runnable() {
                        @Override
                        public void run() {
                            commandCallBackOnError(command, throwable);
                        }
                    });
                    return;
                }
                if (!StringUtil.isEmpty(result.getCorrelationId())
                        && !command.getParameters().getCorrelationId().equals(result.getCorrelationId())) {
                    Logger.info(TAG + methodName,
                            "Completed duplicate request with correlation id : **"
                                    + command.getParameters().getCorrelationId() + ", having the same result as : "
                                    + result.getCorrelationId() + ", with the status : "
                                    + result.getStatus().getLogStatus());
                }
                // Return command result will post() result for us.
                returnCommandResult(command, result, handler);
            }
        };
    }

    // Suppressing unchecked warnings due to casting of Throwable to the generic type of TaskCompletedCallbackWithError
    @SuppressWarnings(WarningType.unchecked_warning)
    private static void commandCallBackOnError(@SuppressWarnings(WarningType.rawtype_warning) @NonNull BaseCommand command, Throwable throwable) {
        command.getCallback().onError(ExceptionAdapter.baseExceptionFromException(throwable));
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
    private static CommandResult executeCommand(@SuppressWarnings(WarningType.rawtype_warning) BaseCommand command) {

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
            if (baseException instanceof UserCancelException) {
                commandResult = new CommandResult(CommandResult.ResultStatus.CANCEL, null,
                        command.getParameters().getCorrelationId());
            } else {
                //Post On Error
                commandResult = new CommandResult(CommandResult.ResultStatus.ERROR, baseException,
                        command.getParameters().getCorrelationId());
            }
        } else /* baseException == null */ {
            if (result != null && result instanceof AcquireTokenResult) {
                //Handler handler, final BaseCommand command, BaseException baseException, AcquireTokenResult result
                commandResult = getCommandResultFromTokenResult(baseException, (AcquireTokenResult) result,
                        command.getParameters().getCorrelationId());
            } else {
                //For commands that don't return an AcquireTokenResult
                commandResult = new CommandResult(CommandResult.ResultStatus.COMPLETED, result,
                        command.getParameters().getCorrelationId());
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
    private static void returnCommandResult(@SuppressWarnings(WarningType.rawtype_warning) final BaseCommand command,
                                            final CommandResult result, @NonNull final Handler handler) {

        optionallyReorderTasks(command);

        handler.post(new Runnable() {
            @Override
            public void run() {
                switch (result.getStatus()) {
                    case ERROR:
                        commandCallbackOnError(command, result);
                        break;
                    case COMPLETED:
                        commandCallbackOnTaskCompleted(command, result);
                        break;
                    case CANCEL:
                        command.getCallback().onCancel();
                        break;
                    default:

                }
            }
        });
    }


    /**
     * This method optionally re-orders tasks to bring the task that launched
     * the interactive activity to the foreground.  This is useful when the activity provided
     * to us does not have a taskAffinity and as a result it's possible that other apps or the home
     * screen could be in the task stack ahead of the app that launched the interactive
     * authorization UI.
     * @param command The BaseCommand.
     */
    private static void optionallyReorderTasks(@SuppressWarnings(WarningType.rawtype_warning)final BaseCommand command){
        final String methodName = ":optionallyReorderTasks";
        if(command instanceof InteractiveTokenCommand){
            InteractiveTokenCommand interactiveTokenCommand = (InteractiveTokenCommand)command;
            InteractiveTokenCommandParameters interactiveTokenCommandParameters = (InteractiveTokenCommandParameters)interactiveTokenCommand.getParameters();

            if(interactiveTokenCommandParameters.getHandleNullTaskAffinity() && !interactiveTokenCommand.getHasTaskAffinity()) {
                //If an interactive command doesn't have a task affinity bring the
                //task that launched the command to the foreground
                //In order for this to work the app has to have requested the re-order tasks permission
                //https://developer.android.com/reference/android/Manifest.permission#REORDER_TASKS
                //if the permission has not been granted nothing will happen if you just invoke the method
                ActivityManager activityManager = (ActivityManager) command.getParameters().getAndroidApplicationContext().getSystemService(Context.ACTIVITY_SERVICE);
                if (activityManager != null) {
                    activityManager.moveTaskToFront(interactiveTokenCommand.getTaskId(), 0);
                } else {
                    Logger.warn(TAG + methodName, "ActivityManager was null; Unable to bring task for the foreground.");
                }
            }
        }
    }

    // Suppressing unchecked warnings due to casting of the result to the generic type of TaskCompletedCallbackWithError
    @SuppressWarnings(WarningType.unchecked_warning)
    private static void commandCallbackOnError(@SuppressWarnings("rawtypes") BaseCommand command, CommandResult result) {
        command.getCallback().onError(ExceptionAdapter.baseExceptionFromException((Throwable) result.getResult()));
    }

    // Suppressing unchecked warnings due to casting of the result to the generic type of TaskCompletedCallback
    @SuppressWarnings(WarningType.unchecked_warning)
    private static void commandCallbackOnTaskCompleted(@SuppressWarnings("rawtypes") BaseCommand command, CommandResult result) {
        command.getCallback().onTaskCompleted(result.getResult());
    }

    /**
     * Cache the result of the command (if eligible to do so) in order to protect the service from clients
     * making the requests in a tight loop
     *
     * @param command
     * @param commandResult
     */
    private static void cacheCommandResult(@SuppressWarnings(WarningType.rawtype_warning) BaseCommand command,
                                           CommandResult commandResult) {
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
        final String errorCode;
        if (exception instanceof BrokerCommunicationException) {
            errorCode = ((BrokerCommunicationException) exception).getCategory().toString();
        } else {
            errorCode = exception.getErrorCode();
        }
        //TODO : ADO 1373343 Add the whole transient exception category.
        if (exception instanceof IntuneAppProtectionPolicyRequiredException
                || nonCacheableErrorCodes.contains(errorCode)) {
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
    private static CommandResult getCommandResultFromTokenResult(BaseException baseException,
                                                                 @NonNull AcquireTokenResult result, @NonNull String correlationId) {
        //Token Commands
        if (result.getSucceeded()) {
            return new CommandResult(CommandResult.ResultStatus.COMPLETED,
                    result.getLocalAuthenticationResult(), correlationId);
        } else {
            //Get MsalException from Authorization and/or Token Error Response
            baseException = ExceptionAdapter.exceptionFromAcquireTokenResult(result);
            if (baseException instanceof UserCancelException) {
                return new CommandResult(CommandResult.ResultStatus.CANCEL, null, correlationId);
            } else {
                return new CommandResult(CommandResult.ResultStatus.ERROR, baseException, correlationId);
            }
        }
    }

    public static void beginInteractive(final InteractiveTokenCommand command) {
        final String methodName = ":beginInteractive";
        synchronized (sLock) {
            final LocalBroadcastManager localBroadcastManager =
                    LocalBroadcastManager.getInstance(command.getParameters().getAndroidApplicationContext());

            // only send broadcast to cancel if within broker
            if (command.getParameters() instanceof BrokerInteractiveTokenCommandParameters) {
                // Send a broadcast to cancel if any active auth request is present.
                localBroadcastManager.sendBroadcast(
                        new Intent(CANCEL_INTERACTIVE_REQUEST)
                );
            }

            sInteractiveExecutor.execute(new Runnable() {
                @Override
                public void run() {
                    final CommandParameters commandParameters = command.getParameters();
                    final String correlationId = initializeDiagnosticContext(
                            commandParameters.getCorrelationId(),
                            commandParameters.getSdkType() == null ?
                                    SdkType.UNKNOWN.getProductName() : commandParameters.getSdkType().getProductName(),
                            commandParameters.getSdkVersion()
                    );
                    try {
                        // set correlation id on parameters as it may not already be set
                        commandParameters.setCorrelationId(correlationId);

                        logParameters(TAG + methodName, correlationId, commandParameters, command.getPublicApiId());

                        EstsTelemetry.getInstance().initTelemetryForCommand(command);

                        EstsTelemetry.getInstance().emitApiId(command.getPublicApiId());

                        final BroadcastReceiver resultReceiver = new BroadcastReceiver() {
                            @Override
                            public void onReceive(Context context, Intent intent) {
                                completeInteractive(intent);
                            }
                        };

                        CommandResult commandResult;
                        Handler handler = new Handler(Looper.getMainLooper());

                        localBroadcastManager.registerReceiver(
                                resultReceiver,
                                new IntentFilter(RETURN_INTERACTIVE_REQUEST_RESULT));

                        sCommand = command;

                        //Try executing request
                        commandResult = executeCommand(command);
                        sCommand = null;
                        localBroadcastManager.unregisterReceiver(resultReceiver);

                        // set correlation id on Local Authentication Result
                        setCorrelationIdOnResult(commandResult, correlationId);

                        Logger.info(TAG + methodName,
                                "Completed interactive request for correlation id : **" + correlationId +
                                        ", with the status : " + commandResult.getStatus().getLogStatus());

                        EstsTelemetry.getInstance().flush(command, commandResult);
                        Telemetry.getInstance().flush(correlationId);
                        returnCommandResult(command, commandResult, handler);
                    } finally {
                        DiagnosticContext.clear();
                    }
                }
            });
        }
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

    public static String initializeDiagnosticContext(@Nullable final String requestCorrelationId, final String sdkType, final String sdkVersion) {
        final String methodName = ":initializeDiagnosticContext";

        final String correlationId = TextUtils.isEmpty(requestCorrelationId) ?
                UUID.randomUUID().toString() :
                requestCorrelationId;

        final com.microsoft.identity.common.internal.logging.RequestContext rc =
                new com.microsoft.identity.common.internal.logging.RequestContext();
        rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
        rc.put(AuthenticationConstants.SdkPlatformFields.PRODUCT, sdkType);
        rc.put(AuthenticationConstants.SdkPlatformFields.VERSION, sdkVersion);
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

    private static void setCorrelationIdOnResult(@NonNull final CommandResult commandResult,
                                                 @NonNull final String correlationId) {
        // set correlation id on Local Authentication Result
        if (commandResult.getResult() != null &&
                commandResult.getResult() instanceof LocalAuthenticationResult) {
            final LocalAuthenticationResult localAuthenticationResult =
                    (LocalAuthenticationResult) commandResult.getResult();
            localAuthenticationResult.setCorrelationId(correlationId);
        }
    }

}
