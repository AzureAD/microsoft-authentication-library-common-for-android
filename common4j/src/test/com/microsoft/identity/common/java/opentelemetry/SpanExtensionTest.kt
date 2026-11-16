// Copyright (c) Microsoft Corporation.
// All rights reserved.
//
// Licensed under the MIT License.

package com.microsoft.identity.common.java.opentelemetry

import io.opentelemetry.api.common.Attributes
import io.opentelemetry.api.trace.Span
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions

/**
 * Tests the event payload emitted by the span outcome helpers.
 */
class SpanExtensionTest {
    @Test
    fun recordOutcome_withoutException_recordsOnlyOutcome() {
        val span = mock(Span::class.java)

        SpanExtension.recordOutcome(span, "browser_launch", "succeeded")

        verify(span).addEvent(
            "browser_launch",
            Attributes.builder()
                .put(AttributeName.operation_outcome.name, "succeeded")
                .build()
        )
        verifyNoMoreInteractions(span)
    }

    @Test
    fun recordOutcome_withException_recordsOutcomeAndExceptionType() {
        val span = mock(Span::class.java)

        SpanExtension.recordOutcome(span, "vc_dispatch", "failed", "SecurityException")

        verify(span).addEvent(
            "vc_dispatch",
            Attributes.builder()
                .put(AttributeName.operation_outcome.name, "failed")
                .put(AttributeName.error_type.name, "SecurityException")
                .build()
        )
        verifyNoMoreInteractions(span)
    }

    @Test
    fun recordOutcome_fallback_recordsBothDecisionsWithoutChangingSpanStatus() {
        val span = mock(Span::class.java)

        SpanExtension.recordOutcome(span, "browser_launch", "failed", "ActivityNotFoundException")
        SpanExtension.recordOutcome(span, "browser_fallback", "inline_fallback_requested")

        val ordered = org.mockito.Mockito.inOrder(span)
        ordered.verify(span).addEvent(
            "browser_launch",
            Attributes.builder()
                .put(AttributeName.operation_outcome.name, "failed")
                .put(AttributeName.error_type.name, "ActivityNotFoundException")
                .build()
        )
        ordered.verify(span).addEvent(
            "browser_fallback",
            Attributes.builder()
                .put(AttributeName.operation_outcome.name, "inline_fallback_requested")
                .build()
        )
        verifyNoMoreInteractions(span)
    }
}
