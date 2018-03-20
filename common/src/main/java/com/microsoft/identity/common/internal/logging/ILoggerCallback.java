package com.microsoft.identity.common.internal.logging;

/**
 * Interface for apps to configure the external logging and implement the callback to designate the
 * output of the log messages.
 */
public interface ILoggerCallback {
    /**
     * Interface method for apps to hand off each log message as it's generated.
     * @param tag The TAG for the log message.
     * @param logLevel The {@link CommonCoreLogger.LogLevel} for the generated message.
     * @param message The detailed message.
     * @param containsPII True if the log message contains PII, false otherwise.
     */
    // TODO: 3/19/2018 Do we need additional message part and adal/msal error code?
    void log(String tag, CommonCoreLogger.LogLevel logLevel, String message, boolean containsPII);
}
