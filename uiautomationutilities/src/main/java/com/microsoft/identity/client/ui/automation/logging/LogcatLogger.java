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

import android.util.Log;

import androidx.annotation.NonNull;

import lombok.EqualsAndHashCode;

/**
 * An implementation of {@link ILogger} to send logs to Android logcat. This is just a wrapper
 * around {@link Log} utility provided by Android. If you just want to purely send logs to logcat,
 * then you should not use this {@link LogcatLogger} and rather use the Android provided
 * {@link Log} API.
 * <p>
 * The purpose of this {@link LogcatLogger} is to facilitate sending logs to multiple places when
 * used in conjunction with {@link Logger} and {@link LoggerRegistry#registerLogger(ILogger)}.
 */
@EqualsAndHashCode
public class LogcatLogger implements ILogger {

    private static final LogcatLogger INSTANCE = new LogcatLogger();

    /**
     * Get an instance of the {@link LogcatLogger}.
     *
     * @return a LogcatLogger
     */
    public static LogcatLogger getInstance() {
        return INSTANCE;
    }

    private LogcatLogger() {
    }

    @Override
    public void e(@NonNull String tag, @NonNull String message) {
        Log.e(tag, message);
    }

    @Override
    public void e(@NonNull String tag, @NonNull String message, @NonNull Throwable exception) {
        Log.e(tag, message, exception);
    }

    @Override
    public void w(@NonNull String tag, @NonNull String message) {
        Log.w(tag, message);
    }

    @Override
    public void w(@NonNull String tag, @NonNull String message, @NonNull Throwable exception) {
        Log.w(tag, message, exception);
    }

    @Override
    public void i(@NonNull String tag, @NonNull String message) {
        Log.i(tag, message);
    }

    @Override
    public void i(@NonNull String tag, @NonNull String message, @NonNull Throwable exception) {
        Log.i(tag, message, exception);
    }

    @Override
    public void v(@NonNull String tag, @NonNull String message) {
        Log.v(tag, message);
    }

    @Override
    public void v(@NonNull String tag, @NonNull String message, @NonNull Throwable exception) {
        Log.v(tag, message, exception);
    }
}
