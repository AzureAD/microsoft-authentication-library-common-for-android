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

import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.logging.Logger;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;
import com.microsoft.identity.common.java.opentelemetry.SpanExtension;
import com.microsoft.identity.common.java.telemetry.Telemetry;
import com.microsoft.identity.common.java.telemetry.events.HttpEndEvent;
import com.microsoft.identity.common.java.telemetry.events.HttpStartEvent;
import com.microsoft.identity.common.java.util.StringUtil;
import com.microsoft.identity.common.java.util.ported.Consumer;
import com.microsoft.identity.common.java.util.ported.Function;
import com.microsoft.identity.common.java.util.ported.Supplier;

import net.jcip.annotations.ThreadSafe;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import javax.annotation.Nullable;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;

import io.opentelemetry.api.trace.Span;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NonNull;

import static com.microsoft.identity.common.java.AuthenticationConstants.AAD.CLIENT_REQUEST_ID;
import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.CONTENT_TYPE;
import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.XMS_CCS_REQUEST_ID;
import static com.microsoft.identity.common.java.net.HttpConstants.HeaderField.XMS_CCS_REQUEST_SEQUENCE;

/**
 * A client object for handling HTTP requests and responses.  This class accepts a RetryPolicy that
 * is applied to the responses.  By default, the policy is a null policy not retrying anything.  The
 * default settings for read timeout and connection timeout are 30s, and the default setting for the
 * size of the buffer for reading objects from the http stream is 1024 bytes.
 * <p>
 * There are two ways to supply timeout values to this class, one method takes suppliers, the other
 * one integers.  If you use both of these, the suppliers method will take precendence over the method
 * using integers.
 * <p>
 * TODO: add telemetry for exceptions/intermediary failures in this class.
 */
@AllArgsConstructor
@ThreadSafe
public class UrlConnectionHttpClient extends AbstractHttpClient {
    private static final String TAG = UrlConnectionHttpClient.class.getSimpleName();

    protected static final int RETRY_TIME_WAITING_PERIOD_MSEC = 1000;
    protected static final int DEFAULT_CONNECT_TIME_OUT_MS = 60000;
    protected static final int DEFAULT_READ_TIME_OUT_MS = 60000;
    protected static final int DEFAULT_STREAM_BUFFER_SIZE_BYTE = 1024;

    private static final transient AtomicReference<UrlConnectionHttpClient> defaultReference = new AtomicReference<>(null);

    /**
     * Retry policy of this HttpClient. Default is {@link NoRetryPolicy}.
     */
    private final IRetryPolicy<HttpResponse> retryPolicy;

    /**
     * Size of the stream buffer.
     */
    private final int streamBufferSize;

    /**
     * To be set in {@link java.net.URLConnection#setConnectTimeout(int)}.
     * If {@link UrlConnectionHttpClient#connectTimeoutMsSupplier} is provided, the value will be loaded from there instead.
     * Default is {@link UrlConnectionHttpClient#DEFAULT_CONNECT_TIME_OUT_MS}.
     */
    private final int connectTimeoutMs;

    /**
     * To be set in {@link java.net.URLConnection#setReadTimeout(int)}.
     * If {@link UrlConnectionHttpClient#readTimeoutMsSupplier} is provided, the value will be loaded from there instead.
     * Default is {@link UrlConnectionHttpClient#DEFAULT_READ_TIME_OUT_MS}.
     */
    private final int readTimeoutMs;

    /**
     * A provider for loading values to be set in {@link java.net.URLConnection#setConnectTimeout(int)}.
     */
    private final Supplier<Integer> connectTimeoutMsSupplier;

    /**
     * A provider for loading values to be set in {@link java.net.URLConnection#setReadTimeout(int)}.
     */
    private final Supplier<Integer> readTimeoutMsSupplier;

    /**
     * A socket factory for creating the connection's {@link javax.net.ssl.SSLSocket} objects.
     */
    private final SSLSocketFactoryWrapper sslSocketFactory;

    /**
     * Default Constructor, for constructing Lombok's Builder only. Do not expose.
     */
    @Builder
    private UrlConnectionHttpClient(@Nullable final IRetryPolicy<HttpResponse> retryPolicy,
                                    @Nullable final Integer streamBufferSize,
                                    @Nullable final Integer connectTimeoutMs,
                                    @Nullable final Integer readTimeoutMs,
                                    @Nullable final Supplier<Integer> connectTimeoutMsSupplier,
                                    @Nullable final Supplier<Integer> readTimeoutMsSupplier,
                                    @Nullable final List<String> supportedSslProtocols,
                                    @Nullable final SSLContext sslContext) {

        this.retryPolicy = retryPolicy != null ?
                retryPolicy : new NoRetryPolicy();
        this.streamBufferSize = streamBufferSize != null ?
                streamBufferSize : DEFAULT_STREAM_BUFFER_SIZE_BYTE;
        this.connectTimeoutMs = connectTimeoutMs != null ?
                connectTimeoutMs : DEFAULT_CONNECT_TIME_OUT_MS;
        this.readTimeoutMs = readTimeoutMs != null ?
                readTimeoutMs : DEFAULT_READ_TIME_OUT_MS;
        this.connectTimeoutMsSupplier = connectTimeoutMsSupplier;
        this.readTimeoutMsSupplier = readTimeoutMsSupplier;

        final List<String> protocol = supportedSslProtocols != null ?
                supportedSslProtocols : SSLSocketFactoryWrapper.SUPPORTED_SSL_PROTOCOLS;

        if (sslContext == null) {
            // TODO: Build the default SSLContext ourselves with the defined JSSE provider.
            this.sslSocketFactory = new SSLSocketFactoryWrapper((SSLSocketFactory) SSLSocketFactory.getDefault(),
                    protocol);
        } else {
            this.sslSocketFactory = new SSLSocketFactoryWrapper(sslContext.getSocketFactory(),
                    protocol);
        }
    }

    /**
     * Obtain a static default instance of the HTTP Client class.
     *
     * @return a default-configured HttpClient.
     */
    public static synchronized UrlConnectionHttpClient getDefaultInstance() {
        UrlConnectionHttpClient reference = defaultReference.get();
        if (reference == null) {
            defaultReference.compareAndSet(null, UrlConnectionHttpClient.builder()
                    .retryPolicy(StatusCodeAndExceptionRetry.builder()
                            .number(3)
                            .extensionFactor(2)
                            .isAcceptable(new Function<HttpResponse, Boolean>() {
                                public Boolean apply(HttpResponse response) {
                                    return response != null && response.getStatusCode() < 400;
                                }
                            })
                            .initialDelay(RETRY_TIME_WAITING_PERIOD_MSEC)
                            .isRetryable(new Function<HttpResponse, Boolean>() {
                                public Boolean apply(HttpResponse response) {
                                    return response != null && isRetryableError(response.getStatusCode());
                                }
                            })
                            .isRetryableException(new Function<Exception, Boolean>() {
                                public Boolean apply(Exception e) {
                                    return e instanceof SocketTimeoutException;
                                }
                            })
                            .build())
                    .build());
            reference = defaultReference.get();
        }
        return reference;
    }

    /**
     * Record the beginning of an http request.
     */
    private static void recordHttpTelemetryEventStart(@NonNull final String requestMethod,
                                                      @NonNull final URL requestUrl,
                                                      final String requestId) {
        Telemetry.emit(
                new HttpStartEvent()
                        .putMethod(requestMethod)
                        .putPath(requestUrl)
                        .putRequestIdHeader(requestId)
        );
    }

    /**
     * Record the end of an http event.
     *
     * @param response
     */
    private static void recordHttpTelemetryEventEnd(final HttpResponse response) {
        final HttpEndEvent httpEndEvent = new HttpEndEvent();

        if (null != response) {
            httpEndEvent.putStatusCode(response.getStatusCode());
        }

        Telemetry.emit(httpEndEvent);
    }

    /**
     * Sends an HTTP request of the specified method; applies appropriate provided arguments where
     * applicable.
     *
     * @param httpMethod     One of: GET, POST, HEAD, PUT, DELETE, TRACE, OPTIONS, PATCH.
     * @param requestUrl     The recipient {@link URL}.
     * @param requestHeaders Headers used to send the http request.
     * @param requestContent Optional request body, if applicable.
     * @return HttpResponse  The response for this request.
     * @throws IOException If an error is encountered while servicing this request.
     */
    @Override
    public HttpResponse method(@NonNull final HttpClient.HttpMethod httpMethod,
                               @NonNull final URL requestUrl,
                               @NonNull final Map<String, String> requestHeaders,
                               final byte[] requestContent) throws IOException {
        recordHttpTelemetryEventStart(httpMethod.name(), requestUrl, requestHeaders.get(CLIENT_REQUEST_ID));
        final HttpRequest request = constructHttpRequest(httpMethod, requestUrl, requestHeaders, requestContent);
        return retryPolicy.attempt(new Callable<HttpResponse>() {
            public HttpResponse call() throws IOException {
                return executeHttpSend(request, new Consumer<HttpResponse>() {
                    @Override
                    public void accept(HttpResponse httpResponse) {
                        recordHttpTelemetryEventEnd(httpResponse);
                    }
                });
            }
        });
    }

    private static HttpRequest constructHttpRequest(@NonNull HttpClient.HttpMethod httpMethod,
                                                    @NonNull URL requestUrl,
                                                    @NonNull Map<String, String> requestHeaders,
                                                    byte[] requestContent) {

        // Apply special backcompat behaviors for PATCH, if reqd
        if (HttpClient.HttpMethod.PATCH == httpMethod) {
            // Because HttpURLConnection predates RFC-5789, we need to fallback on POST w/ a backcompat
            // workaround. See: https://stackoverflow.com/a/32503192/741827
            httpMethod = HttpClient.HttpMethod.POST;
            // This map may be immutable.
            requestHeaders = new HashMap<>(requestHeaders);
            requestHeaders.put("X-HTTP-Method-Override", HttpClient.HttpMethod.PATCH.name());
        }

        // Construct request
        return new HttpRequest(
                requestUrl,
                requestHeaders,
                httpMethod.name(), // HttpURLConnection doesn't natively support PATCH
                requestContent,
                null
        );
    }

    /**
     * Convert stream into the string.
     *
     * @param inputStream {@link InputStream} to be converted to be a string.
     * @return The converted string
     * @throws IOException Thrown when failing to access inputStream stream.
     */
    private String convertStreamToString(final InputStream inputStream) throws IOException {
        try {
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream,
                    AuthenticationConstants.CHARSET_UTF8));
            final char[] buffer = new char[streamBufferSize];
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
    private static void safeCloseStream(final Closeable stream) {
        final String methodName = ":safeCloseStream";

        if (stream == null) {
            return;
        }

        try {
            stream.close();
        } catch (final IOException e) {
            Logger.error(TAG + methodName, "Encountered IO exception when trying to close the stream", e);
        }
    }

    private HttpResponse executeHttpSend(HttpRequest request, Consumer<HttpResponse> completionCallback) throws IOException {
        final HttpURLConnection urlConnection = setupConnection(request);

        sendRequest(urlConnection, request.getRequestContent(), request.getRequestHeaders().get(HttpConstants.HeaderField.CONTENT_TYPE));

        InputStream responseStream = null;
        HttpResponse response = null;
        try {
            try {
                responseStream = urlConnection.getInputStream();
            } catch (final SocketTimeoutException socketTimeoutException) {
                // SocketTimeoutExcetion is thrown when connection timeout happens. For connection
                // timeout, we want to retry once. Throw the exception to the upper layer, and the
                // upper layer will handle the retry.
                throw socketTimeoutException;
            } catch (final IOException ioException) {
                // 404, for example, will generate an exception.  We should catch it.
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

            final Span span = SpanExtension.current();

            if (response.getHeaders() != null && response.getHeaders().size() > 0) {
                span.setAttribute(
                        AttributeName.response_content_type.name(),
                        response.getHeaderValue(CONTENT_TYPE, 0)
                );

                span.setAttribute(
                        com.microsoft.identity.common.java.opentelemetry.AttributeName.ccs_request_id.name(),
                        response.getHeaderValue(XMS_CCS_REQUEST_ID, 0)
                );

                span.setAttribute(
                        com.microsoft.identity.common.java.opentelemetry.AttributeName.ccs_request_sequence.name(),
                        response.getHeaderValue(XMS_CCS_REQUEST_SEQUENCE, 0)
                );
            }

            span.setAttribute(
                    AttributeName.response_body_length.name(),
                    responseBody.length()
            );
            span.setAttribute(
                    AttributeName.http_status_code.name(),
                    response.getStatusCode()
            );

        } finally {
            completionCallback.accept(response);
            safeCloseStream(responseStream);
        }

        return response;
    }

    private HttpURLConnection setupConnection(HttpRequest request) throws IOException {
        final String methodName = ":setupConnection";
        final HttpURLConnection urlConnection = HttpUrlConnectionFactory.createHttpURLConnection(request.getRequestUrl());

        // Apply request headers and update the headers with default attributes first
        final Set<Map.Entry<String, String>> headerEntries = request.getRequestHeaders().entrySet();

        for (final Map.Entry<String, String> entry : headerEntries) {
            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        if (urlConnection instanceof HttpsURLConnection) {
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslSocketFactory);
        } else if ("https".equalsIgnoreCase(request.getRequestUrl().getProtocol())) {
            throw new IllegalStateException("Trying to initiate a HTTPS request, but didn't get back HttpsURLConnection");
        } else if ("http".equalsIgnoreCase(request.getRequestUrl().getProtocol())) {
            Logger.warn(TAG + methodName, "Making a request for non-https URL.");
        } else {
            Logger.warn(TAG + methodName, "gets a request from an unexpected protocol: " + request.getRequestUrl().getProtocol());
        }

        urlConnection.setRequestMethod(request.getRequestMethod());
        urlConnection.setConnectTimeout(getConnectTimeoutMs());
        urlConnection.setReadTimeout(getReadTimeoutMs());
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setUseCaches(true);
        urlConnection.setDoInput(true);

        return urlConnection;
    }

    private int getReadTimeoutMs() {
        return readTimeoutMsSupplier == null ? readTimeoutMs : readTimeoutMsSupplier.get();
    }

    private int getConnectTimeoutMs() {
        return connectTimeoutMsSupplier == null ? connectTimeoutMs : connectTimeoutMsSupplier.get();
    }

    private static void sendRequest(@NonNull final HttpURLConnection connection,
                                    final byte[] contentRequest,
                                    final String requestContentType) throws IOException {
        if (contentRequest == null) {
            return;
        }

        connection.setDoOutput(true);

        if (!StringUtil.isNullOrEmpty(requestContentType)) {
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
     * Check if the given status code is the retryable status code(500/503/504).
     *
     * @param statusCode The status to check.
     * @return True if the status code is 500, 503 or 504, false otherwise.
     */
    public static boolean isRetryableError(final int statusCode) {
        return statusCode == HttpURLConnection.HTTP_INTERNAL_ERROR
                || statusCode == HttpURLConnection.HTTP_GATEWAY_TIMEOUT
                || statusCode == HttpURLConnection.HTTP_UNAVAILABLE;
    }

}
