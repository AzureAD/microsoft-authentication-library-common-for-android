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
package com.microsoft.identity.common.internal.broker;

import com.microsoft.identity.common.java.opentelemetry.NoopSpan;

import java.util.HashMap;
import java.util.Map;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;

/**
 * Test span that records the string attributes set on it, so a test can assert what the Auth UX
 * bridge stamped onto the current span.
 *
 * <p>Extends the production {@link NoopSpan} so every other member is already a no-op, which keeps
 * this to the single method under test and avoids taking an OpenTelemetry SDK / exporter dependency
 * just to read one attribute.
 *
 * <p>Written in Java rather than Kotlin deliberately: {@code Span#setAttribute(String, String)} is a
 * Java interface <em>default</em> method that {@code NoopSpan} overrides, and Kotlin will not let a
 * subclass override it ("overrides nothing"), whether by inheritance or by interface delegation.
 * Java has no such restriction.
 */
public class RecordingSpan extends NoopSpan {

    private final Map<String, String> attributes = new HashMap<>();

    public RecordingSpan() {
        super(SpanContext.getInvalid());
    }

    @Override
    public Span setAttribute(final String key, final String value) {
        attributes.put(key, value);
        return this;
    }

    /** Attribute value recorded for {@code key}, or null when it was never set. */
    public String getAttribute(final String key) {
        return attributes.get(key);
    }

    /** Whether {@code key} was ever set on this span. */
    public boolean hasAttribute(final String key) {
        return attributes.containsKey(key);
    }
}
