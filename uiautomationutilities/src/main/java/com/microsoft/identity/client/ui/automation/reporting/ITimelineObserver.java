package com.microsoft.identity.client.ui.automation.reporting;

import com.google.gson.JsonElement;

import java.util.List;

import lombok.NonNull;

public interface ITimelineObserver {

    void onRecordingStarted();

    void onEventStart(@NonNull TimelineEvent event);

    void onEventEnd(@NonNull TimelineEvent event);

    void onRecordingEnded(@NonNull List<TimelineEventSeries> events, @NonNull JsonElement json);
}
