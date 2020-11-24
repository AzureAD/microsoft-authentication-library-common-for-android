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

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.utils.AdbShellUtils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

import lombok.EqualsAndHashCode;

/**
 * An implementation of {@link ILogger} to write logs to a file on desk. The filename must be
 * supplied to the FileLogger and the file will be created in the files directory reserved by the OS
 * for the calling application. This directory is what is returned by {@link Context#getFilesDir()}.
 */
@EqualsAndHashCode
public class FileLogger implements ILogger {

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final String mFileName;

    public FileLogger(@NonNull final String filename) {
        mFileName = filename;
    }

    @Override
    public void e(@NonNull String tag, @NonNull String message) {
        log(LogLevel.ERROR, tag, message, null);
    }

    @Override
    public void e(@NonNull String tag, @NonNull String message, @NonNull Throwable exception) {
        log(LogLevel.ERROR, tag, message, exception);
    }

    @Override
    public void w(@NonNull String tag, @NonNull String message) {
        log(LogLevel.WARN, tag, message, null);
    }

    @Override
    public void w(@NonNull String tag, @NonNull String message, @NonNull Throwable exception) {
        log(LogLevel.WARN, tag, message, exception);
    }

    @Override
    public void i(@NonNull String tag, @NonNull String message) {
        log(LogLevel.INFO, tag, message, null);
    }

    @Override
    public void i(@NonNull String tag, @NonNull String message, @NonNull Throwable exception) {
        log(LogLevel.INFO, tag, message, exception);
    }

    @Override
    public void v(@NonNull String tag, @NonNull String message) {
        log(LogLevel.VERBOSE, tag, message, null);
    }

    @Override
    public void v(@NonNull String tag, @NonNull String message, @NonNull Throwable exception) {
        log(LogLevel.VERBOSE, tag, message, exception);
    }

    private void log(LogLevel logLevel, String tag, String message, Throwable throwable) {
        final String logMessage = formatMessage(logLevel, tag, message, throwable);
        writeLogToFile(logMessage);
    }

    private void writeLogToFile(final String logMessage) {
        try {
            final File logFile = getLogFile();
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(logMessage);
            buf.newLine();
            buf.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public File getLogFile() throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        final File directory = context.getFilesDir();
        final File logFile = new File(directory, mFileName);

        if (!logFile.exists()) {
            final boolean fileCreated = logFile.createNewFile();
            if (!fileCreated) {
                throw new IOException("Unable to create new log file :(");
            }
        }

        return logFile;
    }

    public String getLogFileName() {
        return mFileName;
    }

    private String formatMessage(
            @NonNull final LogLevel logLevel,
            @NonNull final String tag,
            @NonNull final String message,
            @Nullable final Throwable throwable) {
        final String logMessage = getUTCDateTimeAsString() + ": " + logLevel.getLabel() + "/" + tag
                + ": " + message;
        return logMessage + (throwable == null ? "" : '\n' + Log.getStackTraceString(throwable));
    }

    private static String getUTCDateTimeAsString() {
        final SimpleDateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));

        return dateFormat.format(new Date());
    }
}
