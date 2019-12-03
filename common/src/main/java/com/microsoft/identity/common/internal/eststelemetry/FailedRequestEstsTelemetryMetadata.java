package com.microsoft.identity.common.internal.eststelemetry;

public class FailedRequestEstsTelemetryMetadata {

    private String mApiId;
    private String mCorrelationId;

    public FailedRequestEstsTelemetryMetadata(String mApiId, String mCorrelationId) {
        this.mApiId = mApiId;
        this.mCorrelationId = mCorrelationId;
    }

    @Override
    public String toString() {
        return mApiId + Schema.Separator.COMMA + mCorrelationId;
    }
}
