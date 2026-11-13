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
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.microsoft.identity.common.java.authorities.Authority;
import com.microsoft.identity.common.java.authscheme.BearerAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.authscheme.PopAuthenticationSchemeInternal;
import com.microsoft.identity.common.java.broker.IBrokerAccount;
import com.microsoft.identity.common.java.cache.BrokerOAuth2TokenCache;
import com.microsoft.identity.common.java.commands.parameters.BrokerSilentTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.RopcTokenCommandParameters;
import com.microsoft.identity.common.java.commands.parameters.TokenCommandParameters;
import com.microsoft.identity.common.java.dto.AccountRecord;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightConfig;
import com.microsoft.identity.common.java.flighting.MockFlightsManager;
import com.microsoft.identity.common.java.flighting.MockFlightsProvider;
import com.microsoft.identity.common.java.interfaces.IPlatformComponents;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.request.BrokerRequestType;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import io.opentelemetry.api.trace.Span;
import io.opentelemetry.context.Scope;
import lombok.NonNull;

/**
 * Deterministic unit tests for {@link ServerThrottleManager}, {@link TokenThrottleKey},
 * {@link LiveThrottleCapture}, and {@link TokenThrottleContext}.
 */
@RunWith(JUnit4.class)
public class ServerThrottleManagerTest {

    private static final long INITIAL_NANOS = 10_000_000_000L;
    private static final long INITIAL_EPOCH_MS = 1_794_571_200_000L;

    private AtomicFakeClock mClock;
    private ConfigurableFlightsProvider mFlightsProvider;
    private ServerThrottleManager mManager;

    @Before
    public void setUp() {
        DiagnosticContext.INSTANCE.clear();
        mClock = new AtomicFakeClock(INITIAL_NANOS, INITIAL_EPOCH_MS);
        mFlightsProvider = new ConfigurableFlightsProvider();
        mFlightsProvider.putFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "true");
        final MockFlightsManager flightsManager = new MockFlightsManager();
        flightsManager.setMockBrokerFlightsProvider(mFlightsProvider);
        CommonFlightsManager.INSTANCE.initializeCommonFlightsManager(flightsManager);

        ServerThrottleManager.setBrokerHostProcess(true);
        mManager = new ServerThrottleManager(mClock);
        ServerThrottleManager.setInstanceForTesting(mManager);
    }

    @After
    public void tearDown() {
        ServerThrottleManager.setBrokerHostProcess(false);
        mManager.clearAll();
        ServerThrottleManager.getInstance().clearAll();
        ServerThrottleManager.setInstanceForTesting(null);
        CommonFlightsManager.INSTANCE.resetFlightsManager();
        DiagnosticContext.INSTANCE.clear();
    }

    @Test
    public void commonFlightsAndPublicEnums_exposePinnedDefaultsAndValues() {
        assertEquals("EnableBrokerServerThrottling", CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey());
        assertEquals(Boolean.FALSE, CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getDefaultValue());
        assertEquals("BrokerThrottleDefault429Seconds", CommonFlight.BROKER_THROTTLE_DEFAULT_429_SECONDS.getKey());
        assertEquals(Integer.valueOf(60), CommonFlight.BROKER_THROTTLE_DEFAULT_429_SECONDS.getDefaultValue());
        assertEquals("BrokerThrottleDefault5xxSeconds", CommonFlight.BROKER_THROTTLE_DEFAULT_5XX_SECONDS.getKey());
        assertEquals(Integer.valueOf(120), CommonFlight.BROKER_THROTTLE_DEFAULT_5XX_SECONDS.getDefaultValue());
        assertEquals("BrokerThrottleMaxRetryAfterSeconds", CommonFlight.BROKER_THROTTLE_MAX_RETRY_AFTER_SECONDS.getKey());
        assertEquals(Integer.valueOf(3600), CommonFlight.BROKER_THROTTLE_MAX_RETRY_AFTER_SECONDS.getDefaultValue());
        assertEquals("BrokerThrottleCacheMaxEntries", CommonFlight.BROKER_THROTTLE_CACHE_MAX_ENTRIES.getKey());
        assertEquals(Integer.valueOf(512), CommonFlight.BROKER_THROTTLE_CACHE_MAX_ENTRIES.getDefaultValue());
        assertEquals("EnableEstsThrottleLibCapability", CommonFlight.ENABLE_ESTS_THROTTLE_LIB_CAPABILITY.getKey());
        assertEquals(Boolean.FALSE, CommonFlight.ENABLE_ESTS_THROTTLE_LIB_CAPABILITY.getDefaultValue());

        assertEquals("not_throttled", ThrottleOutcome.NOT_THROTTLED.getTelemetryValue());
        assertEquals("fresh_server_throttle", ThrottleOutcome.FRESH_SERVER_THROTTLE.getTelemetryValue());
        assertEquals("locally_suppressed", ThrottleOutcome.LOCALLY_SUPPRESSED.getTelemetryValue());

        assertTrue(MonotonicClock.SYSTEM.currentTimeMillis() > 0L);
        final long n1 = MonotonicClock.SYSTEM.nanoTime();
        final long n2 = MonotonicClock.SYSTEM.nanoTime();
        assertTrue(n2 - n1 >= 0L);
        assertSame(mManager, ServerThrottleManager.getInstance());
    }

    @Test
    public void checkPreFlight_monotonicExpiryBoundary_expiresAtMinusOneVsExactExpiry() {
        final TokenThrottleKey key = TokenThrottleKey.forAtOrRt(
                "https://login.microsoftonline.com/common",
                "client-1",
                Arrays.asList("User.Read"),
                null,
                "uid.utid",
                "Bearer",
                null,
                BrokerRequestType.REGULAR.name(),
                null
        );

        final Map<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Retry-After", Collections.singletonList("60"));
        headers.put("x-ms-clitelem", Collections.singletonList("1,50058,1001,120.5,ring3"));
        final HttpResponse response = new HttpResponse(429, "", headers);
        final MicrosoftTokenErrorResponse parsedError = new MicrosoftTokenErrorResponse();
        parsedError.setError("429");
        parsedError.setSubError("bad_token");
        parsedError.setErrorDescription("Too many token requests");

        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-origin")) {
            assertTrue(mManager.onHttpResponse(scope.context(), response, parsedError, null));
        }

        final long expiresAtNanos = INITIAL_NANOS + TimeUnit.SECONDS.toNanos(60);

        // At t0 + 1ns (59.999999999s remaining) -> ceiling Retry-After is 60s
        mClock.setNanoTime(INITIAL_NANOS + 1L);
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-1")) {
            final TokenResult suppressed = mManager.checkPreFlight(scope.context());
            assertNotNull(suppressed);
            assertTrue(suppressed.isLocallySuppressedThrottle());
            assertEquals(
                    Collections.singletonList("60"),
                    HeaderSerializationUtil.fromJson(
                            suppressed.getErrorResponse().getResponseHeadersJson()
                    ).get("Retry-After")
            );
        }

        // At expiresAtNanos - 1ns (1ns remaining) -> suppressed with ceiling Retry-After == "1" and caller corrId
        mClock.setNanoTime(expiresAtNanos - 1L);
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-caller-2")) {
            final TokenResult suppressed = mManager.checkPreFlight(scope.context());
            assertNotNull(suppressed);
            assertFalse(suppressed.getSuccess());
            assertTrue(suppressed.isLocallySuppressedThrottle());
            assertEquals(429, suppressed.getErrorResponse().getStatusCode());
            assertEquals(ServiceException.REQUEST_THROTTLED_AT_ESTS_GATEWAY, suppressed.getErrorResponse().getError());
            assertEquals("bad_token", suppressed.getErrorResponse().getSubError());
            assertNull(suppressed.getErrorResponse().getResponseBody());
            assertEquals(
                    Collections.singletonList("1"),
                    HeaderSerializationUtil.fromJson(
                            suppressed.getErrorResponse().getResponseHeadersJson()
                    ).get("Retry-After")
            );
            assertTrue(suppressed.getErrorResponse().getErrorDescription().contains("retryAfterSeconds=1"));
            assertTrue(suppressed.getErrorResponse().getErrorDescription().contains("correlationId=corr-caller-2"));
            assertFalse(suppressed.getErrorResponse().getErrorDescription().contains("corr-origin"));
            assertNotNull(suppressed.getCliTelemInfo());
            assertEquals("ring3", suppressed.getCliTelemInfo().getSpeRing());
            assertEquals("50058", suppressed.getCliTelemInfo().getServerErrorCode());
        }

        // Interactive flow bypasses pre-flight suppression even before expiry
        try (TokenThrottleContext.TokenThrottleScope interactiveScope =
                     TokenThrottleContext.open(key, true, "corr-interactive")) {
            assertNull(mManager.checkPreFlight(interactiveScope.context()));
        }
        assertEquals(1, mManager.getActiveEntryCount());

        // At exact expiresAtNanos -> allowed and evicted
        mClock.setNanoTime(expiresAtNanos);
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-after")) {
            assertNull(mManager.checkPreFlight(scope.context()));
        }
        assertEquals(0, mManager.getActiveEntryCount());
    }

    @Test
    public void tokenThrottleKey_allPublicOverloads_canonicalizeAndIsolateDimensions() throws Exception {
        final Authority authorityA = mock(Authority.class);
        when(authorityA.getAuthorityURL()).thenReturn(new URL("https://login.microsoftonline.com/tenant-a/"));
        final Authority authorityB = mock(Authority.class);
        when(authorityB.getAuthorityURL()).thenReturn(new URL("https://login.microsoftonline.com/tenant-b"));

        final IBrokerAccount brokerAccount = mock(IBrokerAccount.class);
        when(brokerAccount.getUsername()).thenReturn("Alice@Contoso.com");

        final AccountRecord accountRecord = new AccountRecord();
        accountRecord.setHomeAccountId("uid-1.utid-1");
        accountRecord.setLocalAccountId("oid-1");
        accountRecord.setUsername("alice@contoso.com");

        final BrokerSilentTokenCommandParameters baseBrokerParams = BrokerSilentTokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .oAuth2TokenCache(mock(BrokerOAuth2TokenCache.class))
                .authority(authorityA)
                .clientId("Client-App-1")
                .scopes(new HashSet<>(Arrays.asList("Mail.Read", "User.Read")))
                .claimsRequestJson("{\"id_token\":null,\"access_token\":{\"acrs\":{\"essential\":true}}}")
                .authenticationScheme(new BearerAuthenticationSchemeInternal())
                .homeAccountId("UID-1.UTID-1")
                .localAccountId("oid-1")
                .brokerAccount(brokerAccount)
                .account(accountRecord)
                .requestType(BrokerRequestType.REGULAR)
                .callerUid(1001)
                .callerPackageName("com.contoso.app")
                .build();

        final TokenThrottleKey key1 = TokenThrottleKey.forAtOrRt(baseBrokerParams, null, null, null);
        assertEquals(ThrottleScopeType.AT_OR_RT, key1.getScopeType());
        assertEquals(64, key1.getSha256Hex().length());
        assertEquals(key1.getSha256Hex().substring(0, 8), key1.getKeyPrefix());
        assertFalse(key1.toString().contains("alice"));
        assertFalse(key1.toString().contains("uid-1"));

        // Reordered scopes, reordered JSON claims keys, and trailing slash/case in authority produce identical hash
        final BrokerSilentTokenCommandParameters equivalentParams = baseBrokerParams.toBuilder()
                .scopes(new HashSet<>(Arrays.asList("user.read  mail.read")))
                .claimsRequestJson("{\"access_token\":{\"acrs\":{\"essential\":true}},\"id_token\":null}")
                .build();
        final TokenThrottleKey keyEquivalent = TokenThrottleKey.forAtOrRt(
                equivalentParams,
                "HTTPS://Login.MicrosoftOnline.com/TENANT-A///",
                null,
                null
        );
        assertEquals(key1, keyEquivalent);
        assertEquals(key1.hashCode(), keyEquivalent.hashCode());

        // Nested App Auth (NAA) brkClientId:childClientId isolates from parent and sibling nested apps
        final TokenThrottleKey naaChildA = TokenThrottleKey.forAtOrRt(
                baseBrokerParams.toBuilder().childClientId("child-a").childRedirectUri("brk://a").build(),
                null, null, null
        );
        final TokenThrottleKey naaChildB = TokenThrottleKey.forAtOrRt(
                baseBrokerParams.toBuilder().childClientId("child-b").childRedirectUri("brk://b").build(),
                null, null, null
        );
        assertNotEquals(key1.getSha256Hex(), naaChildA.getSha256Hex());
        assertNotEquals(naaChildA.getSha256Hex(), naaChildB.getSha256Hex());

        // Distinct tenant, distinct scheme (PoP), distinct claims, distinct mamEnrollmentId, distinct requestType
        final PopAuthenticationSchemeInternal popScheme = mock(PopAuthenticationSchemeInternal.class);
        when(popScheme.getName()).thenReturn("PoP");
        assertNotEquals(key1, TokenThrottleKey.forAtOrRt(
                baseBrokerParams.toBuilder().authority(authorityB).build(), null, null, null));
        assertNotEquals(key1, TokenThrottleKey.forAtOrRt(
                baseBrokerParams.toBuilder().authenticationScheme(popScheme).build(), null, null, null));
        assertNotEquals(key1, TokenThrottleKey.forAtOrRt(
                baseBrokerParams.toBuilder().claimsRequestJson("{\"access_token\":{\"polids\":{\"essential\":true}}}").build(),
                null, null, null));
        assertNotEquals(key1, TokenThrottleKey.forAtOrRt(
                baseBrokerParams.toBuilder().mamEnrollmentId("mam-1").build(), null, null, null));
        assertNotEquals(key1, TokenThrottleKey.forAtOrRt(
                baseBrokerParams.toBuilder().requestType(BrokerRequestType.BROKER_RT_REQUEST).build(),
                null, null, null));

        // RopcTokenCommandParameters.getUsername() fallback & user account tag inclusion
        final RopcTokenCommandParameters ropcParams = RopcTokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .authority(authorityA)
                .clientId("ropc-client")
                .scopes(Collections.singleton("User.Read"))
                .username("ropc.user@contoso.com")
                .password("placeholder-not-cached")
                .build();
        final TokenThrottleKey ropcKey = TokenThrottleKey.forAtOrRt(ropcParams, null, null, null);
        assertFalse(ropcKey.getAccountTagHashes().isEmpty());
        final RopcTokenCommandParameters otherRopcParams = ropcParams.toBuilder()
                .username("other.user@contoso.com")
                .build();
        assertNotEquals(ropcKey, TokenThrottleKey.forAtOrRt(otherRopcParams, null, null, null));

        // DCF fallbackAccountOrSessionId isolates device codes WITHOUT polluting user accountTagHashes
        final TokenCommandParameters dcfParams = TokenCommandParameters.builder()
                .platformComponents(mock(IPlatformComponents.class))
                .authority(authorityA)
                .clientId("dcf-client")
                .scopes(Collections.singleton("User.Read"))
                .build();
        final TokenThrottleKey dcfKey1 = TokenThrottleKey.forAtOrRt(dcfParams, null, "dcf:code-111", null);
        final TokenThrottleKey dcfKey2 = TokenThrottleKey.forAtOrRt(dcfParams, null, "dcf:code-222", null);
        assertNotEquals(dcfKey1, dcfKey2);
        assertTrue("DCF session fallback must not pollute user accountTagHashes", dcfKey1.getAccountTagHashes().isEmpty());

        final TokenThrottleKey dcfWithExplicitExtra = TokenThrottleKey.forAtOrRt(
                dcfParams, null, "dcf:code-111", Collections.singletonList("user@contoso.com")
        );
        assertEquals(1, dcfWithExplicitExtra.getAccountTagHashes().size());

        // BROKER_PRT, DEVICE_TOKEN (empty accountTagHashes + clientId/resource/scope/deviceId isolation), FOCI_PROBE
        final TokenThrottleKey prtKey = TokenThrottleKey.forBrokerPrt(
                "https://login.microsoftonline.com/common",
                "uid-1.utid-1",
                "refresh_token",
                Arrays.asList("oid-1", "alice@contoso.com")
        );
        assertEquals(ThrottleScopeType.BROKER_PRT, prtKey.getScopeType());
        assertEquals(3, prtKey.getAccountTagHashes().size());

        final TokenThrottleKey devKeyAppA = TokenThrottleKey.forDeviceToken(
                "https://login.microsoftonline.com/tenant-a",
                "app-a",
                "https://graph.microsoft.com/",
                "Device.Read",
                "device-id-1"
        );
        final TokenThrottleKey devKeyAppB = TokenThrottleKey.forDeviceToken(
                "https://login.microsoftonline.com/tenant-a",
                "app-b",
                "https://graph.microsoft.com",
                "Device.Read",
                "device-id-1"
        );
        final TokenThrottleKey devKeyResourceB = TokenThrottleKey.forDeviceToken(
                "https://login.microsoftonline.com/tenant-a",
                "app-a",
                "https://management.azure.com",
                "Device.Read",
                "device-id-1"
        );
        assertEquals(ThrottleScopeType.DEVICE_TOKEN, devKeyAppA.getScopeType());
        assertTrue("DEVICE_TOKEN must have empty user accountTagHashes", devKeyAppA.getAccountTagHashes().isEmpty());
        assertNotEquals(devKeyAppA, devKeyAppB);
        assertNotEquals(devKeyAppA, devKeyResourceB);

        final TokenThrottleKey fociClientA = TokenThrottleKey.forFociProbe(
                "https://login.microsoftonline.com/common",
                "foci-client-a",
                "User.Read openid",
                "uid-1.utid-1",
                Collections.singletonList("alice@contoso.com")
        );
        final TokenThrottleKey fociClientB = TokenThrottleKey.forFociProbe(
                "https://login.microsoftonline.com/common",
                "foci-client-b",
                "openid user.read",
                "uid-1.utid-1",
                Collections.singletonList("alice@contoso.com")
        );
        assertEquals(ThrottleScopeType.FOCI_PROBE, fociClientA.getScopeType());
        assertEquals(2, fociClientA.getAccountTagHashes().size());
        assertNotEquals(fociClientA, fociClientB);
    }

    @Test
    public void liveThrottleCapture_toTokenResultAndToServiceException_preserveFreshServerMetadata() {
        final TokenThrottleKey key = TokenThrottleKey.forBrokerPrt(
                "https://login.microsoftonline.com/common",
                "uid.utid",
                "srv_challenge",
                null
        );
        final Map<String, List<String>> rawHeaders = new LinkedHashMap<>();
        rawHeaders.put("retry-after", Collections.singletonList("45"));
        rawHeaders.put("x-ms-request-id", Collections.singletonList("req-123"));
        rawHeaders.put("x-ms-clitelem", Collections.singletonList("1,50058,1002,88.0,ring4"));

        final HttpResponse response = new HttpResponse(400, "{\"error\":\"invalid_grant\"}", rawHeaders);
        final MicrosoftTokenErrorResponse parsedError = new MicrosoftTokenErrorResponse();
        parsedError.setError("invalid_grant");
        parsedError.setSubError("bad_token");
        parsedError.setErrorDescription("Token refresh temporarily throttled");

        final LiveThrottleCapture capture;
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-live")) {
            assertTrue(mManager.onHttpResponse(scope.context(), response, parsedError, null));
            capture = scope.context().getLiveThrottleResult();
        }

        assertNotNull(capture);
        assertEquals(400, capture.getHttpStatusCode());
        assertEquals("invalid_grant", capture.getErrorCode());
        assertEquals("bad_token", capture.getSubErrorCode());
        assertEquals(45L, capture.getRetryAfterSeconds());
        assertEquals("corr-live", capture.getCorrelationId());
        assertEquals(Collections.singletonList("45"), capture.getResponseHeaders().get("Retry-After"));
        assertEquals(Collections.singletonList("req-123"), capture.getResponseHeaders().get("x-ms-request-id"));

        final TokenResult liveResult = capture.toTokenResult();
        assertFalse(liveResult.getSuccess());
        assertFalse(liveResult.isLocallySuppressedThrottle());
        assertNull(liveResult.getErrorResponse().getResponseBody());
        assertEquals(400, liveResult.getErrorResponse().getStatusCode());
        assertEquals("invalid_grant", liveResult.getErrorResponse().getError());
        assertEquals("bad_token", liveResult.getErrorResponse().getSubError());
        assertTrue(liveResult.getErrorResponse().getErrorDescription().contains("retryAfterSeconds=45"));
        assertTrue(liveResult.getErrorResponse().getErrorDescription().contains("correlationId=corr-live"));
        assertEquals("ring4", liveResult.getCliTelemInfo().getSpeRing());

        final ServiceException defaultEx = capture.toServiceException(null);
        assertFalse(defaultEx.isLocallySuppressedThrottle());
        assertEquals(400, defaultEx.getHttpStatusCode());
        assertEquals("invalid_grant", defaultEx.getErrorCode());
        assertEquals("bad_token", defaultEx.getSubErrorCode());
        assertEquals("corr-live", defaultEx.getCorrelationId());
        assertEquals(Collections.singletonList("45"), defaultEx.getHttpResponseHeaders().get("Retry-After"));
        assertEquals("ring4", defaultEx.getSpeRing());
        assertEquals("50058", defaultEx.getCliTelemErrorCode());
        assertEquals("1002", defaultEx.getCliTelemSubErrorCode());
        assertEquals("88.0", defaultEx.getRefreshTokenAge());

        final ServiceException overrideEx = capture.toServiceException("corr-override");
        assertEquals("corr-override", overrideEx.getCorrelationId());
        assertTrue(overrideEx.getMessage().contains("correlationId=corr-override"));
    }

    @Test
    public void tokenThrottleContext_nestedScopesAndIdempotentClose() throws Exception {
        final TokenThrottleKey outerKey = TokenThrottleKey.forBrokerPrt(
                "https://login.microsoftonline.com/common", "acct-outer", "refresh_token", null);
        final TokenThrottleKey innerKey = TokenThrottleKey.forFociProbe(
                "https://login.microsoftonline.com/common", "client-inner", "openid", "acct-outer", null);

        assertNull(TokenThrottleContext.current());
        try (TokenThrottleContext.TokenThrottleScope outer = TokenThrottleContext.open(outerKey, false, "corr-outer")) {
            assertSame(outer.context(), TokenThrottleContext.current());
            assertEquals("corr-outer", TokenThrottleContext.current().getCorrelationId());

            final Callable<TokenThrottleKey> crossPackageCallable = new Callable<TokenThrottleKey>() {
                @Override
                public TokenThrottleKey call() {
                    try (TokenThrottleContext.TokenThrottleScope inner =
                                 TokenThrottleContext.open(innerKey, true, "corr-inner")) {
                        assertSame(inner.context(), TokenThrottleContext.current());
                        assertTrue(TokenThrottleContext.current().isInteractiveOrigin());
                        return TokenThrottleContext.current().getThrottleKey();
                    }
                }
            };
            assertEquals(innerKey, crossPackageCallable.call());

            // Outer context restored after inner scope closes
            assertSame(outer.context(), TokenThrottleContext.current());
            outer.close();
            // Double close is safe and idempotent
            outer.close();
            assertNull(TokenThrottleContext.current());
        }
        assertNull(TokenThrottleContext.current());
    }

    @Test
    public void resolveDefaultErrorCode_and_isThrottledServiceException() {
        assertEquals(
                ServiceException.REQUEST_THROTTLED_AT_ESTS_GATEWAY,
                ServerThrottleManager.resolveDefaultErrorCode(429, null)
        );
        assertEquals(
                ServiceException.REQUEST_THROTTLED_AT_ESTS_GATEWAY,
                ServerThrottleManager.resolveDefaultErrorCode(429, "")
        );
        assertEquals(
                ServiceException.REQUEST_THROTTLED_AT_ESTS_GATEWAY,
                ServerThrottleManager.resolveDefaultErrorCode(429, " 429 ")
        );
        assertEquals(
                ServiceException.SERVICE_NOT_AVAILABLE,
                ServerThrottleManager.resolveDefaultErrorCode(503, null)
        );
        assertEquals(
                ServiceException.SERVICE_NOT_AVAILABLE,
                ServerThrottleManager.resolveDefaultErrorCode(503, "503")
        );
        assertEquals(
                "temporarily_unavailable",
                ServerThrottleManager.resolveDefaultErrorCode(503, "temporarily_unavailable")
        );
        // Non-429/5xx preserves numeric fallback "400" (MicrosoftStsTokenResponseHandlerTest invariant)
        assertEquals("400", ServerThrottleManager.resolveDefaultErrorCode(400, "400"));
        assertEquals("invalid_grant", ServerThrottleManager.resolveDefaultErrorCode(400, "invalid_grant"));
        assertEquals(ServiceException.UNKNOWN_ERROR, ServerThrottleManager.resolveDefaultErrorCode(400, null));

        assertFalse(ServerThrottleManager.isThrottledServiceException(null));
        assertFalse(ServerThrottleManager.isThrottledServiceException(new ClientException("io_error")));

        final ServiceException plain400 = new ServiceException("invalid_grant", "bad grant", 400, null);
        assertFalse(ServerThrottleManager.isThrottledServiceException(plain400));

        final ServiceException suppressed400 = new ServiceException("invalid_grant", "suppressed", 400, null);
        suppressed400.setLocallySuppressedThrottle(true);
        assertTrue(ServerThrottleManager.isThrottledServiceException(suppressed400));

        final ServiceException status429 = new ServiceException("unknown", "throttled", 429, null);
        assertTrue(ServerThrottleManager.isThrottledServiceException(status429));

        final ServiceException status503 = new ServiceException("unknown", "unavailable", 503, null);
        assertTrue(ServerThrottleManager.isThrottledServiceException(status503));

        final ServiceException gatewayCode = new ServiceException(
                ServiceException.REQUEST_THROTTLED_AT_ESTS_GATEWAY, "gateway", 0, null);
        assertTrue(ServerThrottleManager.isThrottledServiceException(gatewayCode));

        final ServiceException retryAfter400 = new ServiceException("invalid_grant", "backoff", 400, null);
        final HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("Retry-After", Collections.singletonList("30"));
        retryAfter400.setHttpResponseHeaders(headers);
        assertTrue(ServerThrottleManager.isThrottledServiceException(retryAfter400));

        // Outside Broker host process (isBrokerHostProcess == false), isThrottledServiceException MUST return false
        ServerThrottleManager.setBrokerHostProcess(false);
        assertFalse(ServerThrottleManager.isThrottledServiceException(suppressed400));
        assertFalse(ServerThrottleManager.isThrottledServiceException(status429));
        assertFalse(ServerThrottleManager.isThrottledServiceException(status503));
        assertFalse(ServerThrottleManager.isThrottledServiceException(gatewayCode));
        assertFalse(ServerThrottleManager.isThrottledServiceException(retryAfter400));

        // When ECS kill-switch disables ENABLE_BROKER_SERVER_THROTTLING, isThrottledServiceException MUST return false
        ServerThrottleManager.setBrokerHostProcess(true);
        mFlightsProvider.putFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "false");
        assertFalse(ServerThrottleManager.isThrottledServiceException(suppressed400));
        assertFalse(ServerThrottleManager.isThrottledServiceException(status429));
        assertFalse(ServerThrottleManager.isThrottledServiceException(status503));
        assertFalse(ServerThrottleManager.isThrottledServiceException(gatewayCode));
        assertFalse(ServerThrottleManager.isThrottledServiceException(retryAfter400));

        // Re-enabling flight restores Broker throttle classification
        mFlightsProvider.putFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "true");
        assertTrue(ServerThrottleManager.isThrottledServiceException(status429));
    }

    @Test
    public void boundedCapacity_evictsExpiredEntriesFirstThenLeastRecentlyUsed() {
        final ServerThrottleManager smallManager = new ServerThrottleManager(mClock, 4);
        final TokenThrottleKey k1 = keyForIndex(1);
        final TokenThrottleKey k2 = keyForIndex(2);
        final TokenThrottleKey k3 = keyForIndex(3);
        final TokenThrottleKey k4 = keyForIndex(4);
        final TokenThrottleKey k5 = keyForIndex(5);
        final TokenThrottleKey k6 = keyForIndex(6);

        // Insert k1 with short 10s TTL, and k2..k4 with 120s TTL at distinct timestamps
        record429(smallManager, k1, "10");
        mClock.addMillis(10L);
        record429(smallManager, k2, "120");
        mClock.addMillis(10L);
        record429(smallManager, k3, "120");
        mClock.addMillis(10L);
        record429(smallManager, k4, "120");
        assertEquals(4, smallManager.getActiveEntryCount());

        // Advance 15s -> k1 is expired; inserting k5 evicts expired k1 without evicting live k2..k4
        mClock.addSeconds(15L);
        record429(smallManager, k5, "120");
        assertEquals(4, smallManager.getActiveEntryCount());
        assertAllowed(smallManager, k1);
        assertSuppressed(smallManager, k2);
        assertSuppressed(smallManager, k4);
        assertSuppressed(smallManager, k5);

        // Note: checking k2, k4, k5 above touched their lastAccessedNanos, leaving k3 as the oldest LRU!
        mClock.addMillis(10L);
        record429(smallManager, k6, "120");
        assertEquals(4, smallManager.getActiveEntryCount());
        assertAllowed(smallManager, k3);
        assertSuppressed(smallManager, k2);
        assertSuppressed(smallManager, k4);
        assertSuppressed(smallManager, k5);
        assertSuppressed(smallManager, k6);

        // Populate 80 entries under maxEntries=80, then dynamically lower BROKER_THROTTLE_CACHE_MAX_ENTRIES
        // via ECS and update an ALREADY-CACHED key -> must still evict down to the new cap (64)!
        mFlightsProvider.putFlight(CommonFlight.BROKER_THROTTLE_CACHE_MAX_ENTRIES.getKey(), "80");
        for (int i = 0; i < 80; i++) {
            mClock.addMillis(1L);
            record429(mManager, keyForIndex(100 + i), "120");
        }
        assertEquals(80, mManager.getActiveEntryCount());

        // Flight clamping below minimum 64 clamps to 64; updating existing keyForIndex(179) trims 16 oldest LRU entries
        mFlightsProvider.putFlight(CommonFlight.BROKER_THROTTLE_CACHE_MAX_ENTRIES.getKey(), "10");
        mClock.addMillis(1L);
        record429(mManager, keyForIndex(179), "120");
        assertEquals(64, mManager.getActiveEntryCount());
        assertAllowed(mManager, keyForIndex(100));
        assertAllowed(mManager, keyForIndex(115));
        assertSuppressed(mManager, keyForIndex(116));
        assertSuppressed(mManager, keyForIndex(179));
    }

    @Test
    public void zeroSecretAndRawBodyPersistence_sanitizesDescriptionsAndDropsRawBodies() {
        final TokenThrottleKey key = keyForIndex(42);
        final String sensitiveRawBody = "{\"refresh_token\":\"rt-secret-xyz\",\"access_token\":\"eyJhbGciOiJSUzI1NiJ9.payload.sig\"}";

        final String[] unsafeDescriptions = new String[]{
                "Bearer eyJhbGciOiJSUzI1NiJ9.secret",
                "Invalid refresh_token supplied",
                "User alice@contoso.com failed password check",
                "<html><body>503 Gateway Error</body></html>"
        };
        for (final String unsafe : unsafeDescriptions) {
            final MicrosoftTokenErrorResponse parsedError = new MicrosoftTokenErrorResponse();
            parsedError.setError("invalid_grant");
            parsedError.setErrorDescription(unsafe);
            final HttpResponse response = new HttpResponse(
                    429,
                    sensitiveRawBody,
                    Collections.singletonMap("Retry-After", Collections.singletonList("30"))
            );
            try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-sec")) {
                assertTrue(mManager.onHttpResponse(scope.context(), response, parsedError, null));
                final TokenResult live = scope.context().getLiveThrottleResult().toTokenResult();
                assertNull(live.getErrorResponse().getResponseBody());
                assertSanitized(live.getErrorResponse().getErrorDescription());
            }
            try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-sec-2")) {
                final TokenResult suppressed = mManager.checkPreFlight(scope.context());
                assertNotNull(suppressed);
                assertNull(suppressed.getErrorResponse().getResponseBody());
                assertSanitized(suppressed.getErrorResponse().getErrorDescription());
            }
        }
    }

    @Test
    public void evictAccountIdentifiers_vs_evictBrokerPrtForAccountIdentifiers() {
        final List<String> aliceIds = Arrays.asList("uid-alice.utid", "oid-alice", "alice@contoso.com");
        final TokenThrottleKey aliceAt = TokenThrottleKey.forAtOrRt(
                "https://login.microsoftonline.com/common", "client-1", Collections.singleton("User.Read"),
                null, "uid-alice.utid", "Bearer", null, "REGULAR", aliceIds);
        final TokenThrottleKey alicePrt = TokenThrottleKey.forBrokerPrt(
                "https://login.microsoftonline.com/common", "uid-alice.utid", "refresh_token", aliceIds);
        final TokenThrottleKey aliceFoci = TokenThrottleKey.forFociProbe(
                "https://login.microsoftonline.com/common", "foci-1", "openid", "uid-alice.utid", aliceIds);
        final TokenThrottleKey sharedDeviceToken = TokenThrottleKey.forDeviceToken(
                "https://login.microsoftonline.com/common", "client-1", "https://graph.microsoft.com",
                "Device.Read", "shared-device-id-99");
        final TokenThrottleKey bobAt = TokenThrottleKey.forAtOrRt(
                "https://login.microsoftonline.com/common", "client-1", Collections.singleton("User.Read"),
                null, "uid-bob.utid", "Bearer", null, "REGULAR", Collections.singletonList("bob@contoso.com"));

        record429(mManager, aliceAt, "120");
        record429(mManager, alicePrt, "120");
        record429(mManager, aliceFoci, "120");
        record429(mManager, sharedDeviceToken, "120");
        record429(mManager, bobAt, "120");
        assertEquals(5, mManager.getActiveEntryCount());

        // PRT save evicts ONLY Alice's BROKER_PRT entry
        assertEquals(1, mManager.evictBrokerPrtForAccountIdentifiers("ALICE@CONTOSO.COM"));
        assertAllowed(mManager, alicePrt);
        assertSuppressed(mManager, aliceAt);
        assertSuppressed(mManager, aliceFoci);
        assertSuppressed(mManager, sharedDeviceToken);
        assertSuppressed(mManager, bobAt);

        // Re-record Alice's PRT, then run device-wide account eviction -> evicts AT_OR_RT + BROKER_PRT + FOCI_PROBE
        // while preserving DEVICE_TOKEN and Bob's AT_OR_RT!
        record429(mManager, alicePrt, "120");
        assertEquals(3, mManager.evictAccountIdentifiers("OID-ALICE"));
        assertAllowed(mManager, aliceAt);
        assertAllowed(mManager, alicePrt);
        assertAllowed(mManager, aliceFoci);
        assertSuppressed(mManager, sharedDeviceToken);
        assertSuppressed(mManager, bobAt);
        assertEquals(2, mManager.getActiveEntryCount());
    }

    @Test
    public void concurrencyStress_32ThreadsCyclicBarrier_threadSafeAndBounded() throws Exception {
        final ServerThrottleManager concurrentManager = new ServerThrottleManager(mClock, 16);
        final int threadCount = 32;
        final CyclicBarrier barrier = new CyclicBarrier(threadCount);
        final ExecutorService pool = Executors.newFixedThreadPool(threadCount);
        final List<Future<Void>> futures = new ArrayList<>(threadCount);

        try {
            for (int t = 0; t < threadCount; t++) {
                final int workerId = t;
                futures.add(pool.submit(new Callable<Void>() {
                    @Override
                    public Void call() throws Exception {
                        barrier.await(5, TimeUnit.SECONDS);
                        for (int iter = 0; iter < 40; iter++) {
                            mClock.addMillis(1L);
                            final int slot = (workerId + iter) % 24;
                            final String userUpn = "user" + (slot % 6) + "@contoso.com";
                            final TokenThrottleKey key = (slot % 3 == 0)
                                    ? TokenThrottleKey.forBrokerPrt(
                                            "https://login.microsoftonline.com/common",
                                            userUpn, "refresh_token", null)
                                    : TokenThrottleKey.forAtOrRt(
                                            "https://login.microsoftonline.com/common",
                                            "client-" + slot, Collections.singleton("User.Read"),
                                            null, userUpn, "Bearer", null, "REGULAR", null);

                            try (TokenThrottleContext.TokenThrottleScope scope =
                                         TokenThrottleContext.open(key, false, "corr-" + workerId)) {
                                if (iter % 4 == 0) {
                                    final HttpResponse r = new HttpResponse(
                                            429, "", Collections.singletonMap("Retry-After", Collections.singletonList("60")));
                                    concurrentManager.onHttpResponse(scope.context(), r, null, null);
                                } else if (iter % 7 == 0) {
                                    concurrentManager.evictBrokerPrtForAccountIdentifiers(userUpn);
                                } else if (iter % 13 == 0) {
                                    concurrentManager.evictAccountIdentifiers(userUpn);
                                } else {
                                    concurrentManager.checkPreFlight(scope.context());
                                }
                            }
                        }
                        return null;
                    }
                }));
            }
            for (final Future<Void> f : futures) {
                f.get(10, TimeUnit.SECONDS);
            }
            assertTrue(concurrentManager.getActiveEntryCount() <= 16);
        } finally {
            pool.shutdownNow();
        }
    }

    @Test
    public void flightAndBrokerHostKillSwitch_disablesSuppressionAndClearsStateImmediately() {
        final TokenThrottleKey key = keyForIndex(7);
        final HttpResponse r429 = new HttpResponse(
                429, "", Collections.singletonMap("Retry-After", Collections.singletonList("60")));

        record429(mManager, key, "60");
        assertEquals(1, mManager.getActiveEntryCount());
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "c1")) {
            assertTrue(ServerThrottleManager.shouldSuppressRetry(r429));
        }

        // Non-Broker process -> inactive even if flight is true; clears map on access
        ServerThrottleManager.setBrokerHostProcess(false);
        assertFalse(ServerThrottleManager.isBrokerHostProcess());
        assertFalse(ServerThrottleManager.isServerThrottlingActive());
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "c2")) {
            assertFalse(ServerThrottleManager.shouldSuppressRetry(r429));
            assertNull(mManager.checkPreFlight(scope.context()));
        }
        assertEquals(0, mManager.getActiveEntryCount());

        // Re-enable Broker host, record entry, then flip ECS flight to false -> clears map immediately
        ServerThrottleManager.setBrokerHostProcess(true);
        assertTrue(ServerThrottleManager.isServerThrottlingActive());
        record429(mManager, key, "60");
        assertEquals(1, mManager.getActiveEntryCount());

        mFlightsProvider.putFlight(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING.getKey(), "false");
        assertFalse(ServerThrottleManager.isServerThrottlingActive());
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "c3")) {
            assertFalse(mManager.onHttpResponse(scope.context(), r429, null, null));
            assertEquals(0, mManager.getActiveEntryCount());
            assertNull(mManager.checkPreFlight(scope.context()));
        }
    }

    @Test
    public void openTelemetryAttributes_distinguishNotThrottledFreshServerThrottleAndLocallySuppressed() {
        final TokenThrottleKey key = keyForIndex(99);

        // 1. Not throttled (plain HTTP 400 without Retry-After)
        final Span notThrottledSpan = mock(Span.class, CALLS_REAL_METHODS);
        try (Scope spanScope = SpanExtension.makeCurrentSpan(notThrottledSpan);
             TokenThrottleContext.TokenThrottleScope throttleScope = TokenThrottleContext.open(key, false, "c-ok")) {
            final HttpResponse plain400 = new HttpResponse(400, "{\"error\":\"authorization_pending\"}", null);
            assertFalse(mManager.onHttpResponse(throttleScope.context(), plain400, null, null));
        }
        verify(notThrottledSpan).setAttribute(
                eq(AttributeName.throttle_outcome.name()),
                eq(ThrottleOutcome.NOT_THROTTLED.getTelemetryValue())
        );
        verify(notThrottledSpan, never()).setAttribute(eq(AttributeName.throttle_scope_type.name()), anyString());

        // 2. Fresh server throttle (HTTP 429 with Retry-After: 60)
        final Span freshSpan = mock(Span.class, CALLS_REAL_METHODS);
        try (Scope spanScope = SpanExtension.makeCurrentSpan(freshSpan);
             TokenThrottleContext.TokenThrottleScope throttleScope = TokenThrottleContext.open(key, false, "c-fresh")) {
            final HttpResponse r429 = new HttpResponse(
                    429, "", Collections.singletonMap("Retry-After", Collections.singletonList("60")));
            assertTrue(mManager.onHttpResponse(throttleScope.context(), r429, null, null));
        }
        verify(freshSpan).setAttribute(AttributeName.throttle_outcome.name(), "fresh_server_throttle");
        verify(freshSpan).setAttribute(AttributeName.throttle_scope_type.name(), "AT_OR_RT");
        verify(freshSpan).setAttribute(AttributeName.throttle_http_status.name(), 429L);
        verify(freshSpan).setAttribute(AttributeName.throttle_retry_after_ms.name(), 60_000L);
        verify(freshSpan).setAttribute(AttributeName.throttle_retry_after_source.name(), "delta_seconds");
        verify(freshSpan).setAttribute(AttributeName.throttle_key_prefix.name(), key.getKeyPrefix());
        verify(freshSpan).setAttribute(AttributeName.throttle_cache_size.name(), 1L);

        // 3. Locally suppressed 10.2s later (49.8s == 49_800ms remaining)
        mClock.addMillis(10_200L);
        final Span suppressedSpan = mock(Span.class, CALLS_REAL_METHODS);
        try (Scope spanScope = SpanExtension.makeCurrentSpan(suppressedSpan);
             TokenThrottleContext.TokenThrottleScope throttleScope = TokenThrottleContext.open(key, false, "c-sup")) {
            assertNotNull(mManager.checkPreFlight(throttleScope.context()));
        }
        verify(suppressedSpan).setAttribute(AttributeName.throttle_outcome.name(), "locally_suppressed");
        verify(suppressedSpan).setAttribute(AttributeName.throttle_scope_type.name(), "AT_OR_RT");
        verify(suppressedSpan).setAttribute(AttributeName.throttle_http_status.name(), 429L);
        verify(suppressedSpan).setAttribute(AttributeName.throttle_retry_after_ms.name(), 49_800L);
        verify(suppressedSpan).setAttribute(AttributeName.throttle_key_prefix.name(), key.getKeyPrefix());
        verify(suppressedSpan, never()).setAttribute(eq(AttributeName.throttle_retry_after_source.name()), anyString());
    }

    private static TokenThrottleKey keyForIndex(final int index) {
        return TokenThrottleKey.forAtOrRt(
                "https://login.microsoftonline.com/common",
                "client-" + index,
                Collections.singletonList("User.Read"),
                null,
                "acct-" + index,
                "Bearer",
                null,
                "REGULAR",
                null
        );
    }

    private static void record429(
            final ServerThrottleManager manager,
            final TokenThrottleKey key,
            final String retryAfterSeconds) {
        final HttpResponse response = new HttpResponse(
                429,
                "",
                Collections.singletonMap("Retry-After", Collections.singletonList(retryAfterSeconds))
        );
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-rec")) {
            assertTrue(manager.onHttpResponse(scope.context(), response, null, null));
        }
    }

    private static void assertSuppressed(final ServerThrottleManager manager, final TokenThrottleKey key) {
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-chk")) {
            assertNotNull("Expected active suppression for " + key, manager.checkPreFlight(scope.context()));
        }
    }

    private static void assertAllowed(final ServerThrottleManager manager, final TokenThrottleKey key) {
        try (TokenThrottleContext.TokenThrottleScope scope = TokenThrottleContext.open(key, false, "corr-chk")) {
            assertNull("Expected request allowed for " + key, manager.checkPreFlight(scope.context()));
        }
    }

    private static void assertSanitized(final String description) {
        assertNotNull(description);
        assertTrue(description.startsWith("Request throttled by service."));
        assertFalse(description.contains("eyJ"));
        assertFalse(description.contains("refresh_token"));
        assertFalse(description.contains("password"));
        assertFalse(description.contains("alice@contoso.com"));
        assertFalse(description.contains("<"));
        assertFalse(description.contains("rt-secret-xyz"));
    }

    private static final class ConfigurableFlightsProvider extends MockFlightsProvider {
        private final Map<String, String> mOverrides = new ConcurrentHashMap<>();

        void putFlight(final String key, final String value) {
            mOverrides.put(key, value);
            addFlight(key, value);
        }

        @Override
        public boolean isFlightEnabled(@NonNull final IFlightConfig flightConfig) {
            final String override = mOverrides.get(flightConfig.getKey());
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
            final String override = mOverrides.get(flightConfig.getKey());
            if (override != null) {
                return Integer.parseInt(override);
            }
            return ((Integer) flightConfig.getDefaultValue()).intValue();
        }
    }

    private static final class AtomicFakeClock implements MonotonicClock {
        private final AtomicLong mNanos;
        private final AtomicLong mEpochMs;

        AtomicFakeClock(final long initialNanos, final long initialEpochMs) {
            mNanos = new AtomicLong(initialNanos);
            mEpochMs = new AtomicLong(initialEpochMs);
        }

        void setNanoTime(final long nanos) {
            mNanos.set(nanos);
        }

        void addMillis(final long deltaMs) {
            mNanos.addAndGet(TimeUnit.MILLISECONDS.toNanos(deltaMs));
            mEpochMs.addAndGet(deltaMs);
        }

        void addSeconds(final long deltaSeconds) {
            addMillis(TimeUnit.SECONDS.toMillis(deltaSeconds));
        }

        @Override
        public long nanoTime() {
            return mNanos.get();
        }

        @Override
        public long currentTimeMillis() {
            return mEpochMs.get();
        }
    }
}
