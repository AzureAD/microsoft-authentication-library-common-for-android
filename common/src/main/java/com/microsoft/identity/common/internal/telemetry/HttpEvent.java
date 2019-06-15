package com.microsoft.identity.common.internal.telemetry;
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
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public class HttpEvent extends BaseEvent {
    private static final String TAG = HttpEvent.class.getSimpleName();

    public HttpEvent() {
        super();
        putEventName(TELEMETRY_EVENT_HTTP_REQUEST);
    }

    /*
    TELEMETRY_KEY_HTTP_METHOD
    TELEMETRY_KEY_HTTP_PATH
    TELEMETRY_KEY_HTTP_REQUEST_ID_HEADER
    TELEMETRY_KEY_HTTP_RESPONSE_CODE
    TELEMETRY_KEY_OAUTH_ERROR_CODE
    TELEMETRY_KEY_HTTP_RESPONSE_METHOD
    TELEMETRY_KEY_REQUEST_QUERY_PARAMS
    TELEMETRY_KEY_HTTP_ERROR_DOMAIN
    TELEMETRY_KEY_USER_AGENT
    HTTP_ERROR_IN_EVENT: false or true
     */
//    public static final String OAUTH_ERROR_CODE = "oauth_error_code";
//    public static final String HTTP_PATH = "http_path";
//    public static final String HTTP_USER_AGENT = "user_agent";
//    public static final String HTTP_METHOD = "method";
//    public static final String HTTP_QUERY_PARAMETERS = "query_params";
//    public static final String HTTP_RESPONSE_CODE = "response_code";
//    public static final String HTTP_API_VERSION = "api_version";
//    public static final String REQUEST_ID_HEADER = "x_ms_request_id";
//
//    // Network
//    private static final String NETWORK_CONNECTION_KEY = "network_connection";
//    private static final String NETWORK_POWER_OPTIMIZATION_KEY = "network_carrier";

    //TODO
}
