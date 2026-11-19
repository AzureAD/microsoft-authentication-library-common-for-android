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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightConfig;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import lombok.NonNull;

/**
 * Deterministic unit tests for {@link RetryAfterParser}.
 */
@RunWith(JUnit4.class)
public class RetryAfterParserTest {

    // Sun, 06 Nov 1994 08:49:37 GMT == 784_111_777_000L epoch ms
    private static final long RFC_9110_EXAMPLE_EPOCH_MS = 784_111_777_000L;
    // Fri, 13 Nov 2026 12:00:00 GMT == 1_794_571_200_000L epoch ms
    private static final long BASE_2026_EPOCH_MS = 1_794_571_200_000L;

    private static final long DEFAULT_429_SECONDS = 60L;
    private static final long DEFAULT_5XX_SECONDS = 120L;
    private static final long MAX_RETRY_AFTER_SECONDS = 3600L;

    private FakeMonotonicClock mClock;

    @Before
    public void setUp() {
        mClock = new FakeMonotonicClock(1_000_000_000L, BASE_2026_EPOCH_MS);
    }

    @After
    public void tearDown() {
        ServerThrottleManager.setBrokerHostProcess(false);
        ServerThrottleManager.getInstance().clearAll();
        ServerThrottleManager.setInstanceForTesting(null);
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void parseRetryAfterMillis_deltaSeconds_parsesCaseInsensitiveAndWhitespace() {
        assertEquals(
                Long.valueOf(1_000L),
                RetryAfterParser.parseRetryAfterMillis(
                        headerMap("Retry-After", "1"),
                        mClock,
                        MAX_RETRY_AFTER_SECONDS
                )
        );
        assertEquals(
                Long.valueOf(120_000L),
                RetryAfterParser.parseRetryAfterMillis(
                        headerMap("retry-after", "120"),
                        mClock,
                        MAX_RETRY_AFTER_SECONDS
                )
        );
        assertEquals(
                Long.valueOf(45_000L),
                RetryAfterParser.parseRetryAfterMillis(
                        headerMap("  rEtRy-AfTeR  ", "  45 \t "),
                        mClock,
                        MAX_RETRY_AFTER_SECONDS
                )
        );

        final Map<String, List<String>> multiValueHeaders = new LinkedHashMap<>();
        multiValueHeaders.put("RETRY-AFTER", Arrays.asList("", "   ", "not-a-date", "30", "90"));
        final RetryAfterParser.BackoffResolution resolution = RetryAfterParser.resolveBackoff(
                429,
                multiValueHeaders,
                mClock,
                DEFAULT_429_SECONDS,
                DEFAULT_5XX_SECONDS,
                MAX_RETRY_AFTER_SECONDS
        );
        assertEquals(30_000L, resolution.mBackoffMillis);
        assertEquals(RetryAfterParser.SOURCE_DELTA_SECONDS, resolution.mSource);
    }

    @Test
    public void parseRetryAfterMillis_zeroNegativeMalformedOverflowAndLongHeaderValues() {
        assertNull(RetryAfterParser.parseRetryAfterMillis(null, mClock, MAX_RETRY_AFTER_SECONDS));
        assertNull(RetryAfterParser.parseRetryAfterMillis(
                Collections.<String, List<String>>emptyMap(),
                mClock,
                MAX_RETRY_AFTER_SECONDS
        ));

        final Map<String, List<String>> nullKeyAndList = new HashMap<>();
        nullKeyAndList.put(null, Collections.singletonList("10"));
        nullKeyAndList.put("Retry-After", null);
        assertNull(RetryAfterParser.parseRetryAfterMillis(nullKeyAndList, mClock, MAX_RETRY_AFTER_SECONDS));

        for (final String invalid : new String[]{"0", "000", "-1", "-60", "+10", "1.5", "abc", "", "   "}) {
            assertNull(
                    "Expected null for directive: " + invalid,
                    RetryAfterParser.parseRetryAfterMillis(
                            headerMap("Retry-After", invalid),
                            mClock,
                            MAX_RETRY_AFTER_SECONDS
                    )
            );
        }

        // > 128-character individual header value must be ignored
        final StringBuilder overLimit = new StringBuilder(130);
        for (int i = 0; i < 127; i++) {
            overLimit.append(' ');
        }
        overLimit.append("15"); // length 129 > 128
        assertEquals(129, overLimit.length());
        assertNull(RetryAfterParser.parseRetryAfterMillis(
                headerMap("Retry-After", overLimit.toString()),
                mClock,
                MAX_RETRY_AFTER_SECONDS
        ));

        // Subsequent valid value in list still wins when >128-char value is skipped
        final Map<String, List<String>> fallbackAfterLong = new LinkedHashMap<>();
        fallbackAfterLong.put("Retry-After", Arrays.asList(overLimit.toString(), "25"));
        assertEquals(
                Long.valueOf(25_000L),
                RetryAfterParser.parseRetryAfterMillis(fallbackAfterLong, mClock, MAX_RETRY_AFTER_SECONDS)
        );

        // Clamping above maxRetryAfterSeconds (3600s) and huge integer overflow
        final RetryAfterParser.BackoffResolution aboveCap = RetryAfterParser.resolveBackoff(
                429,
                headerMap("Retry-After", "7200"),
                mClock,
                DEFAULT_429_SECONDS,
                DEFAULT_5XX_SECONDS,
                MAX_RETRY_AFTER_SECONDS
        );
        assertEquals(3_600_000L, aboveCap.mBackoffMillis);
        assertEquals(RetryAfterParser.SOURCE_CLAMPED_MAX, aboveCap.mSource);

        final RetryAfterParser.BackoffResolution hugeOverflow = RetryAfterParser.resolveBackoff(
                429,
                headerMap("Retry-After", "999999999999999999999999999999"),
                mClock,
                DEFAULT_429_SECONDS,
                DEFAULT_5XX_SECONDS,
                MAX_RETRY_AFTER_SECONDS
        );
        assertEquals(3_600_000L, hugeOverflow.mBackoffMillis);
        assertEquals(RetryAfterParser.SOURCE_CLAMPED_MAX, hugeOverflow.mSource);
    }

    @Test
    public void parseRetryAfterMillis_httpDateFormats_rfc1123Rfc850AndSingleOrDoubleSpaceAsctime() {
        // Anchor 90s before RFC 9110 §5.6.7 canonical timestamp (Sun, 06 Nov 1994 08:49:37 GMT)
        mClock.setCurrentTimeMillis(RFC_9110_EXAMPLE_EPOCH_MS - 90_000L);

        final String[] rfc9110Variants = new String[]{
                "Sun, 06 Nov 1994 08:49:37 GMT",      // IMF-fixdate / RFC 1123
                "Sunday, 06-Nov-94 08:49:37 GMT",     // Obsolete RFC 850
                "Sun Nov  6 08:49:37 1994",           // ANSI C asctime() double-space single-digit day
                "Sun Nov 6 08:49:37 1994"             // ANSI C asctime() single-space single-digit day
        };
        for (final String dateHeader : rfc9110Variants) {
            final RetryAfterParser.BackoffResolution res = RetryAfterParser.resolveBackoff(
                    400,
                    headerMap("Retry-After", dateHeader),
                    mClock,
                    DEFAULT_429_SECONDS,
                    DEFAULT_5XX_SECONDS,
                    MAX_RETRY_AFTER_SECONDS
            );
            assertEquals("Failed for header: " + dateHeader, 90_000L, res.mBackoffMillis);
            assertEquals(RetryAfterParser.SOURCE_HTTP_DATE, res.mSource);
        }

        // Modern two-digit day (Fri, 13 Nov 2026 12:02:30 GMT == BASE_2026_EPOCH_MS + 150_000L)
        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS);
        for (final String modernHeader : new String[]{
                "Fri, 13 Nov 2026 12:02:30 GMT",
                "Friday, 13-Nov-26 12:02:30 GMT",
                "Fri Nov 13 12:02:30 2026"
        }) {
            assertEquals(
                    "Failed for modern header: " + modernHeader,
                    Long.valueOf(150_000L),
                    RetryAfterParser.parseRetryAfterMillis(
                            headerMap("Retry-After", modernHeader),
                            mClock,
                            MAX_RETRY_AFTER_SECONDS
                    )
            );
        }

        // Positive sub-second HTTP-date delta (+250ms) clamps up to minimum 1000ms
        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS + 150_000L - 250L);
        assertEquals(
                Long.valueOf(1_000L),
                RetryAfterParser.parseRetryAfterMillis(
                        headerMap("Retry-After", "Fri, 13 Nov 2026 12:02:30 GMT"),
                        mClock,
                        MAX_RETRY_AFTER_SECONDS
                )
        );

        // Future HTTP-date beyond 3600s (+7200s -> Fri, 13 Nov 2026 14:00:00 GMT) clamps to 3600s
        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS);
        final RetryAfterParser.BackoffResolution clampedFutureDate = RetryAfterParser.resolveBackoff(
                429,
                headerMap("Retry-After", "Fri, 13 Nov 2026 14:00:00 GMT"),
                mClock,
                DEFAULT_429_SECONDS,
                DEFAULT_5XX_SECONDS,
                MAX_RETRY_AFTER_SECONDS
        );
        assertEquals(3_600_000L, clampedFutureDate.mBackoffMillis);
        assertEquals(RetryAfterParser.SOURCE_CLAMPED_MAX, clampedFutureDate.mSource);

        // Equal (0ms) and past (-10s) HTTP-dates return null
        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS + 150_000L);
        assertNull(RetryAfterParser.parseRetryAfterMillis(
                headerMap("Retry-After", "Fri, 13 Nov 2026 12:02:30 GMT"),
                mClock,
                MAX_RETRY_AFTER_SECONDS
        ));
        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS + 160_000L);
        assertNull(RetryAfterParser.parseRetryAfterMillis(
                headerMap("Retry-After", "Fri, 13 Nov 2026 12:02:30 GMT"),
                mClock,
                MAX_RETRY_AFTER_SECONDS
        ));
    }

    @Test
    public void parseRetryAfterMillis_nonLenientRejectsInvalidCalendarDatesAndTrailingGarbage() {
        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS - TimeUnit.DAYS.toMillis(300));
        final String[] malformedDates = new String[]{
                "Fri, 30 Feb 2026 12:00:00 GMT",                  // Invalid calendar date (Feb 30)
                "Fri, 13 Nov 2026 25:00:00 GMT",                  // Invalid hour 25
                "Fri, 13 Nov 2026 12:60:00 GMT",                  // Invalid minute 60
                "Fri, 13 Nov 2026 12:02:30 GMT trailing-garbage", // Unconsumed trailing text
                "Fri Nov 13 12:02:30 2026 GMT"                    // Trailing timezone on asctime
        };
        for (final String badDate : malformedDates) {
            assertNull(
                    "Expected non-lenient rejection for: " + badDate,
                    RetryAfterParser.parseRetryAfterMillis(
                            headerMap("Retry-After", badDate),
                            mClock,
                            MAX_RETRY_AFTER_SECONDS
                    )
            );
        }
    }

    @Test
    public void parseRetryAfterMillis_nonUsDefaultLocaleInvariance() {
        final Locale originalLocale = Locale.getDefault();
        try {
            for (final Locale testLocale : new Locale[]{Locale.FRANCE, Locale.GERMANY, new Locale("ar", "SA")}) {
                Locale.setDefault(testLocale);
                mClock.setCurrentTimeMillis(RFC_9110_EXAMPLE_EPOCH_MS - 45_000L);
                assertEquals(
                        Long.valueOf(45_000L),
                        RetryAfterParser.parseRetryAfterMillis(
                                headerMap("Retry-After", "Sun, 06 Nov 1994 08:49:37 GMT"),
                                mClock,
                                MAX_RETRY_AFTER_SECONDS
                        )
                );
                assertEquals(
                        Long.valueOf(45_000L),
                        RetryAfterParser.parseRetryAfterMillis(
                                headerMap("Retry-After", "Sunday, 06-Nov-94 08:49:37 GMT"),
                                mClock,
                                MAX_RETRY_AFTER_SECONDS
                        )
                );
                assertEquals(
                        Long.valueOf(45_000L),
                        RetryAfterParser.parseRetryAfterMillis(
                                headerMap("Retry-After", "Sun Nov  6 08:49:37 1994"),
                                mClock,
                                MAX_RETRY_AFTER_SECONDS
                        )
                );
            }
        } finally {
            Locale.setDefault(originalLocale);
        }
    }

    @Test
    public void productionThrottlingClasses_haveZeroJavaTimeBytecodeReferences() throws IOException {
        final Class<?>[] throttlingClasses = new Class<?>[]{
                RetryAfterParser.class,
                ServerThrottleManager.class,
                ThrottleRecord.class,
                TokenThrottleKey.class,
                LiveThrottleCapture.class,
                TokenThrottleContext.class,
                MonotonicClock.class,
                ThrottleScopeType.class,
                ThrottleOutcome.class
        };
        final String forbiddenInternalPkg = "java/" + "time/";
        for (final Class<?> clazz : throttlingClasses) {
            final String resourceName = clazz.getSimpleName() + ".class";
            try (InputStream in = clazz.getResourceAsStream(resourceName)) {
                assertNotNull("Missing class resource for " + clazz.getName(), in);
                final ByteArrayOutputStream out = new ByteArrayOutputStream();
                final byte[] buffer = new byte[1024];
                int read;
                while ((read = in.read(buffer)) != -1) {
                    out.write(buffer, 0, read);
                }
                final String latin1 = new String(out.toByteArray(), StandardCharsets.ISO_8859_1);
                assertFalse(
                        clazz.getName() + " must not reference java.time.* under desugar=false",
                        latin1.contains(forbiddenInternalPkg)
                );
            }
        }
    }

    @Test
    public void httpDateConvertedOnceToNanoTime_isImmuneToSubsequentWallClockJumps() {
        enableBrokerThrottlingFlight();
        final ServerThrottleManager manager = new ServerThrottleManager(mClock);
        ServerThrottleManager.setInstanceForTesting(manager);

        final TokenThrottleKey key = TokenThrottleKey.forBrokerPrt(
                "https://login.microsoftonline.com/common",
                "home-acct-1",
                "refresh_token",
                null
        );

        // Record HTTP-date 120s in the future at nanoTime = 5_000_000_000L
        mClock.setNanoTime(5_000_000_000L);
        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS);
        final HttpResponse http429WithDate = new HttpResponse(
                429,
                "",
                headerMap("Retry-After", "Fri, 13 Nov 2026 12:02:00 GMT")
        );

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-1")) {
            assertTrue(manager.onHttpResponse(scope.context(), http429WithDate, null, null));
        }

        // Advance monotonic clock by 60s, but jump wall clock forward by +10 hours and backward by -10 hours
        mClock.setNanoTime(5_000_000_000L + TimeUnit.SECONDS.toNanos(60));
        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS + TimeUnit.HOURS.toMillis(10));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-2")) {
            final TokenResult suppressedForwardJump = manager.checkPreFlight(scope.context());
            assertNotNull(suppressedForwardJump);
            assertEquals(
                    Collections.singletonList("60"),
                    HeaderSerializationUtil.fromJson(
                            suppressedForwardJump.getErrorResponse().getResponseHeadersJson()
                    ).get("Retry-After")
            );
        }

        mClock.setCurrentTimeMillis(BASE_2026_EPOCH_MS - TimeUnit.HOURS.toMillis(10));
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-3")) {
            final TokenResult suppressedBackwardJump = manager.checkPreFlight(scope.context());
            assertNotNull(suppressedBackwardJump);
        }

        // Advance monotonic clock to exact 120s deadline -> expires regardless of backward wall clock
        mClock.setNanoTime(5_000_000_000L + TimeUnit.SECONDS.toNanos(120));
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-4")) {
            assertNull(manager.checkPreFlight(scope.context()));
        }
    }

    @Test
    public void resolveBackoffMillis_statusCodeEligibilityAndFallbacks() {
        // HTTP 429: default 60s when missing, "0", or expired HTTP-date; honors valid Retry-After
        assertEquals(
                60_000L,
                RetryAfterParser.resolveBackoffMillis(
                        429, null, mClock, DEFAULT_429_SECONDS, DEFAULT_5XX_SECONDS, MAX_RETRY_AFTER_SECONDS
                )
        );
        assertEquals(
                60_000L,
                RetryAfterParser.resolveBackoffMillis(
                        429,
                        headerMap("Retry-After", "0"),
                        mClock,
                        DEFAULT_429_SECONDS,
                        DEFAULT_5XX_SECONDS,
                        MAX_RETRY_AFTER_SECONDS
                )
        );
        assertEquals(
                15_000L,
                RetryAfterParser.resolveBackoffMillis(
                        429,
                        headerMap("Retry-After", "15"),
                        mClock,
                        DEFAULT_429_SECONDS,
                        DEFAULT_5XX_SECONDS,
                        MAX_RETRY_AFTER_SECONDS
                )
        );

        // HTTP 5xx: default 120s when missing/expired; honors valid Retry-After
        for (final int status5xx : new int[]{500, 502, 503, 504, 599}) {
            final RetryAfterParser.BackoffResolution fallback5xx = RetryAfterParser.resolveBackoff(
                    status5xx,
                    null,
                    mClock,
                    DEFAULT_429_SECONDS,
                    DEFAULT_5XX_SECONDS,
                    MAX_RETRY_AFTER_SECONDS
            );
            assertEquals(120_000L, fallback5xx.mBackoffMillis);
            assertEquals(RetryAfterParser.SOURCE_DEFAULT_5XX, fallback5xx.mSource);
        }
        assertEquals(
                25_000L,
                RetryAfterParser.resolveBackoffMillis(
                        503,
                        headerMap("Retry-After", "25"),
                        mClock,
                        DEFAULT_429_SECONDS,
                        DEFAULT_5XX_SECONDS,
                        MAX_RETRY_AFTER_SECONDS
                )
        );

        // Default fallback exceeding maxRetryAfterSeconds is clamped
        final RetryAfterParser.BackoffResolution clampedDefault429 = RetryAfterParser.resolveBackoff(
                429, null, mClock, 120L, 120L, 30L
        );
        assertEquals(30_000L, clampedDefault429.mBackoffMillis);
        assertEquals(RetryAfterParser.SOURCE_CLAMPED_MAX, clampedDefault429.mSource);

        final RetryAfterParser.BackoffResolution clampedDefault5xx = RetryAfterParser.resolveBackoff(
                503, null, mClock, 60L, 120L, 45L
        );
        assertEquals(45_000L, clampedDefault5xx.mBackoffMillis);
        assertEquals(RetryAfterParser.SOURCE_CLAMPED_MAX, clampedDefault5xx.mSource);

        // Other non-2xx (302, 400, 401, 403): throttled ONLY with valid positive future Retry-After
        for (final int non2xx : new int[]{302, 400, 401, 403}) {
            assertEquals(
                    40_000L,
                    RetryAfterParser.resolveBackoffMillis(
                            non2xx,
                            headerMap("Retry-After", "40"),
                            mClock,
                            DEFAULT_429_SECONDS,
                            DEFAULT_5XX_SECONDS,
                            MAX_RETRY_AFTER_SECONDS
                    )
            );
            assertEquals(
                    0L,
                    RetryAfterParser.resolveBackoffMillis(
                            non2xx,
                            null,
                            mClock,
                            DEFAULT_429_SECONDS,
                            DEFAULT_5XX_SECONDS,
                            MAX_RETRY_AFTER_SECONDS
                    )
            );
            assertEquals(
                    0L,
                    RetryAfterParser.resolveBackoffMillis(
                            non2xx,
                            headerMap("Retry-After", "0"),
                            mClock,
                            DEFAULT_429_SECONDS,
                            DEFAULT_5XX_SECONDS,
                            MAX_RETRY_AFTER_SECONDS
                    )
            );
        }

        // HTTP 2xx and out-of-range codes: NEVER throttled even if Retry-After is present
        for (final int nonEligible : new int[]{100, 200, 201, 204, 299, 600}) {
            assertEquals(
                    0L,
                    RetryAfterParser.resolveBackoffMillis(
                            nonEligible,
                            headerMap("Retry-After", "60"),
                            mClock,
                            DEFAULT_429_SECONDS,
                            DEFAULT_5XX_SECONDS,
                            MAX_RETRY_AFTER_SECONDS
                    )
            );
        }
    }

    @Test
    public void singletonRetryAfterHeader_clampsMinimumAndReturnsUnmodifiableMap() {
        final Map<String, List<String>> normal = RetryAfterParser.singletonRetryAfterHeader(42L);
        assertEquals(Collections.singletonList("42"), normal.get(RetryAfterParser.RETRY_AFTER_HEADER));

        final Map<String, List<String>> zeroClamped = RetryAfterParser.singletonRetryAfterHeader(0L);
        assertEquals(Collections.singletonList("1"), zeroClamped.get(RetryAfterParser.RETRY_AFTER_HEADER));

        final Map<String, List<String>> negativeClamped = RetryAfterParser.singletonRetryAfterHeader(-15L);
        assertEquals(Collections.singletonList("1"), negativeClamped.get(RetryAfterParser.RETRY_AFTER_HEADER));

        try {
            normal.put("X-Extra", Collections.singletonList("1"));
            fail("Expected singletonRetryAfterHeader to return an unmodifiable map");
        } catch (final UnsupportedOperationException expected) {
            // Expected
        }
    }

    private static Map<String, List<String>> headerMap(final String key, final String value) {
        final Map<String, List<String>> map = new LinkedHashMap<>();
        map.put(key, Collections.singletonList(value));
        return map;
    }

    private static void enableBrokerThrottlingFlight() {
        final MockFlightsProvider provider = new MockFlightsProvider() {
            @Override
            public int getIntValue(@NonNull final IFlightConfig flightConfig) {
                return ((Integer) flightConfig.getDefaultValue()).intValue();
            }
        };
        provider.addFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "true");
        final MockFlightsManager flightsManager = new MockFlightsManager();
        flightsManager.setMockBrokerFlightsProvider(provider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(flightsManager);
        ServerThrottleManager.setBrokerHostProcess(true);
    }

    private static final class FakeMonotonicClock implements MonotonicClock {
        private long mNanoTime;
        private long mCurrentTimeMillis;

        FakeMonotonicClock(final long nanoTime, final long currentTimeMillis) {
            mNanoTime = nanoTime;
            mCurrentTimeMillis = currentTimeMillis;
        }

        void setNanoTime(final long nanoTime) {
            mNanoTime = nanoTime;
        }

        void setCurrentTimeMillis(final long currentTimeMillis) {
            mCurrentTimeMillis = currentTimeMillis;
        }

        @Override
        public long nanoTime() {
            return mNanoTime;
        }

        @Override
        public long currentTimeMillis() {
            return mCurrentTimeMillis;
        }
    }
}
