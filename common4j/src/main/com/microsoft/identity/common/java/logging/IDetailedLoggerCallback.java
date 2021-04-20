package com.microsoft.identity.common.java.logging;

/**
 * An extension of ILoggerCallback - so that it prints the discarded log.
 * This is for testing only (to verify that logs are actually discarded).
 */
interface IDetailedLoggerCallback extends ILoggerCallback {
    /**
     * Messages that are discarded by the loggers.
     */
    void discardedLog(String tag, Logger.LogLevel logLevel, String message, boolean containsPII);
}