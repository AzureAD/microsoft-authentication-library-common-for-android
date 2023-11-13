package com.microsoft.identity.common.java.opentelemetry.perf;

import com.google.gson.annotations.SerializedName;

import java.util.Date;

public class Breadcrumb {

    @SerializedName("name")
    private final String name;

    @SerializedName("timestamp")
    private final Date timestamp;

    public Breadcrumb(String name, Date timestamp) {
        this.name = name;
        this.timestamp = timestamp;
    }

    public String getName() {
        return name;
    }

    public Date getTimestamp() {
        return timestamp;
    }
}
