/**
 * // Copyright (c) Microsoft Corporation.
 * // All rights reserved.
 * //
 * // This code is licensed under the MIT License.
 * //
 * // Permission is hereby granted, free of charge, to any person obtaining a copy
 * // of this software and associated documentation files(the "Software"), to deal
 * // in the Software without restriction, including without limitation the rights
 * // to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
 * // copies of the Software, and to permit persons to whom the Software is
 * // furnished to do so, subject to the following conditions :
 * //
 * // The above copyright notice and this permission notice shall be included in
 * // all copies or substantial portions of the Software.
 * //
 * // THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * // IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * // FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * // AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * // LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * // OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * // THE SOFTWARE.
 * */

//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.client.ui.automation.logging;

import androidx.annotation.NonNull;

import com.microsoft.identity.client.ui.automation.logging.appender.AppenderRegistry;
import com.microsoft.identity.client.ui.automation.logging.appender.IAppender;

/**
 * A logger to write/send logs. The logs will be sent to all the registered appenders.
 * See {@link IAppender}. The appender must be registered with the {@link AppenderRegistry} to use
 * that appender.
 */
public class Logger {

    private static final AppenderRegistry APPENDER_REGISTRY = AppenderRegistry.getInstance();

    public static AppenderRegistry getAppenderRegistry() {
        return APPENDER_REGISTRY;
    }

    /**
     * Send a {@link LogLevel#ERROR} log message.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    public static void e(@NonNull final String tag,
                         @NonNull final String message) {
        log(LogLevel.ERROR, tag, message, null);
    }

    /**
     * Send a {@link LogLevel#ERROR} log message.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    public static void e(@NonNull final String tag,
                         @NonNull final String message,
                         @NonNull final Throwable exception) {
        log(LogLevel.ERROR, tag, message, exception);
    }


    /**
     * Send a {@link LogLevel#WARN} log message.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    public static void w(@NonNull final String tag,
                         @NonNull final String message) {
        log(LogLevel.WARN, tag, message, null);
    }

    /**
     * Send a {@link LogLevel#WARN} log message.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    public static void w(@NonNull final String tag,
                         @NonNull final String message,
                         @NonNull final Throwable exception) {
        log(LogLevel.WARN, tag, message, exception);
    }

    /**
     * Send a {@link LogLevel#INFO} log message.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    public static void i(@NonNull final String tag,
                         @NonNull final String message) {
        log(LogLevel.INFO, tag, message, null);
    }

    /**
     * Send a {@link LogLevel#INFO} log message.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    public static void i(@NonNull final String tag,
                         @NonNull final String message,
                         @NonNull final Throwable exception) {
        log(LogLevel.INFO, tag, message, exception);
    }


    /**
     * Send a {@link LogLevel#VERBOSE} log message.
     *
     * @param tag     Used to identify the source of a log message.
     *                It usually identifies the class or activity where the log call occurs.
     * @param message The error message to log.
     */
    public static void v(@NonNull final String tag,
                         @NonNull final String message) {
        log(LogLevel.VERBOSE, tag, message, null);
    }

    /**
     * Send a {@link LogLevel#VERBOSE} log message.
     *
     * @param tag       Used to identify the source of a log message.
     *                  It usually identifies the class or activity where the log call occurs.
     * @param message   The error message to log.
     * @param exception An exception to log
     */
    public static void v(@NonNull final String tag,
                         @NonNull final String message,
                         @NonNull final Throwable exception) {
        log(LogLevel.VERBOSE, tag, message, exception);
    }

    private static void log(final LogLevel logLevel,
                            final String tag,
                            final String message,
                            final Throwable throwable) {
        for (final IAppender appender : APPENDER_REGISTRY.getRegisteredAppenders()) {
            appender.append(logLevel, tag, message, throwable);
        }
    }
}
