package com.microsoft.identity.client.ui.automation.logging.formatter;

import com.microsoft.identity.client.ui.automation.logging.LogLevel;

public interface ILogFormatter {

    /**
     * Formats the log message into a single String and returns it.
     *
     * @param logLevel  the level of the log
     * @param tag       the tag associated to this log
     * @param message   the message to log
     * @param throwable the exception to log
     */
    String format(LogLevel logLevel, String tag, String message, Throwable throwable);

}
