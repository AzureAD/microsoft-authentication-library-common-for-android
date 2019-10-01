package com.microsoft.identity.common.internal.servertelemetry;

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

    RequestTelemetry(boolean isCurrentRequest) {
        this(Schema.Value.SCHEMA_VERSION, isCurrentRequest);
    }

    RequestTelemetry(String schemaVersion, boolean isCurrentRequest) {
        mIsCurrentRequest = isCurrentRequest;
        mSchemaVersion = schemaVersion;
        mCommonTelemetry = new ConcurrentHashMap<>();
        mPlatformTelemetry = new ConcurrentHashMap<>();
    }

    private void putInCommonTelemetry(String key, String value) {
        // avoid overwrites
        if (!mCommonTelemetry.containsKey(key)) {
            mCommonTelemetry.put(key, value);
        }
    }

    private void putInPlatformTelemetry(String key, String value) {
        // avoid overwrites
        if (!mPlatformTelemetry.containsKey(key)) {
            mPlatformTelemetry.put(key, value);
        }
    }

    void clearTelemetry() {
        mCommonTelemetry.clear();
        mPlatformTelemetry.clear();
    }

    void putTelemetry(String key, String value) {
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
    private String getTelemetryHeaderStringFromFields(String[] fields, Map<String, String> telemetry) {
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
