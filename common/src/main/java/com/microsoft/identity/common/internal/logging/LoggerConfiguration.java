package com.microsoft.identity.common.internal.logging;

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.logging.Logger.LogLevel;

import static com.microsoft.identity.common.internal.logging.LoggerConfiguration.SerializedNames.LOGCAT_ENABLED;
import static com.microsoft.identity.common.internal.logging.LoggerConfiguration.SerializedNames.LOG_LEVEL;
import static com.microsoft.identity.common.internal.logging.LoggerConfiguration.SerializedNames.PII_ENABLED;

public class LoggerConfiguration {

    /**
     * Field names used for serialization by Gson.
     */
    public static final class SerializedNames {
        public static final String PII_ENABLED = "pii_enabled";
        public static final String LOG_LEVEL = "log_level";
        public static final String LOGCAT_ENABLED = "logcat_enabled";
    }

    @SerializedName(PII_ENABLED)
    private boolean mPiiEnabled;

    @SerializedName(LOG_LEVEL)
    private LogLevel mLogLevel;

    @SerializedName(LOGCAT_ENABLED)
    private boolean mLogcatEnabled;

    /**
     * Gets the Pii Enabled state.
     *
     * @return True if Pii logging is allowed. False otherwise.
     */
    public boolean isPiiEnabled() {
        return mPiiEnabled;
    }

    /**
     * Gets the {@link LogLevel} to use.
     *
     * @return The LogLevel.
     */
    public LogLevel getLogLevel() {
        return mLogLevel;
    }

    /**
     * Gets the Logcat enabled state.
     *
     * @return True if Logcat is enabled, false otherwise.
     */
    public boolean isLogcatEnabled() {
        return mLogcatEnabled;
    }
}