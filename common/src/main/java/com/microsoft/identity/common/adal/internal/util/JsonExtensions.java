package com.microsoft.identity.common.adal.internal.util;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


/**
 * Helper methods related to JSON
 */
public class JsonExtensions {

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
}
