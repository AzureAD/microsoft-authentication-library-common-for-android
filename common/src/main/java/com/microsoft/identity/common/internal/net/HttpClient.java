package com.microsoft.identity.common.internal.net;

import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

public interface HttpClient {
    HttpResponse method(@NonNull String httpMethod,
                        @NonNull URL requestUrl,
                        @NonNull Map<String, String> requestHeaders,
                        @Nullable byte[] requestContent) throws IOException;

    HttpResponse method(@NonNull HttpMethod httpMethod,
                        @NonNull URL requestUrl,
                        @NonNull Map<String, String> requestHeaders,
                        @Nullable byte[] requestContent) throws IOException;

    HttpResponse put(@NonNull URL requestUrl,
                     @NonNull Map<String, String> requestHeaders,
                     @Nullable byte[] requestContent) throws IOException;

    HttpResponse patch(@NonNull URL requestUrl,
                       @NonNull Map<String, String> requestHeaders,
                       @Nullable byte[] requestContent) throws IOException;

    HttpResponse options(@NonNull URL requestUrl,
                         @NonNull Map<String, String> requestHeaders) throws IOException;

    HttpResponse post(@NonNull URL requestUrl,
                      @NonNull Map<String, String> requestHeaders,
                      @Nullable byte[] requestContent) throws IOException;

    HttpResponse delete(@NonNull URL requestUrl,
                        @NonNull Map<String, String> requestHeaders,
                        @Nullable byte[] requestContent) throws IOException;

    HttpResponse get(@NonNull URL requestUrl,
                     @NonNull Map<String, String> requestHeaders) throws IOException;

    HttpResponse head(@NonNull URL requestUrl,
                      @NonNull Map<String, String> requestHeaders) throws IOException;

    HttpResponse trace(@NonNull URL requestUrl,
                       @NonNull Map<String, String> requestHeaders) throws IOException;

    enum HttpMethod {
        GET,
        HEAD,
        PUT,
        POST,
        OPTIONS,
        PATCH,
        DELETE,
        TRACE;

        private static final Map<String, UrlConnectionHttpClient.HttpMethod> validMethods;

        static {
            validMethods = new LinkedHashMap<>(HttpMethod.values().length);
            for (HttpMethod method: HttpMethod.values()) {
                validMethods.put(method.name(), method);
            }
        }

        public static HttpMethod validateAndNormalizeMethod(@NonNull final String httpMethod) {
            if (TextUtils.isEmpty(httpMethod)) {
                throw new IllegalArgumentException("HTTP method cannot be null or blank");
            }

            HttpMethod method = validMethods.get(httpMethod);
            if (method != null) {
                return method;
            }
            throw new IllegalArgumentException("Unknown or unsupported HTTP method: " + httpMethod);
        }

    }
}
