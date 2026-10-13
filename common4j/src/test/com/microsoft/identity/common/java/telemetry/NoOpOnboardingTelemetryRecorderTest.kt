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

import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

/**
 * Unit tests for [NoOpOnboardingTelemetryRecorder] — the null-object implementation of
 * [IOnboardingTelemetryRecorder]. The recorder must swallow every recording call and always
 * finalize to an empty (never null) blob, so callers can hold it as a `@NonNull` field.
 */
@RunWith(JUnit4::class)
class NoOpOnboardingTelemetryRecorderTest {

    private val recorder: IOnboardingTelemetryRecorder = NoOpOnboardingTelemetryRecorder

    @Test
    fun finalizeBlob_returnsEmptyBlobSentinel() {
        assertEquals(IOnboardingTelemetryRecorder.EMPTY_BLOB, recorder.finalizeBlob())
    }

    @Test
    fun recordingCalls_doNotThrow_whenInvokedUnguarded() {
        // The broker change this null-object exists for converts mOnboardingRecorder to @NonNull
        // and deletes every `if (recorder != null)` guard, so production code now calls straight
        // into these methods on INSTANCE. That makes "a no-op body never throws" a real contract
        // rather than a triviality: replacing either body with TODO() or an
        // UnsupportedOperationException — the wrong turn a "not supported" stub would take — would
        // crash the brokered flow instead of silently doing nothing.
        recorder.addStep(OnboardingTelemetryConstants.STEP_AUTHENTICATION_STARTED)
        recorder.addStep(OnboardingTelemetryConstants.STEP_TOKEN_ISSUED)
        recorder.addBlockingError(OnboardingTelemetryConstants.BLOCKING_ERROR_DEVICE_REGISTRATION_NEEDED)

        // Recording left nothing worth emitting, so the caller's `blob.isEmpty()` skip still holds.
        assertEquals(IOnboardingTelemetryRecorder.EMPTY_BLOB, recorder.finalizeBlob())
    }
}
