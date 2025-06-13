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

import java.util.List;

import io.opentelemetry.api.baggage.Baggage;
import io.opentelemetry.api.baggage.BaggageBuilder;
import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.sdk.trace.ReadableSpan;

/**
 * Utility class for working with OpenTelemetry Baggage objects.
 */
public class BaggageExtension {

    private static final String TAG = BaggageExtension.class.getSimpleName();

    /**
     * Default constructor.
     */
    private BaggageExtension() {
        // Utility class, private constructor to prevent instantiation
    }

    /**
     * Extracts baggage items from a ReadableSpan based on the specified attribute names.
     *
     * @param span The span from which to extract baggage data.
     * @param attributeNames List of attribute names to extract from the span.
     * @return A Baggage object containing extracted attributes.
     */
    public static Baggage getBaggageFromReadableSpan(final Span span, final List<String> attributeNames) {
        final BaggageBuilder baggageBuilder = Baggage.builder();
        if (span instanceof ReadableSpan && attributeNames != null && !attributeNames.isEmpty()) {
            ReadableSpan readableSpan = (ReadableSpan) span;
            attributeNames.forEach(attributeName -> {
                final String value = readableSpan.getAttribute(AttributeKey.stringKey(attributeName));

                if (value != null && !value.isEmpty()) {
                    baggageBuilder.put(attributeName, value);
                }
            });
        }

        return baggageBuilder.build();
    }

    /**
     * Makes the provided Baggage current in the context, catching any exceptions silently.
     * This is useful in scenarios where Baggage propagation should not interrupt normal operation flow.
     *
     * @param baggage The Baggage to make current.
     * @return the resulting scope.
     */
    public static Scope makeBaggageCurrent(final Baggage baggage) {
        if (baggage == null) {
            return SpanExtension.NoopScope.INSTANCE;
        }

        try {
            return baggage.storeInContext(Context.current()).makeCurrent();
        } catch (Exception e) {
            Logger.error(TAG + ":makeBaggageCurrent", e.getMessage(), e);
            return SpanExtension.NoopScope.INSTANCE;
        }
    }
}
