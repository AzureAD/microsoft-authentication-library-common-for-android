package com.microsoft.identity.client.ui.automation.logging;

public interface LogStrategy {

    void log(final LogLevel logLevel, final String tag, final String message, final Throwable throwable);

}
