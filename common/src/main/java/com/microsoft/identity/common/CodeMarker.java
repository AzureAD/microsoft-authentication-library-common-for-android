package com.microsoft.identity.common;

public class CodeMarker {
    private String marker;
    private long timeMilliseconds;
    private String timeStamp;
    private long threadId;

    public CodeMarker(String marker, long timeMilliseconds, String timeStamp, long id) {
        this.marker = marker;
        this.timeMilliseconds = timeMilliseconds;
        this.timeStamp = timeStamp;
        this.threadId = id;
    }

    public long getThreadId() {
        return threadId;
    }

    public void setThreadId(long threadId) {
        this.threadId = threadId;
    }

    public String getTimeStamp() {
        return timeStamp;
    }

    public void setTimeStamp(String timeStamp) {
        this.timeStamp = timeStamp;
    }

    public String getMarker() {
        return marker;
    }

    public void setMarker(String marker) {
        this.marker = marker;
    }

    public long getTimeMilliseconds() {
        return timeMilliseconds;
    }

    public void setTimeMilliseconds(long timeMilliseconds) {
        this.timeMilliseconds = timeMilliseconds;
    }
}
