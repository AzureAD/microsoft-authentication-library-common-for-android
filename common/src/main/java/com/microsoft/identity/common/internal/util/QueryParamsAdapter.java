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
package com.microsoft.identity.common.internal.util;

import android.text.TextUtils;
import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.logging.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Class to serialize and deserialize query parameters from List<Pair<String, String>> to json String
 * and vice versa.
 *
 * NOTE: Even we no longer use Pair (Since it's android-only), we are keeping this the same
 *       to maintain backcompat with serialized value from older common that still uses it.
 */
public class QueryParamsAdapter extends TypeAdapter<List<Pair<String, String>>> {

    private static final String TAG = QueryParamsAdapter.class.getSimpleName();

    private static final Gson mGson;

    static {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(
                QueryParamsAdapter.class,
                new QueryParamsAdapter()
        );
        mGson = gsonBuilder.create();
    }

    @Override
    public void write(final JsonWriter out, final List<Pair<String, String>> queryParams) throws IOException {
        out.beginObject();

        for (final Pair<String, String> keyValuePair : queryParams) {
            out.name(keyValuePair.first);
            out.value(keyValuePair.second);
        }

        out.endObject();
    }

    @Override
    public List<Pair<String, String>> read(final JsonReader in) throws IOException {
        in.beginObject();
        final List<Pair<String, String>> result = new ArrayList<>();
        while (in.hasNext()) {
            final String key = in.nextName();
            final String value = in.nextString();
            final Pair<String, String> keyValuePair = new Pair<>(key, value);
            result.add(keyValuePair);
        }
        in.endObject();
        return result;
    }

    public static String _toJson(final List<Map.Entry<String, String>> extraQueryStringParameters) {
        final List<Pair<String, String>> extraQpPairs = new ArrayList<>();
        for (final Map.Entry<String, String> entry: extraQueryStringParameters) {
            extraQpPairs.add(new Pair<String, String>(entry.getKey(), entry.getValue()));
        }
        return mGson.toJson(extraQpPairs, getPairListType());
    }

    public static List<Map.Entry<String, String>> _fromJson(final String jsonString)
            throws ClientException{
        final String methodName = ":_fromJson";

        if (TextUtils.isEmpty(jsonString)) {
            return new ArrayList<>();
        }

        try {
            final List<Pair<String, String>> extraQpPairs = mGson.fromJson(jsonString, getPairListType());
            final List<Map.Entry<String, String>> extraQpMapEntries = new ArrayList<>();
            for (final Pair<String, String> entry: extraQpPairs) {
                if (!StringUtil.isEmpty(entry.first)) {
                    extraQpMapEntries.add(new AbstractMap.SimpleEntry<String, String>(entry.first, entry.second));
                }
            }
            return extraQpMapEntries;
        } catch (final JsonSyntaxException e) {
            final String errorMessage = "malformed json string:" + jsonString;
            Logger.error(TAG + methodName, errorMessage, e);
            throw new ClientException(ClientException.JSON_PARSE_FAILURE, errorMessage, e);
        }
    }

    /**
     * Create a Type for the List of query params
     *
     * @return a Type object representing the type of the query params in this case List<Pair<String, String>>
     */
    private static Type getPairListType() {
        return TypeToken.getParameterized(List.class, TypeToken.getParameterized(Pair.class, String.class, String.class).getRawType()).getType();
    }
}
