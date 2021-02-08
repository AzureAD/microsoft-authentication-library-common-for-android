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

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.net.HttpClient.HttpMethod;
import com.microsoft.identity.common.internal.net.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to set a mock request interceptor at runtime.
 */
public class MockHttpClient {

    /**
     * Store a map with Pair<HttpMethod, urlRegex>  as the key. This allows us to have different
     * interceptors for methods with a particular url pattern.
     * When the HttpMethod in the Pair is null, we will match all requests specified by the urlRegex
     * regardless of the http method.
     */
    private static final Map<Pair<HttpMethod, String>, HttpRequestInterceptor> interceptors = new HashMap<>();

    /**
     * Will set the interceptor for all methods and request urls
     *
     * @param interceptor  the http request interceptor
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, String)
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, HttpMethod)
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, HttpMethod, String)
     */
    public static void setInterceptor(@NonNull final HttpRequestInterceptor interceptor) {
        MockHttpClient.setInterceptor(interceptor, null, ".*");
    }

    /**
     * Will set the request interceptor for the specified url regex and all http methods.
     *
     * @param interceptor  the http interceptor
     * @param urlRegex     the url pattern in regex
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor)
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, HttpMethod)
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, HttpMethod, String)
     */
    public static void setInterceptor(@NonNull final HttpRequestInterceptor interceptor,
                                      @NonNull String urlRegex) {
        MockHttpClient.setInterceptor(interceptor, null, urlRegex);
    }

    /**
     * Will set the request interceptor for the specified http method and all urls .
     *
     * @param interceptor  the http interceptor
     * @param method       the request method
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor)
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, String)
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, HttpMethod, String)
     */
    public static void setInterceptor(@NonNull final HttpRequestInterceptor interceptor,
                                      @NonNull HttpMethod method) {
        MockHttpClient.setInterceptor(interceptor, method, ".*");
    }

    /**
     * Will set the request interceptor for the specified http method and url regex pattern
     *
     * @param interceptor  the http interceptor
     * @param method       the request method
     * @param urlRegex     the url regex pattern.
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor)
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, String)
     * @see MockHttpClient#setInterceptor(HttpRequestInterceptor, HttpMethod)
     */
    public static void setInterceptor(@NonNull final HttpRequestInterceptor interceptor,
                                      @Nullable final HttpMethod method,
                                      @NonNull final String urlRegex) {
        interceptors.put(new Pair<>(method, urlRegex), interceptor);
    }

    /**
     * Will return the http request interceptor that is configured to intercept the HttpMethod and
     * request url specified
     *
     * @param method  the http method
     * @param url     the request url to intercept
     * @return  the http request interceptor configured for the http method and request url
     */
    public static HttpRequestInterceptor intercept(@NonNull final HttpMethod method, @NonNull final URL url) {
        // for each pair of HttpMethod and url regex
        for (Pair<HttpMethod, String> pair : interceptors.keySet()) {
            // if url matches and the HttpMethod matches (or is null)  the interceptor has been found.
            if ((pair.first == null || pair.first.compareTo(method) == 0) && url.toString().matches(pair.second)) {
                // return the http interceptor
                return interceptors.get(pair);
            }
        }
        return null;
    }

    /**
     * A shorthand for intercepting http request by giving the response for all outgoing requests.
     *
     * @param httpResponse  the http response
     * @see MockHttpClient#setHttpResponse(HttpResponse, String)
     * @see MockHttpClient#setHttpResponse(HttpResponse, HttpMethod)
     * @see MockHttpClient#setHttpResponse(HttpResponse, HttpMethod, String)
     */
    public static void setHttpResponse(@NonNull final HttpResponse httpResponse) {
        MockHttpClient.setHttpResponse(httpResponse, null, ".*");
    }

    /**
     * A shorthand for intercepting http requests by giving the response for all outgoing
     * requests with the specified http method
     *
     * @param httpResponse  the http response
     * @param method        the method that will be returning the response
     * @see MockHttpClient#setHttpResponse(HttpResponse)
     * @see MockHttpClient#setHttpResponse(HttpResponse, HttpMethod)
     * @see MockHttpClient#setHttpResponse(HttpResponse, HttpMethod, String)
     */
    public static void setHttpResponse(@NonNull final HttpResponse httpResponse, @NonNull HttpMethod method) {
        MockHttpClient.setHttpResponse(httpResponse, method, ".*");
    }

    /**
     * A shorthand for intercepting http requests by giving the response for all outgoing
     * requests that match the specified url regex.
     *
     * @param httpResponse  the http response
     * @param urlRegex      the url regex to match requests
     * @see MockHttpClient#setHttpResponse(HttpResponse)
     * @see MockHttpClient#setHttpResponse(HttpResponse, HttpMethod)
     * @see MockHttpClient#setHttpResponse(HttpResponse, HttpMethod, String)
     */
    public static void setHttpResponse(@NonNull final HttpResponse httpResponse, @NonNull final String urlRegex) {
        MockHttpClient.setHttpResponse(httpResponse, null, urlRegex);
    }

    /**
     * A shorthand for intercepting requests by giving the response for all outgoing requests that
     * match the specified url regex and http method
     *
     * @param httpResponse  the http response
     * @param method        the request method
     * @param urlRegex      the url regex pattern to match
     * @see MockHttpClient#setHttpResponse(HttpResponse)
     * @see MockHttpClient#setHttpResponse(HttpResponse, String)
     * @see MockHttpClient#setHttpResponse(HttpResponse, HttpMethod)
     */
    public static void setHttpResponse(@NonNull final HttpResponse httpResponse,
                                       @Nullable final HttpMethod method,
                                       @NonNull final String urlRegex) {
        MockHttpClient.setInterceptor(new HttpRequestInterceptor() {
            @Override
            public HttpResponse intercept(@NonNull HttpMethod httpMethod,
                                          @NonNull URL requestUrl,
                                          @NonNull Map<String, String> requestHeaders,
                                          @Nullable byte[] requestContent) {
                return httpResponse;
            }
        }, method, urlRegex);
    }

    /**
     * Clears the existing interceptors
     */
    public static void reset() {
        MockHttpClient.interceptors.clear();
    }
}
