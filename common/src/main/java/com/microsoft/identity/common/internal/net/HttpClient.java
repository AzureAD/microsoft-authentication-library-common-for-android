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
