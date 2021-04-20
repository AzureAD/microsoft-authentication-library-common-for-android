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
package com.microsoft.identity.common.java.internal.net;

import com.microsoft.identity.common.java.util.ported.Function;
import lombok.NonNull;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static com.microsoft.identity.common.java.internal.net.UrlConnectionHttpClient.Supplier;

/**
 * Internal class for handling http request.
 */
public class HttpRequest {

    private static final String HOST = "Host";

    /**
     * Value of read timeout in milliseconds.
     */
    @Deprecated
    public static int READ_TIMEOUT = 30000;

    /**
     * Value of connect timeout in milliseconds.
     */
    @Deprecated
    public static int CONNECT_TIMEOUT = 30000;

    /**
     * The waiting time before doing retry to prevent hitting the server immediately failure.
     */
    private static final int RETRY_TIME_WAITING_PERIOD_MSEC = 1000;
    private static final int STREAM_BUFFER_SIZE = 1024;
    private static final HttpClient DEFAULT_HTTP_CLIENT = UrlConnectionHttpClient.builder()
            .connectTimeoutMsSupplier(new Supplier<Integer>() { public Integer get() { return CONNECT_TIMEOUT; }})
            .readTimeoutMsSupplier(new Supplier<Integer>() { public Integer get() { return READ_TIMEOUT; }})
            .streamBufferSize(STREAM_BUFFER_SIZE)
            .retryPolicy(new StatusCodeAndExceptionRetry.StatusCodeAndExceptionRetryBuilder()
                    .number(1)
                    .extensionFactor(2)
                    .isAcceptable(new Function<HttpResponse, Boolean>() {
                        public Boolean apply(HttpResponse response) {
                            return response != null && response.getStatusCode() < 400;
                        }
                    })
                    .initialDelay(RETRY_TIME_WAITING_PERIOD_MSEC)
                    .isRetryable(new Function<HttpResponse, Boolean>() {
                        public Boolean apply(HttpResponse response) {
                            return response != null && HttpRequest.isRetryableError(response.getStatusCode());
                        }
                    })
                    .isRetryableException(new Function<Exception, Boolean>() {
                        public Boolean apply(Exception e) {
                            return e instanceof SocketTimeoutException;
                        }
                    })
                    .build())
            .build();

    public static final String REQUEST_METHOD_GET = "GET";
    public static final String REQUEST_METHOD_POST = "POST";
    public static final String REQUEST_METHOD_HEAD = "HEAD";
    public static final String REQUEST_METHOD_PUT = "PUT";
    public static final String REQUEST_METHOD_DELETE = "DELETE";
    public static final String REQUEST_METHOD_TRACE = "TRACE";
    public static final String REQUEST_METHOD_OPTIONS = "OPTIONS";
    public static final String REQUEST_METHOD_PATCH = "PATCH";

    private static final Set<String> HTTP_METHODS = new LinkedHashSet<>();

    static {
        HTTP_METHODS.add(REQUEST_METHOD_GET);
        HTTP_METHODS.add(REQUEST_METHOD_POST);
        HTTP_METHODS.add(REQUEST_METHOD_HEAD);
        HTTP_METHODS.add(REQUEST_METHOD_PUT);
        HTTP_METHODS.add(REQUEST_METHOD_DELETE);
        HTTP_METHODS.add(REQUEST_METHOD_TRACE);
        HTTP_METHODS.add(REQUEST_METHOD_OPTIONS);
        HTTP_METHODS.add(REQUEST_METHOD_PATCH);
    }

    // class variables
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

    Map<String, String> getmRequestHeaders() {
        return Collections.unmodifiableMap(mRequestHeaders);
    }

    private final String mRequestContentType;
    private final String mRequestMethod;
    private final Map<String, String> mRequestHeaders = new HashMap<>();

    /**
     * Constructor for {@link HttpRequest} with request {@link URL} and request headers.
     *
     * @param requestUrl     The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     */
    private HttpRequest(@NonNull final URL requestUrl,
                        @NonNull final Map<String, String> requestHeaders,
                        @NonNull final String requestMethod) {
        this(requestUrl, requestHeaders, requestMethod, null, null);
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
        mRequestContent = requestContent;
        mRequestContentType = requestContentType;
    }

    /**
     * Send a POST request {@link URL}, headers, post message and the request content type.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Post message sent in the post request.
     * @param requestContentType Request content type.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    @Deprecated
    public static HttpResponse sendPost(@NonNull final URL requestUrl,
                                        @NonNull final Map<String, String> requestHeaders,
                                        final byte[] requestContent,
                                        final String requestContentType)
            throws IOException {
        return sendWithMethod(
                REQUEST_METHOD_POST,
                requestUrl,
                requestHeaders,
                requestContent,
                requestContentType
        );
    }

    /**
     * Send a GET request {@link URL} and request headers.
     *
     * @param requestUrl     The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     * @return HttpResponse
     * @throws IOException throw if service error happen during http request.
     */
    @Deprecated
    public static HttpResponse sendGet(@NonNull final URL requestUrl,
                                       @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        return sendWithMethod(
                REQUEST_METHOD_GET,
                requestUrl,
                requestHeaders,
                null,
                null
        );
    }

    /**
     * Send a HEAD request {@link URL}, headers, post message and the request content type.
     *
     * @param requestUrl     The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    @Deprecated
    public static HttpResponse sendHead(@NonNull final URL requestUrl,
                                        @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        return sendWithMethod(
                REQUEST_METHOD_HEAD,
                requestUrl,
                requestHeaders,
                null,
                null
        );
    }

    /**
     * Send a PUT request {@link URL}, headers, post message and the request content type.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @param requestContentType Request content type.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    @Deprecated
    public static HttpResponse sendPut(@NonNull final URL requestUrl,
                                       @NonNull final Map<String, String> requestHeaders,
                                       final byte[] requestContent,
                                       final String requestContentType)
            throws IOException {
        return sendWithMethod(
                REQUEST_METHOD_PUT,
                requestUrl,
                requestHeaders,
                requestContent,
                requestContentType
        );
    }

    /**
     * Send a DELETE request {@link URL}, headers, post message and the request content type.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @param requestContentType Request content type.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    @Deprecated
    public static HttpResponse sendDelete(@NonNull final URL requestUrl,
                                          @NonNull final Map<String, String> requestHeaders,
                                          final byte[] requestContent,
                                          final String requestContentType)
            throws IOException {
        return sendWithMethod(
                REQUEST_METHOD_DELETE,
                requestUrl,
                requestHeaders,
                requestContent,
                requestContentType
        );
    }

    /**
     * Send a TRACE request {@link URL}, headers, post message and the request content type.
     *
     * @param requestUrl     The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    @Deprecated
    public static HttpResponse sendTrace(@NonNull final URL requestUrl,
                                         @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        return sendWithMethod(
                REQUEST_METHOD_TRACE,
                requestUrl,
                requestHeaders,
                null,
                null
        );
    }

    /**
     * Send an OPTIONS request {@link URL}, headers, post message and the request content type.
     *
     * @param requestUrl     The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    @Deprecated
    public static HttpResponse sendOptions(@NonNull final URL requestUrl,
                                           @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        return sendWithMethod(
                REQUEST_METHOD_OPTIONS,
                requestUrl,
                requestHeaders,
                null,
                null
        );
    }

    /**
     * Send a PATCH request {@link URL}, headers, post message and the request content type.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @param requestContentType Request content type.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    @Deprecated
    public static HttpResponse sendPatch(@NonNull final URL requestUrl,
                                         @NonNull final Map<String, String> requestHeaders,
                                         final byte[] requestContent,
                                         final String requestContentType)
            throws IOException {
        return sendWithMethod(
                REQUEST_METHOD_PATCH,
                requestUrl,
                requestHeaders,
                requestContent,
                requestContentType
        );
    }

    /**
     * Sends an HTTP request of the specified method; applies appropriate provided arguments where
     * applicable.
     *
     * @param httpMethod         One of: GET, POST, HEAD, PUT, DELETE, TRACE, OPTIONS, PATCH.
     * @param requestUrl         The recipient {@link URL}.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @param requestContentType Optional request content type (may also be supplied via header).
     * @return HttpResponse
     * @throws IOException If an error is encountered while servicing this request.
     */
    @Deprecated
    public static HttpResponse sendWithMethod(@NonNull final String httpMethod,
                                              @NonNull final URL requestUrl,
                                              @NonNull final Map<String, String> requestHeaders,
                                              final byte[] requestContent,
                                              final String requestContentType)
            throws IOException {
        Map<String, String> headerMap = requestHeaders;
        if (requestContentType != null) {
            headerMap = new LinkedHashMap<>(headerMap);
            if (requestContentType != null) {
                headerMap.put(HttpConstants.HeaderField.CONTENT_TYPE, requestContentType);
            }
        }
        HttpResponse response = DEFAULT_HTTP_CLIENT.method(httpMethod, requestUrl, headerMap, requestContent);
        if (response != null && isRetryableError(response.getStatusCode())) {
            throw new UnknownServiceException("Retry failed again with 500/503/504");
        }
        return response;
    }

    /**
     * Check if the given status code is the retryable status code(500/503/504).
     *
     * @param statusCode The status to check.
     * @return True if the status code is 500, 503 or 504, false otherwise.
     */
    private static boolean isRetryableError(final int statusCode) {
        return statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR
                || statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT
                || statusCode == HttpURLConnection.HTTP_UNAVAILABLE;
    }
}