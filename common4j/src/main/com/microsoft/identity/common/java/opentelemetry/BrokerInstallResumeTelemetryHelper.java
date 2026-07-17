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
package com.microsoft.identity.common.java.opentelemetry;

import com.microsoft.identity.common.java.util.StringUtil;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import lombok.NonNull;

/**
 * Stateful telemetry helper for the MAM broker-install request-resume funnel (PBI-4). Owns a single
 * {@link SpanName#BrokerInstallResume} span across the park -&gt; resume boundary (which spans different
 * callbacks and a Play Store round-trip), stamping a boolean per stage plus a {@code broker_install_resume_stage}
 * string, and terminating the span exactly once with {@link StatusCode#OK} on delivery or
 * {@link StatusCode#ERROR} on failure.
 * <p>
 * Modeled on {@link CertBasedAuthTelemetryHelper}. Emitted only on the flighted resume path.
 */
public class BrokerInstallResumeTelemetryHelper {

    /** Funnel stage values for {@link AttributeName#broker_install_resume_stage}. */
    public static final String STAGE_PARKED = "parked";
    public static final String STAGE_REFERRER_FIRED = "referrer_fired";
    public static final String STAGE_RESUME_RECEIVED = "resume_received";
    public static final String STAGE_RETRY_SUCCESS = "retry_success";
    public static final String STAGE_DELIVERED = "delivered";

    private final Span mSpan;

    /**
     * @param spanContext the parent span context (e.g. the interactive/ATS span).
     */
    public BrokerInstallResumeTelemetryHelper(@NonNull final SpanContext spanContext) {
        mSpan = OTelUtility.createSpanFromParent(SpanName.BrokerInstallResume.name(), spanContext);
    }

    /**
     * Use when no parent span context is available.
     */
    public BrokerInstallResumeTelemetryHelper() {
        mSpan = OTelUtility.createSpan(SpanName.BrokerInstallResume.name());
    }

    /**
     * Stamps the correlation id for joinability with the interactive/ATS span.
     *
     * @param correlationId the request correlation id.
     */
    public void setCorrelationId(final String correlationId) {
        if (!StringUtil.isNullOrEmpty(correlationId)) {
            mSpan.setAttribute(AttributeName.correlation_id.name(), correlationId);
        }
    }

    /** Stage 1: the interactive request was parked. */
    public void onParked() {
        mSpan.setAttribute(AttributeName.broker_install_resume_parked.name(), true);
        mSpan.setAttribute(AttributeName.broker_install_resume_stage.name(), STAGE_PARKED);
    }

    /** Stage 2: the Play Store launch carrying the install referrer was fired. */
    public void onReferrerFired() {
        mSpan.setAttribute(AttributeName.broker_install_resume_referrer_fired.name(), true);
        mSpan.setAttribute(AttributeName.broker_install_resume_stage.name(), STAGE_REFERRER_FIRED);
    }

    /** Stage 3: the mam_resume redirect was received and matched a parked request. */
    public void onResumeReceived() {
        mSpan.setAttribute(AttributeName.broker_install_resume_resume_received.name(), true);
        mSpan.setAttribute(AttributeName.broker_install_resume_stage.name(), STAGE_RESUME_RECEIVED);
    }

    /**
     * A resume redirect arrived but no parked request matched (process-death indicator). Terminates the
     * span with an error status.
     */
    @SuppressFBWarnings
    public void onResumeReceivedNoPark() {
        mSpan.setAttribute(AttributeName.broker_install_resume_no_park.name(), true);
        mSpan.setStatus(StatusCode.ERROR, "resume received but no parked request matched");
        mSpan.end();
    }

    /** Stage 4: the silent broker retry succeeded. */
    public void onRetrySuccess() {
        mSpan.setAttribute(AttributeName.broker_install_resume_retry_success.name(), true);
        mSpan.setAttribute(AttributeName.broker_install_resume_stage.name(), STAGE_RETRY_SUCCESS);
    }

    /**
     * Stage 5: the token was delivered to the app's original callback. Terminates the span with OK.
     */
    @SuppressFBWarnings
    public void onDelivered() {
        mSpan.setAttribute(AttributeName.broker_install_resume_delivered.name(), true);
        mSpan.setAttribute(AttributeName.broker_install_resume_stage.name(), STAGE_DELIVERED);
        mSpan.setStatus(StatusCode.OK);
        mSpan.end();
    }

    /**
     * Records a failure at the given funnel stage and terminates the span with an error status.
     *
     * @param stage  the funnel stage at which the failure occurred (one of the STAGE_* constants).
     * @param reason a bounded, non-PII failure reason.
     */
    @SuppressFBWarnings
    public void onFailed(@NonNull final String stage, @NonNull final String reason) {
        mSpan.setAttribute(AttributeName.broker_install_resume_stage.name(), stage);
        mSpan.setAttribute(AttributeName.broker_install_resume_failure_reason.name(), reason);
        mSpan.setStatus(StatusCode.ERROR, reason);
        mSpan.end();
    }

    /**
     * Records a failure with an exception at the given funnel stage and terminates the span with an
     * error status.
     *
     * @param stage     the funnel stage at which the failure occurred.
     * @param throwable the exception that caused the failure.
     */
    @SuppressFBWarnings
    public void onFailed(@NonNull final String stage, @NonNull final Throwable throwable) {
        mSpan.setAttribute(AttributeName.broker_install_resume_stage.name(), stage);
        mSpan.recordException(throwable);
        mSpan.setStatus(StatusCode.ERROR);
        mSpan.end();
    }
}
