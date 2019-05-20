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

import android.util.Pair;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;

/**
 * Class to serialize and deserialize query parameters from List<Pair<String, String>> to json String
 * and vice versa
 */
public class QueryParamsAdapter extends TypeAdapter<List<Pair<String, String>>> {

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

        for(final Pair<String, String> pair : queryParams){
            out.name(pair.first);
            out.value(pair.second);
        }
        out.endObject();
    }

    @Override
    public List<Pair<String, String>> read(final JsonReader in) throws IOException {
        in.beginObject();
        final List<Pair<String, String>> result = new ArrayList<>();
        while (in.hasNext()){
            final String key = in.nextName();
            final String value = in.nextString();
            final Pair<String, String> pair = new Pair<>(key, value);
            result.add(pair);
        }
        in.endObject();
        return result;
    }

    public static String _toJson(final List<Pair<String, String>> extraQueryStringParameters) {
        final Type listType = new TypeToken<List<Pair<String, String>>>(){}.getType();
        return mGson.toJson(extraQueryStringParameters, listType);
    }

    public static List<Pair<String, String>> _fromJson(final String jsonString) {
        final Type listType = new TypeToken<List<Pair<String, String>>>(){}.getType();
        return mGson.fromJson(jsonString, listType);
    }

}
