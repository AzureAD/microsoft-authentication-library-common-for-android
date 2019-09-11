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
package com.microsoft.identity.common.internal.logging;

import android.os.Build;
import android.util.Log;

import androidx.annotation.Nullable;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class Logger {

    private static final String CUSTOM_LOG_ERROR = "Custom log failed to log message:%s";
    private static ExecutorService sLogExecutor = Executors.newSingleThreadExecutor();
    private static final Logger INSTANCE = new Logger();
    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Turn on the VERBOSE level logging by default.
    private LogLevel mLogLevel = LogLevel.VERBOSE;
    private ILoggerCallback mExternalLogger;

    // Disable to log PII by default.
    private static boolean sAllowPii = false;
    // Disable to Logcat logging by default.
    private static boolean sAllowLogcat = false;

    /**
     * Enum class for LogLevel that the sdk recognizes.
     */
    public enum LogLevel {
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
        VERBOSE
    }

    /**
     * @return The single instance of {@link Logger}.
     */
    public static Logger getInstance() {
        return INSTANCE;
    }

    /**
     * Enable/Disable log message with PII (personal identifiable information) info.
     * By default, the SDK doesn't log any PII.
     *
     * @param allowPii True if enabling PII info to be logged, false otherwise.
     */
    public static void setAllowPii(final boolean allowPii) {
        sAllowPii = allowPii;
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk disables it.
     *
     * @param allowLogcat True if enabling the logcat logging, false otherwise.
     */
    public static void setAllowLogcat(final boolean allowLogcat) {
        sAllowLogcat = allowLogcat;
    }

    /**
     * Get if log PII is enabled.
     *
     * @return true if pii is allowed in logging, false otherwise.
     */
    public static boolean getAllowPii() {
        return sAllowPii;
    }

    /**
     * Get if logcat is enabled.
     *
     * @return true if logcat turned on, false otherwise.
     */
    public static boolean getAllowLogcat() {
        return sAllowLogcat;
    }

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level
     * logging.
     *
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        mLogLevel = logLevel;
    }

    /**
     * Set the custom logger. Configures external logging to configure a callback that
     * the sdk will use to pass each log message. Overriding the logger callback is not allowed.
     *
     * @param externalLogger The reference to the {@link ILoggerCallback} that can
     *                       output the logs to the designated places.
     */
    public void setExternalLogger(final ILoggerCallback externalLogger) {
        mExternalLogger = externalLogger;
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
                             @Nullable final String errorMessage,
                             @Nullable final Throwable exception) {
        getInstance().log(
                tag,
                LogLevel.ERROR,
                DiagnosticContext.getRequestContext().toJsonString(),
                errorMessage,
                exception,
                false
        );
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
                             @Nullable final String correlationID,
                             @Nullable final String errorMessage,
                             @Nullable final Throwable exception) {
        getInstance().log(
                tag,
                LogLevel.ERROR,
                correlationID, errorMessage,
                exception,
                false
        );
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
                                @Nullable final String errorMessage,
                                @Nullable final Throwable exception) {
        getInstance().log(
                tag,
                LogLevel.ERROR,
                DiagnosticContext.getRequestContext().toJsonString(),
                errorMessage,
                exception,
                true
        );
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
                                @Nullable final String correlationID,
                                @Nullable final String errorMessage,
                                @Nullable final Throwable exception) {
        getInstance().log(
                tag,
                LogLevel.ERROR,
                correlationID,
                errorMessage,
                exception,
                true
        );
    }

    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void warn(final String tag, @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.WARN,
                DiagnosticContext.getRequestContext().toJsonString(),
                message,
                null,
                false
        );
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
                            @Nullable final String correlationID,
                            @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.WARN,
                correlationID,
                message,
                null,
                false
        );
    }

    /**
     * Send a {@link LogLevel#WARN} log message with PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void warnPII(final String tag, @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.WARN,
                DiagnosticContext.getRequestContext().toJsonString(),
                message,
                null,
                true
        );
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
                               @Nullable final String correlationID,
                               @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.WARN,
                correlationID,
                message,
                null,
                true
        );
    }

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void info(final String tag, @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.INFO,
                DiagnosticContext.getRequestContext().toJsonString(),
                message,
                null,
                false
        );
    }

    /**
     * * Send a {@link LogLevel#INFO} log message without PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the
     *                      class or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message       The message to log.
     */
    public static void info(final String tag,
                            @Nullable final String correlationID,
                            @Nullable final String message) {
        getInstance().log(tag, LogLevel.INFO, correlationID, message, null, false);
    }

    /**
     * Send a {@link LogLevel#INFO} log message with PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void infoPII(final String tag, @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.INFO,
                DiagnosticContext.getRequestContext().toJsonString(),
                message,
                null,
                true
        );
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
                               @Nullable final String correlationID,
                               @Nullable final String message) {
        getInstance().log(tag, LogLevel.INFO, correlationID, message, null, true);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void verbose(final String tag, @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.VERBOSE,
                DiagnosticContext.getRequestContext().toJsonString(),
                message,
                null,
                false
        );
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
                               @Nullable final String correlationID,
                               @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.VERBOSE,
                correlationID,
                message,
                null,
                false
        );
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message with PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void verbosePII(final String tag, @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.VERBOSE,
                DiagnosticContext.getRequestContext().toJsonString(),
                message,
                null,
                true
        );
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
                                  @Nullable final String correlationID,
                                  @Nullable final String message) {
        getInstance().log(
                tag,
                LogLevel.VERBOSE,
                correlationID,
                message,
                null,
                true
        );
    }

    private void log(final String tag,
                     final LogLevel logLevel,
                     @Nullable final String correlationID,
                     @Nullable final String message,
                     @Nullable final Throwable throwable,
                     final boolean containsPII) {
        sLogExecutor.execute(new Runnable() {
            @Override
            public void run() {
                if (logLevel.compareTo(mLogLevel) > 0) {
                    return;
                }

                // Developer turns off PII logging, if the log meLoggerSettingssage contains any PII,
                // we should not send it.
                if (!sAllowPii && containsPII) {
                    return;
                }

                //Format the log message.
                final String logMessage = formatMessage(correlationID, message, throwable);

                // Send logs into Logcat.
                if (sAllowLogcat) {
                    sendLogcatLogs(tag, logLevel, logMessage);
                }

                // Send logs into external logger callback.
                final ILoggerCallback externalLogger = mExternalLogger;

                if (null != externalLogger) {
                    try {
                        mExternalLogger.log(tag, logLevel, logMessage, containsPII);
                    } catch (final Exception e) {
                        // log message as warning to report callback error issue
                        if (!containsPII || sAllowPii) {
                            Log.w(tag, String.format(CUSTOM_LOG_ERROR, logMessage));
                        }
                    }
                }
            }
        });
    }

    /**
     * Wrap the log message.
     * If correlation id exists:
     * <library_version> <platform> <platform_version> [<timestamp> - <correlation_id>] <log_message>
     * If correlation id doesn't exist:
     * <library_version> <platform> <platform_version> [<timestamp>] <log_message>
     */
    private String formatMessage(@Nullable final String correlationID,
                                 @Nullable final String message,
                                 @Nullable final Throwable throwable) {
        final String logMessage = StringExtensions.isNullOrBlank(message) ? "N/A" : message;
        return " [" + getUTCDateTimeAsString()
                + (StringExtensions.isNullOrBlank(correlationID) ? "] " : " - " + correlationID + "] ")
                + logMessage
                + " Android " + Build.VERSION.SDK_INT
                + (throwable == null ? "" : '\n' + Log.getStackTraceString(throwable));
    }

    private static String getUTCDateTimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(new Date());
    }

    /**
     * Send logs to logcat as the default logging if developer doesn't turn off the logcat logging.
     */
    private void sendLogcatLogs(final String tag, final LogLevel logLevel, final String message) {
        // Append additional message to the message part for logcat logging
        switch (logLevel) {
            case ERROR:
                Log.e(tag, message);
                break;

            case WARN:
                Log.w(tag, message);
                break;

            case INFO:
                Log.i(tag, message);
                break;

            case VERBOSE:
                Log.v(tag, message);
                break;

            default:
                throw new IllegalArgumentException("Unknown log level");
        }
    }
}