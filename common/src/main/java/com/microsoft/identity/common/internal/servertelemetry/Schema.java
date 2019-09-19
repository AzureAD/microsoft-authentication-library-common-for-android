package com.microsoft.identity.common.internal.servertelemetry;

import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Arrays;

public class Schema {

    public static final String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    public static final String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

    public static final class Key {
        public static final String SCHEMA_VERSION = "schema_version";
        public static final String API_ID = "api_id";
        public static final String SCENARIO_ID = "scenario_id";
        public static final String TELEMETRY_ENABLED = "telemetry_enabled";
        public static final String FORCE_REFRESH = "force_refresh";
        public static final String LOGGING_ENABLED = "logging_enabled";
        public static final String CORRELATION_ID = "correlation_id";
        public static final String ERROR_CODE = "error_code";
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
            Key.FORCE_REFRESH,

    };

    /**
     * This array defines the platform schema for current request
     * NOTE: These fields must always be listed in the correct order in this array.
     *      Failure do so will break the schema.
     */
    private static final String[] currentRequestPlatformFields = new String[] {

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

    private static String getSchemaCompliantStringFromBoolean(boolean value) {
        return value ? Schema.Value.TRUE : Schema.Value.FALSE;
    }

    private static String getSchemaCompliantStringFromString(String value) {
        return StringUtil.isEmpty(value) ? Schema.Value.EMPTY : value;
    }

    static String getSchemaCompliantString(Object obj) {
        if (obj == null) {
            return Schema.Value.EMPTY;
        } else if (obj instanceof String) {
            return getSchemaCompliantStringFromString((String) obj);
        } else if (obj instanceof Boolean) {
            return getSchemaCompliantStringFromBoolean(((Boolean) obj).booleanValue());
        } else {
            return obj.toString();
        }
    }
}
