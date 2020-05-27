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
package com.microsoft.identity.common.internal.net;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;
import androidx.core.util.Consumer;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.HttpEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.HttpStartEvent;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownServiceException;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import lombok.SneakyThrows;

import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.CLIENT_REQUEST_ID;
import static com.microsoft.identity.common.internal.net.HttpUrlConnectionFactory.createHttpURLConnection;

/**
 * Internal class for handling http request.
 */
public final class HttpRequest {

    private static final String HOST = "Host";

    /**
     * The waiting time before doing retry to prevent hitting the server immediately failure.
     */
    private static final int RETRY_TIME_WAITING_PERIOD_MSEC = 1000;
    private static final int STREAM_BUFFER_SIZE = 1024;
    private static final HttpClient DEFAULT_HTTP_CLIENT = HttpClient.builder()
            .connectTimeoutMs(RETRY_TIME_WAITING_PERIOD_MSEC)
            .readTimeoutMs(RETRY_TIME_WAITING_PERIOD_MSEC)
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

    /**
     * Value of read timeout in milliseconds.
     */
    public static int READ_TIMEOUT = 30000;

    /**
     * Value of connect timeout in milliseconds.
     */
    public static int CONNECT_TIMEOUT = 30000;

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
    HttpRequest(@NonNull final URL requestUrl,
                @NonNull final Map<String, String> requestHeaders,
                @NonNull final String requestMethod,
                @Nullable final byte[] requestContent,
                @Nullable final String requestContentType) {
        mRequestUrl = requestUrl;
        mRequestHeaders.put(HOST, requestUrl.getAuthority());
        mRequestHeaders.putAll(requestHeaders);
        mRequestMethod = requestMethod;
        mRequestContent = requestContent;
        mRequestContentType = requestContentType;
    }

    private static void recordHttpTelemetryEventStart(@NonNull final String requestMethod,
                                                      @NonNull final URL requestUrl,
                                                      @Nullable final String requestId) {
        Telemetry.emit(
                new HttpStartEvent()
                        .putMethod(requestMethod)
                        .putPath(requestUrl)
                        .putRequestIdHeader(requestId)
        );
    }

    private static void recordHttpTelemetryEventEnd(@Nullable final HttpResponse response) {
        final HttpEndEvent httpEndEvent = new HttpEndEvent();

        if (null != response) {
            httpEndEvent.putStatusCode(response.getStatusCode());
        }

        Telemetry.emit(httpEndEvent);
    }

    /**
     * Send a POST request {@link URL}, headers, and post message.  The request content type
     * will be taken from the appropriate header field.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Post message sent in the post request.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    @Deprecated
    public static HttpResponse sendPostWithoutRetries(@NonNull final URL requestUrl,
                                        @NonNull final Map<String, String> requestHeaders,
                                        @Nullable final byte[] requestContent)
            throws IOException {
        return sendWithMethod_v2(
                REQUEST_METHOD_POST,
                requestUrl,
                requestHeaders,
                requestContent
        );
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
                                        @Nullable final byte[] requestContent,
                                        @Nullable final String requestContentType)
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
    public static HttpResponse sendGetWithoutRetries(@NonNull final URL requestUrl,
                                       @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        return sendWithMethod_v2(
                REQUEST_METHOD_GET,
                requestUrl,
                requestHeaders,
                null
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
    public static HttpResponse sendHeadWithoutRetries(@NonNull final URL requestUrl,
                                        @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        return sendWithMethod_v2(
                REQUEST_METHOD_HEAD,
                requestUrl,
                requestHeaders,
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
     * Send a PUT request {@link URL}, headers, and post message.  The content type will
     * be taken from the appropriate header field.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    public static HttpResponse sendPutWithoutRetries(@NonNull final URL requestUrl,
                                       @NonNull final Map<String, String> requestHeaders,
                                       @Nullable final byte[] requestContent)
            throws IOException {
        return sendWithMethod_v2(
                REQUEST_METHOD_PUT,
                requestUrl,
                requestHeaders,
                requestContent
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
                                       @Nullable final byte[] requestContent,
                                       @Nullable final String requestContentType)
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
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    public static HttpResponse sendDeleteWithoutRetries(@NonNull final URL requestUrl,
                                          @NonNull final Map<String, String> requestHeaders,
                                          @Nullable final byte[] requestContent)
            throws IOException {
        return sendWithMethod_v2(
                REQUEST_METHOD_DELETE,
                requestUrl,
                requestHeaders,
                requestContent
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
                                          @Nullable final byte[] requestContent,
                                          @Nullable final String requestContentType)
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
    public static HttpResponse sendTraceWithoutRetries(@NonNull final URL requestUrl,
                                         @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        return sendWithMethod_v2(
                REQUEST_METHOD_TRACE,
                requestUrl,
                requestHeaders,
                null
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
    public static HttpResponse sendOptionsWithoutRetries(@NonNull final URL requestUrl,
                                           @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        return sendWithMethod_v2(
                REQUEST_METHOD_OPTIONS,
                requestUrl,
                requestHeaders,
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
                                         @Nullable final byte[] requestContent,
                                         @Nullable final String requestContentType)
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
     * Send a PATCH request {@link URL}, headers, post message.  The content type will be taken
     * from the request headers.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    public static HttpResponse sendPatchWithouRetries(@NonNull final URL requestUrl,
                                                      @NonNull final Map<String, String> requestHeaders,
                                                      @Nullable final byte[] requestContent)
            throws IOException {
        return sendWithMethod_v2(
                REQUEST_METHOD_PATCH,
                requestUrl,
                requestHeaders,
                requestContent
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
                                              @Nullable final byte[] requestContent,
                                              @Nullable final String requestContentType)
            throws IOException {
        if (requestContentType != null) {
            return DEFAULT_HTTP_CLIENT.method(httpMethod, requestUrl, requestHeaders, requestContent);
        } else {
            Map<String, String> headerMap = new LinkedHashMap<>(requestHeaders);
            if (requestContentType != null) {
                headerMap.put(HttpConstants.HeaderField.CONTENT_TYPE, requestContentType);
            }
            return DEFAULT_HTTP_CLIENT.method(httpMethod, requestUrl, requestHeaders, requestContent);
        }
    }

    /**
     * Sends an HTTP request of the specified method; applies appropriate provided arguments where
     * applicable.
     *
     * @param httpMethod         One of: GET, POST, HEAD, PUT, DELETE, TRACE, OPTIONS, PATCH.
     * @param requestUrl         The recipient {@link URL}.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @return HttpResponse
     * @throws IOException If an error is encountered while servicing this request.
     */
    public static HttpResponse sendWithMethod_v2(@NonNull final String httpMethod,
                                                 @NonNull final URL requestUrl,
                                                 @NonNull final Map<String, String> requestHeaders,
                                                 @Nullable final byte[] requestContent)
            throws IOException {
        return getHttpResponseInternal(httpMethod, requestUrl, requestHeaders, requestContent, null,
                new Function<HttpRequest, HttpResponse>() {
            @SneakyThrows
            @Override
            public HttpResponse apply(HttpRequest input) {
                return input.send(new Consumer<HttpResponse>(){
                    @Override
                    public void accept(HttpResponse httpResponse) {
                        recordHttpTelemetryEventEnd(httpResponse);
                    }
                });
            }
        });
    }

    private static HttpResponse getHttpResponseInternal(@NonNull String httpMethod, @NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent, @Nullable String requestContentType,
                                                        Function<HttpRequest, HttpResponse> producer) throws IOException {
        // Validate the HTTP method...
        String normalizedHttpMethod = validateAndNormalizeMethod(httpMethod);

        // Log Telemetry event start
        recordHttpTelemetryEventStart(
                normalizedHttpMethod,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );
        final HttpRequest httpRequest = constructHttpRequest(normalizedHttpMethod, requestUrl, requestHeaders, requestContent, requestContentType);

        return producer.apply(httpRequest);
    }

    private static HttpRequest constructHttpRequest(@NonNull String httpMethod, @NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent, @Nullable String requestContentType) {

        // Apply special backcompat behaviors for PATCH, if reqd
        if (REQUEST_METHOD_PATCH.equals(httpMethod)) {
            // Because HttpURLConnection predates RFC-5789, we need to fallback on POST w/ a backcompat
            // workaround. See: https://stackoverflow.com/a/32503192/741827
            httpMethod = REQUEST_METHOD_POST;
            // This map may be immutable.
            requestHeaders = new HashMap<>(requestHeaders);
            requestHeaders.put("X-HTTP-Method-Override", httpMethod);
        }

        // Construct request
        return new HttpRequest(
                requestUrl,
                requestHeaders,
                httpMethod, // HttpURLConnection doesn't natively support PATCH
                requestContent,
                requestContentType
        );
    }

    private static String validateAndNormalizeMethod(@NonNull final String httpMethod) {
        if (TextUtils.isEmpty(httpMethod)) {
            throw new IllegalArgumentException("HTTP method cannot be null or blank");
        }

        String normalizedHttpMethod = httpMethod.toUpperCase(Locale.US);

        if (!HTTP_METHODS.contains(normalizedHttpMethod)) {
            throw new IllegalArgumentException("Unknown or unsupported HTTP method: " + httpMethod);
        }
        return normalizedHttpMethod;
    }

    /**
     * Send http request.
     */
    private HttpResponse send(Consumer<HttpResponse> completionCallback) throws IOException {
        return executeHttpSend(false, completionCallback);
    }

    private HttpResponse executeHttpSend(boolean maskException, Consumer<HttpResponse> completionCallback) throws IOException {
        final HttpURLConnection urlConnection = setupConnection();
        urlConnection.setRequestMethod(mRequestMethod);
        urlConnection.setUseCaches(true);
        setRequestBody(urlConnection, mRequestContent, mRequestContentType);

        InputStream responseStream = null;

        HttpResponse response = null;
        try {
            if (maskException) {
                try {
                    responseStream = urlConnection.getInputStream();
                } catch (final SocketTimeoutException socketTimeoutException) {
                    // SocketTimeoutExcetion is thrown when connection timeout happens. For connection
                    // timeout, we want to retry once. Throw the exception to the upper layer, and the
                    // upper layer will handle the rety.
                    throw socketTimeoutException;
                } catch (final IOException ioException) {
                    responseStream = urlConnection.getErrorStream();
                }
            } else {
                responseStream = urlConnection.getInputStream();
            }

            final int statusCode = urlConnection.getResponseCode();
            final Date date = new Date(urlConnection.getDate());

            final String responseBody = responseStream == null
                    ? ""
                    : convertStreamToString(responseStream);

            response = new HttpResponse(
                    date,
                    statusCode,
                    responseBody,
                    urlConnection.getHeaderFields()
            );
        } finally {
            completionCallback.accept(response);
            safeCloseStream(responseStream);
        }

        return response;
    }

    private HttpURLConnection setupConnection() throws IOException {
        final HttpURLConnection urlConnection = createHttpURLConnection(mRequestUrl);

        // Apply request headers and update the headers with default attributes first
        final Set<Map.Entry<String, String>> headerEntries = mRequestHeaders.entrySet();

        for (final Map.Entry<String, String> entry : headerEntries) {
            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        urlConnection.setConnectTimeout(CONNECT_TIMEOUT);
        urlConnection.setReadTimeout(READ_TIMEOUT);
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setUseCaches(false);
        urlConnection.setDoInput(true);

        return urlConnection;
    }

    private static void setRequestBody(@NonNull final HttpURLConnection connection,
                                       @Nullable final byte[] contentRequest,
                                       @Nullable final String requestContentType) throws IOException {
        if (contentRequest == null) {
            return;
        }

        connection.setDoOutput(true);

        if (!StringUtil.isEmpty(requestContentType)) {
            connection.setRequestProperty("Content-Type", requestContentType);
        }

        connection.setRequestProperty("Content-Length", String.valueOf(contentRequest.length));

        OutputStream out = null;

        try {
            out = connection.getOutputStream();
            out.write(contentRequest);
        } finally {
            safeCloseStream(out);
        }
    }

    /**
     * Convert stream into the string.
     *
     * @param inputStream {@link InputStream} to be converted to be a string.
     * @return The converted string
     * @throws IOException Thrown when failing to access inputStream stream.
     */
    private static String convertStreamToString(final InputStream inputStream) throws IOException {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
            final char[] buffer = new char[STREAM_BUFFER_SIZE];
            final StringBuilder stringBuilder = new StringBuilder();
            int charsRead;

            while ((charsRead = reader.read(buffer)) > -1) {
                stringBuilder.append(buffer, 0, charsRead);
            }

            return stringBuilder.toString();
        } finally {
            safeCloseStream(inputStream);
        }
    }

    /**
     * Close the stream safely.
     *
     * @param stream stream to be closed
     */
    private static void safeCloseStream(@Nullable final Closeable stream) {
        if (stream == null) {
            return;
        }

        try {
            stream.close();
        } catch (final IOException e) {
            //Encountered IO exception when trying to close the stream"
        }
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