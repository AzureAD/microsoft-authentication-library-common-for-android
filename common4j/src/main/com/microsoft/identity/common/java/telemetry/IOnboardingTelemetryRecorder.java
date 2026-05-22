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
 * {@code broker4j}. This interface exposes only the recording surface — {@link #addStep}
 * and {@link #addBlockingError} — so broker4j code (e.g. interactive error handlers,
 * controllers) can populate the recorder without taking an Android dependency.
 *
 * <p>The owning Android-side caller (e.g. {@code AccountChooserActivity}) constructs the
 * concrete recorder from the seed JSON, passes the {@code IOnboardingTelemetryRecorder}
 * view down through broker4j call sites, and calls {@code finalizeBlob()} on the concrete
 * recorder once the flow completes.
 */
public interface IOnboardingTelemetryRecorder {

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
     * @param errorCode Blocking-error constant from
     *                  {@link com.microsoft.identity.common.java.telemetry.OnboardingTelemetryConstants}
     *                  (e.g. {@code BLOCKING_ERROR_DEVICE_REGISTRATION_NEEDED}). Not a numeric
     *                  service auth error code.
     */
    void addBlockingError(@NonNull String errorCode);
}
