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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * This class defines the schema for server-side telemetry
 */
public class SchemaConstants {

    public static final String SCHEMA_VERSION_KEY = "schema_version";
    public static final String CURRENT_SCHEMA_VERSION = "2";

    // starting from two, as we were sending platform telemetry prior to it being versioned
    public static final String CURRENT_PLATFORM_SCHEMA_VERSION = "2";

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

        public static final String PLATFORM_SCHEMA_VERSION = "platform_schema_version";

        // flw and multiple reg fields
        // More details here:
        // https://dev.azure.com/IdentityDivision/DevEx/_git/AuthLibrariesApiReview/pullrequest/5168
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
     * This array defines the "Android only" platform fields for current request.
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final List<String> currentRequestAndroidPlatformFields = Arrays.asList(
            // Android custom platform fields
            SchemaConstants.Key.ACCOUNT_STATUS,
            SchemaConstants.Key.ID_TOKEN_STATUS,
            SchemaConstants.Key.AT_STATUS,
            SchemaConstants.Key.RT_STATUS,
            SchemaConstants.Key.FRT_STATUS,
            SchemaConstants.Key.MRRT_STATUS
    );

    /**
     * This array defines the "Android and iOS shared" platform fields for current request in the
     * FLW scenario.
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final List<String> currentRequestSharedFlwPlatformFieldsForAndroidAndiOSBroker = Arrays.asList(
            // flw shared fields between Android and iOS
            Key.IS_SHARED_DEVICE,
            Key.REG_TYPE,
            Key.REG_SOURCE,
            Key.FLW_SIGNOUT_APP,
            Key.FLW_SIGNIN_APP
    );

    /**
     * This array defines the "Android and iOS shared" platform fields for current request in the
     * Multiple Registration scenario.
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final List<String> currentRequestSharedMultipleWpjPlatformFieldsForAndroidAndiOSBroker = Arrays.asList(
            // Multiple WPJ shared fields between Android and iOS
            Key.IS_SHARED_DEVICE,
            Key.REG_NUM,
            Key.CLOUD_NUM,
            Key.REG_SEQ_NUM,
            Key.REQ_PURPOSE,
            Key.REG_SOURCE
    );

    /**
     * This array defines the platform schema for last request
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final List<String> lastRequestPlatformFields = Arrays.asList(
            Key.PLATFORM_SCHEMA_VERSION,
            SchemaConstants.Key.ALL_TELEMETRY_DATA_SENT
    );

    /**
     * This array defines fields for which emitting is allowed outside of a DiagnosticContext.
     * We have lots of code that is executed outside of a DiagnosticContext i.e.
     * ThreadLocal is not populated with a correlation Id. Since the current telemetry design is
     * strictly around DiagnosticContext, therefore we need this supplemental cache to capture these
     * fields that are emitted in code that is running outside that context.
     */
    private static final List<String> allowedFieldsForOfflineEmit = Arrays.asList(
            Key.FLW_SIGNIN_APP,
            Key.FLW_SIGNOUT_APP
    );

    /**
     * Indicates if the supplied field is part of the platform field schema for current request.
     *
     * @param key the key that needs to be checked
     * @return a boolean indicating if the field if part of current request platform schema
     */
    static boolean isCurrentPlatformField(final String key) {
        return currentRequestAndroidPlatformFields.contains(key) ||
                currentRequestSharedFlwPlatformFieldsForAndroidAndiOSBroker.contains(key) ||
                currentRequestSharedMultipleWpjPlatformFieldsForAndroidAndiOSBroker.contains(key);
    }

    /**
     * Indicates if the supplied field is part of the platform field schema for last request.
     *
     * @param key the key that needs to be checked
     * @return a boolean indicating if the field if part of last request platform schema
     */
    static boolean isLastPlatformField(final String key) {
        return lastRequestPlatformFields.contains(key);
    }

    /**
     * Indicates if this field is allowed to be emitted outside of a DiagnosticContext.
     * We have lots of code that is executed outside of a DiagnosticContext i.e.
     * ThreadLocal is not populated with a correlation Id. Since the current telemetry design is
     * strictly around DiagnosticContext, therefore we need this supplemental cache to capture these
     * fields that are emitted in code that is running outside that context.
     */
    static boolean isOfflineEmitAllowedForThisField(final String key) {
        return allowedFieldsForOfflineEmit.contains(key);
    }

    /**
     * Get all the "ordered" platform fields for the current request.
     *
     * @param isSharedDeviceScenario a boolean that indicates if we're capturing telemetry in shared
     *                               device scenario
     * @return a {@link List} of ordered current request platform fields
     */
    static List<String> getCurrentRequestPlatformFields(final boolean isSharedDeviceScenario) {
        final List<String> consolidatedPlatformFields = new ArrayList<>();

        consolidatedPlatformFields.add(Key.PLATFORM_SCHEMA_VERSION);

        if (isSharedDeviceScenario) {
            consolidatedPlatformFields.addAll(currentRequestSharedFlwPlatformFieldsForAndroidAndiOSBroker);
        } else {
            consolidatedPlatformFields.addAll(currentRequestSharedMultipleWpjPlatformFieldsForAndroidAndiOSBroker);
        }

        consolidatedPlatformFields.addAll(currentRequestAndroidPlatformFields);

        return consolidatedPlatformFields;
    }

    /**
     * Get all the "ordered" platform fields for the last request.
     *
     * @return a {@link List} of ordered last request platform fields
     */
    static List<String> getLastRequestPlatformFields() {
        return lastRequestPlatformFields;
    }
}
