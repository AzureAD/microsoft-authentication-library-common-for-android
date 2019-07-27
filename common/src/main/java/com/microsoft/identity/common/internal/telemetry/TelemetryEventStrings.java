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
    public static final class App {
        public static final String BUILD = "app_build";
        public static final String NAME = "app_name";
        public static final String VERSION = "app_version";
    }

    public static final class Os {
        public static final String NAME = "os_name";
        public static final String OS_NAME = "android";
        public static final String VERSION = "os_version";
        public static final String SECURITY_PATCH = "security_patch";
    }

    public static final class Device {
        public static final String MANUFACTURER = "device_manufacturer";
        public static final String MODEL = "device_model";
        public static final String NAME = "device_name";
        public static final String TIMEZONE = "time_zone";
    }

    public static final class Event {
        public static final String API_START_EVENT = "api_start_event";
        public static final String API_END_EVENT = "api_end_event";

        public static final String CACHE_START_EVENT = "cache_start_event";
        public static final String CACHE_END_EVENT = "cache_end_event";

        public static final String UI_START_EVENT = "ui_start_event";
        public static final String UI_END_EVENT = "ui_end_event";

        public static final String HTTP_START_EVENT = "http_start_event";
        public static final String HTTP_END_EVENT = "http_end_event";

        public static final String BROKER_START_EVENT = "broker_start_event";
        public static final String BROKER_END_EVENT = "broker_end_event";

        public static final String AUTHORITY_VALIDATION_START_EVENT = "authority_validation_start_event";
        public static final String AUTHORITY_VALIDATION_END_EVENT = "authority_validation_end_event";
    }

    public static final class EventType {
        public static final String API_EVENT = "api_event";
        public static final String CACHE_EVENT = "cache_event";
        public static final String UI_EVENT = "ui_event";
        public static final String HTTP_EVENT = "http_event";
        public static final String BROKER_EVENT = "broker_event";
        public static final String AUTHORITY_VALIDATION_EVENT = "authority_validation_event";

    }

    public static final class Key {
        public static final String EVENT_NAME = "event_name";
        public static final String EVENT_TYPE = "event_type";
        public static final String AUTHORITY_TYPE = "authority_type";
        public static final String AUTHORITY_VALIDATION_STATUS = "authority_validation_status";
        public static final String EXTENDED_EXPIRES_ON_SETTING = "extended_expires_on_setting";
        public static final String PROMPT_BEHAVIOR = "prompt_behavior";
        public static final String UI_BEHAVIOR = "ui_behavior";
        public static final String RESULT_STATUS = "status";
        public static final String IDP = "idp";
        public static final String TENANT_ID = "tenant_id";
        public static final String USER_ID = "user_id";
        public static final String OCCUR_TIME = "occur_time";
        public static final String START_TIME = "start_time";
        public static final String END_TIME = "stop_time";
        public static final String RESPONSE_TIME = "response_time";
        public static final String NETWORK_CONNECTION = "network_connection";
        public static final String POWER_OPTIMIZATION = "power_optimization";
        public static final String IS_FORCE_PROMPT = "force_prompt";
        public static final String IS_FORCE_REFRESH = "force_refresh";
        public static final String DEVICE_ID = "device_id";
        public static final String APPLICATION_NAME = "application_name";
        public static final String APPLICATION_VERSION = "application_version";
        public static final String SDK_NAME = "sdk_name";
        public static final String SDK_VERSION = "sdk_version";
        public static final String LOGIN_HINT = "login_hint";
        public static final String CLAIM_REQUEST = "claim_request";
        public static final String REDIRECT_URI = "redirect_uri";
        public static final String SCOPE_SIZE = "scope_size";
        public static final String SCOPE = "scope_value";
        public static final String NTLM_HANDLED = "ntlm";
        public static final String UI_EVENT_COUNT = "ui_event_count";
        public static final String EVENT_COUNT = "event_count";
        public static final String BROKER_APP = "broker_app";
        public static final String BROKER_VERSION = "broker_version";
        public static final String BROKER_PROTOCOL_VERSION = "broker_protocol_version";
        public static final String BROKER_APP_USED = "broker_app_used";
        public static final String CLIENT_ID = "client_id";
        public static final String HTTP_EVENT_COUNT = "http_event_count";
        public static final String CACHE_EVENT_COUNT = "cache_event_count";
        public static final String API_ID = "api_id";
        public static final String TOKEN_TYPE = "token_type";
        public static final String IS_RT = "is_rt";
        public static final String IS_MRRT = "is_mrrt";
        public static final String IS_FRT = "is_frt";
        public static final String RT_STATUS = "token_rt_status";
        public static final String MRRT_STATUS = "token_mrrt_status";
        public static final String FRT_STATUS = "token_frt_status";
        public static final String CORRELATION_ID = "correlation_id";
        public static final String IS_EXTENED_LIFE_TIME_TOKEN = "is_extended_life_time_token";
        public static final String ERROR_CODE = "error_code";
        public static final String PROTOCOL_CODE = "error_protocol_code";
        public static final String ERROR_DESCRIPTION = "error_description";
        public static final String ERROR_DOMAIN = "error_domain";
        public static final String HTTP_METHOD = "method";
        public static final String HTTP_PATH = "http_path";
        public static final String HTTP_REQUEST_ID_HEADER = "x_ms_request_id";
        public static final String HTTP_RESPONSE_CODE = "response_code";
        public static final String OAUTH_ERROR_CODE = "oauth_error_code";
        public static final String HTTP_RESPONSE_METHOD = "response_method";
        public static final String REQUEST_QUERY_PARAMS = "query_params";
        public static final String USER_AGENT = "user_agent";
        public static final String HTTP_ERROR_DOMAIN = "http_error_domain";
        public static final String AUTHORITY = "authority";
        public static final String GRANT_TYPE = "grant_type";
        public static final String STATUS = "status";
        public static final String REQUEST_CODE = "request_code";
        public static final String RESULT_CODE = "result_code";
        public static final String USER_CANCEL = "user_cancel";
        public static final String UI_CANCELLED = "ui_cancelled";
        public static final String SERVER_ERROR_CODE = "server_error_code";
        public static final String SERVER_SUBERROR_CODE = "server_sub_error_code";
        public static final String RT_AGE = "rt_age";
        public static final String SPE_INFO = "spe_info";
        public static final String SPE_RING = "spe_ring";
        public static final String IS_SUCCESSFUL = "is_successful";
        public static final String WIPE_APP = "wipe_app";
        public static final String WIPE_TIME = "wipe_time";
        public static final String BROKER_ACTION = "broker_action";
        public static final String USER_CANCELLED = "user_cancelled";
        public static final String ACCOUNTS_NUMBER = "accounts_number";
        public static final String IS_DEVICE_SHARED = "is_device_shared";

        public static final String TOKEN_GRANT = "token_grant";
        public static final String ACQUIRE_TOKEN_SILENT = "acquire_token_silent_handler";
        public static final String AUTHORIZATION_CODE = "authorization_code";
        public static final String TOKEN_CACHE_LOOKUP = "token_cache_lookup";
        public static final String TOKEN_CACHE_WRITE = "token_cache_write";
        public static final String TOKEN_CACHE_DELETE = "token_cache_delete";
        public static final String APP_METADATA_WRITE = "app_metadata_write";
        public static final String APP_METADATA_DELETE = "app_metadata_delete";
    }
    
    public static final class Value {
        public static final String TRUE = "true";
        public static final String FALSE = "false";
        public static final String TRIED = "tried";
        public static final String NOT_FOUND = "not_found";
        public static final String ACCESS_TOKEN = "access_token";
        public static final String REFRESH_TOKEN = "refresh_token";
        public static final String MULTI_RESOURCE_REFRESH_TOKEN = "multi_resource_refresh_token";
        public static final String FAMILY_REFRESH_TOKEN = "family_refresh_token";
        public static final String ADFS_TOKEN = "ADFS_access_token_refresh_token";
        public static final String BY_CODE = "by_code";
        public static final String BY_REFRESH_TOKEN = "by_refresh_token";
        public static final String SUCCEEDED = "succeeded";
        public static final String FAILED = "failed";
        public static final String CANCELLED = "cancelled";
        public static final String UNKNOWN = "unknown";
        public static final String AUTHORITY_AAD = "aad";
        public static final String AUTHORITY_ADFS = "adfs";
        public static final String AUTHORITY_B2C = "b2c";
    }

    public static final class Api {
        public static final String BROKER_ACQUIRE_TOKEN_INTERACTIVE = "201";
        public static final String BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE = "202";
        public static final String BROKER_ACQUIRE_TOKEN_SILENT= "203";
        public static final String GET_BROKER_DEVICE_MODE= "204";
        public static final String BROKER_GET_CURRENT_ACCOUNT= "205";
        public static final String BROKER_GET_ACCOUNTS= "206";
        public static final String BROKER_REMOVE_ACCOUNT= "207";
        public static final String BROKER_REMOVE_ACCOUNT_FROM_SHARED_DEVICE= "208";

        public static final String LOCAL_ACQUIRE_TOKEN_INTERACTIVE = "101";
        public static final String LOCAL_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE = "1032";
        public static final String LOCAL_ACQUIRE_TOKEN_SILENT= "103";
        public static final String LOCAL_GET_ACCOUNTS= "106";
        public static final String LOCAL_REMOVE_ACCOUNT= "107";
    }
}

