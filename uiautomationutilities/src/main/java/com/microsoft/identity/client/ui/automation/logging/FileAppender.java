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

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * An implementation of {@link IAppender} to write logs to a file on desk. The filename must be
 * supplied to the FileLogger and the file will be created in the files directory reserved by the OS
 * for the calling application. This directory is what is returned by {@link Context#getFilesDir()}.
 */
public class FileAppender implements IAppender {

    private static final String TAG = FileAppender.class.getSimpleName();

    private static final String DATE_FORMAT = "yyyy-MM-dd HH:mm:ss";

    private final String mFileName;
    private final File mLogFile;
    private BufferedWriter mBufferedWriter;


    public FileAppender(@NonNull final String filename) throws IOException {
        mFileName = filename;
        mLogFile = createLogFile(filename);
    }

    @Override
    public void append(LogLevel logLevel, String tag, String message, Throwable throwable) {
        final String logMessage = formatMessage(logLevel, tag, message, throwable);
        try {
            writeLogToFile(logMessage);
        } catch (final IOException e) {
            Log.e(TAG, "Error while trying to write log to file.", e);
        }
    }

    private void writeLogToFile(final String logMessage) throws IOException {
        try {
            writeUsingBufferedWriter(logMessage);
        } catch (IOException e) {
            // the writer may have got closed. So let's create a new one and re-attempt write.
            mBufferedWriter = createBufferedWriterForLogFile();
            writeUsingBufferedWriter(logMessage);
        }
    }

    private void writeUsingBufferedWriter(final String message) throws IOException {
        if (mBufferedWriter == null) {
            mBufferedWriter = createBufferedWriterForLogFile();
        }

        mBufferedWriter.append(message);
        mBufferedWriter.newLine();
    }

    private BufferedWriter createBufferedWriterForLogFile() throws IOException {
        return new BufferedWriter(new FileWriter(mLogFile, true));
    }

    private File createLogFile(@NonNull final String filename) throws IOException {
        final Context context = ApplicationProvider.getApplicationContext();
        final File directory = context.getFilesDir();
        final File logFile = new File(directory, filename);

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

    public File getLogFile() {
        return mLogFile;
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

    public void closeWriter() {
        try {
            mBufferedWriter.close();
        } catch (final IOException e) {
            Log.e(TAG, "Error while trying to close writer", e);
        }
    }
}
