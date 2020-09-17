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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Consumer;

import com.microsoft.identity.common.internal.telemetry.Telemetry;
import com.microsoft.identity.common.internal.telemetry.events.HttpEndEvent;
import com.microsoft.identity.common.internal.telemetry.events.HttpStartEvent;
import com.microsoft.identity.common.internal.util.StringUtil;

import net.jcip.annotations.Immutable;
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
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import lombok.AllArgsConstructor;
import lombok.Builder;
import static com.microsoft.identity.common.adal.internal.AuthenticationConstants.AAD.CLIENT_REQUEST_ID;
import static com.microsoft.identity.common.internal.net.HttpUrlConnectionFactory.createHttpURLConnection;

/**
 * A client object for handling HTTP requests and responses.  This class accepts a RetryPolicy that
 * is applied to the responses.  By default, the policy is a null policy not retrying anything.  The
 * default settings for read timeout and connection timeout are 30s, and the default setting for the
 * size of the buffer for reading objects from the http stream is 1024 bytes.
 *
 * There are two ways to supply timeout values to this class, one method takes suppliers, the other
 * one integers.  If you use both of these, the suppliers method will take precendence over the method
 * using integers.
 */
@AllArgsConstructor
@Builder
@ThreadSafe
@Immutable
public class UrlConnectionHttpClient implements HttpClient {
    /**
     * A functional interface modeled off of java.util.function.Supplier for providing
     * values to callers.
     * @param <T> the type of value provided.
     */
    interface Supplier<T> {
        /**
         * @return an instance of a T.
         */
        T get();
    }

    /**
     * A utility method for creating suppliers of particular values.
     * @param value the value to supply.
     * @param <T> the type of the value to return.
     * @return a new Supplier that returns the passed value.
     */
    public static <T> Supplier<T> supplierOf(final T value) {
        return new Supplier<T>() {
            public T get() {
                return value;
            }
        };
    }

    @Builder.Default
    private final RetryPolicy<HttpResponse> retryPolicy = new NoRetryPolicy();
    @Builder.Default
    private final int connectTimeoutMs = 30000;
    @Builder.Default
    private final int readTimeoutMs = 30000;
    @Builder.Default
    private final Supplier<Integer> connectTimeoutMsSupplier = null;
    @Builder.Default
    private final Supplier<Integer> readTimeoutMsSupplier = null;
    @Builder.Default
    private final int streamBufferSize = 1024;

    private static transient AtomicReference<UrlConnectionHttpClient> defaultReference = new AtomicReference<>(null);

    /**
     * Obtain a static default instance of the HTTP Client class.
     * @return a default-configured HttpClient.
     */
    public static UrlConnectionHttpClient getDefaultInstance() {
        UrlConnectionHttpClient reference = defaultReference.get();
        if (reference == null) {
            defaultReference.compareAndSet(null, UrlConnectionHttpClient.builder().build());
            reference = defaultReference.get();
        }
        return reference;
    }

    /**
     * Record the beginning of an http request.
     */
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

    /**
     * Record the end of an http event.
     * @param response
     */
    private static void recordHttpTelemetryEventEnd(@Nullable final HttpResponse response) {
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
     * @param httpMethod         One of: GET, POST, HEAD, PUT, DELETE, TRACE, OPTIONS, PATCH.
     * @param requestUrl         The recipient {@link URL}.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @return HttpResponse      The response for this request.
     * @throws IOException If an error is encountered while servicing this request.
     */
    public HttpResponse method(@NonNull final String httpMethod,
                                      @NonNull final URL requestUrl,
                                      @NonNull final Map<String, String> requestHeaders,
                                      @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.validateAndNormalizeMethod(httpMethod), requestUrl, requestHeaders, requestContent);
    }

    /**
     * Sends an HTTP request of the specified method; applies appropriate provided arguments where
     * applicable.
     *
     * @param httpMethod         One of: GET, POST, HEAD, PUT, DELETE, TRACE, OPTIONS, PATCH.
     * @param requestUrl         The recipient {@link URL}.
     * @param requestHeaders     Headers used to send the http request.
     * @param requestContent     Optional request body, if applicable.
     * @return HttpResponse      The response for this request.
     * @throws IOException If an error is encountered while servicing this request.
     */
    @Override
    public HttpResponse method(@NonNull final HttpMethod httpMethod,
                               @NonNull final URL requestUrl,
                               @NonNull final Map<String, String> requestHeaders,
                               @Nullable final byte[] requestContent) throws IOException {
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

    private static HttpRequest constructHttpRequest(@NonNull HttpMethod httpMethod,
                                                    @NonNull URL requestUrl,
                                                    @NonNull Map<String, String> requestHeaders,
                                                    @Nullable byte[] requestContent) {

        // Apply special backcompat behaviors for PATCH, if reqd
        if (HttpMethod.PATCH == httpMethod) {
            // Because HttpURLConnection predates RFC-5789, we need to fallback on POST w/ a backcompat
            // workaround. See: https://stackoverflow.com/a/32503192/741827
            httpMethod = HttpMethod.POST;
            // This map may be immutable.
            requestHeaders = new HashMap<>(requestHeaders);
            requestHeaders.put("X-HTTP-Method-Override", HttpMethod.PATCH.name());
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

    @Override
    public HttpResponse put(@NonNull final URL requestUrl,
                            @NonNull final Map<String, String> requestHeaders,
                            @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.PUT.name(), requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse patch(@NonNull final URL requestUrl,
                              @NonNull final Map<String, String> requestHeaders,
                              @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.PATCH.name(), requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse options(@NonNull final URL requestUrl,
                                @NonNull final Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.OPTIONS.name(), requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse post(@NonNull final URL requestUrl,
                             @NonNull final Map<String, String> requestHeaders,
                             @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.POST.name(), requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse delete(@NonNull final URL requestUrl,
                               @NonNull final Map<String, String> requestHeaders,
                               @Nullable final byte[] requestContent) throws IOException {
        return method(HttpMethod.POST.name(), requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse get(@NonNull final URL requestUrl,
                            @NonNull final Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.GET.name(), requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse head(@NonNull final URL requestUrl,
                             @NonNull final Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.HEAD.name(), requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse trace(@NonNull final URL requestUrl,
                              @NonNull final Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.TRACE.name(), requestUrl, requestHeaders, null);
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
            final BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
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

    private HttpResponse executeHttpSend(HttpRequest request, Consumer<HttpResponse> completionCallback) throws IOException {
        final HttpURLConnection urlConnection = setupConnection(request);
        urlConnection.setRequestMethod(request.getRequestMethod());
        urlConnection.setUseCaches(true);
        setRequestBody(urlConnection, request.getRequestContent(), request.getmRequestHeaders().get(HttpConstants.HeaderField.CONTENT_TYPE));

        InputStream responseStream = null;

        HttpResponse response = null;
        try {
            try {
                responseStream = urlConnection.getInputStream();
            } catch (final SocketTimeoutException socketTimeoutException) {
                // SocketTimeoutExcetion is thrown when connection timeout happens. For connection
                // timeout, we want to retry once. Throw the exception to the upper layer, and the
                // upper layer will handle the rety.
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
        } finally {
            completionCallback.accept(response);
            safeCloseStream(responseStream);
        }

        return response;
    }

    private HttpURLConnection setupConnection(HttpRequest request) throws IOException {
        final HttpURLConnection urlConnection = createHttpURLConnection(request.getRequestUrl());

        // Apply request headers and update the headers with default attributes first
        final Set<Map.Entry<String, String>> headerEntries = request.getmRequestHeaders().entrySet();

        for (final Map.Entry<String, String> entry : headerEntries) {
            urlConnection.setRequestProperty(entry.getKey(), entry.getValue());
        }

        urlConnection.setConnectTimeout(getConnectTimeoutMs());
        urlConnection.setReadTimeout(getReadTimeoutMs());
        urlConnection.setInstanceFollowRedirects(true);
        urlConnection.setUseCaches(false);
        urlConnection.setDoInput(true);

        return urlConnection;
    }

    private Integer getReadTimeoutMs() {
        return readTimeoutMsSupplier == null ? readTimeoutMs : readTimeoutMsSupplier.get();
    }

    private Integer getConnectTimeoutMs() {
        return connectTimeoutMsSupplier == null ? connectTimeoutMs : connectTimeoutMsSupplier.get();
    }

    private static void setRequestBody(@NonNull final HttpURLConnection connection,
                                       @Nullable final byte[] contentRequest,
                                       @Nullable final String requestContentType) throws IOException {
        if (contentRequest == null || contentRequest.length == 0) {
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
}
