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

import java.util.concurrent.TimeUnit;

import io.opentelemetry.api.common.AttributeKey;
import io.opentelemetry.api.common.Attributes;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import lombok.AllArgsConstructor;

/**
 * A custom noop implementation of {@link Span} to be used in MSAL for scenarios where
 * minSdkVersion of calling application < 24. The reason for this is because the default no-op
 * implementation uses "static interface methods" and these get left out of the APK for applications
 * whose MIN SDK is < 24 and minification through R8 is DISABLED because the consumers-rules are NOT
 * honored when minification is disabled and R8 applies some default proguard rules that leaves
 * static interface methods out of the APK. This causes a "NoSuchMethodError" when such methods are
 * invoked.
 * This is not a problem for Broker Hosting applications as the MIN SDK for those is already {@literal >}= 24.
 * The issue only arises for the some MSAL consumers falling into the above situation. Tracing is
 * disabled by default anyway for MSAL (and only turned on for Broker) and Open Telemetry uses its
 * own Noop implementations, however, here we are just providing our own that DON'T use static
 * interface methods.
 */
@AllArgsConstructor
public class NoopSpan implements Span {

    private final SpanContext spanContext;

    @Override
    public Span setAttribute(String key, String value) {
        return this;
    }

    @Override
    public Span setAttribute(String key, long value) {
        return this;
    }

    @Override
    public Span setAttribute(String key, double value) {
        return this;
    }

    @Override
    public Span setAttribute(String key, boolean value) {
        return this;
    }

    @Override
    public <T> Span setAttribute(AttributeKey<T> key, T value) {
        return this;
    }

    @Override
    public Span setAllAttributes(Attributes attributes) {
        return this;
    }

    @Override
    public Span addEvent(String name) {
        return this;
    }

    @Override
    public Span addEvent(String name, long timestamp, TimeUnit unit) {
        return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes) {
        return this;
    }

    @Override
    public Span addEvent(String name, Attributes attributes, long timestamp, TimeUnit unit) {
        return this;
    }

    @Override
    public Span setStatus(StatusCode statusCode) {
        return this;
    }

    @Override
    public Span setStatus(StatusCode statusCode, String description) {
        return this;
    }

    @Override
    public Span recordException(Throwable exception) {
        return this;
    }

    @Override
    public Span recordException(Throwable exception, Attributes additionalAttributes) {
        return this;
    }

    @Override
    public Span updateName(String name) {
        return this;
    }

    @Override
    public void end() {}

    @Override
    public void end(long timestamp, TimeUnit unit) {}

    @Override
    public SpanContext getSpanContext() {
        return spanContext;
    }

    @Override
    public boolean isRecording() {
        return false;
    }

    @Override
    public String toString() {
        return "NoopSpan{" + spanContext + '}';
    }
}
