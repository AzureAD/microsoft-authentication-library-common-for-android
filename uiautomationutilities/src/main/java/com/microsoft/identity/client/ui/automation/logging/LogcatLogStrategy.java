package com.microsoft.identity.client.ui.automation.logging;

import android.util.Log;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class LogcatLogStrategy implements LogStrategy {
    @Override
    public void log(final LogLevel logLevel, final String tag, final String message, final Throwable throwable) {
        // Append additional message to the message part for logcat logging
        switch (logLevel) {
            case ERROR:
                Log.e(tag, message, throwable);
                break;

            case WARN:
                Log.w(tag, message, throwable);
                break;

            case INFO:
                Log.i(tag, message, throwable);
                break;

            case VERBOSE:
                Log.v(tag, message, throwable);
                break;

            default:
                throw new IllegalArgumentException("Unknown log level");
        }
    }
}
