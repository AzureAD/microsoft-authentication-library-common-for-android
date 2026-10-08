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
import com.microsoft.identity.common.java.exception.ClientException
import com.microsoft.identity.common.java.result.AcquireTokenResult
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.*
import org.robolectric.RobolectricTestRunner
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

@RunWith(RobolectricTestRunner::class)
class OnboardingTelemetryRequestTest {
    @Before
    fun setUp() = OnboardingRecorderRegistry.clearForTest()

    @After
    fun tearDown() = OnboardingRecorderRegistry.clearForTest()

    @Test
    fun brokerAndNonbrokerOwnersRetainRecorderAcrossViewsAndRemediation() {
        listOf("brokered", "non-brokered").forEach { mode ->
            val recorder = newRecorder(mode)
            val owner = OnboardingTelemetryRequest(recorder)
            try {
                assertTrue(owner.register("authorization-1"))
                assertSame(recorder, OnboardingRecorderRegistry.get("authorization-1"))
                recorder.addStep("first-view")
                recorder.addBlockingError("remediation")
                assertTrue(owner.register("authorization-2"))
                assertSame(recorder, OnboardingRecorderRegistry.get("authorization-2"))
                recorder.addStep("resumed-view")
                verify(recorder, never()).finalizeBlob()

                val result = AcquireTokenResult()
                owner.complete(result)
                val json = JSONObject(result.onboardingBlob!!)
                assertEquals(mode, json.getString("onboarding_mode"))
                assertEquals(2, json.getJSONArray("steps_list").length())
                assertEquals(1, json.getJSONArray("blocking_errors").length())
                assertNull(OnboardingRecorderRegistry.get("authorization-1"))
                assertNull(OnboardingRecorderRegistry.get("authorization-2"))
                verify(recorder, times(1)).finalizeBlob()
            } finally {
                owner.close()
            }
        }
    }

    @Test
    fun terminalSnapshotIsCachedAcrossResultAndExceptionAndCannotReopen() {
        val recorder = newRecorder()
        val owner = OnboardingTelemetryRequest(recorder)
        assertTrue(owner.register("request"))
        val result = AcquireTokenResult()
        owner.complete(result)
        recorder.addStep("too-late")
        val failure = ClientException("failure")
        owner.complete(failure)
        owner.complete(result)
        owner.close()
        assertEquals(result.onboardingBlob, failure.onboardingBlob)
        assertFalse(owner.register("retry-after-terminal"))
        verify(recorder, times(1)).finalizeBlob()
        assertEquals(0, OnboardingRecorderRegistry.size())
    }

    @Test
    fun finallyCleansLaunchFailureWithoutFinalizingIntermediateInterruption() {
        val recorder = newRecorder()
        val owner = OnboardingTelemetryRequest(recorder)
        try {
            assertTrue(owner.register("launch"))
        } finally {
            owner.close()
        }
        assertNull(OnboardingRecorderRegistry.get("launch"))
        verify(recorder, never()).finalizeBlob()
        assertNull(owner.finalizeOnce())
    }

    @Test
    fun serializationFailureIsBestEffortAndNotRetried() {
        val recorder = newRecorder()
        doThrow(IllegalStateException("serialization")).`when`(recorder).finalizeBlob()
        val owner = OnboardingTelemetryRequest(recorder)
        owner.register("failure")
        val result = AcquireTokenResult()
        owner.complete(result)
        owner.complete(ClientException("failure"))
        assertNull(result.onboardingBlob)
        assertNull(OnboardingRecorderRegistry.get("failure"))
        verify(recorder, times(1)).finalizeBlob()
    }

    @Test
    fun emptySnapshotIsNotRetriedOrAttached() {
        val recorder = newRecorder()
        doReturn("").`when`(recorder).finalizeBlob()
        val owner = OnboardingTelemetryRequest(recorder)
        owner.register("empty")
        val result = AcquireTokenResult()
        owner.complete(result)
        owner.complete(ClientException("failure"))
        assertNull(result.onboardingBlob)
        assertEquals(0, OnboardingRecorderRegistry.size())
        verify(recorder, times(1)).finalizeBlob()
    }

    @Test
    fun staleOwnerCloseDoesNotRemoveNewRegistration() {
        val first = OnboardingTelemetryRequest(newRecorder())
        val second = OnboardingTelemetryRequest(newRecorder())
        assertTrue(first.register("same"))
        OnboardingRecorderRegistry.unregister("same")
        assertTrue(second.register("same"))
        first.close()
        assertSame(second.recorder, OnboardingRecorderRegistry.get("same"))
        second.close()
    }

    @Test
    fun rejectedRegistrationDoesNotRemoveAnotherOwner() {
        val first = OnboardingTelemetryRequest(newRecorder())
        val second = OnboardingTelemetryRequest(newRecorder())
        assertTrue(first.register("same"))
        assertFalse(second.register("same"))
        second.complete(AcquireTokenResult())
        assertSame(first.recorder, OnboardingRecorderRegistry.get("same"))
        first.close()
    }

    @Test
    fun concurrentIndependentRequestsKeepTheirOwnRecorderAndCleanup() {
        val executor = Executors.newFixedThreadPool(4)
        val ready = CountDownLatch(4)
        try {
            val futures = (1..4).map { index ->
                val recorder = newRecorder()
                executor.submit {
                    OnboardingTelemetryRequest(recorder).use { owner ->
                        assertTrue(owner.register("request-$index"))
                        ready.countDown()
                        assertTrue(ready.await(10, TimeUnit.SECONDS))
                        assertSame(recorder, OnboardingRecorderRegistry.get("request-$index"))
                        owner.complete(AcquireTokenResult())
                        verify(recorder, times(1)).finalizeBlob()
                    }
                }
            }
            futures.forEach { it.get(15, TimeUnit.SECONDS) }
            assertEquals(0, OnboardingRecorderRegistry.size())
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun concurrentCompletionFinalizesOnceAndAttachesSameSnapshot() {
        val recorder = newRecorder()
        val owner = OnboardingTelemetryRequest(recorder)
        owner.register("request")
        val executor = Executors.newFixedThreadPool(4)
        try {
            val futures = (1..4).map {
                executor.submit<String?> {
                    val result = AcquireTokenResult()
                    owner.complete(result)
                    result.onboardingBlob
                }
            }
            val blobs = futures.map { it.get(15, TimeUnit.SECONDS) }
            assertNotNull(blobs.first())
            assertTrue(blobs.all { it == blobs.first() })
            verify(recorder, times(1)).finalizeBlob()
            assertEquals(0, OnboardingRecorderRegistry.size())
        } finally {
            executor.shutdownNow()
            owner.close()
        }
    }

    private fun newRecorder(mode: String = "brokered") = spy(
        OnboardingTelemetryRecorder(
            """{"schema_version":"1.0.0","session_correlation_id":"session","onboarding_mode":"$mode"}""",
            "client", "scope", ApplicationProvider.getApplicationContext()
        )
    )
}
