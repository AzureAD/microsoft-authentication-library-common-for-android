//  Copyright (c) Microsoft Corporation.
//  All rights reserved.
//
//  This code is licensed under the MIT License.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files(the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and / or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions :
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.
package com.microsoft.identity.common.java.util;

import com.microsoft.identity.common.java.net.HttpResponse;
import com.microsoft.identity.common.java.opentelemetry.AttributeName;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;

import io.opentelemetry.api.trace.Span;

public class HashMapExtensions {

    /**
     * Get key value pairs from response.
     *
     * @param webResponse {@link HttpResponse} to convert to a map
     * @return HashMap
     * @throws JSONException
     */
    public static HashMap<String, String> getJsonResponse(HttpResponse webResponse) throws JSONException {
        if (webResponse != null && !StringUtil.isNullOrEmpty(webResponse.getBody())) {
            return getJsonResponseFromResponseBody(webResponse.getBody());
        }
        return new HashMap<>();
    }


    /**
     * Get key value pairs from response.
     *
     * @param responseBody {@link HttpResponse}'s body.
     * @return HashMap
     * @throws JSONException
     */
    public static HashMap<String, String> getJsonResponseFromResponseBody(String responseBody) throws JSONException {
        final HashMap<String, String> response = new HashMap<>();
        Span span = Span.current();
        span.setAttribute(AttributeName.response_body_length.name(), responseBody.length());
        if (!StringUtil.isNullOrEmpty(responseBody)) {
            final JSONObject jsonObject = new JSONObject(responseBody);
            final Iterator<String> keyIterator = jsonObject.keys();
            while (keyIterator.hasNext()) {
                String key = keyIterator.next();
                response.put(key, jsonObject.get(key).toString());
            }
        }
        return response;
    }
}
