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

import com.google.gson.annotations.SerializedName;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Collection;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public abstract class RequestTelemetry implements IRequestTelemetry {

    private final static String TAG = RequestTelemetry.class.getSimpleName();

    @SerializedName(Schema.SCHEMA_VERSION_KEY)
    String mSchemaVersion;

    @SerializedName("platform_telemetry")
    ConcurrentMap<String, String> mPlatformTelemetry;

    RequestTelemetry(@NonNull final String schemaVersion) {
        mSchemaVersion = schemaVersion;
        mPlatformTelemetry = new ConcurrentHashMap<>();
    }

    private void putInPlatformTelemetry(final String key, final String value) {
        mPlatformTelemetry.putIfAbsent(key, value);
    }

    void clearTelemetry() {
        mPlatformTelemetry.clear();
    }

    void putTelemetry(@Nullable final String key, @Nullable final String value) {
        if (key == null) {
            return;
        }

        final String methodName = ":putTelemetry";
        final String schemaCompliantValueString = Schema.getSchemaCompliantStringFromString(value);

        final boolean isCurrentRequest = this instanceof CurrentRequestTelemetry;

        if (Schema.isCommonField(key, isCurrentRequest)) {
            putInCommonTelemetry(key, schemaCompliantValueString);
        } else if (Schema.isPlatformField(key, isCurrentRequest)) {
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

    Map<String, String> getPlatformTelemetry() {
        return mPlatformTelemetry;
    }

    String getPlatformTelemetryHeaderString() {
        final String[] platformFields;

        if (this instanceof CurrentRequestTelemetry) {
            platformFields = Schema.getCurrentRequestPlatformFields();
        } else {
            platformFields = Schema.getLastRequestPlatformFields();
        }

        return getHeaderStringForFields(platformFields, mPlatformTelemetry);
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
    String getHeaderStringForFields(@Nullable final String[] fields, @Nullable final Map<String, String> telemetry) {
        if (fields == null || telemetry == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            final String key = fields[i];
            final String value = telemetry.get(key);
            final String compliantValueString = Schema.getSchemaCompliantStringFromString(value);
            sb.append(compliantValueString);
            if (i != fields.length - 1) {
                sb.append(',');
            }
        }

        return sb.toString();
    }

    String getHeaderStringForCollection(Collection fields) {
        if (fields == null) {
            return "";
        }

        return getHeaderStringForArray(fields.toArray());
    }

    private String getHeaderStringForArray(Object[] fields) {
        if (fields == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < fields.length; i++) {
            final String val = Schema.getSchemaCompliantString(fields[i].toString());
            sb.append(val);
            if (i != fields.length - 1) {
                sb.append(',');
            }
        }

        return sb.toString();
    }

    @Override
    public String getCompleteHeaderString() {
        final String methodName = ":getCompleteHeaderString";
        if (StringUtil.isEmpty(mSchemaVersion)) {
            Logger.verbose(
                    TAG + methodName,
                    "SCHEMA_VERSION is null or empty. " +
                            "Telemetry Header String cannot be formed."
            );

            return null;
        }

        return mSchemaVersion + "|" + this.getHeaderStringForFields() + "|" + getPlatformTelemetryHeaderString();
    }

    abstract void putInCommonTelemetry(final String key, final String value);
}
