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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;
import com.microsoft.identity.common.java.exception.ClientException;
import com.microsoft.identity.common.java.logging.Logger;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import lombok.AllArgsConstructor;

/**
 * Class to serialize and deserialize query parameters from List<Map.Entry<String, String>> to json String
 * and vice versa.
 *
 * NOTE: Even we no longer use Pair (Since it's android-only), we are keeping this the same
 *       to maintain backcompat with serialized value from older common that still uses it.
 */
@AllArgsConstructor
public class QueryParamsAdapter extends TypeAdapter<List<Map.Entry<String, String>>> {

    private static final String TAG = QueryParamsAdapter.class.getSimpleName();

    private static final Gson mGson;

    /**
     * If enabled, this will write a json string in the 'proper' format.
     * i.e. {"eqp1":"1","eqp2","2"}.
     *
     * Otherwise, it will write in the backcompat 'List<Pair<String, String>' format.
     * i.e. [{"first":"eqp1","second":"1"},{"first":"eqp2","second":"2"}]
     * */
    boolean mWriteProperFormat = false;

    static {
        final GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(
                getListType(),
                // Turn off the "proper" format for now, because it'll break backcompat.
                new QueryParamsAdapter(false)
        );
        mGson = gsonBuilder.create();
    }

    @Override
    public void write(final JsonWriter out, final List<Map.Entry<String, String>> queryParams) throws IOException {
        if (mWriteProperFormat) {
            // The "proper" format. We don't use it now because it'll break backcompat. The older broker doesn't support it.
            // i.e. {"eqp1":"1","eqp2","2"}.
            writeProperFormat(out, queryParams);
        } else {
            // Backcompat to support the old "List<Pair<String,String>>" format
            // i.e. [{"first":"eqp1","second":"1"},{"first":"eqp2","second":"2"}]
            writeListPairFormat(out, queryParams);
        }
    }

    private void writeProperFormat(final JsonWriter out, final List<Map.Entry<String, String>> queryParams) throws IOException {
        out.beginObject();
        for (final Map.Entry<String, String> keyValuePair : queryParams) {
            out.name(keyValuePair.getKey());
            out.value(keyValuePair.getValue());
        }
        out.endObject();
    }

    private void writeListPairFormat(final JsonWriter out, final List<Map.Entry<String, String>> queryParams) throws IOException {
        out.beginArray();
        for (final Map.Entry<String, String> keyValuePair : queryParams) {
            out.beginObject();
            out.name("first");
            out.value(keyValuePair.getKey());
            out.name("second");
            out.value(keyValuePair.getValue());
            out.endObject();
        }
        out.endArray();
    }

    @Override
    public List<Map.Entry<String, String>> read(final JsonReader in) throws IOException {
        switch (in.peek()) {
            // Backcompat to support the old "List<Pair<String,String>>" format
            // i.e. [{"first":"eqp1","second":"1"},{"first":"eqp2","second":"2"}]
            case BEGIN_ARRAY:
                return readListPairFormat(in);

            // i.e. {"eqp1":"1","eqp2","2"}, the "proper" one.
            // We don't use it now because it's not compatible with the old Broker.
            case BEGIN_OBJECT:
                return readProperFormat(in);
        }

        return new ArrayList<>();
    }

    private List<Map.Entry<String, String>> readProperFormat(final JsonReader in) throws IOException {
        final List<Map.Entry<String, String>> result = new ArrayList<>();

        in.beginObject();
        while (in.hasNext()) {
            final Map.Entry<String, String> keyValuePair = new AbstractMap.SimpleEntry<>(in.nextName(), in.nextString());
            result.add(keyValuePair);
        }
        in.endObject();
        return result;
    }

    private List<Map.Entry<String, String>> readListPairFormat(final JsonReader in) throws IOException {
        final List<Map.Entry<String, String>> result = new ArrayList<>();

        in.beginArray();
        while (in.hasNext()) {
            in.beginObject();

            String key = "";
            String value = "";

            while (in.hasNext()) {
                final String name = in.nextName();
                if (StringUtil.equalsIgnoreCase(name, "first")) {
                    key = in.nextString();
                } else if (StringUtil.equalsIgnoreCase(name, "second")) {
                    value = in.nextString();
                } else{
                    throw new JsonSyntaxException("Unexpected NAME field: " + name);
                }
            }

            result.add(new AbstractMap.SimpleEntry<>(key, value));
            in.endObject();
        }
        in.endArray();
        return result;
    }

    /**
     * Serializes a query string parameter map.
     *
     * @param extraQueryStringParameters an object to serialize.
     * @return a serialized string.
     * */
    public static String _toJson(final List<Map.Entry<String, String>> extraQueryStringParameters) {
        return mGson.toJson(extraQueryStringParameters, getListType());
    }

    /**
     * Deserializes a string into a query string parameter map.
     *
     * @param jsonString a string to deserialize.
     * @return a deserialized object.
     * */
    public static List<Map.Entry<String, String>> _fromJson(final String jsonString)
            throws ClientException{
        final String methodName = ":_fromJson";

        if (StringUtil.isNullOrEmpty(jsonString)) {
            return new ArrayList<>();
        }

        try {
            return mGson.fromJson(jsonString, getListType());
        } catch (final JsonSyntaxException e) {
            final String errorMessage = "malformed json string:" + jsonString;
            Logger.error(TAG + methodName, errorMessage, e);
            throw new ClientException(ClientException.JSON_PARSE_FAILURE, errorMessage, e);
        }
    }

    /**
     * Create a Type for the List of query params.
     *
     * @return a Type object representing the type of the query params.
     */
    public static Type getListType() {
        return TypeToken.getParameterized(List.class, TypeToken.getParameterized(Map.Entry.class, String.class, String.class).getRawType()).getType();
    }
}
