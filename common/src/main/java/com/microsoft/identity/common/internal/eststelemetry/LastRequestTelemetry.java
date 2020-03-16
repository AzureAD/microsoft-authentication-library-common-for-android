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

    void incrementSilentSuccessCount() {
        mSilentSuccessfulCount++;
    }

    void resetSilentSuccessCount() {
        mSilentSuccessfulCount = 0;
    }

    private void appendError(final String errorCode) {
        mErrors.add(errorCode);
    }

    private void appendFailedRequest(final String apiId, final String correlationId) {
        failedRequests.add(new FailedRequest(apiId, correlationId));
    }

    void appendFailedRequestWithError(final String apiId, final String correlationId, final String errorCode) {
        appendFailedRequest(apiId, correlationId);
        appendError(errorCode);
    }

    void wipeFailedRequestData() {
        failedRequests.clear();
        mErrors.clear();
    }
}
