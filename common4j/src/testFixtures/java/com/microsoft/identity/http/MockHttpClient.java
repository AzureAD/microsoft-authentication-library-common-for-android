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
import com.microsoft.identity.common.java.net.HttpClient.HttpMethod;
import com.microsoft.identity.common.java.net.HttpRequest;
import com.microsoft.identity.common.java.net.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Predicate;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.NonNull;

/**
 * Class to set a mock request interceptor at runtime.
 */
public class MockHttpClient {

    /**
     * Store a map with HttpRequestMatcher as the key. This allows us to have different
     * interceptors for matching different request patterns.
     */
    private static final Map<HttpRequestMatcher, HttpRequestInterceptor> interceptors = new HashMap<>();

    private static final AtomicBoolean sSaveRequests = new AtomicBoolean(false);

    private static final AtomicLong sTotalRequests = new AtomicLong(0);

    public MockHttpClient(final boolean saveRequests) {
        sSaveRequests.set(saveRequests);
    }

    private static final List<HttpRequest> sInterceptedRequests = Collections.synchronizedList(new ArrayList<>());

    public MockHttpClient() {
        this(false);
    }

    /**
     * Installs a mock http client instance to use in providing the request interceptors.
     * <p>
     * Invoke the uninstall() method to remove all the set interceptors
     *
     * @return the mock http client object
     * @see MockHttpClient#uninstall()
     */
    public static MockHttpClient install() {
        sTotalRequests.set(0);
        sInterceptedRequests.clear();
        return new MockHttpClient(false);
    }

    /**
     * Installs a mock http client instance to use in providing the request interceptors.
     * <p>
     * Invoke the uninstall() method to remove all the set interceptors.
     *
     * @return the mock http client object
     * @see MockHttpClient#uninstall()
     */
    public static MockHttpClient installCapturing() {
        sTotalRequests.set(0);
        sInterceptedRequests.clear();
        return new MockHttpClient(true);
    }

    public long getTotalRequests() {
        return sTotalRequests.get();
    }

    public List<HttpRequest> getInterceptedRequests() {
        return new ArrayList<>(sInterceptedRequests);
    }

    /**
     * Will return the http request interceptor that is configured to intercept the request
     *
     * @param method the http method
     * @param url    the request url to intercept
     * @return the http request interceptor configured for the http method and request url
     */
    public static HttpRequestInterceptor getInterceptor(
            @NonNull final HttpMethod method,
            @NonNull final URL url,
            final Map<String, String> requestHeaders,
            final byte[] requestContent
    ) {
        // this is also not quite right, but will work given the current usage model, where getInterceptor is always called
        sTotalRequests.incrementAndGet();
        // for each pair of HttpMethod and url regex
        for (HttpRequestMatcher matcher : interceptors.keySet()) {
            if (matcher.matches(method, url, requestHeaders, requestContent)) {
                // return the http interceptor
                final HttpRequestInterceptor httpRequestInterceptor = interceptors.get(matcher);
                return new HttpRequestInterceptor() {
                    @Override
                    public HttpResponse performIntercept(@NonNull HttpClient.HttpMethod httpMethod, @NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent) throws IOException {
                        if (sSaveRequests.get()) {
                            sInterceptedRequests.add(new HttpRequest(url, requestHeaders, method.name(), requestContent, null));
                        }
                        return httpRequestInterceptor.performIntercept(httpMethod, requestUrl, requestHeaders, requestContent);
                    }
                };
            }
        }
        return null;
    }

    private final List<HttpRequestMatcher> matchers = new ArrayList<>(); // store a list of matchers for a single MockHttpClient object instance

    /**
     * Quickly match all the http requests and respond with the specified http response
     *
     * @param httpResponse the http response
     */
    public void intercept(@NonNull final HttpResponse httpResponse) {
        intercept(HttpRequestMatcher.builder().build(), new HttpRequestInterceptor() {
            @Override
            public HttpResponse performIntercept(
                    @NonNull HttpMethod httpMethod,
                    @NonNull URL requestUrl,
                    @NonNull Map<String, String> requestHeaders,
                    @Nullable byte[] requestContent) throws IOException {
                return httpResponse;
            }
        });
    }

    /**
     * Quickly match all the http requests with the specified interceptor
     *
     * @param interceptor the http request interceptor
     */
    public void intercept(@NonNull final HttpRequestInterceptor interceptor) {
        intercept(HttpRequestMatcher.builder().build(), interceptor);
    }

    /**
     * Quickly match all the http requests that match the url specified
     *
     * @param url         the URL to match
     * @param interceptor the http interceptor
     */
    public void intercept(@NonNull final URL url, @NonNull final HttpRequestInterceptor interceptor) {
        intercept(
                HttpRequestMatcher.builder()
                        .url(new Predicate<URL>() {
                            @Override
                            public boolean test(URL u) {
                                return u.toString().equals(url.toString());
                            }
                        })
                        .build(),
                interceptor
        );
    }

    /**
     * Quickly match all the http requests that match the url and the http method specified
     *
     * @param method      the http method
     * @param url         the request url
     * @param interceptor the request interceptor
     */
    public void intercept(
            @NonNull final HttpMethod method,
            @NonNull final URL url,
            @NonNull final HttpRequestInterceptor interceptor) {
        intercept(
                HttpRequestMatcher.builder()
                        .url(new Predicate<URL>() {
                            @Override
                            public boolean test(URL u) {
                                return u.toString().equals(url.toString());
                            }
                        })
                        .method(new Predicate<HttpMethod>() {
                            @Override
                            public boolean test(HttpMethod m) {
                                return m == method;
                            }
                        }).build(),
                interceptor
        );
    }

    /**
     * Match all the http requests to match the http method specified.
     *
     * @param method      the http method
     * @param interceptor the request interceptor
     */
    public void intercept(
            @NonNull final HttpMethod method,
            @NonNull final HttpRequestInterceptor interceptor
    ) {
        intercept(
                HttpRequestMatcher.builder()
                        .method(new Predicate<HttpMethod>() {
                            @Override
                            public boolean test(HttpMethod m) {
                                return m == method;
                            }
                        })
                        .build(),
                interceptor
        );
    }

    /**
     * Intercept a request and respond with the specified http response
     *
     * @param matcher      the request matcher
     * @param httpResponse the http response
     */
    public void intercept(HttpRequestMatcher matcher, HttpResponse httpResponse) {
        intercept(
                matcher,
                new HttpRequestInterceptor() {
                    @Override
                    public HttpResponse performIntercept(
                            @NonNull HttpMethod httpMethod,
                            @NonNull URL requestUrl,
                            @NonNull Map<String, String> requestHeaders,
                            @Nullable byte[] requestContent) throws IOException {
                        return httpResponse;
                    }
                }
        );
    }

    /**
     * Adds the matcher and interceptor to the MockHttpClient
     *
     * @param matcher     the request matcher
     * @param interceptor the request interceptor
     */
    public void intercept(HttpRequestMatcher matcher, HttpRequestInterceptor interceptor) {
        if (!matchers.contains(matcher)) {
            matchers.add(matcher);
        }
        MockHttpClient.interceptors.put(matcher, interceptor);
    }

    /**
     * Removes all the set http request interceptors for this mock instance
     */
    public void uninstall() {
        for (HttpRequestMatcher matcher : matchers) {
            MockHttpClient.interceptors.remove(matcher);
        }
    }
}
