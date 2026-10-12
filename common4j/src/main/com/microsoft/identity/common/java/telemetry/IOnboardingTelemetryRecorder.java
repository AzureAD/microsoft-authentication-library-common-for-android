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
package com.microsoft.identity.common.java.telemetry;

import lombok.NonNull;

/**
 * Common4j-visible facet of the onboarding telemetry recorder.
 *
 * <p>The concrete recorder (Kotlin {@code OnboardingTelemetryRecorder} in the {@code common}
 * Android module) depends on Android {@code Context} for SharedPreferences-backed session
 * correlation persistence, which makes it unavailable to pure-Java modules like
 * {@code broker4j}. This interface exposes the recording surface — {@link #addStep} and
 * {@link #addBlockingError} — together with terminal {@link #finalizeBlob() finalization}, so
 * broker4j code (e.g. interactive error handlers, controllers) can populate the recorder, and
 * the owning Android-side caller can finalize it, without taking an Android dependency or
 * downcasting to the concrete recorder.
 *
 * <p>The owning Android-side caller (e.g. {@code AccountChooserActivity}) constructs the
 * concrete recorder from the seed JSON, passes the {@code IOnboardingTelemetryRecorder}
 * view down through broker4j call sites, and calls {@link #finalizeBlob()} through this
 * interface once the flow completes.
 */
public interface IOnboardingTelemetryRecorder {

    /**
     * Sentinel returned by {@link #finalizeBlob()} when there is nothing worth emitting (e.g. the
     * session correlation id is missing, so the blob could not be joined with the broker side or
     * with retries).
     *
     * <p>Because {@link #finalizeBlob()} is {@code @NonNull}, this "nothing to emit" value is part
     * of the interface contract, not an implementation detail: callers decide whether to skip MATS
     * emission with {@code blob.isEmpty()} (or equivalent), which only holds if every implementation
     * agrees the vacuum value is exactly {@code ""}. Exposing it here gives every implementation
     * (the concrete {@code OnboardingTelemetryRecorder}, a no-op / null-object recorder, test
     * doubles) one canonical constant to return instead of re-declaring the magic string.
     */
    @NonNull String EMPTY_BLOB = "";

    /**
     * Record a step in the onboarding flow. The implementation captures a timestamp
     * for each step internally.
     *
     * @param stepId Step ID constant from
     *               {@link com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants}
     *               (e.g. {@code STEP_AUTHENTICATION_STARTED}).
     */
    void addStep(@NonNull String stepId);

    /**
     * Record a blocking onboarding error detected during the flow.
     *
     * @param errorCode Blocking-error identifier to record. Either a symbolic blocking-error
     *                  constant from
     *                  {@link com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants}
     *                  (e.g. {@code BLOCKING_ERROR_DEVICE_REGISTRATION_NEEDED}), or a numeric
     *                  server/STS error code surfaced by
     *                  {@link com.microsoft.identity.common.java.telemetry.OnboardingBlockingErrorParser}
     *                  or the Auth UX JS bridge (e.g. {@code "530003"}). Recorded verbatim as an
     *                  opaque string.
     */
    void addBlockingError(@NonNull String errorCode);

    /**
     * Finalize the onboarding flow and serialize the accumulated telemetry into the populated
     * blob JSON string.
     *
     * <p>Called by the owning Android-side caller (e.g. {@code AccountChooserActivity}) on the
     * terminal brokered outcome, once all steps and blocking errors have been recorded. Because
     * this lives on the interface, callers finalize through the {@code IOnboardingTelemetryRecorder}
     * view without downcasting to the concrete recorder, so every implementation — including a
     * no-op / null-object recorder — participates in finalization explicitly rather than being
     * silently skipped.
     *
     * <p>Declared as a {@code default} method (returning the empty-blob sentinel) rather than an
     * abstract one to preserve binary compatibility: {@code IOnboardingTelemetryRecorder} ships in
     * released {@code common4j} artifacts (since 24.3.0), so adding an abstract method would break
     * any downstream implementation compiled against an earlier version (surfacing as an
     * {@link AbstractMethodError} at runtime) and would make this a MAJOR rather than a MINOR
     * change. The concrete {@code OnboardingTelemetryRecorder} overrides this to emit the real
     * blob; the default simply yields the documented "nothing worth emitting" result.
     *
     * @return The populated blob JSON string, or {@link #EMPTY_BLOB} when there is nothing worth
     *         emitting (e.g. the session correlation id is missing). Never {@code null}.
     */
    @NonNull
    default String finalizeBlob() {
        return EMPTY_BLOB;
    }
}
