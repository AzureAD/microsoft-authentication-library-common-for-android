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

import edu.umd.cs.findbugs.annotations.Nullable;
import io.opentelemetry.api.trace.Span;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

/**
 * Model representing server telemetry data from the x-ms-clientdata response header
 * (/token responses) and the clientdata query parameter (/authorize redirect URLs).
 * Both use a pipe-delimited format: account_type|error|sub_error|caller_data_boundary|cloud_instance.
 * Contains server-side error codes, account type, cloud instance, and data boundary info.
 */
@Getter
@NoArgsConstructor(access = AccessLevel.PRIVATE)
@Accessors(prefix = "m")
public class ClientDataInfo {

    private static final String TAG = ClientDataInfo.class.getSimpleName();

    /** Maximum length for any individual field when emitting to a span. */
    private static final int MAX_FIELD_LENGTH = 256;

    /**
     * The name of the {@code clientdata} query parameter added to /authorize redirect URIs
     * by eSTS/MSA when {@code clidata=1} is included in the authorization request.
     * Use this constant everywhere the parameter name is referenced to avoid typos.
     */
    public static final String CLIENTDATA_QUERY_PARAMETER = "clientdata";

    /** Account type value for MSA accounts. */
    private static final String ACCOUNT_TYPE_MSA_RAW = "m";

    /** Account type value for AAD accounts. */
    private static final String ACCOUNT_TYPE_AAD_RAW = "e";

    /** Display value for MSA account type. */
    private static final String ACCOUNT_TYPE_MSA = "MSA";

    /** Display value for AAD account type. */
    private static final String ACCOUNT_TYPE_AAD = "AAD";

    /**
     * Positional index for account_type in the pipe-delimited format:
     * account_type|error|sub_error|caller_data_boundary|cloud_instance
     */
    private static final int PIPE_INDEX_ACCOUNT_TYPE = 0;
    private static final int PIPE_INDEX_ERROR = 1;
    private static final int PIPE_INDEX_SUB_ERROR = 2;
    private static final int PIPE_INDEX_CALLER_DATA_BOUNDARY = 3;
    private static final int PIPE_INDEX_CLOUD_INSTANCE = 4;
    private static final int PIPE_MIN_SEGMENTS = 3;

    private String mError;
    private String mSubError;
    private String mAccountType;
    private String mCloudInstance;
    private String mCallerDataBoundary;

    /**
     * The original raw pipe-delimited string this instance was parsed from.
     * Always set when the instance was constructed via {@link #fromPipeDelimited(String)},
     * which is the only public entry point. Exposed for partner teams that want to
     * inspect or forward the unparsed payload.
     */
    private String mRaw;

    /**
     * Parses an already-decoded pipe-delimited clientdata query parameter value.
     * The caller is responsible for URL-decoding before passing (e.g. values from
     * {@link com.microsoft.identity.common.java.util.UrlUtil#getParameters} are
     * already decoded). Decoding twice would corrupt values containing '+' or '%'.
     * Positional format: account_type|error|sub_error|caller_data_boundary|cloud_instance.
     * Requires at least 3 segments.
     *
     * @param decodedValue already-decoded pipe-delimited string, may be null.
     * @return parsed {@link ClientDataInfo}, or null on failure/empty input.
     */
    @Nullable
    public static ClientDataInfo fromPipeDelimited(@Nullable final String decodedValue) {
        if (StringUtil.isNullOrEmpty(decodedValue)) {
            return null;
        }
        try {
            final String[] segments = decodedValue.split("\\|", -1);

            if (segments.length < PIPE_MIN_SEGMENTS) {
                Logger.warn(TAG, "clientdata pipe-delimited value has fewer than " + PIPE_MIN_SEGMENTS + " segments.");
                return null;
            }

            final ClientDataInfo info = new ClientDataInfo();
            info.mRaw = decodedValue;
            info.mAccountType = emptyToNull(segments[PIPE_INDEX_ACCOUNT_TYPE]);
            info.mError = emptyToNull(segments[PIPE_INDEX_ERROR]);
            info.mSubError = emptyToNull(segments[PIPE_INDEX_SUB_ERROR]);
            info.mCallerDataBoundary = segments.length > PIPE_INDEX_CALLER_DATA_BOUNDARY
                    ? emptyToNull(segments[PIPE_INDEX_CALLER_DATA_BOUNDARY]) : null;
            info.mCloudInstance = segments.length > PIPE_INDEX_CLOUD_INSTANCE
                    ? emptyToNull(segments[PIPE_INDEX_CLOUD_INSTANCE]) : null;
            return info;
        } catch (final Exception e) {
            Logger.warn(TAG, "Failed to parse clientdata pipe-delimited value: " + e.getMessage());
            // Emit that we failed to parse the clientdata value
            final Span span = SpanExtension.current();
            span.setAttribute(AttributeName.server_error.name(), "msal_android_parsing_failed");
            return null;
        }
    }

    /**
     * Sets each non-null field as a span attribute on the current span via {@link SpanExtension}.
     * account_type values are mapped: "m" -> "MSA", "e" -> "AAD".
     * Each field is truncated to {@value #MAX_FIELD_LENGTH} characters.
     */
    public void emitToSpan() {
        final Span span = SpanExtension.current();
        if (mError != null) {
            span.setAttribute(AttributeName.server_error.name(), truncate(mError));
        }
        if (mSubError != null) {
            span.setAttribute(AttributeName.server_sub_error.name(), truncate(mSubError));
        }
        if (mAccountType != null) {
            // account_type is an existing AttributeName; reuse it (m -> MSA, e -> AAD).
            final String mappedAccountType = mapAccountType(mAccountType);
            span.setAttribute(AttributeName.account_type.name(), truncate(mappedAccountType));
        }
        if (mCloudInstance != null) {
            span.setAttribute(AttributeName.server_cloud_instance.name(), truncate(mCloudInstance));
        }
        if (mCallerDataBoundary != null) {
            span.setAttribute(AttributeName.server_caller_data_boundary.name(), truncate(mCallerDataBoundary));
        }
    }

    private static String mapAccountType(final String raw) {
        if (ACCOUNT_TYPE_MSA_RAW.equalsIgnoreCase(raw)) {
            return ACCOUNT_TYPE_MSA;
        } else if (ACCOUNT_TYPE_AAD_RAW.equalsIgnoreCase(raw)) {
            return ACCOUNT_TYPE_AAD;
        }
        Logger.warn(TAG, "Unknown account_type value in clientdata; emitting as UNKNOWN.");
        return "UNKNOWN";
    }

    @Nullable
    private static String truncate(@Nullable final String value) {
        if (value == null || value.length() <= MAX_FIELD_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_FIELD_LENGTH);
    }

    @Nullable
    private static String emptyToNull(final String value) {
        return StringUtil.isNullOrEmpty(value) ? null : value;
    }
}
