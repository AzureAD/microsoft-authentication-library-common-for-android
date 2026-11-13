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

import com.microsoft.identity.common.java.exception.BaseException;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.flighting.CommonFlight;
import com.microsoft.identity.common.java.flighting.CommonFlightsManager;
import com.microsoft.identity.common.java.flighting.IFlightsProvider;
import com.microsoft.identity.common.java.logging.DiagnosticContext;
import com.microsoft.identity.common.java.logging.IRequestContext;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import javax.annotation.Nullable;

import io.opentelemetry.api.trace.Span;
import lombok.NonNull;

/**
 * Process-local, bounded, thread-safe coordinator for Broker server-directed token endpoint
 * throttling.
 */
public final class ServerThrottleManager {

    private static final String TAG = ServerThrottleManager.class.getSimpleName();
    private static final String X_MS_CLITELEM_HEADER = "x-ms-clitelem";
    private static final String DEFAULT_THROTTLED_DESCRIPTION = "Request throttled by service.";
    private static final String SERVICE_UNAVAILABLE_DESCRIPTION = "Service temporarily unavailable.";
    private static final String TOO_MANY_REQUESTS_DESCRIPTION = "Too many requests.";

    private static final long DEFAULT_429_SECONDS_FALLBACK = 60L;
    private static final long DEFAULT_5XX_SECONDS_FALLBACK = 120L;
    private static final long MAX_RETRY_AFTER_SECONDS_FALLBACK = 3600L;
    private static final int DEFAULT_CACHE_MAX_ENTRIES = 512;
    private static final int MIN_CACHE_MAX_ENTRIES = 64;
    private static final int MAX_CACHE_MAX_ENTRIES = 4096;
    private static final long NANOS_PER_MILLI = 1_000_000L;
    private static final long NANOS_MILLI_CEILING_ROUNDING = 999_999L;

    private static final ServerThrottleManager DEFAULT_INSTANCE =
            new ServerThrottleManager(MonotonicClock.SYSTEM);

    @Nullable
    private static volatile ServerThrottleManager sTestingInstance;

    private static volatile boolean sIsBrokerHostProcess = false;

    @NonNull
    private final MonotonicClock mClock;

    private final int mMaxEntriesOverride;

    @NonNull
    private final ConcurrentHashMap<String, ThrottleRecord> mCache = new ConcurrentHashMap<>();

    @NonNull
    private final ReentrantLock mWriteLock = new ReentrantLock();

    /**
     * Creates a {@link ServerThrottleManager} anchored to the given {@link MonotonicClock}.
     *
     * @param clock monotonic/epoch clock.
     */
    public ServerThrottleManager(@NonNull final MonotonicClock clock) {
        this(clock, 0);
    }

    ServerThrottleManager(@NonNull final MonotonicClock clock, final int maxEntriesOverride) {
        mClock = clock;
        mMaxEntriesOverride = maxEntriesOverride;
    }

    /**
     * Returns the active singleton {@link ServerThrottleManager}.
     *
     * @return non-null {@link ServerThrottleManager}.
     */
    @NonNull
    public static ServerThrottleManager getInstance() {
        final ServerThrottleManager testingInstance = sTestingInstance;
        return testingInstance != null ? testingInstance : DEFAULT_INSTANCE;
    }

    /**
     * Overrides or resets the singleton instance for deterministic unit testing.
     *
     * @param instance test instance or {@code null} to restore default.
     */
    public static void setInstanceForTesting(@Nullable final ServerThrottleManager instance) {
        sTestingInstance = instance;
    }

    /**
     * Marks whether the current process is an Android Broker host process.
     *
     * @param isBrokerHost {@code true} inside Broker host process; {@code false} in client SDKs.
     */
    public static void setBrokerHostProcess(final boolean isBrokerHost) {
        sIsBrokerHostProcess = isBrokerHost;
    }

    /**
     * Returns whether the current process has been initialized as an Android Broker host process.
     *
     * @return {@code true} if running in Broker host process.
     */
    public static boolean isBrokerHostProcess() {
        return sIsBrokerHostProcess;
    }

    /**
     * Returns {@code true} only when running in a Broker host process AND
     * {@link CommonFlight#ENABLE_BROKER_SERVER_THROTTLING} is enabled.
     *
     * @return {@code true} when server-directed throttling is active.
     */
    public static boolean isServerThrottlingActive() {
        if (!sIsBrokerHostProcess) {
            return false;
        }
        final IFlightsProvider flightsProvider = CommonFlightsManager.INSTANCE.getFlightsProvider();
        return flightsProvider != null
                && flightsProvider.isFlightEnabled(CommonFlight.ENABLE_BROKER_SERVER_THROTTLING);
    }

    /**
     * Normalizes missing/empty or numeric status-code fallback errors on HTTP {@code 429} and
     * {@code 500..599} while preserving non-empty error codes on other HTTP statuses.
     *
     * @param statusCode HTTP status code.
     * @param rawError   raw OAuth error string (nullable).
     * @return non-null normalized error code.
     */
    @NonNull
    public static String resolveDefaultErrorCode(
            final int statusCode,
            @Nullable final String rawError) {
        final boolean isMissing = StringUtil.isNullOrEmpty(rawError);
        if (isMissing || (rawError != null && String.valueOf(statusCode).equals(rawError.trim()))) {
            if (statusCode == 429) {
                return ServiceException.REQUEST_THROTTLED_AT_ESTS_GATEWAY;
            }
            if (statusCode >= 500 && statusCode <= 599) {
                return ServiceException.SERVICE_NOT_AVAILABLE;
            }
            if (isMissing) {
                return ServiceException.UNKNOWN_ERROR;
            }
        }
        return rawError != null ? rawError : ServiceException.UNKNOWN_ERROR;
    }

    /**
     * Formats a deterministic, non-PII throttled error message carrying {@code retryAfterSeconds}
     * and optional {@code correlationId}.
     *
     * @param baseDescription   optional base error description.
     * @param retryAfterSeconds positive remaining backoff seconds (clamped to {@code >= 1}).
     * @param correlationId     optional request correlation ID.
     * @return formatted error message.
     */
    @NonNull
    public static String formatThrottledErrorMessage(
            @Nullable final String baseDescription,
            final long retryAfterSeconds,
            @Nullable final String correlationId) {
        final long clampedSeconds = Math.max(1L, retryAfterSeconds);
        final String sanitizedBase = sanitizeErrorDescription(baseDescription);
        final StringBuilder sb = new StringBuilder(sanitizedBase.length() + 64);
        sb.append(sanitizedBase);
        if (!sanitizedBase.endsWith(".")) {
            sb.append('.');
        }
        sb.append(" retryAfterSeconds=").append(clampedSeconds);
        if (!StringUtil.isNullOrEmpty(correlationId)) {
            sb.append("; correlationId=").append(correlationId.trim());
        }
        return sb.toString();
    }

    /**
     * Returns {@code true} when internal transport retries (HTTP 500/503/504 1s retry or
     * HTTP 401 PKeyAuth challenge follow-up POST) must be suppressed for an active
     * {@link TokenThrottleContext}.
     *
     * @param response HTTP response received from transport.
     * @return {@code true} if retry must be suppressed.
     */
    public static boolean shouldSuppressRetry(@Nullable final HttpResponse response) {
        if (response == null || !isServerThrottlingActive()) {
            return false;
        }
        if (TokenThrottleContext.current() == null) {
            return false;
        }
        final ServerThrottleManager manager = getInstance();
        final long backoffMillis = RetryAfterParser.resolveBackoffMillis(
                response.getStatusCode(),
                response.getHeaders(),
                manager.mClock,
                getDefault429Seconds(),
                getDefault5xxSeconds(),
                getMaxRetryAfterSeconds()
        );
        return backoffMillis > 0L;
    }

    /**
     * Returns {@code true} when server-directed throttling is active in the Broker host process
     * and {@code exception} is a {@link ServiceException} representing either a locally
     * suppressed throttle or a fresh server throttle response.
     *
     * @param exception candidate exception.
     * @return {@code true} if throttled while Broker server throttling is active.
     */
    public static boolean isThrottledServiceException(@Nullable final BaseException exception) {
        if (!isServerThrottlingActive()) {
            return false;
        }
        if (!(exception instanceof ServiceException)) {
            return false;
        }
        final ServiceException serviceException = (ServiceException) exception;
        if (serviceException.isLocallySuppressedThrottle()) {
            return true;
        }
        final int statusCode = serviceException.getHttpStatusCode();
        if (statusCode == 429 || (statusCode >= 500 && statusCode <= 599)) {
            return true;
        }
        if (ServiceException.REQUEST_THROTTLED_AT_ESTS_GATEWAY.equalsIgnoreCase(
                serviceException.getErrorCode())) {
            return true;
        }
        if (statusCode != ServiceException.DEFAULT_STATUS_CODE && statusCode < 300) {
            return false;
        }
        final Long retryAfterMillis = RetryAfterParser.parseRetryAfterMillis(
                serviceException.getHttpResponseHeaders(),
                getInstance().mClock,
                getMaxRetryAfterSeconds()
        );
        return retryAfterMillis != null && retryAfterMillis.longValue() > 0L;
    }

    /**
     * Checks whether a non-interactive request matches an active backoff window.
     *
     * @param ctx active {@link TokenThrottleContext} (nullable).
     * @return locally suppressed {@link TokenResult}, or {@code null} if allowed to proceed.
     */
    @Nullable
    public TokenResult checkPreFlight(@Nullable final TokenThrottleContext ctx) {
        if (!isServerThrottlingActive()) {
            if (!mCache.isEmpty()) {
                clearAll();
            }
            return null;
        }
        if (ctx == null || ctx.isInteractiveOrigin()) {
            return null;
        }

        final TokenThrottleKey throttleKey = ctx.getThrottleKey();
        final String keyHex = throttleKey.getSha256Hex();
        final ThrottleRecord record = mCache.get(keyHex);
        if (record == null) {
            return null;
        }

        final long nowNanos = mClock.nanoTime();
        if (record.isExpired(nowNanos)) {
            mWriteLock.lock();
            try {
                mCache.remove(keyHex, record);
            } finally {
                mWriteLock.unlock();
            }
            return null;
        }

        final long remainingNanos = record.getExpiresAtNanos() - nowNanos;
        record.touch(nowNanos);
        final String correlationId = resolveEffectiveCorrelationId(ctx, null);
        final TokenResult suppressedResult = record.toSuppressedTokenResult(remainingNanos, correlationId);

        final long remainingMillis = computeCeilingMillis(remainingNanos);
        emitTelemetryAndLog(
                ctx,
                ThrottleOutcome.LOCALLY_SUPPRESSED,
                throttleKey,
                record.getHttpStatusCode(),
                remainingMillis,
                null,
                null
        );
        return suppressedResult;
    }

    /**
     * Inspects an HTTP token-endpoint response and records bounded throttle state when eligible.
     *
     * @param ctx          active {@link TokenThrottleContext} (nullable; falls back to {@link TokenThrottleContext#current()}).
     * @param response     HTTP response.
     * @param parsedError  optional parsed {@link TokenErrorResponse}.
     * @param cliTelemInfo optional parsed {@link CliTelemInfo}.
     * @return {@code true} if the response triggered server-directed throttling.
     */
    public boolean onHttpResponse(
            @Nullable final TokenThrottleContext ctx,
            @NonNull final HttpResponse response,
            @Nullable final TokenErrorResponse parsedError,
            @Nullable final CliTelemInfo cliTelemInfo) {
        if (!isServerThrottlingActive()) {
            if (!mCache.isEmpty()) {
                clearAll();
            }
            return false;
        }

        final TokenThrottleContext effectiveCtx = ctx != null ? ctx : TokenThrottleContext.current();
        if (effectiveCtx == null) {
            return false;
        }

        final int statusCode = response.getStatusCode();
        final RetryAfterParser.BackoffResolution resolution = RetryAfterParser.resolveBackoff(
                statusCode,
                response.getHeaders(),
                mClock,
                getDefault429Seconds(),
                getDefault5xxSeconds(),
                getMaxRetryAfterSeconds()
        );
        if (resolution.mBackoffMillis <= 0L) {
            if (effectiveCtx.getLiveThrottleResult() == null) {
                setOutcomeAttributeOnly(effectiveCtx, ThrottleOutcome.NOT_THROTTLED);
            }
            return false;
        }

        final String rawError = parsedError != null ? parsedError.getError() : null;
        final String normalizedErrorCode = resolveDefaultErrorCode(statusCode, rawError);
        final String subErrorCode = parsedError != null && !StringUtil.isNullOrEmpty(parsedError.getSubError())
                ? parsedError.getSubError().trim()
                : null;
        final String sanitizedDescription = sanitizeErrorDescription(
                parsedError != null ? parsedError.getErrorDescription() : null
        );
        final CliTelemInfo resolvedCliTelem = cliTelemInfo != null
                ? new CliTelemInfo(cliTelemInfo)
                : extractCliTelemFromHeaders(response.getHeaders());
        final String effectiveCorrelationId = resolveEffectiveCorrelationId(effectiveCtx, parsedError);
        final long retryAfterSeconds = Math.max(1L, (resolution.mBackoffMillis + 999L) / 1000L);

        final TokenThrottleKey throttleKey = effectiveCtx.getThrottleKey();
        final String keyHex = throttleKey.getSha256Hex();
        final int maxEntries = resolveMaxCacheEntries();
        final long nowNanos = mClock.nanoTime();
        final ThrottleRecord newRecord = new ThrottleRecord(
                throttleKey,
                nowNanos,
                resolution.mBackoffMillis,
                statusCode,
                normalizedErrorCode,
                subErrorCode,
                sanitizedDescription,
                resolvedCliTelem
        );
        final int cacheSizeAfterInsert;

        mWriteLock.lock();
        try {
            int maxAllowedBeforePut = mCache.containsKey(keyHex) ? maxEntries : maxEntries - 1;
            if (mCache.size() > maxAllowedBeforePut) {
                evictExpiredLocked(nowNanos);
                maxAllowedBeforePut = mCache.containsKey(keyHex) ? maxEntries : maxEntries - 1;
                while (mCache.size() > maxAllowedBeforePut && !mCache.isEmpty()) {
                    if (!evictLeastRecentlyUsedLocked(keyHex)) {
                        break;
                    }
                }
            }
            mCache.put(keyHex, newRecord);
            cacheSizeAfterInsert = mCache.size();
        } finally {
            mWriteLock.unlock();
        }

        final LiveThrottleCapture liveCapture = new LiveThrottleCapture(
                statusCode,
                normalizedErrorCode,
                subErrorCode,
                sanitizedDescription,
                retryAfterSeconds,
                response.getHeaders(),
                resolvedCliTelem,
                effectiveCorrelationId
        );
        effectiveCtx.recordLiveThrottle(liveCapture);

        if (parsedError != null) {
            parsedError.setError(normalizedErrorCode);
            parsedError.setResponseHeadersJson(
                    HeaderSerializationUtil.toJson(liveCapture.getResponseHeaders())
            );
        }

        emitTelemetryAndLog(
                effectiveCtx,
                ThrottleOutcome.FRESH_SERVER_THROTTLE,
                throttleKey,
                statusCode,
                resolution.mBackoffMillis,
                resolution.mSource,
                Long.valueOf(cacheSizeAfterInsert)
        );
        return true;
    }

    /**
     * Evicts all user-tagged throttle records ({@code AT_OR_RT}, {@code BROKER_PRT},
     * {@code FOCI_PROBE}) matching any of the supplied account identifiers, while leaving
     * device-scoped {@code DEVICE_TOKEN} entries intact.
     *
     * @param rawAccountIds account identifiers (homeAccountId, localAccountId, username).
     * @return number of active entries evicted.
     */
    public int evictAccountIdentifiers(@Nullable final String... rawAccountIds) {
        final Set<String> targetTags = TokenThrottleKey.hashAccountTags(rawAccountIds);
        if (targetTags.isEmpty()) {
            return 0;
        }
        final long nowNanos = mClock.nanoTime();
        int evicted = 0;
        mWriteLock.lock();
        try {
            final Iterator<Map.Entry<String, ThrottleRecord>> iterator = mCache.entrySet().iterator();
            while (iterator.hasNext()) {
                final ThrottleRecord record = iterator.next().getValue();
                if (record.isExpired(nowNanos)) {
                    iterator.remove();
                } else if (record.hasAnyAccountTag(targetTags)) {
                    iterator.remove();
                    evicted++;
                }
            }
        } finally {
            mWriteLock.unlock();
        }
        return evicted;
    }

    /**
     * Evicts only {@link ThrottleScopeType#BROKER_PRT} entries matching any of the supplied
     * account identifiers (invoked when a fresh or recovered PRT is persisted).
     *
     * @param rawAccountIds account identifiers.
     * @return number of active {@code BROKER_PRT} entries evicted.
     */
    public int evictBrokerPrtForAccountIdentifiers(@Nullable final String... rawAccountIds) {
        final Set<String> targetTags = TokenThrottleKey.hashAccountTags(rawAccountIds);
        if (targetTags.isEmpty()) {
            return 0;
        }
        final long nowNanos = mClock.nanoTime();
        int evicted = 0;
        mWriteLock.lock();
        try {
            final Iterator<Map.Entry<String, ThrottleRecord>> iterator = mCache.entrySet().iterator();
            while (iterator.hasNext()) {
                final ThrottleRecord record = iterator.next().getValue();
                if (record.isExpired(nowNanos)) {
                    iterator.remove();
                } else if (record.getScopeType() == ThrottleScopeType.BROKER_PRT
                        && record.hasAnyAccountTag(targetTags)) {
                    iterator.remove();
                    evicted++;
                }
            }
        } finally {
            mWriteLock.unlock();
        }
        return evicted;
    }

    /**
     * Returns the current count of non-expired throttle records in memory.
     *
     * @return active entry count.
     */
    public int getActiveEntryCount() {
        if (mCache.isEmpty()) {
            return 0;
        }
        final long nowNanos = mClock.nanoTime();
        mWriteLock.lock();
        try {
            evictExpiredLocked(nowNanos);
            return mCache.size();
        } finally {
            mWriteLock.unlock();
        }
    }

    /**
     * Clears all cached throttle state immediately.
     */
    public void clearAll() {
        mWriteLock.lock();
        try {
            mCache.clear();
        } finally {
            mWriteLock.unlock();
        }
    }

    private void evictExpiredLocked(final long nowNanos) {
        final Iterator<Map.Entry<String, ThrottleRecord>> iterator = mCache.entrySet().iterator();
        while (iterator.hasNext()) {
            if (iterator.next().getValue().isExpired(nowNanos)) {
                iterator.remove();
            }
        }
    }

    private boolean evictLeastRecentlyUsedLocked(@Nullable final String excludedKey) {
        String lruKey = null;
        ThrottleRecord lruRecord = null;
        for (final Map.Entry<String, ThrottleRecord> entry : mCache.entrySet()) {
            final String candidateKey = entry.getKey();
            if (excludedKey != null && excludedKey.equals(candidateKey)) {
                continue;
            }
            final ThrottleRecord candidate = entry.getValue();
            if (lruRecord == null
                    || candidate.getLastAccessedNanos() - lruRecord.getLastAccessedNanos() < 0L) {
                lruKey = candidateKey;
                lruRecord = candidate;
            }
        }
        if (lruKey != null) {
            mCache.remove(lruKey);
            return true;
        }
        return false;
    }

    private int resolveMaxCacheEntries() {
        if (mMaxEntriesOverride > 0) {
            return mMaxEntriesOverride;
        }
        final IFlightsProvider provider = CommonFlightsManager.INSTANCE.getFlightsProvider();
        final int configured = provider != null
                ? provider.getIntValue(CommonFlight.BROKER_THROTTLE_CACHE_MAX_ENTRIES)
                : DEFAULT_CACHE_MAX_ENTRIES;
        if (configured < MIN_CACHE_MAX_ENTRIES) {
            return MIN_CACHE_MAX_ENTRIES;
        }
        if (configured > MAX_CACHE_MAX_ENTRIES) {
            return MAX_CACHE_MAX_ENTRIES;
        }
        return configured;
    }

    private static long getDefault429Seconds() {
        final IFlightsProvider provider = CommonFlightsManager.INSTANCE.getFlightsProvider();
        if (provider == null) {
            return DEFAULT_429_SECONDS_FALLBACK;
        }
        return Math.max(1L, provider.getIntValue(CommonFlight.BROKER_THROTTLE_DEFAULT_429_SECONDS));
    }

    private static long getDefault5xxSeconds() {
        final IFlightsProvider provider = CommonFlightsManager.INSTANCE.getFlightsProvider();
        if (provider == null) {
            return DEFAULT_5XX_SECONDS_FALLBACK;
        }
        return Math.max(1L, provider.getIntValue(CommonFlight.BROKER_THROTTLE_DEFAULT_5XX_SECONDS));
    }

    private static long getMaxRetryAfterSeconds() {
        final IFlightsProvider provider = CommonFlightsManager.INSTANCE.getFlightsProvider();
        if (provider == null) {
            return MAX_RETRY_AFTER_SECONDS_FALLBACK;
        }
        return Math.max(1L, provider.getIntValue(CommonFlight.BROKER_THROTTLE_MAX_RETRY_AFTER_SECONDS));
    }

    @Nullable
    private static CliTelemInfo extractCliTelemFromHeaders(
            @Nullable final Map<String, List<String>> headers) {
        if (headers == null || headers.isEmpty()) {
            return null;
        }
        for (final Map.Entry<String, List<String>> entry : headers.entrySet()) {
            final String key = entry.getKey();
            if (key == null || !X_MS_CLITELEM_HEADER.equalsIgnoreCase(key.trim())) {
                continue;
            }
            final List<String> values = entry.getValue();
            if (values != null && !values.isEmpty() && !StringUtil.isNullOrEmpty(values.get(0))) {
                return CliTelemInfo.fromXMsCliTelemHeader(values.get(0));
            }
        }
        return null;
    }

    @Nullable
    private static String resolveEffectiveCorrelationId(
            @NonNull final TokenThrottleContext ctx,
            @Nullable final TokenErrorResponse parsedError) {
        if (parsedError instanceof MicrosoftTokenErrorResponse) {
            final String errorCorrelationId = ((MicrosoftTokenErrorResponse) parsedError).getCorrelationId();
            if (!StringUtil.isNullOrEmpty(errorCorrelationId)) {
                return errorCorrelationId.trim();
            }
        }
        if (!StringUtil.isNullOrEmpty(ctx.getCorrelationId())) {
            return ctx.getCorrelationId();
        }
        final IRequestContext requestContext = DiagnosticContext.INSTANCE.getRequestContext();
        if (requestContext != null) {
            final String threadCorrelationId = requestContext.get(DiagnosticContext.CORRELATION_ID);
            if (!StringUtil.isNullOrEmpty(threadCorrelationId)
                    && !DiagnosticContext.UNSET_CORRELATION_ID.equals(threadCorrelationId)) {
                return threadCorrelationId.trim();
            }
        }
        return null;
    }

    /**
     * Selects a static, non-PII throttling guidance string from a bounded allowlist rather than
     * retaining arbitrary server descriptions that could embed user/account/tenant GUIDs or other
     * identifiers.
     */
    @NonNull
    static String sanitizeErrorDescription(@Nullable final String rawDescription) {
        if (rawDescription == null || StringUtil.isNullOrEmpty(rawDescription)) {
            return DEFAULT_THROTTLED_DESCRIPTION;
        }
        String candidate = rawDescription.trim();
        final int suffixIdx = candidate.indexOf(" retryAfterSeconds=");
        if (suffixIdx >= 0) {
            candidate = candidate.substring(0, suffixIdx).trim();
        }
        while (candidate.endsWith(".")) {
            candidate = candidate.substring(0, candidate.length() - 1).trim();
        }
        if ("request throttled by service".equalsIgnoreCase(candidate)) {
            return DEFAULT_THROTTLED_DESCRIPTION;
        }
        if ("service temporarily unavailable".equalsIgnoreCase(candidate)
                || "temporarily unavailable".equalsIgnoreCase(candidate)) {
            return SERVICE_UNAVAILABLE_DESCRIPTION;
        }
        if ("too many requests".equalsIgnoreCase(candidate)) {
            return TOO_MANY_REQUESTS_DESCRIPTION;
        }
        return DEFAULT_THROTTLED_DESCRIPTION;
    }

    private static long computeCeilingMillis(final long remainingNanos) {
        if (remainingNanos <= 0L) {
            return 1L;
        }
        if (remainingNanos > Long.MAX_VALUE - NANOS_MILLI_CEILING_ROUNDING) {
            return remainingNanos / NANOS_PER_MILLI;
        }
        return Math.max(1L, (remainingNanos + NANOS_MILLI_CEILING_ROUNDING) / NANOS_PER_MILLI);
    }

    private static void setOutcomeAttributeOnly(
            @NonNull final TokenThrottleContext ctx,
            @NonNull final ThrottleOutcome outcome) {
        final Span currentSpan = SpanExtension.current();
        currentSpan.setAttribute(AttributeName.throttle_outcome.name(), outcome.getTelemetryValue());
        final Span contextSpan = ctx.getSpan();
        if (contextSpan != currentSpan) {
            contextSpan.setAttribute(AttributeName.throttle_outcome.name(), outcome.getTelemetryValue());
        }
    }

    private static void emitTelemetryAndLog(
            @NonNull final TokenThrottleContext ctx,
            @NonNull final ThrottleOutcome outcome,
            @NonNull final TokenThrottleKey key,
            final int statusCode,
            final long retryAfterMs,
            @Nullable final String retryAfterSource,
            @Nullable final Long cacheSize) {
        final Span currentSpan = SpanExtension.current();
        applyThrottleAttributes(
                currentSpan,
                outcome,
                key,
                statusCode,
                retryAfterMs,
                retryAfterSource,
                cacheSize
        );
        final Span contextSpan = ctx.getSpan();
        if (contextSpan != currentSpan) {
            applyThrottleAttributes(
                    contextSpan,
                    outcome,
                    key,
                    statusCode,
                    retryAfterMs,
                    retryAfterSource,
                    cacheSize
            );
        }

        Logger.info(
                TAG,
                "[ServerThrottle] outcome=" + outcome.getTelemetryValue()
                        + " scope=" + key.getScopeType().name()
                        + " status=" + statusCode
                        + " retryAfterMs=" + retryAfterMs
                        + " keyPrefix=" + key.getKeyPrefix()
        );
    }

    private static void applyThrottleAttributes(
            @NonNull final Span span,
            @NonNull final ThrottleOutcome outcome,
            @NonNull final TokenThrottleKey key,
            final int statusCode,
            final long retryAfterMs,
            @Nullable final String retryAfterSource,
            @Nullable final Long cacheSize) {
        span.setAttribute(AttributeName.throttle_outcome.name(), outcome.getTelemetryValue());
        span.setAttribute(AttributeName.throttle_scope_type.name(), key.getScopeType().name());
        span.setAttribute(AttributeName.throttle_http_status.name(), (long) statusCode);
        span.setAttribute(AttributeName.throttle_retry_after_ms.name(), retryAfterMs);
        span.setAttribute(AttributeName.throttle_key_prefix.name(), key.getKeyPrefix());
        if (retryAfterSource != null) {
            span.setAttribute(AttributeName.throttle_retry_after_source.name(), retryAfterSource);
        }
        if (cacheSize != null) {
            span.setAttribute(AttributeName.throttle_cache_size.name(), cacheSize.longValue());
        }
    }
}
