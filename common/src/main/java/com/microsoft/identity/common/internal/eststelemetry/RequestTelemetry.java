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
package com.microsoft.identity.common.internal.eststelemetry;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class RequestTelemetry {

    private final static String TAG = RequestTelemetry.class.getSimpleName();

    private boolean mIsCurrentRequest;
    private String mSchemaVersion;
    private ConcurrentMap<String, String> mCommonTelemetry;
    private ConcurrentMap<String, String> mPlatformTelemetry;

    RequestTelemetry(@NonNull final boolean isCurrentRequest) {
        this(Schema.CURRENT_SCHEMA_VERSION, isCurrentRequest);
    }

    RequestTelemetry(@NonNull final String schemaVersion, @NonNull final boolean isCurrentRequest) {
        mIsCurrentRequest = isCurrentRequest;
        mSchemaVersion = schemaVersion;
        mCommonTelemetry = new ConcurrentHashMap<>();
        mPlatformTelemetry = new ConcurrentHashMap<>();
    }

    private void putInCommonTelemetry(final String key, final String value) {
        mCommonTelemetry.putIfAbsent(key, value);
    }

    private void putInPlatformTelemetry(final String key, final String value) {
        mPlatformTelemetry.putIfAbsent(key, value);
    }

    void clearTelemetry() {
        mCommonTelemetry.clear();
        mPlatformTelemetry.clear();
    }

    void putTelemetry(@Nullable final String key, @Nullable final String value) {
        if (key == null) {
            return;
        }

        final String methodName = ":putTelemetry";
        final String schemaCompliantValueString = Schema.getSchemaCompliantString(value);

        if (Schema.isCommonField(key, mIsCurrentRequest)) {
            putInCommonTelemetry(key, schemaCompliantValueString);
        } else if (Schema.isPlatformField(key, mIsCurrentRequest)) {
            putInPlatformTelemetry(key, schemaCompliantValueString);
        } else {
            Logger.verbose(
                    TAG + methodName,
                    "Supplied key not added to Server telemetry map " +
                            "as it is not part of either common or platform schema."
            );
        }
    }

    String getSchemaVersion() {
        return mSchemaVersion;
    }

    Map<String, String> getCommonTelemetry() {
        return mCommonTelemetry;
    }

    Map<String, String> getPlatformTelemetry() {
        return mPlatformTelemetry;
    }

    @Nullable
    String getCompleteTelemetryHeaderString() {
        final String methodName = ":getCompleteTelemetryHeaderString";

        if (StringUtil.isEmpty(mSchemaVersion)) {
            Logger.verbose(
                    TAG + methodName,
                    "SCHEMA_VERSION is null or empty. " +
                            "Telemetry Header String cannot be formed."
            );

            return null;
        }

        final String schemaVersionString = Schema.getSchemaCompliantString(mSchemaVersion);
        final String commonSchemaString = getCommonTelemetryHeaderString();
        final String platformSchemaString = getPlatformTelemetryHeaderString();
        return schemaVersionString + "|" + commonSchemaString + "|" + platformSchemaString;
    }

    private String getCommonTelemetryHeaderString() {
        final String[] commonFields = Schema.getCommonFields(mIsCurrentRequest);
        return getTelemetryHeaderStringFromFields(commonFields, mCommonTelemetry);
    }

    private String getPlatformTelemetryHeaderString() {
        final String[] platformFields = Schema.getPlatformFields(mIsCurrentRequest);
        return getTelemetryHeaderStringFromFields(platformFields, mPlatformTelemetry);
    }

    /**
     * This method loops over provided telemetry fields and creates a header string for those fields.
     * It is important to ensure that the fields array passed to this method is in the correct order,
     * as determined in {@link Schema}.
     * Failure to do so will return a malformed header string.
     *
     * @param fields    The fields that need to be included in the header string
     * @param telemetry A HashMap of telemetry data that maps keys (fields) to their values
     * @return a telemetry header string composed from provided telemetry fields and values
     */
    @NonNull
    private String getTelemetryHeaderStringFromFields(@Nullable final String[] fields, @Nullable final Map<String, String> telemetry) {
        if (fields == null || telemetry == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            final String key = fields[i];
            final String value = telemetry.get(key);
            final String compliantValueString = Schema.getSchemaCompliantString(value);
            sb.append(compliantValueString);
            if (i != fields.length - 1) {
                sb.append(',');
            }
        }

        return sb.toString();
    }
}
