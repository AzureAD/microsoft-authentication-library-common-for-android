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

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import javax.net.ssl.SSLContext;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * A class that simplifies its child's implementation, by making every methods in {@link HttpClient}
 * calling a single method - the only method the child class needs to override.
 * */
public abstract class AbstractHttpClient implements HttpClient {

    @Override
    public HttpResponse method(@NonNull final String httpMethod,
                               @NonNull final URL requestUrl,
                               @NonNull final Map<String, String> requestHeaders,
                               @Nullable final byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.validateAndNormalizeMethod(httpMethod), requestUrl, requestHeaders, requestContent);
    }

    @Override
    public abstract HttpResponse method(@NonNull final HttpMethod httpMethod,
                                        @NonNull final URL requestUrl,
                                        @NonNull final Map<String, String> requestHeaders,
                                        @Nullable final byte[] requestContent) throws IOException;

    @Override
    public HttpResponse put(@NonNull final URL requestUrl,
                            @NonNull final Map<String, String> requestHeaders,
                            @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.PUT, requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse patch(@NonNull final URL requestUrl,
                              @NonNull final Map<String, String> requestHeaders,
                              @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.PATCH, requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse options(@NonNull final URL requestUrl,
                                @NonNull final Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.OPTIONS, requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse post(@NonNull final URL requestUrl,
                             @NonNull final Map<String, String> requestHeaders,
                             @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.POST, requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse delete(@NonNull final URL requestUrl,
                               @NonNull final Map<String, String> requestHeaders,
                               @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.DELETE, requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse get(@NonNull final URL requestUrl,
                            @NonNull final Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.GET, requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse head(@NonNull final URL requestUrl,
                             @NonNull final Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.HEAD, requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse trace(@NonNull final URL requestUrl,
                              @NonNull final Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.TRACE, requestUrl, requestHeaders, null);
    }
}
