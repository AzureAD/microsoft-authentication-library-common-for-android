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
package com.microsoft.identity.common.internal.telemetry;

public final class TelemetryEventStrings {
    public static final String TELEMETRY_EVENT_API_START_EVENT = "api_start_event";
    public static final String TELEMETRY_EVENT_API_END_EVENT = "api_end_event";
    public static final String TELEMETRY_EVENT_API_EVENT = "api_event";

    public static final String TELEMETRY_EVENT_CACHE_START_EVENT = "cache_start_event";
    public static final String TELEMETRY_EVENT_CACHE_END_EVENT = "cache_end_event";
    public static final String TELEMETRY_EVENT_CACHE_EVENT = "cache_event";

    public static final String TELEMETRY_EVENT_UI_START_EVENT = "ui_start_event";
    public static final String TELEMETRY_EVENT_UI_END_EVENT = "ui_end_event";
    public static final String TELEMETRY_EVENT_UI_EVENT = "ui_event";

    public static final String TELEMETRY_EVENT_HTTP_START_EVENT = "http_start_event";
    public static final String TELEMETRY_EVENT_HTTP_END_EVENT = "http_end_event";
    public static final String TELEMETRY_EVENT_HTTP_EVENT = "http_event";

    public static final String TELEMETRY_EVENT_BROKER_START_EVENT = "broker_start_event";
    public static final String TELEMETRY_EVENT_BROKER_END_EVENT = "broker_end_event";
    public static final String TELEMETRY_EVENT_BROKER_EVENT = "broker_event";

    public static final String TELEMETRY_EVENT_AUTHORITY_VALIDATION_START_EVENT = "authority_validation_start_event";
    public static final String TELEMETRY_EVENT_AUTHORITY_VALIDATION_END_EVENT = "authority_validation_end_event";
    public static final String TELEMETRY_EVENT_AUTHORITY_VALIDATION_EVENT = "authority_validation_event";

    public static final String TELEMETRY_EVENT_TOKEN_GRANT = "token_grant";
    public static final String TELEMETRY_EVENT_ACQUIRE_TOKEN_SILENT = "acquire_token_silent_handler";
    public static final String TELEMETRY_EVENT_AUTHORIZATION_CODE = "authorization_code";
    public static final String TELEMETRY_EVENT_TOKEN_CACHE_LOOKUP = "token_cache_lookup";
    public static final String TELEMETRY_EVENT_TOKEN_CACHE_WRITE = "token_cache_write";
    public static final String TELEMETRY_EVENT_TOKEN_CACHE_DELETE = "token_cache_delete";
    public static final String TELEMETRY_EVENT_APP_METADATA_WRITE = "app_metadata_write";
    public static final String TELEMETRY_EVENT_APP_METADATA_DELETE = "app_metadata_delete";

    // Telemetry property name, only alphabetic letters, dots, and underscores are allowed.
    public static final String TELEMETRY_KEY_EVENT_NAME = "event_name";
    public static final String TELEMETRY_KEY_EVENT_TYPE = "event_type";
    public static final String TELEMETRY_KEY_AUTHORITY_TYPE = "authority_type";
    public static final String TELEMETRY_KEY_AUTHORITY_VALIDATION_STATUS = "authority_validation_status";
    public static final String TELEMETRY_KEY_EXTENDED_EXPIRES_ON_SETTING = "extended_expires_on_setting";
    public static final String TELEMETRY_KEY_PROMPT_BEHAVIOR = "prompt_behavior";
    public static final String TELEMETRY_KEY_UI_BEHAVIOR = "ui_behavior";
    public static final String TELEMETRY_KEY_RESULT_STATUS = "status";
    public static final String TELEMETRY_KEY_IDP = "idp";
    public static final String TELEMETRY_KEY_TENANT_ID = "tenant_id";
    public static final String TELEMETRY_KEY_USER_ID = "user_id";
    public static final String TELEMETRY_KEY_OCCUR_TIME = "occur_time";
    public static final String TELEMETRY_KEY_START_TIME = "start_time";
    public static final String TELEMETRY_KEY_END_TIME = "stop_time";
    public static final String TELEMETRY_KEY_RESPONSE_TIME = "response_time";
    public static final String TELEMETRY_KEY_NETWORK_CONNECTION = "network_connection";
    public static final String TELEMETRY_KEY_POWER_OPTIMIZATION = "power_optimization";
    public static final String TELEMETRY_KEY_IS_FORCE_PROMPT = "force_prompt";
    public static final String TELEMETRY_KEY_IS_FORCE_REFRESH = "force_refresh";
    public static final String TELEMETRY_KEY_DEVICE_ID = "device_id";
    public static final String TELEMETRY_KEY_APPLICATION_NAME = "application_name";
    public static final String TELEMETRY_KEY_APPLICATION_VERSION = "application_version";
    public static final String TELEMETRY_KEY_SDK_NAME = "sdk_name";
    public static final String TELEMETRY_KEY_SDK_VERSION = "sdk_version";
    public static final String TELEMETRY_KEY_LOGIN_HINT = "login_hint";
    public static final String TELEMETRY_KEY_CLAIM_REQUEST = "claim_request";
    public static final String TELEMETRY_KEY_REDIRECT_URI = "redirect_uri";
    public static final String TELEMETRY_KEY_SCOPE_SIZE = "scope_size";
    public static final String TELEMETRY_KEY_SCOPE = "scope_value";
    public static final String TELEMETRY_KEY_NTLM_HANDLED = "ntlm";
    public static final String TELEMETRY_KEY_UI_EVENT_COUNT = "ui_event_count";
    public static final String TELEMETRY_KEY_API_EVENT_COUNT = "api_event_count";
    public static final String TELEMETRY_KEY_BROKER_APP = "broker_app";
    public static final String TELEMETRY_KEY_BROKER_VERSION = "broker_version";
    public static final String TELEMETRY_KEY_BROKER_PROTOCOL_VERSION = "broker_protocol_version";
    public static final String TELEMETRY_KEY_BROKER_APP_USED = "broker_app_used";
    public static final String TELEMETRY_KEY_CLIENT_ID = "client_id";
    public static final String TELEMETRY_KEY_HTTP_EVENT_COUNT = "http_event_count";
    public static final String TELEMETRY_KEY_CACHE_EVENT_COUNT = "cache_event_count";
    public static final String TELEMETRY_KEY_API_ID = "api_id";
    public static final String TELEMETRY_KEY_TOKEN_TYPE = "token_type";
    public static final String TELEMETRY_KEY_IS_RT = "is_rt";
    public static final String TELEMETRY_KEY_IS_MRRT = "is_mrrt";
    public static final String TELEMETRY_KEY_IS_FRT = "is_frt";
    public static final String TELEMETRY_KEY_RT_STATUS = "token_rt_status";
    public static final String TELEMETRY_KEY_MRRT_STATUS = "token_mrrt_status";
    public static final String TELEMETRY_KEY_FRT_STATUS = "token_frt_status";
    public static final String TELEMETRY_KEY_CORRELATION_ID = "correlation_id";
    public static final String TELEMETRY_KEY_IS_EXTENED_LIFE_TIME_TOKEN = "is_extended_life_time_token";
    public static final String TELEMETRY_KEY_API_ERROR_CODE = "api_error_code";
    public static final String TELEMETRY_KEY_PROTOCOL_CODE = "error_protocol_code";
    public static final String TELEMETRY_KEY_ERROR_DESCRIPTION = "error_description";
    public static final String TELEMETRY_KEY_ERROR_DOMAIN = "error_domain";
    public static final String TELEMETRY_KEY_HTTP_METHOD = "method";
    public static final String TELEMETRY_KEY_HTTP_PATH = "http_path";
    public static final String TELEMETRY_KEY_HTTP_REQUEST_ID_HEADER = "x_ms_request_id";
    public static final String TELEMETRY_KEY_HTTP_RESPONSE_CODE = "response_code";
    public static final String TELEMETRY_KEY_OAUTH_ERROR_CODE = "oauth_error_code";
    public static final String TELEMETRY_KEY_HTTP_RESPONSE_METHOD = "response_method";
    public static final String TELEMETRY_KEY_REQUEST_QUERY_PARAMS = "query_params";
    public static final String TELEMETRY_KEY_USER_AGENT = "user_agent";
    public static final String TELEMETRY_KEY_HTTP_ERROR_DOMAIN = "http_error_domain";
    public static final String TELEMETRY_KEY_AUTHORITY = "authority";
    public static final String TELEMETRY_KEY_GRANT_TYPE = "grant_type";
    public static final String TELEMETRY_KEY_API_STATUS = "api_status";
    public static final String TELEMETRY_KEY_REQUEST_CODE = "request_code";
    public static final String TELEMETRY_KEY_RESULT_CODE = "result_code";
    public static final String TELEMETRY_KEY_USER_CANCEL = "user_cancel";
    public static final String TELEMETRY_KEY_UI_CANCELLED = "ui_cancelled";
    public static final String TELEMETRY_KEY_SERVER_ERROR_CODE = "server_error_code";
    public static final String TELEMETRY_KEY_SERVER_SUBERROR_CODE = "server_sub_error_code";
    public static final String TELEMETRY_KEY_RT_AGE = "rt_age";
    public static final String TELEMETRY_KEY_SPE_INFO = "spe_info";
    public static final String TELEMETRY_KEY_SPE_RING = "spe_ring";
    public static final String TELEMETRY_KEY_IS_SUCCESSFUL = "is_successful";
    public static final String TELEMETRY_KEY_WIPE_APP = "wipe_app";
    public static final String TELEMETRY_KEY_WIPE_TIME = "wipe_time";

    public static final String TELEMETRY_KEY_BROKER_ACTION = "broker_action";

    // App
    public static final String TELEMETRY_KEY_APP_BUILD = "app_build";
    // Device
    public static final String TELEMETRY_KEY_DEVICE_MANUFACTURER = "device_manufacturer";
    public static final String TELEMETRY_KEY_DEVICE_MODEL = "device_model";
    public static final String TELEMETRY_KEY_DEVICE_NAME = "device_name";
    public static final String TELEMETRY_KEY_DEVICE_TIMEZONE = "time_zone";
    // OS
    public static final String TELEMETRY_KEY_OS_NAME = "os_name";
    public static final String TELEMETRY_VALUE_OS_NAME = "android";
    public static final String TELEMETRY_KEY_OS_VERSION = "os_version";
    public static final String TELEMETRY_KEY_TIMEZONE = "timezone";
    public static final String TELEMETRY_KEY_OS_SECURITY_PATCH = "security_patch";

    // Telemetry property value
    public static final String TELEMETRY_VALUE_TRUE = "true";
    public static final String TELEMETRY_VALUE_FALSE = "false";
    public static final String TELEMETRY_VALUE_TRIED = "tried";
    public static final String TELEMETRY_VALUE_USER_CANCELLED = "user_cancelled";
    public static final String TELEMETRY_VALUE_NOT_FOUND = "not_found";
    public static final String TELEMETRY_VALUE_ACCESS_TOKEN = "access_token";
    public static final String TELEMETRY_VALUE_REFRESH_TOKEN = "refresh_token";
    public static final String TELEMETRY_VALUE_MULTI_RESOURCE_REFRESH_TOKEN = "multi_resource_refresh_token";
    public static final String TELEMETRY_VALUE_FAMILY_REFRESH_TOKEN = "family_refresh_token";
    public static final String TELEMETRY_VALUE_ADFS_TOKEN = "ADFS_access_token_refresh_token";
    public static final String TELEMETRY_VALUE_BY_CODE = "by_code";
    public static final String TELEMETRY_VALUE_BY_REFRESH_TOKEN = "by_refresh_token";
    public static final String TELEMETRY_VALUE_SUCCEEDED = "succeeded";
    public static final String TELEMETRY_VALUE_FAILED = "failed";
    public static final String TELEMETRY_VALUE_CANCELLED = "cancelled";
    public static final String TELEMETRY_VALUE_UNKNOWN = "unknown";
    public static final String TELEMETRY_VALUE_AUTHORITY_AAD = "aad";
    public static final String TELEMETRY_VALUE_AUTHORITY_ADFS = "adfs";
    public static final String TELEMETRY_VALUE_AUTHORITY_B2C = "b2c";

    //Telemetry API ID
    public static final String API_BROKER_ACQUIRE_TOKEN_INTERACTIVE = "201";
    public static final String API_BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE = "202";
    public static final String API_BROKER_ACQUIRE_TOKEN_SILENT= "203";
    public static final String API_GET_BROKER_DEVICE_MODE= "204";
    public static final String API_BROKER_GET_CURRENT_ACCOUNT= "205";
    public static final String API_BROKER_GET_ACCOUNTS= "206";
    public static final String API_BROKER_REMOVE_ACCOUNT= "207";
    public static final String API_BROKER_REMOVE_ACCOUNT_FROM_SHARED_DEVICE= "208";

    public static final String API_LOCAL_ACQUIRE_TOKEN_INTERACTIVE = "101";
    public static final String API_LOCAL_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE = "1032";
    public static final String API_LOCAL_ACQUIRE_TOKEN_SILENT= "103";
    public static final String API_LOCAL_GET_ACCOUNTS= "106";
    public static final String API_LOCAL_REMOVE_ACCOUNT= "107";
}

