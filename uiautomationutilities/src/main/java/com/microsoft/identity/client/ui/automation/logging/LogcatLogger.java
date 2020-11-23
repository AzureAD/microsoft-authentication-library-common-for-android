package com.microsoft.identity.client.ui.automation.logging;

import android.util.Log;

import androidx.annotation.NonNull;

import lombok.EqualsAndHashCode;

@EqualsAndHashCode
public class LogcatLogger implements ILogger {

    private static final LogcatLogger INSTANCE = new LogcatLogger();

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
