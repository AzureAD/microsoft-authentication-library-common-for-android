package com.microsoft.identity.common.internal.logging;

import android.os.Build;
import android.util.Log;

import com.microsoft.identity.common.adal.internal.util.StringExtensions;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.atomic.AtomicReference;

public final class CommonCoreLogger {
    private static final CommonCoreLogger INSTANCE = new CommonCoreLogger();
    static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    // Turn on the VERBOSE level logging by default.
    private LogLevel mLogLevel  = LogLevel.VERBOSE;

    private AtomicReference<ILoggerCallback> mExternalLogger = new AtomicReference<>(null);

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
     * @return The single instance of {@link CommonCoreLogger}
     */
    public static CommonCoreLogger getInstance() {
        return INSTANCE;
    }

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level logging.
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
     * @throws IllegalStateException if external logger is already set, and the caller is trying to set it again.
     */
    public void setExternalLogger(final ILoggerCallback externalLogger) {
        if (externalLogger == null) {
            return;
        }

        if (mExternalLogger.get() != null) {
            throw new IllegalStateException("External logger is already set, cannot be set again.");
        }

        mExternalLogger.set(externalLogger);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     */
    public static void error(final String tag, final String correlationID, final String errorMessage, final Throwable exception) {
        getInstance().log(tag, LogLevel.ERROR, correlationID, errorMessage, exception, false);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message with PII.
     */
    public static void errorPII(final String tag, final String correlationID, final String errorMessage, final Throwable exception) {
        getInstance().log(tag, LogLevel.ERROR, correlationID, errorMessage, exception, true);
    }

    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     */
    public static void warn(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.WARN, correlationID, message, null, false);
    }

    /**
     * Send a {@link LogLevel#WARN} log message with PII.
     */
    public static void warnPII(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.WARN, correlationID, message, null, true);
    }

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     */
    public static void info(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.INFO, correlationID, message, null, false);
    }

    /**
     * Send a {@link LogLevel#INFO} log message with PII.
     */
    public static void infoPII(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.INFO, correlationID, message, null, true);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     */
    public static void verbose(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.VERBOSE, correlationID, message, null, false);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message with PII.
     */
    public static void verbosePII(final String tag, final String correlationID, final String message) {
        getInstance().log(tag, LogLevel.VERBOSE, correlationID, message, null, true);
    }

    /**
     * TODO. Need to discuss on how to keep the correlationID. CorrelationID should be per request => need to sync with Telemetry implementation
     *
     * @param tag
     * @param logLevel
     * @param correlationID
     * @param message
     * @param throwable
     * @param containsPII
     */
    private void log(final String tag, final LogLevel logLevel, final String correlationID,
                     final String message, final Throwable throwable, final boolean containsPII) {
        if (logLevel.compareTo(mLogLevel) > 0) {
            return;
        }

        // Developer turns off PII logging, if the log message contains any PII, we should not send it.
        if (!LoggerSettings.getInstance().getAllowPii() && containsPII) {
            return;
        }

        //Format the log message.
        final StringBuilder logMessage = new StringBuilder();
        logMessage.append(formatMessage(correlationID, message));

        // Add stacktrace to message.
        if (throwable != null) {
            logMessage.append('\n').append(Log.getStackTraceString(throwable));
        }

        // Send logs into Logcat.
        if (LoggerSettings.getInstance().getAllowLogcat()) {
            sendLogcatLogs(tag, logLevel, logMessage.toString());
        }

        // Send logs into external logger callback.
        if (mExternalLogger.get() != null) {
            mExternalLogger.get().log(tag, logLevel, logMessage.toString(), containsPII);
        }

    }

    /**
     * Wrap the log message.
     * If correlation id exists:
     * MSAL <library_version> <platform> <platform_version> [<timestamp> - <correlation_id>] <log_message>
     * If correlation id doesn't exist:
     * MSAL <library_version> <platform> <platform_version> [<timestamp>] <log_message>
     */
    private String formatMessage(final String correlationID, final String message) {
        final String logMessage = StringExtensions.isNullOrBlank(message) ? "N/A" : message;

        return "Common Core " + "SDK version is not set yet."
                + " Android " + Build.VERSION.SDK_INT
                + " [" + getUTCDateTimeAsString()
                + (StringExtensions.isNullOrBlank(correlationID) ? "] " : " - " + correlationID + "] ")
                + logMessage;
    }

    private static String getUTCDateTimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT);
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