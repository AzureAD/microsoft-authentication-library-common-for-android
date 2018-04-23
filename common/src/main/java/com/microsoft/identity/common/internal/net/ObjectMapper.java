package com.microsoft.identity.common.internal.net;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.net.URLEncoder;
import java.util.Iterator;
import java.util.TreeMap;

public class ObjectMapper {

    public final static String ENCODING_SCHEME = "UTF-8";

    public static String serializeObjectToJsonString(Object object) {
        return new Gson().toJson(object);
    }

    public static <T> T deserializeJsonStringToObject(String json, Class<T> objectClass) {
        return new Gson().fromJson(json, objectClass);
    }

    public static String serializeObjectToFormUrlEncoded(Object object) throws UnsupportedEncodingException {
        String json = ObjectMapper.serializeObjectToJsonString(object);
        Type stringMap = new TypeToken<TreeMap<String, String>>() {
        }.getType();
        TreeMap<String, String> fields = new Gson().fromJson(json, stringMap);

        StringBuilder builder = new StringBuilder();

        Iterator<TreeMap.Entry<String, String>> iterator = fields.entrySet().iterator();

        while (iterator.hasNext()) {
            TreeMap.Entry<String, String> entry = iterator.next();
            builder.append(URLEncoder.encode(entry.getKey(), ENCODING_SCHEME));
            builder.append('=');
            builder.append(URLEncoder.encode(entry.getValue(), ENCODING_SCHEME));

            if (iterator.hasNext()) {
                builder.append('&');
            }
        }
        return builder.toString();
    }

}
