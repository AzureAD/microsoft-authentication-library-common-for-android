package com.microsoft.identity.common.internal.eststelemetry;

import com.google.gson.annotations.SerializedName;

public class CurrentRequestTelemetry extends RequestTelemetry {

    private String mApiId;

    private String mForceRefresh;

    private boolean mReturningFromCache; // this is not actually part of schema. needed to track other things.

    public boolean getReturningFromCache() {
        return mReturningFromCache;
    }

    public void setReturningFromCache(boolean returningFromCache) {
        this.mReturningFromCache = returningFromCache;
    }

    String getApiId() {
        return mApiId;
    }

    String getForceRefresh() {
        return mForceRefresh;
    }

    CurrentRequestTelemetry() {
        super(Schema.CURRENT_SCHEMA_VERSION);
    }

    @Override
    public String getHeaderStringForFields() {
        return mApiId + "," + mForceRefresh;
    }

    void putInCommonTelemetry(final String key, final String value) {
        switch (key) {
            case Schema.Key.API_ID:
                mApiId = value;
                break;
            case Schema.Key.FORCE_REFRESH:
                mForceRefresh = value;
                break;
            default:
                break;
        }
    }
}
