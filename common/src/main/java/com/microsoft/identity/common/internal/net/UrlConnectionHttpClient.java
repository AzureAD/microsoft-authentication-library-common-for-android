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

import androidx.annotation.Nullable;

import com.microsoft.identity.common.java.net.AbstractHttpClient;
import com.microsoft.identity.common.java.net.IRetryPolicy;

import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicReference;

import lombok.NonNull;

import static com.microsoft.identity.common.java.net.UrlConnectionHttpClient.DEFAULT_CONNECT_TIME_OUT_MS;
import static com.microsoft.identity.common.java.net.UrlConnectionHttpClient.DEFAULT_READ_TIME_OUT_MS;
import static com.microsoft.identity.common.java.net.UrlConnectionHttpClient.DEFAULT_STREAM_BUFFER_SIZE;

/**
 * Deprecated
 * <p>
 * Currently served as an adapter of {@link com.microsoft.identity.common.java.net.UrlConnectionHttpClient}
 */
public class UrlConnectionHttpClient extends AbstractHttpClient {

    /**
     * A functional interface modeled off of java.util.function.Supplier for providing
     * values to callers.
     *
     * @param <T> the type of value provided.
     */
    public interface Supplier<T> extends com.microsoft.identity.common.java.util.ported.Supplier<T> {
    }

    private static transient AtomicReference<UrlConnectionHttpClient> defaultReference = new AtomicReference<>(null);

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private RetryPolicy<HttpResponse> mRetryPolicy = new NoRetryPolicy();
        private int mConnectTimeoutMs = DEFAULT_CONNECT_TIME_OUT_MS;
        private int mReadTimeoutMs = DEFAULT_READ_TIME_OUT_MS;
        private Supplier<Integer> mConnectTimeoutMsSupplier = null;
        private Supplier<Integer> mReadTimeoutMsSupplier = null;
        private int mStreamBufferSize = DEFAULT_STREAM_BUFFER_SIZE;

        public Builder retryPolicy(@NonNull final RetryPolicy<HttpResponse> retryPolicy) {
            mRetryPolicy = retryPolicy;
            return this;
        }

        public Builder connectTimeoutMs(final int connectTimeoutMs) {
            mConnectTimeoutMs = connectTimeoutMs;
            return this;
        }

        public Builder readTimeoutMs(final int readTimeoutMs) {
            mReadTimeoutMs = readTimeoutMs;
            return this;
        }

        public Builder connectTimeoutMsSupplier(@NonNull final Supplier<Integer> connectTimeoutMsSupplier) {
            mConnectTimeoutMsSupplier = connectTimeoutMsSupplier;
            return this;
        }

        public Builder readTimeoutMsSupplier(@NonNull final Supplier<Integer> readTimeoutMsSupplier) {
            mReadTimeoutMsSupplier = readTimeoutMsSupplier;
            return this;
        }

        public Builder streamBufferSize(final int streamBufferSize) {
            mStreamBufferSize = streamBufferSize;
            return this;
        }

        public UrlConnectionHttpClient build() {
            return new UrlConnectionHttpClient(mRetryPolicy,
                    mConnectTimeoutMs,
                    mReadTimeoutMs,
                    mConnectTimeoutMsSupplier,
                    mReadTimeoutMsSupplier,
                    mStreamBufferSize);
        }
    }

    private final com.microsoft.identity.common.java.net.UrlConnectionHttpClient sAdaptedObject;

    private UrlConnectionHttpClient(final RetryPolicy<HttpResponse> retryPolicy,
                                    int connectTimeoutMs,
                                    int readTimeoutMs,
                                    Supplier<Integer> connectTimeoutMsSupplier,
                                    @Nullable Supplier<Integer> readTimeoutMsSupplier,
                                    int streamBufferSize) {
        sAdaptedObject = new com.microsoft.identity.common.java.net.UrlConnectionHttpClient(
                new IRetryPolicy<com.microsoft.identity.common.java.net.HttpResponse>() {
                    @Override
                    public com.microsoft.identity.common.java.net.HttpResponse attempt(final Callable<com.microsoft.identity.common.java.net.HttpResponse> supplier) throws IOException {
                        return retryPolicy.attempt(new Callable<HttpResponse>() {
                            @Override
                            public HttpResponse call() throws Exception {
                                return new HttpResponse(supplier.call());
                            }
                        });
                    }
                }, connectTimeoutMs, readTimeoutMs, connectTimeoutMsSupplier, readTimeoutMsSupplier, streamBufferSize);
    }

    private UrlConnectionHttpClient(@NonNull final com.microsoft.identity.common.java.net.UrlConnectionHttpClient clientToBeAdapted) {
        sAdaptedObject = new com.microsoft.identity.common.java.net.UrlConnectionHttpClient(clientToBeAdapted);
    }

    /**
     * Obtain a static default instance of the HTTP Client class.
     *
     * @return a default-configured HttpClient.
     */
    public static synchronized UrlConnectionHttpClient getDefaultInstance() {
        UrlConnectionHttpClient reference = defaultReference.get();
        if (reference == null) {
            final com.microsoft.identity.common.java.net.UrlConnectionHttpClient defaultInstance =
                    com.microsoft.identity.common.java.net.UrlConnectionHttpClient.getDefaultInstance();

            defaultReference.compareAndSet(null, new UrlConnectionHttpClient(defaultInstance));
            reference = defaultReference.get();
        }
        return reference;
    }

    @Override
    public HttpResponse method(@NonNull String httpMethod,
                               @NonNull URL requestUrl,
                               @NonNull Map<String, String> requestHeaders,
                               byte[] requestContent) throws IOException {
        return new HttpResponse(super.method(httpMethod, requestUrl, requestHeaders, requestContent));
    }

    @Override
    public HttpResponse method(@NonNull HttpMethod httpMethod, @NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, byte[] requestContent) throws IOException {
        return new HttpResponse(sAdaptedObject.method(httpMethod, requestUrl, requestHeaders, requestContent));
    }

    @Override
    public HttpResponse put(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, byte[] requestContent) throws IOException {
        return new HttpResponse(sAdaptedObject.put(requestUrl, requestHeaders, requestContent));
    }

    @Override
    public HttpResponse patch(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, byte[] requestContent) throws IOException {
        return new HttpResponse(sAdaptedObject.patch(requestUrl, requestHeaders, requestContent));
    }

    @Override
    public HttpResponse options(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders) throws IOException {
        return new HttpResponse(sAdaptedObject.options(requestUrl, requestHeaders));
    }

    @Override
    public HttpResponse post(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, byte[] requestContent) throws IOException {
        return new HttpResponse(sAdaptedObject.post(requestUrl, requestHeaders, requestContent));
    }

    @Override
    public HttpResponse delete(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, byte[] requestContent) throws IOException {
        return new HttpResponse(sAdaptedObject.delete(requestUrl, requestHeaders, requestContent));
    }

    @Override
    public HttpResponse get(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders) throws IOException {
        return new HttpResponse(sAdaptedObject.get(requestUrl, requestHeaders));
    }

    @Override
    public HttpResponse head(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders) throws IOException {
        return new HttpResponse(sAdaptedObject.head(requestUrl, requestHeaders));
    }

    @Override
    public HttpResponse trace(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders) throws IOException {
        return new HttpResponse(sAdaptedObject.trace(requestUrl, requestHeaders));
    }
}
