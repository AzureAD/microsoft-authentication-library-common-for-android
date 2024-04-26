// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.java.logging;

import com.microsoft.identity.common.java.nativeauth.util.ILoggable;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ThrowableUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import edu.umd.cs.findbugs.annotations.Nullable;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.Getter;
import lombok.NonNull;
import lombok.Setter;
import lombok.Synchronized;
import lombok.experimental.Accessors;

public class Logger {

    private static final ExecutorService sLogExecutor = Executors.newSingleThreadExecutor();
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";
    private static final String UNSET = "UNSET";

    // Turn on the VERBOSE level logging by default.
    @Setter()
    @Getter()
    @Accessors(prefix = "s")
    private static LogLevel sLogLevel = LogLevel.VERBOSE;

    // Disable to log PII by default.
    @Setter()
    @Getter()
    @Accessors(prefix = "s")
    private static boolean sAllowPii = false;

    @Accessors(prefix = "s")
    private static String sPlatformString = "";

    private static final ReentrantReadWriteLock sLoggersLock = new ReentrantReadWriteLock();

    private static final Map<String, ILoggerCallback> sLoggers = new HashMap<>();

    private static final SimpleDateFormat sDateTimeFormatter;
    static {
        sDateTimeFormatter = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        sDateTimeFormatter.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    /**
     * Set the platform string to be used when generating logs.
     *
     * @param platformString the platform string to set
     */
    @Synchronized
    public static void setPlatformString(String platformString) {
        Logger.sPlatformString = platformString;
    }

    /**
     * Enum class for LogLevel that the sdk recognizes.
     */
    public enum LogLevel {
        /**
         * No logs should be emitted.
         */
        NO_LOG,
        /**
         * Error level logging.
         */
        ERROR,
        /**
         * Warn level logging.
         */
        WARN,
        /**
         * Info level logging.
         */
        INFO,
        /**
         * Verbose level logging.
         */
        VERBOSE,
        /**
         * Undefined. Should be used in test only.
         */
        UNDEFINED
    }

    // Visible for testing
    static synchronized void resetLogger() {
        sLoggersLock.writeLock().lock();
        try {
            sLoggers.clear();
            sAllowPii = false;
            sPlatformString = "";
            sLogLevel = LogLevel.VERBOSE;
        } finally {
            sLoggersLock.writeLock().unlock();
        }
    }

    public static boolean setLogger(@NonNull String identifier,
                                    ILoggerCallback callback) {
        sLoggersLock.writeLock().lock();
        try {
            if (callback == null) {
                sLoggers.remove(identifier);
                return true;
            }

            if (sLoggers.containsValue(callback)){
                return false;
            }

            sLoggers.put(identifier, callback);
            return true;
        } finally {
            sLoggersLock.writeLock().unlock();
        }
    }

    /**
     * Get only the required metadata from the DiagnosticContext
     * to plug it in the log lines.
     * Here we are considering the correlation_id and the thread_name. Calls private getDiagnosticContextMetadata with
     * correlationId from existing RequestContext in DiagnosticContext
     * The need for this is because DiagnosticContext contains additional metadata which is not always required to be logged.
     *
     * @return String The concatenation of thread_name and correlation_id to serve as the required metadata in the log lines.
     */
    public static synchronized String getDiagnosticContextMetadata() {
        return getDiagnosticContextMetadata(null);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     *
     * @param tag          Used to identify the source of a log message.
     *                     It usually identifies the class or activity where the log call occurs.
     * @param errorMessage The error message to log.
     * @param exception    An exception to log
     */
    public static void error(final String tag,
                             final String errorMessage,
                             final Throwable exception) {
        log(tag, LogLevel.ERROR, null, errorMessage, null, exception, false);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param errorMessage  The error message to log.
     * @param exception     An exception to log.
     */
    public static void error(final String tag,
                             final String correlationID,
                             final String errorMessage,
                             final Throwable exception) {
        log(tag, LogLevel.ERROR, correlationID, errorMessage, null, exception, false);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message with PII.
     *
     * @param tag          Used to identify the source of a log message. It usually identifies the
     *                     class or activity where the log call occurs.
     * @param errorMessage The error message to log.
     * @param exception    An exception to log.
     */
    public static void errorPII(final String tag,
                                final String errorMessage,
                                final Throwable exception) {
        log(tag, LogLevel.ERROR, null, errorMessage, null, exception, true);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message with PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param errorMessage  The error message to log.
     * @param exception     An exception to log.
     */
    public static void errorPII(final String tag,
                                final String correlationID,
                                final String errorMessage,
                                final Throwable exception) {
        log(tag, LogLevel.ERROR, correlationID, errorMessage, null, exception, true);
    }

    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void warn(final String tag,
                            final String message) {
        log(tag, LogLevel.WARN, null, message, null, null, false);
    }

    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message       The message to log.
     */
    public static void warn(final String tag,
                            final String correlationID,
                            final String message) {
        log(tag, LogLevel.WARN, correlationID, message, null, null, false);
    }

    /**
     * Send a {@link Logger.LogLevel#WARN} log message. The object to printed may contain PII,
     * depending on its containsPii() value. If isAllowPii() is set to false, a PII-safe string
     * representation of the object will be used. If allowPII() is set to true, an un-sanitised
     * string representation of the object will be used.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message The message to log.
     * @param object The object to be printed.
     */
    public static void warnWithObject(final String tag,
                                      final String correlationID,
                                      final String message,
                                      final ILoggable object) {
        if (isAllowPii()) {
            log(tag, Logger.LogLevel.WARN, correlationID, message, object.toUnsanitizedString(), null, object.containsPii());
        } else {
            log(tag, Logger.LogLevel.WARN, correlationID, message, object.toString(), null, false);
        }
    }

    /**
     * Send a {@link Logger.LogLevel#WARN} log message. The object to printed may contain PII,
     * depending on its containsPii() value. If isAllowPii() is set to false, a PII-safe string
     * representation of the object will be used. If allowPII() is set to true, an un-sanitised
     * string representation of the object will be used.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     * @param object The object to be printed.
     */
    public static void warnWithObject(final String tag,
                                      final String message,
                                      final ILoggable object) {
        if (isAllowPii()) {
            log(tag, Logger.LogLevel.WARN, null, message, object.toUnsanitizedString(), null, object.containsPii());
        } else {
            log(tag, Logger.LogLevel.WARN, null, message, object.toString(), null, false);
        }
    }

    /**
     * Send a {@link LogLevel#WARN} log message with PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void warnPII(final String tag,
                               final String message) {
        log(tag, LogLevel.WARN, null, message, null, null, true);
    }

    /**
     * Send a {@link LogLevel#WARN} log message with PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message       The message to log.
     */
    public static void warnPII(final String tag,
                               final String correlationID,
                               final String message) {
        log(tag, LogLevel.WARN, correlationID, message, null, null, true);
    }

    /**
     * Send a {@link Logger.LogLevel#INFO} log message without PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void info(final String tag,
                            final String message) {
        log(tag, Logger.LogLevel.INFO, null, message, null, null, false);
    }

    /**
     * Send a {@link Logger.LogLevel#INFO} log message. The object to printed may contain PII,
     * depending on its containsPii() value. If isAllowPii() is set to false, a PII-safe string
     * representation of the object will be used. If allowPII() is set to true, an un-sanitised
     * string representation of the object will be used.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     * @param object The object to be printed.
     */
    public static void infoWithObject(final String tag,
                            final String message,
                            final ILoggable object) {
        if (isAllowPii()) {
            log(tag, Logger.LogLevel.INFO, null, message, object.toUnsanitizedString(), null, object.containsPii());
        } else {
            log(tag, Logger.LogLevel.INFO, null, message, object.toString(), null, false);
        }
    }

    /**
     * Send a {@link Logger.LogLevel#INFO} log message. The object to printed may contain PII,
     * depending on its containsPii() value. If isAllowPii() is set to false, a PII-safe string
     * representation of the object will be used. If allowPII() is set to true, an un-sanitised
     * string representation of the object will be used.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message The message to log.
     * @param object The object to be printed.
     */
    public static void infoWithObject(final String tag,
                                      final String correlationID,
                                      final String message,
                                      final ILoggable object) {
        if (isAllowPii()) {
            log(tag, Logger.LogLevel.INFO, correlationID, message, object.toUnsanitizedString(), null, object.containsPii());
        } else {
            log(tag, Logger.LogLevel.INFO, correlationID, message, object.toString(), null, false);
        }
    }

    /**
     * * Send a {@link Logger.LogLevel#INFO} log message without PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message       The message to log.
     */
    public static void info(final String tag,
                            final String correlationID,
                            final String message) {
        log(tag, LogLevel.INFO, correlationID, message, null, null, false);
    }

    /**
     * Send a {@link LogLevel#INFO} log message with PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void infoPII(final String tag,
                               final String message) {
        log(tag, LogLevel.INFO, null, message, null, null, true);
    }

    /**
     * Send a {@link LogLevel#INFO} log message with PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message       The message to log.
     */
    public static void infoPII(final String tag,
                               final String correlationID,
                               final String message) {
        log(tag, LogLevel.INFO, correlationID, message, null, null, true);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void verbose(final String tag,
                               final String message) {
        log(tag, LogLevel.VERBOSE, null, message, null, null, false);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message       The message to log.
     */
    public static void verbose(final String tag,
                               final String correlationID,
                               final String message) {
        log(tag, LogLevel.VERBOSE, correlationID, message, null, null, false);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message with PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void verbosePII(final String tag,
                                  final String message) {
        log(tag, LogLevel.VERBOSE, null, message, null, null, true);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message with PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message       The message to log.
     */
    public static void verbosePII(final String tag,
                                  final String correlationID,
                                  final String message) {
        log(tag, LogLevel.VERBOSE, correlationID, message, null, null, true);
    }

    private static void log(final String tag,
                            @NonNull final LogLevel logLevel,
                            final String correlationId,
                            final String message,
                            @Nullable final String objectToLog,
                            final Throwable throwable,
                            final boolean containsPII) {
        if ((sLogLevel == LogLevel.NO_LOG) || logLevel.compareTo(sLogLevel) > 0 || (!sAllowPii && containsPII)) {
            return;
        }

        final Date now = new Date();
        final String diagnosticMetadata = getDiagnosticContextMetadata(correlationId);

        sLogExecutor.execute(new Runnable() {
            @Override
            @SuppressFBWarnings(value = "DE_MIGHT_IGNORE",
                    justification = "If logging throws, there is nothing left to do but swallow the exception and move on.")
            public void run() {
                final String dateTimeStamp = sDateTimeFormatter.format(now);
                //Format the log message.
                final String logMessage = formatMessage(diagnosticMetadata, sPlatformString, message, objectToLog, dateTimeStamp, throwable);

                sLoggersLock.readLock().lock();
                try {
                    for (final String loggerCallbackKey : sLoggers.keySet()) {
                        try {
                            final ILoggerCallback callback = sLoggers.get(loggerCallbackKey);
                            if (callback != null) {
                                callback.log(tag, logLevel, logMessage, containsPII);
                            }
                        } catch (final Exception e) {
                            // Do nothing.
                        }
                    }
                } finally {
                    sLoggersLock.readLock().unlock();
                }
            }
        });
    }

    /**
     * Temporary method to allow the project to compile, without having to make changes everywhere
     */
    private static String formatMessage(@Nullable final String diagnosticMetadata,
                                        @Nullable final String platformString,
                                        @Nullable final String message,
                                        @Nullable final String objectToLog,
                                        @NonNull final String dateTimeStamp,
                                        @Nullable final Throwable throwable) {
        final String logMessage = StringUtil.isNullOrEmpty(message) ? "N/A" : message;
        final String logObject = StringUtil.isNullOrEmpty(objectToLog) ? "" : objectToLog;

        return "[" + dateTimeStamp
                + (StringUtil.isNullOrEmpty(diagnosticMetadata) ? " " : " - " + diagnosticMetadata + " ")
                + "- " + platformString + "] "
                + logMessage
                + logObject
                + (throwable == null ? "" : '\n' + ThrowableUtil.getStackTraceAsString(throwable));
    }

    /**
     * Get only the required metadata from the DiagnosticContext to plug it in the log lines.
     * Here thread_name is taken from DiagnosticContext, and correlationId from input parameter.
     * The need for this is because DiagnosticContext contains additional metadata which is not always required to be logged.
     *
     * @return String The concatenation of thread_name and correlation_id to serve as the required metadata in the log lines.
     */
    private static String getDiagnosticContextMetadata(@Nullable String correlationId) {
        final IRequestContext requestContext = DiagnosticContext.INSTANCE.getRequestContext();
        String threadId = requestContext.get(DiagnosticContext.THREAD_ID);

        if (StringUtil.isNullOrEmpty(threadId)) {
            threadId = UNSET;
        }
        if (StringUtil.isNullOrEmpty(correlationId)) {
            correlationId = requestContext.get(DiagnosticContext.CORRELATION_ID);
            if (StringUtil.isNullOrEmpty(correlationId)) {
                correlationId = UNSET;
            }
        }

        return String.format("%s: %s, %s: %s",
                DiagnosticContext.THREAD_ID, threadId, DiagnosticContext.CORRELATION_ID, correlationId);
    }
}
