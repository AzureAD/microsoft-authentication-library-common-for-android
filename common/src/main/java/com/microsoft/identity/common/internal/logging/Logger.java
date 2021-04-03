// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.logging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

@Deprecated
public class Logger extends com.microsoft.identity.common.logging.Logger {

    private static final String TAG = Logger.class.getSimpleName();

    private static boolean sLogDeprecationWarning = true;

    private static final Logger INSTANCE = new Logger();

    private final com.microsoft.identity.common.logging.Logger mInstanceDelegate
            = com.microsoft.identity.common.logging.Logger.getInstance();

    /**
     * Enum class for LogLevel that the sdk recognizes.
     */
    @Deprecated
    public enum LogLevel {
        /**
         * Error level logging.
         */
        ERROR,
        /**
         * Warn level logging.
         */
        WARN,
        /**
         * Info level logging.
         */
        INFO,
        /**
         * Verbose level logging.
         */
        VERBOSE
    }

    @Deprecated
    public static void setAllowPii(final boolean allowPii) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.setAllowPii(allowPii);
    }

    @Deprecated
    public static void setAllowLogcat(final boolean allowLogcat) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.setAllowLogcat(allowLogcat);
    }

    @Deprecated
    public static boolean getAllowPii() {
        logDeprecationWarning();
        return com.microsoft.identity.common.logging.Logger.getAllowPii();
    }

    @Deprecated
    public static boolean getAllowLogcat() {
        logDeprecationWarning();
        return com.microsoft.identity.common.logging.Logger.getAllowLogcat();
    }

    @Deprecated
    public static String getDiagnosticContextMetadata() {
        logDeprecationWarning();
        return com.microsoft.identity.common.logging.Logger.getDiagnosticContextMetadata();
    }

    @Deprecated
    public static void error(final String tag,
                             @Nullable final String errorMessage,
                             @Nullable final Throwable exception) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.error(tag, errorMessage, exception);
    }

    @Deprecated
    public static void error(final String tag,
                             @Nullable final String correlationID,
                             @Nullable final String errorMessage,
                             @Nullable final Throwable exception) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.error(tag, correlationID, errorMessage, exception);
    }

    @Deprecated
    public static void errorPII(final String tag,
                                @Nullable final String errorMessage,
                                @Nullable final Throwable exception) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.errorPII(tag, errorMessage, exception);
    }

    @Deprecated
    public static void errorPII(final String tag,
                                @Nullable final String correlationID,
                                @Nullable final String errorMessage,
                                @Nullable final Throwable exception) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.errorPII(tag, correlationID, errorMessage, exception);
    }

    @Deprecated
    public static void warn(final String tag, @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.warn(tag, message);
    }

    @Deprecated
    public static void warn(final String tag,
                            @Nullable final String correlationID,
                            @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.warn(tag, correlationID, message);
    }

    @Deprecated
    public static void warnPII(final String tag, @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.warnPII(tag, message);
    }

    @Deprecated
    public static void warnPII(final String tag,
                               @Nullable final String correlationID,
                               @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.warnPII(tag, correlationID, message);
    }

    @Deprecated
    public static void info(final String tag, @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.info(tag, message);
    }

    @Deprecated
    public static void info(final String tag,
                            @Nullable final String correlationID,
                            @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.info(tag, correlationID, message);
    }

    @Deprecated
    public static void infoPII(final String tag, @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.infoPII(tag, message);
    }

    @Deprecated
    public static void infoPII(final String tag,
                               @Nullable final String correlationID,
                               @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.infoPII(tag, correlationID, message);
    }

    @Deprecated
    public static void verbose(final String tag, @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.verbose(tag, message);
    }

    @Deprecated
    public static void verbose(final String tag,
                               @Nullable final String correlationID,
                               @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.verbose(tag, correlationID, message);
    }

    @Deprecated
    public static void verbosePII(final String tag, @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.verbosePII(tag, message);
    }

    @Deprecated
    public static void verbosePII(final String tag,
                                  @Nullable final String correlationID,
                                  @Nullable final String message) {
        logDeprecationWarning();
        com.microsoft.identity.common.logging.Logger.verbosePII(tag, correlationID, message);
    }

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level
     * logging.
     *
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        logDeprecationWarning();
        mInstanceDelegate.setLogLevel(adapt(logLevel));
    }

    public static Logger getInstance() {
        logDeprecationWarning();
        return INSTANCE;
    }

    public void setExternalLogger(final ILoggerCallback externalLogger) {
        logDeprecationWarning();
        mInstanceDelegate.setExternalLogger(new com.microsoft.identity.common.logging.ILoggerCallback() {
            @Override
            public void log(final String tag,
                            final com.microsoft.identity.common.logging.Logger.LogLevel logLevel,
                            final String message,
                            final boolean containsPII) {
                externalLogger.log(tag, adapt(logLevel), message, containsPII);
            }
        });
    }

    private static LogLevel adapt(com.microsoft.identity.common.logging.Logger.LogLevel in) {
        switch (in) {
            case ERROR:
                return LogLevel.ERROR;
            case WARN:
                return LogLevel.WARN;
            case INFO:
                return LogLevel.INFO;
            case VERBOSE:
                return LogLevel.VERBOSE;
            default:
                throw new RuntimeException("Unknown or invalid log level");
        }
    }

    private static com.microsoft.identity.common.logging.Logger.LogLevel adapt(@NonNull final LogLevel in) {
        switch (in) {
            case ERROR:
                return com.microsoft.identity.common.logging.Logger.LogLevel.ERROR;
            case WARN:
                return com.microsoft.identity.common.logging.Logger.LogLevel.WARN;
            case INFO:
                return com.microsoft.identity.common.logging.Logger.LogLevel.INFO;
            case VERBOSE:
                return com.microsoft.identity.common.logging.Logger.LogLevel.VERBOSE;
            default:
                throw new RuntimeException("Unknown or invalid log level");
        }
    }

    private static void logDeprecationWarning() {
        if (sLogDeprecationWarning) {
            sLogDeprecationWarning = false;
            com.microsoft.identity.common.logging.Logger.warn(TAG, "This class is deprecated. "
                    + "Migrate usage to: com.microsoft.identity.common.logging.Logger");
        }
    }
}