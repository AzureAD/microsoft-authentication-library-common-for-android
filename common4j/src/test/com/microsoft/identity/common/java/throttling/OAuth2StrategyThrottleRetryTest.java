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

import static com.microsoft.identity.common.java.AuthenticationConstants.Broker.CHALLENGE_REQUEST_HEADER;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.AuthenticationSettings;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightConfig;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.HttpUrlConnectionFactory;
import com.microsoft.identity.common.java.net.StatusCodeAndExceptionRetry;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Configuration;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsOAuth2Strategy;
import com.microsoft.identity.common.java.providers.microsoft.microsoftsts.MicrosoftStsTokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.ITokenResponseHandler;
import com.microsoft.identity.common.java.providers.oauth2.OAuth2StrategyParameters;
import com.microsoft.identity.common.java.providers.oauth2.TokenRequest;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;
import com.microsoft.identity.common.java.util.ported.Function;
import com.microsoft.identity.http.MockConnection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BiFunction;

import javax.net.ssl.HttpsURLConnection;

import lombok.NonNull;

/**
 * Deterministic unit tests for outbound HTTP and PKeyAuth retry suppression and response capture
 * across {@link StatusCodeAndExceptionRetry}, {@link UrlConnectionHttpClient},
 * {@link com.microsoft.identity.common.java.providers.oauth2.OAuth2Strategy}, and
 * {@link MicrosoftStsOAuth2Strategy}.
 */
@RunWith(JUnit4.class)
public class OAuth2StrategyThrottleRetryTest {

    private static final String AUTHORITY_URL = "https://login.microsoftonline.com/common";
    private static final String TOKEN_ENDPOINT_URL = "https://login.microsoftonline.com/common/oAuth2/v2.0/token";

    private static final String PKEYAUTH_CHALLENGE_VALUE =
            "PKeyAuth CertAuthorities=\"OU=82dbaca4-3e81-46ca-9c73-0950c1eaca97,CN=MS-Organization-Access,DC=windows,DC=net\", "
                    + "Version=\"1.0\", Context=\"ctx-pkey-test\", nonce=\"nonce-pkey-test\"";

    private static final String MOCK_TOKEN_SUCCESS_JSON = "{"
            + "\"token_type\":\"Bearer\","
            + "\"scope\":\"User.Read\","
            + "\"expires_in\":3599,"
            + "\"ext_expires_in\":3599,"
            + "\"access_token\":\"mock-access-token\","
            + "\"refresh_token\":\"mock-refresh-token\","
            + "\"id_token\":\"mock-id-token\","
            + "\"client_info\":\"mock-client-info\""
            + "}";

    private FakeClock mClock;
    private ToggleFlightsProvider mFlightsProvider;
    private ServerThrottleManager mManager;
    private TokenThrottleKey mKey;

    @Before
    public void setUp() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        AuthenticationSettings.INSTANCE.setCertificateLoader(null);

        mClock = new FakeClock(20_000_000_000L, 1_794_571_200_000L);
        mFlightsProvider = new ToggleFlightsProvider();
        mFlightsProvider.setFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "true");
        final MockFlightsManager flightsManager = new MockFlightsManager();
        flightsManager.setMockBrokerFlightsProvider(mFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(flightsManager);

        ServerThrottleManager.setBrokerHostProcess(true);
        mManager = new ServerThrottleManager(mClock);
        ServerThrottleManager.setInstanceForTesting(mManager);

        mKey = TokenThrottleKey.forAtOrRt(
                AUTHORITY_URL,
                "client-1",
                Collections.singletonList("User.Read"),
                null,
                "uid.utid",
                "Bearer",
                null,
                "REGULAR",
                null
        );
    }

    @After
    public void tearDown() {
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        AuthenticationSettings.INSTANCE.setCertificateLoader(null);
        ServerThrottleManager.setBrokerHostProcess(false);
        mManager.clearAll();
        ServerThrottleManager.getInstance().clearAll();
        ServerThrottleManager.setInstanceForTesting(null);
        CommonFlightsManager.INSTANCE.resetFlightsManager();
    }

    @Test
    public void statusCodeAndExceptionRetry_suppresses500_503_504UnderActiveBrokerThrottleAndPreservesBaselineRetries() throws Exception {
        final StatusCodeAndExceptionRetry retryPolicy = StatusCodeAndExceptionRetry.builder()
                .number(1)
                .initialDelay(1)
                .extensionFactor(2)
                .isAcceptable(new Function<HttpResponse, Boolean>() {
                    @Override
                    public Boolean apply(final HttpResponse response) {
                        return response != null && response.getStatusCode() < 400;
                    }
                })
                .isRetryable(new BiFunction<HttpResponse, Integer, Boolean>() {
                    @Override
                    public Boolean apply(final HttpResponse response, final Integer attemptNumber) {
                        return response != null && UrlConnectionHttpClient.isRetryableError(response.getStatusCode());
                    }
                })
                .build();

        // 1. Active Broker TokenThrottleContext suppresses retry on 500, 503, and 504 (with or without Retry-After)
        for (final int statusCode : new int[]{500, 503, 504}) {
            final AtomicInteger attempts = new AtomicInteger(0);
            try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-5xx")) {
                final HttpResponse result = retryPolicy.attempt(new Callable<HttpResponse>() {
                    @Override
                    public HttpResponse call() {
                        final int callIndex = attempts.incrementAndGet();
                        if (callIndex == 1) {
                            return new HttpResponse(
                                    statusCode,
                                    "{}",
                                    statusCode == 503
                                            ? Collections.singletonMap("Retry-After", Collections.singletonList("90"))
                                            : null
                            );
                        }
                        return new HttpResponse(200, "{}", null);
                    }
                });
                assertEquals("Expected immediate return without retry for status " + statusCode,
                        statusCode, result.getStatusCode());
                assertEquals("Expected exactly 1 attempt for status " + statusCode, 1, attempts.get());
            }
        }

        // 2. Without an active TokenThrottleContext (e.g. non-token endpoints), baseline 1-retry executes
        final AtomicInteger noContextAttempts = new AtomicInteger(0);
        final HttpResponse recoveredWithoutContext = retryPolicy.attempt(new Callable<HttpResponse>() {
            @Override
            public HttpResponse call() {
                return noContextAttempts.incrementAndGet() == 1
                        ? new HttpResponse(503, "{}", Collections.singletonMap("Retry-After", Collections.singletonList("60")))
                        : new HttpResponse(200, "{}", null);
            }
        });
        assertEquals(200, recoveredWithoutContext.getStatusCode());
        assertEquals(2, noContextAttempts.get());

        // 3. In client SDK process (isBrokerHostProcess == false), baseline 1-retry executes even with context
        ServerThrottleManager.setBrokerHostProcess(false);
        final AtomicInteger clientProcessAttempts = new AtomicInteger(0);
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-client")) {
            final HttpResponse clientRecovered = retryPolicy.attempt(new Callable<HttpResponse>() {
                @Override
                public HttpResponse call() {
                    return clientProcessAttempts.incrementAndGet() == 1
                            ? new HttpResponse(503, "{}", null)
                            : new HttpResponse(200, "{}", null);
                }
            });
            assertEquals(200, clientRecovered.getStatusCode());
            assertEquals(2, clientProcessAttempts.get());
        }

        // 4. In Broker process with flight disabled (ENABLE_BROKER_SERVER_THROTTLING == false), baseline retry executes
        ServerThrottleManager.setBrokerHostProcess(true);
        mFlightsProvider.setFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "false");
        final AtomicInteger flightOffAttempts = new AtomicInteger(0);
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-off")) {
            final HttpResponse flightOffRecovered = retryPolicy.attempt(new Callable<HttpResponse>() {
                @Override
                public HttpResponse call() {
                    return flightOffAttempts.incrementAndGet() == 1
                            ? new HttpResponse(504, "{}", null)
                            : new HttpResponse(200, "{}", null);
                }
            });
            assertEquals(200, flightOffRecovered.getStatusCode());
            assertEquals(2, flightOffAttempts.get());
        }
    }

    @Test
    public void urlConnectionHttpClientDefaultInstance_suppresses503RetryWhenActiveAndRetriesWhenFlightOff() throws Exception {
        final URL tokenUrl = new URL(TOKEN_ENDPOINT_URL);
        final byte[] postBody = "grant_type=refresh_token".getBytes(StandardCharsets.UTF_8);

        // Active Broker TokenThrottleContext: 503 is NOT retried; second queued connection stays untouched
        final Map<String, List<String>> retryAfterHeaders =
                Collections.singletonMap("Retry-After", Collections.singletonList("45"));
        HttpUrlConnectionFactory.addMockedConnection(createMockPostFailureConnection(503, "{}", retryAfterHeaders));
        HttpUrlConnectionFactory.addMockedConnection(createMockPostSuccessConnection(MOCK_TOKEN_SUCCESS_JSON));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-uc-1")) {
            final HttpResponse suppressedRetryResponse = UrlConnectionHttpClient.getDefaultInstance().post(
                    tokenUrl,
                    Collections.<String, String>emptyMap(),
                    postBody
            );
            assertEquals(503, suppressedRetryResponse.getStatusCode());
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
        }

        // Flight OFF: UrlConnectionHttpClient.getDefaultInstance() performs its baseline retry and returns 200
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        mFlightsProvider.setFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "false");
        HttpUrlConnectionFactory.addMockedConnection(createMockPostFailureConnection(503, "{}", retryAfterHeaders));
        HttpUrlConnectionFactory.addMockedConnection(createMockPostSuccessConnection(MOCK_TOKEN_SUCCESS_JSON));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-uc-off")) {
            final HttpResponse retriedResponse = UrlConnectionHttpClient.getDefaultInstance().post(
                    tokenUrl,
                    Collections.<String, String>emptyMap(),
                    postBody
            );
            assertEquals(200, retriedResponse.getStatusCode());
            assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
        }
    }

    @Test
    public void microsoftStsOAuth2Strategy_skipsSecondPKeyAuthPostOn401WithRetryAfterAndRecordsThrottle() throws Exception {
        final MicrosoftStsOAuth2Strategy strategy = createMicrosoftStsStrategy();
        final MicrosoftStsTokenRequest tokenRequest = createTokenRequest();

        final Map<String, List<String>> throttledPKeyHeaders = new LinkedHashMap<>();
        throttledPKeyHeaders.put(CHALLENGE_REQUEST_HEADER, Collections.singletonList(PKEYAUTH_CHALLENGE_VALUE));
        throttledPKeyHeaders.put("Retry-After", Collections.singletonList("60"));

        HttpUrlConnectionFactory.addMockedConnection(
                createMockPostFailureConnection(
                        HttpURLConnection.HTTP_UNAUTHORIZED,
                        "{\"error\":\"invalid_client\",\"error_description\":\"Throttled challenge\"}",
                        throttledPKeyHeaders
                )
        );
        HttpUrlConnectionFactory.addMockedConnection(createMockPostSuccessConnection(MOCK_TOKEN_SUCCESS_JSON));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-pkey-401")) {
            final TokenResult result = strategy.requestToken(tokenRequest);
            assertFalse(result.getSuccess());
            // Second connection (PKeyAuth follow-up POST) was skipped!
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            // OAuth2Strategy.requestToken recorded the 401 + Retry-After throttle entry
            assertEquals(1, mManager.getActiveEntryCount());
            assertNotNull(scope.context().getLiveThrottleResult());
            assertEquals(401, scope.context().getLiveThrottleResult().getHttpStatusCode());
            assertEquals(60L, scope.context().getLiveThrottleResult().getRetryAfterSeconds());
        }

        // Subsequent pre-flight check suppresses equivalent silent request
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-pkey-next")) {
            final TokenResult suppressed = mManager.checkPreFlight(scope.context());
            assertNotNull(suppressed);
            assertTrue(suppressed.isLocallySuppressedThrottle());
            assertEquals(401, suppressed.getErrorResponse().getStatusCode());
        }
    }

    @Test
    public void microsoftStsOAuth2Strategy_preservesPKeyAuthChallengeWhenNoValidRetryAfterOrFlightDisabled() throws Exception {
        final MicrosoftStsOAuth2Strategy strategy = createMicrosoftStsStrategy();
        final MicrosoftStsTokenRequest tokenRequest = createTokenRequest();

        // 1. Plain 401 PKeyAuth challenge WITHOUT Retry-After -> executes follow-up POST and succeeds
        final Map<String, List<String>> plainPKeyHeaders =
                Collections.singletonMap(CHALLENGE_REQUEST_HEADER, Collections.singletonList(PKEYAUTH_CHALLENGE_VALUE));
        HttpUrlConnectionFactory.addMockedConnection(
                createMockPostFailureConnection(HttpURLConnection.HTTP_UNAUTHORIZED, "{}", plainPKeyHeaders)
        );
        HttpUrlConnectionFactory.addMockedConnection(createMockPostSuccessConnection(MOCK_TOKEN_SUCCESS_JSON));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-pkey-plain")) {
            final TokenResult plainResult = strategy.requestToken(tokenRequest);
            assertTrue(plainResult.getSuccess());
            assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            assertEquals(0, mManager.getActiveEntryCount());
        }

        // 2. 401 PKeyAuth challenge with Retry-After: 0 (non-positive) -> executes follow-up POST and succeeds
        final Map<String, List<String>> zeroRetryHeaders = new LinkedHashMap<>();
        zeroRetryHeaders.put(CHALLENGE_REQUEST_HEADER, Collections.singletonList(PKEYAUTH_CHALLENGE_VALUE));
        zeroRetryHeaders.put("Retry-After", Collections.singletonList("0"));
        HttpUrlConnectionFactory.addMockedConnection(
                createMockPostFailureConnection(HttpURLConnection.HTTP_UNAUTHORIZED, "{}", zeroRetryHeaders)
        );
        HttpUrlConnectionFactory.addMockedConnection(createMockPostSuccessConnection(MOCK_TOKEN_SUCCESS_JSON));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-pkey-zero")) {
            final TokenResult zeroResult = strategy.requestToken(tokenRequest);
            assertTrue(zeroResult.getSuccess());
            assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            assertEquals(0, mManager.getActiveEntryCount());
        }

        // 3. Flight OFF: 401 PKeyAuth challenge EVEN WITH Retry-After: 60 executes follow-up POST cleanly
        mFlightsProvider.setFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "false");
        final Map<String, List<String>> throttledPKeyHeaders = new LinkedHashMap<>();
        throttledPKeyHeaders.put(CHALLENGE_REQUEST_HEADER, Collections.singletonList(PKEYAUTH_CHALLENGE_VALUE));
        throttledPKeyHeaders.put("Retry-After", Collections.singletonList("60"));
        HttpUrlConnectionFactory.addMockedConnection(
                createMockPostFailureConnection(HttpURLConnection.HTTP_UNAUTHORIZED, "{}", throttledPKeyHeaders)
        );
        HttpUrlConnectionFactory.addMockedConnection(createMockPostSuccessConnection(MOCK_TOKEN_SUCCESS_JSON));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-pkey-off")) {
            final TokenResult flightOffResult = strategy.requestToken(tokenRequest);
            assertTrue(flightOffResult.getSuccess());
            assertEquals(0, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            assertEquals(0, mManager.getActiveEntryCount());
        }
    }

    @Test
    public void oauth2StrategyRequestToken_capturesEmpty429AndHtml503AndRespectsFlightOffAndDtoHelpers() throws Exception {
        final MicrosoftStsOAuth2Strategy strategy = createMicrosoftStsStrategy();
        final MicrosoftStsTokenRequest tokenRequest = createTokenRequest();

        // 1. Empty-body HTTP 429 with x-ms-clitelem -> normalized error, 60s Retry-After, cached record
        final Map<String, List<String>> clitelemOnly =
                Collections.singletonMap("x-ms-clitelem", Collections.singletonList("1,50058,1001,25.0,ring5"));
        HttpUrlConnectionFactory.addMockedConnection(createMockPostFailureConnection(429, "", clitelemOnly));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-429")) {
            final TokenResult result429 = strategy.requestToken(tokenRequest);
            assertFalse(result429.getSuccess());
            assertFalse(result429.isLocallySuppressedThrottle());
            assertEquals(
                    ServiceException.REQUEST_THROTTLED_AT_ESTS_GATEWAY,
                    result429.getErrorResponse().getError()
            );
            assertEquals(
                    Collections.singletonList("60"),
                    HeaderSerializationUtil.fromJson(
                            result429.getErrorResponse().getResponseHeadersJson()
                    ).get("Retry-After")
            );
            assertEquals(1, mManager.getActiveEntryCount());
        }

        // 2. HTML-body HTTP 503 -> no 1s retry (2nd queued connection untouched), normalized SERVICE_NOT_AVAILABLE, 120s Retry-After
        mManager.clearAll();
        HttpUrlConnectionFactory.addMockedConnection(
                createMockPostFailureConnection(503, "<html>503 Service Unavailable</html>", null)
        );
        HttpUrlConnectionFactory.addMockedConnection(createMockPostSuccessConnection(MOCK_TOKEN_SUCCESS_JSON));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-503")) {
            final TokenResult result503 = strategy.requestToken(tokenRequest);
            assertFalse(result503.getSuccess());
            assertEquals(1, HttpUrlConnectionFactory.getMockedConnectionCountInQueue());
            assertEquals(
                    ServiceException.SERVICE_NOT_AVAILABLE,
                    result503.getErrorResponse().getError()
            );
            assertEquals(
                    Collections.singletonList("120"),
                    HeaderSerializationUtil.fromJson(
                            result503.getErrorResponse().getResponseHeadersJson()
                    ).get("Retry-After")
            );
            assertEquals(1, mManager.getActiveEntryCount());
        }

        // 3. Custom ITokenResponseHandler returning null -> safely returns null without NPE
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        HttpUrlConnectionFactory.addMockedConnection(createMockPostFailureConnection(429, "", null));
        assertNull(strategy.requestToken(tokenRequest, new ITokenResponseHandler<TokenResult>() {
            @Override
            public TokenResult handleTokenResponse(@NonNull final HttpResponse response) {
                return null;
            }
        }));

        // 4. Flight OFF isolation on HTTP 429
        HttpUrlConnectionFactory.clearMockedConnectionQueue();
        mManager.clearAll();
        mFlightsProvider.setFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "false");
        HttpUrlConnectionFactory.addMockedConnection(createMockPostFailureConnection(429, "", null));

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(mKey, false, "corr-429-off")) {
            final TokenResult flightOff429 = strategy.requestToken(tokenRequest);
            assertFalse(flightOff429.getSuccess());
            assertNull(scope.context().getLiveThrottleResult());
            assertEquals(0, mManager.getActiveEntryCount());
        }

        // 5. Verify public DTO suppression/status mutators on ServiceException and TokenResult
        final ServiceException serviceException = new ServiceException("err", "msg", null);
        assertEquals(ServiceException.DEFAULT_STATUS_CODE, serviceException.getHttpStatusCode());
        assertFalse(serviceException.isLocallySuppressedThrottle());
        serviceException.setHttpStatusCode(429);
        serviceException.setLocallySuppressedThrottle(true);
        assertEquals(429, serviceException.getHttpStatusCode());
        assertTrue(serviceException.isLocallySuppressedThrottle());

        final TokenResult dtoResult = new TokenResult();
        assertFalse(dtoResult.isLocallySuppressedThrottle());
        dtoResult.setLocallySuppressedThrottle(true);
        assertTrue(dtoResult.isLocallySuppressedThrottle());
    }

    private static MicrosoftStsOAuth2Strategy createMicrosoftStsStrategy() throws Exception {
        final MicrosoftStsOAuth2Configuration config = new MicrosoftStsOAuth2Configuration();
        config.setAuthorityUrl(new URL(AUTHORITY_URL));
        final OAuth2StrategyParameters params = OAuth2StrategyParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .usingOpenIdConfiguration(false)
                .build();
        return new MicrosoftStsOAuth2Strategy(config, params);
    }

    private static MicrosoftStsTokenRequest createTokenRequest() {
        final MicrosoftStsTokenRequest request = new MicrosoftStsTokenRequest();
        request.setClientId("client-1");
        request.setGrantType(TokenRequest.GrantTypes.REFRESH_TOKEN);
        request.setScope("User.Read");
        request.setClientAppName("com.contoso.app");
        request.setClientAppVersion("1.0.0");
        return request;
    }

    private static HttpsURLConnection createMockPostFailureConnection(
            final int statusCode,
            final String body,
            final Map<String, List<String>> headers) throws IOException {
        final HttpsURLConnection connection = MockConnection.getMockedConnectionWithFailureResponse(statusCode, body);
        when(connection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(connection.getHeaderFields()).thenReturn(
                headers != null ? headers : Collections.<String, List<String>>emptyMap()
        );
        return connection;
    }

    private static HttpsURLConnection createMockPostSuccessConnection(final String jsonBody) throws IOException {
        final HttpsURLConnection connection = MockConnection.getCommonHttpUrlConnection();
        when(connection.getOutputStream()).thenReturn(new ByteArrayOutputStream());
        when(connection.getInputStream()).thenReturn(
                new ByteArrayInputStream(jsonBody.getBytes(StandardCharsets.UTF_8))
        );
        when(connection.getResponseCode()).thenReturn(HttpURLConnection.HTTP_OK);
        when(connection.getHeaderFields()).thenReturn(
                Collections.singletonMap("Content-Type", Collections.singletonList("application/json"))
        );
        return connection;
    }

    private static final class ToggleFlightsProvider extends MockFlightsProvider {
        private final Map<String, String> mValues = new ConcurrentHashMap<>();

        void setFlight(final String key, final String value) {
            mValues.put(key, value);
            addFlight(key, value);
        }

        @Override
        public boolean isFlightEnabled(@NonNull final IFlightConfig flightConfig) {
            final String override = mValues.get(flightConfig.getKey());
            if (override != null) {
                return Boolean.parseBoolean(override);
            }
            return ((Boolean) flightConfig.getDefaultValue()).booleanValue();
        }

        @Override
        public boolean getBooleanValue(@NonNull final IFlightConfig flightConfig) {
            return isFlightEnabled(flightConfig);
        }

        @Override
        public int getIntValue(@NonNull final IFlightConfig flightConfig) {
            final String override = mValues.get(flightConfig.getKey());
            if (override != null) {
                return Integer.parseInt(override);
            }
            return ((Integer) flightConfig.getDefaultValue()).intValue();
        }
    }

    private static final class FakeClock implements MonotonicClock {
        private final long mNanoTime;
        private final long mCurrentTimeMillis;

        FakeClock(final long nanoTime, final long currentTimeMillis) {
            mNanoTime = nanoTime;
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
