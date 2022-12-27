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
package com.microsoft.identity.http;

import com.microsoft.identity.common.java.net.HttpClient;

import java.net.URL;
import java.util.Map;
import java.util.function.Predicate;
import java.util.regex.Pattern;

import lombok.Builder;
import lombok.NonNull;

/**
 * Used to match http request based on headers, body, url and method. A request is considered as matched
 * if all these 4 parameters are matched.
 */
@Builder
public class HttpRequestMatcher {

    @NonNull
    @Builder.Default
    private Predicate<Map<String, String>> headers = new Predicate<Map<String, String>>() {
        @Override
        public boolean test(Map<String, String> header) {
            return true;
        }
    };
    @NonNull
    @Builder.Default
    private Predicate<byte[]> body = new Predicate<byte[]>() {
        @Override
        public boolean test(byte[] content) {
            return true;
        }
    };

    @NonNull
    @Builder.Default
    private Predicate<URL> url = new Predicate<URL>() {
        @Override
        public boolean test(URL s) {
            return true;
        }
    };

    @NonNull
    @Builder.Default
    private Predicate<HttpClient.HttpMethod> method = new Predicate<HttpClient.HttpMethod>() {
        @Override
        public boolean test(HttpClient.HttpMethod s) {
            return true;
        }
    };


    /**
     * Checks whether the request matches the predicates defined in this request matcher.
     *
     * @param method         the request method
     * @param url            the request URL
     * @param requestHeaders the request headers
     * @param requestContent the request body/content
     * @return true if the predicates matched this request
     */
    public boolean matches(
            final HttpClient.HttpMethod method,
            @NonNull final URL url,
            final Map<String, String> requestHeaders,
            final byte[] requestContent) {
        return this.method.test(method) && this.url.test(url)
                && this.headers.test(requestHeaders) && this.body.test(requestContent);
    }

    public static class HttpRequestMatcherBuilder {

        /**
         * CHeck for the TRACE http method
         *
         * @return the builder
         */
        public HttpRequestMatcherBuilder isTRACE() {
            return methodIs(HttpClient.HttpMethod.TRACE);
        }

        /**
         * CHeck for the OPTIONS http method
         *
         * @return the builder
         */
        public HttpRequestMatcherBuilder isOPTIONS() {
            return methodIs(HttpClient.HttpMethod.OPTIONS);
        }

        /**
         * CHeck for the HEAD http method
         *
         * @return the builder
         */
        public HttpRequestMatcherBuilder isHEAD() {
            return methodIs(HttpClient.HttpMethod.HEAD);
        }

        /**
         * CHeck for the DELETE http method
         *
         * @return the builder
         */
        public HttpRequestMatcherBuilder isDELETE() {
            return methodIs(HttpClient.HttpMethod.DELETE);
        }

        /**
         * CHeck for the PATCH http method
         *
         * @return the builder
         */
        public HttpRequestMatcherBuilder isPATCH() {
            return methodIs(HttpClient.HttpMethod.PATCH);
        }

        /**
         * CHeck for the PUT http method
         *
         * @return the builder
         */
        public HttpRequestMatcherBuilder isPUT() {
            return methodIs(HttpClient.HttpMethod.PUT);
        }

        /**
         * CHeck for the GET http method
         *
         * @return the builder
         */
        public HttpRequestMatcherBuilder isGET() {
            return methodIs(HttpClient.HttpMethod.GET);
        }

        /**
         * CHeck for the POST http method
         *
         * @return the builder
         */
        public HttpRequestMatcherBuilder isPOST() {
            return methodIs(HttpClient.HttpMethod.POST);
        }

        /**
         * Check for the Http Method
         *
         * @param method the Http method
         * @return the builder
         */
        public HttpRequestMatcherBuilder methodIs(HttpClient.HttpMethod method) {
            method(new Predicate<HttpClient.HttpMethod>() {
                @Override
                public boolean test(HttpClient.HttpMethod _method) {
                    return _method == method;
                }
            });
            return this;
        }

        /**
         * Check for a URL pattern
         *
         * @param pattern the pattern
         * @return the builder
         */
        public HttpRequestMatcherBuilder urlPattern(Pattern pattern) {
            url(new Predicate<URL>() {
                @Override
                public boolean test(URL url) {
                    return pattern.matcher(url.toExternalForm()).matches();
                }
            });
            return this;
        }

        public HttpRequestMatcherBuilder headersContain(Map.Entry<String, String> header) {
            headers(new Predicate<Map<String, String>>() {
                @Override
                public boolean test(Map<String, String> headers) {
                    return header.getValue().equals(headers.get(header.getKey()));
                }
            });
            return this;
        }
    }
}
