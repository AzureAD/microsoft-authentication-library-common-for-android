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
package com.microsoft.identity.common.internal.telemetry.events;
import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public class HttpStartEvent extends BaseEvent {

    private static final String TAG = HttpStartEvent.class.getSimpleName();

    public HttpStartEvent() {
        super();
        names(TELEMETRY_EVENT_HTTP_REQUEST_START);
    }

    public HttpStartEvent putMethod(String method) {
        this.put(TELEMETRY_KEY_HTTP_METHOD, method);
        return this;
    }

    public HttpStartEvent putPath(String path) {
        this.put(TELEMETRY_KEY_HTTP_PATH, path);
        return this;
    }

    public HttpStartEvent putRequestIdHeader(String requestIdHeader) {
        this.put(TELEMETRY_KEY_HTTP_REQUEST_ID_HEADER, requestIdHeader);
        return this;
    }

    public HttpStartEvent putResponseCode(String responseCode) {
        this.put(TELEMETRY_KEY_HTTP_RESPONSE_CODE, responseCode);
        return this;
    }

    public HttpStartEvent putResponseMethod(String responseMethod) {
        this.put(TELEMETRY_KEY_HTTP_RESPONSE_METHOD, responseMethod);
        return this;
    }

    public HttpStartEvent putRequestQueryParams(String requestQueryParams) {
        this.put(TELEMETRY_KEY_REQUEST_QUERY_PARAMS, requestQueryParams);
        return this;
    }

    public HttpStartEvent putErrorDomain(String errorDomain) {
        this.put(TELEMETRY_KEY_HTTP_ERROR_DOMAIN, errorDomain);
        return this;
    }

    public HttpStartEvent isNetworkConnected(boolean isConnected) {
        this.put(TELEMETRY_KEY_NETWORK_CONNECTION, String.valueOf(isConnected));
        return this;
    }

    public HttpStartEvent isPowerOptimizationOn(boolean isDozed) {
        this.put(TELEMETRY_KEY_POWER_OPTIMIZATION, String.valueOf(isDozed));
        return this;
    }
}
