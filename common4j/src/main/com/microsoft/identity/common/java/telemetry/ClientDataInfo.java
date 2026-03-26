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
import java.nio.charset.Charset;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;

/**
 * Model class representing telemetry data from the x-ms-clientdata HTTP response header
 * (returned by the /token endpoint) and the clientdata query parameter (from /authorize
 * redirect URLs).
 *
 * <p>Two parsers are provided:
 * <ul>
 *   <li>{@link #fromJson(String)} – for the URL-encoded JSON format used by /token</li>
 *   <li>{@link #fromPipeDelimited(String)} – for the pipe-delimited format used by /authorize</li>
 * </ul>
 *
 * <p>Call {@link #emitToSpan()} to emit all non-null fields as OpenTelemetry span attributes
 * on the current span.
 */
@Getter
@Setter
@Accessors(prefix = "m")
public class ClientDataInfo {

    private static final String TAG = ClientDataInfo.class.getSimpleName();

    /** Maximum length (in characters) to which each field is truncated before emission. */
    private static final int MAX_FIELD_LENGTH = 256;

    // JSON keys used in the /token response header value.
    private static final String JSON_KEY_ERROR = "Error";
    private static final String JSON_KEY_SUB_ERROR = "SubError";
    private static final String JSON_KEY_ACCOUNT_TYPE = "AccountType";
    private static final String JSON_KEY_CLOUD_INSTANCE = "cloud_instance";
    private static final String JSON_KEY_CALLER_DATA_BOUNDARY = "caller_data_boundary";

    // Positional indices for the pipe-delimited /authorize redirect value.
    private static final int PIPE_INDEX_ACCOUNT_TYPE = 0;
    private static final int PIPE_INDEX_ERROR = 1;
    private static final int PIPE_INDEX_SUB_ERROR = 2;
    private static final int PIPE_INDEX_CALLER_DATA_BOUNDARY = 3;
    private static final int PIPE_INDEX_CLOUD_INSTANCE = 4;
    private static final int PIPE_MIN_SEGMENTS = 3;

    // Account-type mappings: server value -> human-readable label.
    private static final String ACCOUNT_TYPE_MSA_RAW = "m";
    private static final String ACCOUNT_TYPE_AAD_RAW = "e";
    private static final String ACCOUNT_TYPE_MSA = "MSA";
    private static final String ACCOUNT_TYPE_AAD = "AAD";

    private String mError;
    private String mSubError;
    private String mAccountType;
    private String mCloudInstance;
    private String mCallerDataBoundary;

    /**
     * Parses a URL-encoded JSON string (as returned in the x-ms-clientdata header) into a
     * {@link ClientDataInfo} instance.
     *
     * <p>The expected JSON keys are: {@code Error}, {@code SubError}, {@code AccountType},
     * {@code cloud_instance}, {@code caller_data_boundary}.
     *
     * @param urlEncodedJson URL-encoded JSON string, or {@code null} / empty.
     * @return A populated {@link ClientDataInfo}, or {@code null} on failure.
     */
    @Nullable
    public static ClientDataInfo fromJson(@Nullable final String urlEncodedJson) {
        if (StringUtil.isNullOrEmpty(urlEncodedJson)) {
            return null;
        }

        try {
            final String decoded = URLDecoder.decode(urlEncodedJson, "UTF-8");
            final JSONObject json = new JSONObject(decoded);

            final ClientDataInfo info = new ClientDataInfo();
            info.mError = nullIfEmpty(json.optString(JSON_KEY_ERROR, null));
            info.mSubError = nullIfEmpty(json.optString(JSON_KEY_SUB_ERROR, null));
            info.mAccountType = nullIfEmpty(json.optString(JSON_KEY_ACCOUNT_TYPE, null));
            info.mCloudInstance = nullIfEmpty(json.optString(JSON_KEY_CLOUD_INSTANCE, null));
            info.mCallerDataBoundary = nullIfEmpty(json.optString(JSON_KEY_CALLER_DATA_BOUNDARY, null));
            return info;
        } catch (final JSONException | UnsupportedEncodingException e) {
            Logger.warn(TAG, "Failed to parse x-ms-clientdata JSON value: " + e.getMessage());
            return null;
        }
    }

    /**
     * Parses a URL-encoded pipe-delimited string (as returned in the clientdata query parameter
     * of /authorize redirect URLs) into a {@link ClientDataInfo} instance.
     *
     * <p>The positional format is:
     * {@code account_type|error|sub_error[|caller_data_boundary[|cloud_instance]]}
     *
     * <p>At least 3 segments are required.
     *
     * @param urlEncodedValue URL-encoded pipe-delimited value, or {@code null} / empty.
     * @return A populated {@link ClientDataInfo}, or {@code null} on failure.
     */
    @Nullable
    public static ClientDataInfo fromPipeDelimited(@Nullable final String urlEncodedValue) {
        if (StringUtil.isNullOrEmpty(urlEncodedValue)) {
            return null;
        }

        try {
            final String decoded = URLDecoder.decode(urlEncodedValue, "UTF-8");
            final String[] segments = decoded.split("\\|", -1);

            if (segments.length < PIPE_MIN_SEGMENTS) {
                Logger.warn(TAG, "clientdata pipe-delimited value has fewer than " + PIPE_MIN_SEGMENTS + " segments.");
                return null;
            }

            final ClientDataInfo info = new ClientDataInfo();
            info.mAccountType = nullIfEmpty(segments[PIPE_INDEX_ACCOUNT_TYPE]);
            info.mError = nullIfEmpty(segments[PIPE_INDEX_ERROR]);
            info.mSubError = nullIfEmpty(segments[PIPE_INDEX_SUB_ERROR]);

            if (segments.length > PIPE_INDEX_CALLER_DATA_BOUNDARY) {
                info.mCallerDataBoundary = nullIfEmpty(segments[PIPE_INDEX_CALLER_DATA_BOUNDARY]);
            }
            if (segments.length > PIPE_INDEX_CLOUD_INSTANCE) {
                info.mCloudInstance = nullIfEmpty(segments[PIPE_INDEX_CLOUD_INSTANCE]);
            }

            return info;
        } catch (final UnsupportedEncodingException e) {
            Logger.warn(TAG, "Failed to decode clientdata pipe-delimited value: " + e.getMessage());
            return null;
        }
    }

    /**
     * Emits all non-null fields as attributes on the current OpenTelemetry span.
     *
     * <p>Each field is truncated to at most {@value #MAX_FIELD_LENGTH} characters before
     * emission. The account_type field is mapped: {@code "m"} → {@code "MSA"},
     * {@code "e"} → {@code "AAD"}.
     */
    public void emitToSpan() {
        if (mError != null) {
            SpanExtension.current().setAttribute(
                    AttributeName.server_error.name(), truncate(mError));
        }
        if (mSubError != null) {
            SpanExtension.current().setAttribute(
                    AttributeName.server_sub_error.name(), truncate(mSubError));
        }
        if (mAccountType != null) {
            final String mappedAccountType = mapAccountType(mAccountType);
            SpanExtension.current().setAttribute(
                    AttributeName.account_type.name(), truncate(mappedAccountType));
        }
        if (mCloudInstance != null) {
            SpanExtension.current().setAttribute(
                    AttributeName.server_cloud_instance.name(), truncate(mCloudInstance));
        }
        if (mCallerDataBoundary != null) {
            SpanExtension.current().setAttribute(
                    AttributeName.server_caller_data_boundary.name(), truncate(mCallerDataBoundary));
        }
    }

    /**
     * Maps raw account-type tokens from the server to a human-readable label.
     *
     * @param rawAccountType the raw value from the server (e.g. {@code "m"} or {@code "e"}).
     * @return {@code "MSA"}, {@code "AAD"}, or the original value if unrecognized.
     */
    private static String mapAccountType(@Nullable final String rawAccountType) {
        if (ACCOUNT_TYPE_MSA_RAW.equals(rawAccountType)) {
            return ACCOUNT_TYPE_MSA;
        } else if (ACCOUNT_TYPE_AAD_RAW.equals(rawAccountType)) {
            return ACCOUNT_TYPE_AAD;
        }
        return rawAccountType;
    }

    /**
     * Truncates the given string to {@value #MAX_FIELD_LENGTH} characters if necessary.
     *
     * @param value the string to truncate.
     * @return the original string, or a truncated version of it.
     */
    private static String truncate(@Nullable final String value) {
        if (value == null || value.length() <= MAX_FIELD_LENGTH) {
            return value;
        }
        return value.substring(0, MAX_FIELD_LENGTH);
    }

    /**
     * Returns {@code null} if the given string is null or empty; otherwise returns the string.
     */
    @Nullable
    private static String nullIfEmpty(@Nullable final String value) {
        return StringUtil.isNullOrEmpty(value) ? null : value;
    }
}
