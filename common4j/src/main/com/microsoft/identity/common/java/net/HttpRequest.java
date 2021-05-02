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
package com.microsoft.identity.common.java.net;

import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import lombok.NonNull;

/**
 * Internal class representing a http request.
 */
public class HttpRequest {

    private static final String HOST = "Host";

    private final URL mRequestUrl;

    private final byte[] mRequestContent;

    URL getRequestUrl() {
        return mRequestUrl;
    }

    byte[] getRequestContent() {
        return mRequestContent;
    }

    String getRequestContentType() {
        return mRequestContentType;
    }

    String getRequestMethod() {
        return mRequestMethod;
    }

    Map<String, String> getRequestHeaders() {
        return Collections.unmodifiableMap(mRequestHeaders);
    }

    private final String mRequestContentType;
    private final String mRequestMethod;
    private final Map<String, String> mRequestHeaders = new HashMap<>();

    /**
     * Constructor for {@link HttpRequest} with request {@link URL}, headers, post message and the
     * request content type.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Post message sent in the post request.
     * @param requestContentType Request content type.
     */
    public HttpRequest(@NonNull final URL requestUrl,
                @NonNull final Map<String, String> requestHeaders,
                @NonNull final String requestMethod,
                final byte[] requestContent,
                final String requestContentType) {
        mRequestUrl = requestUrl;
        mRequestHeaders.put(HOST, requestUrl.getAuthority());
        mRequestHeaders.putAll(requestHeaders);
        mRequestMethod = requestMethod;
        mRequestContent = requestContent;
        mRequestContentType = requestContentType;
    }

    /**
     * Check if the given status code is the retryable status code(500/503/504).
     *
     * @param statusCode The status to check.
     * @return True if the status code is 500, 503 or 504, false otherwise.
     */
    public static boolean isRetryableError(final int statusCode) {
        return statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR
                || statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT
                || statusCode == HttpURLConnection.HTTP_UNAVAILABLE;
    }
}
