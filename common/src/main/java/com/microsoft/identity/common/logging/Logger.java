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
package com.microsoft.identity.common.logging;

import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;


/**
 * Android's Logger. Wraps around common4j's logger (with an addition of Logcat).
 */
public class Logger {

    private static final String ANDROID_LOGCAT_LOGGER_IDENTIFIER = "ANDROID_LOGCAT_LOGGER";
    private static final String ANDROID_EXTERNAL_LOGGER_IDENTIFIER = "ANDROID_EXTERNAL_LOGGER";

    private static final Logger INSTANCE = new Logger();

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
        VERBOSE;

        private com.microsoft.identity.common.java.logging.Logger.LogLevel convertToJavaLogLevel() {
            switch (this) {
                case INFO:
                    return com.microsoft.identity.common.java.logging.Logger.LogLevel.INFO;
                case WARN:
                    return com.microsoft.identity.common.java.logging.Logger.LogLevel.WARN;
                case ERROR:
                    return com.microsoft.identity.common.java.logging.Logger.LogLevel.ERROR;
                default:
                    return com.microsoft.identity.common.java.logging.Logger.LogLevel.VERBOSE;
            }
        }

        private static LogLevel convertFromJavaLogLevel(
                @NonNull com.microsoft.identity.common.java.logging.Logger.LogLevel logLevel){
            switch (logLevel) {
                case INFO:
                    return INFO;
                case WARN:
                    return WARN;
                case ERROR:
                    return ERROR;
                default:
                    return VERBOSE;
            }
        }
    }

    static {
        setAndroidLogger();
    }

    /**
     * Initializes and set Logcat logger as (one of) common4j's external logger.
     * Also set Android's platform string.
     *
     * This must be called in Android code before we start using ANY common4j's code.
     * There will be a lot of places (adapters) at first, but eventually the number will shrink
     * once we finish each flows e2e.
     * */
    public static void setAndroidLogger(){
        com.microsoft.identity.common.java.logging.Logger.setLogger(
                ANDROID_LOGCAT_LOGGER_IDENTIFIER,
                new com.microsoft.identity.common.java.logging.ILoggerCallback() {
                    @Override
                    public void log(String tag,
                                    com.microsoft.identity.common.java.logging.Logger.LogLevel logLevel,
                                    String message,
                                    boolean containsPII) {
                        // Send logs into Logcat.
                        if (sAllowLogcat) {
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
                                    // Do nothing. Do not throw.
                            }
                        }
                    }
                });

        com.microsoft.identity.common.java.logging.Logger.setPlatformString("Android " + Build.VERSION.SDK_INT);
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
        com.microsoft.identity.common.java.logging.Logger.setAllowPii(allowPii);
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
        return com.microsoft.identity.common.java.logging.Logger.isAllowPii();
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
        com.microsoft.identity.common.java.logging.Logger.setLogLevel(logLevel.convertToJavaLogLevel());
    }

    /**
     * Set the custom logger. Configures external logging to configure a callback that
     * the sdk will use to pass each log message. Overriding the logger callback is not allowed.
     *
     * @param externalLogger The reference to the {@link ILoggerCallback} that can
     *                       output the logs to the designated places.
     */
    public void setExternalLogger(final ILoggerCallback externalLogger) {
        com.microsoft.identity.common.java.logging.Logger.setLogger(
                ANDROID_EXTERNAL_LOGGER_IDENTIFIER,
                new com.microsoft.identity.common.java.logging.ILoggerCallback() {
                    @Override
                    public void log(String tag,
                                    com.microsoft.identity.common.java.logging.Logger.LogLevel logLevel,
                                    String message,
                                    boolean containsPII) {
                        externalLogger.log(tag,
                                LogLevel.convertFromJavaLogLevel(logLevel),
                                message,
                                containsPII);
                    }
                });
    }

    /**
     * Get only the required metadata from the DiagnosticContext
     * to plug it in the log lines.
     * Here we are considering the correlation_id and the thread_name.
     * The need for this is because DiagnosticContext contains additional metadata which is not always required to be logged.
     *
     * @return String The concatenation of thread_name and correlation_id to serve as the required metadata in the log lines.
     */
    public static String getDiagnosticContextMetadata() {
        return com.microsoft.identity.common.java.logging.Logger.getDiagnosticContextMetadata();
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
        com.microsoft.identity.common.java.logging.Logger.error(
                tag,
                errorMessage,
                exception);
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
        com.microsoft.identity.common.java.logging.Logger.error(
                tag,
                correlationID,
                errorMessage,
                exception
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
        com.microsoft.identity.common.java.logging.Logger.errorPII(
                tag,
                errorMessage,
                exception
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
        com.microsoft.identity.common.java.logging.Logger.errorPII(
                tag,
                correlationID,
                errorMessage,
                exception
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
        com.microsoft.identity.common.java.logging.Logger.warn(
                tag,
                message
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
        com.microsoft.identity.common.java.logging.Logger.warn(
                tag,
                correlationID,
                message
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
        com.microsoft.identity.common.java.logging.Logger.warnPII(
                tag,
                message
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
        com.microsoft.identity.common.java.logging.Logger.warnPII(
                tag,
                correlationID,
                message
        );
    }

    /**
     * Send a {@link Logger.LogLevel#INFO} log message without PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void info(final String tag, @Nullable final String message) {
        com.microsoft.identity.common.java.logging.Logger.info(
                tag,
                message
        );
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
                            @Nullable final String correlationID,
                            @Nullable final String message) {
        com.microsoft.identity.common.java.logging.Logger.info(
                tag,
                correlationID,
                message
        );
    }

    /**
     * Send a {@link LogLevel#INFO} log message with PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void infoPII(final String tag, @Nullable final String message) {
        com.microsoft.identity.common.java.logging.Logger.infoPII(
                tag,
                message
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
        com.microsoft.identity.common.java.logging.Logger.infoPII(
                tag,
                correlationID,
                message
        );
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @param tag     Used to identify the source of a log message. It usually identifies the class
     *                or activity where the log call occurs.
     * @param message The message to log.
     */
    public static void verbose(final String tag, @Nullable final String message) {
        com.microsoft.identity.common.java.logging.Logger.verbose(
                tag,
                message
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
        com.microsoft.identity.common.java.logging.Logger.verbose(
                tag,
                correlationID,
                message
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
        com.microsoft.identity.common.java.logging.Logger.verbosePII(
                tag,
                message
        );
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message with PII.
     *
     * @param tag           Used to identify the source of a log message. It usually identifies the class
     *                      or activity where the log call occurs.
     * @param correlationID Unique identifier for a request or flow used to trace program execution.
     * @param message       The message to log.
     */
    public static void verbosePII(final String tag,
                                  final String correlationID,
                                  @Nullable final String message) {
        com.microsoft.identity.common.java.logging.Logger.verbosePII(
                tag,
                correlationID,
                message
        );
    }
}
