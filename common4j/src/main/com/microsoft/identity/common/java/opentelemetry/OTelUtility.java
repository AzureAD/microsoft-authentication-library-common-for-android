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

import com.microsoft.identity.common.java.logging.Logger;


import javax.annotation.Nullable;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.logs.LogRecordBuilder;
import io.opentelemetry.api.metrics.LongCounter;
import io.opentelemetry.api.metrics.Meter;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import lombok.NonNull;

public class OTelUtility {
    private static final String TAG = OTelUtility.class.getSimpleName();

    /**
     * Creates a span (with shared basic attributes).
     **/
    @NonNull
    public static Span createSpan(@NonNull final String name) {
        final Tracer tracer = OpenTelemetryHolder.getTracer(TAG);
        return tracer.spanBuilder(name).startSpan();
    }

    /**
     * Creates a span from a parent Span Context (with shared basic attributes) and caller pkg name
     * pre-populated on the span upon creation.
     **/
    @NonNull
    public static Span createSpan(@NonNull final String name, @NonNull final String callingPackageName) {
        final Tracer tracer = OpenTelemetryHolder.getTracer(TAG);
        return tracer.spanBuilder(name)
                .setAttribute(AttributeName.calling_package_name.name(), callingPackageName)
                .startSpan();
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

        final Tracer tracer = OpenTelemetryHolder.getTracer(TAG);

        return tracer.spanBuilder(name)
                .setParent(Context.current().with(Span.wrap(parentSpanContext)))
                .startSpan();
    }

    /**
     * Creates a span from a parent Span Context (with shared basic attributes) and caller pkg name
     * pre-populated on the span upon creation.
     **/
    @NonNull
    public static Span createSpanFromParent(@NonNull final String name,
                                            @Nullable final SpanContext parentSpanContext,
                                            @NonNull final String callingPackageName) {
        final String methodTag = TAG + ":createSpanFromParent";

        if (parentSpanContext == null) {
            Logger.verbose(methodTag, "parentSpanContext is NULL. Creating span without parent.");
            return createSpan(name, callingPackageName);
        }

        if (!parentSpanContext.isValid()) {
            Logger.warn(methodTag, "parentSpanContext is INVALID. Creating span without parent.");
            return createSpan(name, callingPackageName);
        }

        final Tracer tracer = OpenTelemetryHolder.getTracer(TAG);

        return tracer.spanBuilder(name)
                .setParent(Context.current().with(Span.wrap(parentSpanContext)))
                .setAttribute(AttributeName.calling_package_name.name(), callingPackageName)
                .startSpan();
    }

    /**
     * Creates a span (with shared basic attributes).
     **/
    @NonNull
    public static LongCounter createLongCounter(@NonNull final String name, @NonNull final String description) {
        final Meter meter = OpenTelemetryHolder.getMeter(TAG);

        return meter
                .counterBuilder(name)
                .setDescription(description)
                .setUnit("count")
                .build();
    }


    public static LogRecordBuilder createLogBuilder() {
        final io.opentelemetry.api.logs.Logger logger = OpenTelemetryHolder.getLogger(TAG);
        return logger.logRecordBuilder();
    }
}
