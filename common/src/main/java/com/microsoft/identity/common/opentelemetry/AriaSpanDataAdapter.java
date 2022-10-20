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
package com.microsoft.identity.common.opentelemetry;

import com.microsoft.applications.telemetry.EventProperties;
import com.microsoft.identity.common.java.exception.IErrorInformation;
import com.microsoft.identity.common.java.logging.Logger;

import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.sdk.trace.data.EventData;
import io.opentelemetry.sdk.trace.data.SpanData;
import io.opentelemetry.sdk.trace.internal.data.ExceptionEventData;
import lombok.NonNull;

/**
 * An implementation of {@link ISpanDataAdapter} to adapt {@link SpanData} to {@link EventProperties}
 * i.e. a format needed by Aria to uploaded telemetry.
 */
public class AriaSpanDataAdapter implements ISpanDataAdapter<EventProperties> {

    private static final String TAG = AriaSpanDataAdapter.class.getSimpleName();

    private static final String TABLE_NAME = "android_spans";

    @Override
    public EventProperties adapt(@NonNull final SpanData spanData) {
        final EventProperties eventProperties = new EventProperties(TABLE_NAME);

        // top level span information
        eventProperties.setProperty(AriaPropertyName.span_name.name(), spanData.getName());
        eventProperties.setProperty(AriaPropertyName.span_kind.name(), spanData.getKind().name());
        eventProperties.setProperty(AriaPropertyName.trace_id.name(), spanData.getTraceId());
        eventProperties.setProperty(AriaPropertyName.span_id.name(), spanData.getSpanId());
        eventProperties.setProperty(AriaPropertyName.span_status.name(), spanData.getStatus().getStatusCode().name());
        eventProperties.setProperty(AriaPropertyName.parent_span_id.name(), spanData.getParentSpanId());

        final long elapsedTimeNanos = spanData.getEndEpochNanos() - spanData.getStartEpochNanos();
        final long elapsedTimeMillis = TimeUnit.NANOSECONDS.toMillis(elapsedTimeNanos);
        eventProperties.setProperty(AriaPropertyName.elapsed_time.name(), elapsedTimeMillis);

        // Each Attribute becomes an Aria Event Property
        fillAttributes(eventProperties, spanData.getAttributes(), "");

        // Span event information
        // Spans also have a concept of Event
        // An Exception during a Span is considered an Event
        spanData.getEvents().forEach(eventData -> {
            fillEventData(eventProperties, eventData);
        });

        return eventProperties;
    }

    private void fillEventData(@NonNull final EventProperties eventProperties,
                               @NonNull final EventData eventData) {
        final String eventName = eventData.getName();
        final String prefix = "event_" + eventName;

        if (eventData instanceof ExceptionEventData) {
            fillExceptionEventData(eventProperties, (ExceptionEventData) eventData);
        } else {
            fillAttributes(eventProperties, eventData.getAttributes(), prefix);
        }
    }

    private void fillExceptionEventData(@NonNull final EventProperties eventProperties,
                                        @NonNull final ExceptionEventData exceptionEventData) {
        fillThrowableData(eventProperties, exceptionEventData.getException());
    }

    private void fillThrowableData(@NonNull final EventProperties eventProperties,
                                   @NonNull final Throwable throwable) {
        eventProperties.setProperty("error_message", throwable.getMessage());

        if (throwable instanceof IErrorInformation) {
            fillErrorInformation(eventProperties, (IErrorInformation) throwable);
        }
    }

    private void fillErrorInformation(@NonNull final EventProperties eventProperties,
                                      @NonNull final IErrorInformation errorInformation) {
        eventProperties.setProperty("error_code", errorInformation.getErrorCode());
    }

    private void fillAttributes(@NonNull final EventProperties eventProperties,
                                @NonNull final Attributes attributes,
                                @NonNull final String attributeKeyPrefix) {
        attributes.forEach((attributeKey, attributeValue) -> {
            fillAttribute(eventProperties, attributeKey, attributeKeyPrefix, attributeValue);
        });
    }

    private void fillAttribute(@NonNull final EventProperties eventProperties,
                               @NonNull final AttributeKey<?> attributeKey,
                               @NonNull final String attributeKeyPrefix,
                               @NonNull final Object attributeValue) {
        final String methodTag = TAG + ":fillAttribute";
        switch (attributeKey.getType()) {
            case STRING:
                eventProperties.setProperty(attributeKeyPrefix + attributeKey.getKey(), (String) attributeValue);
                break;
            case BOOLEAN:
                eventProperties.setProperty(attributeKeyPrefix + attributeKey.getKey(), (Boolean) attributeValue);
                break;
            case LONG:
                eventProperties.setProperty(attributeKeyPrefix + attributeKey.getKey(), (Long) attributeValue);
                break;
            case DOUBLE:
                eventProperties.setProperty(attributeKeyPrefix + attributeKey.getKey(), (Double) attributeValue);
                break;
            default:
                Logger.info(methodTag, "Unsupported attribute of type: " + attributeKey.getType());
        }
    }
}