package com.microsoft.identity.common.internal.telemetry;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class BaseEvent extends Properties {
    public static final String START_TIME = "start_time";
    public static final String STOP_TIME = "stop_time";
    public static final String EVENT_NAME = "event_name";
    public static final String IS_COMPLETED = "is_completed";

    /**
     * Put the event name value into the properties map.
     *
     * @param eventName String of the event name
     * @return the event object
     */
    public BaseEvent putEventName(@NonNull String eventName) {
        put(EVENT_NAME, eventName);
        return this;
    }

    /**
     * Put the event start time into the properties map.
     *
     * @param eventStartTime Long of the event start time. If null, then put the current time as the start time.
     * @return the event object
     */
    public BaseEvent putEventStartTime(@Nullable Long eventStartTime) {
        if (null == eventStartTime) {
            put(START_TIME, String.valueOf(System.currentTimeMillis()));
        } else {
            put(START_TIME, eventStartTime.toString());
        }

        return this;
    }

    /**
     * Put the event stop time into the properties map.
     *
     * @param eventStopTime Long of the event stop time. If null, then put the current time as the stop time.
     * @return the event object
     */
    public BaseEvent putEventStopTime(@Nullable Long eventStopTime) {
        if (null == eventStopTime) {
            put(STOP_TIME, String.valueOf(System.currentTimeMillis()));
        } else {
            put(STOP_TIME, eventStopTime.toString());
        }

        return this;
    }

    /**
     * Put the event completion status into the properties map.
     *
     * @param isCompleted "true" if the event is completed, "false" otherwise.
     * @return the event object
     */
    public BaseEvent isCompleted(final Boolean isCompleted) {
        put(IS_COMPLETED, isCompleted.toString());
        return this;
    }
}
