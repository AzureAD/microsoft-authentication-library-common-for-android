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

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.microsoft.identity.common.WarningType;
import com.microsoft.identity.common.adal.internal.AuthenticationConstants;
import com.microsoft.identity.common.internal.commands.parameters.IHasExtraParameters;
import com.microsoft.identity.common.internal.logging.Logger;
import com.microsoft.identity.common.internal.util.StringUtil;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.TreeMap;

public final class ObjectMapper {

    /**
     * Encoding scheme.
     */
    public static final String ENCODING_SCHEME = "UTF-8";
    public static final String TAG = ObjectMapper.class.getSimpleName();

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
        return new Gson().toJson(object);
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
        return new Gson().fromJson(json, objectClass);
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
        Map<String, String> fields = constuctMapFromObject(object);

        final StringBuilder builder = new StringBuilder();

        Iterator<TreeMap.Entry<String, String>> iterator = fields.entrySet().iterator();

        while (iterator.hasNext()) {
            TreeMap.Entry<String, String> entry = iterator.next();
            builder.append(URLEncoder.encode(entry.getKey(), AuthenticationConstants.ENCODING_UTF8));
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue(), AuthenticationConstants.ENCODING_UTF8));

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
    public static Map<String, String> constuctMapFromObject(Object object) {
        String json = ObjectMapper.serializeObjectToJsonString(object);
        Type stringMap = new TypeToken<TreeMap<String, String>>() {
        }.getType();
        TreeMap<String, String> fields = new Gson().fromJson(json, stringMap);
        if (object instanceof IHasExtraParameters) {
            final IHasExtraParameters params = (IHasExtraParameters) object;
            if (params.getExtraParameters() != null) {
                for (final Map.Entry<String, String> e : params.getExtraParameters()) {
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
     * @return Map<String,Object>
     */
    public static Map<String, Object> serializeObjectHashMap(final Object object) {
        String json = ObjectMapper.serializeObjectToJsonString(object);

        // Suppressing unchecked warnings due to casting of type Map to Map<String, Object>
        @SuppressWarnings(WarningType.unchecked_warning)
        Map<String, Object> objectHashMap = new Gson().fromJson(json, Map.class);

        return objectHashMap;
    }


    /**
     * Method to deserialize the query string into a map.
     *
     * @param queryString String
     * @return Map
     */
    public static Map<String, String> deserializeQueryStringToMap(final String queryString) {
        final Map<String, String> decodedUrlMap = new HashMap<>();

        if (StringUtil.isEmpty(queryString)) {
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

                if (!StringUtil.isEmpty(key) && !StringUtil.isEmpty(value)) {
                    decodedUrlMap.put(key, value);
                }
            } catch (final UnsupportedEncodingException e) {
                Logger.error(TAG, null, "Decode failed.", e);
            }
        }

        return decodedUrlMap;
    }
}
