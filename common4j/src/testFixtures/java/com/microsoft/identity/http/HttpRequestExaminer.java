package com.microsoft.identity.http;

import com.microsoft.identity.common.java.net.HttpClient;
import com.microsoft.identity.common.java.net.HttpResponse;

import java.io.IOException;
import java.net.URL;
import java.util.Map;

import edu.umd.cs.findbugs.annotations.Nullable;
import lombok.AllArgsConstructor;
import lombok.NonNull;

@AllArgsConstructor
public abstract class HttpRequestExaminer implements HttpRequestInterceptor {
    private final HttpClient mOriginalClient;

    @Override
    public HttpResponse performIntercept(@NonNull final HttpClient.HttpMethod httpMethod,
                                         @NonNull final URL requestUrl,
                                         @NonNull final Map<String, String> requestHeaders,
                                         @Nullable final byte[] requestContent) throws IOException {
        examineRequestHeaders(requestHeaders);
        examineRequestUrl(requestUrl);
        return mOriginalClient.method(
                httpMethod,
                requestUrl,
                requestHeaders,
                requestContent,
                null
        );
    }

    public abstract void examineRequestUrl(@NonNull final URL requestUrl);

    public abstract void examineRequestHeaders(@NonNull final Map<String, String> requestHeaders);
}
