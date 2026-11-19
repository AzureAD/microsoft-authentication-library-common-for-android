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

import java.text.ParsePosition;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * Android API-24-compatible RFC 9110 {@code Retry-After} parser and HTTP status eligibility
 * evaluator. Deliberately avoids {@code java.time} types because core library desugaring is
 * disabled across Android auth modules.
 */
public final class RetryAfterParser {

    public static final String RETRY_AFTER_HEADER = "Retry-After";

    static final String SOURCE_DELTA_SECONDS = "delta_seconds";
    static final String SOURCE_HTTP_DATE = "http_date";
    static final String SOURCE_DEFAULT_429 = "default_429";
    static final String SOURCE_DEFAULT_5XX = "default_5xx";
    static final String SOURCE_CLAMPED_MAX = "clamped_max";

    private static final int MAX_HEADER_VALUE_LENGTH = 128;
    private static final long MILLIS_PER_SECOND = 1000L;
    private static final long MIN_BACKOFF_MILLIS = 1000L;
    private static final long FIFTY_YEARS_MILLIS = 50L * 365L * 24L * 60L * 60L * 1000L;
    private static final int ASCTIME_DOUBLE_SPACE_LENGTH = 24;

    private static final String RFC_1123_PATTERN = "EEE, dd MMM yyyy HH:mm:ss zzz";
    private static final String RFC_850_PATTERN = "EEEE, dd-MMM-yy HH:mm:ss zzz";
    private static final String ASCTIME_PATTERN = "EEE MMM d HH:mm:ss yyyy";

    private RetryAfterParser() {
        // Utility class
    }

    /**
     * Package-private result pairing resolved backoff milliseconds with telemetry source.
     */
    static final class BackoffResolution {
        final long mBackoffMillis;
        @Nullable
        final String mSource;

        BackoffResolution(final long backoffMillis, @Nullable final String source) {
            mBackoffMillis = backoffMillis;
            mSource = source;
        }
    }

    private static final class ParsedDirective {
        final long mBackoffMillis;
        @NonNull
        final String mSource;

        ParsedDirective(final long backoffMillis, @NonNull final String source) {
            mBackoffMillis = backoffMillis;
            mSource = source;
        }
    }

    /**
     * Parses the {@code Retry-After} header from HTTP response headers into positive backoff
     * milliseconds clamped to {@code [1000L, maxRetryAfterSeconds * 1000L]}.
     *
     * @param headers              HTTP response headers (nullable).
     * @param clock                monotonic/epoch clock.
     * @param maxRetryAfterSeconds upper bound in seconds (clamped to {@code >= 1}).
     * @return positive backoff duration in milliseconds, or {@code null} if absent, zero, expired, or invalid.
     */
    @Nullable
    public static Long parseRetryAfterMillis(
            @Nullable final Map<String, List<String>> headers,
            @NonNull final MonotonicClock clock,
            final long maxRetryAfterSeconds) {
        final ParsedDirective parsed = parseRetryAfterDirective(headers, clock, maxRetryAfterSeconds);
        return parsed != null ? Long.valueOf(parsed.mBackoffMillis) : null;
    }

    /**
     * Resolves the authoritative backoff duration in milliseconds for an HTTP token endpoint
     * response, or {@code <= 0} if the response is not eligible for server-directed throttling.
     *
     * @param httpStatusCode       HTTP status code.
     * @param headers              HTTP response headers (nullable).
     * @param clock                monotonic/epoch clock.
     * @param default429Seconds    fallback duration in seconds for HTTP 429 without valid Retry-After.
     * @param default5xxSeconds    fallback duration in seconds for HTTP 5xx without valid Retry-After.
     * @param maxRetryAfterSeconds maximum allowed backoff in seconds.
     * @return positive backoff in milliseconds when eligible, or {@code 0L} when not throttled.
     */
    public static long resolveBackoffMillis(
            final int httpStatusCode,
            @Nullable final Map<String, List<String>> headers,
            @NonNull final MonotonicClock clock,
            final long default429Seconds,
            final long default5xxSeconds,
            final long maxRetryAfterSeconds) {
        return resolveBackoff(
                httpStatusCode,
                headers,
                clock,
                default429Seconds,
                default5xxSeconds,
                maxRetryAfterSeconds
        ).mBackoffMillis;
    }

    /**
     * Builds an unmodifiable single-entry {@code Retry-After} header map in whole positive seconds.
     *
     * @param retryAfterSeconds remaining backoff seconds (clamped to {@code >= 1}).
     * @return unmodifiable header map.
     */
    @NonNull
    public static Map<String, List<String>> singletonRetryAfterHeader(final long retryAfterSeconds) {
        final long clampedSeconds = Math.max(1L, retryAfterSeconds);
        final Map<String, List<String>> map = new LinkedHashMap<>(1);
        map.put(RETRY_AFTER_HEADER, Collections.singletonList(String.valueOf(clampedSeconds)));
        return Collections.unmodifiableMap(map);
    }

    @NonNull
    static BackoffResolution resolveBackoff(
            final int httpStatusCode,
            @Nullable final Map<String, List<String>> headers,
            @NonNull final MonotonicClock clock,
            final long default429Seconds,
            final long default5xxSeconds,
            final long maxRetryAfterSeconds) {
        if (httpStatusCode < 300 || httpStatusCode >= 600) {
            return new BackoffResolution(0L, null);
        }

        final long effectiveMaxMillis = secondsToMillisClamped(Math.max(1L, maxRetryAfterSeconds));
        final ParsedDirective parsed = parseRetryAfterDirective(headers, clock, maxRetryAfterSeconds);
        if (parsed != null) {
            return new BackoffResolution(parsed.mBackoffMillis, parsed.mSource);
        }

        if (httpStatusCode == 429) {
            final long fallbackMillis = secondsToMillisClamped(Math.max(1L, default429Seconds));
            if (fallbackMillis > effectiveMaxMillis) {
                return new BackoffResolution(effectiveMaxMillis, SOURCE_CLAMPED_MAX);
            }
            return new BackoffResolution(Math.max(MIN_BACKOFF_MILLIS, fallbackMillis), SOURCE_DEFAULT_429);
        }

        if (httpStatusCode >= 500) {
            final long fallbackMillis = secondsToMillisClamped(Math.max(1L, default5xxSeconds));
            if (fallbackMillis > effectiveMaxMillis) {
                return new BackoffResolution(effectiveMaxMillis, SOURCE_CLAMPED_MAX);
            }
            return new BackoffResolution(Math.max(MIN_BACKOFF_MILLIS, fallbackMillis), SOURCE_DEFAULT_5XX);
        }

        return new BackoffResolution(0L, null);
    }

    @Nullable
    private static ParsedDirective parseRetryAfterDirective(
            @Nullable final Map<String, List<String>> headers,
            @NonNull final MonotonicClock clock,
            final long maxRetryAfterSeconds) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }

        final long effectiveMaxSeconds = Math.max(1L, maxRetryAfterSeconds);
        final long maxRetryAfterMillis = secondsToMillisClamped(effectiveMaxSeconds);

        boolean nowInitialized = false;
        long nowMillis = 0L;

        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            final String headerName = entry.getKey();
            if (headerName == null || !RETRY_AFTER_HEADER.equalsIgnoreCase(headerName.trim())) {
                continue;
            }
            final List<String> values = entry.getValue();
            if (values == null) {
                continue;
            }
            for (final String rawValue : values) {
                if (rawValue == null || rawValue.length() > MAX_HEADER_VALUE_LENGTH) {
                    continue;
                }
                final String trimmed = rawValue.trim();
                if (trimmed.isEmpty()) {
                    continue;
                }

                if (isAllAsciiDigits(trimmed)) {
                    final ParsedDirective deltaDirective = parsePositiveDeltaSeconds(
                            trimmed,
                            effectiveMaxSeconds,
                            maxRetryAfterMillis
                    );
                    if (deltaDirective != null) {
                        return deltaDirective;
                    }
                    continue;
                }

                if (!nowInitialized) {
                    nowMillis = clock.currentTimeMillis();
                    nowInitialized = true;
                }
                final ParsedDirective dateDirective = parseHttpDateDirective(
                        trimmed,
                        nowMillis,
                        maxRetryAfterMillis
                );
                if (dateDirective != null) {
                    return dateDirective;
                }
            }
        }
        return null;
    }

    private static boolean isAllAsciiDigits(@NonNull final String value) {
        final int len = value.length();
        if (len == 0) {
            return false;
        }
        for (int i = 0; i < len; i++) {
            final char ch = value.charAt(i);
            if (ch < '0' || ch > '9') {
                return false;
            }
        }
        return true;
    }

    @Nullable
    private static ParsedDirective parsePositiveDeltaSeconds(
            @NonNull final String digits,
            final long effectiveMaxSeconds,
            final long maxRetryAfterMillis) {
        final int len = digits.length();
        long seconds = 0L;
        boolean clamped = false;
        for (int i = 0; i < len; i++) {
            final int digit = digits.charAt(i) - '0';
            if (seconds > (Long.MAX_VALUE - digit) / 10L) {
                clamped = true;
                break;
            }
            seconds = seconds * 10L + digit;
            if (seconds > effectiveMaxSeconds) {
                clamped = true;
                break;
            }
        }
        if (!clamped && seconds <= 0L) {
            return null;
        }
        if (clamped) {
            return new ParsedDirective(maxRetryAfterMillis, SOURCE_CLAMPED_MAX);
        }
        final long millis = Math.min(secondsToMillisClamped(seconds), maxRetryAfterMillis);
        return new ParsedDirective(Math.max(MIN_BACKOFF_MILLIS, millis), SOURCE_DELTA_SECONDS);
    }

    @Nullable
    private static ParsedDirective parseHttpDateDirective(
            @NonNull final String trimmed,
            final long nowMillis,
            final long maxRetryAfterMillis) {
        Date parsedDate = parseStrictHttpDate(trimmed, RFC_1123_PATTERN, null);
        if (parsedDate == null) {
            parsedDate = parseStrictHttpDate(
                    trimmed,
                    RFC_850_PATTERN,
                    new Date(nowMillis - FIFTY_YEARS_MILLIS)
            );
        }
        if (parsedDate == null) {
            parsedDate = parseStrictHttpDate(normalizeAsctime(trimmed), ASCTIME_PATTERN, null);
        }
        if (parsedDate == null) {
            return null;
        }

        final long rawDeltaMillis = parsedDate.getTime() - nowMillis;
        if (rawDeltaMillis <= 0L) {
            return null;
        }
        if (rawDeltaMillis > maxRetryAfterMillis) {
            return new ParsedDirective(maxRetryAfterMillis, SOURCE_CLAMPED_MAX);
        }
        return new ParsedDirective(Math.max(MIN_BACKOFF_MILLIS, rawDeltaMillis), SOURCE_HTTP_DATE);
    }

    @Nullable
    private static Date parseStrictHttpDate(
            @NonNull final String candidate,
            @NonNull final String pattern,
            @Nullable final Date twoDigitYearStart) {
        final SimpleDateFormat formatter = new SimpleDateFormat(pattern, Locale.US);
        formatter.setTimeZone(TimeZone.getTimeZone("GMT"));
        formatter.setLenient(false);
        if (twoDigitYearStart != null) {
            formatter.set2DigitYearStart(twoDigitYearStart);
        }
        final ParsePosition position = new ParsePosition(0);
        final Date parsed;
        try {
            parsed = formatter.parse(candidate, position);
        } catch (final RuntimeException ignored) {
            return null;
        }
        if (parsed == null
                || position.getErrorIndex() != -1
                || position.getIndex() != candidate.length()) {
            return null;
        }
        return parsed;
    }

    @NonNull
    private static String normalizeAsctime(@NonNull final String candidate) {
        if (candidate.length() == ASCTIME_DOUBLE_SPACE_LENGTH
                && candidate.charAt(3) == ' '
                && candidate.charAt(7) == ' '
                && candidate.charAt(8) == ' '
                && Character.isDigit(candidate.charAt(9))) {
            return candidate.substring(0, 8) + candidate.substring(9);
        }
        return candidate;
    }

    private static long secondsToMillisClamped(final long seconds) {
        if (seconds <= 0L) {
            return 0L;
        }
        if (seconds > Long.MAX_VALUE / MILLIS_PER_SECOND) {
            return Long.MAX_VALUE;
        }
        return seconds * MILLIS_PER_SECOND;
    }
}
