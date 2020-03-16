package com.microsoft.identity.common.internal.eststelemetry;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

public class LastRequestTelemetry extends RequestTelemetry {

    @SerializedName("silent_successful_count")
    private int mSilentSuccessfulCount;

    @SerializedName("failed_requests")
    private Queue<FailedRequest> failedRequests;

    @SerializedName("errors")
    private Queue<String> mErrors;

    LastRequestTelemetry(@NonNull String schemaVersion) {
        super(schemaVersion);
        mSilentSuccessfulCount = 0;
        failedRequests = new ConcurrentLinkedQueue<>();
        mErrors = new ConcurrentLinkedQueue<>();
    }

    @Override
    public String getHeaderStringForFields() {
        return mSilentSuccessfulCount + "|" + getHeaderStringForFields(failedRequests) + "|" + getHeaderStringForFields(mErrors);
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
        failedRequests.add(new FailedRequest(apiId, correlationId));
    }

    public void appendFailedRequestWithError(final String apiId, final String correlationId, final String errorCode) {
        appendFailedRequest(apiId, correlationId);
        appendError(errorCode);
    }

    public void wipeFailedRequestData() {
        failedRequests.clear();
        mErrors.clear();
    }
}
