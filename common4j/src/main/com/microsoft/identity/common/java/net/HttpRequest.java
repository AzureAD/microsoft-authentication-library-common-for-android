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

import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;

import lombok.Getter;
import lombok.NonNull;
import lombok.experimental.Accessors;

/**
 * Internal class representing a http request.
 */
public class HttpRequest {

    private static final String HOST = "Host";

    @Getter
    @Accessors(prefix = "m")
    private final URL mRequestUrl;

    @Accessors(prefix = "m")
    private final byte[] mRequestContent;

    public byte[] getRequestContent() {
        if (mRequestContent == null) {
            return null;
        }

        return Arrays.copyOf(mRequestContent, mRequestContent.length);
    }

    @Getter
    @Accessors(prefix = "m")
    private final String mRequestContentType;

    @Getter
    @Accessors(prefix = "m")
    private final String mRequestMethod;

    private final Map<String, String> mRequestHeaders = new HashMap<>();

    Map<String, String> getRequestHeaders() {
        return Collections.unmodifiableMap(mRequestHeaders);
    }

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
        mRequestContent = requestContent != null ? Arrays.copyOf(requestContent, requestContent.length) : null;
        mRequestContentType = requestContentType;
    }
}
