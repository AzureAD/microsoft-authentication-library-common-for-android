package com.microsoft.identity.common.adal.internal.util;

import android.text.TextUtils;
import android.util.Log;

import com.microsoft.identity.common.adal.internal.net.HttpWebResponse;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public final class HashMapExtensions {

    private static final String TAG = "HashMapExtensions";

    private HashMapExtensions() {
        // Intentionally left blank
    }
    /**
     * decode url string into a key value pairs with default query delimiter.
     *
     * @param parameters URL query parameter
     * @return key value pairs
     */
    static HashMap<String, String> urlFormDecode(String parameters) {
        return urlFormDecodeData(parameters, "&");
    }

    /**
     * decode url string into a key value pairs with given query delimiter given
     * string as a=1&b=2 will return key value of [[a,1],[b,2]].
     *
     * @param parameters URL parameter to be decoded
     * @param delimiter query delimiter
     * @return Map key value pairs
     */
    static HashMap<String, String> urlFormDecodeData(String parameters, String delimiter) {
        final HashMap<String, String> result = new HashMap<>();

        if (!StringExtensions.isNullOrBlank(parameters)) {
            StringTokenizer parameterTokenizer = new StringTokenizer(parameters, delimiter);

            while (parameterTokenizer.hasMoreTokens()) {
                String pair = parameterTokenizer.nextToken();
                String[] elements = pair.split("=");

                if (elements.length == 2) {
                    String key = null;
                    String value = null;
                    try {
                        key = StringExtensions.urlFormDecode(elements[0].trim());
                        value = StringExtensions.urlFormDecode(elements[1].trim());
                    } catch (UnsupportedEncodingException e) {
                        Log.d(TAG, e.getMessage());
                    }

                    if (!StringExtensions.isNullOrBlank(key)
                            && !StringExtensions.isNullOrBlank(value)) {
                        result.put(key, value);
                    }
                }
            }
        }

        return result;
    }


    /**
     * get key value pairs from response.
     * @param webResponse HttpWebResponse to convert to a map
     * @return Map
     * @throws JSONException
     */
    static Map<String, String> getJsonResponse(HttpWebResponse webResponse) throws JSONException {
        final Map<String, String> response = new HashMap<>();
        if (webResponse != null && !TextUtils.isEmpty(webResponse.getBody())) {
            JSONObject jsonObject = new JSONObject(webResponse.getBody());
            Iterator<?> i = jsonObject.keys();
            while (i.hasNext()) {
                String key = (String) i.next();
                response.put(key, jsonObject.getString(key));
            }
        }
        return response;
    }

}
