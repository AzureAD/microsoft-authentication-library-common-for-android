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
package com.microsoft.identity.common.adal.internal.util;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.internal.broker.BrokerResult;
import com.microsoft.identity.common.internal.cache.ICacheRecord;
import com.microsoft.identity.common.internal.util.ICacheRecordGsonAdapter;

import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Helper methods related to JSON.
 */
public final class JsonExtensions {

    private JsonExtensions() {
        // Utility class.
    }

    /**
     * Extract JSON Object into Map<String, String>.
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
            responseItems.put(key, jsonObject.getString(key));
        }

        return responseItems;
    }

    /**
     * Extract JSON Object into List<ICacheRecord>.
     *
     * @param jsonString String
     * @return List
     */
    public static List<ICacheRecord> getICacheRecordListFromJsonString(String accountJson) {
        GsonBuilder builder = new GsonBuilder();
        builder.registerTypeAdapter(ICacheRecord.class, new ICacheRecordGsonAdapter());

        final Type listOfCacheRecords = new TypeToken<List<ICacheRecord>>() {
        }.getType();
        return builder.create().fromJson(accountJson, listOfCacheRecords);
    }

    /**
     * Converts List<ICacheRecord> into a json string.
     *
     * @param List
     * @return a JSON string
     */
    public static String getJsonStringFromICacheRecordList(List<ICacheRecord> cacheRecords) {
        final Type listOfCacheRecords = new TypeToken<List<ICacheRecord>>() {
        }.getType();
        return new Gson().toJson(cacheRecords, listOfCacheRecords);
    }

    /**
     * Extract JSON Object into BrokerResult.
     *
     * @param jsonString String
     * @return BrokerResult
     */
    public static BrokerResult getBrokerResultFromJsonString(@NonNull final String jsonString) {
        return new GsonBuilder()
                .registerTypeAdapter(ICacheRecord.class, new ICacheRecordGsonAdapter())
                .create()
                .fromJson(
                        jsonString,
                        BrokerResult.class
                );
    }
}
