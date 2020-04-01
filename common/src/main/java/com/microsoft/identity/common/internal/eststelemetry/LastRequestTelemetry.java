// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// This code is licensed under the MIT License.
//
// Permission is hereby granted, free of charge, to any person obtaining a copy
// of this software and associated documentation files(the "Software"), to deal
// in the Software without restriction, including without limitation the rights
// to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
// copies of the Software, and to permit persons to whom the Software is
// furnished to do so, subject to the following conditions :
//
// The above copyright notice and this permission notice shall be included in
// all copies or substantial portions of the Software.
//
// THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// THE SOFTWARE.
package com.microsoft.identity.common.internal.eststelemetry;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class LastRequestTelemetry extends RequestTelemetry {

    @SerializedName("silent_successful_count")
    private int silentSuccessfulCount;

    @SerializedName("failed_requests")
    private List<FailedRequest> failedRequests;

    LastRequestTelemetry(@NonNull String schemaVersion) {
        super(schemaVersion);
        silentSuccessfulCount = 0;
        failedRequests = new ArrayList<>();
    }

    List<FailedRequest> getFailedRequests() {
        return failedRequests;
    }

    @Override
    public String getHeaderStringForFields() {
        // the first one contains the api id anc correlation id part
        // the second one contains the error codes
        final Pair<String, String> headerSegments = getHeaderStringForFailedRequests();

        final StringBuilder sb = new StringBuilder();
        sb.append(silentSuccessfulCount)
                .append(SchemaConstants.SEPARATOR_PIPE)
                .append(headerSegments.first)
                .append(SchemaConstants.SEPARATOR_PIPE)
                .append(headerSegments.second);

        return sb.toString();
    }

    void incrementSilentSuccessCount() {
        silentSuccessfulCount++;
    }

    void resetSilentSuccessCount() {
        silentSuccessfulCount = 0;
    }


    void appendFailedRequest(final String apiId, final String correlationId, final String error) {
        failedRequests.add(new FailedRequest(apiId, correlationId, error));
    }

    void appendFailedRequest(final FailedRequest failedRequest) {
        failedRequests.add(failedRequest);
    }

    void wipeFailedRequestAndErrorForSubList(Collection<FailedRequest> failedRequestsToRemove) {
        if (failedRequestsToRemove != null) {
            failedRequests.removeAll(failedRequestsToRemove);
        }
    }

    @Override
    public RequestTelemetry copySharedValues(@NonNull final RequestTelemetry requestTelemetry) {
        if (requestTelemetry instanceof LastRequestTelemetry) {
            this.silentSuccessfulCount = ((LastRequestTelemetry) requestTelemetry).silentSuccessfulCount;
        }

        return super.copySharedValues(requestTelemetry);
    }

    private Pair<String, String> getHeaderStringForFailedRequests() {
        if (failedRequests == null) {
            return new Pair<>("", "");
        }

        final FailedRequest[] failedRequestsArray = failedRequests.toArray(new FailedRequest[0]);

        if (failedRequestsArray == null) {
            return new Pair<>("", "");
        }

        final StringBuilder apiIdCorrelationIdSegmentBuilder = new StringBuilder();
        final StringBuilder errorSegmentBuilder = new StringBuilder();

        for (int i = 0; i < failedRequestsArray.length; i++) {
            final FailedRequest failedRequest = failedRequestsArray[i];
            apiIdCorrelationIdSegmentBuilder.append(failedRequest.toApiIdCorrelationString());
            errorSegmentBuilder.append(failedRequest.toErrorCodeString());
            if (i != failedRequestsArray.length - 1) {
                apiIdCorrelationIdSegmentBuilder.append(',');
                errorSegmentBuilder.append(',');
            }
        }

        return new Pair<>(apiIdCorrelationIdSegmentBuilder.toString(), errorSegmentBuilder.toString());
    }
}
