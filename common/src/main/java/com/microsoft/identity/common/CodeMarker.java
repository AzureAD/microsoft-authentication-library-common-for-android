package com.microsoft.identity.common;

public class CodeMarker {
    private int marker;
    private long timeMilliseconds;
    private String timeStamp;
    private long threadId;

    public CodeMarker(int marker, long timeMilliseconds, String timeStamp, long id) {
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

    public int getMarker() {
        return marker;
    }

    public void setMarker(int marker) {
        this.marker = marker;
    }

    public long getTimeMilliseconds() {
        return timeMilliseconds;
    }

    public void setTimeMilliseconds(long timeMilliseconds) {
        this.timeMilliseconds = timeMilliseconds;
    }
}
