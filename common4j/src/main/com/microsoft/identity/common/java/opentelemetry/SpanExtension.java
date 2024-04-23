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
// OUT OF OR IN CO
package com.microsoft.identity.common.java.opentelemetry;

import com.microsoft.identity.common.java.logging.Logger;

import io.opentelemetry.api.internal.ImmutableSpanContext;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.SpanId;
import io.opentelemetry.api.trace.TraceId;
import io.opentelemetry.context.ImplicitContextKeyed;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

/**
 * Extension methods for {@link Span}.
 * <p>
 * This basically provides a custom, safe implementation of {@link Span#current()} to be used in
 * MSAL for scenarios where minSdkVersion of calling application {@literal <} 24. The reason for this is
 * because the default no-op implementation uses "static interface methods" and these get left out
 * of the APK for applications whose MIN SDK is {@literal <} 24 and minification through R8 is DISABLED because
 * the consumers-rules are NOT honored when minification is disabled and R8 applies some default
 * proguard rules that leaves static interface methods out of the APK. This causes a
 * "NoSuchMethodError" when such methods are invoked.
 * This is not a problem for Broker Hosting applications as the MIN SDK for those is already {@literal >}= 24.
 * The issue only arises for the some MSAL consumers falling into the above situation. Tracing is
 * disabled by default anyway for MSAL (and only turned on for Broker) and Open Telemetry uses its
 * own Noop implementations, however, here we are just providing our own that DON'T use static
 * interface methods.
 */
public class SpanExtension {

    private static final String TAG = SpanExtension.class.getSimpleName();

    private static final SpanContext INVALID =
            ImmutableSpanContext.create(
                    TraceId.getInvalid(),
                    SpanId.getInvalid(),
                    new NoopTraceFlags(),
                    new NoopTraceState(),
                    /* remote= */ false,
                    /* valid= */ false
            );

    public static Span current() {
        try {
            return Span.current();
        } catch (final NoSuchMethodError error) {
            Logger.error(TAG + ":getCurrentSpan", error.getMessage(), error);
            return new NoopSpan(INVALID);
        }
    }

    /**
     * A safe implementation of {@link ImplicitContextKeyed#makeCurrent()} that doesn't crash. The
     * default implementation in Open Telemetry sometimes throws an NPE deep into OTel's code. Per
     * our telemetry this is happening on 0.08% of devices i.e. the impact is minimal. That said,
     * we're creating this safe wrapper here in effort to mitigate even that minimal impact. If we
     * get an NPE then we just return a Noop Scope.
     *
     * @param span the {@link Span} that needs to be made current
     * @return a {@link Scope}
     */
    public static Scope makeCurrentSpan(@NonNull final Span span) {
        try {
            return span.makeCurrent();
        } catch (final AbstractMethodError | Exception exception) {
            Logger.error(TAG + ":makeCurrentSpan", exception.getMessage(), exception);
            return NoopScope.INSTANCE;
        }
    }

    /**
     * This is a custom No-op implementation of {@link Scope}. This should be viewed the same as the
     * default Noop implementation in {@link io.opentelemetry.context.ThreadLocalContextStorage}.
     * We just made a custom one since the default one is package-private.
     */
    enum NoopScope implements Scope {
        INSTANCE;

        @Override
        public void close() {
        }
    }

}
