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

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

import java.util.ArrayList;
import java.util.List;

public class LastRequestTelemetry extends RequestTelemetry {

    @SerializedName("silent_successful_count")
    private int silentSuccessfulCount;

    @SerializedName("failed_requests")
    private List<FailedRequest> failedRequests;

    @SerializedName("errors")
    private List<String> errors;

    LastRequestTelemetry(@NonNull String schemaVersion) {
        super(schemaVersion);
        silentSuccessfulCount = 0;
        failedRequests = new ArrayList<>();
        errors = new ArrayList<>();
    }

    List<FailedRequest> getFailedRequests() {
        return failedRequests;
    }

    List<String> getErrors() {
        return errors;
    }

    @Override
    public String getHeaderStringForFields() {
        return silentSuccessfulCount + "|" + getHeaderStringForFields(failedRequests) + "|" + getHeaderStringForFields(errors);
    }

    void incrementSilentSuccessCount() {
        silentSuccessfulCount++;
    }

    void resetSilentSuccessCount() {
        silentSuccessfulCount = 0;
    }

    private void appendError(final String errorCode) {
        errors.add(errorCode);
    }

    private void appendFailedRequest(final String apiId, final String correlationId) {
        failedRequests.add(new FailedRequest(apiId, correlationId));
    }

    void appendFailedRequestWithError(final String apiId, final String correlationId, final String errorCode) {
        appendFailedRequest(apiId, correlationId);
        appendError(errorCode);
    }

    private void appendFailedRequest(final FailedRequest failedRequest) {
        failedRequests.add(failedRequest);
    }

    void appendFailedRequestWithError(final FailedRequest failedRequest, final String errorCode) {
        appendFailedRequest(failedRequest);
        appendError(errorCode);
    }

    private void wipeFailedRequestForSubList(int index) {
        failedRequests = failedRequests.subList(index, failedRequests.size());
    }

    private void wipeErrorForSubList(int index) {
        errors = errors.subList(index, errors.size());
    }

    void wipeFailedRequestAndErrorForSubList(int index) {
        if (index < 0 || index > failedRequests.size() || index > errors.size()) {
            return;
        }

        wipeFailedRequestForSubList(index);
        wipeErrorForSubList(index);
    }

    @Override
    public RequestTelemetry derive(@NonNull final RequestTelemetry requestTelemetry) {
        if (requestTelemetry instanceof LastRequestTelemetry) {
            this.silentSuccessfulCount = ((LastRequestTelemetry) requestTelemetry).silentSuccessfulCount;
        }

        return super.derive(requestTelemetry);
    }
}
