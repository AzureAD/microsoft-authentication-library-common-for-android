package com.microsoft.identity.common.internal.servertelemetry;

import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Arrays;

/**
 * This class defines the schema for server-side telemetry
 */
public class Schema {

    public static final String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    public static final String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

    public static final class Key {
        // new keys
        public static final String SCHEMA_VERSION = "schema_version";
        public static final String SCENARIO_ID = "scenario_id";
        public static final String TELEMETRY_ENABLED = "telemetry_enabled";
        public static final String LOGGING_ENABLED = "logging_enabled";

        //imported keys
        public static final String API_ID = TelemetryEventStrings.Key.API_ID;
        public static final String FORCE_REFRESH = TelemetryEventStrings.Key.IS_FORCE_REFRESH;
        public static final String CORRELATION_ID = TelemetryEventStrings.Key.CORRELATION_ID;
        public static final String ERROR_CODE = TelemetryEventStrings.Key.ERROR_CODE;
        public static final String ACCOUNT_STATUS = TelemetryEventStrings.Key.ACCOUNT_STATUS;
        public static final String ID_TOKEN_STATUS = TelemetryEventStrings.Key.ID_TOKEN_STATUS;
        public static final String AT_STATUS = TelemetryEventStrings.Key.AT_STATUS;
        public static final String RT_STATUS = TelemetryEventStrings.Key.RT_STATUS;
        public static final String FRT_STATUS = TelemetryEventStrings.Key.FRT_STATUS;
        public static final String MRRT_STATUS = TelemetryEventStrings.Key.MRRT_STATUS;
    }

    public static final class Value {
        public static final String SCHEMA_VERSION = "1";
        public static final String TRUE = "1";
        public static final String FALSE = "0";
        public static final String EMPTY = "";
    }

    /**
     * This array defines the common schema for current request.
     * NOTE: These fields must always be listed in the correct order in this array.
     *      Failure do so will break the schema.
     */
    private static final String[] currentRequestCommonFields = new String[]{
            Key.API_ID,
            Key.FORCE_REFRESH
    };

    /**
     * This array defines the platform schema for current request
     * NOTE: These fields must always be listed in the correct order in this array.
     *      Failure do so will break the schema.
     */
    private static final String[] currentRequestPlatformFields = new String[] {
            Key.ACCOUNT_STATUS,
            Key.ID_TOKEN_STATUS,
            Key.AT_STATUS,
            Key.RT_STATUS,
            Key.FRT_STATUS,
            Key.MRRT_STATUS
    };

    /**
     * This array defines the common schema for last request
     * NOTE: These fields must always be listed in the correct order in this array.
     *      Failure do so will break the schema.
     */
    private static final String[] lastRequestCommonFields = new String[]{
            Key.API_ID,
            Key.CORRELATION_ID,
            Key.ERROR_CODE
    };

    /**
     * This array defines the platform schema for last request
     * NOTE: These fields must always be listed in the correct order in this array.
     *      Failure do so will break the schema.
     */
    private static final String[] lastRequestPlatformFields = new String[] {

    };

    private static boolean isCurrentCommonField(String key) {
        return Arrays.asList(currentRequestCommonFields).contains(key);
    }

    private static boolean isLastCommonField(String key) {
        return Arrays.asList(lastRequestCommonFields).contains(key);
    }

    private static boolean isCurrentPlatformField(String key) {
        return Arrays.asList(currentRequestPlatformFields).contains(key);
    }

    private static boolean isLastPlatformField(String key) {
        return Arrays.asList(lastRequestPlatformFields).contains(key);
    }

    private static String[] getCurrentRequestCommonFields() {
        return currentRequestCommonFields;
    }

    private static String[] getCurrentRequestPlatformFields() {
        return currentRequestPlatformFields;
    }

    private static String[] getLastRequestCommonFields() {
        return lastRequestCommonFields;
    }

    private static String[] getLastRequestPlatformFields() {
        return lastRequestPlatformFields;
    }

    private static String[] getFieldsCopy(String[] fields) {
        return Arrays.copyOf(fields, fields.length);
    }

    static String[] getCommonFields(boolean isCurrent) {
        final String[] fields =  isCurrent ? getCurrentRequestCommonFields() : getLastRequestCommonFields();

        // returning a copy here so other classes cannot modify the schema
        return getFieldsCopy(fields);
    }

    static String[] getPlatformFields(boolean isCurrent) {
        final String[] fields =  isCurrent ? getCurrentRequestPlatformFields() : getLastRequestPlatformFields();

        // returning a copy here so other classes cannot modify the schema
        return getFieldsCopy(fields);
    }

    static boolean isCommonField(String key, boolean isCurrent) {
        return isCurrent ? isCurrentCommonField(key) : isLastCommonField(key);
    }

    static boolean isPlatformField(String key, boolean isCurrent) {
        return isCurrent ? isCurrentPlatformField(key) : isLastPlatformField(key);
    }

    static boolean isCurrentField(String key) {
        return isCurrentCommonField(key) || isCurrentPlatformField(key);
    }

    static boolean isLastField(String key) {
        return isLastCommonField(key) || isLastPlatformField(key);
    }

    static String getSchemaCompliantString(String s) {
        if (StringUtil.isEmpty(s)) {
            return Value.EMPTY;
        } else if (s.equals(TelemetryEventStrings.Value.TRUE)) {
            return Value.TRUE;
        } else if (s.equals(TelemetryEventStrings.Value.FALSE)) {
            return Value.FALSE;
        } else {
            return s;
        }
    }
}
