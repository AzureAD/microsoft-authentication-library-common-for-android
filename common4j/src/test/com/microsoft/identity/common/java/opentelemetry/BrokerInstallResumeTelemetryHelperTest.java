//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

package com.microsoft.identity.common.java.opentelemetry;

import org.junit.Test;

/**
 * Smoke tests for {@link BrokerInstallResumeTelemetryHelper} (PBI-4). Verifies the funnel lifecycle
 * methods run against the default (no-op) OpenTelemetry span without throwing, on both the success and
 * failure paths. Per repo guidance we do not assert individual attribute attachment.
 */
public class BrokerInstallResumeTelemetryHelperTest {

    @Test
    public void successFunnel_runsWithoutThrowing() {
        final BrokerInstallResumeTelemetryHelper helper = new BrokerInstallResumeTelemetryHelper();
        helper.setCorrelationId("3f2504e0-4f89-11d3-9a0c-0305e82c3301");
        helper.onParked();
        helper.onReferrerFired();
        helper.onResumeReceived();
        helper.onRetrySuccess();
        helper.onDelivered();
    }

    @Test
    public void failureWithReason_runsWithoutThrowing() {
        final BrokerInstallResumeTelemetryHelper helper = new BrokerInstallResumeTelemetryHelper();
        helper.onParked();
        helper.onFailed(BrokerInstallResumeTelemetryHelper.STAGE_RETRY_SUCCESS, "silent_retry_timeout");
    }

    @Test
    public void failureWithException_runsWithoutThrowing() {
        final BrokerInstallResumeTelemetryHelper helper = new BrokerInstallResumeTelemetryHelper();
        helper.onParked();
        helper.onFailed(BrokerInstallResumeTelemetryHelper.STAGE_RESUME_RECEIVED,
                new IllegalStateException("boom"));
    }

    @Test
    public void resumeReceivedNoPark_runsWithoutThrowing() {
        final BrokerInstallResumeTelemetryHelper helper = new BrokerInstallResumeTelemetryHelper();
        helper.onResumeReceivedNoPark();
    }

    @Test
    public void setCorrelationId_nullOrEmpty_isNoOp() {
        final BrokerInstallResumeTelemetryHelper helper = new BrokerInstallResumeTelemetryHelper();
        helper.setCorrelationId(null);
        helper.setCorrelationId("");
        helper.onDelivered();
    }
}
