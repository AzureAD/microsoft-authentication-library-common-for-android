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

import android.support.annotation.Nullable;
import android.util.Base64;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.microsoft.identity.common.exception.ErrorStrings;
import com.microsoft.identity.common.exception.ServiceException;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.Iterator;
import java.util.Map;

/**
 * String utilities
 */
public class StringUtil {
    public static boolean isEmpty(final String message) {
        return message == null || message.trim().length() == 0;
    }

    /**
     * This method is to check if the specified Json could be deserialized into an object of the specified class
     *
     * @param jsonInString
     * @param cls
     * @return true if the jsonInString could be deserialized into the specified class. False otherwise.
     */
    public static void validateJsonFormat(final String jsonInString, @Nullable Class<?> cls) throws ServiceException {
        try {
            final Gson gson = new Gson();
            if (null == cls) {
                gson.fromJson(jsonInString, Object.class);
            } else {
                gson.fromJson(jsonInString, cls);
            }
        } catch (final JsonSyntaxException jsonSyntaxException) {
            throw new ServiceException(ErrorStrings.JSON_PARSE_FAILURE,
                    "The passed-in string is not JSON valid format.", jsonSyntaxException);
        }
    }

    /**
     * Check if the raw JWT string is valid JSON formatted.
     *
     * @param rawJWTStr
     * @throws ServiceException
     */
    public static void validateJWTFormat(final String rawJWTStr) throws ServiceException {
        final String idBody = extractJWTBody(rawJWTStr);
        final byte[] data = Base64.decode(idBody, Base64.URL_SAFE);
        try {
            final String decodedBody = new String(data, "UTF-8");
            validateJsonFormat(decodedBody, null);
        } catch (final UnsupportedEncodingException exception) {
            throw new ServiceException(ErrorStrings.UNSUPPORTED_ENCODING, exception.getMessage(), exception);
        }
    }

    static String extractJWTBody(final String idToken) throws ServiceException {
        final int firstDot = idToken.indexOf('.');
        final int secondDot = idToken.indexOf('.', firstDot + 1);
        final int invalidDot = idToken.indexOf('.', secondDot + 1);

        if (invalidDot == -1 && firstDot > 0 && secondDot > 0) {
            return idToken.substring(firstDot + 1, secondDot);
        } else {
            throw new ServiceException(ErrorStrings.INVALID_JWT, "Cannot parse IdToken", null);
        }
    }

    public static void extractJsonObjects(Map<String, String> responseItems, String jsonStr) throws ServiceException{
        try {
            final JSONObject jsonObject = new JSONObject(jsonStr);
            final Iterator<?> i = jsonObject.keys();

            while (i.hasNext()) {
                final String key = (String) i.next();
                responseItems.put(key, jsonObject.getString(key));
            }
        } catch (final JSONException jsonException) {
            throw new ServiceException(ErrorStrings.UNSUPPORTED_ENCODING, jsonException.getMessage(), jsonException);
        }


    }
}
