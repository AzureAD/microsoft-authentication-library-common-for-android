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

@Deprecated
public class Logger extends com.microsoft.identity.common.logging.Logger {

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

    /**
     * Set the log level for diagnostic purpose. By default, the sdk enables the verbose level
     * logging.
     *
     * @param logLevel The {@link LogLevel} to be enabled for the diagnostic logging.
     */
    public void setLogLevel(final LogLevel logLevel) {
        mInstanceDelegate.setLogLevel(adapt(logLevel));
    }

    public static Logger getInstance() {
        return INSTANCE;
    }

    public void setExternalLogger(final ILoggerCallback externalLogger) {
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

}