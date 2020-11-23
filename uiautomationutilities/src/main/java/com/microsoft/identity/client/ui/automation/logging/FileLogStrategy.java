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

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class FileLogStrategy implements LogStrategy {

    private final String mFileName;

    public FileLogStrategy(@NonNull final String filename) {
        mFileName = filename;
    }

    @Override
    public void log(LogLevel logLevel, String tag, String message, Throwable throwable) {
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

    public String getLogFilePath() {
        try {
            return getLogFile().getAbsolutePath();
        } catch (IOException e) {
            return "";
        }
    }
//
//    private FileOutputStream getLogFile() throws IOException {
//        final Context context = ApplicationProvider.getApplicationContext();
//        final String fileName = mFileName + ".log";
//        return context.openFileOutput(fileName, MODE_APPEND);
//    }

    private String formatMessage(
            @NonNull final LogLevel logLevel,
            @NonNull final String tag,
            @NonNull final String message,
            @Nullable final Throwable throwable) {
        final String logMessage = logLevel.getLabel() + "/" + tag + ": " + message;
        return logMessage + (throwable == null ? "" : '\n' + Log.getStackTraceString(throwable));
    }
}
