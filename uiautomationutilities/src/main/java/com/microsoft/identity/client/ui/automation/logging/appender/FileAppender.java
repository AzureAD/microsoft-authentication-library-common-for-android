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
package com.microsoft.identity.client.ui.automation.logging.appender;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.test.core.app.ApplicationProvider;

import com.microsoft.identity.client.ui.automation.logging.LogLevel;
import com.microsoft.identity.client.ui.automation.logging.formatter.ILogFormatter;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * An implementation of {@link IAppender} to write logs to a file on desk. The filename must be
 * supplied to the FileLogger and the file will be created in the files directory reserved by the OS
 * for the calling application. This directory is what is returned by {@link Context#getFilesDir()}.
 */
public class FileAppender implements IAppender {

    private static final String TAG = FileAppender.class.getSimpleName();

    private final String mFileName;
    private final File mLogFile;
    private final BufferedWriter mBufferedWriter;
    private final ILogFormatter mLogFormatter;


    public FileAppender(@NonNull final String filename, @NonNull final ILogFormatter logFormatter) throws IOException {
        mFileName = filename;
        mLogFile = createLogFile(filename);
        mBufferedWriter = new BufferedWriter(new FileWriter(mLogFile, true));
        mLogFormatter = logFormatter;
    }

    @Override
    public void append(LogLevel logLevel, String tag, String message, Throwable throwable) {
        final String logMessage = mLogFormatter.format(logLevel, tag, message, throwable);
        try {
            writeUsingBufferedWriter(logMessage);
        } catch (final IOException e) {
            Log.e(TAG, "Error while trying to write log to file.", e);
        }
    }

    private void writeUsingBufferedWriter(final String message) throws IOException {
        mBufferedWriter.append(message);
        mBufferedWriter.newLine();
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

    public void closeWriter() {
        try {
            mBufferedWriter.close();
            AppenderRegistry.getInstance().unregisterAppender(this);
        } catch (IOException e) {
            Log.e(TAG, "Error while trying to close file writer.", e);
        }
    }
}
