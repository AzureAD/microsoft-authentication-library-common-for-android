package com.microsoft.identity.common.internal.logging;

public class LoggerSettings {
    // Disable to log PII by default.
    private static boolean mAllowPii = false;
    // Disable to Logcat logging by default.
    private static boolean mAllowLogcat = false;

    private LoggerSettings() { }

    /**
     *  Enable/Disable log message with PII (personal identifiable information) info.
     *  By default, the SDK doesn't log any PII.
     *
     * @param allowPii True if enabling PII info to be logged, false otherwise.
     */
    public static void setAllowPii(final boolean allowPii) {
        mAllowPii = allowPii;
    }

    /**
     * Enable/Disable the Android logcat logging. By default, the sdk disables it.
     *
     * @param allowLogcat True if enabling the logcat logging, false otherwise.
     */
    public static void setAllowLogcat(final boolean allowLogcat) {
        mAllowLogcat = allowLogcat;
    }

    /**
     * Get if log PII is enabled.
     */
    public static boolean getAllowPii() {
        return mAllowPii;
    }

    /**
     * Get if logcat is enabled.
     */
    public static boolean getAllowLogcat() {
        return mAllowLogcat;
    }
}
