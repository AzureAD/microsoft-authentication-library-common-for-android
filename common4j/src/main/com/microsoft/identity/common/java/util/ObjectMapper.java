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

import lombok.NonNull;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.TypeAdapter;
import com.google.gson.TypeAdapterFactory;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonToken;
import com.google.gson.stream.JsonWriter;
import com.microsoft.identity.common.java.WarningType;
import com.microsoft.identity.common.java.AuthenticationConstants;
import com.microsoft.identity.common.java.commands.parameters.IHasExtraParameters;
import com.microsoft.identity.common.java.logging.Logger;

import java.io.IOException;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

import lombok.val;

public final class ObjectMapper {

    /**
     * Encoding scheme.
     */
    public static final String ENCODING_SCHEME = "UTF-8";
    public static final String TAG = ObjectMapper.class.getSimpleName();
    public static final Gson GSON = new GsonBuilder()
            .registerTypeAdapterFactory(new UnknownParamTypeAdapterFactory())
            .create();

    /*
     * This likely deserves a comment.  What we're doing here is hooking the underlying GSON implementation's
     * use of JsonReader.  We KNOW that what's happening in the type adapter that we're delegating to is that if the
     * name of the field isn't in the names that they mapped, they skip it.  What we're doing instead is to keep
     * track of the last seen name, and then when we get called to skip a value, if it will be a string, save it in a map.
     * This map is linked to preserve the order of the parameters for testing use.  At the end of this process, we
     * store the resulting Iterable in the object that can accept it.
     *
     * In order to do this, we're providing a completely fake reader object to a new json reader and then
     * delegating all of its operations away.
     */
    public static class UnknownParamTypeAdapterFactory implements TypeAdapterFactory {
        @Override
        public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
            final TypeAdapter<T> adapter = gson.getDelegateAdapter(this, type);

            if (IHasExtraParameters.class.isAssignableFrom(type.getRawType())) {
                return new TypeAdapter<T>() {
                    @Override
                    public void write(JsonWriter out, T value) throws IOException {
                        adapter.write(out, value);
                    }

                    @Override
                    public T read(final JsonReader in) throws IOException {
                        final Map<String, String> otherKeys = new LinkedHashMap<>();
                        final Reader reader = new Reader() {
                            @Override
                            public int read(char[] cbuf, int off, int len) throws IOException {
                                return 0;
                            }

                            @Override
                            public void close() throws IOException {

                            }
                        };
                        final JsonReader jsonReader = new JsonReader(reader) {
                            String lastName = null;

                            public void beginArray() throws IOException {
                                in.beginArray();
                            }

                            public void endArray() throws IOException {
                                in.endArray();
                            }

                            public void beginObject() throws IOException {
                                in.beginObject();
                            }

                            public void endObject() throws IOException {
                                in.endObject();
                            }

                            public boolean hasNext() throws IOException {
                                return in.hasNext();
                            }

                            public JsonToken peek() throws IOException {
                                return in.peek();
                            }

                            public String nextName() throws IOException {
                                final String name = in.nextName();
                                lastName = name;
                                return name;
                            }

                            public String nextString() throws IOException {
                                return in.nextString();
                            }

                            public boolean nextBoolean() throws IOException {
                                return in.nextBoolean();
                            }

                            public void nextNull() throws IOException {
                                in.nextNull();
                            }

                            public double nextDouble() throws IOException {
                                return in.nextDouble();
                            }

                            public long nextLong() throws IOException {
                                return in.nextLong();
                            }

                            public int nextInt() throws IOException {
                                return in.nextInt();
                            }

                            public void close() throws IOException {
                                in.close();
                            }

                            public void skipValue() throws IOException {
                                JsonToken token = in.peek();
                                if (token == JsonToken.STRING) {
                                    otherKeys.put(lastName, in.nextString());
                                } else {
                                    in.skipValue();
                                }
                            }

                            @NonNull
                            @Override
                            public String toString() {
                                return in.toString();
                            }

                            public String getPath() {
                                return in.getPath();
                            }
                        };
                        T output = adapter.read(jsonReader);
                        ((IHasExtraParameters) output).setExtraParameters(Collections.unmodifiableMap(otherKeys).entrySet());
                        return output;
                    }
                };
            }
            return null;
        }
    }

    private ObjectMapper() {
        // Utility class.
    }

    /**
     * Serialize object to JSON string.
     *
     * @param object Object
     * @return JSON string
     */
    public static String serializeObjectToJsonString(Object object) {
        return GSON.toJson(object);
    }

    /**
     * Serialize object to JSON string.  Only serialize fields marked with the GSON Expose annotation
     *
     * @param object Object
     * @return JSON string
     */
    public static String serializeExposedFieldsOfObjectToJsonString(Object object) {
        GsonBuilder builder = new GsonBuilder();
        builder.excludeFieldsWithoutExposeAnnotation();
        Gson gson = builder.create();
        return gson.toJson(object);
    }

    /**
     * Deserialize Json String to Object.
     *
     * @param json        String
     * @param objectClass object class
     * @param <T>         type T
     * @return object
     */
    public static <T> T deserializeJsonStringToObject(String json, Class<T> objectClass) {
        return GSON.fromJson(json, objectClass);
    }

    /**
     * Method for serializing the contents of an object as a Url Encoded string.  Important to the implementation of
     * this method is the behavior of GSON which excludes null fields from the resulting JSON.  A TreeMap was used to
     * simplify testing.... the resulting url encoded string is in alphabetical order (keys).
     *
     * @param object Object
     * @return String
     * @throws UnsupportedEncodingException thrown if encoding not supported
     */
    public static String serializeObjectToFormUrlEncoded(Object object) throws UnsupportedEncodingException {
        Map<String, String> fields = constructMapFromObject(object);

        final StringBuilder builder = new StringBuilder();

        Iterator<Map.Entry<String, String>> iterator = fields.entrySet().iterator();

        //URLEncoder.encode doesn't support (String, Charset) in JDK 7,8
        while (iterator.hasNext()) {
            Map.Entry<String, String> entry = iterator.next();
            builder.append(URLEncoder.encode(entry.getKey(), AuthenticationConstants.ENCODING_UTF8_STRING));
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue(), AuthenticationConstants.ENCODING_UTF8_STRING));

            if (iterator.hasNext()) {
                builder.append('&');
            }
        }
        return builder.toString();
    }

    /**
     * Method for converting the contents of an object into a map.  Important to the implementation of
     * this method is the behavior of GSON which excludes null fields from the resulting JSON.  A
     * TreeMap was chosen to simplify testing, and the resulting map is in key order.
     *
     * @param object the object to convert.
     * @return a map representation of the object.
     */
    public static Map<String, String> constructMapFromObject(Object object) {
        String json = ObjectMapper.serializeObjectToJsonString(object);
        final Type stringMap = TypeToken.getParameterized(TreeMap.class, String.class, String.class).getType();
        TreeMap<String, String> fields = new Gson().fromJson(json, stringMap);
        if (object instanceof IHasExtraParameters) {
            final IHasExtraParameters params = (IHasExtraParameters) object;
            val extraParams = params.getExtraParameters();
            if (extraParams != null) {
                for (final Map.Entry<String, String> e : extraParams) {
                    if (e.getKey() != null) {
                        fields.put(e.getKey(), e.getValue());
                    }
                }
            }
        }
        return fields;
    }

    /**
     * Method to serialize the object into a map.
     *
     * @param object Object
     * @return a hash map.
     */
    public static Map<String, Object> serializeObjectHashMap(final Object object) {
        String json = ObjectMapper.serializeObjectToJsonString(object);

        // Suppressing unchecked warnings due to casting of type Map to Map<String, Object>
        @SuppressWarnings(WarningType.unchecked_warning)
        Map<String, Object> objectHashMap = GSON.fromJson(json, Map.class);

        return objectHashMap;
    }


    /**
     * Method to deserialize the query string into a map.
     *
     * @param queryString String
     * @return a {@link LinkedHashMap} containing the query params.
     */
    public static Map<String, String> deserializeQueryStringToMap(final String queryString) {
        final Map<String, String> decodedUrlMap = new LinkedHashMap<>();

        if (StringUtil.isNullOrEmpty(queryString)) {
            return decodedUrlMap;
        }

        final StringTokenizer tokenizer = new StringTokenizer(queryString, "&");
        while (tokenizer.hasMoreTokens()) {
            final String pair = tokenizer.nextToken();
            final String[] elements = pair.split("=");

            if (elements.length != 2) {
                continue;
            }

            try {
                final String key = URLDecoder.decode(elements[0], ENCODING_SCHEME);
                final String value = URLDecoder.decode(elements[1], ENCODING_SCHEME);

                if (!StringUtil.isNullOrEmpty(key) && !StringUtil.isNullOrEmpty(value)) {
                    decodedUrlMap.put(key, value);
                }
            } catch (final UnsupportedEncodingException e) {
                Logger.error(TAG, null, "Decode failed.", e);
            }
        }

        return decodedUrlMap;
    }
}

