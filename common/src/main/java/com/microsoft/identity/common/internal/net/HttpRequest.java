package com.microsoft.identity.common.internal.net;

import java.net.URL;
import java.util.Map;

import lombok.NonNull;

public class HttpRequest extends com.microsoft.identity.common.java.internal.net.HttpRequest {

    /**
     * Constructor for {@link com.microsoft.identity.common.java.internal.net.HttpRequest} with request {@link URL}, headers, post message and the
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
                final byte[] requestContent,
                final String requestContentType) {
        super(requestUrl, requestHeaders, requestMethod, requestContent, requestContentType);
    }
}
