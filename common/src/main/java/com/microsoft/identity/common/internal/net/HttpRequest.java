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
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

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

    static final String REQUEST_METHOD_GET = "GET";
    static final String REQUEST_METHOD_POST = "POST";
    static final String REQUEST_METHOD_HEAD = "HEAD";
    static final String REQUEST_METHOD_PUT = "PUT";
    static final String REQUEST_METHOD_DELETE = "DELETE";
    static final String REQUEST_METHOD_TRACE = "TRACE";
    static final String REQUEST_METHOD_OPTIONS = "OPTIONS";
    static final String REQUEST_METHOD_PATCH = "PATCH";

    private static final Set<String> HTTP_METHODS = new HashSet<>();

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
    private HttpRequest(@NonNull final URL requestUrl,
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
     * Send post request {@link URL}, headers, post message and the request content type.
     *
     * @param requestUrl         The {@link URL} to make the http request.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Post message sent in the post request.
     * @param requestContentType Request content type.
     * @return HttpResponse
     * @throws IOException throw if error happen during http send request.
     */
    public static HttpResponse sendPost(@NonNull final URL requestUrl,
                                        @NonNull final Map<String, String> requestHeaders,
                                        @Nullable final byte[] requestContent,
                                        @Nullable final String requestContentType)
            throws IOException {
        recordHttpTelemetryEventStart(
                REQUEST_METHOD_POST,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                REQUEST_METHOD_POST,
                requestContent,
                requestContentType
        );

        final HttpResponse response = httpRequest.send();

        recordHttpTelemetryEventEnd(response);

        return response;
    }

    /**
     * Send Get request {@link URL} and request headers.
     *
     * @param requestUrl     The {@link URL} to make the http request.
     * @param requestHeaders Headers used to send the http request.
     * @return HttpResponse
     * @throws IOException throw if service error happen during http request.
     */
    public static HttpResponse sendGet(@NonNull final URL requestUrl,
                                       @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        recordHttpTelemetryEventStart(
                REQUEST_METHOD_GET,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                REQUEST_METHOD_GET
        );

        final HttpResponse response = httpRequest.send();

        recordHttpTelemetryEventEnd(response);

        return response;
    }

    public static HttpResponse sendHead(@NonNull final URL requestUrl,
                                        @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        recordHttpTelemetryEventStart(
                REQUEST_METHOD_HEAD,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                REQUEST_METHOD_HEAD
        );

        final HttpResponse response = httpRequest.send();

        recordHttpTelemetryEventEnd(response);

        return response;
    }

    public static HttpResponse sendPut(@NonNull final URL requestUrl,
                                       @NonNull final Map<String, String> requestHeaders,
                                       @Nullable final byte[] requestContent,
                                       @Nullable final String requestContentType)
            throws IOException {
        recordHttpTelemetryEventStart(
                REQUEST_METHOD_PUT,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                REQUEST_METHOD_PUT,
                requestContent,
                requestContentType
        );

        final HttpResponse response = httpRequest.send();

        recordHttpTelemetryEventEnd(response);

        return response;
    }

    public static HttpResponse sendDelete(@NonNull final URL requestUrl,
                                          @NonNull final Map<String, String> requestHeaders,
                                          @Nullable final byte[] requestContent,
                                          @Nullable final String requestContentType)
            throws IOException {
        recordHttpTelemetryEventStart(
                REQUEST_METHOD_DELETE,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                REQUEST_METHOD_DELETE,
                requestContent,
                requestContentType
        );

        final HttpResponse response = httpRequest.send();

        recordHttpTelemetryEventEnd(response);

        return response;
    }

    public static HttpResponse sendTrace(@NonNull final URL requestUrl,
                                         @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        recordHttpTelemetryEventStart(
                REQUEST_METHOD_TRACE,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                REQUEST_METHOD_TRACE
        );

        final HttpResponse response = httpRequest.send();

        recordHttpTelemetryEventEnd(response);

        return response;
    }

    public static HttpResponse sendOptions(@NonNull final URL requestUrl,
                                           @NonNull final Map<String, String> requestHeaders)
            throws IOException {
        recordHttpTelemetryEventStart(
                REQUEST_METHOD_OPTIONS,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                REQUEST_METHOD_OPTIONS
        );

        final HttpResponse response = httpRequest.send();

        recordHttpTelemetryEventEnd(response);

        return response;
    }

    public static HttpResponse sendPatch(@NonNull final URL requestUrl,
                                         @NonNull final Map<String, String> requestHeaders,
                                         @Nullable final byte[] requestContent,
                                         @Nullable final String requestContentType)
            throws IOException {
        recordHttpTelemetryEventStart(
                REQUEST_METHOD_PATCH,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        // Because HttpURLConnection predates RFC-5789, we need to fallback on POST w/ a backcompat
        // workaround. See: https://stackoverflow.com/a/32503192/741827
        requestHeaders.put("X-HTTP-Method-Override", "PATCH");

        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                REQUEST_METHOD_POST, // HttpURLConnection doesn't natively support PATCH
                requestContent,
                requestContentType
        );

        final HttpResponse response = httpRequest.send();

        recordHttpTelemetryEventEnd(response);

        return response;
    }

    public static HttpResponse sendWithMethod(@NonNull final String httpMethod,
                                              @NonNull final URL requestUrl,
                                              @NonNull final Map<String, String> requestHeaders,
                                              @Nullable final byte[] requestContent,
                                              @Nullable final String requestContentType)
            throws IOException {
        // Validate the HTTP method...
        if (TextUtils.isEmpty(httpMethod)) {
            throw new IllegalArgumentException("HTTP method cannot be null or blank");
        }

        String normalizedHttpMethod = httpMethod.toUpperCase(Locale.US);

        if (!HTTP_METHODS.contains(normalizedHttpMethod)) {
            throw new IllegalArgumentException("Unknown or unsupported HTTP method: " + httpMethod);
        }

        // Log Telemetry event start
        recordHttpTelemetryEventStart(
                normalizedHttpMethod,
                requestUrl,
                requestHeaders.get(CLIENT_REQUEST_ID)
        );

        // Apply special backcompat behaviors for PATCH, if reqd
        if (REQUEST_METHOD_PATCH.equals(normalizedHttpMethod)) {
            // Because HttpURLConnection predates RFC-5789, we need to fallback on POST w/ a backcompat
            // workaround. See: https://stackoverflow.com/a/32503192/741827
            normalizedHttpMethod = REQUEST_METHOD_POST;
            requestHeaders.put("X-HTTP-Method-Override", "PATCH");
        }

        // Place request
        final HttpRequest httpRequest = new HttpRequest(
                requestUrl,
                requestHeaders,
                normalizedHttpMethod, // HttpURLConnection doesn't natively support PATCH
                requestContent,
                requestContentType
        );

        final HttpResponse response = httpRequest.send();

        // Log Telemetry event end
        recordHttpTelemetryEventEnd(response);

        // Return the result
        return response;
    }

    /**
     * Send http request.
     */
    private HttpResponse send() throws IOException {
        final HttpResponse response = sendWithRetry();

        if (response != null && isRetryableError(response.getStatusCode())) {
            throw new UnknownServiceException("Retry failed again with 500/503/504");
        }

        return response;
    }

    /**
     * Execute the send request, and retry if needed. Retry happens on all the endpoint when
     * receiving {@link SocketTimeoutException} or retryable error 500/503/504.
     */
    private HttpResponse sendWithRetry() throws IOException {
        final HttpResponse httpResponse;

        try {
            httpResponse = executeHttpSend();
        } catch (final SocketTimeoutException socketTimeoutException) {
            // In android, network timeout is thrown as the SocketTimeOutException, we need to
            // catch this and perform retry. If retry also fails with timeout, the
            // socketTimeoutException will be bubbled up
            waitBeforeRetry();
            return executeHttpSend();
        }

        if (isRetryableError(httpResponse.getStatusCode())) {
            // retry if we get 500/503/504
            waitBeforeRetry();
            return executeHttpSend();
        }

        return httpResponse;
    }

    private HttpResponse executeHttpSend() throws IOException {
        final HttpURLConnection urlConnection = setupConnection();
        urlConnection.setRequestMethod(mRequestMethod);
        urlConnection.setUseCaches(true);
        setRequestBody(urlConnection, mRequestContent, mRequestContentType);

        InputStream responseStream = null;

        final HttpResponse response;
        try {
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

    /**
     * Having the thread wait for 1 second before doing the retry to avoid hitting server
     * immediately.
     */
    private void waitBeforeRetry() {
        try {
            Thread.sleep(RETRY_TIME_WAITING_PERIOD_MSEC);
        } catch (final InterruptedException interrupted) {
            //Fail the have the thread waiting for 1 second before doing the retry
        }
    }
}