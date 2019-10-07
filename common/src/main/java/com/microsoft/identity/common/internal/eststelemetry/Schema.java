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

import com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.util.Arrays;

/**
 * This class defines the schema for server-side telemetry
 */
public class Schema {

    public static final String SCHEMA_VERSION_KEY = "schema_version";
    public static final String CURRENT_SCHEMA_VERSION = "1";

    public static final String CURRENT_REQUEST_HEADER_NAME = "x-client-current-telemetry";
    public static final String LAST_REQUEST_HEADER_NAME = "x-client-last-telemetry";

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
    }

    public static final class Value {
        public static final String TRUE = "1";
        public static final String FALSE = "0";
        public static final String EMPTY = "";
    }

    /**
     * This array defines the common schema for current request.
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final String[] currentRequestCommonFields = new String[]{
            Key.API_ID,
            Key.FORCE_REFRESH
    };

    /**
     * This array defines the platform schema for current request
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final String[] currentRequestPlatformFields = new String[]{
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
     * Failure do so will break the schema.
     */
    private static final String[] lastRequestCommonFields = new String[]{
            Key.API_ID,
            Key.CORRELATION_ID,
            Key.ERROR_CODE
    };

    /**
     * This array defines the platform schema for last request
     * NOTE: These fields must always be listed in the correct order in this array.
     * Failure do so will break the schema.
     */
    private static final String[] lastRequestPlatformFields = new String[]{

    };

    private static boolean isCurrentCommonField(final String key) {
        return Arrays.asList(currentRequestCommonFields).contains(key);
    }

    private static boolean isLastCommonField(final String key) {
        return Arrays.asList(lastRequestCommonFields).contains(key);
    }

    private static boolean isCurrentPlatformField(final String key) {
        return Arrays.asList(currentRequestPlatformFields).contains(key);
    }

    private static boolean isLastPlatformField(final String key) {
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

    /**
     * Get a list of common telemetry fields. These are telemetry fields that are shared between
     * all MSAL platforms and each platform includes these fields as headers in each request to ests.
     *
     * @param isCurrent denotes if to get common fields for current or last request
     * @return A string array that contains the common fields based on the value of isCurrent parameter
     */
    static String[] getCommonFields(final boolean isCurrent) {
        return isCurrent ? getCurrentRequestCommonFields() : getLastRequestCommonFields();
    }

    /**
     * Get a list of platform specific telemetry fields. These are specific to the Android platform.
     * The platform has complete control over what platform fields they want to track as part of
     * telemetry sent in each header to ests.
     *
     * @param isCurrent denotes if to get common fields for current or last request
     * @return A string array that contains the common fields based on the value of isCurrent parameter
     */
    static String[] getPlatformFields(final boolean isCurrent) {
        return isCurrent ? getCurrentRequestPlatformFields() : getLastRequestPlatformFields();
    }

    static boolean isCommonField(final String key, final boolean isCurrent) {
        return isCurrent ? isCurrentCommonField(key) : isLastCommonField(key);
    }

    static boolean isPlatformField(final String key, final boolean isCurrent) {
        return isCurrent ? isCurrentPlatformField(key) : isLastPlatformField(key);
    }

    static String getSchemaCompliantStringFromBoolean(final boolean val) {
        return val ? Value.TRUE : Value.FALSE;
    }

    static String getSchemaCompliantString(final String s) {
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
