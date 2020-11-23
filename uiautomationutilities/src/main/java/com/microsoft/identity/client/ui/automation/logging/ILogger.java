package com.microsoft.identity.client.ui.automation.logging;

import androidx.annotation.NonNull;

public interface ILogger {

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    void e(@NonNull final String tag,
           @NonNull final String message);

    /**
     * Send a {@link LogLevel#ERROR} log message without PII.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    void e(@NonNull final String tag,
           @NonNull final String message,
           @NonNull final Throwable exception);


    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    void w(@NonNull final String tag,
           @NonNull final String message);

    /**
     * Send a {@link LogLevel#WARN} log message without PII.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    void w(@NonNull final String tag,
           @NonNull final String message,
           @NonNull final Throwable exception);

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    void i(@NonNull final String tag,
           @NonNull final String message);

    /**
     * Send a {@link LogLevel#INFO} log message without PII.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    void i(@NonNull final String tag,
           @NonNull final String message,
           @NonNull final Throwable exception);

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    void v(@NonNull final String tag,
           @NonNull final String message);

    /**
     * Send a {@link LogLevel#VERBOSE} log message without PII.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    void v(@NonNull final String tag,
           @NonNull final String message,
           @NonNull final Throwable exception);


}
