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
package com.microsoft.identity.common.java.telemetry

/**
 * Null-object implementation of [IOnboardingTelemetryRecorder].
 *
 * The two recording entry points are no-ops: [addStep] and [addBlockingError] discard their input.
 * `finalizeBlob()` is intentionally not overridden — the interface's own `default` already returns
 * [IOnboardingTelemetryRecorder.EMPTY_BLOB], so the null-object inherits that identical
 * "nothing worth emitting" result (an empty blob, exactly what the concrete recorder returns when
 * it has no joinable session correlation id).
 *
 * This lets callers treat the recorder as always-present (`@NonNull`) and drop the scattered
 * `if (recorder != null)` guards: when a request carries no onboarding seed JSON (older clients,
 * standalone broker flows), `INSTANCE` is substituted for `null` and every step / blocking-error
 * emission and blob finalization stays a silent no-op.
 *
 * The recorder is stateless, so a single shared `INSTANCE` is exposed (accessed from Java as
 * `NoOpOnboardingTelemetryRecorder.INSTANCE`).
 */
object NoOpOnboardingTelemetryRecorder : IOnboardingTelemetryRecorder {

    override fun addStep(stepId: String) {
        // No-op: this recorder intentionally discards onboarding steps.
    }

    override fun addBlockingError(errorCode: String) {
        // No-op: this recorder intentionally discards blocking errors.
    }

    // finalizeBlob() is intentionally not overridden: IOnboardingTelemetryRecorder's default
    // implementation already returns EMPTY_BLOB, which is exactly the null-object's behavior.
}
