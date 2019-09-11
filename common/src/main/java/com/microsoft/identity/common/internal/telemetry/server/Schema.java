package com.microsoft.identity.common.internal.telemetry.server;

public class Schema {

    public static final class Key {
        public static final String SCHEMA_VERSION = "schema_version";
        public static final String API_ID = "api_id";
        public static final String SCENARIO_ID = "scenario_id";
        public static final String TELEMETRY_ENABLED = "telemetry_enabled";
        public static final String FORCE_REFRESH = "force_refresh";
        public static final String LOGGING_ENABLED = "logging_enabled";
        public static final String CORRELATION_ID = "correlation_id";
        public static final String ERROR_CODE = "error_code";
        public static final String ERROR_COUNT = "error_count";
    }

    public static final class Value {
        public static final String SCHEMA_VERSION = "1";
    }



}
