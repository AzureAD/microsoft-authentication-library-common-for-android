package com.microsoft.identity.common.java.telemetry.events;

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;

/**
 * An error event captures an Exception and extracts details from it. We can specify an errorTag that
 * can be a value that points to the exact location in the code where the exception was caught.
 */
public class ErrorEvent extends BaseEvent {

    public ErrorEvent() {
        super();
        types(TelemetryEventStrings.EventType.ERROR_EVENT);
    }

    public ErrorEvent putException(Exception exception) {
        if (exception == null) {
            return this;
        }

        put(TelemetryEventStrings.Key.ERROR_CLASS_NAME, exception.getClass().getSimpleName());
        put(TelemetryEventStrings.Key.ERROR_DESCRIPTION, exception.getMessage()); // pii

        if (exception instanceof BaseException) {
            final BaseException adaptedException = (BaseException) exception;

            if (adaptedException.getCause() != null) {
                put(TelemetryEventStrings.Key.ERROR_CLASS_NAME, adaptedException.getCause().getClass().getSimpleName());
            }

            put(TelemetryEventStrings.Key.ERROR_CODE, adaptedException.getErrorCode());
            final long errorTag = adaptedException.getErrorTag();

            if (errorTag != -1) {
                put(TelemetryEventStrings.Key.EVENT_NAME, String.valueOf(adaptedException.getErrorTag()));
            }
        }

        return this;
    }
}
