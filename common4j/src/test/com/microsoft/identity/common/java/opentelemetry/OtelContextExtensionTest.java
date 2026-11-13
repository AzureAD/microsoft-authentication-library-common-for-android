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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.microsoft.identity.common.java.throttling.TokenThrottleContext;
import com.microsoft.identity.common.java.throttling.TokenThrottleKey;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;

/**
 * Deterministic unit tests for {@link OtelContextExtension} verifying {@code wrap(Callable)} and
 * {@code wrap(Runnable)} propagate {@link TokenThrottleContext} and OpenTelemetry {@link Span}
 * across background executor threads and degrade safely on {@link NoSuchMethodError}.
 */
@RunWith(JUnit4.class)
public class OtelContextExtensionTest {

    @Test
    public void wrapCallable_propagatesTokenThrottleContextAndSpanOntoExecutorWorkerThread() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Span parentSpan = mock(Span.class, CALLS_REAL_METHODS);
        final TokenThrottleKey fociKey = TokenThrottleKey.forFociProbe(
                "https://login.microsoftonline.com/common",
                "foci-client-id",
                "User.Read openid",
                "uid.utid",
                Collections.singletonList("alice@contoso.com")
        );

        try {
            try (Scope spanScope = SpanExtension.makeCurrentSpan(parentSpan);
                 TokenThrottleContext.TokenThrottleScope throttleScope =
                         TokenThrottleContext.open(fociKey, false, "corr-callable-1")) {

                // Unwrapped Callable on raw worker pool loses both TokenThrottleContext and Span
                final TokenThrottleContext unwrappedWorkerContext = executor.submit(
                        new Callable<TokenThrottleContext>() {
                            @Override
                            public TokenThrottleContext call() {
                                assertNotSame(parentSpan, SpanExtension.current());
                                return TokenThrottleContext.current();
                            }
                        }
                ).get(5, TimeUnit.SECONDS);
                assertNull(unwrappedWorkerContext);

                // Wrapped Callable propagates both TokenThrottleContext and active Span
                final Callable<String> wrappedCallable = OtelContextExtension.wrap(new Callable<String>() {
                    @Override
                    public String call() {
                        final TokenThrottleContext workerCtx = TokenThrottleContext.current();
                        assertSame(throttleScope.context(), workerCtx);
                        assertEquals(fociKey, workerCtx.getThrottleKey());
                        assertEquals("corr-callable-1", workerCtx.getCorrelationId());
                        assertSame(parentSpan, SpanExtension.current());
                        SpanExtension.current().setAttribute(
                                AttributeName.throttle_key_prefix.name(),
                                fociKey.getKeyPrefix()
                        );
                        return "foci-worker-result";
                    }
                });

                final String result = executor.submit(wrappedCallable).get(5, TimeUnit.SECONDS);
                assertEquals("foci-worker-result", result);
                verify(parentSpan).setAttribute(AttributeName.throttle_key_prefix.name(), fociKey.getKeyPrefix());
            }

            // After scopes close, subsequent tasks on the reused worker thread see clean context
            final TokenThrottleContext postCloseContext = executor.submit(new Callable<TokenThrottleContext>() {
                @Override
                public TokenThrottleContext call() {
                    assertNotSame(parentSpan, SpanExtension.current());
                    return TokenThrottleContext.current();
                }
            }).get(5, TimeUnit.SECONDS);
            assertNull(postCloseContext);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    public void wrapRunnable_propagatesTokenThrottleContextAndSpanOntoExecutorWorkerThread() throws Exception {
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        final Span parentSpan = mock(Span.class, CALLS_REAL_METHODS);
        final TokenThrottleKey deviceKey = TokenThrottleKey.forDeviceToken(
                "https://login.microsoftonline.com/tenant-1",
                "wpj-client",
                "https://graph.microsoft.com",
                "Device.Read",
                "device-id-123"
        );

        final AtomicReference<TokenThrottleContext> observedContext = new AtomicReference<>();
        final AtomicReference<Span> observedSpan = new AtomicReference<>();

        try {
            try (Scope spanScope = SpanExtension.makeCurrentSpan(parentSpan);
                 TokenThrottleContext.TokenThrottleScope throttleScope =
                         TokenThrottleContext.open(deviceKey, false, "corr-runnable-1")) {

                final Runnable wrappedRunnable = OtelContextExtension.wrap(new Runnable() {
                    @Override
                    public void run() {
                        observedContext.set(TokenThrottleContext.current());
                        observedSpan.set(SpanExtension.current());
                        SpanExtension.current().setAttribute(
                                AttributeName.throttle_scope_type.name(),
                                deviceKey.getScopeType().name()
                        );
                    }
                });

                executor.submit(wrappedRunnable).get(5, TimeUnit.SECONDS);
                assertSame(throttleScope.context(), observedContext.get());
                assertSame(parentSpan, observedSpan.get());
                verify(parentSpan).setAttribute(
                        AttributeName.throttle_scope_type.name(),
                        deviceKey.getScopeType().name()
                );
            }

            // Reused worker thread is clean once wrapped Runnable finishes
            final AtomicReference<TokenThrottleContext> afterClose = new AtomicReference<>();
            executor.submit(new Runnable() {
                @Override
                public void run() {
                    afterClose.set(TokenThrottleContext.current());
                }
            }).get(5, TimeUnit.SECONDS);
            assertNull(afterClose.get());
        } finally {
            executor.shutdownNow();
        }
    }

    @SuppressWarnings("unchecked")
    @Test
    public void wrapCallableAndRunnable_fallbackSafelyWhenNoSuchMethodErrorOccurs() throws Exception {
        final Context brokenContext = mock(Context.class, CALLS_REAL_METHODS);
        doThrow(new NoSuchMethodError("Simulated minSdk < 24 static interface strip"))
                .when(brokenContext).wrap(any(Callable.class));
        doThrow(new NoSuchMethodError("Simulated minSdk < 24 static interface strip"))
                .when(brokenContext).wrap(any(Runnable.class));

        final Callable<String> rawCallable = new Callable<String>() {
            @Override
            public String call() {
                return "fallback-callable-ok";
            }
        };
        final AtomicBoolean runnableRan = new AtomicBoolean(false);
        final Runnable rawRunnable = new Runnable() {
            @Override
            public void run() {
                runnableRan.set(true);
            }
        };

        try (Scope brokenScope = brokenContext.makeCurrent()) {
            final Callable<String> wrappedCallable = OtelContextExtension.wrap(rawCallable);
            assertSame(rawCallable, wrappedCallable);
            assertEquals("fallback-callable-ok", wrappedCallable.call());

            final Runnable wrappedRunnable = OtelContextExtension.wrap(rawRunnable);
            assertSame(rawRunnable, wrappedRunnable);
            wrappedRunnable.run();
            assertTrue(runnableRan.get());
        }
    }

    @Test
    public void tokenThrottleContext_fallsBackCleanlyWhenOtelContextExtensionCurrentReturnsNull() {
        final TokenThrottleKey key = TokenThrottleKey.forBrokerPrt(
                "https://login.microsoftonline.com/common",
                "uid.utid",
                "refresh_token",
                null
        );

        try (MockedStatic<OtelContextExtension> mockedOtel = Mockito.mockStatic(OtelContextExtension.class)) {
            mockedOtel.when(OtelContextExtension::current).thenReturn(null);

            assertNull(TokenThrottleContext.current());
            try (TokenThrottleContext.TokenThrottleScope outer =
                         TokenThrottleContext.open(key, false, "corr-fallback")) {
                assertSame(outer.context(), TokenThrottleContext.current());
                assertEquals("corr-fallback", TokenThrottleContext.current().getCorrelationId());
            }
            assertNull(TokenThrottleContext.current());
        }
    }
}
