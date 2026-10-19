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

import androidx.test.core.app.ApplicationProvider
import com.microsoft.identity.common.java.logging.DiagnosticContext
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class OnboardingRecorderRegistryTest {
    @Before
    fun setUp() {
        OnboardingRecorderRegistry.clearForTest()
    }

    @After
    fun tearDown() {
        OnboardingRecorderRegistry.clearForTest()
    }

    @Test
    fun `invalid correlation IDs never register or resolve`() {
        val recorder = recorder("session")

        assertFalse(OnboardingRecorderRegistry.register(null, recorder))
        assertFalse(OnboardingRecorderRegistry.register("", recorder))
        assertFalse(OnboardingRecorderRegistry.register("   ", recorder))
        assertFalse(
            OnboardingRecorderRegistry.register(
                DiagnosticContext.UNSET_CORRELATION_ID,
                recorder
            )
        )

        assertNull(OnboardingRecorderRegistry.get(null))
        assertNull(OnboardingRecorderRegistry.get(""))
        assertNull(OnboardingRecorderRegistry.get(DiagnosticContext.UNSET_CORRELATION_ID))
        assertEquals(0, OnboardingRecorderRegistry.size())
    }

    @Test
    fun `different correlation IDs resolve only their recorder`() {
        val first = recorder("first")
        val second = recorder("second")

        assertTrue(OnboardingRecorderRegistry.register("correlation-1", first))
        assertTrue(OnboardingRecorderRegistry.register("correlation-2", second))

        assertSame(first, OnboardingRecorderRegistry.get("correlation-1"))
        assertSame(second, OnboardingRecorderRegistry.get("correlation-2"))
    }

    @Test
    fun `same recorder registration is idempotent`() {
        val recorder = recorder("session")

        assertTrue(OnboardingRecorderRegistry.register("correlation", recorder))
        assertTrue(OnboardingRecorderRegistry.register("correlation", recorder))

        assertSame(recorder, OnboardingRecorderRegistry.get("correlation"))
        assertEquals(1, OnboardingRecorderRegistry.size())
    }

    @Test
    fun `different recorder cannot replace active entry`() {
        val active = recorder("active")
        val conflicting = recorder("conflicting")

        assertTrue(OnboardingRecorderRegistry.register("correlation", active))
        assertFalse(OnboardingRecorderRegistry.register("correlation", conflicting))

        assertSame(active, OnboardingRecorderRegistry.get("correlation"))
        assertEquals(1, OnboardingRecorderRegistry.size())
    }

    @Test
    fun `cleanup requires matching recorder identity and is repeatable`() {
        val active = recorder("active")
        val wrong = recorder("wrong")
        assertTrue(OnboardingRecorderRegistry.register("correlation", active))

        assertFalse(OnboardingRecorderRegistry.unregister("correlation", wrong))
        assertSame(active, OnboardingRecorderRegistry.get("correlation"))

        assertTrue(OnboardingRecorderRegistry.unregister("correlation", active))
        assertFalse(OnboardingRecorderRegistry.unregister("correlation", active))
        assertNull(OnboardingRecorderRegistry.get("correlation"))
        assertEquals(0, OnboardingRecorderRegistry.size())
    }

    @Test
    fun `registry does not reject or evict a seventeenth request`() {
        val recorders = (1..17).associateWith { recorder("session-$it") }

        recorders.forEach { (index, recorder) ->
            assertTrue(OnboardingRecorderRegistry.register("correlation-$index", recorder))
        }

        assertEquals(17, OnboardingRecorderRegistry.size())
        recorders.forEach { (index, recorder) ->
            assertSame(recorder, OnboardingRecorderRegistry.get("correlation-$index"))
        }
    }

    private fun recorder(sessionId: String) = OnboardingTelemetryRecorder(
        seedJson = """
            {
              "schema_version":"1.0.0",
              "session_correlation_id":"$sessionId",
              "onboarding_mode":"non-brokered"
            }
        """.trimIndent(),
        clientId = "client-id",
        target = "scope",
        context = ApplicationProvider.getApplicationContext()
    )
}
