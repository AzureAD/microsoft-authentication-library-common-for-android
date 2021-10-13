package com.microsoft.identity.client.ui.automation.reporting;

import androidx.annotation.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;

import java.lang.reflect.Type;
import java.util.ArrayDeque;

import lombok.Data;
import lombok.NonNull;

import static com.microsoft.identity.client.ui.automation.reporting.Timeline.TIMELINE_DATE_FORMAT;

@Data
public class TimelineEventSeries {
    private static final Object object = new Object();
    private transient ArrayDeque<TimelineEvent> eventStack = new ArrayDeque<>();
    private transient final ITimelineObserver observer;

    private String entity;
    private int order;

    public TimelineEventSeries(final String entity, @NonNull ITimelineObserver observer) {
        this.entity = entity;
        this.observer = observer;
    }

    @NonNull
    public TimelineEvent startEvent(@NonNull final String title, @Nullable final String description) {
        final TimelineEvent first = eventStack.peekFirst();
        final TimelineEvent newEvent = new TimelineEvent(entity, title, description);

        if (first != null && !first.isComplete() && first != newEvent) {
            first.finish();
            observer.onEventEnd(first);
        }

        eventStack.push(newEvent);
        observer.onEventStart(newEvent);
        order = eventStack.size();

        return newEvent;
    }

    @Nullable
    public TimelineEvent endEvent(@Nullable final Throwable failureException) {
        final TimelineEvent first = eventStack.getFirst();
        if (first != null) {
            first.finish(failureException);
            observer.onEventEnd(first);
        }
        return first;
    }

    @Nullable
    public TimelineEvent endEvent(@Nullable final String result) {
        return this.endEvent(false, result);
    }

    @Nullable
    public TimelineEvent endEvent(final boolean failed, @Nullable final String result) {
        final TimelineEvent first = eventStack.peekFirst();
        if (first != null) {
            first.finish(failed, result);
            observer.onEventEnd(first);
        }
        return first;
    }

    public ArrayDeque<TimelineEvent> getEventStack() {
        return eventStack.clone();
    }


    public static class TimelineEventSeriesSerializer implements JsonSerializer<TimelineEventSeries> {

        @Override
        public JsonElement serialize(TimelineEventSeries src, Type typeOfSrc, JsonSerializationContext context) {
            final Gson gson = new GsonBuilder().setDateFormat(TIMELINE_DATE_FORMAT).create();
            final JsonObject jsonObject = (JsonObject) gson.toJsonTree(src);

            jsonObject.add("events", gson.toJsonTree(src.getEventStack()));

            return jsonObject;
        }
    }
}
