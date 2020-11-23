package com.microsoft.identity.client.ui.automation.logging;

import androidx.annotation.NonNull;

import java.util.HashSet;
import java.util.Set;

public class LoggerRegistry {

    private static final LoggerRegistry INSTANCE = new LoggerRegistry();

    private final Set<ILogger> mRegisteredLoggers = new HashSet<>();

    private LoggerRegistry() {
    }

    private static LoggerRegistry getInstance() {
        return INSTANCE;
    }

    public static void registerLogger(@NonNull final ILogger logger) {
        getInstance().mRegisteredLoggers.add(logger);
    }

    public static void unregisterLogger(@NonNull final ILogger logger) {
        getInstance().mRegisteredLoggers.remove(logger);
    }

    public static Set<ILogger> getRegisteredLoggers() {
        return getInstance().mRegisteredLoggers;
    }

    public void unregisterAllLoggers() {
        getInstance().mRegisteredLoggers.clear();
    }
}
