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
package com.microsoft.identity.common.internal.telemetry

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.java.logging.DiagnosticContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingRecorderRegistryTest {
    @Before
    fun setUp() = OnboardingRecorderRegistry.clearForTest()

    @After
    fun tearDown() = OnboardingRecorderRegistry.clearForTest()

    @Test
    fun registerGetAndUnregisterUseCorrelationId() {
        val first = newRecorder()
        val second = newRecorder()
        OnboardingRecorderRegistry.register("first", first)
        OnboardingRecorderRegistry.register("second", second)

        assertSame(first, OnboardingRecorderRegistry.get("first"))
        assertSame(second, OnboardingRecorderRegistry.get("second"))

        OnboardingRecorderRegistry.unregister("first")
        assertNull(OnboardingRecorderRegistry.get("first"))
        assertSame(second, OnboardingRecorderRegistry.get("second"))
    }

    @Test
    fun unusableCorrelationIdsAreRejected() {
        OnboardingRecorderRegistry.register(null, newRecorder())
        OnboardingRecorderRegistry.register("", newRecorder())
        OnboardingRecorderRegistry.register(
            DiagnosticContext.UNSET_CORRELATION_ID,
            newRecorder()
        )

        assertEquals(0, OnboardingRecorderRegistry.size())
    }

    @Test
    fun registryIsBounded() {
        repeat(100) {
            OnboardingRecorderRegistry.register("correlation-$it", newRecorder())
        }

        assertEquals(16, OnboardingRecorderRegistry.size())
        assertNull(OnboardingRecorderRegistry.get("correlation-0"))
    }

    private fun newRecorder() = OnboardingTelemetryRecorder(
        """{"schema_version":"1.0.0","session_correlation_id":"id","onboarding_mode":"brokered"}""",
        "client",
        "scope",
        ApplicationProvider.getApplicationContext<Context>()
    )
}
