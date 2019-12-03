package com.microsoft.identity.common.internal.eststelemetry;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LastRequestTelemetry extends RequestTelemetry {

    @SerializedName("silent_successful_count")
    private int mSilentSuccessfulCount;

    @SerializedName("failed_requests")
    private Queue<FailedRequestEstsTelemetryMetadata> mFailedRequestsMetadata;

    @SerializedName("errors")
    private Queue<String> mErrors;

    LastRequestTelemetry(@NonNull String schemaVersion) {
        super(schemaVersion);
        mSilentSuccessfulCount = 0;
        mFailedRequestsMetadata = new ConcurrentLinkedQueue<>();
        mErrors = new ConcurrentLinkedQueue<>();
    }

    @Override
    public String getHeaderStringForFields() {
        return mSilentSuccessfulCount + "|" + getHeaderStringForFields(mFailedRequestsMetadata) + "|" + getHeaderStringForFields(mErrors);
    }

    public void incrementSilentSuccessCount() {
        mSilentSuccessfulCount++;
    }

    public void resetSilentSuccessCount() {
        mSilentSuccessfulCount = 0;
    }

    private void appendError(final String errorCode) {
        mErrors.add(errorCode);
    }

    private void appendFailedRequest(final String apiId, final String correlationId) {
        mFailedRequestsMetadata.add(new FailedRequestEstsTelemetryMetadata(apiId, correlationId));
    }

    public void appendFailedRequestWithError(final String apiId, final String correlationId, final String errorCode) {
        appendFailedRequest(apiId, correlationId);
        appendError(errorCode);
    }

    public void wipeFailedRequestData() {
        mFailedRequestsMetadata.clear();
        mErrors.clear();
    }

    void putInCommonTelemetry(final String key, final String value) {
        return;
    }
}
