package com.microsoft.identity.common.internal.eststelemetry;

public class FailedRequest {

    private String mApiId;
    private String mCorrelationId;

    public FailedRequest(String mApiId, String mCorrelationId) {
        this.mApiId = mApiId;
        this.mCorrelationId = mCorrelationId;
    }

    @Override
    public String toString() {
        return mApiId + ',' + mCorrelationId;
    }
}
