package com.microsoft.identity.client.ui.automation.reporting;

import androidx.annotation.Nullable;
import androidx.test.core.app.ApplicationProvider;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.microsoft.identity.client.ui.automation.R;
import com.microsoft.identity.client.ui.automation.logging.Logger;
import com.microsoft.identity.client.ui.automation.utils.CommonUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Scanner;

import lombok.NonNull;

public class Timeline {

    private static final String TAG = Timeline.class.getSimpleName();

    private static class InstanceHolder {
        private static final Timeline INSTANCE = new Timeline();
    }

    public static final String TIMELINE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    public static Timeline getInstance() {
        return InstanceHolder.INSTANCE;
    }


    private transient final Map<String, TimelineEventSeries> timelineEntries = new HashMap<>();
    private transient final TimelineObserver observerHandler = new TimelineObserver();
    private transient boolean isRecording = false;
    private transient final List<ITimelineObserver> mObservers = new ArrayList<>();

    private Date startTime = null;
    private Date endTime = null;

    private Timeline() {
    }

    public synchronized void startRecording() {
        timelineEntries.clear();
        isRecording = true;
        startTime = new Date();
        endTime = null;
        observerHandler.onRecordingStarted();

        Logger.i(TAG, "Timeline recording started.");
    }

    public synchronized static void start(@NonNull final String entity, @NonNull final String category) {
        start(entity, category, null);
    }

    public synchronized static void start(
            @NonNull final String entity,
            @NonNull final String category,
            @Nullable final String description
    ) {
        final Timeline timeline = getInstance();
        if (timeline.isRecording) {
            final TimelineEventSeries eventSeries =
                    timeline.timelineEntries.containsKey(entity) ?
                            timeline.timelineEntries.get(entity) : new TimelineEventSeries(entity, timeline.observerHandler);

            timeline.timelineEntries.put(entity, eventSeries);

            if (eventSeries != null) {
                eventSeries.startEvent(category, description);

                Logger.i(TAG, "Event(" + category + ") started recording for entity (" + entity + ")");
            }

        }
    }

    public static synchronized void finish(@NonNull final String entity) {
        finish(entity, false, null);
    }

    public static synchronized void finish(@NonNull final String entity, @Nullable final String result) {
        finish(entity, false, result);
    }

    public static synchronized void finish(@NonNull final String entity, @Nullable final Throwable failureException) {
        final Timeline timeline = getInstance();
        final TimelineEventSeries eventSeries = timeline.getEventSeries(entity);
        if (eventSeries == null || !timeline.isRecording) {
            return;
        }

        final TimelineEvent event = eventSeries.endEvent(failureException);
        if (event != null) {
            Logger.i(TAG, "Event(" + event.getCategory() + ") finished recording for entity (" + entity + ")");
        }
    }

    public static synchronized void finish(@NonNull final String entity, final boolean failed, @Nullable final String result) {
        final Timeline timeline = getInstance();
        final TimelineEventSeries eventSeries = timeline.getEventSeries(entity);
        if (eventSeries == null || !timeline.isRecording) {
            return;
        }

        final TimelineEvent event = eventSeries.endEvent(failed, result);
        if (event != null) {
            Logger.i(TAG, "Event(" + event.getCategory() + ") finished recording for entity (" + entity + ")");
        }
    }

    public JsonElement toJson(@Nullable final String title) {
        final GsonBuilder gsonBuilder = new GsonBuilder()
                .setDateFormat(TIMELINE_DATE_FORMAT);

        gsonBuilder.registerTypeAdapter(TimelineEventSeries.class, new TimelineEventSeries.TimelineEventSeriesSerializer());

        final Gson gson = gsonBuilder.create();
        final JsonObject jsonObject = (JsonObject) gson.toJsonTree(this);

        jsonObject.add("eventSeries", gson.toJsonTree(timelineEntries.values()));
        jsonObject.addProperty("title", title);

        return jsonObject;
    }


    public synchronized TimelineEventSeries getEventSeries(String entity) {
        return timelineEntries.get(entity);
    }


    public synchronized void stopRecording() {
        isRecording = false;
        endTime = new Date();
        observerHandler.onRecordingEnded(getEvents(), toJson(null));
    }

    public synchronized boolean isRecording() {
        return isRecording;
    }

    public long getDuration() {
        final Date endDate = endTime == null ? new Date() : endTime;
        return startTime == null ? 0 : endDate.getTime() - startTime.getTime();
    }

    public void addObserver(final @NonNull ITimelineObserver observer) {
        mObservers.add(observer);
    }

    public void removeObserver(final @NonNull ITimelineObserver observer) {
        mObservers.remove(observer);
    }

    public List<TimelineEventSeries> getEvents() {
        return new ArrayList<>(timelineEntries.values());
    }


    public static void createHTMLVisuals(final File outputFile, final String timelineTitle, final List<JsonElement> timelines) throws IOException {
        final InputStream templateStream = ApplicationProvider.getApplicationContext().getResources().openRawResource(R.raw.template);
        final Scanner scanner = new Scanner(templateStream);

        final StringBuilder stringBuilder = new StringBuilder();
        while (scanner.hasNextLine()) {
            stringBuilder.append(scanner.nextLine()).append("\n");
        }

        final JsonArray jsonArray = new JsonArray();
        for (JsonElement timeline : timelines) {
            jsonArray.add(timeline);
        }

        String content = stringBuilder.toString();
        content = content.replace("{{ pageTitle }}", timelineTitle);
        content = content.replace("{{ header }}", timelineTitle);
        content = content.replace("{{ timelines }}", jsonArray.toString());

        CommonUtils.writeToFile(outputFile, content, false);
    }

    private final class TimelineObserver implements ITimelineObserver {
        @Override
        public synchronized void onRecordingStarted() {
            for (ITimelineObserver observer : mObservers) {
                observer.onRecordingStarted();
            }
        }

        @Override
        public synchronized void onEventStart(@NonNull TimelineEvent event) {
            for (ITimelineObserver observer : mObservers) {
                observer.onEventStart(event);
            }
        }

        @Override
        public synchronized void onEventEnd(@NonNull TimelineEvent event) {
            for (ITimelineObserver observer : mObservers) {
                observer.onEventEnd(event);
            }
        }

        @Override
        public synchronized void onRecordingEnded(@NonNull final List<TimelineEventSeries> events, final @NonNull JsonElement json) {
            for (ITimelineObserver observer : mObservers) {
                observer.onRecordingEnded(events, json);
            }
        }
    }
}
