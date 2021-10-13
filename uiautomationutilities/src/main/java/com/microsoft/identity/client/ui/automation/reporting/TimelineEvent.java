package com.microsoft.identity.client.ui.automation.reporting;

import androidx.annotation.Nullable;

import com.google.gson.annotations.Expose;

import java.util.Date;

import lombok.Data;
import lombok.NonNull;

@Data
public class TimelineEvent {
    protected static final String[] HEADER = new String[]{"Entity", "Category", "Description", "Failed", "Result", "Duration", "Start Time", "End Time"};

    @Expose
    private String entity;
    @Expose
    private String category;
    @Expose
    private String description;
    @Expose
    private boolean failed = false;
    @Expose
    private String result;
    @Expose
    private Date startTime;
    @Expose
    private Date endTime;
    private Throwable failureException;

    public TimelineEvent(@NonNull final String entity, @NonNull final String category, @Nullable final String description, @NonNull final Date startTime) {
        this.entity = entity;
        this.category = category;
        this.description = description;
        this.startTime = startTime;
    }

    public TimelineEvent(@NonNull final String entity, @NonNull final String category, @Nullable final String description) {
        this(entity, category, description, new Date());
    }

    public long duration() {
        final Date end = endTime == null ? new Date() : endTime;

        return end.getTime() - startTime.getTime();
    }

    public boolean isComplete() {
        return endTime != null;
    }

    public void finish() {
        this.finish(false, null);
    }

    public void finish(@Nullable final Throwable failureException) {
        if (!isComplete()) {
            this.failureException = failureException;
            this.finish(true, failureException == null ? "" : failureException.getMessage());
        }
    }

    public void finish(final boolean failed, @Nullable final String result) {
        if (!isComplete()) {
            this.failed = failed;
            this.result = result;
            this.endTime = new Date();
        }
    }
}