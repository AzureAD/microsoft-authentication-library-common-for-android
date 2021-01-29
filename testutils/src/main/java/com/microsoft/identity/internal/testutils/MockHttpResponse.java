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
package com.microsoft.identity.internal.testutils;

import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.microsoft.identity.common.internal.net.HttpClient.HttpMethod;
import com.microsoft.identity.common.internal.net.HttpResponse;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Class to  set a mock request interceptor at runtime.
 */
public class MockHttpResponse {

    private static final HashMap<Pair<HttpMethod, String>, MockHttpRequestInterceptor> interceptors = new HashMap<>();

    public static void setInterceptor(@NonNull final MockHttpRequestInterceptor interceptor) {
        MockHttpResponse.setInterceptor(interceptor, null, ".*");
    }

    public static void setInterceptor(@NonNull final MockHttpRequestInterceptor interceptor,
                                      @NonNull String urlRegex) {
        MockHttpResponse.setInterceptor(interceptor, null, urlRegex);
    }

    public static void setInterceptor(@NonNull final MockHttpRequestInterceptor interceptor,
                                      @NonNull HttpMethod method) {
        MockHttpResponse.setInterceptor(interceptor, method, ".*");
    }


    public static void setInterceptor(@NonNull final MockHttpRequestInterceptor interceptor,
                                      @Nullable final HttpMethod method,
                                      @NonNull final String urlRegex) {
        interceptors.put(new Pair<>(method, urlRegex), interceptor);
    }

    public static MockHttpRequestInterceptor intercept(@NonNull final HttpMethod method, @NonNull final URL url) {
        for (Pair<HttpMethod, String> pair : interceptors.keySet()) {
            if ((pair.first == null || pair.first.compareTo(method) == 0) && url.toString().matches(pair.second)) {
                return interceptors.get(pair);
            }
        }
        return null;
    }

    public static void setHttpResponse(@NonNull final HttpResponse httpResponse) {
        MockHttpResponse.setHttpResponse(httpResponse, null, ".*");
    }

    public static void setHttpResponse(@NonNull final HttpResponse httpResponse, @NonNull HttpMethod method) {
        MockHttpResponse.setHttpResponse(httpResponse, method, ".*");
    }

    public static void setHttpResponse(@NonNull final HttpResponse httpResponse, @NonNull final String urlRegex) {
        MockHttpResponse.setHttpResponse(httpResponse, null, urlRegex);
    }

    public static void setHttpResponse(@NonNull final HttpResponse httpResponse, @Nullable final HttpMethod method, @NonNull final String urlRegex) {
        MockHttpResponse.setInterceptor(new MockHttpRequestInterceptor() {
            @Override
            public HttpResponse method(@NonNull HttpMethod httpMethod, @NonNull URL requestUrl, @NonNull Map<String, String> requestHeaders, @Nullable byte[] requestContent) {
                return httpResponse;
            }
        }, method, urlRegex);
    }
}
