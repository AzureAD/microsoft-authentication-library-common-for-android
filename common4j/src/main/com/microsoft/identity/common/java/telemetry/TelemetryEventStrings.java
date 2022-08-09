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
package com.microsoft.identity.common.java.telemetry;

public class TelemetryEventStrings {
    private static final String EVENT_PREFIX = "Microsoft.MSAL.";

    public static final class App {
        public static final String BUILD = EVENT_PREFIX + "application_build";
        public static final String NAME = EVENT_PREFIX + "application_name";
        public static final String PACKAGE = EVENT_PREFIX + "application_package";
        public static final String VERSION = EVENT_PREFIX + "application_version";
    }

    public static final class Os {
        public static final String NAME = EVENT_PREFIX + "os_name";
        public static final String OS_NAME = "android";
        public static final String VERSION = EVENT_PREFIX + "os_version";
        public static final String SECURITY_PATCH = EVENT_PREFIX + "security_patch";
    }

    public static final class Device {
        public static final String MANUFACTURER = EVENT_PREFIX + "device_manufacturer";
        public static final String MODEL = EVENT_PREFIX + "device_model";
        public static final String NAME = EVENT_PREFIX + "device_name";
        public static final String TIMEZONE = EVENT_PREFIX + "time_zone";
        public static final String ID = EVENT_PREFIX + "device_guid";
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

        public static final String UI_SHOWN_EVENT = "ui_shown_event";

        public static final String BROKER_START_EVENT = "broker_start_event";
        public static final String BROKER_END_EVENT = "broker_end_event";

        public static final String CONTENT_PROVIDER_CALL_EVENT = "content_provider_call_event";

        public static final String DEPRECATED_API_USAGE_EVENT = "deprecated_api_usage_event";
    }

    public static final class EventType {
        public static final String API_EVENT = EVENT_PREFIX + "api_event";
        public static final String CACHE_EVENT = EVENT_PREFIX + "cache_event";
        public static final String UI_EVENT = EVENT_PREFIX + "ui_event";
        public static final String HTTP_EVENT = EVENT_PREFIX + "http_event";
        public static final String BROKER_EVENT = EVENT_PREFIX + "broker_event";
        public static final String LIBRARY_CONSUMER_EVENT = EVENT_PREFIX + "library_consumer_event";
        public static final String ERROR_EVENT = EVENT_PREFIX + "error_event";
        public static final String CONTENT_PROVIDER_EVENT = EVENT_PREFIX + "content_provider_event";
    }

    public static final class Key {
        public static final String EVENT_NAME = EVENT_PREFIX + "event_name";
        public static final String EVENT_TYPE = EVENT_PREFIX + "event_type";
        public static final String AUTHORITY_TYPE = EVENT_PREFIX + "authority_type";
        public static final String AUTHORITY_NAME = EVENT_PREFIX + "authority_name"; //adal
        public static final String AUTHENTICATION_SCHEME = EVENT_PREFIX + "authentication_scheme";
        public static final String AUTHORITY_VALIDATION_STATUS = EVENT_PREFIX + "authority_validation_status";
        public static final String EXTENDED_EXPIRES_ON_SETTING = EVENT_PREFIX + "extended_expires_on_setting";
        public static final String PROMPT_BEHAVIOR = EVENT_PREFIX + "prompt_behavior";
        public static final String IDP_NAME = EVENT_PREFIX + "idp";
        public static final String TENANT_ID = EVENT_PREFIX + "tenant_id";
        public static final String USER_ID = EVENT_PREFIX + "user_id";
        public static final String OCCUR_TIME = EVENT_PREFIX + "occur_time"; //msal only
        public static final String START_TIME = EVENT_PREFIX + "start_time";
        public static final String END_TIME = EVENT_PREFIX + "stop_time";
        public static final String RESPONSE_TIME = "response_time";
        public static final String NETWORK_CONNECTION = EVENT_PREFIX + "network_connection"; //msal only
        public static final String POWER_OPTIMIZATION = EVENT_PREFIX + "power_optimization"; //msal only
        public static final String IS_FORCE_PROMPT = EVENT_PREFIX + "force_prompt"; //msal only
        public static final String IS_FORCE_REFRESH = EVENT_PREFIX + "force_refresh"; //msal only
        public static final String SDK_NAME = EVENT_PREFIX + "sdk_name"; //msal only
        public static final String SDK_VERSION = EVENT_PREFIX + "sdk_version"; //msal only
        public static final String LOGIN_HINT = EVENT_PREFIX + "login_hint";
        public static final String CLAIM_REQUEST = EVENT_PREFIX + "claim_request"; //msal only
        public static final String REDIRECT_URI = EVENT_PREFIX + "redirect_uri"; //msal only
        public static final String SCOPE_SIZE = EVENT_PREFIX + "scope_size"; //msal only
        public static final String SCOPE = EVENT_PREFIX + "scope_value";//msal only
        public static final String NTLM_HANDLED = EVENT_PREFIX + "ntlm";
        public static final String UI_EVENT_COUNT = EVENT_PREFIX + "ui_event_count";
        public static final String CACHE_EVENT_COUNT = EVENT_PREFIX + "cache_event_count";
        public static final String HTTP_EVENT_COUNT = EVENT_PREFIX + "http_event_count";
        public static final String BROKER_APP = EVENT_PREFIX + "broker_app";
        public static final String BROKER_VERSION = EVENT_PREFIX + "broker_version";
        public static final String BROKER_PROTOCOL_VERSION = EVENT_PREFIX + "broker_protocol_version"; //msal only
        public static final String BROKER_APP_USED = EVENT_PREFIX + "broker_app_used";
        public static final String CLIENT_ID = EVENT_PREFIX + "client_id";
        public static final String API_ID = EVENT_PREFIX + "api_id";
        public static final String TOKEN_TYPE = EVENT_PREFIX + "token_type";
        public static final String IS_RT = EVENT_PREFIX + "is_rt";
        public static final String IS_AT = EVENT_PREFIX + "is_at";
        public static final String IS_MRRT = EVENT_PREFIX + "is_mrrt";
        public static final String IS_FRT = EVENT_PREFIX + "is_frt";
        public static final String RT_STATUS = EVENT_PREFIX + "rt_status";  //msal only
        public static final String AT_STATUS = EVENT_PREFIX + "at_status";  //msal only
        public static final String ID_TOKEN_STATUS = EVENT_PREFIX + "id_token_status";  //msal only
        public static final String V1_ID_TOKEN_STATUS = EVENT_PREFIX + "v1_id_token_status";
        public static final String ACCOUNT_STATUS = EVENT_PREFIX + "account_status";
        public static final String MRRT_STATUS = EVENT_PREFIX + "mrrt_status"; //msal only
        public static final String FRT_STATUS = EVENT_PREFIX + "frt_status"; //msal only
        public static final String CORRELATION_ID = EVENT_PREFIX + "correlation_id";
        public static final String ERROR_CODE = EVENT_PREFIX + "error_code";
        public static final String ERROR_DESCRIPTION = EVENT_PREFIX + "error_description"; //msal only
        public static final String ERROR_DOMAIN = EVENT_PREFIX + "error_domain"; //msal only
        public static final String HTTP_METHOD = EVENT_PREFIX + "method";
        public static final String HTTP_PATH = EVENT_PREFIX + "http_path";
        public static final String HTTP_REQUEST_ID_HEADER = EVENT_PREFIX + "x_ms_request_id";
        public static final String HTTP_RESPONSE_CODE = EVENT_PREFIX + "response_code";
        public static final String OAUTH_ERROR_CODE = EVENT_PREFIX + "oauth_error_code";
        public static final String REQUEST_QUERY_PARAMS = EVENT_PREFIX + "query_params";
        public static final String USER_AGENT = EVENT_PREFIX + "user_agent";
        public static final String HTTP_ERROR_DOMAIN = EVENT_PREFIX + "http_error_domain"; //msal only
        public static final String AUTHORITY = EVENT_PREFIX + "authority";
        public static final String GRANT_TYPE = EVENT_PREFIX + "grant_type"; //msal only
        public static final String REQUEST_CODE = EVENT_PREFIX + "request_code"; //msal only
        public static final String RESULT_CODE = EVENT_PREFIX + "result_code"; //msal only
        public static final String USER_CANCEL = EVENT_PREFIX + "user_cancel";
        public static final String UI_VISIBLE = EVENT_PREFIX + "ui_visible";
        public static final String UI_CANCELLED = EVENT_PREFIX + "ui_cancelled"; //msal only
        public static final String UI_COMPLETE = EVENT_PREFIX + "ui_complete"; //msal only
        public static final String SERVER_ERROR_CODE = EVENT_PREFIX + "server_error_code";
        public static final String SERVER_SUBERROR_CODE = EVENT_PREFIX + "server_sub_error_code";
        public static final String RT_AGE = EVENT_PREFIX + "rt_age";
        public static final String SPE_INFO = EVENT_PREFIX + "spe_info";
        public static final String SPE_RING = EVENT_PREFIX + "spe_ring"; //msal only
        public static final String IS_SUCCESSFUL = "_is_successful"; //sub key
        public static final String WIPE_APP = EVENT_PREFIX + "wipe_app"; //msal only
        public static final String WIPE_TIME = EVENT_PREFIX + "wipe_time"; //msal only
        public static final String BROKER_ACTION = EVENT_PREFIX + "broker_action"; //msal only
        public static final String BROKER_STRATEGY = EVENT_PREFIX + "broker_strategy";
        public static final String ACCOUNTS_NUMBER = EVENT_PREFIX + "accounts_number";
        public static final String IS_DEVICE_SHARED = EVENT_PREFIX + "is_device_shared";
        public static final String CLASS_NAME = EVENT_PREFIX + "class_name";
        public static final String CLASS_METHOD = EVENT_PREFIX + "class_method";
        public static final String PACKAGE_NAME = EVENT_PREFIX + "package_name";
        public static final String CALLER_APP_PACKAGE_NAME = EVENT_PREFIX + "caller_app_package_name";
        public static final String CALLER_APP_VERSION = EVENT_PREFIX + "caller_app_version";
        public static final String CALLER_APP_UUID = EVENT_PREFIX + "caller_app_uuid";
        public static final String IPC_STRATEGY = EVENT_PREFIX + "ipc_strategy";
        public static final String ERROR_TAG = EVENT_PREFIX + "error_tag";
        public static final String ERROR_CLASS_NAME = EVENT_PREFIX + "error_class_name";
        public static final String ERROR_COUNT = EVENT_PREFIX + "error_count";
        public static final String ERROR_LOCATION_CLASS_NAME = EVENT_PREFIX + "error_location_class_name";
        public static final String ERROR_LOCATION_METHOD_NAME = EVENT_PREFIX + "error_location_method_name";
        public static final String ERROR_LOCATION_LINE_NUMBER = EVENT_PREFIX + "error_location_line_number";
        public static final String IS_WPJ_JOINED = EVENT_PREFIX + "is_wpj_joined";
        public static final String IS_ERROR_EVENT = EVENT_PREFIX + "is_error_event";
        public static final String CONTENT_PROVIDER_URI = EVENT_PREFIX + "content_provider_uri";
        public static final String ENROLLMENT_ID_NULL = EVENT_PREFIX + "enrollment_id_null";
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
        public static final String ACCOUNT_MANAGER = "account_manager";
        public static final String BOUND_SERVICE = "bound_service";
        public static final String CONTENT_PROVIDER = "content_provider";
        public static final String UNSET = "UNSET";
    }

    public static final class Api {
        public static final String BROKER_ACQUIRE_TOKEN_INTERACTIVE = "201";
        public static final String BROKER_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE = "202";
        public static final String BROKER_ACQUIRE_TOKEN_SILENT = "203";
        public static final String GET_BROKER_DEVICE_MODE = "204";
        public static final String BROKER_GET_CURRENT_ACCOUNT = "205";
        public static final String BROKER_GET_ACCOUNTS = "206";
        public static final String BROKER_REMOVE_ACCOUNT = "207";
        public static final String BROKER_REMOVE_ACCOUNT_FROM_SHARED_DEVICE = "208";

        public static final String LOCAL_ACQUIRE_TOKEN_INTERACTIVE = "101";
        public static final String LOCAL_COMPLETE_ACQUIRE_TOKEN_INTERACTIVE = "1032";
        public static final String LOCAL_ACQUIRE_TOKEN_SILENT = "103";
        public static final String LOCAL_GET_ACCOUNTS = "106";
        public static final String LOCAL_REMOVE_ACCOUNT = "107";
        public static final String LOCAL_DEVICE_CODE_FLOW_ACQUIRE_URL_AND_CODE = "108";
        public static final String LOCAL_DEVICE_CODE_FLOW_POLLING = "109";
    }

    public static final class BrokerApi {
        public static final String ACQUIRE_TOKEN_SILENT = "301";
        public static final String ACQUIRE_TOKEN_INTERACTIVE = "302";
    }
}

