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
package com.microsoft.identity.internal.testutils.shadows;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.net.UrlConnectionHttpClient;
import com.microsoft.identity.common.java.net.AbstractHttpClient;
import com.microsoft.identity.internal.testutils.HttpRequestInterceptor;
import com.microsoft.identity.internal.testutils.MockHttpClient;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

/**
 * Allows us to mock http request responses by shadowing the {@link HttpClient}.
 * <p>
 * <p>
 * We need to shadow the {@link AbstractHttpClient} because we are using an instance of the
 * {@link UrlConnectionHttpClient} in the method here
 * {@link ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])} therefore using the
 * {@link UrlConnectionHttpClient} as the shadow would prevent us from making an actual http request
 * when there are no interceptors defined for the request.
 *
 * @see MockHttpClient for setting up interceptors and mock responses
 * @see HttpRequestInterceptor an implementation for intercepting http requests.
 */
@Implements(AbstractHttpClient.class)
public class ShadowHttpClient {

    /**
     * Execute the http method using either the interceptor or the actual http client.
     *
     * @param httpMethod     the http method
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @param requestContent the request body
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see HttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     */
    public HttpResponse method(@NonNull HttpClient.HttpMethod httpMethod,
                               @NonNull URL requestUrl,
                               @NonNull Map<String, String> requestHeaders,
                               @Nullable byte[] requestContent) throws IOException {
        HttpRequestInterceptor interceptor = MockHttpClient.intercept(httpMethod, requestUrl, requestHeaders, requestContent);
        if (interceptor == null) {
            return UrlConnectionHttpClient.getDefaultInstance().method(httpMethod, requestUrl, requestHeaders, requestContent);
        } else {
            return interceptor.intercept(httpMethod, requestUrl, requestHeaders, requestContent);
        }
    }

    /**
     * This will shadow the PUT method in the http client
     *
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @param requestContent the request body
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     * @see HttpClient#put(URL, Map, byte[])
     */
    @Implementation
    public HttpResponse put(@NonNull URL requestUrl,
                            @NonNull Map<String, String> requestHeaders,
                            @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.PUT, requestUrl, requestHeaders, requestContent);
    }

    /**
     * This will shadow the PATCH method in the http client
     *
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @param requestContent the request body
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     * @see HttpClient#patch(URL, Map, byte[])
     */
    @Implementation
    public HttpResponse patch(@NonNull URL requestUrl,
                              @NonNull Map<String, String> requestHeaders,
                              @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.PATCH, requestUrl, requestHeaders, requestContent);
    }

    /**
     * This will shadow the OPTIONS method in the http client
     *
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     * @see HttpClient#options(URL, Map)
     */
    @Implementation
    public HttpResponse options(@NonNull URL requestUrl,
                                @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpClient.HttpMethod.OPTIONS, requestUrl, requestHeaders, null);
    }

    /**
     * This will shadow the POST method in the http client
     *
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @param requestContent the request body content
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     * @see HttpClient#post(URL, Map, byte[])
     */
    @Implementation
    protected HttpResponse post(@NonNull URL requestUrl,
                                @NonNull Map<String, String> requestHeaders,
                                @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.POST, requestUrl, requestHeaders, requestContent);
    }

    /**
     * This will shadow the DELETE method in the http client
     *
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @param requestContent the request body content
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     * @see HttpClient#delete(URL, Map, byte[])
     */
    @Implementation
    public HttpResponse delete(@NonNull URL requestUrl,
                               @NonNull Map<String, String> requestHeaders,
                               @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.DELETE, requestUrl, requestHeaders, requestContent);
    }

    /**
     * This will shadow the GET method in the http client
     *
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     * @see HttpClient#get(URL, Map)
     */
    @Implementation
    public HttpResponse get(@NonNull URL requestUrl,
                            @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpClient.HttpMethod.GET, requestUrl, requestHeaders, null);
    }

    /**
     * This will shadow the HEAD method in the http client
     *
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     * @see HttpClient#head(URL, Map)
     */
    @Implementation
    public HttpResponse head(@NonNull URL requestUrl,
                             @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpClient.HttpMethod.HEAD, requestUrl, requestHeaders, null);
    }

    /**
     * This will shadow the TRACE method in the http client
     *
     * @param requestUrl     the request url
     * @param requestHeaders the request headers
     * @return the mocked response or the actual response
     * @throws IOException throw an IOException when an error occurred
     * @see ShadowHttpClient#method(HttpClient.HttpMethod, URL, Map, byte[])
     * @see HttpClient#trace(URL, Map)
     */
    @Implementation
    public HttpResponse trace(@NonNull URL requestUrl,
                              @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpClient.HttpMethod.TRACE, requestUrl, requestHeaders, null);
    }

}
