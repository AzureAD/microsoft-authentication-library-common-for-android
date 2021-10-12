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
package com.microsoft.identity.common.java.eststelemetry;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import lombok.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@SuppressFBWarnings("RCN_REDUNDANT_NULLCHECK_WOULD_HAVE_BEEN_A_NPE")
public class LastRequestTelemetry extends RequestTelemetry {

    final static int FAILED_REQUEST_CAP = 100;

    @SerializedName("silent_successful_count")
    private int silentSuccessfulCount;

    @SerializedName("failed_requests")
    private List<FailedRequest> failedRequests;

    LastRequestTelemetry(@NonNull String schemaVersion) {
        super(schemaVersion);
        silentSuccessfulCount = 0;
        failedRequests = new ArrayList<>();
    }

    /**
     * Get a list of Failed Request objects. The list returned here is unmodifiable. To add new
     * elements to this list use the {@link LastRequestTelemetry#appendFailedRequest} method.
     *
     * @return an unmodifiable list of {@link FailedRequest} objects
     */
    /* package */ List<FailedRequest> getFailedRequests() {
        return Collections.unmodifiableList(failedRequests);
    }

    @Override
    public String getHeaderStringForFields() {
        // the first one contains the api id anc correlation id part
        // the second one contains the error codes
        final Map.Entry<String, String> headerSegments = getHeaderStringForFailedRequests();

        final StringBuilder sb = new StringBuilder();
        sb.append(silentSuccessfulCount)
                .append(SchemaConstants.SEPARATOR_PIPE)
                .append(headerSegments.getKey())
                .append(SchemaConstants.SEPARATOR_PIPE)
                .append(headerSegments.getValue());

        return sb.toString();
    }

    void incrementSilentSuccessCount() {
        silentSuccessfulCount++;
    }

    void resetSilentSuccessCount() {
        silentSuccessfulCount = 0;
    }


    void appendFailedRequest(final String apiId, final String correlationId, final String error) {
        appendFailedRequest(new FailedRequest(apiId, correlationId, error));
    }

    void appendFailedRequest(final FailedRequest failedRequest) {
        // this should usually not be greater than - at most should be equal to the cap
        // because the only we to add to this list is via this append method.
        // The only time this could be greater than the cap is for some existing devices that may
        // have caught themselves in a bad state after accumulating too much telemetry in
        // Shared Preferences. (of course prior to the cap being put in place).
        // So will just take the last (most recent) 100 items here to get those out of the bad state,
        // and also to avoid having too much telemetry in the cache going forward.
        if (failedRequests.size() >= FAILED_REQUEST_CAP) {
            final int beginIndex = failedRequests.size() - FAILED_REQUEST_CAP + 1;
            final int endIndex = failedRequests.size();
            failedRequests = failedRequests.subList(beginIndex, endIndex);
        }
        failedRequests.add(failedRequest);
    }

    void wipeFailedRequestAndErrorForSubList(Collection<FailedRequest> failedRequestsToRemove) {
        if (failedRequestsToRemove != null) {
            failedRequests.removeAll(failedRequestsToRemove);
        }
    }

    @Override
    public IRequestTelemetry copySharedValues(@NonNull final IRequestTelemetry requestTelemetry) {
        if (requestTelemetry instanceof LastRequestTelemetry) {
            this.silentSuccessfulCount = ((LastRequestTelemetry) requestTelemetry).silentSuccessfulCount;
        }

        return super.copySharedValues(requestTelemetry);
    }

    private Map.Entry<String, String> getHeaderStringForFailedRequests() {
        if (failedRequests == null) {
            return new AbstractMap.SimpleEntry<>("", "");
        }

        final FailedRequest[] failedRequestsArray = failedRequests.toArray(new FailedRequest[0]);

        if (failedRequestsArray == null) {
            return new AbstractMap.SimpleEntry<>("", "");
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

        return new AbstractMap.SimpleEntry<>(apiIdCorrelationIdSegmentBuilder.toString(), errorSegmentBuilder.toString());
    }
}
