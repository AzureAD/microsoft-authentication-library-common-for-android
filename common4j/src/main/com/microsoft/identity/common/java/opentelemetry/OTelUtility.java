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
package com.microsoft.identity.common.java.opentelemetry;

import static com.microsoft.identity.common.java.opentelemetry.AttributeName.parent_span_name;

import com.microsoft.identity.common.java.logging.Logger;

import javax.annotation.Nullable;

import io.opentelemetry.api.GlobalOpenTelemetry;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.sdk.trace.ReadableSpan;
import lombok.NonNull;

public class OTelUtility {
    private static final String TAG = OTelUtility.class.getSimpleName();

    /**
     * Creates a span (with shared basic attributes).
     **/
    @NonNull
    public static Span createSpan(@NonNull final String name) {
        final Tracer tracer = GlobalOpenTelemetry.getTracer(TAG);
        final Span span = tracer.spanBuilder(name).startSpan();

        // Current span is the parent of span just created.
        span.setAttribute(parent_span_name.name(), getCurrentSpanName());
        return span;
    }

    /**
     * Creates a span from a parent Span Context (with shared basic attributes).
     **/
    @NonNull
    public static Span createSpanFromParent(@NonNull final String name,
                                            @Nullable final SpanContext parentSpanContext) {
        final String methodTag = TAG + ":createSpanFromParent";

        if (parentSpanContext == null) {
            Logger.verbose(methodTag, "parentSpanContext is NULL. Creating span without parent.");
            return createSpan(name);
        }

        if (!parentSpanContext.isValid()) {
            Logger.warn(methodTag, "parentSpanContext is INVALID. Creating span without parent.");
            return createSpan(name);
        }

        final Tracer tracer = GlobalOpenTelemetry.getTracer(TAG);
        final Span span = tracer.spanBuilder(name)
                .setParent(Context.current().with(Span.wrap(parentSpanContext)))
                .startSpan();

        if (parentSpanContext instanceof SerializableSpanContext) {
            span.setAttribute(parent_span_name.name(), ((SerializableSpanContext) parentSpanContext).getParentSpanName());
        }

        return span;
    }

    /**
     * Creates a span (with shared basic attributes).
     **/
    @NonNull
    public static LongCounter createLongCounter(@NonNull final String name, @NonNull final String description) {
        final Meter meter = GlobalOpenTelemetry.getMeter(TAG);

        return meter
                .counterBuilder(name)
                .setDescription(description)
                .setUnit("count")
                .build();
    }

    /**
     * Get name of the current span, if possible.
     **/
    @Nullable
    public static Attributes getCurrentSpanAttributes() {
        final Span span = SpanExtension.current();
        if (span instanceof ReadableSpan) {
            return ((ReadableSpan) span).toSpanData().getAttributes();
        }
        return null;
    }

    /**
     * Get name of the current span, if possible.
     **/
    @NonNull
    public static String getCurrentSpanName() {
        final Span span = SpanExtension.current();
        if (span instanceof ReadableSpan) {
            return ((ReadableSpan) span).getName();
        }
        return "null";
    }

}
