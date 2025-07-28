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

import java.util.HashMap;
import java.util.Map;

import javax.annotation.Nullable;

import io.opentelemetry.api.baggage.propagation.W3CBaggagePropagator;
import io.opentelemetry.api.trace.propagation.W3CTraceContextPropagator;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapGetter;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.experimental.UtilityClass;

/**
 * Extension class for handling OpenTelemetry context propagation.
 * Provides utility methods for injecting and extracting context which contains Baggage and SpanContext.
 */
@UtilityClass
public final class TextMapPropagatorExtension {

    private static final String TAG = TextMapPropagatorExtension.class.getSimpleName();

    /**
     * Injects the current context into a carrier.
     *
     * @param context The context to inject. If null, the current context will be used.
     * @return A map containing the injected context properties.
     */
    public static HashMap<String, String> inject(final Context context) {
        try {
            final HashMap<String, String> carrier = new HashMap<>();
            final Context contextToInject = context != null ? context : Context.current();

            final TextMapSetter<Map<String, String>> setter = new TextMapSetter<Map<String, String>>() {
                @Override
                public void set(final Map<String, String> carrier, final String key, final String value) {
                    if (carrier != null && key != null && value != null) {
                        carrier.put(key, value);
                    }
                }
            };

            final TextMapPropagator propagator = TextMapPropagator.composite(
                    W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance()
            );
            propagator.inject(contextToInject, carrier, setter);
            return carrier;
        } catch (final Throwable e) {
            // Log the error and return an empty map if injection fails
            Logger.error(TAG + ":inject", "Failed to inject context", e);
            return new HashMap<>();
        }
    }

    /**
     * Extracts context from a carrier map.
     *
     * @param carrier The carrier containing context information.
     * @return The extracted context, or null if extraction fails.
     */
    @Nullable
    public static Context extract(final Map<String, String> carrier) {
        try {
            if (carrier == null || carrier.isEmpty()) {
                return Context.current();
            }

            final TextMapGetter<Map<String, String>> getter = new TextMapGetter<Map<String, String>>() {
                @Override
                public String get(final Map<String, String> carrier, final String key) {
                    return carrier.get(key);
                }

                @Override
                public Iterable<String> keys(final Map<String, String> carrier) {
                    return carrier.keySet();
                }
            };

            final TextMapPropagator propagator = TextMapPropagator.composite(
                    W3CTraceContextPropagator.getInstance(),
                    W3CBaggagePropagator.getInstance()
            );
            return propagator.extract(Context.current(), carrier, getter);
        } catch (final Exception | NoSuchMethodError e) {
            // Log the error and return null if extraction fails
            Logger.error(TAG + ":extract", "Failed to extract context", e);
            return null;
        }
    }
}
