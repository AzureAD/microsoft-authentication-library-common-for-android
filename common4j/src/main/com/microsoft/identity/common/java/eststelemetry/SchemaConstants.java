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
package com.microsoft.identity.common.java.eststelemetry;

import com.microsoft.identity.common.java.telemetry.TelemetryEventStrings;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * This class defines the schema for server-side telemetry
 */
public class SchemaConstants {

    public static final String SCHEMA_VERSION_KEY = "schema_version";
    public static final String CURRENT_SCHEMA_VERSION = "2";

    public static final String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    public static final String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

    public static final String SEPARATOR_PIPE = "|";
    public static final String SEPARATOR_COMMA = ",";

    public static final int HEADER_DATA_LIMIT = 3800;

    public static final class Key {
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
        public static final String ALL_TELEMETRY_DATA_SENT = "is_all_telemetry_data_sent";

        // flw and multiple reg fields
        public static final String IS_SHARED_DEVICE = "isSharedScenario";
        public static final String REG_TYPE = "reg_type";
        public static final String REG_SOURCE = "reg_source";
        public static final String FLW_SIGNOUT_APP = "flw_signout_app";
        public static final String FLW_SIGNIN_APP = "flw_signin_app";
        public static final String REG_NUM = "reg_num";
        public static final String CLOUD_NUM = "cloud_num";
        public static final String REG_SEQ_NUM = "reg_seq_num";
        public static final String REQ_PURPOSE = "req_purpose";
    }

    public static final class Value {
        public static final String TRUE = "1";
        public static final String FALSE = "0";
        public static final String EMPTY = "";
    }


    /**
     * This array defines the platform schema for current request
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final String[] currentRequestPlatformFields = new String[]{
            // shared fields between Android and iOS
            Key.IS_SHARED_DEVICE,
            // Multiple WPJ shared fields between Android and iOS
            Key.REG_NUM,
            Key.CLOUD_NUM,
            Key.REG_SEQ_NUM,
            Key.REQ_PURPOSE,
            // flw shared fields between Android and iOS
            Key.REG_TYPE,
            Key.REG_SOURCE,
            Key.FLW_SIGNOUT_APP,
            Key.FLW_SIGNIN_APP,
            // Android custom platform fields
            SchemaConstants.Key.ACCOUNT_STATUS,
            SchemaConstants.Key.ID_TOKEN_STATUS,
            SchemaConstants.Key.AT_STATUS,
            SchemaConstants.Key.RT_STATUS,
            SchemaConstants.Key.FRT_STATUS,
            SchemaConstants.Key.MRRT_STATUS,
    };

    /**
     * This array defines the platform schema for last request
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final String[] lastRequestPlatformFields = new String[]{
            SchemaConstants.Key.ALL_TELEMETRY_DATA_SENT
    };

    private static final String[] allowedFieldsForOfflineEmit = new String[]{
            Key.FLW_SIGNIN_APP,
            Key.FLW_SIGNOUT_APP
    };

    static boolean isCurrentPlatformField(final String key) {
        return Arrays.asList(currentRequestPlatformFields).contains(key);
    }

    static boolean isLastPlatformField(final String key) {
        return Arrays.asList(lastRequestPlatformFields).contains(key);
    }

    static boolean isOfflineEmitAllowedForThisField(final String key) {
        return Arrays.asList(allowedFieldsForOfflineEmit).contains(key);
    }

    static String[] getCurrentRequestPlatformFields() {
        return currentRequestPlatformFields;
    }

    static String[] getLastRequestPlatformFields() {
        return lastRequestPlatformFields;
    }
}
