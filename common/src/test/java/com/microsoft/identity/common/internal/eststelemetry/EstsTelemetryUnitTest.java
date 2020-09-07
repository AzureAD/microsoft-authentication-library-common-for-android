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

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import java.util.List;
import java.util.UUID;

import static com.microsoft.identity.common.internal.eststelemetry.LastRequestTelemetry.FAILED_REQUEST_CAP;

@RunWith(RobolectricTestRunner.class)
public class EstsTelemetryUnitTest {

    @Test
    public void LastRequestTelemetryFailedRequestListIsCapped() {
        final LastRequestTelemetry lastRequestTelemetry = new LastRequestTelemetry(
                SchemaConstants.CURRENT_SCHEMA_VERSION
        );

        for (int i = 0; i < (FAILED_REQUEST_CAP + 20); i++) {
            final String apiId = "fake-api-id";
            final String correlation = UUID.randomUUID().toString();
            final String errorCode = "fake-error-code";
            final FailedRequest failedRequest = new FailedRequest(apiId, correlation, errorCode);
            lastRequestTelemetry.appendFailedRequest(failedRequest);
            final List<FailedRequest> failedRequests = lastRequestTelemetry.getFailedRequests();
            Assert.assertTrue(failedRequests.size() <= FAILED_REQUEST_CAP);
        }

        Assert.assertEquals(FAILED_REQUEST_CAP, lastRequestTelemetry.getFailedRequests().size());

    }


}
