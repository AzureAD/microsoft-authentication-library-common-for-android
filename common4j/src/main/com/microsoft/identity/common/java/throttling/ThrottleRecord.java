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

import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * In-memory cache entry representing an active server-directed token endpoint backoff window.
 * <p>
 * Stores strictly monotonic {@code System.nanoTime()} deadlines, hashed account eviction tags,
 * HTTP status, normalized OAuth error/suberror codes, sanitized non-PII guidance, and
 * {@link CliTelemInfo}. Raw HTTP response bodies, tokens, secrets, passwords, and raw claims
 * are never stored.
 */
public final class ThrottleRecord {

    private static final long NANOS_PER_SECOND = 1_000_000_000L;
    private static final long NANOS_CEILING_ROUNDING = 999_999_999L;

    @NonNull
    private final ThrottleScopeType mScopeType;

    @NonNull
    private final Set<String> mAccountTagHashes;

    private final long mExpiresAtNanos;

    @NonNull
    private final AtomicLong mLastAccessedNanos;

    private final int mHttpStatusCode;

    @NonNull
    private final String mErrorCode;

    @Nullable
    private final String mSubErrorCode;

    @NonNull
    private final String mErrorDescription;

    @Nullable
    private final CliTelemInfo mCliTelemInfo;

    ThrottleRecord(
            @NonNull final TokenThrottleKey key,
            final long nowNanos,
            final long backoffMillis,
            final int httpStatusCode,
            @NonNull final String errorCode,
            @Nullable final String subErrorCode,
            @NonNull final String sanitizedErrorDescription,
            @Nullable final CliTelemInfo cliTelemInfo) {
        mScopeType = key.getScopeType();
        final Set<String> keyTags = key.getAccountTagHashes();
        mAccountTagHashes = keyTags.isEmpty()
                ? Collections.<String>emptySet()
                : Collections.unmodifiableSet(new LinkedHashSet<>(keyTags));
        mExpiresAtNanos = nowNanos + TimeUnit.MILLISECONDS.toNanos(Math.max(1L, backoffMillis));
        mLastAccessedNanos = new AtomicLong(nowNanos);
        mHttpStatusCode = httpStatusCode;
        mErrorCode = errorCode;
        mSubErrorCode = subErrorCode;
        mErrorDescription = sanitizedErrorDescription;
        mCliTelemInfo = cliTelemInfo != null ? new CliTelemInfo(cliTelemInfo) : null;
    }

    @NonNull
    public ThrottleScopeType getScopeType() {
        return mScopeType;
    }

    @NonNull
    public Set<String> getAccountTagHashes() {
        return Collections.unmodifiableSet(mAccountTagHashes);
    }

    public long getExpiresAtNanos() {
        return mExpiresAtNanos;
    }

    public long getLastAccessedNanos() {
        return mLastAccessedNanos.get();
    }

    void touch(final long nowNanos) {
        mLastAccessedNanos.set(nowNanos);
    }

    public int getHttpStatusCode() {
        return mHttpStatusCode;
    }

    @NonNull
    public String getErrorCode() {
        return mErrorCode;
    }

    @Nullable
    public String getSubErrorCode() {
        return mSubErrorCode;
    }

    @NonNull
    public String getErrorDescription() {
        return mErrorDescription;
    }

    @Nullable
    public CliTelemInfo getCliTelemInfo() {
        return mCliTelemInfo != null ? new CliTelemInfo(mCliTelemInfo) : null;
    }

    boolean isExpired(final long nowNanos) {
        return nowNanos - mExpiresAtNanos >= 0L;
    }

    boolean hasAnyAccountTag(@NonNull final Set<String> targetTagHashes) {
        if (mAccountTagHashes.isEmpty() || targetTagHashes.isEmpty()) {
            return false;
        }
        for (final String tag : targetTagHashes) {
            if (mAccountTagHashes.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    static long computeCeilingRetryAfterSeconds(final long remainingNanos) {
        if (remainingNanos <= 0L) {
            return 1L;
        }
        if (remainingNanos > Long.MAX_VALUE - NANOS_CEILING_ROUNDING) {
            return remainingNanos / NANOS_PER_SECOND;
        }
        return Math.max(1L, (remainingNanos + NANOS_CEILING_ROUNDING) / NANOS_PER_SECOND);
    }

    @NonNull
    TokenResult toSuppressedTokenResult(
            final long remainingNanos,
            @Nullable final String callerCorrelationId) {
        final long retryAfterSeconds = computeCeilingRetryAfterSeconds(remainingNanos);
        final MicrosoftTokenErrorResponse errorResponse = new MicrosoftTokenErrorResponse();
        errorResponse.setStatusCode(mHttpStatusCode);
        errorResponse.setError(mErrorCode);
        errorResponse.setSubError(mSubErrorCode);
        errorResponse.setErrorDescription(
                ServerThrottleManager.formatThrottledErrorMessage(
                        mErrorDescription,
                        retryAfterSeconds,
                        callerCorrelationId
                )
        );
        errorResponse.setResponseBody(null);
        errorResponse.setResponseHeadersJson(
                HeaderSerializationUtil.toJson(
                        RetryAfterParser.singletonRetryAfterHeader(retryAfterSeconds)
                )
        );
        if (!StringUtil.isNullOrEmpty(callerCorrelationId)) {
            errorResponse.setCorrelationId(callerCorrelationId.trim());
        }

        final TokenResult result = new TokenResult(null, errorResponse);
        result.setCliTelemInfo(getCliTelemInfo());
        result.setLocallySuppressedThrottle(true);
        return result;
    }
}
