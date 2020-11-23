package com.microsoft.identity.client.ui.automation.logging;

public enum LogLevel {

    /**
     * Error level logging.
     */
    ERROR('E'),

    /**
     * Warn level logging.
     */
    WARN('W'),
    /**
     * Info level logging.
     */
    INFO('I'),

    /**
     * Verbose level logging.
     */
    VERBOSE('V');

    private final char label;

    LogLevel(char label) {
        this.label = label;
    }

    public char getLabel() {
        return this.label;
    }
}
