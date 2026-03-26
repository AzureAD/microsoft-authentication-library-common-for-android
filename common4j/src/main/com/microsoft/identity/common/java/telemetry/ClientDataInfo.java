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
package com.microsoft.identity.common.java.telemetry;

import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.util.StringUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;

import edu.umd.cs.findbugs.annotations.Nullable;
import io.opentelemetry.api.trace.Span;

/**
 * Represents the parsed content of the x-ms-clientdata response header (token endpoint)
 * or the clientdata query parameter (authorize redirect).
 *
 * <p>The token endpoint delivers a URL-encoded JSON payload; the authorize endpoint
 * delivers a pipe-delimited string. Both formats carry up to 5 fields:
 * <ol>
 *   <li>accountType – "m" (MSA) or "e" (AAD)</li>
 *   <li>error       – server error code</li>
 *   <li>subError    – server sub-error code</li>
 *   <li>speRing     – SPE ring identifier</li>
 *   <li>tokenAge    – refresh-token age</li>
 * </ol>
 *
 * <p>Field values longer than {@link #MAX_FIELD_LENGTH} characters are silently truncated.
 */
public class ClientDataInfo {

    private static final String TAG = ClientDataInfo.class.getSimpleName();

    /** Maximum allowed length for any single field value. */
    public static final int MAX_FIELD_LENGTH = 256;

    // JSON keys used in the token-endpoint payload.
    static final String JSON_KEY_ACCOUNT_TYPE = "at";
    static final String JSON_KEY_ERROR        = "e";
    static final String JSON_KEY_SUB_ERROR    = "se";
    static final String JSON_KEY_SPE_RING     = "sr";
    static final String JSON_KEY_TOKEN_AGE    = "ta";

    // Pipe-delimited segment indices (authorize redirect).
    static final int PIPE_IDX_ACCOUNT_TYPE = 0;
    static final int PIPE_IDX_ERROR        = 1;
    static final int PIPE_IDX_SUB_ERROR    = 2;
    static final int PIPE_IDX_SPE_RING     = 3;
    static final int PIPE_IDX_TOKEN_AGE    = 4;
    static final int PIPE_MIN_SEGMENTS     = 3;

    // Account type constants for mapping.
    private static final String ACCOUNT_TYPE_MSA = "m";
    private static final String ACCOUNT_TYPE_AAD = "e";
    private static final String ACCOUNT_TYPE_MSA_DISPLAY = "MSA";
    private static final String ACCOUNT_TYPE_AAD_DISPLAY = "AAD";

    private final String mAccountType;
    private final String mError;
    private final String mSubError;
    private final String mSpeRing;
    private final String mTokenAge;

    private ClientDataInfo(
            @Nullable final String accountType,
            @Nullable final String error,
            @Nullable final String subError,
            @Nullable final String speRing,
            @Nullable final String tokenAge) {
        mAccountType = truncate(accountType);
        mError       = truncate(error);
        mSubError    = truncate(subError);
        mSpeRing     = truncate(speRing);
        mTokenAge    = truncate(tokenAge);
    }

    // Getters -----------------------------------------------------------------

    @Nullable
    public String getAccountType() {
        return mAccountType;
    }

    @Nullable
    public String getError() {
        return mError;
    }

    @Nullable
    public String getSubError() {
        return mSubError;
    }

    @Nullable
    public String getSpeRing() {
        return mSpeRing;
    }

    @Nullable
    public String getTokenAge() {
        return mTokenAge;
    }

    // Parsers -----------------------------------------------------------------

    /**
     * Parses a URL-encoded JSON string returned in the x-ms-clientdata token-endpoint header.
     *
     * @param urlEncodedJson URL-encoded JSON payload, e.g.
     *                       {@code %7B%22at%22%3A%22m%22%2C%22e%22%3A%220%22%7D}.
     * @return a populated {@link ClientDataInfo}, or {@code null} if the input is absent,
     *         empty, or malformed.
     */
    @Nullable
    public static ClientDataInfo fromJson(@Nullable final String urlEncodedJson) {
        if (StringUtil.isNullOrEmpty(urlEncodedJson)) {
            return null;
        }

        final String decoded;
        try {
            decoded = URLDecoder.decode(urlEncodedJson, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            // UTF-8 is always available; should never happen.
            Logger.warn(TAG, "UTF-8 not supported while decoding clientdata JSON.");
            return null;
        }

        try {
            final JSONObject json = new JSONObject(decoded);
            return new ClientDataInfo(
                    optString(json, JSON_KEY_ACCOUNT_TYPE),
                    optString(json, JSON_KEY_ERROR),
                    optString(json, JSON_KEY_SUB_ERROR),
                    optString(json, JSON_KEY_SPE_RING),
                    optString(json, JSON_KEY_TOKEN_AGE)
            );
        } catch (final JSONException e) {
            Logger.warn(TAG, "Failed to parse clientdata JSON: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses the pipe-delimited string returned in the clientdata authorize redirect parameter.
     *
     * <p>Expected format: {@code accountType|error|subError[|speRing[|tokenAge]]}
     * At least {@value #PIPE_MIN_SEGMENTS} segments are required; fewer segments cause
     * this method to return {@code null}.
     *
     * @param value the raw pipe-delimited value.
     * @return a populated {@link ClientDataInfo}, or {@code null} if the input is absent,
     *         empty, or contains fewer than {@value #PIPE_MIN_SEGMENTS} segments.
     */
    @Nullable
    public static ClientDataInfo fromPipeDelimited(@Nullable final String value) {
        if (StringUtil.isNullOrEmpty(value)) {
            return null;
        }

        final String[] parts = value.split("\\|", -1);
        if (parts.length < PIPE_MIN_SEGMENTS) {
            Logger.warn(TAG, "clientdata pipe-delimited value has fewer than " + PIPE_MIN_SEGMENTS + " segments.");
            return null;
        }

        return new ClientDataInfo(
                emptyToNull(parts[PIPE_IDX_ACCOUNT_TYPE]),
                emptyToNull(parts[PIPE_IDX_ERROR]),
                emptyToNull(parts[PIPE_IDX_SUB_ERROR]),
                parts.length > PIPE_IDX_SPE_RING ? emptyToNull(parts[PIPE_IDX_SPE_RING]) : null,
                parts.length > PIPE_IDX_TOKEN_AGE ? emptyToNull(parts[PIPE_IDX_TOKEN_AGE]) : null
        );
    }

    // Telemetry ---------------------------------------------------------------

    /**
     * Emits all non-null fields as attributes on the current OTel span.
     * The account type is mapped to a human-readable value ("MSA" or "AAD") before emission.
     */
    public void emitToSpan() {
        emitToSpan(SpanExtension.current());
    }

    /**
     * Emits all non-null fields as attributes on the provided {@code span}.
     * The account type is mapped to a human-readable value ("MSA" or "AAD") before emission.
     *
     * @param span the target {@link Span}.
     */
    public void emitToSpan(@Nullable final Span span) {
        if (span == null) {
            return;
        }

        if (!StringUtil.isNullOrEmpty(mAccountType)) {
            span.setAttribute(AttributeName.account_type.name(), mapAccountType(mAccountType));
        }
        if (!StringUtil.isNullOrEmpty(mError)) {
            span.setAttribute(AttributeName.server_client_data_error.name(), mError);
        }
        if (!StringUtil.isNullOrEmpty(mSubError)) {
            span.setAttribute(AttributeName.server_client_data_sub_error.name(), mSubError);
        }
        if (!StringUtil.isNullOrEmpty(mSpeRing)) {
            span.setAttribute(AttributeName.server_client_data_spe_ring.name(), mSpeRing);
        }
        if (!StringUtil.isNullOrEmpty(mTokenAge)) {
            span.setAttribute(AttributeName.server_client_data_token_age.name(), mTokenAge);
        }
    }

    // Helpers -----------------------------------------------------------------

    private static String mapAccountType(@Nullable final String raw) {
        if (ACCOUNT_TYPE_MSA.equalsIgnoreCase(raw)) {
            return ACCOUNT_TYPE_MSA_DISPLAY;
        } else if (ACCOUNT_TYPE_AAD.equalsIgnoreCase(raw)) {
            return ACCOUNT_TYPE_AAD_DISPLAY;
        }
        return raw;
    }

    @Nullable
    private static String truncate(@Nullable final String value) {
        if (value == null) {
            return null;
        }
        return value.length() > MAX_FIELD_LENGTH ? value.substring(0, MAX_FIELD_LENGTH) : value;
    }

    @Nullable
    private static String emptyToNull(@Nullable final String value) {
        return StringUtil.isNullOrEmpty(value) ? null : value;
    }

    @Nullable
    private static String optString(@Nullable final JSONObject json, @Nullable final String key) {
        if (json == null || key == null || !json.has(key)) {
            return null;
        }
        final String value = json.optString(key, null);
        return StringUtil.isNullOrEmpty(value) ? null : value;
    }
}
