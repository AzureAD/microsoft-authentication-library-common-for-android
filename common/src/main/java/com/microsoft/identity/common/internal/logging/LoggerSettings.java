package com.microsoft.identity.common.internal.logging;

public class LoggerSettings {
    private static final LoggerSettings sINSTANCE = new LoggerSettings();
    // Disable to log PII by default.
    private boolean mAllowPii = false;
    // Disable to Logcat logging by default.
    private boolean mAllowLogcat = false;

    /**
     * @return The single instance of {@link LoggerSettings}.
     */
    public static LoggerSettings getInstance() {
        return sINSTANCE;
    }

    public void setAllowPii(final boolean allowPii) {
        mAllowPii = allowPii;
    }

    public void setAllowLogcat(final boolean allowLogcat) {
        mAllowLogcat = allowLogcat;
    }

    public boolean getAllowPii() {
        return mAllowPii;
    }

    public boolean getAllowLogcat() {
        return mAllowLogcat;
    }
}
