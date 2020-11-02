package com.microsoft.identity.common.internal.net;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

public abstract class AbstractHttpClient implements HttpClient {

    @Override
    public HttpResponse method(@NonNull String httpMethod, @NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent) throws IOException {
        return method(HttpClient.HttpMethod.validateAndNormalizeMethod(httpMethod), requestUrl, requestHeaders, requestContent);
    }

    @Override
    public abstract HttpResponse method(@NonNull HttpMethod httpMethod, @NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requstContent) throws IOException;

    @Override
    public HttpResponse put(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent) throws IOException {
        return method(HttpMethod.PUT, requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse patch(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent) throws IOException {
        return method(HttpMethod.PATCH, requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse options(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.OPTIONS, requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse post(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent) throws IOException {
        return method(HttpMethod.POST, requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse delete(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent) throws IOException {
        return method(HttpMethod.DELETE, requestUrl, requestHeaders, requestContent);
    }

    @Override
    public HttpResponse get(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.GET, requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse head(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.HEAD, requestUrl, requestHeaders, null);
    }

    @Override
    public HttpResponse trace(@NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders) throws IOException {
        return method(HttpMethod.TRACE, requestUrl, requestHeaders, null);
    }
}
