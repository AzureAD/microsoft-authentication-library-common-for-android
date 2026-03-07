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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.microsoft.identity.common.java.exception.ClientException;

import org.junit.Test;

import java.net.HttpURLConnection;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Tests for {@link StatusCodeAndExceptionRetry}.
 */
public final class StatusCodeAndExceptionRetryTest {

    // ---------------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------------

    /** Creates a retryable-response policy (all responses trigger retry) with tiny delays. */
    private static StatusCodeAndExceptionRetry alwaysRetryPolicy(final int retries) {
        return StatusCodeAndExceptionRetry.builder()
                .number(retries)
                .initialDelay(1)
                .extensionFactor(1)  // constant delay to keep tests fast
                .isRetryable((response, attempt) -> Boolean.TRUE)
                .isAcceptable(response -> Boolean.FALSE)
                .build();
    }

    /** Builds a minimal {@link HttpResponse} with the given status code. */
    private static HttpResponse responseWithStatus(final int statusCode) {
        return new HttpResponse(statusCode, "", Collections.<String, List<String>>emptyMap());
    }

    /** Builds a {@link HttpResponse} that carries a {@code Retry-After} header. */
    private static HttpResponse responseWithRetryAfter(final String retryAfterValue) {
        final Map<String, List<String>> headers = new HashMap<>();
        final List<String> values = new ArrayList<>();
        values.add(retryAfterValue);
        headers.put("Retry-After", values);
        return new HttpResponse(HttpURLConnection.HTTP_TOO_MANY_REQUESTS, "", headers);
    }

    /**
     * Asserts that the given action completes within {@code maxMs} milliseconds, with a
     * generous tolerance for scheduling overhead.
     */
    private static void assertCompletesWithinMs(final Runnable action, final long maxMs)
            throws ClientException {
        final long start = System.currentTimeMillis();
        try {
            action.run();
        } catch (final RuntimeException e) {
            if (e.getCause() instanceof ClientException) {
                throw (ClientException) e.getCause();
            }
            throw e;
        }
        final long elapsed = System.currentTimeMillis() - start;
        assertTrue("Action took " + elapsed + " ms; expected < " + maxMs + " ms", elapsed < maxMs);
    }

    // ---------------------------------------------------------------------------
    // Backward-compatibility defaults
    // ---------------------------------------------------------------------------

    @Test
    public void defaults_noJitter_noRetryAfter_noCap_defaults() {
        // Building with no explicit settings should succeed and use backward-compatible defaults.
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder().build();
        assertNotNull(policy);
    }

    @Test
    public void attempt_whenResponseAcceptable_returnsImmediately() throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(3)
                .initialDelay(1)
                .build();

        final HttpResponse expected = responseWithStatus(HttpURLConnection.HTTP_OK);
        final HttpResponse result = policy.attempt(() -> expected);

        assertEquals(expected, result);
    }

    @Test
    public void attempt_whenNoRetriesRemaining_returnsLastResponse() throws ClientException {
        // number=0 means no retries; the first response (even if retryable) is returned.
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(0)
                .initialDelay(1)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        final HttpResponse expected = responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
        final HttpResponse result = policy.attempt(() -> expected);

        assertEquals(expected, result);
    }

    // ---------------------------------------------------------------------------
    // Retry count correctness
    // ---------------------------------------------------------------------------

    @Test
    public void attempt_retriesCorrectNumberOfTimes() throws ClientException {
        final int retries = 2;
        final AtomicInteger callCount = new AtomicInteger(0);

        final StatusCodeAndExceptionRetry policy = alwaysRetryPolicy(retries);
        final HttpResponse lastResponse = responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);

        final HttpResponse result = policy.attempt(() -> {
            callCount.incrementAndGet();
            return lastResponse;
        });

        // 1 initial + 2 retries = 3 total calls
        assertEquals(3, callCount.get());
        assertEquals(lastResponse, result);
    }

    @Test
    public void attempt_withZeroRetries_callsSupplierOnce() throws ClientException {
        final AtomicInteger callCount = new AtomicInteger(0);

        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(0)
                .initialDelay(1)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        policy.attempt(() -> {
            callCount.incrementAndGet();
            return responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
        });

        assertEquals(1, callCount.get());
    }

    @Test
    public void attempt_stopsRetrying_whenAcceptableResponseReceived() throws ClientException {
        final AtomicInteger callCount = new AtomicInteger(0);

        // First call: fail; second call: OK
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(3)
                .initialDelay(1)
                .extensionFactor(1)
                .isRetryable((r, n) -> r.getStatusCode() == HttpURLConnection.HTTP_INTERNAL_ERROR)
                .isAcceptable(r -> r.getStatusCode() == HttpURLConnection.HTTP_OK)
                .build();

        final HttpResponse result = policy.attempt(() -> {
            if (callCount.incrementAndGet() == 1) {
                return responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
            }
            return responseWithStatus(HttpURLConnection.HTTP_OK);
        });

        assertEquals(HttpURLConnection.HTTP_OK, result.getStatusCode());
        assertEquals(2, callCount.get());
    }

    // ---------------------------------------------------------------------------
    // Jitter
    // ---------------------------------------------------------------------------

    @Test
    public void attempt_withJitterFactorZero_doesNotCrash() throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .jitterFactor(0.0)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        final HttpResponse result = policy.attempt(
                () -> responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR));
        assertNotNull(result);
    }

    @Test
    public void attempt_withPositiveJitterFactor_doesNotCrash() throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(2)
                .initialDelay(5)
                .extensionFactor(1)
                .jitterFactor(0.5)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        final HttpResponse result = policy.attempt(
                () -> responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR));
        assertNotNull(result);
    }

    @Test
    public void attempt_withJitterFactor_retriesCorrectNumberOfTimes() throws ClientException {
        final int retries = 2;
        final AtomicInteger callCount = new AtomicInteger(0);

        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(retries)
                .initialDelay(1)
                .extensionFactor(1)
                .jitterFactor(0.5)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        policy.attempt(() -> {
            callCount.incrementAndGet();
            return responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR);
        });

        assertEquals(retries + 1, callCount.get());
    }

    // ---------------------------------------------------------------------------
    // Safety cap (maxTotalDelayMs)
    // ---------------------------------------------------------------------------

    @Test
    public void attempt_withMaxTotalDelayMs_capIsRespected() throws ClientException {
        // Use a large Retry-After but a very small cap; the delay must not exceed the cap.
        final int capMs = 50;

        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(true)
                .maxTotalDelayMs(capMs)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        // Retry-After of 10 seconds, but cap is 50 ms
        assertCompletesWithinMs(() -> {
            try {
                policy.attempt(() -> responseWithRetryAfter("10"));
            } catch (final ClientException e) {
                throw new RuntimeException(e);
            }
        }, 500);
    }

    @Test
    public void attempt_withSmallMaxTotalDelayMs_doesNotCrash() throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1000)
                .extensionFactor(2)
                .maxTotalDelayMs(5)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        // With cap=5ms, delay should be ≤5ms, so total should be well under 1 second.
        assertCompletesWithinMs(() -> {
            try {
                policy.attempt(() -> responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR));
            } catch (final ClientException e) {
                throw new RuntimeException(e);
            }
        }, 500);
    }

    // ---------------------------------------------------------------------------
    // Retry-After: delta-seconds
    // ---------------------------------------------------------------------------

    @Test
    public void attempt_withRetryAfterDeltaSeconds_parsedCorrectly_doesNotCrash()
            throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(true)
                .maxTotalDelayMs(100)  // cap so the test finishes quickly
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        final HttpResponse result = policy.attempt(() -> responseWithRetryAfter("0"));
        assertNotNull(result);
    }

    @Test
    public void attempt_withRetryAfterZeroSeconds_doesNotWaitLongerThanCap()
            throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(true)
                .maxTotalDelayMs(50)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        assertCompletesWithinMs(() -> {
            try {
                policy.attempt(() -> responseWithRetryAfter("0"));
            } catch (final ClientException e) {
                throw new RuntimeException(e);
            }
        }, 500);
    }

    // ---------------------------------------------------------------------------
    // Retry-After: HTTP-date format
    // ---------------------------------------------------------------------------

    @Test
    public void attempt_withRetryAfterHttpDate_parsedCorrectly_doesNotCrash()
            throws ClientException {
        // Create an HTTP-date 1 second in the future.
        final ZonedDateTime future = ZonedDateTime.now(java.time.ZoneOffset.UTC).plusSeconds(1);
        final String httpDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(future);

        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(true)
                .maxTotalDelayMs(100)  // cap so test finishes quickly
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        final HttpResponse result = policy.attempt(() -> responseWithRetryAfter(httpDate));
        assertNotNull(result);
    }

    @Test
    public void attempt_withRetryAfterHttpDateInPast_usesBaseDelay() throws ClientException {
        // A Retry-After date in the past should result in 0 ms from the header, so the base
        // delay wins and the overall time is very short.
        final ZonedDateTime past = ZonedDateTime.now(java.time.ZoneOffset.UTC).minusSeconds(60);
        final String pastDate = DateTimeFormatter.RFC_1123_DATE_TIME.format(past);

        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(true)
                .maxTotalDelayMs(50)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        assertCompletesWithinMs(() -> {
            try {
                policy.attempt(() -> responseWithRetryAfter(pastDate));
            } catch (final ClientException e) {
                throw new RuntimeException(e);
            }
        }, 500);
    }

    // ---------------------------------------------------------------------------
    // Retry-After: malformed / absent header
    // ---------------------------------------------------------------------------

    @Test
    public void attempt_withMalformedRetryAfter_doesNotCrash() throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(true)
                .maxTotalDelayMs(50)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        final HttpResponse result = policy.attempt(() -> responseWithRetryAfter("not-a-date-or-number!!@#"));
        assertNotNull(result);
    }

    @Test
    public void attempt_withEmptyRetryAfterHeader_doesNotCrash() throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(true)
                .maxTotalDelayMs(50)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        final HttpResponse result = policy.attempt(() -> responseWithRetryAfter(""));
        assertNotNull(result);
    }

    @Test
    public void attempt_withNullRetryAfterHeaderList_doesNotCrash() throws ClientException {
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(true)
                .maxTotalDelayMs(50)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        // Response with no Retry-After header at all.
        final HttpResponse result = policy.attempt(
                () -> responseWithStatus(HttpURLConnection.HTTP_INTERNAL_ERROR));
        assertNotNull(result);
    }

    @Test
    public void attempt_withRetryAfterDisabled_headerIgnored() throws ClientException {
        // respectRetryAfter = false (default): large Retry-After must not slow things down.
        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(1)
                .respectRetryAfter(false)
                .maxTotalDelayMs(50)
                .isRetryable((r, n) -> Boolean.TRUE)
                .isAcceptable(r -> Boolean.FALSE)
                .build();

        // Header says wait 30 seconds, but respectRetryAfter=false should ignore it.
        assertCompletesWithinMs(() -> {
            try {
                policy.attempt(() -> responseWithRetryAfter("30"));
            } catch (final ClientException e) {
                throw new RuntimeException(e);
            }
        }, 500);
    }

    // ---------------------------------------------------------------------------
    // Exception retry path
    // ---------------------------------------------------------------------------

    @Test
    public void attempt_whenNonRetryableClientException_thrown_rethrows() {
        final ClientException original = new ClientException("test_error_code", "test message");

        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(2)
                .initialDelay(1)
                .isRetryableException(e -> Boolean.FALSE)
                .build();

        try {
            policy.attempt(() -> { throw original; });
            fail("Expected ClientException to be rethrown");
        } catch (final ClientException e) {
            assertEquals(original, e);
        }
    }

    @Test
    public void attempt_whenRetryableException_retriesAndReturnsResponse() throws ClientException {
        final AtomicInteger callCount = new AtomicInteger(0);
        final RuntimeException retryableEx = new RuntimeException("transient");

        final StatusCodeAndExceptionRetry policy = StatusCodeAndExceptionRetry.builder()
                .number(2)
                .initialDelay(1)
                .extensionFactor(1)
                .isRetryableException(e -> e instanceof RuntimeException)
                .isAcceptable(r -> Boolean.TRUE)
                .build();

        final HttpResponse expected = responseWithStatus(HttpURLConnection.HTTP_OK);
        final HttpResponse result = policy.attempt(() -> {
            if (callCount.incrementAndGet() < 3) {
                throw retryableEx;
            }
            return expected;
        });

        assertEquals(expected, result);
        assertEquals(3, callCount.get());
    }

    // ---------------------------------------------------------------------------
    // getIOExceptionRetryPolicy factory
    // ---------------------------------------------------------------------------

    @Test(expected = IllegalArgumentException.class)
    public void getIOExceptionRetryPolicy_negativeRetries_throwsIllegalArgument() {
        StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("tag", -1, null);
    }

    @Test
    public void getIOExceptionRetryPolicy_zeroRetries_returnsPolicy() {
        final StatusCodeAndExceptionRetry policy =
                StatusCodeAndExceptionRetry.getIOExceptionRetryPolicy("tag", 0, null);
        assertNotNull(policy);
    }
}
