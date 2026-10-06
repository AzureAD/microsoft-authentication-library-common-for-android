// Copyright (c) Microsoft Corporation.
// Licensed under the MIT License.

package com.microsoft.identity.common.java.opentelemetry

import io.opentelemetry.api.OpenTelemetry
import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

/** Exercises the process latch without adding a production reset API. */
class OpenTelemetryHolderLifecycleTest {
    private val instanceField = OpenTelemetryHolder::class.java
        .getDeclaredField("sOpenTelemetry").apply { isAccessible = true }
    private val disabledField = OpenTelemetryHolder::class.java
        .getDeclaredField("sDisabled").apply { isAccessible = true }
    private var previousInstance: Any? = null
    private var previouslyDisabled = false

    @Before
    fun setUp() {
        previousInstance = instanceField.get(null)
        previouslyDisabled = disabledField.getBoolean(null)
        instanceField.set(null, OpenTelemetry.noop())
        disabledField.setBoolean(null, false)
    }

    @After
    fun tearDown() {
        instanceField.set(null, previousInstance)
        disabledField.setBoolean(null, previouslyDisabled)
    }

    @Test
    fun legacySetterPreservesEnabledBehavior() {
        val instance = mock(OpenTelemetry::class.java)
        OpenTelemetryHolder.setOpenTelemetry(instance)
        assertSame(instance, OpenTelemetryHolder.getOpenTelemetry())
        assertFalse(OpenTelemetryHolder.isDisabled())
    }

    @Test
    fun disableReturnsActualInstanceAndPublishesNoop() {
        val instance = mock(OpenTelemetry::class.java)
        assertTrue(OpenTelemetryHolder.trySetOpenTelemetry(instance))
        assertSame(instance, OpenTelemetryHolder.disable())
        assertTrue(OpenTelemetryHolder.isDisabled())
        assertFalse(OpenTelemetryHolder.getTracer("test").spanBuilder("test").startSpan().isRecording)
        OpenTelemetryHolder.getMeter("test").counterBuilder("counter").build().add(1)
    }

    @Test
    fun repeatedDisableAndLegacySetterCannotResurrect() {
        OpenTelemetryHolder.disable()
        val noop = OpenTelemetryHolder.getOpenTelemetry()
        assertSame(noop, OpenTelemetryHolder.disable())
        OpenTelemetryHolder.setOpenTelemetry(mock(OpenTelemetry::class.java))
        assertSame(noop, OpenTelemetryHolder.getOpenTelemetry())
    }

    @Test
    fun rejectedInstallationLeavesOwnershipWithCaller() {
        OpenTelemetryHolder.disable()
        val noop = OpenTelemetryHolder.getOpenTelemetry()
        assertFalse(OpenTelemetryHolder.trySetOpenTelemetry(mock(OpenTelemetry::class.java)))
        assertSame(noop, OpenTelemetryHolder.getOpenTelemetry())
    }
}
