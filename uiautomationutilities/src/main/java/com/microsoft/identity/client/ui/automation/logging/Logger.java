package com.microsoft.identity.client.ui.automation.logging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.HashSet;
import java.util.Set;

public class Logger {

    private static final Logger INSTANCE = new Logger();
    private final Set<LogStrategy> mLogStrategies = new HashSet<>();

    /**
     * @return The single instance of {@link Logger}.
     */
    private static Logger getInstance() {
        return INSTANCE;
    }

    public static Set<LogStrategy> getLogStrategies() {
        return getInstance().mLogStrategies;
    }

    public static void addStrategy(final LogStrategy logStrategy) {
        getLogStrategies().add(logStrategy);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    public static void e(@NonNull final String tag,
                         @NonNull final String message) {
        getInstance().log(
                LogLevel.ERROR,
                tag,
                message,
                null
        );
    }

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    public static void e(@NonNull final String tag,
                         @NonNull final String message,
                         @NonNull final Throwable exception) {
        getInstance().log(
                LogLevel.ERROR,
                tag,
                message,
                exception
        );
    }


    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    public static void w(@NonNull final String tag,
                         @NonNull final String message) {
        getInstance().log(
                LogLevel.WARN,
                tag,
                message,
                null
        );
    }

    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    public static void w(@NonNull final String tag,
                         @NonNull final String message,
                         @NonNull final Throwable exception) {
        getInstance().log(
                LogLevel.WARN,
                tag,
                message,
                exception
        );
    }

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    public static void i(@NonNull final String tag,
                         @NonNull final String message) {
        getInstance().log(
                LogLevel.INFO,
                tag,
                message,
                null
        );
    }

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    public static void i(@NonNull final String tag,
                         @NonNull final String message,
                         @NonNull final Throwable exception) {
        getInstance().log(
                LogLevel.INFO,
                tag,
                message,
                exception
        );
    }


    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    public static void v(@NonNull final String tag,
                         @NonNull final String message) {
        getInstance().log(
                LogLevel.VERBOSE,
                tag,
                message,
                null
        );
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    public static void v(@NonNull final String tag,
                         @NonNull final String message,
                         @NonNull final Throwable exception) {
        getInstance().log(
                LogLevel.VERBOSE,
                tag,
                message,
                exception
        );
    }

    private void log(@NonNull final LogLevel logLevel,
                     @NonNull final String tag,
                     @NonNull final String message,
                     @Nullable final Throwable throwable) {
        for (LogStrategy logStrategy : mLogStrategies) {
            logStrategy.log(logLevel, tag, message, throwable);
        }
    }

}
