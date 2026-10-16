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
package com.microsoft.identity.common.java.controllers;

import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.CANCEL_AUTHORIZATION_REQUEST;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterAliases.RETURN_AUTHORIZATION_REQUEST_RESULT;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.REQUEST_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.LocalBroadcasterFields.RESULT_CODE;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.PRODUCT;
import static com.microsoft.identity.common.java.AuthenticationConstants.SdkPlatformFields.VERSION;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_DCF_COMMAND_EXECUTION_END;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_DCF_COMMAND_EXECUTION_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_DCF_EXECUTOR_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_DCF_FUTURE_OBJECT_CREATION_END;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_DCF_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_END;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_EXECUTOR_START;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END;
import static com.microsoft.identity.common.java.marker.PerfConstants.CodeMarkerConstants.ACQUIRE_TOKEN_SILENT_START;

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.BuildConfig;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.broker.telemetry.EventTag;
import com.microsoft.identity.common.java.broker.telemetry.TelemetryHelper;
import com.microsoft.identity.common.java.commands.BaseCommand;
import com.microsoft.identity.common.java.commands.DeviceCodeFlowAuthResultCommand;
import com.microsoft.identity.common.java.commands.DeviceCodeFlowCommand;
import com.microsoft.identity.common.java.commands.DeviceCodeFlowTokenResultCommand;
import com.microsoft.identity.common.java.commands.ICommandResult;
import com.microsoft.identity.common.java.commands.InteractiveTokenCommand;
import com.microsoft.identity.common.java.commands.SilentTokenCommand;
import com.microsoft.identity.common.java.commands.parameters.BrokerInteractiveTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.CommandParameters;
import com.microsoft.identity.common.java.configuration.LibraryConfiguration;
import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ErrorStrings;
import com.microsoft.identity.common.java.exception.UserCancelException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.logging.RequestContext;
import com.microsoft.identity.common.java.marker.CodeMarkerManager;
import com.microsoft.identity.common.java.net.CancellationSignal;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.OtelContextExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.providers.oauth2.AuthorizationResult;
import com.microsoft.identity.common.java.request.SdkType;
import com.microsoft.identity.common.java.result.AcquireTokenResult;
import com.microsoft.identity.common.java.result.FinalizableResultFuture;
import com.microsoft.identity.common.java.result.ILocalAuthenticationResult;
import com.microsoft.identity.common.java.result.LocalAuthenticationResult;
import com.microsoft.identity.common.java.result.VoidResult;
import com.microsoft.identity.common.java.telemetry.Telemetry;
import com.microsoft.identity.common.java.util.BiConsumer;
import com.microsoft.identity.common.java.util.IPlatformUtil;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.LocalBroadcaster;
import com.microsoft.identity.common.java.util.ported.PropertyBag;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.atomic.AtomicBoolean;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

public class CommandDispatcher {

    private static final String TAG = CommandDispatcher.class.getSimpleName();
    private static final int DEFAULT_SILENT_REQUEST_THREAD_POOL_SIZE = 12;
    private static final int LEGACY_SILENT_REQUEST_THREAD_POOL_SIZE = 5;
    // CPU core count is used as a reference for sizing SilentRequestThreadPool.
    private static final int CPU_CORE_COUNT = Runtime.getRuntime().availableProcessors();
    private static final int DCF_REQUEST_THREAD_POOL_SIZE = 5;
    private static final int EXECUTOR_GRACEFUL_TERMINATION_TIMEOUT_MS = 500;
    private static final int EXECUTOR_FORCED_TERMINATION_TIMEOUT_MS = 1000;
    // Cache the pool size for the session
    //@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static final int SILENT_REQUEST_THREAD_POOL_SIZE = computeSilentRequestThreadPoolSize();
    private static volatile int sMaximumQueuedCommands = 64;
    private static final Object sAdmissionLock = new Object();
    private static int sMaximumOutstandingCallers = 128;
    private static int sAdmittedCallers;
    private static volatile ExecutorService sInteractiveExecutor = createBoundedExecutor(1, 1);
    private static volatile ExecutorService sSilentExecutor =
            createBoundedExecutor(SILENT_REQUEST_THREAD_POOL_SIZE, SILENT_REQUEST_THREAD_POOL_SIZE);
    private static volatile ExecutorService sDCFExecutor =
            createBoundedExecutor(DCF_REQUEST_THREAD_POOL_SIZE, DCF_REQUEST_THREAD_POOL_SIZE);
    private static final Object sLock = new Object();
    private static volatile InteractiveTokenCommand sCommand = null;
    private static final CommandResultCache sCommandResultCache = new CommandResultCache();

    private static final Object mapAccessLock = new Object();
    private static final ConcurrentMap<FinalizableResultFuture<CommandResult>, QueuedCommand> sCommandTasks =
            new ConcurrentHashMap<>();

    private static ThreadPoolExecutor createBoundedExecutor(final int core, final int maximum) {
        return new ThreadPoolExecutor(core, maximum, 30L, TimeUnit.SECONDS,
                new LinkedBlockingQueue<Runnable>(sMaximumQueuedCommands));
    }

    /**
     * Configures admission while idle, before submitting work. Accepted callbacks also retain a caller slot until
     * delivery. Reconfiguration with outstanding work fails instead of dropping or resurrecting commands.
     */
    public static void configureMemoryLimits(final int maximumQueuedCommands, final int maximumOutstandingCallers,
                                             final long maximumResultPayloadBytes) {
        if (maximumQueuedCommands <= 0 || maximumOutstandingCallers <= 0 || maximumResultPayloadBytes < 0) {
            throw new IllegalArgumentException("Invalid command memory limits");
        }
        final ExecutorService oldSilent;
        final ExecutorService oldInteractive;
        final ExecutorService oldDcf;
        synchronized (mapAccessLock) {
            synchronized (sAdmissionLock) {
                if (sAdmittedCallers != 0 || !sCommandTasks.isEmpty()
                        || busy(sSilentExecutor) || busy(sInteractiveExecutor) || busy(sDCFExecutor)) {
                    throw new IllegalStateException("Command memory limits can only change while idle");
                }
                sMaximumQueuedCommands = maximumQueuedCommands;
                sMaximumOutstandingCallers = maximumOutstandingCallers;
                oldSilent = sSilentExecutor;
                oldInteractive = sInteractiveExecutor;
                oldDcf = sDCFExecutor;
                final ThreadPoolExecutor silentPool = (ThreadPoolExecutor) oldSilent;
                sSilentExecutor = createBoundedExecutor(silentPool.getCorePoolSize(), silentPool.getMaximumPoolSize());
                sInteractiveExecutor = createBoundedExecutor(1, 1);
                sDCFExecutor = createBoundedExecutor(DCF_REQUEST_THREAD_POOL_SIZE, DCF_REQUEST_THREAD_POOL_SIZE);
                sCommandResultCache.setMaximumPayloadBytes(maximumResultPayloadBytes);
            }
        }
        oldSilent.shutdown();
        oldInteractive.shutdown();
        oldDcf.shutdown();
    }

    private static boolean busy(final ExecutorService executor) {
        final ThreadPoolExecutor pool = (ThreadPoolExecutor) executor;
        return pool.getActiveCount() != 0 || !pool.getQueue().isEmpty();
    }

    /** Returns accepted callers whose terminal callbacks have not yet been delivered or detached. */
    public static int getAdmittedCallerCount() {
        synchronized (sAdmissionLock) {
            return sAdmittedCallers;
        }
    }

    private static CallerReservation reserveCaller() {
        synchronized (sAdmissionLock) {
            if (sAdmittedCallers < sMaximumOutstandingCallers) {
                ++sAdmittedCallers;
                return new CallerReservation();
            }
        }
        // Posting another callback here would make the platform's callback queue the unbounded overflow queue.
        throw new RejectedExecutionException("Command caller capacity exhausted",
                new ClientException(ClientException.COMMAND_QUEUE_FULL, "Too many outstanding command callers"));
    }

    private static final class CallerReservation implements AutoCloseable {
        private final AtomicBoolean released = new AtomicBoolean();
        private String correlationId;

        @Override
        public void close() {
            if (released.compareAndSet(false, true)) {
                synchronized (sAdmissionLock) {
                    --sAdmittedCallers;
                }
            }
        }
    }

    private static final class CallerRegistration {
        private CallerReservation reservation;
        private BiConsumer<CommandResult, Throwable> consumer;
    }

    /**
     * Enum representing the possible outcomes of silent executor termination during reset.
     */
    private enum ExecutorTerminationOutcome {
        /** Executor terminated successfully within graceful timeout. */
        GRACEFUL,
        /** Executor required forced shutdown but eventually terminated. */
        FORCED,
        /** Executor did not fully terminate even after forced shutdown. */
        FAILED
    }

    //@GuardedBy("mapAccessLock")
    //Suppressing rawtype warnings due to the generic type BaseCommand
    @SuppressWarnings(WarningType.rawtype_warning)
    private static volatile ConcurrentMap<BaseCommand, FinalizableResultFuture<CommandResult>> sExecutingCommandMap = new ConcurrentHashMap<>();

    /**
     * Outcome of cancellation signal processing for a timed-out ATS request.
     * Emitted as the {@link AttributeName#cancellation_outcome} span attribute
     * to track cancellation effectiveness during flight rollout.
     */
    private enum CancellationOutcome {
        /** Worker detected cancellation before executeCommand() — no work performed, thread slot freed immediately. */
        skipped_before_execution,
        /** Worker's HTTP connection was disconnected mid-I/O by CancellationSignal.cancel(). */
        disconnected_during_execution
    }

    /**
     * Enum representing the lifecycle states of a request for timeout classification.
     * This enables precise tracking of where a timeout occurred in the request processing pipeline.
     */
    private enum RequestState {
        /** Request is waiting to acquire the mapAccessLock (lock contention) */
        WAITING_FOR_LOCK,
        /** Request has been queued in the thread pool executor (thread pool contention) */
        QUEUED,
        /** Request is actively being executed by a worker thread (slow execution) */
        EXECUTING
    }

    // Track request state per request for timeout classification
    // Maps correlation ID to its current state in the request lifecycle
    private static final ConcurrentMap<String, RequestState> sRequestStateMap =
        new ConcurrentHashMap<>();

    // Timeout diagnostic message templates
    private static final String TIMEOUT_MSG_LOCK_CONTENTION = 
        "Lock contention detected. Request timed out waiting to acquire mapAccessLock.";
    private static final String TIMEOUT_MSG_THREAD_POOL_SATURATED = 
        "Thread pool saturated. All %d threads busy, %d requests queued.";
    private static final String TIMEOUT_MSG_THREAD_POOL_CONTENTION = 
        "Thread pool contention. Request queued but not picked up in time.";
    private static final String TIMEOUT_MSG_EXECUTION_SLOW = 
        "Slow execution. Request was executing but didn't complete in time.";
    private static final String TIMEOUT_MSG_UNKNOWN_STATE = 
        "Unknown state '%s'.";

    // Compute the thread pool size based on flight configuration
    private static int computeSilentRequestThreadPoolSize() {
        if (CommonFlightsManager.INSTANCE.getFlightsProvider() != null) {
            boolean useIncreasedPoolSize = CommonFlightsManager.INSTANCE.getFlightsProvider()
                    .getBooleanValue(CommonFlight.USE_INCREASED_DEFAULT_SILENT_REQUEST_THREAD_POOL_SIZE);
            return useIncreasedPoolSize ? DEFAULT_SILENT_REQUEST_THREAD_POOL_SIZE
                    : LEGACY_SILENT_REQUEST_THREAD_POOL_SIZE;
        }
        return LEGACY_SILENT_REQUEST_THREAD_POOL_SIZE;
    }

    /**
     * Returns the cached thread pool size for silent requests.
     * This value is computed once during class initialization based on flight configuration.
     *
     * @return the cached pool size for the session
     */
    private static int getSilentRequestThreadPoolSize() {
        return SILENT_REQUEST_THREAD_POOL_SIZE;
    }

    /**
     * Returns the approximate number of threads that are actively
     * executing tasks in the silent request thread pool.
     */
    public static int getSilentRequestActiveCount(){
        return ((ThreadPoolExecutor)sSilentExecutor).getActiveCount();
    }

    /**
     * Returns the core pool size of the silent request thread pool.
     *
     * @return the core pool size of the silent executor
     */
    public static int getSilentExecutorPoolSize() {
        return ((ThreadPoolExecutor) sSilentExecutor).getCorePoolSize();
    }

    /**
     * Returns the default silent request thread pool size.
     * Used as default value when pool size is not available from broker response.
     *
     * @return the default pool size constant
     */
    public static int getDefaultSilentExecutorPoolSize() {
        return getSilentRequestThreadPoolSize();
    }

    private static void removeExecutingFuture(final FinalizableResultFuture<CommandResult> future) {
        synchronized (mapAccessLock) {
            // Commands can mutate their hash during execution. Also, an older worker must not remove its replacement.
            final ConcurrentMap<BaseCommand, FinalizableResultFuture<CommandResult>> remaining = new ConcurrentHashMap<>();
            for (final Map.Entry<BaseCommand, FinalizableResultFuture<CommandResult>> entry : sExecutingCommandMap.entrySet()) {
                if (entry.getValue() != future) {
                    remaining.put(entry.getKey(), entry.getValue());
                }
            }
            sExecutingCommandMap = remaining;
        }
    }

    //@VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static int outstandingCommands() {
        synchronized (mapAccessLock) {
            return sExecutingCommandMap.size();
        }
    }

    //@VisibleForTesting(otherwise = VisibleForTesting.NONE)
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

    //@VisibleForTesting(otherwise = VisibleForTesting.NONE)
    public static void clearState() throws Exception {
        final ExecutorService oldSilent;
        final ExecutorService oldInteractive;
        final ExecutorService oldDcf;
        synchronized (mapAccessLock) {
            oldSilent = sSilentExecutor;
            oldInteractive = sInteractiveExecutor;
            oldDcf = sDCFExecutor;
            sSilentExecutor = createBoundedExecutor(getSilentRequestThreadPoolSize(), getSilentRequestThreadPoolSize());
            sInteractiveExecutor = createBoundedExecutor(1, 1);
            sDCFExecutor = createBoundedExecutor(DCF_REQUEST_THREAD_POOL_SIZE, DCF_REQUEST_THREAD_POOL_SIZE);
            sExecutingCommandMap.clear();
            sRequestStateMap.clear();
        }
        try {
            shutdownNowAndCancel(oldSilent);
        } finally {
            try {
                shutdownNowAndCancel(oldInteractive);
            } finally {
                shutdownNowAndCancel(oldDcf);
            }
        }
    }

    /**
     * Builds a consistent diagnostic message for timeout exceptions.
     *
     * @param location The method where timeout occurred
     * @param message The specific timeout reason message
     * @param activeThreads Number of active threads in the pool
     * @param queueSize Number of requests waiting in the queue
     * @return Formatted diagnostic message
     */
    private static String buildTimeoutDiagnosticMessage(
            @NonNull final String location,
            @NonNull final String message,
            final int activeThreads,
            final int queueSize) {
        return String.format(
            "Timeout in %s: %s [ActiveThreads=%d, QueueSize=%d, PoolSize=%d]",
            location, message, activeThreads, queueSize, getSilentRequestThreadPoolSize());
    }

    /**
     * Creates a ClientException with a specific timeout error code based on the request state.
     * This enables definitive classification of timeout root causes for telemetry and diagnostics.
     *
     * Classification logic:
     * - WAITING_FOR_LOCK: Timeout while waiting for mapAccessLock (lock contention)
     * - QUEUED + pool saturated: All threads busy AND queue has waiting requests (severe saturation)
     * - QUEUED + not saturated: Threads busy but queue not fully saturated (moderate contention)
     * - EXECUTING: Request was picked up but didn't complete (slow execution)
     * - null/unknown: Fallback to generic timeout
     *
     * @param timeoutLocation The method where timeout occurred
     * @param correlationId The correlation ID of the request
     * @param state The request state at timeout (WAITING_FOR_LOCK, QUEUED, EXECUTING, or null)
     * @param activeThreads Number of active threads in the pool
     * @param queueSize Number of requests waiting in the queue
     * @param cause The original TimeoutException
     * @return ClientException with classified error code and diagnostic message
     */
    private static ClientException createTimeoutException(
            @NonNull final String timeoutLocation,
            @NonNull final String correlationId,
            @Nullable final RequestState state,
            final int activeThreads,
            final int queueSize,
            @NonNull final TimeoutException cause) {
        final String errorCode;
        final String reasonMessage;
        
        if (state == null) {
            errorCode = ClientException.TIMED_OUT;
            reasonMessage = String.format(TIMEOUT_MSG_UNKNOWN_STATE, "null");
        } else {
            switch (state) {
                case WAITING_FOR_LOCK:
                    errorCode = ClientException.TIMED_OUT_LOCK_CONTENTION;
                    reasonMessage = TIMEOUT_MSG_LOCK_CONTENTION;
                    break;
                case QUEUED:
                    // All threads busy AND requests queued = Pool completely saturated
                    if (activeThreads >= getSilentRequestThreadPoolSize() && queueSize > 0) {
                        errorCode = ClientException.TIMED_OUT_THREAD_POOL_SATURATED;
                        reasonMessage = String.format(TIMEOUT_MSG_THREAD_POOL_SATURATED, activeThreads, queueSize);
                    } else {
                        // Threads busy but pool not saturated = Temporary contention
                        errorCode = ClientException.TIMED_OUT_THREAD_POOL_CONTENTION;
                        reasonMessage = TIMEOUT_MSG_THREAD_POOL_CONTENTION;
                    }
                    break;
                case EXECUTING:
                    errorCode = ClientException.TIMED_OUT_EXECUTION;
                    reasonMessage = TIMEOUT_MSG_EXECUTION_SLOW;
                    break;
                default:
                    errorCode = ClientException.TIMED_OUT;
                    reasonMessage = String.format(TIMEOUT_MSG_UNKNOWN_STATE, state.name());
                    break;
            }
        }
        
        final String diagnosticMessage = buildTimeoutDiagnosticMessage(
            timeoutLocation, reasonMessage, activeThreads, queueSize);
        Logger.error(TAG, diagnosticMessage, cause);

        final ClientException exception = new ClientException(errorCode, diagnosticMessage, cause);
        exception.setCorrelationId(correlationId);
        return exception;
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
     * submitDcfAuthResultSilent - Run the Device Code Flow command using a DCF thread pool
     * @param command   {@link DeviceCodeFlowAuthResultCommand}
     * @return {@link AuthorizationResult}
     * @throws BaseException
     */
    public static AuthorizationResult submitDcfAuthResultSilent(@NonNull final DeviceCodeFlowAuthResultCommand command)
            throws BaseException {
        final CommandResult commandResult;
        try {
            commandResult = submitSilentReturningFuture(command).get();
        } catch (final RejectedExecutionException e) {
            throw admissionException(e);
        } catch (final InterruptedException | ExecutionException e) {
            throw ExceptionAdapter.baseExceptionFromException(e);
        }

        if (commandResult.getStatus() == ICommandResult.ResultStatus.COMPLETED){
            return (AuthorizationResult) commandResult.getResult();
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.ERROR){
            throw ExceptionAdapter.baseExceptionFromException((Throwable) commandResult.getResult());
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.CANCEL){
            throw new UserCancelException(ErrorStrings.USER_CANCELLED,
                    "Request cancelled by user");
        } else {
            throw new ClientException(ErrorStrings.UNKNOWN_ERROR, "Unexpected CommandResult status");
        }
    }

    /**
     * submitDcfTokenResultSilent - Run the Device Code Flow command using a DCF thread pool
     * @param command   {@link DeviceCodeFlowTokenResultCommand}
     * @return {@link ILocalAuthenticationResult}
     * @throws BaseException
     */
    public static ILocalAuthenticationResult submitDcfTokenResultSilent(@NonNull final DeviceCodeFlowTokenResultCommand command)
            throws BaseException {
        final CommandResult commandResult;
        try {
            commandResult = submitSilentReturningFuture(command).get();
        } catch (final RejectedExecutionException e) {
            throw admissionException(e);
        } catch (final InterruptedException | ExecutionException e) {
            throw ExceptionAdapter.baseExceptionFromException(e);
        }

        if (commandResult.getStatus() == ICommandResult.ResultStatus.COMPLETED){
            return (ILocalAuthenticationResult) commandResult.getResult();
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.ERROR){
             throw ExceptionAdapter.baseExceptionFromException((Throwable) commandResult.getResult());
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.CANCEL){
            throw new UserCancelException(ErrorStrings.USER_CANCELLED,
                    "Request cancelled by user");
        } else {
            throw new ClientException(ErrorStrings.UNKNOWN_ERROR, "Unexpected CommandResult status");
        }
    }


    /**
     * Perform acquireTokenSilent command synchronously.
     *
     * @param command {@link SilentTokenCommand}
     * @return ILocalAuthenticationResult
     * @throws BaseException
     * */
    // TODO: If we want this to be generic, we should make the success type of CommandResult to match with type T from BaseCommand<T>
    //       currently, CommandResult from BaseCommand<AcquireTokenResult> stores "ILocalAuthenticationResult".
    public static ILocalAuthenticationResult submitAcquireTokenSilentSync(@NonNull final SilentTokenCommand command)
            throws BaseException {
        final CommandResult commandResult;
        // Initialize to null, will be set after submitSilentReturningFuture returns
        // when it's guaranteed to be initialized on the parameters
        String correlationId = null;
        FinalizableResultFuture<CommandResult> future = null;
        final CallerRegistration registration = new CallerRegistration();
        try {
            future = submitCommand(command, registration, true);
            correlationId = command.getParameters().getCorrelationId();
            if (BuildConfig.DISABLE_ACQUIRE_TOKEN_SILENT_TIMEOUT){
                commandResult = future.get();
            } else {
                final int silentTokenTimeOutMs = CommonFlightsManager.INSTANCE.getFlightsProvider().getIntValue(CommonFlight.ACQUIRE_TOKEN_SILENT_TIMEOUT_MILLISECONDS);
                commandResult = future.get(silentTokenTimeOutMs, TimeUnit.MILLISECONDS);
            }
        } catch (final TimeoutException e) {
            final RequestState state = correlationId == null ? null : sRequestStateMap.get(correlationId);
            final int activeThreads = getSilentRequestActiveCount();
            final int queuedRequests = ((ThreadPoolExecutor) sSilentExecutor).getQueue().size();
            detachCaller(command, future, registration);
            // Classify timeout based on request state using correlation ID
            final String effectiveCorrelationId = correlationId != null ? correlationId : "unknown";
            throw createTimeoutException(
                    "submitAcquireTokenSilentSync",
                    effectiveCorrelationId,
                    state,
                    activeThreads,
                    queuedRequests,
                    e);
        } catch (final RejectedExecutionException e) {
            throw admissionException(e);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            detachCaller(command, future, registration);
            throw ExceptionAdapter.baseExceptionFromException(e);
        } catch (final ExecutionException e) {
            throw ExceptionAdapter.baseExceptionFromException(e);
        } finally {
            // Only clean up if correlationId was initialized
            if (correlationId != null) {
                sRequestStateMap.remove(correlationId);
            }
        }

        if (commandResult.getStatus() == ICommandResult.ResultStatus.COMPLETED){
            return (ILocalAuthenticationResult) commandResult.getResult();
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.ERROR){
            throw ExceptionAdapter.baseExceptionFromException((Throwable) commandResult.getResult());
        } else if (commandResult.getStatus() == ICommandResult.ResultStatus.CANCEL){
            throw new UserCancelException(ErrorStrings.USER_CANCELLED,
                    "Request cancelled by user");
        } else {
            throw new ClientException(ErrorStrings.UNKNOWN_ERROR, "Unexpected CommandResult status");
        }
    }

    /**
     * submitSilent - Run a command using the silent thread pool, and return the future governing it.
     *
     * <p><b>Telemetry note &mdash; coalesced callers.</b> When an equivalent command is already in
     * flight, this method attaches a listener to the existing future and returns early. That caller
     * therefore records neither {@link EventTag#BrokerCommandQueued} nor
     * {@link EventTag#BrokerCommandExecutionStart}, and every downstream tag (cache, network, token)
     * is recorded on the <em>executing</em> command's collector rather than the joining caller's.
     * The joining caller's {@code execution_flow} consequently ends at
     * {@code BrokerRequestDeserialized} and resumes at {@code BrokerResponseSerialized}.
     *
     * <p>This gap is expected and does not indicate a stall. A flow carrying
     * {@code BrokerRequestDeserialized} without {@code BrokerCommandExecutionStart} was coalesced
     * onto an in-flight identical request; the token returned to that caller is still correct. Note
     * that {@link CommandParameters} excludes both {@code correlationId} and {@code eventCollector}
     * from {@code equals}, so genuinely distinct requests do coalesce here.
     *
     * <p>Do <em>not</em> close the gap by hoisting {@code BrokerCommandQueued} above the caching
     * branch: no command is queued on behalf of a joining caller, so the event would be untrue, and
     * the resulting flow would misread as executor starvation rather than as a dedup.
     *
     * @param command command to submit
     * @throws RejectedExecutionException if the caller/callback budget is full; its cause is
     *         {@link ClientException#COMMAND_QUEUE_FULL}. No callback is posted for an unaccepted caller.
     */
    //@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static FinalizableResultFuture<CommandResult> submitSilentReturningFuture(@SuppressWarnings(WarningType.rawtype_warning)
                                                                                     @NonNull final BaseCommand command) {
        return submitCommand(command, new CallerRegistration(), true);
    }

    private static BaseException admissionException(final RejectedExecutionException exception) {
        return exception.getCause() instanceof ClientException ? (ClientException) exception.getCause()
                : ExceptionAdapter.baseExceptionFromException(exception);
    }

    private static void detachCaller(final BaseCommand command, final FinalizableResultFuture<CommandResult> future,
                                     final CallerRegistration registration) {
        if (future == null || registration.consumer == null) {
            return;
        }
        boolean cancel = false;
        synchronized (mapAccessLock) {
            if (future.removeWhenComplete(registration.consumer)) {
                registration.reservation.close();
                sRequestStateMap.remove(command.getParameters().getCorrelationId());
                if (CommonFlightsManager.INSTANCE.getFlightsProvider()
                        .getBooleanValue(CommonFlight.ENABLE_HTTP_CANCELLATION_ON_TIMEOUT)) {
                    if (future.getPendingConsumerCount() == 0 && !future.isDone()) {
                        removeExecutingFuture(future);
                        cancel = true;
                    }
                }
            }
        }
        if (cancel) {
            future.cancelSignal();
        }
    }

    private static FinalizableResultFuture<CommandResult> submitCommand(
            @NonNull final BaseCommand command, final CallerRegistration registration, final boolean coalesce) {
        registration.reservation = reserveCaller();
        try {
            return submitAdmittedCommand(command, registration, coalesce);
        } catch (final RuntimeException | Error failure) {
            registration.reservation.close();
            if (command.getParameters().getCorrelationId() != null) {
                sRequestStateMap.remove(command.getParameters().getCorrelationId());
            }
            throw failure;
        }
    }

    private static FinalizableResultFuture<CommandResult> submitAdmittedCommand(
            final BaseCommand command, final CallerRegistration registration, final boolean coalesce) {
        final CodeMarkerManager codeMarkerManager = CodeMarkerManager.getInstance();
        final String methodName;
        final ExecutorService commandExecutor;

        final CommandParameters commandParameters = command.getParameters();
        final String correlationId = initializeDiagnosticContext(commandParameters.getCorrelationId(),
                commandParameters.getSdkType() == null ? SdkType.UNKNOWN.getProductName() :
                        commandParameters.getSdkType().getProductName(), commandParameters.getSdkVersion());

        // set correlation id on parameters as it may not already be set
        commandParameters.setCorrelationId(correlationId);
        registration.reservation.correlationId = correlationId;

        final boolean isDeviceCodeFlowRequest = coalesce && isDeviceCodeFlowRequest(command);
        // Use DCF thread pool for DCF requests so that in case user chooses to follow interactive sign in,
        // future silent calls are not blocked
        if (isDeviceCodeFlowRequest) {
            codeMarkerManager.markCode(ACQUIRE_TOKEN_DCF_START);
            methodName = ":submitDCF";
        } else {
            codeMarkerManager.markCode(ACQUIRE_TOKEN_SILENT_START);
            methodName = coalesce ? ":submitSilent" : ":submit";
        }

        logParameters(TAG + methodName, correlationId, commandParameters, command.getPublicApiId());

        // Track state BEFORE entering synchronized block using correlation ID
        // This enables detection of lock contention timeouts
        // Only track state for non-DCF requests
        if (!isDeviceCodeFlowRequest) {
            sRequestStateMap.put(correlationId, RequestState.WAITING_FOR_LOCK);
        }

        final FinalizableResultFuture<CommandResult> finalFuture;
        final boolean owner;
        final boolean registered;
        final boolean eligible = coalesce && command.isEligibleForCaching();
        registration.consumer = getCommandResultConsumer(command, registration.reservation);
        synchronized (mapAccessLock) {
            commandExecutor = isDeviceCodeFlowRequest ? sDCFExecutor : sSilentExecutor;
            // Update state: lock acquired, now queued for thread pool, only track non-DCF requests
            if (!isDeviceCodeFlowRequest) {
                sRequestStateMap.put(correlationId, RequestState.QUEUED);
            }

            final FinalizableResultFuture<CommandResult> existing = eligible ? sExecutingCommandMap.get(command) : null;
            if (existing != null && !existing.getCancellationSignal().isCancelled()) {
                finalFuture = existing;
                owner = false;
                if (!isDeviceCodeFlowRequest) {
                    sRequestStateMap.put(correlationId, RequestState.EXECUTING);
                }
            } else {
                finalFuture = new FinalizableResultFuture<>();
                owner = true;
                if (eligible) {
                    sExecutingCommandMap.put(command, finalFuture);
                }
            }
            registered = finalFuture.tryWhenComplete(registration.consumer);
        }
        if (!registered) {
            finalFuture.whenComplete(registration.consumer);
        }
        if (!owner) {
            return finalFuture;
        }
        try {
            SpanExtension.current().setAttribute(
                    AttributeName.num_concurrent_silent_requests.name(),
                    sExecutingCommandMap.size()
            );
            int queueSize = ((ThreadPoolExecutor)sSilentExecutor).getQueue().size();
            SpanExtension.current().setAttribute(
                    AttributeName.silent_requests_queue_size.name(),
                    queueSize
            );


            // Report the actual pool size being used
            SpanExtension.current().setAttribute(
                    AttributeName.silent_executor_pool_size.name(),
                    getSilentExecutorPoolSize()
            );

            // Recorded before execute() so this event can never be timestamped after
            // BrokerCommandExecutionStart, which the worker thread may record immediately.
            TelemetryHelper.addEvent(
                    commandParameters.getEventCollector(),
                    EventTag.BrokerCommandQueued
            );
            submitTrackedCommand(commandExecutor, command, finalFuture, new Runnable() {
                @Override
                public void run() {
                    TelemetryHelper.addEvent(
                            commandParameters.getEventCollector(),
                            EventTag.BrokerCommandExecutionStart
                    );
                    final CancellationSignal cancellationSignal = finalFuture.getCancellationSignal();

                    // Update state: thread has picked up the request, now executing
                    // Only track non-DCF requests (timeout classification only in submitAcquireTokenSilentSync)
                    if (!isDeviceCodeFlowRequest) {
                        sRequestStateMap.put(correlationId, RequestState.EXECUTING);
                    }

                    codeMarkerManager.markCode(isDeviceCodeFlowRequest ? ACQUIRE_TOKEN_DCF_EXECUTOR_START : ACQUIRE_TOKEN_SILENT_EXECUTOR_START);
                    try {
                        //initializing again since the request is transferred to a different thread pool
                        initializeDiagnosticContext(correlationId, commandParameters.getSdkType() == null ?
                                        SdkType.UNKNOWN.getProductName() : commandParameters.getSdkType().getProductName(),
                                commandParameters.getSdkVersion());

                        // Check cancellation before executing — the caller may have
                        // already timed out while this was queued in the thread pool.
                        if (cancellationSignal.isCancelled()) {
                            SpanExtension.current().setAttribute(
                                    AttributeName.cancellation_outcome.name(),
                                    CancellationOutcome.skipped_before_execution.name());
                            Logger.info(TAG + methodName,
                                "PRE_START_CANCELLATION: Worker skipped execution."
                                + " correlationId=" + correlationId
                                + ", activeCount=" + getSilentRequestActiveCount()
                                + ", queueSize=" + ((ThreadPoolExecutor) sSilentExecutor).getQueue().size());
                            finalFuture.setException(cancelledCommandException());
                            return;
                        }

                        CommandResult<?> commandResult = null;

                        codeMarkerManager.markCode(isDeviceCodeFlowRequest ? ACQUIRE_TOKEN_DCF_COMMAND_EXECUTION_START : ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_START);
                        try {
                            commandResult = executeCommand(command);
                        } finally {
                            codeMarkerManager.markCode(isDeviceCodeFlowRequest ? ACQUIRE_TOKEN_DCF_COMMAND_EXECUTION_END : ACQUIRE_TOKEN_SILENT_COMMAND_EXECUTION_END);
                        }
                        Logger.info(TAG + methodName, "Completed silent request as owner for correlation id : **"
                                + correlationId + ", with the status : " + commandResult.getStatus().getLogStatus()
                                + " is cacheable : " + command.isEligibleForCaching());
                        // TODO 1309671 : change required to stop the LocalAuthenticationResult object from mutating in cases of cached command.
                        finalFuture.setResult(commandResult);
                    } catch (final Throwable t) {
                        Logger.error(TAG + methodName, "Command execution or result delivery failed", t);
                        if (cancellationSignal.isCancelled()) {
                            SpanExtension.current().setAttribute(
                                    AttributeName.cancellation_outcome.name(),
                                    CancellationOutcome.disconnected_during_execution.name());
                            Logger.info(TAG + methodName,
                                "CANCELLED_EXECUTION: Worker completed after cancellation."
                                + " correlationId=" + correlationId);
                        }
                        finalFuture.setException(new ExecutionException(t));
                        if (t instanceof Error) {
                            throw (Error) t;
                        }
                    } finally {
                        DiagnosticContext.INSTANCE.clear();
                    }
                    codeMarkerManager.markCode(isDeviceCodeFlowRequest ? ACQUIRE_TOKEN_DCF_FUTURE_OBJECT_CREATION_END : ACQUIRE_TOKEN_SILENT_FUTURE_OBJECT_CREATION_END);
                }
            });
        } catch (final RuntimeException | Error failure) {
            try {
                finalFuture.setException(failure);
            } finally {
                removeExecutingFuture(finalFuture);
                finalFuture.setCleanedUp();
            }
            throw failure;
        }
        return finalFuture;
    }

    public static void submitAndForget(@NonNull final BaseCommand command){
        submitAndForgetReturningFuture(command);
    }

    //@VisibleForTesting(otherwise = VisibleForTesting.PRIVATE)
    public static FinalizableResultFuture<CommandResult> submitAndForgetReturningFuture(@SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command){
        return submitCommand(command, new CallerRegistration(), false);
    }

    private static ClientException cancelledCommandException() {
        return new ClientException(ClientException.INTERRUPTED_OPERATION, "Command cancelled before completion");
    }

    private static void submitTrackedCommand(final ExecutorService executor, final BaseCommand command,
                                             final FinalizableResultFuture<CommandResult> future,
                                             final Runnable operation) {
        final String correlationId = command.getParameters().getCorrelationId();
        final QueuedCommand task = new QueuedCommand(executor, future, OtelContextExtension.wrap(operation), () -> {
            if (!future.isDone()) {
                future.setException(cancelledCommandException());
            }
        }, () -> {
            sCommandTasks.remove(future);
            removeExecutingFuture(future);
            if (correlationId != null) {
                sRequestStateMap.remove(correlationId);
            }
            future.setCleanedUp();
        });
        sCommandTasks.put(future, task);
        task.registerCancellation();
        if (task.isFinished()) {
            return;
        }
        try {
            executor.execute(task);
        } catch (final RejectedExecutionException rejected) {
            try {
                future.setException(new ClientException(ClientException.COMMAND_QUEUE_FULL,
                        "Command executor cannot accept more work"));
            } finally {
                task.cancelBeforeExecution();
            }
        } catch (final RuntimeException | Error failure) {
            try {
                future.setException(failure);
            } finally {
                task.cancelBeforeExecution();
            }
            throw failure;
        }
        // Cancellation can win between registering its callback and the executor inserting the wrapper.
        if (task.isFinished()) {
            task.removeFromQueue();
        }
    }

    private static void logParameters(@NonNull String tag, @NonNull String correlationId,
                                      @NonNull CommandParameters parameters,
                                      @Nullable String publicApiId) {
        final String TAG = tag + ":" + parameters.getClass().getSimpleName();

        //TODO:1315871 - conversion of PublicApiId in readable form.
        Logger.info(TAG, "Starting request with request context: "
                        + DiagnosticContext.INSTANCE.getRequestContext().toJsonString()
                        + ", with PublicApiId: " + publicApiId);

        parameters.logParameters(TAG, correlationId);
    }

    private static BiConsumer<CommandResult, Throwable> getCommandResultConsumer(
            @SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command,
            final CallerReservation reservation) {

        final String methodName = ":getCommandResultConsumer";

        return new BiConsumer<CommandResult, Throwable>() {
            @Override
            public void accept(CommandResult result, final Throwable throwable) {
                if (null != throwable) {
                    Logger.info(TAG + methodName, "Request encountered an exception " +
                            "(this maybe a duplicate request which caries the exception encountered by the original request)");
                    returnCommandResult(command, null, throwable, reservation);
                    return;
                }

                if (!StringUtil.isNullOrEmpty(result.getCorrelationId())
                        && !command.getParameters().getCorrelationId().equals(result.getCorrelationId())) {
                    Logger.info(TAG + methodName,
                            "Completed duplicate request with correlation id : **"
                                    + command.getParameters().getCorrelationId() + ", having the same result as : "
                                    + result.getCorrelationId() + ", with the status : "
                                    + result.getStatus().getLogStatus());
                }
                // Return command result will post() result for us.
                returnCommandResult(command, result, null, reservation);
            }
        };
    }

    // Suppressing unchecked warnings due to casting of Throwable to the generic type of TaskCompletedCallbackWithError
    @SuppressWarnings(WarningType.unchecked_warning)
    private static void commandCallBackOnError(@SuppressWarnings(WarningType.rawtype_warning) @NonNull BaseCommand command, Throwable throwable) {
        command.getCallback().onError(ExceptionAdapter.baseExceptionFromException(throwable));
    }

    public static void clearCommandCache() {
        sCommandResultCache.clear();
    }

    /** Drops advisory result-cache memory only. Does not cancel work or remove persisted credentials. */
    public static void trimMemory() {
        sCommandResultCache.trimMemory();
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
        CommandResult<?> commandResult = null;

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

        final String correlationId = command.getParameters().getCorrelationId();
        if (baseException != null) {
            if (baseException instanceof UserCancelException) {
                commandResult = CommandResult.ofNull(CommandResult.ResultStatus.CANCEL,
                        correlationId);
            } else {
                //Post On Error
                commandResult = CommandResult.of(CommandResult.ResultStatus.ERROR, baseException,
                        correlationId);
            }
        } else /* baseException == null */ {
            //Handler handler, final BaseCommand command, BaseException baseException, AcquireTokenResult result
            if (result != null && result instanceof AcquireTokenResult) {
                commandResult = getCommandResultFromTokenResult((AcquireTokenResult) result,
                        command.getParameters());
            } else if (result instanceof VoidResult) {
                commandResult = new CommandResult(CommandResult.ResultStatus.VOID, result,
                        command.getParameters().getCorrelationId());
            } else if (result == null) {
                commandResult = CommandResult.ofNull(CommandResult.ResultStatus.COMPLETED, correlationId);
            } else {
                //For commands that don't return an AcquireTokenResult
                commandResult = new CommandResult<>(CommandResult.ResultStatus.COMPLETED, result,
                        correlationId);
            }
        }

        // set correlation id on Local Authentication Result
        setCorrelationIdOnResult(commandResult, correlationId);
        setTelemetryOnResultAndFlush(commandResult, correlationId);
        return commandResult;
    }

    private static void setTelemetryOnResultAndFlush(@NonNull final CommandResult commandResult,
                                                     @NonNull final String correlationId) {
        final List<Map<String, String>> telemetryMap = Telemetry.getInstance().getMap(correlationId);
        commandResult.setTelemetryMap(telemetryMap);

        if (commandResult.getResult() instanceof LocalAuthenticationResult) {
            ((LocalAuthenticationResult) commandResult.getResult()).setTelemetry(telemetryMap);
        } else if (commandResult.getResult() instanceof BaseException) {
            ((BaseException) commandResult.getResult()).setTelemetry(telemetryMap);
        } else if (commandResult.getResult() != null) {
            Logger.verbose(
                    TAG + ":setTelemetryOnResult",
                    "Not setting telemetry on result as result type is " +
                            commandResult.getResult().getClass().getCanonicalName() +
                            " and doesn't support telemetry at this time."
            );
        }

        Telemetry.getInstance().flush(correlationId);
    }

        /**
         * Return the result of the command to the caller via the callback associated with the command
         *
         * @param command
         * @param result
         */
        private static void returnCommandResult (
        @SuppressWarnings(WarningType.rawtype_warning) @NonNull final BaseCommand command,
        @Nullable final CommandResult result, @Nullable final Throwable throwable,
        final CallerReservation reservation){
            try {
                final IPlatformUtil platformUtil = command.getParameters().getPlatformComponents().getPlatformUtil();
                if (throwable == null) {
                    platformUtil.onReturnCommandResult(command);
                }
                platformUtil.postCommandResult(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (throwable != null) {
                                commandCallBackOnError(command, throwable);
                                return;
                            }
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
                        } finally {
                            releaseCaller(command, reservation);
                        }
                    }
                });
            } catch (final RuntimeException | Error failure) {
                releaseCaller(command, reservation);
                throw failure;
            }
        }

        private static void releaseCaller(final BaseCommand command, final CallerReservation reservation) {
            if (reservation.correlationId != null) {
                sRequestStateMap.remove(reservation.correlationId);
            }
            reservation.close();
        }

        // Suppressing unchecked warnings due to casting of the result to the generic type of TaskCompletedCallbackWithError
        @SuppressWarnings(WarningType.unchecked_warning)
        private static void commandCallbackOnError (@SuppressWarnings("rawtypes") BaseCommand
        command, CommandResult result){
            command.getCallback().onError(ExceptionAdapter.baseExceptionFromException((Throwable) result.getResult()));
        }

        // Suppressing unchecked warnings due to casting of the result to the generic type of TaskCompletedCallback
        @SuppressWarnings(WarningType.unchecked_warning)
        private static void commandCallbackOnTaskCompleted
        (@SuppressWarnings("rawtypes") BaseCommand command, CommandResult result){
            command.getCallback().onTaskCompleted(result.getResult());
        }

        /**
         * Cache the result of the command (if eligible to do so) in order to protect the service from clients
         * making the requests in a tight loop
         *
         * @param command
         * @param commandResult
         */
        @SuppressWarnings("unused")
        private static void cacheCommandResult
        (@SuppressWarnings(WarningType.rawtype_warning) BaseCommand command,
                CommandResult commandResult){
            if (command.isEligibleForCaching() && eligibleToCache(commandResult)) {
                sCommandResultCache.put(command, commandResult);
            }
        }

    /**
     * Get Commandresult from acquiretokenresult
     *
     * @param result
     */
    private static CommandResult getCommandResultFromTokenResult(@NonNull AcquireTokenResult result, @NonNull CommandParameters commandParameters) {
        //Token Commands
        if (result.getSucceeded()) {
            return new CommandResult<>(CommandResult.ResultStatus.COMPLETED,
                    result.getLocalAuthenticationResult(), commandParameters.getCorrelationId());
        } else {
            //Get MsalException from Authorization and/or Token Error Response
            final BaseException baseException = ExceptionAdapter.exceptionFromAcquireTokenResult(result, commandParameters);
            if (baseException instanceof UserCancelException) {
                return CommandResult.ofNull(CommandResult.ResultStatus.CANCEL, commandParameters.getCorrelationId());
            } else {
                return new CommandResult<>(CommandResult.ResultStatus.ERROR, baseException, commandParameters.getCorrelationId());
            }
        }
    }

        /**
         * Determine if the command result should be cached
         *
         * @param commandResult
         * @return
         */
        private static boolean eligibleToCache ( @NonNull final CommandResult commandResult) {
            final String methodName = ":eligibleToCache";
            switch (commandResult.getStatus()) {
                case ERROR:
                    if (commandResult.getResult() instanceof BaseException) {
                        return ((BaseException) commandResult.getResult()).isCacheable();
                    }
                    Logger.warn(TAG + methodName, "Get status ERROR, but result is not a BaseException");
                    return true;
                case COMPLETED:
                    return true;
                default:
                    return false;
            }
        }

        public static void beginInteractive (final InteractiveTokenCommand command) {
            final String methodName = ":beginInteractive";
            final CallerReservation reservation = reserveCaller();
            final FinalizableResultFuture<CommandResult> future = new FinalizableResultFuture<>();
            future.whenComplete(getCommandResultConsumer(command, reservation));
            final ExecutorService executor;
            ExecutorService restartedExecutor = null;
            try {
                synchronized (sLock) {
                    if (LibraryConfiguration.getInstance().isAuthorizationInCurrentTask()
                            || command.getParameters() instanceof BrokerInteractiveTokenCommandParameters) {
                        if (LocalBroadcaster.INSTANCE.hasReceivers(CANCEL_AUTHORIZATION_REQUEST)) {
                            LocalBroadcaster.INSTANCE.broadcast(CANCEL_AUTHORIZATION_REQUEST, new PropertyBag());
                        } else if (LocalBroadcaster.INSTANCE.hasReceivers(RETURN_AUTHORIZATION_REQUEST_RESULT)) {
                            // A result receiver without a cancellation listener can leave the previous worker waiting for UI.
                            Logger.info(TAG + methodName, "Restarting a blocked interactive executor");
                            restartedExecutor = sInteractiveExecutor;
                            sInteractiveExecutor = createBoundedExecutor(1, 1);
                        }
                    }
                    executor = sInteractiveExecutor;
                }
                if (restartedExecutor != null) {
                    shutdownNowAndCancel(restartedExecutor);
                }
                // Recorded before execute() so this event can never be timestamped after
                // BrokerCommandExecutionStart, which the worker thread may record immediately.
                TelemetryHelper.addEvent(
                        command.getParameters().getEventCollector(),
                        EventTag.BrokerCommandQueued
                );
                submitTrackedCommand(executor, command, future, new Runnable() {
                    @Override
                    public void run() {
                        final CommandParameters commandParameters = command.getParameters();
                        TelemetryHelper.addEvent(
                                commandParameters.getEventCollector(),
                                EventTag.BrokerCommandExecutionStart
                        );
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

                            final BaseException[] receiverException = new BaseException[1];

                            final LocalBroadcaster.IReceiverCallback resultReceiver = new LocalBroadcaster.IReceiverCallback() {
                                @Override
                                public void onReceive(@NonNull PropertyBag dataBag) {
                                    try {
                                        completeInteractive(dataBag);
                                    } catch (final Exception e) {
                                        receiverException[0] = ExceptionAdapter.baseExceptionFromException(e);
                                    }
                                }
                            };

                            CommandResult commandResult;

                            synchronized (sLock) {
                                LocalBroadcaster.INSTANCE.registerCallback(
                                        RETURN_AUTHORIZATION_REQUEST_RESULT, resultReceiver);
                                sCommand = command;
                            }

                            //Try executing request
                            commandResult = executeCommand(command);

                            // If we received an exception during the receiver callback execution,
                            // we should set that as the command result
                            if (receiverException[0] != null) {
                                commandResult = CommandResult.of(CommandResult.ResultStatus.ERROR, receiverException[0], correlationId);
                            }

                            Logger.info(TAG + methodName,
                                    "Completed interactive request for correlation id : **" + correlationId +
                                            statusMsg(commandResult.getStatus().getLogStatus()));

                            clearInteractiveCommand(command);
                            future.setResult(commandResult);
                        } catch (final Throwable failure) {
                            Logger.error(TAG + methodName, "Interactive execution or result delivery failed", failure);
                            clearInteractiveCommand(command);
                            future.setException(failure);
                            if (failure instanceof Error) {
                                throw (Error) failure;
                            }
                        } finally {
                            clearInteractiveCommand(command);
                            DiagnosticContext.INSTANCE.clear();
                        }
                    }
                });
            } catch (final RuntimeException | Error failure) {
                reservation.close();
                throw failure;
            }
        }

        private static void clearInteractiveCommand(final InteractiveTokenCommand command) {
            synchronized (sLock) {
                if (sCommand == command) {
                    sCommand = null;
                    LocalBroadcaster.INSTANCE.unregisterCallback(RETURN_AUTHORIZATION_REQUEST_RESULT);
                }
            }
        }

        private static void completeInteractive ( final PropertyBag propertyBag){
            final String methodName = ":completeInteractive";

            int requestCode = propertyBag.<Integer>getOrDefault(REQUEST_CODE, -1);
            int resultCode = propertyBag.<Integer>getOrDefault(RESULT_CODE, -1);

            final InteractiveTokenCommand command = sCommand;
            if (command != null) {
                command.onFinishAuthorizationSession(requestCode, resultCode, propertyBag);
            } else {
                Logger.warn(TAG + methodName, "sCommand is null, No interactive call in progress to complete.");
            }
        }

        public static String initializeDiagnosticContext (
        @Nullable final String requestCorrelationId, final String sdkType, final String sdkVersion){
            final String methodName = ":initializeDiagnosticContext";

            final String correlationId = StringUtil.isNullOrEmpty(requestCorrelationId) ?
                    UUID.randomUUID().toString() :
                    requestCorrelationId;

            final RequestContext rc = new RequestContext();
            rc.put(DiagnosticContext.CORRELATION_ID, correlationId);
            rc.put(PRODUCT, sdkType);
            rc.put(VERSION, sdkVersion);
            DiagnosticContext.INSTANCE.setRequestContext(rc);
            Logger.verbose(
                    TAG + methodName,
                    "Initialized new DiagnosticContext"
            );

            return correlationId;
        }

        public static int getCachedResultCount () {
            return sCommandResultCache.getSize();
        }

        private static void setCorrelationIdOnResult ( @NonNull final CommandResult commandResult,
        @NonNull final String correlationId){
            // set correlation id on Local Authentication Result
            if (commandResult.getResult() != null &&
                    commandResult.getResult() instanceof LocalAuthenticationResult) {
                final LocalAuthenticationResult localAuthenticationResult =
                        (LocalAuthenticationResult) commandResult.getResult();
                localAuthenticationResult.setCorrelationId(correlationId);
            }
        }

        private static String statusMsg (String status){
            return ", with the status : " + status;
        }

        private static boolean isDeviceCodeFlowRequest(BaseCommand command) {
            return (command instanceof DeviceCodeFlowCommand
                    || command instanceof DeviceCodeFlowAuthResultCommand
                    || command instanceof DeviceCodeFlowTokenResultCommand);
        }
        
    /***
     * Stops the SilentRequestsExecutor.
     * Waits 1 Sec for existing silent requests to finish, before terminating them.
     * WARN!! No new silent requests will be processed after this until the executor is reset
     * This is expected to be called when in Shared Device Mode when global signout is performed.
     */
    public static void stopSilentRequestExecutor() {
        final String methodTag = TAG + ":stopSilentRequestExecutor";
        Logger.info(methodTag, "shutting down..");
        final ExecutorService executor = sSilentExecutor;
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1000, TimeUnit.MILLISECONDS)) {
                Logger.warn(methodTag, "terminating now");
                shutdownNowAndCancel(executor);
            }
        } catch (final InterruptedException e) {
            Logger.warn(methodTag, "terminating again");
            shutdownNowAndCancel(executor);
            Thread.currentThread().interrupt();
        }
    }

    private static List<Runnable> shutdownNowAndCancel(final ExecutorService executor) {
        // Seal submission before the snapshot. The dropped list also covers a wrapper missed by registry iteration.
        final List<Runnable> dropped = executor.shutdownNow();
        final List<QueuedCommand> tasks = new ArrayList<>();
        for (final QueuedCommand task : sCommandTasks.values()) {
            if (task.executor == executor) {
                tasks.add(task);
            }
        }
        for (final Runnable task : dropped) {
            if (task instanceof QueuedCommand) {
                tasks.add((QueuedCommand) task);
            }
        }
        Error fatalCallbackFailure = null;
        for (final QueuedCommand task : tasks) {
            try {
                try {
                    task.future.cancelSignal();
                } finally {
                    if (!task.future.isDone()) {
                        task.future.setException(cancelledCommandException());
                    }
                }
            } catch (final RuntimeException | Error callbackFailure) {
                Logger.error(TAG, "Command cancellation callback failed during shutdown", callbackFailure);
                if (callbackFailure instanceof Error && fatalCallbackFailure == null) {
                    fatalCallbackFailure = (Error) callbackFailure;
                }
            }
        }
        if (fatalCallbackFailure != null) {
            throw fatalCallbackFailure;
        }
        return dropped;
    }
    /**
     * Checks if the given package name belongs to a Broker application.
     *
     * @param packageName the package name to check
     * @return true if the package name is a Broker app, false otherwise
     */
    private static boolean isBrokerPackageName(@Nullable final String packageName) {
        if (StringUtil.isNullOrEmpty(packageName)) {
            return false;
        }
        return AuthenticationConstants.Broker.AZURE_AUTHENTICATOR_APP_PACKAGE_NAME.equals(packageName) ||
                AuthenticationConstants.Broker.COMPANY_PORTAL_APP_PACKAGE_NAME.equals(packageName) ||
                AuthenticationConstants.Broker.LTW_APP_PACKAGE_NAME.equals(packageName);
    }

    /**
     * Initializes the silent executor with expanded thread pool size based on Processor count.
     * <p>
     * This method should ONLY be called by Broker during its initialization phase
     * when the flight check determines that expanded pool is enabled.
     * MSAL client apps should NOT call this method - they will use the default pool size.
     * </p>
     * <p>
     * This enables Broker to handle more concurrent silent token requests (12 vs CPU count based on flight)
     * without affecting MSAL client apps which run in separate processes.
     * </p>
     *
     * @param callingPackageName the package name of the calling application for validation
     * @throws ClientException if called from a non-Broker application
     */
    public static void initializeSilentExecutorWithExpandedPool(@NonNull final String callingPackageName) throws ClientException {
        final String methodTag = TAG + ":initializeSilentExecutorWithExpandedPool";

        // Validate caller is a Broker application
        if (!isBrokerPackageName(callingPackageName)) {
            Logger.error(methodTag, "Method called from non-Broker application: " + callingPackageName, null);
            throw new ClientException(
                    ErrorStrings.BROKER_ONLY_OPERATION,
                    "This operation is only available for Broker applications."
            );
        }

        Logger.info(methodTag, "Expanding silent thread pool size for Broker to core pool size " + CPU_CORE_COUNT + ", based on processor count.");
        resetSilentRequestExecutorWithSize(CPU_CORE_COUNT);
    }


    /**
     * Resets the SilentRequestsExecutor with a custom thread pool size.
     * <p>
     * This gracefully shuts down the existing executor before creating a new one.
     * </p>
     * <p>
     * <strong>IMPORTANT:</strong> This method should ONLY be called during Broker initialization
     * or after global signout in Shared Device Mode when no silent requests are active.
     * </p>
     *
     * @param poolSize the desired thread pool size. Must be positive non-zero integer.
     */
    private static void resetSilentRequestExecutorWithSize(final int poolSize) {
        final String methodTag = TAG + ":resetSilentRequestExecutorWithSize";

        final int effectivePoolSize;
        if (poolSize <= 0) {
            final int defaultPoolSize = getSilentRequestThreadPoolSize();
            Logger.error(methodTag, "Invalid poolSize: " + poolSize + ". Using default: " + defaultPoolSize, null);
            effectivePoolSize = defaultPoolSize;
        } else {
            effectivePoolSize = poolSize;
        }

        Logger.info(methodTag, "Resetting silent Executor with pool size: " + effectivePoolSize);

        // Step 1: Atomically swap executor while holding lock (minimal lock duration)
        // This ensures new submissions go to the new executor immediately
        final ExecutorService oldExecutor;
        synchronized (mapAccessLock) {
            oldExecutor = sSilentExecutor;
            sSilentExecutor = createBoundedExecutor(effectivePoolSize, effectivePoolSize * 2);
            Logger.info(methodTag, "Swapped to new executor with " + effectivePoolSize + " threads");
            sExecutingCommandMap.clear();
        }

        // Step 2: Shutdown old executor OUTSIDE the lock to avoid deadlock
        // Workers can now complete their finally blocks (which need mapAccessLock)
        shutdownOldExecutor(oldExecutor, methodTag);
    }

    /**
     * Gracefully shuts down an old executor service.
     * <p>
     * <strong>IMPORTANT:</strong> Must be called WITHOUT holding mapAccessLock to avoid deadlock.
     * Worker tasks in their finally blocks need mapAccessLock to complete cleanup.
     * </p>
     *
     * @param executor  the old executor to shutdown
     * @param methodTag tag for logging
     */
    private static void shutdownOldExecutor(@NonNull final ExecutorService executor,
                                            @NonNull final String methodTag) {
        ExecutorTerminationOutcome terminationOutcome = ExecutorTerminationOutcome.FAILED;

        try {
            // Gracefully shutdown old executor
            executor.shutdown();

            // Wait for graceful completion
            if (executor.awaitTermination(EXECUTOR_GRACEFUL_TERMINATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                terminationOutcome = ExecutorTerminationOutcome.GRACEFUL;
            } else {
                Logger.warn(methodTag, "Executor did not terminate gracefully within " +
                        EXECUTOR_GRACEFUL_TERMINATION_TIMEOUT_MS + "ms, forcing shutdown");

                final List<Runnable> droppedTasks = shutdownNowAndCancel(executor);

                if (!droppedTasks.isEmpty()) {
                    Logger.warn(methodTag, "Forced shutdown dropped " + droppedTasks.size() +
                            " tasks (expected during reset after signout)");
                }

                // Wait for forced termination to complete
                if (executor.awaitTermination(EXECUTOR_FORCED_TERMINATION_TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
                    terminationOutcome = ExecutorTerminationOutcome.FORCED;
                } else {
                    Logger.error(methodTag, "Executor did not terminate after forced shutdown within " +
                            EXECUTOR_FORCED_TERMINATION_TIMEOUT_MS + "ms", null);
                }
            }
        } catch (final InterruptedException e) {
            Logger.warn(methodTag, "Interrupted while waiting for executor shutdown");
            shutdownNowAndCancel(executor);
            Thread.currentThread().interrupt();
        } catch (final RuntimeException e) {
            Logger.error(methodTag, "Unexpected exception during executor shutdown: " + e.getMessage(), e);
            try {
                shutdownNowAndCancel(executor);
            } catch (final RuntimeException ignored) {
                Logger.error(methodTag, "Failed to force shutdown executor", ignored);
            }
        }

        Logger.info(methodTag, "Old executor shutdown complete: " + terminationOutcome.name());
    }

    /***
     * Resets the SilentRequestsExecutor with the default thread pool size.
     * This creates a new Executor for silent requests.
     * This is expected to be called after global signout is performed in Shared Device mode.
     * This should be called if previously the Executor was stopped using 'stopSilentRequestExecutor'
     */
    public static void resetSilentRequestExecutor() {
        resetSilentRequestExecutorWithSize(getSilentRequestThreadPoolSize());
    }
}
