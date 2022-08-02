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
package com.microsoft.identity.http;

import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.NonNull;

/**
 * An {@link HttpRequestInterceptor} that allows to re-write the http request and then proceeds
 * with the modified request.
 */
@AllArgsConstructor
public abstract class HttpRequestRewriter implements HttpRequestInterceptor {

    private final HttpClient mOriginalClient;

    @Override
    public HttpResponse performIntercept(@NonNull final HttpClient.HttpMethod httpMethod,
                                         @NonNull final URL requestUrl,
                                         @NonNull final Map<String, String> requestHeaders,
                                         @Nullable final byte[] requestContent) throws IOException {
        return mOriginalClient.method(
                rewriteHttpMethod(httpMethod),
                rewriteRequestUrl(requestUrl),
                rewriteRequestHeaders(requestHeaders),
                rewriteRequestContent(requestContent)
        );
    }

    /**
     * Rewrites the HTTP Method for this request.
     *
     * @param httpMethod the original HTTP method
     * @return the modified HTTP method
     */
    public abstract HttpClient.HttpMethod rewriteHttpMethod(@NonNull final HttpClient.HttpMethod httpMethod);

    /**
     * Rewrites the URL for this request.
     *
     * @param requestUrl the original request URL
     * @return the modified request URL
     */
    public abstract URL rewriteRequestUrl(@NonNull final URL requestUrl);

    /**
     * Rewrites the headers for this request.
     *
     * @param requestHeaders the original request headers
     * @return the modified request headers
     */
    public abstract Map<String, String> rewriteRequestHeaders(@NonNull final Map<String, String> requestHeaders);

    /**
     * Rewrites the content for this request.
     *
     * @param requestContent the original request content
     * @return the modified request content
     */
    public abstract byte[] rewriteRequestContent(@Nullable final byte[] requestContent);
}
