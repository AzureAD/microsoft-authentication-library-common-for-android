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
package com.microsoft.identity.common.java.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Helper methods related to JSON.
 */
public class JsonUtil {

    /**
     * Extract JSON Object into a String Map
     *
     * @param jsonString String
     * @return Map
     * @throws JSONException if JSON string is malformed.
     */
    public static Map<String, String> extractJsonObjectIntoMap(final String jsonString)
            throws JSONException {

        final JSONObject jsonObject = new JSONObject(jsonString);
        final Iterator<String> keyIterator = jsonObject.keys();

        final Map<String, String> responseItems = new HashMap<>();
        while (keyIterator.hasNext()) {
            final String key = keyIterator.next();
            final String value = jsonObject.get(key).toString();
            responseItems.put(key, value);
        }

        return responseItems;
    }
}
