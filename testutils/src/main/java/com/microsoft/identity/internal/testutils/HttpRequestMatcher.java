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
package com.microsoft.identity.internal.testutils;

import com.microsoft.identity.common.internal.net.HttpClient;

import java.net.URL;
import java.util.Map;
import java.util.function.Predicate;

import lombok.Builder;
import lombok.NonNull;

/**
 * Used to match http request based on headers, body, url and method. A request is considered as matched
 * if all these 4 parameters are matched.
 */
@Builder
public class HttpRequestMatcher {

    private Predicate<Map<String, String>> headers = header -> true;
    private Predicate<byte[]> body = content -> true;
    private Predicate<URL> url = s -> true;
    private Predicate<HttpClient.HttpMethod> method = s -> true;


    /**
     * Checks whether the request matches the predicates defined in this request matcher.
     *
     * @param method         the request method
     * @param url            the request URL
     * @param requestHeaders the request headers
     * @param requestContent the request body/content
     * @return true if the predicates matched this request
     */
    public boolean matches(
            final HttpClient.HttpMethod method,
            @NonNull final URL url,
            final Map<String, String> requestHeaders,
            final byte[] requestContent) {
        return (null == this.method || this.method.test(method))
                && (null == this.url || this.url.test(url))
                && (null == this.headers || this.headers.test(requestHeaders))
                && (null == this.body || this.body.test(requestContent));
    }
}
