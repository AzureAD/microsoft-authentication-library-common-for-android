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

import com.microsoft.identity.common.java.controllers.ExceptionAdapter;
import com.microsoft.identity.common.java.exception.ServiceException;
import com.microsoft.identity.common.java.providers.microsoft.MicrosoftTokenErrorResponse;
import com.microsoft.identity.common.java.providers.oauth2.TokenResult;
import com.microsoft.identity.common.java.telemetry.CliTelemInfo;
import com.microsoft.identity.common.java.util.HeaderSerializationUtil;
import com.microsoft.identity.common.java.util.StringUtil;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import lombok.NonNull;

/**
 * Captures a fresh server-directed throttle response observed during an active
 * {@link TokenThrottleContext} so multi-step flows (such as {@code getNonce} followed by
 * {@code /token}) can short-circuit immediately while preserving {@code fresh_server_throttle}
 * telemetry and server error metadata.
 */
public final class LiveThrottleCapture {

    private final int mHttpStatusCode;

    @NonNull
    private final String mErrorCode;

    @Nullable
    private final String mSubErrorCode;

    @NonNull
    private final String mErrorDescription;

    private final long mRetryAfterSeconds;

    @NonNull
    private final Map<String, List<String>> mResponseHeaders;

    @Nullable
    private final CliTelemInfo mCliTelemInfo;

    @Nullable
    private final String mCorrelationId;

    /**
     * Creates a {@link LiveThrottleCapture} instance.
     *
     * @param httpStatusCode    HTTP status code.
     * @param errorCode         normalized OAuth error code.
     * @param subErrorCode      optional OAuth suberror code.
     * @param errorDescription  sanitized non-PII error description.
     * @param retryAfterSeconds resolved positive backoff seconds (clamped to {@code >= 1}).
     * @param responseHeaders   HTTP response headers (normalized to include {@code Retry-After}).
     * @param cliTelemInfo      optional {@link CliTelemInfo} parsed from {@code x-ms-clitelem}.
     * @param correlationId     optional request correlation ID.
     */
    public LiveThrottleCapture(
            final int httpStatusCode,
            @NonNull final String errorCode,
            @Nullable final String subErrorCode,
            @NonNull final String errorDescription,
            final long retryAfterSeconds,
            @Nullable final Map<String, List<String>> responseHeaders,
            @Nullable final CliTelemInfo cliTelemInfo,
            @Nullable final String correlationId) {
        mHttpStatusCode = httpStatusCode;
        mErrorCode = errorCode;
        mSubErrorCode = !StringUtil.isNullOrEmpty(subErrorCode) ? subErrorCode.trim() : null;
        mErrorDescription = errorDescription;
        mRetryAfterSeconds = Math.max(1L, retryAfterSeconds);
        mResponseHeaders = normalizeHeadersWithRetryAfter(responseHeaders, mRetryAfterSeconds);
        mCliTelemInfo = cliTelemInfo != null ? new CliTelemInfo(cliTelemInfo) : null;
        mCorrelationId = !StringUtil.isNullOrEmpty(correlationId) ? correlationId.trim() : null;
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

    public long getRetryAfterSeconds() {
        return mRetryAfterSeconds;
    }

    @NonNull
    public Map<String, List<String>> getResponseHeaders() {
        return Collections.unmodifiableMap(mResponseHeaders);
    }

    @Nullable
    public CliTelemInfo getCliTelemInfo() {
        return mCliTelemInfo != null ? new CliTelemInfo(mCliTelemInfo) : null;
    }

    @Nullable
    public String getCorrelationId() {
        return mCorrelationId;
    }

    /**
     * Converts this live capture into an unsuccessful {@link TokenResult} with
     * {@code isLocallySuppressedThrottle() == false} and {@code responseBody == null}.
     *
     * @return non-null unsuccessful {@link TokenResult}.
     */
    @NonNull
    public TokenResult toTokenResult() {
        final MicrosoftTokenErrorResponse errorResponse = new MicrosoftTokenErrorResponse();
        errorResponse.setStatusCode(mHttpStatusCode);
        errorResponse.setError(mErrorCode);
        errorResponse.setSubError(mSubErrorCode);
        errorResponse.setErrorDescription(
                ServerThrottleManager.formatThrottledErrorMessage(
                        mErrorDescription,
                        mRetryAfterSeconds,
                        mCorrelationId
                )
        );
        errorResponse.setResponseBody(null);
        errorResponse.setResponseHeadersJson(HeaderSerializationUtil.toJson(mResponseHeaders));
        if (!StringUtil.isNullOrEmpty(mCorrelationId)) {
            errorResponse.setCorrelationId(mCorrelationId);
        }

        final TokenResult result = new TokenResult(null, errorResponse);
        result.setCliTelemInfo(getCliTelemInfo());
        result.setLocallySuppressedThrottle(false);
        return result;
    }

    /**
     * Converts this live capture into a {@link ServiceException} preserving status code,
     * {@code Retry-After} headers, suberror, {@link CliTelemInfo}, effective correlation ID,
     * and formatted {@code retryAfterSeconds} message.
     *
     * @param correlationIdOverride optional caller correlation ID override.
     * @return non-null {@link ServiceException}.
     */
    @NonNull
    public ServiceException toServiceException(@Nullable final String correlationIdOverride) {
        final String effectiveCorrelationId = !StringUtil.isNullOrEmpty(correlationIdOverride)
                ? correlationIdOverride.trim()
                : mCorrelationId;
        final String formattedMessage = ServerThrottleManager.formatThrottledErrorMessage(
                mErrorDescription,
                mRetryAfterSeconds,
                effectiveCorrelationId
        );
        final ServiceException serviceException = new ServiceException(
                mErrorCode,
                formattedMessage,
                mHttpStatusCode,
                null
        );
        serviceException.setSubErrorCode(mSubErrorCode);
        serviceException.setHttpResponseHeaders(copyHeadersToHashMap(mResponseHeaders));
        ExceptionAdapter.applyCliTelemInfo(mCliTelemInfo, serviceException);
        if (!StringUtil.isNullOrEmpty(effectiveCorrelationId)) {
            serviceException.setCorrelationId(effectiveCorrelationId);
        }
        serviceException.setLocallySuppressedThrottle(false);
        return serviceException;
    }

    @NonNull
    private static Map<String, List<String>> normalizeHeadersWithRetryAfter(
            @Nullable final Map<String, List<String>> rawHeaders,
            final long retryAfterSeconds) {
        final Map<String, List<String>> copy = new LinkedHashMap<>();
        if (rawHeaders != null) {
            for (final Map.Entry<String, List<String>> entry : rawHeaders.entrySet()) {
                final String key = entry.getKey();
                if (key == null || RetryAfterParser.RETRY_AFTER_HEADER.equalsIgnoreCase(key.trim())) {
                    continue;
                }
                final List<String> values = entry.getValue();
                if (values != null) {
                    copy.put(key, Collections.unmodifiableList(new ArrayList<>(values)));
                }
            }
        }
        copy.put(
                RetryAfterParser.RETRY_AFTER_HEADER,
                Collections.singletonList(String.valueOf(Math.max(1L, retryAfterSeconds)))
        );
        return Collections.unmodifiableMap(copy);
    }

    @NonNull
    private static HashMap<String, List<String>> copyHeadersToHashMap(
            @NonNull final Map<String, List<String>> source) {
        final HashMap<String, List<String>> result = new HashMap<>(source.size());
        for (final Map.Entry<String, List<String>> entry : source.entrySet()) {
            result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
        }
        return result;
    }
}
