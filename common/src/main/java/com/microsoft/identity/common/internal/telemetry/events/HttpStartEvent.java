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
import java.net.URL;

import static com.microsoft.identity.common.internal.telemetry.TelemetryEventStrings.*;

public class HttpStartEvent extends BaseEvent {
    public HttpStartEvent() {
        super();
        names(Event.HTTP_START_EVENT);
        types(EventType.HTTP_EVENT);
    }

    public HttpStartEvent putMethod(String method) {
        put(Key.HTTP_METHOD, method);
        return this;
    }

    //TODO Add unit test for b2c urls where where the format is pretty undefined (now that vanity urls are supported)
    public HttpStartEvent putPath(URL path) {
        final StringBuilder logPath = new StringBuilder();
        logPath.append(path.getProtocol())
                .append("://")
                .append(path.getAuthority())
                .append('/');

        // we do not want to send tenant information
        // index 0 is blank
        // index 1 is tenant
        final String[] splitArray = path.getPath().split("/");
        for (int i = 2; i < splitArray.length; i++) {
            logPath.append(splitArray[i])
                    .append('/');
        }

        put(Key.HTTP_PATH, logPath.toString());
        return this;
    }

    public HttpStartEvent putRequestIdHeader(String requestIdHeader) {
        put(Key.HTTP_REQUEST_ID_HEADER, requestIdHeader);
        return this;
    }

    public HttpStartEvent putRequestQueryParams(String requestQueryParams) {
        put(Key.REQUEST_QUERY_PARAMS, requestQueryParams);
        return this;
    }

    public HttpStartEvent putErrorDomain(String errorDomain) {
        put(Key.HTTP_ERROR_DOMAIN, errorDomain);
        return this;
    }
}
