package com.microsoft.identity.common.adal.internal.net;


import java.util.List;
import java.util.Map;

/**
 * Web response to keep status, response body, headers and related exceptions.
 */
public class HttpWebResponse {
    private final int mStatusCode;
    private final String mResponseBody;
    private final Map<String, List<String>> mResponseHeaders;

    /**
     * Constructor for {@link HttpWebResponse}.
     *
     * @param statusCode      Status code returned for the http call.
     * @param responseBody    Response body returned from the http network call.
     * @param responseHeaders Response header for the network call.
     */
    public HttpWebResponse(int statusCode, String responseBody, Map<String, List<String>> responseHeaders) {
        mStatusCode = statusCode;
        mResponseBody = responseBody;
        mResponseHeaders = responseHeaders;
    }

    /**
     * @return The status code for the network call.
     */
    public int getStatusCode() {
        return mStatusCode;
    }

    /**
     * @return The response headers for the network call.
     */
    public Map<String, List<String>> getResponseHeaders() {
        return mResponseHeaders;
    }

    /**
     * @return The response body for the network call.
     */
    public String getBody() {
        return mResponseBody;
    }

}