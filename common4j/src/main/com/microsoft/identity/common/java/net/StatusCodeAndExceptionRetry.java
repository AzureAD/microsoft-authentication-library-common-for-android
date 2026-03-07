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
package com.microsoft.identity.common.java.net;

import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.util.ported.Function;
import net.jcip.annotations.Immutable;
import net.jcip.annotations.ThreadSafe;

import java.net.SocketTimeoutException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.BiFunction;

import javax.annotation.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

/**
 * A retry policy that implements exponential backoff based around functions that operate on the
 * HttpResponse object and any Exception that might be thrown from that method.  By default, without
 * any setup, this class will not retry at all - any response is acceptable, and no exceptions are
 * retryable.
 */
@AllArgsConstructor
@Builder
@ThreadSafe
@Immutable
public class StatusCodeAndExceptionRetry implements IRetryPolicy<HttpResponse> {
    /**
     * Thread-safe HTTP-date formatters used when parsing the {@code Retry-After} header.
     * Listed in preference order as required by RFC 7231 §7.1.3.
     */
    private static final DateTimeFormatter[] HTTP_DATE_FORMATTERS = {
        DateTimeFormatter.RFC_1123_DATE_TIME,                                     // RFC 1123 (preferred)
        DateTimeFormatter.ofPattern("EEEE, dd-MMM-yy HH:mm:ss zzz", Locale.US),  // RFC 850 (obsolete)
        DateTimeFormatter.ofPattern("EEE MMM d HH:mm:ss yyyy z", Locale.US)      // ANSI C asctime (obsolete)
    };
    @Builder.Default
    private final Function<Exception, Boolean> isRetryableException = new Function<Exception, Boolean>() {
        @Override
        public Boolean apply(Exception input) {
            return Boolean.FALSE;
        }
    };
    @Builder.Default
    private final BiFunction<HttpResponse, Integer, Boolean> isRetryable = new BiFunction<HttpResponse, Integer, Boolean>() {
        @Override
        public Boolean apply(HttpResponse input, Integer attemptNumber) {
            return Boolean.FALSE;
        }
    };
    @Builder.Default
    private final Function<HttpResponse, Boolean> isAcceptable = new Function<HttpResponse, Boolean>() {

        public Boolean apply(HttpResponse input) {
            return Boolean.TRUE;
        }
    };
    @Builder.Default
    private final int number = 1;
    @Builder.Default
    private final int initialDelay = 1000;
    @Builder.Default
    private final int extensionFactor = 2;
    /**
     * Jitter factor applied to each computed delay.
     * <p>
     * When {@code jitterFactor > 0}, the effective delay for each retry is:
     * {@code baseDelay + random(0, baseDelay * jitterFactor)}.
     * When {@code jitterFactor == 0.0} (the default), no jitter is applied and delays
     * are deterministic.
     * </p>
     */
    @Builder.Default
    private final double jitterFactor = 0.0;
    /**
     * Whether to respect the {@code Retry-After} response header.
     * <p>
     * When {@code true}, the computed delay is {@code max(computedDelay, retryAfterMs)},
     * where {@code retryAfterMs} is parsed from the {@code Retry-After} header of the last
     * retryable response. Both delta-seconds and HTTP-date header formats are supported.
     * Malformed or absent headers are silently ignored.
     * </p>
     */
    @Builder.Default
    private final boolean respectRetryAfter = false;
    /**
     * Per-retry safety cap on the total computed delay, in milliseconds.
     * <p>
     * The effective delay for any single retry will never exceed this value, regardless
     * of the base delay, jitter, or {@code Retry-After} header. Defaults to 60&nbsp;000&nbsp;ms
     * (60 seconds).
     * </p>
     */
    @Builder.Default
    private final int maxTotalDelayMs = 60000;

    @Override
    public HttpResponse attempt(Callable<HttpResponse> supplier) throws ClientException {
        int attemptNumber = number;
        int cumulativeDelay = initialDelay;
        HttpResponse lastResponse = null;
        do {
            lastResponse = null;
            try {
                final HttpResponse response = supplier.call();
                //If there are no retries left, or the response is acceptable, or it is not retryable.
                if (attemptNumber <= 0 || isAcceptable.apply(response) || !isRetryable.apply(response, attemptNumber)) {
                    return response;
                }
                lastResponse = response;
            } catch (final Exception e) {
                if (attemptNumber <= 0 || !isRetryableException.apply(e)) {
                    if (e instanceof ClientException) {
                        throw (ClientException) e;
                    }
                    else {
                        throw new RetryFailedException(e);
                    }
                }
            }
        } while (attemptNumber-- > 0 && waited(computeDelay(cumulativeDelay, lastResponse)) && (cumulativeDelay *= extensionFactor) > 0);
        throw new IllegalStateException("This code should not be reachable");
    }

    /**
     * Computes the delay to use before the next retry attempt.
     * <p>
     * The result is:
     * <ol>
     *   <li>Start with {@code baseDelay}.</li>
     *   <li>If {@link #jitterFactor} {@code > 0}, add a random value in
     *       {@code [0, baseDelay * jitterFactor]}.</li>
     *   <li>If {@link #respectRetryAfter} is {@code true} and {@code lastResponse} contains
     *       a parseable {@code Retry-After} header, take the maximum of the current delay and
     *       the header value.</li>
     *   <li>Cap at {@link #maxTotalDelayMs}.</li>
     * </ol>
     * </p>
     *
     * @param baseDelay    The base delay in milliseconds before jitter/header adjustments.
     * @param lastResponse The last retryable {@link HttpResponse}, or {@code null} if the
     *                     previous attempt threw an exception.
     * @return The adjusted delay in milliseconds, capped at {@link #maxTotalDelayMs}.
     */
    private int computeDelay(final int baseDelay, @Nullable final HttpResponse lastResponse) {
        long delay = baseDelay;
        if (jitterFactor > 0.0) {
            final int maxJitter = (int) (baseDelay * jitterFactor);
            if (maxJitter > 0) {
                delay += ThreadLocalRandom.current().nextInt(maxJitter + 1);
            }
        }
        if (respectRetryAfter) {
            final long retryAfterMs = parseRetryAfterHeader(lastResponse);
            if (retryAfterMs >= 0) {
                delay = Math.max(delay, retryAfterMs);
            }
        }
        return (int) Math.min(delay, maxTotalDelayMs);
    }

    /**
     * Parses the {@code Retry-After} header from the given response.
     * <p>
     * Supports both the delta-seconds format (e.g., {@code "120"}) and the HTTP-date
     * format (e.g., {@code "Tue, 15 Nov 1994 08:12:31 GMT"}). Returns {@code -1} if the
     * header is absent, empty, or cannot be parsed in any recognised format.
     * </p>
     *
     * @param response The last retryable response, or {@code null}.
     * @return The parsed delay in milliseconds ({@code >= 0}), or {@code -1} if unavailable.
     */
    private long parseRetryAfterHeader(@Nullable final HttpResponse response) {
        if (response == null) {
            return -1;
        }
        final Map<String, List<String>> headers = response.getHeaders();
        if (headers == null) {
            return -1;
        }
        List<String> values = headers.get("Retry-After");
        if (values == null) {
            values = headers.get("retry-after");
        }
        if (values == null || values.isEmpty()) {
            return -1;
        }
        final String retryAfterValue = values.get(0);
        if (retryAfterValue == null || retryAfterValue.trim().isEmpty()) {
            return -1;
        }
        final String trimmed = retryAfterValue.trim();
        // Try delta-seconds format first.
        try {
            final long seconds = Long.parseLong(trimmed);
            if (seconds >= 0) {
                return seconds * 1000L;
            }
            return -1;
        } catch (final NumberFormatException ignored) {
            // Not a number; fall through to HTTP-date formats.
        }
        // Try HTTP-date formats as defined by RFC 7231.
        for (final DateTimeFormatter formatter : HTTP_DATE_FORMATTERS) {
            try {
                final ZonedDateTime retryDateTime = ZonedDateTime.parse(trimmed, formatter);
                return Math.max(0L, retryDateTime.toInstant().toEpochMilli() - System.currentTimeMillis());
            } catch (final DateTimeParseException ignored) {
                // Try next format.
            }
        }
        return -1;
    }

    /**
     * Just a sleep function that allows for a return to break the loop.
     * @param millis How long, in milliseconds, to pause.
     * @return true if we successfully waited, false if interrupted.
     */
    private boolean waited(final int millis) {
        try {
            Thread.sleep(millis);
            return true;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Creates a retry policy that retries on IO exceptions only.
     * <p>
     * This policy retries ONLY on {@link ClientException#IO_ERROR} exceptions
     * (excluding {@link SocketTimeoutException}).
     * <p>
     * The {@code numberOfRetries} parameter controls how many retry attempts will be made
     * after the initial attempt fails. For example:
     * <ul>
     *     <li>{@code numberOfRetries = 0}: 1 total attempt (no retries)</li>
     *     <li>{@code numberOfRetries = 1}: 2 total attempts (1 original + 1 retry)</li>
     *     <li>{@code numberOfRetries = 2}: 3 total attempts (1 original + 2 retries)</li>
     * </ul>
     * <p>
     * Use this for scenarios where you want IO-error-specific retry logic.
     * For the library's standard retry behavior, use {@link UrlConnectionHttpClient#getDefaultInstance()}.
     *
     * @param tag The logging tag for tracing retry attempts
     * @param numberOfRetries The number of retry attempts after the initial attempt (must be non-negative)
     * @return A configured {@link StatusCodeAndExceptionRetry} instance for IO errors only
     * @throws IllegalArgumentException if numberOfRetries is negative
     */
    public static StatusCodeAndExceptionRetry getIOExceptionRetryPolicy(
            @NonNull final String tag,
            final int numberOfRetries,
            @Nullable String attributeNameForRetry) {
        if (numberOfRetries < 0) {
            throw new IllegalArgumentException("numberOfRetries must be non-negative, got: " + numberOfRetries);
        }
        return StatusCodeAndExceptionRetry.builder()
                .number(numberOfRetries)
                .isRetryableException(e -> {
                    if (attributeNameForRetry != null) {
                        SpanExtension.current().setAttribute(
                                attributeNameForRetry,
                                numberOfRetries
                        );
                    }
                    if (e instanceof ClientException
                            && ((ClientException) e).getErrorCode().equals(ClientException.IO_ERROR)
                            && !(e.getCause() instanceof SocketTimeoutException)) {
                        Logger.info(tag + ":getIOExceptionRetryPolicy", "Retrying due to exception: " + e);
                        return Boolean.TRUE;
                    }
                    return Boolean.FALSE;
                })
                .build();
    }
}
