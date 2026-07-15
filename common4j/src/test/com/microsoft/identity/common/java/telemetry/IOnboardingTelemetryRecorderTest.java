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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;

/**
 * Unit tests for the {@link IOnboardingTelemetryRecorder} default {@link
 * IOnboardingTelemetryRecorder#finalizeBlob()} method.
 *
 * <p>{@code finalizeBlob()} is a {@code default} method so that adding it to the already-released
 * interface stays binary-compatible (see AB#3647677). These tests pin that contract: an
 * implementation that only supplies the recording surface inherits the empty-blob default, and an
 * implementation that overrides {@code finalizeBlob()} is honored.
 */
@RunWith(JUnit4.class)
public class IOnboardingTelemetryRecorderTest {

    /**
     * A minimal recorder that implements only the abstract recording surface ({@link
     * IOnboardingTelemetryRecorder#addStep} / {@link IOnboardingTelemetryRecorder#addBlockingError})
     * and inherits the default {@code finalizeBlob()}.
     */
    private static final class MinimalRecorder implements IOnboardingTelemetryRecorder {
        private final List<String> steps = new ArrayList<>();
        private final List<String> blockingErrors = new ArrayList<>();

        @Override
        public void addStep(final String stepId) {
            steps.add(stepId);
        }

        @Override
        public void addBlockingError(final String errorCode) {
            blockingErrors.add(errorCode);
        }
    }

    @Test
    public void testDefaultFinalizeBlob_ReturnsEmptyString() {
        // A minimal implementer that does not override finalizeBlob() inherits the default, which
        // must return the empty-blob sentinel rather than throwing AbstractMethodError. This is the
        // binary-compatibility guarantee that lets finalizeBlob() be added to the released interface
        // as a MINOR change.
        final IOnboardingTelemetryRecorder recorder = new MinimalRecorder();

        // The recording surface still works, and the inherited default yields an empty blob.
        recorder.addStep("AuthenticationStarted");
        recorder.addBlockingError("BROKER_INSTALLATION_TRIGGERED");

        assertEquals("", recorder.finalizeBlob());
    }

    @Test
    public void testOverriddenFinalizeBlob_IsHonored() {
        // An implementation that overrides finalizeBlob() replaces the default; callers programming
        // to the interface get the override's result with no downcast.
        final String expected = "{\"session_correlation_id\":\"abc-123\"}";
        final IOnboardingTelemetryRecorder recorder = new IOnboardingTelemetryRecorder() {
            @Override
            public void addStep(final String stepId) {
                // no-op
            }

            @Override
            public void addBlockingError(final String errorCode) {
                // no-op
            }

            @Override
            public String finalizeBlob() {
                return expected;
            }
        };

        assertEquals(expected, recorder.finalizeBlob());
    }
}
