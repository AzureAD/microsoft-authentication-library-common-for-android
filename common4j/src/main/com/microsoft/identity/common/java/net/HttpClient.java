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

import com.microsoft.identity.common.java.util.StringUtil;
import lombok.NonNull;

import java.io.IOException;
import java.net.URL;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;

/**
 * An interface providing access to resources backed by web requests.  This provides access to only
 * the verbs enumerated by HttpMethod.
 */
public interface HttpClient {
    /**
     * Execute an arbitrary method by name.  This will fail with an IllegalArgumentException unless
     * httpMethod is one of { PUT, GET, HEAD, POST, PATCH, DELETE, OPTIONS, or TRACE }.
     * @param httpMethod the string of the http method to use.  Must be one of { PUT, GET,
     *                   HEAD, POST, PATCH, DELETE, OPTIONS, or TRACE }, case insensitive.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @param requestContent the body content of the request, if applicable.  May be null.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse method(@NonNull String httpMethod,
                        @NonNull URL requestUrl,
                        @NonNull Map<String, String> requestHeaders,
                        byte[] requestContent) throws IOException;

    /**
     * Execute an arbitrary method.
     * @param httpMethod the HttpMethod to use for the call.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @param requestContent the body content of the request, if applicable.  May be null.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse method(@NonNull HttpMethod httpMethod,
                        @NonNull URL requestUrl,
                        @NonNull Map<String, String> requestHeaders,
                        byte[] requestContent) throws IOException;

    /**
     * Execute an HTTP PUT request.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @param requestContent the body content of the request, if applicable.  May be null.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse put(@NonNull URL requestUrl,
                     @NonNull Map<String, String> requestHeaders,
                     byte[] requestContent) throws IOException;

    /**
     * Execute an HTTP PATCH request.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @param requestContent the body content of the request, if applicable.  May be null.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse patch(@NonNull URL requestUrl,
                       @NonNull Map<String, String> requestHeaders,
                       byte[] requestContent) throws IOException;

    /**
     * Execute an HTTP OPTIONS request.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse options(@NonNull final URL requestUrl,
                         @NonNull final Map<String, String> requestHeaders) throws IOException;

    /**
     * Execute an HTTP POST request.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @param requestContent the body content of the request, if applicable.  May be null.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse post(@NonNull URL requestUrl,
                      @NonNull Map<String, String> requestHeaders,
                      byte[] requestContent) throws IOException;

    /**
     * Execute an HTTP PATCH request.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @param requestContent the body content of the request, if applicable.  May be null.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse delete(@NonNull final URL requestUrl,
                        @NonNull final Map<String, String> requestHeaders,
                        byte[] requestContent) throws IOException;

    /**
     * Execute an HTTP GET request.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse get(@NonNull final URL requestUrl,
                     @NonNull final Map<String, String> requestHeaders) throws IOException;

    /**
     * Execute an HTTP HEAD request.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse head(@NonNull final URL requestUrl,
                      @NonNull final Map<String, String> requestHeaders) throws IOException;

    /**
     * Execute an HTTP TRACE request.
     * @param requestUrl the URL of the resource to operate on.
     * @param requestHeaders the headers for the request.
     * @return an HttpResponse with the result of the call.
     * @throws IOException if there was a communication problem.
     */
    HttpResponse trace(@NonNull final URL requestUrl,
                       @NonNull final Map<String, String> requestHeaders) throws IOException;

    /**
     * An enumeration of the HTTP verbs supported by this client interface.
     */
    enum HttpMethod {
        GET,
        HEAD,
        PUT,
        POST,
        OPTIONS,
        PATCH,
        DELETE,
        TRACE;

        private static final Map<String, HttpClient.HttpMethod> validMethods;

        static {
            validMethods = new LinkedHashMap<>(HttpMethod.values().length);
            for (HttpMethod method: HttpMethod.values()) {
                validMethods.put(method.name(), method);
            }
        }

        public static HttpMethod validateAndNormalizeMethod(@NonNull final String httpMethod) {
            if (StringUtil.isNullOrEmpty(httpMethod)) {
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
