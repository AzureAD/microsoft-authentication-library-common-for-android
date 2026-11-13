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
package com.microsoft.identity.common.java.throttling;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.OtelContextExtension;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.ContextKey;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

/**
 * Request-scoped throttling context stored in OpenTelemetry {@link Context} so outbound HTTP
 * clients, retry policies, and wrapped worker executors share the originating Broker scope.
 */
public final class TokenThrottleContext {

    private static final String TAG = TokenThrottleContext.class.getSimpleName();
    private static final String CONTEXT_KEY_NAME = "msal-broker-token-throttle-context";

    private static volatile ContextKey<TokenThrottleContext> sContextKey;
    private static final Object KEY_INIT_LOCK = new Object();
    private static final ThreadLocal<TokenThrottleContext> FALLBACK_CONTEXT = new ThreadLocal<>();

    /**
     * Auto-closeable scope handle whose {@link #close()} declares no checked exceptions.
     */
    public interface TokenThrottleScope extends AutoCloseable {
        @NonNull
        TokenThrottleContext context();

        @Override
        void close();
    }

    @NonNull
    private final TokenThrottleKey mThrottleKey;

    private final boolean mInteractiveOrigin;

    @Nullable
    private final String mCorrelationId;

    @NonNull
    private final Span mSpan;

    @NonNull
    private final AtomicReference<LiveThrottleCapture> mLiveThrottleCapture = new AtomicReference<>();

    private TokenThrottleContext(
            @NonNull final TokenThrottleKey throttleKey,
            final boolean interactiveOrigin,
            @Nullable final String correlationId,
            @NonNull final Span span) {
        mThrottleKey = throttleKey;
        mInteractiveOrigin = interactiveOrigin;
        mCorrelationId = !StringUtil.isNullOrEmpty(correlationId) ? correlationId.trim() : null;
        mSpan = span;
    }

    /**
     * Opens and activates a {@link TokenThrottleContext} on the current OpenTelemetry
     * {@link Context}, falling back safely without throwing if OpenTelemetry context storage
     * is unavailable.
     *
     * @param key               scoped SHA-256 throttle key.
     * @param interactiveOrigin {@code true} when initiated by an interactive user flow.
     * @param correlationId     optional request correlation ID.
     * @return non-null {@link TokenThrottleScope}.
     */
    @NonNull
    public static TokenThrottleScope open(
            @NonNull final TokenThrottleKey key,
            final boolean interactiveOrigin,
            @Nullable final String correlationId) {
        final Span currentSpan = SpanExtension.current();
        final TokenThrottleContext throttleContext = new TokenThrottleContext(
                key,
                interactiveOrigin,
                correlationId,
                currentSpan
        );

        Scope installedOtelScope = null;
        final ContextKey<TokenThrottleContext> contextKey = getOrCreateContextKey();
        if (contextKey != null) {
            try {
                final Context baseContext = OtelContextExtension.current();
                if (baseContext != null) {
                    installedOtelScope = baseContext.with(contextKey, throttleContext).makeCurrent();
                }
            } catch (final RuntimeException | NoSuchMethodError | AbstractMethodError error) {
                Logger.error(TAG + ":open", error.getMessage(), error);
            }
        }

        final Scope otelScopeToClose = installedOtelScope;
        final boolean usedFallback;
        final TokenThrottleContext previousFallback;
        if (otelScopeToClose == null) {
            usedFallback = true;
            previousFallback = FALLBACK_CONTEXT.get();
            FALLBACK_CONTEXT.set(throttleContext);
        } else {
            usedFallback = false;
            previousFallback = null;
        }

        return new TokenThrottleScope() {
            private final AtomicBoolean mClosed = new AtomicBoolean(false);

            @Override
            @NonNull
            public TokenThrottleContext context() {
                return throttleContext;
            }

            @Override
            public void close() {
                if (!mClosed.compareAndSet(false, true)) {
                    return;
                }
                if (otelScopeToClose != null) {
                    try {
                        otelScopeToClose.close();
                    } catch (final RuntimeException | NoSuchMethodError | AbstractMethodError error) {
                        Logger.error(TAG + ":close", error.getMessage(), error);
                    }
                } else if (usedFallback) {
                    if (previousFallback == null) {
                        FALLBACK_CONTEXT.remove();
                    } else {
                        FALLBACK_CONTEXT.set(previousFallback);
                    }
                }
            }
        };
    }

    /**
     * Returns the currently active {@link TokenThrottleContext}, or {@code null} if none is open.
     *
     * @return active {@link TokenThrottleContext} or {@code null}.
     */
    @Nullable
    public static TokenThrottleContext current() {
        final ContextKey<TokenThrottleContext> contextKey = getOrCreateContextKey();
        if (contextKey != null) {
            try {
                final Context currentContext = OtelContextExtension.current();
                if (currentContext != null) {
                    final TokenThrottleContext found = currentContext.get(contextKey);
                    if (found != null) {
                        return found;
                    }
                }
            } catch (final RuntimeException | NoSuchMethodError | AbstractMethodError error) {
                Logger.error(TAG + ":current", error.getMessage(), error);
            }
        }
        return FALLBACK_CONTEXT.get();
    }

    @NonNull
    public TokenThrottleKey getThrottleKey() {
        return mThrottleKey;
    }

    public boolean isInteractiveOrigin() {
        return mInteractiveOrigin;
    }

    @Nullable
    public String getCorrelationId() {
        return mCorrelationId;
    }

    @NonNull
    public Span getSpan() {
        return mSpan;
    }

    public void recordLiveThrottle(@NonNull final LiveThrottleCapture capture) {
        mLiveThrottleCapture.set(capture);
    }

    @Nullable
    public LiveThrottleCapture getLiveThrottleResult() {
        return mLiveThrottleCapture.get();
    }

    @Nullable
    private static ContextKey<TokenThrottleContext> getOrCreateContextKey() {
        ContextKey<TokenThrottleContext> local = sContextKey;
        if (local != null) {
            return local;
        }
        synchronized (KEY_INIT_LOCK) {
            local = sContextKey;
            if (local == null) {
                try {
                    local = ContextKey.named(CONTEXT_KEY_NAME);
                    sContextKey = local;
                } catch (final RuntimeException | NoSuchMethodError | AbstractMethodError error) {
                    Logger.error(TAG + ":getOrCreateContextKey", error.getMessage(), error);
                    return null;
                }
            }
            return local;
        }
    }
}
