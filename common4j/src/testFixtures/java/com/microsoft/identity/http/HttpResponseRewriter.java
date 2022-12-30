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
 * An {@link HttpRequestInterceptor} that proceeds with the original request and allow to re-write
 * the received http response.
 */
@AllArgsConstructor
public abstract class HttpResponseRewriter implements HttpRequestInterceptor {

    private final HttpClient mOriginalClient;

    @Override
    public HttpResponse performIntercept(@NonNull final HttpClient.HttpMethod httpMethod,
                                         @NonNull final URL requestUrl,
                                         @NonNull final Map<String, String> requestHeaders,
                                         @Nullable final byte[] requestContent) throws IOException {
        final HttpResponse originalResponse = mOriginalClient.method(
                httpMethod,
                requestUrl,
                requestHeaders,
                requestContent
        );

        return rewriteHttpResponse(originalResponse);
    }

    /**
     * Rewrites the received HTTP response for this request.
     *
     * @param originalResponse the original HTTP response
     * @return the modified HTTP response
     */
    public abstract HttpResponse rewriteHttpResponse(@NonNull final HttpResponse originalResponse);
}
